package com.ci.pipeline.service.strategy.pipeline.parameter;

import com.ci.pipeline.dao.entity.PipelineParameter;
import com.ci.pipeline.service.strategy.ParamResolveContext;

/**
 * 流水线参数策略接口（策略模式）。
 * <p>每个参数可以有一个对应的策略实现类，通过 {@code @Component("参数名")} 注册，
 * 由 {@link PipelineParameterStrategyManager} 按参数名路由，找不到则回退到
 * {@link com.ci.pipeline.service.strategy.pipeline.parameter.impl.DefaultPipelineParameterStrategy}。
 *
 * <h3>设计说明</h3>
 * <ul>
 *     <li>子类通常继承 {@code DefaultPipelineParameterStrategy}，按需重写 {@link #buildParameter}，
 *         通过 {@code super.buildParameter()} 复用基类的策略链逻辑后做后置增强；</li>
 *     <li>也可以完全重写 {@link #buildParameter} 实现自定义取值逻辑。</li>
 * </ul>
 */
public interface PipelineParameterStrategy {

    /**
     * 构建参数值（计算初始值或从外部数据源获取）。
     * <p>基类默认实现走 DefaultValueStrategyManager 策略链，全 null 用 defaultValue 兜底。
     * 系统参数（如 app-name、git-url）有自己的策略类，重写本方法直接返回上下文值。
     *
     * @param param   参数定义实体
     * @param context 参数计算上下文（含 pipelineId、appName、已解析的参数值等）
     * @return 计算后的参数值，返回 null 表示无法计算（由调用方决定兜底逻辑）
     */
    String buildParameter(PipelineParameter param, ParamResolveContext context);

    /**
     * 系统内部处理：对参数值做后置转换（值映射、路径归一化等）。
     * <p>默认实现（{@code DefaultPipelineParameterStrategy.systemProcess}）处理通用的值映射转换：
     * 当 {@code needSystemProcess = true} 时，在 {@code optionConfig} 中查找 value 对应的 realValue。
     * <p>个别参数（如 build-context-path、build-module-path）可重写本方法，
     * 先通过 {@code super.systemProcess()} 走默认映射，再做自定义处理（如路径归一化）。
     *
     * @param param     参数定义实体
     * @param value     待处理的参数值（映射前）
     * @return 处理后的参数值
     */
    default String systemProcess(PipelineParameter param, String value) {
        return value;
    }
}
