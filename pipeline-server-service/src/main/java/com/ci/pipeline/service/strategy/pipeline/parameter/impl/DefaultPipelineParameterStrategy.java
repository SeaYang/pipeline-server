package com.ci.pipeline.service.strategy.pipeline.parameter.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ci.pipeline.dao.entity.PipelineParameter;
import com.ci.pipeline.service.strategy.defaultvalue.DefaultValueStrategyManager;
import com.ci.pipeline.service.strategy.pipeline.parameter.PipelineParameterStrategy;
import com.ci.pipeline.service.strategy.ParamResolveContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 流水线参数策略默认实现（基类）。
 * <p>所有按参数名注册的具体策略类都应继承本类，按需重写 {@link #buildParameter}，
 * 通过 {@code super.buildParameter()} 复用基类的策略链逻辑后做后置增强。
 *
 * <h3>基类策略链</h3>
 * <p>不区分参数类型（system / user），统一走默认值策略链：
 * <ol>
 *     <li>{@link DefaultValueStrategyManager#resolve} 按 priority 降序遍历策略链，取第一个非 null；</li>
 *     <li>全 null 用 {@code param.getDefaultValue()} 兜底。</li>
 * </ol>
 * <p>系统参数（如 app-name、git-url 等）有自己的策略类，重写 {@link #buildParameter} 直接返回上下文值，
 * 不会走到基类的策略链。
 *
 * <h3>扩展方式</h3>
 * <pre>{@code
 * @Component("git-branch")
 * public class GitBranchStrategy extends DefaultPipelineParameterStrategy {
 *     @Override
 *     public String buildParameter(PipelineParameter param, ParamResolveContext context) {
 *         // 1. 先走基类策略链获取基础值
 *         String value = super.buildParameter(param, context);
 *         // 2. 后置增强：从 GitLab 获取分支列表填充选项等
 *         // ...
 *         return value;
 *     }
 * }
 * }</pre>
 */
@Slf4j
@Component("DefaultPipelineParameterStrategy")
public class DefaultPipelineParameterStrategy implements PipelineParameterStrategy {

    @Autowired
    protected DefaultValueStrategyManager defaultValueStrategyManager;

    @Override
    public String buildParameter(PipelineParameter param, ParamResolveContext context) {
        // 走默认值策略链（DefaultValueStrategyManager），全 null 用 defaultValue 兜底
        String strategyValue = defaultValueStrategyManager.resolve(
                param.getName(), param.getDefaultValueStrategyConfig(), context);
        return strategyValue != null ? strategyValue : param.getDefaultValue();
    }

    /**
     * 默认系统内部处理：值映射转换。
     * <p>当 {@code needSystemProcess = true} 时，在 {@code optionConfig} 中查找 value 对应的 realValue。
     * 找不到匹配项或 realValue 为空时，原值返回（不做转换）。
     */
    @Override
    public String systemProcess(PipelineParameter param, String value) {
        if (value == null) {
            return null;
        }
        if (!Boolean.TRUE.equals(param.getNeedSystemProcess())) {
            return value;
        }
        if (!StringUtils.hasText(param.getOptionConfig())) {
            return value;
        }
        try {
            JSONArray options = JSON.parseArray(param.getOptionConfig());
            for (int i = 0; i < options.size(); i++) {
                JSONObject opt = options.getJSONObject(i);
                String optValue = opt.getString("value");
                if (value.equals(optValue)) {
                    String realValue = opt.getString("realValue");
                    return StringUtils.hasText(realValue) ? realValue : value;
                }
            }
        } catch (Exception e) {
            log.warn("值映射转换解析 optionConfig 失败, paramName={}, optionConfig={}",
                    param.getName(), param.getOptionConfig(), e);
        }
        return value;
    }
}
