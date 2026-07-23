package com.ci.pipeline.service.strategy.pipeline.parameter.impl;

import com.ci.pipeline.dao.entity.PipelineParameter;
import com.ci.pipeline.service.strategy.ParamResolveContext;
import org.springframework.stereotype.Component;

/**
 * app-name 参数策略：从流水线上下文获取 appName。
 */
@Component("app-name")
public class AppNameStrategy extends DefaultPipelineParameterStrategy {

    @Override
    public String buildParameter(PipelineParameter param, ParamResolveContext context) {
        return context.getAppName();
    }
}
