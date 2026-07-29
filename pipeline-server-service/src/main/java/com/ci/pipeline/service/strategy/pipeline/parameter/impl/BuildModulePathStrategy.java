package com.ci.pipeline.service.strategy.pipeline.parameter.impl;

import com.ci.pipeline.common.util.PathUtil;
import com.ci.pipeline.dao.entity.PipelineParameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * build-module-path 参数策略：构建模块路径。
 * <p>该参数为 user 参数，不需要 {@code buildParameter}（用户自行选择值）。
 * 仅重写 {@link #systemProcess}，在默认值映射转换的基础上，将路径统一转换为以 {@code ./} 开头的相对路径格式。
 */
@Slf4j
@Component("build-module-path")
public class BuildModulePathStrategy extends DefaultPipelineParameterStrategy {

    @Override
    public String systemProcess(PipelineParameter param, String value) {
        // 1. 先走基类的默认值映射转换
        String handleValue = super.systemProcess(param, value);
        // 2. 路径归一化为 ./xxx 格式
        return PathUtil.toRelativePath(handleValue);
    }
}
