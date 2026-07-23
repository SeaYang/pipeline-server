package com.ci.pipeline.service.service.hook;

import com.ci.pipeline.common.enums.PipelineRunStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 状态变化日志 Hook：示例实现，演示 {@link PipelineRunStatusHook} 扩展点的用法。
 * <p>仅在每个状态变更点打印一条结构化日志，业务方如需对接消息通知 / 回调等，新增实现类即可。
 */
@Slf4j
@Component
public class LoggingPipelineRunStatusHook implements PipelineRunStatusHook {

    @Override
    public void onRunning(PipelineRunStatusContext context) {
        log(PipelineRunStatusEnum.RUNNING, context);
    }

    @Override
    public void onSucceeded(PipelineRunStatusContext context) {
        log(PipelineRunStatusEnum.SUCCEEDED, context);
    }

    @Override
    public void onFailed(PipelineRunStatusContext context) {
        log(PipelineRunStatusEnum.FAILED, context);
    }

    @Override
    public void onError(PipelineRunStatusContext context) {
        log(PipelineRunStatusEnum.ERROR, context);
    }

    @Override
    public void onUnknown(PipelineRunStatusContext context) {
        log(PipelineRunStatusEnum.UNKNOWN, context);
    }

    @Override
    public void onCancelled(PipelineRunStatusContext context) {
        log(PipelineRunStatusEnum.CANCELLED, context);
    }

    private void log(PipelineRunStatusEnum status, PipelineRunStatusContext context) {
        log.info("流水线执行状态变更: pipelineRunId={}, name={}, {} -> {}, duration={}s, failMessage={}",
                context.getPipelineRunId(), context.getName(),
                context.getPreviousStatus() != null ? context.getPreviousStatus().getCode() : "null",
                status.getCode(), context.getDuration(), context.getFailMessage());
    }
}
