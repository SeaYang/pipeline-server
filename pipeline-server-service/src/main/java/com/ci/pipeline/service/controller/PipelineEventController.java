package com.ci.pipeline.service.controller;

import com.ci.pipeline.common.result.Result;
import com.ci.pipeline.facade.request.PipelineEventTriggerRequest;
import com.ci.pipeline.service.strategy.event.PipelineEventStrategy;
import com.ci.pipeline.service.strategy.event.PipelineEventStrategyManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 流水线事件触发控制器（对外开放，无认证）。
 * <p>供第三方系统通过 API 自动触发流水线执行。
 */
@Slf4j
@RestController
@RequestMapping("/pipeline/event")
public class PipelineEventController {

    @Autowired
    private PipelineEventStrategyManager strategyManager;

    /**
     * 事件触发流水线。
     * <p>整体 HTTP 状态码始终为 200，通过返回结果中每条记录的 errorMessage 区分成功/失败。
     *
     * @param request 触发请求（含 eventType 和 paramList）
     * @return 触发结果
     */
    @PostMapping("/trigger")
    public Result<Object> trigger(@RequestBody PipelineEventTriggerRequest request) {
        PipelineEventStrategy strategy = strategyManager.getStrategy(request.getEventType());
        return Result.success(strategy.execute(request));
    }
}
