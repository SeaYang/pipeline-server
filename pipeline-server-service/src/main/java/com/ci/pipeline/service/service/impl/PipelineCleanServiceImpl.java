package com.ci.pipeline.service.service.impl;

import com.ci.pipeline.common.constants.PipelineCleanConstants;
import com.ci.pipeline.common.enums.PipelineRunStatusEnum;
import com.ci.pipeline.dao.entity.PipelineCleanRun;
import com.ci.pipeline.dao.entity.PipelineRun;
import com.ci.pipeline.dao.repository.PipelineCleanRunRepository;
import com.ci.pipeline.dao.repository.PipelineRunRepository;
import com.ci.pipeline.service.remote.ArgoWorkflowAgent;
import com.ci.pipeline.service.service.ClusterConfigService;
import com.ci.pipeline.service.service.PipelineCleanService;
import io.argoproj.workflow.models.IoArgoprojWorkflowV1alpha1Workflow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * 流水线终态清理服务实现。
 * <p>终态落地完成后异步触发清理流水线（独立 Argo Workflow）：
 * 先插占位记录（uk_run_name 数据库层幂等）→ 提交清理 Workflow → 回填 cleanRunName。
 * 清理成功由 clean-notify 任务回调置 Succeeded；回调丢失由兜底定时任务反查 Argo 补偿。
 */
@Slf4j
@Service
public class PipelineCleanServiceImpl implements PipelineCleanService {

    @Autowired
    private PipelineCleanRunRepository pipelineCleanRunRepository;

    @Autowired
    private PipelineRunRepository pipelineRunRepository;

    @Autowired
    private ArgoWorkflowAgent argoWorkflowAgent;

    @Autowired
    private ClusterConfigService clusterConfigService;

    @Autowired
    @Qualifier("pipelineCleanExecutor")
    private ThreadPoolTaskExecutor pipelineCleanExecutor;

    @Override
    public void triggerCleanAsync(Long pipelineRunId) {
        pipelineCleanExecutor.execute(() -> {
            try {
                doTriggerClean(pipelineRunId);
            } catch (Exception e) {
                // best-effort：触发失败只打日志，不影响终态落地主流程；由兜底任务发现并补偿
                log.error("触发清理流水线失败, pipelineRunId={}", pipelineRunId, e);
            }
        });
    }

    /**
     * 实际触发逻辑：插占位记录（幂等锁）→ 提交清理 Workflow → 回填 cleanRunName。
     */
    private void doTriggerClean(Long pipelineRunId) {
        PipelineRun run = pipelineRunRepository.selectById(pipelineRunId);
        if (run == null || !StringUtils.hasText(run.getName())) {
            log.warn("流水线执行记录不存在或缺少名称，跳过清理触发, pipelineRunId={}", pipelineRunId);
            return;
        }
        // 1. 先插占位记录（uk_run_name 保证幂等；冲突说明已触发过，直接返回）
        PipelineCleanRun cleanRun = new PipelineCleanRun();
        cleanRun.setRunName(run.getName());
        cleanRun.setStatus(PipelineRunStatusEnum.RUNNING.getCode());
        try {
            pipelineCleanRunRepository.insert(cleanRun);
        } catch (DuplicateKeyException e) {
            log.info("清理记录已存在，跳过重复触发, runName={}", run.getName());
            return;
        }
        // 2. 提交清理 Workflow（模板名固定 pipeline-clean；Argo submit API 传参时模板 default 不生效，
        //    模板声明的参数必须全部显式传值，否则报 "xxx.value is required"）
        String clusterName = clusterConfigService.resolveRunClusterName(run);
        List<String> params = Arrays.asList(
                PipelineCleanConstants.PARAM_PIPELINE_RUN_NAME + "=" + run.getName(),
                PipelineCleanConstants.PARAM_CLEAN_WAIT_SECONDS + "=" + PipelineCleanConstants.CLEAN_WAIT_SECONDS_VALUE);
        IoArgoprojWorkflowV1alpha1Workflow workflow = argoWorkflowAgent.submitWorkflowByTemplate(
                clusterName, clusterConfigService.getNamespace(clusterName),
                PipelineCleanConstants.CLEAN_TEMPLATE_NAME, params);
        // 3. 回填 cleanRunName（提交失败抛异常 → 记录保持 Running + name 为空，由兜底任务发现）
        String cleanRunName = workflow.getMetadata() != null ? workflow.getMetadata().getName() : null;
        pipelineCleanRunRepository.updateCleanRunName(cleanRun.getId(), cleanRunName);
        log.info("清理流水线已提交, runName={}, cleanRunName={}, clusterName={}",
                run.getName(), cleanRunName, clusterName);
    }

    @Override
    public void handleCallback(String pipelineRunName) {
        if (!StringUtils.hasText(pipelineRunName)) {
            throw new com.ci.pipeline.common.exception.BusinessException("pipelineRunName 不能为空");
        }
        PipelineCleanRun cleanRun = pipelineCleanRunRepository.selectByRunName(pipelineRunName);
        if (cleanRun == null) {
            // 记录不存在直接返回成功，防止清理 Workflow 重试时任务反复 Failed
            log.warn("回调对应的清理记录不存在，忽略, pipelineRunName={}", pipelineRunName);
            return;
        }
        // CAS：仅 Running → Succeeded；命中 0 行说明已是终态（重复回调），天然幂等
        int rows = pipelineCleanRunRepository.casMarkSucceeded(cleanRun.getId());
        log.info("清理回调处理完成, runName={}, cleanRunId={}, updated={}",
                cleanRun.getRunName(), cleanRun.getId(), rows);
    }
}
