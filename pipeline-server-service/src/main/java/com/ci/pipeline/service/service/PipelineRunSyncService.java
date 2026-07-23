package com.ci.pipeline.service.service;

/**
 * 流水线执行状态同步引擎。
 * <p>核心职责：查 Argo → 对比状态 → 乐观锁回写 pipeline_run → 触发状态变化 Hook。
 * 同时供「执行后异步轮询」与「手动兜底同步」两类场景复用。
 */
public interface PipelineRunSyncService {

    /**
     * 异步轮询直到终态：循环查 Argo → 对比状态/generation → 乐观锁回写 pipeline_run + 刷新快照，
     * 未到终态则按配置间隔 sleep，命中终态或达到最大尝试次数后退出。
     * <p>整体捕获异常并打日志，避免异步线程静默退出。「执行 / 重试 / 手动兜底同步」均提交此任务到同步线程池。
     *
     * @param pipelineRunId 执行记录 id
     */
    void syncUntilTerminal(Long pipelineRunId);

    /**
     * 终态处理：拉取最新 Argo Workflow，落地任务节点记录（仅 Pod 节点，含日志）并刷新执行详情快照。
     * <p>用于「停止」等由平台直接置终态、绕过常规 phase 流转的场景（常规流转中终态处理已在 {@link #syncUntilTerminal} 内完成）。
     *
     * @param pipelineRunId 执行记录 id
     */
    void handleTerminal(Long pipelineRunId);
}
