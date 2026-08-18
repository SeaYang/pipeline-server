package com.ci.pipeline.service.strategy.event;

import com.ci.pipeline.facade.request.PipelineEventTriggerRequest;

/**
 * 流水线事件触发策略接口。
 * <p>每种事件类型对应一个策略实现类，负责该事件的参数校验与触发执行。
 * <p>策略 Bean 名称必须与事件类型编码（dict_data.dict_key）一致，
 * 通过 Spring Map 注入由 {@link PipelineEventStrategyManager} 自动路由。
 */
public interface PipelineEventStrategy {

    /**
     * 事件类型编码（对应字典 pipeline-event-type 的 dict_key）
     *
     * @return 事件类型编码
     */
    String eventType();

    /**
     * 执行事件触发。
     * <p>不同策略的出参结构可能不同，因此返回 Object。
     * 调用方（Controller）拿到 Object 后，按具体策略的响应类型强转。
     *
     * @param request 触发请求（含 eventType 和 paramList）
     * @return 触发结果，具体类型由策略实现决定
     */
    Object execute(PipelineEventTriggerRequest request);
}
