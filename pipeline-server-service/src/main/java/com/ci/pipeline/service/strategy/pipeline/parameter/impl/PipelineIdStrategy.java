package com.ci.pipeline.service.strategy.pipeline.parameter.impl;

import com.ci.pipeline.dao.entity.PipelineParameter;
import com.ci.pipeline.service.strategy.ParamResolveContext;
import org.springframework.stereotype.Component;

/**
 * pipeline-id 参数策略：从流水线上下文获取 pipelineId。
 */
@Component("pipeline-id")
public class PipelineIdStrategy extends DefaultPipelineParameterStrategy {

    @Override
    public String buildParameter(PipelineParameter param, ParamResolveContext context) {
        return context.getPipelineId() != null ? String.valueOf(context.getPipelineId()) : null;
    }
}
