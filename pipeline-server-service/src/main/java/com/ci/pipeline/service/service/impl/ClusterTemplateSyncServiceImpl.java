package com.ci.pipeline.service.service.impl;

import com.ci.pipeline.common.exception.BusinessException;
import com.ci.pipeline.dao.entity.ClusterInfo;
import com.ci.pipeline.dao.entity.PipelineTemplateVersion;
import com.ci.pipeline.dao.entity.TaskTemplateVersion;
import com.ci.pipeline.dao.repository.PipelineTemplateVersionRepository;
import com.ci.pipeline.dao.repository.TaskTemplateVersionRepository;
import com.ci.pipeline.facade.response.ClusterSyncReportResponse;
import com.ci.pipeline.facade.response.ClusterSyncResultResponse;
import com.ci.pipeline.service.remote.ArgoWorkflowAgent;
import com.ci.pipeline.service.service.ClusterConfigService;
import com.ci.pipeline.service.service.ClusterTemplateSyncService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.argoproj.workflow.models.IoArgoprojWorkflowV1alpha1WorkflowTemplate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 模板多集群同步服务实现。
 * <p>同步范围 = 所有 enabled 集群（摘流 online=0 的集群仍参与同步：摘流是"临时不调度"，
 * 模板保持同步才能在恢复上线时立即可用）。
 * <p>部分成功策略：单集群失败不中断其他集群，明细返回，由调用方决定 DB 状态变更与补偿提示。
 */
@Slf4j
@Service
public class ClusterTemplateSyncServiceImpl implements ClusterTemplateSyncService {

    @Autowired
    private ClusterConfigService clusterConfigService;

    @Autowired
    private ArgoWorkflowAgent argoWorkflowAgent;

    @Autowired
    private PipelineTemplateVersionRepository pipelineTemplateVersionRepository;

    @Autowired
    private TaskTemplateVersionRepository taskTemplateVersionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    @Qualifier("clusterSyncExecutor")
    private ThreadPoolTaskExecutor clusterSyncExecutor;

