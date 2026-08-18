package com.ci.pipeline.service.service;

import com.ci.pipeline.facade.response.ClusterSyncReportResponse;
import com.ci.pipeline.facade.response.ClusterSyncResultResponse;
import io.argoproj.workflow.models.IoArgoprojWorkflowV1alpha1WorkflowTemplate;

import java.util.List;

/**
 * 模板多集群同步服务（发布/删除/新集群接入/手动重推）
 */
public interface ClusterTemplateSyncService {

    /**
     * 保存模板到所有 enabled 集群（发布 EFFECTIVE 时调用）。
     * <p>部分成功策略：单集群失败不中断其他集群，结果明细返回（DB 状态由调用方决定是否变更）。
     * <p>注意：入参为 templateDetail JSON 字符串，内部每个集群独立反序列化——
     * 并行同步时若共享同一个 WorkflowTemplate 对象，update 分支回填的 resourceVersion
     * 会污染并发中的 create 分支，报 "resourceVersion should not be set on objects to be created"。
     *
     * @param templateCode   模板编码（日志用）
     * @param templateDetail WorkflowTemplate JSON 字符串
 * @return 各集群同步结果明细
     */
    List<ClusterSyncResultResponse> saveTemplateToAllClusters(String templateCode,
                                                              String templateDetail);

    /**
     * 从所有 enabled 集群删除模板（删除版本时调用）。
     *
     * @param templateCode 模板编码（日志用）
     * @param templateName WorkflowTemplate 名称
     * @return 各集群同步结果明细
     */
    List<ClusterSyncResultResponse> deleteTemplateFromAllClusters(String templateCode, String templateName);

    /**
     * 全量同步所有 EFFECTIVE 模板（流水线 + 任务）到指定集群（同步执行，返回报告）。
     * <p>集群管理页"同步模板"按钮 / 手动补偿用。
     */
    ClusterSyncReportResponse syncAllTemplatesToCluster(String clusterName);

    /**
     * 全量同步的异步入口（新增集群 autoSyncTemplates=true 时调用，失败只记日志不阻塞）。
     */
    void syncAllTemplatesToClusterAsync(String clusterName);

    /**
     * 手动重推单个流水线模板到指定集群（clusterName 为空时重推所有 enabled 集群）。
     */
    List<ClusterSyncResultResponse> resyncPipelineTemplate(String pipelineTemplateCode, String clusterName);

    /**
     * 手动重推单个任务模板到指定集群（clusterName 为空时重推所有 enabled 集群）。
     */
    List<ClusterSyncResultResponse> resyncTaskTemplate(String taskTemplateCode, String clusterName);
}
