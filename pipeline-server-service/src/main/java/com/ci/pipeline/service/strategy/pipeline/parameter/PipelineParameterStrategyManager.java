package com.ci.pipeline.service.strategy.pipeline.parameter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 流水线参数策略管理器（工厂/路由器）。
 * <p>启动时自动收集所有 {@link PipelineParameterStrategy} Bean，Spring 注入的 Map key 就是
 * {@code @Component} 注解的 value（即参数名）。
 * <p>调用 {@link #getStrategy(String)} 时用参数名查找策略，找不到则回退到
 * {@code DefaultPipelineParameterStrategy}。
 */
@Slf4j
@Component
public class PipelineParameterStrategyManager {

    private static final String DEFAULT_STRATEGY = "DefaultPipelineParameterStrategy";

    private final Map<String, PipelineParameterStrategy> strategyMap;

    @Autowired
    public PipelineParameterStrategyManager(Map<String, PipelineParameterStrategy> strategyMap) {
        this.strategyMap = strategyMap;
        log.info("PipelineParameterStrategyManager 初始化完成, 已注册策略: {}", strategyMap.keySet());
    }

    /**
     * 按参数名获取策略。
     * <p>如果参数名恰好匹配某个特定策略的 Bean 名称（如 {@code git-url}），则使用该策略；
     * 否则使用 {@code DefaultPipelineParameterStrategy}。
     *
     * @param paramName 参数名
     * @return 参数策略（永不返回 null，至少返回 Default 策略）
     */
    public PipelineParameterStrategy getStrategy(String paramName) {
        if (paramName != null && strategyMap.containsKey(paramName)) {
            return strategyMap.get(paramName);
        }
        return strategyMap.get(DEFAULT_STRATEGY);
    }
}