    @Override
    public List<ClusterSyncResultResponse> saveTemplateToAllClusters(String templateCode,
                                                                      String templateDetail) {
        List<ClusterInfo> clusters = clusterConfigService.listEnabled();
        if (clusters.isEmpty()) {
            throw new BusinessException("当前暂无启用的集群，无法同步模板，请先在集群管理页面录入集群");
        }
        return clusters.parallelStream()
                .map(cluster -> {
                    String clusterName = cluster.getClusterName();
                    try {
                        // 每个集群独立反序列化：并行同步下共享同一对象会被 update 分支回填的
                        // resourceVersion 污染 create 分支（见接口 javadoc）
                        IoArgoprojWorkflowV1alpha1WorkflowTemplate workflowTemplate =
                                parseTemplate(templateDetail, templateCode);
                        argoWorkflowAgent.saveWorkflowTemplate(
                                clusterName, clusterConfigService.getNamespace(clusterName), workflowTemplate);
                        return ClusterSyncResultResponse.success(clusterName);
                    } catch (Exception e) {
                        log.error("保存模板到集群失败, templateCode={}, clusterName={}", templateCode, clusterName, e);
                        return ClusterSyncResultResponse.failure(clusterName, e.getMessage());
                    }
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ClusterSyncResultResponse> deleteTemplateFromAllClusters(String templateCode, String templateName) {
        List<ClusterInfo> clusters = clusterConfigService.listEnabled();
        if (clusters.isEmpty()) {
            throw new BusinessException("当前暂无启用的集群，无法删除模板");
        }
        return clusters.parallelStream()
                .map(cluster -> {
                    String clusterName = cluster.getClusterName();
                    try {
                        argoWorkflowAgent.deleteWorkflowTemplate(
                                clusterName, clusterConfigService.getNamespace(clusterName), templateName);
                        return ClusterSyncResultResponse.success(clusterName);
                    } catch (Exception e) {
                        log.error("从集群删除模板失败, templateCode={}, clusterName={}", templateCode, clusterName, e);
                        return ClusterSyncResultResponse.failure(clusterName, e.getMessage());
                    }
                })
                .collect(Collectors.toList());
    }

    @Override
    public ClusterSyncReportResponse syncAllTemplatesToCluster(String clusterName) {
        clusterConfigService.getByClusterName(clusterName);
        String namespace = clusterConfigService.getNamespace(clusterName);

        List<PipelineTemplateVersion> pipelineVersions = pipelineTemplateVersionRepository.listAllEffective();
        List<TaskTemplateVersion> taskVersions = taskTemplateVersionRepository.listAllEffective();
        log.info("全量同步模板到集群开始, clusterName={}, pipelineTemplates={}, taskTemplates={}",
                clusterName, pipelineVersions.size(), taskVersions.size());

        ClusterSyncReportResponse report = new ClusterSyncReportResponse();
        report.setClusterName(clusterName);
        List<ClusterSyncResultResponse> results = new ArrayList<>();

        // 两阶段串行、阶段内并行：Argo 的 lint/create 会校验 templateRef 引用的模板存在，
        // 流水线模板引用任务模板，必须等任务模板全部同步完成后再同步流水线模板，
        // 否则并行下流水线模板的 lint 可能先于任务模板落库而误报
        // "template reference xxx not found"（任务模板事后看是存在的）。
        // 阶段内用 clusterSyncExecutor 并行（设计文档 11.4），缩短新集群接入耗时。
        List<java.util.concurrent.Callable<ClusterSyncResultResponse>> taskJobs = new ArrayList<>();
        taskVersions.forEach(v -> taskJobs.add(() ->
                syncOne(clusterName, namespace, "task", v.getTaskTemplateCode(), v.getTemplateDetail())));
        results.addAll(runSyncJobs(clusterName, taskJobs));

        List<java.util.concurrent.Callable<ClusterSyncResultResponse>> pipelineJobs = new ArrayList<>();
        pipelineVersions.forEach(v -> pipelineJobs.add(() ->
                syncOne(clusterName, namespace, "pipeline", v.getPipelineTemplateCode(), v.getTemplateDetail())));
        results.addAll(runSyncJobs(clusterName, pipelineJobs));

        report.setTotal(results.size());
        report.setSuccessCount((int) results.stream().filter(ClusterSyncResultResponse::getSuccess).count());
        report.setFailureCount((int) results.stream().filter(r -> !r.getSuccess()).count());
        report.setFailures(results.stream().filter(r -> !r.getSuccess()).collect(Collectors.toList()));
        log.info("全量同步模板到集群完成, clusterName={}, total={}, success={}, failure={}",
                clusterName, report.getTotal(), report.getSuccessCount(), report.getFailureCount());
        return report;
    }

    @Override
    public void syncAllTemplatesToClusterAsync(String clusterName) {
        clusterSyncExecutor.execute(() -> {
            try {
                ClusterSyncReportResponse report = syncAllTemplatesToCluster(clusterName);
                if (!report.isAllSuccess()) {
                    log.warn("新集群模板自动同步存在失败项, clusterName={}, failures={}",
                            clusterName, report.getFailures());
                }
            } catch (Exception e) {
                log.error("新集群模板自动同步失败, clusterName={}", clusterName, e);
            }
        });
    }

    @Override
    public List<ClusterSyncResultResponse> resyncPipelineTemplate(String pipelineTemplateCode, String clusterName) {
        PipelineTemplateVersion effective = pipelineTemplateVersionRepository.selectEffectiveByCode(pipelineTemplateCode);
        if (effective == null) {
            throw new BusinessException(String.format("流水线模板无生效版本, pipelineTemplateCode=%s", pipelineTemplateCode));
        }
        return resync("pipeline", pipelineTemplateCode, effective.getTemplateDetail(), clusterName);
    }

    @Override
    public List<ClusterSyncResultResponse> resyncTaskTemplate(String taskTemplateCode, String clusterName) {
        // 任务模板按 code 查生效版本
        List<TaskTemplateVersion> versions = taskTemplateVersionRepository.listByCode(taskTemplateCode);
        TaskTemplateVersion effectiveVersion = versions.stream()
                .filter(v -> "EFFECTIVE".equals(v.getStatus()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        String.format("任务模板无生效版本, taskTemplateCode=%s", taskTemplateCode)));
        return resync("task", taskTemplateCode, effectiveVersion.getTemplateDetail(), clusterName);
    }

    private List<ClusterSyncResultResponse> resync(String type, String code, String templateDetail, String clusterName) {
        if (StringUtils.isNotBlank(clusterName)) {
            clusterConfigService.getByClusterName(clusterName);
            try {
                argoWorkflowAgent.saveWorkflowTemplate(clusterName, clusterConfigService.getNamespace(clusterName),
                        parseTemplate(templateDetail, code));
                return new ArrayList<>(java.util.Collections.singletonList(ClusterSyncResultResponse.success(clusterName)));
            } catch (Exception e) {
                log.error("重推模板到集群失败, type={}, code={}, clusterName={}", type, code, clusterName, e);
                return new ArrayList<>(java.util.Collections.singletonList(
                        ClusterSyncResultResponse.failure(clusterName, e.getMessage())));
            }
        }
        return saveTemplateToAllClusters(code, templateDetail);
    }

    /**
     * 提交一批同步任务到 clusterSyncExecutor 并行执行，等待全部完成后返回结果（不抛异常，失败记入结果）。
     */
    private List<ClusterSyncResultResponse> runSyncJobs(String clusterName,
                                                        List<java.util.concurrent.Callable<ClusterSyncResultResponse>> jobs) {
        List<ClusterSyncResultResponse> results = new ArrayList<>(jobs.size());
        try {
            List<java.util.concurrent.Future<ClusterSyncResultResponse>> futures = new ArrayList<>(jobs.size());
            jobs.forEach(job -> futures.add(clusterSyncExecutor.submit(job)));
            for (java.util.concurrent.Future<ClusterSyncResultResponse> future : futures) {
                try {
                    results.add(future.get());
                } catch (Exception e) {
                    log.error("等待模板同步任务结果失败, clusterName={}", clusterName, e);
                }
            }
        } catch (Exception e) {
            throw new BusinessException("全量同步模板执行失败, clusterName=" + clusterName + ", error=" + e.getMessage());
        }
        return results;
    }

    private ClusterSyncResultResponse syncOne(String clusterName, String namespace, String type, String code, String templateDetail) {
        try {
            IoArgoprojWorkflowV1alpha1WorkflowTemplate workflowTemplate = parseTemplate(templateDetail, code);
            argoWorkflowAgent.saveWorkflowTemplate(clusterName, namespace, workflowTemplate);
            return ClusterSyncResultResponse.success(clusterName);
        } catch (Exception e) {
            log.error("同步模板到集群失败, type={}, code={}, clusterName={}", type, code, clusterName, e);
            return ClusterSyncResultResponse.failure(clusterName, e.getMessage());
        }
    }

    private IoArgoprojWorkflowV1alpha1WorkflowTemplate parseTemplate(String templateDetail, String code) {
        try {
            return objectMapper.readValue(templateDetail, IoArgoprojWorkflowV1alpha1WorkflowTemplate.class);
        } catch (Exception e) {
            throw new BusinessException(String.format("模板详情解析失败, code=%s, error=%s", code, e.getMessage()));
        }
    }
}
