package com.ci.pipeline.service.service;

/**
 * 流水线终态清理服务：终态触发清理流水线 + 回调置成功 + 兜底检查
 */
public interface PipelineCleanService {

    /**
     * 异步触发清理流水线（best-effort，失败不影响终态落地主流程）。
     * 幂等由 pipeline_clean_run 表的 uk_run_name 唯一约束保证。
     *
     * @param pipelineRunId 业务流水线执行记录 id
     */
    void triggerCleanAsync(Long pipelineRunId);

    /**
     * 清理完成回调：按 runName 将 Running 记录 CAS 置为 Succeeded（幂等）。
     *
     * @param pipelineRunName 业务流水线的 pipelineRunName
     */
    void handleCallback(String pipelineRunName);
}
