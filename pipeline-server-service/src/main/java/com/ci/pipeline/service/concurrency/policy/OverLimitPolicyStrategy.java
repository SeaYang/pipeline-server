package com.ci.pipeline.service.concurrency.policy;

import com.ci.pipeline.dao.entity.Pipeline;
import com.ci.pipeline.dao.entity.PipelineRun;
import com.ci.pipeline.dao.entity.PipelineTemplate;

import java.util.List;

/**
 * 超限处理策略接口（策略模式）。
 * <p>新增策略 = 加 {@link com.ci.pipeline.common.enums.OverLimitPolicyEnum} 枚举项
 * + 本接口的实现类（{@code @Component("XxxOverLimit")} 显式命名，bean 名与枚举的 strategyBeanName 一致），
 * 检查编排 {@link com.ci.pipeline.service.concurrency.PipelineConcurrencyChecker} 零改动（开闭原则）。
 * <p>判断与动作分离：{@link #isBlock} 只读判断是否阻断（runPrecheck 语义），
 * {@link #beforeExecute} 在超限时执行有副作用的动作（如终止旧执行腾位，beforeExec 语义）。
 */
public interface OverLimitPolicyStrategy {

    /**
     * 超限时是否阻断本次执行（只读判断，无副作用）。
     *
     * @param pipeline    当前流水线
     * @param template    流水线模板（提供 appMaxRunningLimit 等）
     * @param limit       本层生效的并发上限
     * @param occupying   当前层统计口径下占用额度的执行数
     * @param ownOccupying 本流水线自身占用额度的执行数（ReplaceOldest 判断能否腾位用）
     * @return true = 抛业务异常拒绝新执行
     */
    boolean isBlock(Pipeline pipeline, PipelineTemplate template, int limit,
                    long occupying, long ownOccupying);

    /**
     * 提交前动作（有副作用）：超限且不阻断时执行腾位等动作（如终止本流水线最早执行）。
     * <p>仅在 {@link #isBlock} 返回 false 且 occupying ≥ limit 时被调用。
     *
     * @param pipeline  当前流水线
     * @param ownRuns   本流水线占用额度的执行列表（按 id 升序，最早在前；供终止选择）
     * @return true = 已执行替换/腾位（调用方据此跳过下一层检查）
     */
    boolean beforeExecute(Pipeline pipeline, List<PipelineRun> ownRuns);
}
