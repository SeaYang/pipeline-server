package com.ci.pipeline.service.strategy.defaultvalue;

import com.ci.pipeline.service.strategy.ParamResolveContext;

/**
 * 默认值策略接口（策略模式）。
 * <p>每个策略负责一种默认值来源（如应用配置、最近成功记录等），
 * 按 {@link #strategyType()} 路由，由 {@link DefaultValueStrategyManager} 按 priority 降序遍历。
 */
public interface DefaultValueStrategy {

    /**
     * 策略类型，对应 {@link com.ci.pipeline.common.enums.DefaultValueStrategyTypeEnum}。
     *
     * @return 策略类型编码
     */
    String strategyType();

    /**
     * 获取默认值。
     *
     * @param paramName 参数名
     * @param context   参数计算上下文
     * @return 默认值，返回 null 表示该策略未命中
     */
    String getValue(String paramName, ParamResolveContext context);
}
