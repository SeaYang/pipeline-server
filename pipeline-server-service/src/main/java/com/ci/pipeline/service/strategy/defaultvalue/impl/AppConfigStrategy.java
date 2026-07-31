package com.ci.pipeline.service.strategy.defaultvalue.impl;

import com.ci.pipeline.common.constants.AppParameterConfigConstants;
import com.ci.pipeline.service.service.AppParameterConfigService;
import com.ci.pipeline.service.strategy.ParamResolveContext;
import com.ci.pipeline.service.strategy.defaultvalue.DefaultValueStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * AppConfig 策略：从应用参数配置表（app_parameter_config）读取参数默认值。
 * <p>
 * env 值从 {@link ParamResolveContext#getResolvedValues()} 中获取（key 为 "env"），
 * 查询逻辑：先查指定 env 的配置，未查到则兜底查 default env。
 */
@Slf4j
@Component
public class AppConfigStrategy implements DefaultValueStrategy {

    @Autowired
    private AppParameterConfigService appParameterConfigService;

    @Override
    public String strategyType() {
        return "AppConfig";
    }

    @Override
    public String getValue(String paramName, ParamResolveContext context) {
        String appName = context.getAppName();
        if (!StringUtils.hasText(appName)) {
            return null;
        }

        // 从已解析参数中获取 env 值（env 参数在拓扑排序中先于依赖它的参数被计算）
        Map<String, String> resolvedValues = context.getResolvedValues();
        String env = resolvedValues != null ? resolvedValues.get(AppParameterConfigConstants.PARAM_NAME_ENV) : null;

        return appParameterConfigService.getValue(appName, paramName, env);
    }
}
