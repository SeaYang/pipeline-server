package com.ci.pipeline.service.strategy.defaultvalue.impl;

import com.ci.pipeline.service.strategy.defaultvalue.DefaultValueStrategy;
import com.ci.pipeline.service.strategy.ParamResolveContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * AppConfig 策略（空实现，预留扩展点）。
 * <p>本期直接返回 null，后续可从应用配置表读取参数默认值。
 */
@Slf4j
@Component
public class AppConfigStrategy implements DefaultValueStrategy {

    @Override
    public String strategyType() {
        return "AppConfig";
    }

    @Override
    public String getValue(String paramName, ParamResolveContext context) {
        // 预留扩展：后续从应用配置表读取
        return null;
    }
}
