package com.ci.pipeline.service.service.hook;

/**
 * 流水线执行状态变化 Hook（事件扩展点）。
 * <p>状态同步引擎在检测到状态变更并回写成功后，按新状态分发到对应方法。
 * 所有方法均为 {@code default} 空实现，实现类按需覆写关心的状态即可；
 * 引擎会注入容器内所有 {@link PipelineRunStatusHook} Bean 并逐个调用，
 * 单个 Hook 抛异常不影响整体同步流程。
 */
public interface PipelineRunStatusHook {

    /**
     * 状态变为 Pending 时触发（一般首次落地即为 Pending，此回调较少触发）
     */
    default void onPending(PipelineRunStatusContext context) {
    }

    /**
     * 状态变为 Running 时触发
     */
    default void onRunning(PipelineRunStatusContext context) {
    }

    /**
     * 状态变为 Succeeded（成功）时触发
     */
    default void onSucceeded(PipelineRunStatusContext context) {
    }

    /**
     * 状态变为 Failed（失败）时触发
     */
    default void onFailed(PipelineRunStatusContext context) {
    }

    /**
     * 状态变为 Error（错误）时触发
     */
    default void onError(PipelineRunStatusContext context) {
    }

    /**
     * 状态变为 Unknown（未知，短暂态）时触发
     */
    default void onUnknown(PipelineRunStatusContext context) {
    }

    /**
     * 状态变为 Cancelled（已取消，平台扩展态）时触发
     */
    default void onCancelled(PipelineRunStatusContext context) {
    }
}
