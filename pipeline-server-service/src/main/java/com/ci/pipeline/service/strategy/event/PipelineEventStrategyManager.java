package com.ci.pipeline.service.strategy.event;

import com.ci.pipeline.common.constants.PipelineEventConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 事件触发策略管理器。
 * <p>利用 Spring 的 Map 注入机制，自动装配所有 {@link PipelineEventStrategy} 实现。
 * key = Bean 名称（= eventType），value = 策略实例。
 */
@Component
public class PipelineEventStrategyManager {

    @Autowired
    private Map<String, PipelineEventStrategy> strategyMap;

    /**
     * 根据事件类型获取策略。
     *
     * @param eventType 事件类型编码
     * @return 策略实例
     * @throws IllegalArgumentException 不支持的事件类型
     */
    public PipelineEventStrategy getStrategy(String eventType) {
        PipelineEventStrategy strategy = strategyMap.get(eventType);
        if (strategy == null) {
            throw new IllegalArgumentException(
                    String.format(PipelineEventConstants.MSG_EVENT_TYPE_NOT_SUPPORTED, eventType));
        }
        return strategy;
    }
}
