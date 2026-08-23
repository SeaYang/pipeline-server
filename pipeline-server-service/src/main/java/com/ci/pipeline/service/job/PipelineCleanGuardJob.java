package com.ci.pipeline.service.job;

import com.ci.pipeline.common.enums.PipelineRunStatusEnum;
import com.ci.pipeline.dao.entity.PipelineCleanRun;
import com.ci.pipeline.dao.entity.PipelineRun;
import com.ci.pipeline.dao.repository.PipelineCleanRunRepository;
import com.ci.pipeline.dao.repository.PipelineRunRepository;
import com.ci.pipeline.service.remote.ArgoWorkflowAgent;
import com.ci.pipeline.service.service.ClusterConfigService;
import io.argoproj.workflow.models.IoArgoprojWorkflowV1alpha1Workflow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * 流水线终态清理兜底检查定时任务。
 * <p>由 cron_job 表配置调度（bean_name=pipelineCleanGuardJob, method=execute, 每 5 分钟），
 * 本类不写 @Scheduled。补偿三类异常：提交失败（clean_run_name 为空）、回调丢失、清理 Workflow 卡死。
 * <p>多实例部署时，CAS 更新（WHERE status='Running'）保证同一记录只会被一个实例成功处理。
 */
@Slf4j
@Component("pipelineCleanGuardJob")
public class PipelineCleanGuardJob {

    /** 清理 Workflow 的理论最大时长：sleep 30s + 删除 + 回调重试，给 15 分钟 + 缓冲 */
    private static final int TIMEOUT_MINUTES = 20;

    @Autowired
    private PipelineCleanRunRepository pipelineCleanRunRepository;

    @Autowired
    private PipelineRunRepository pipelineRunRepository;

    @Autowired
    private ArgoWorkflowAgent argoWorkflowAgent;

    @Autowired
    private ClusterConfigService clusterConfigService;

    /**
     * 兜底检查入口（由 cron_job 定时任务反射调用，无参数）。
     */
    public void execute() {
        Date deadline = new Date(System.currentTimeMillis() - TIMEOUT_MINUTES * 60_000L);
        List<PipelineCleanRun> timeouts = pipelineCleanRunRepository.listTimeoutRunning(deadline);
        if (timeouts.isEmpty()) {
            return;
        }
        log.info("[清理兜底] 发现超时 Running 清理记录 {} 条", timeouts.size());
        for (PipelineCleanRun cleanRun : timeouts) {
            try {
                // 1. 反查 Argo 区分成因，回填终态（先更新再通知，终态不可再变 → 通知天然防重发）
                resolveAndMarkTerminal(cleanRun);
                // 2. 通知（空实现，本期仅打日志）
                notifyCleanFailed(cleanRun);
            } catch (Exception e) {
                log.error("[清理兜底] 处理失败, cleanRunId={}, runName={}", cleanRun.getId(), cleanRun.getRunName(), e);
            }
        }
    }

    /**
     * 反查 Argo：按 clean_run_name 查清理 Workflow 的实际 phase，映射回填终态。
     */
    private void resolveAndMarkTerminal(PipelineCleanRun cleanRun) {
        // clean_run_name 为空 → 提交阶段就失败，直接置 Failed
        if (!StringUtils.hasText(cleanRun.getCleanRunName())) {
            pipelineCleanRunRepository.casMarkFailed(cleanRun.getId());
            return;
        }
        PipelineRun run = pipelineRunRepository.selectByName(cleanRun.getRunName());
        String clusterName = clusterConfigService.resolveRunClusterName(run);
        try {
            IoArgoprojWorkflowV1alpha1Workflow wf = argoWorkflowAgent.getWorkflow(
                    clusterName, clusterConfigService.getNamespace(clusterName), cleanRun.getCleanRunName());
            String phase = wf != null && wf.getStatus() != null ? wf.getStatus().getPhase() : null;
            PipelineRunStatusEnum status = PipelineRunStatusEnum.ofCode(phase);
            if (status == PipelineRunStatusEnum.SUCCEEDED || status == null) {
                // Succeeded：回调丢失但实际已成功；null：成功后被 TTL 删除 → "删除即成功"推定
                pipelineCleanRunRepository.casMarkSucceeded(cleanRun.getId());
            } else {
                // Failed / Error / 仍在 Running（卡死）
                pipelineCleanRunRepository.casMarkFailed(cleanRun.getId());
            }
        } catch (Exception e) {
            // 反查异常保守处理：置 Failed，等下一轮扫描或人工介入
            log.warn("[清理兜底] 反查 Argo 失败, cleanRunId={}", cleanRun.getId(), e);
            pipelineCleanRunRepository.casMarkFailed(cleanRun.getId());
        }
    }

    private void notifyCleanFailed(PipelineCleanRun cleanRun) {
        // TODO: 后续接入企微/钉钉；当前仅日志
        log.warn("[清理兜底] 清理流水线未成功, runName={}, cleanRunName={}, status={}, createTime={}",
                cleanRun.getRunName(), cleanRun.getCleanRunName(),
                cleanRun.getStatus(), cleanRun.getCreateTime());
    }
}
