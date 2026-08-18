package com.ci.pipeline.service.concurrency;

import com.ci.pipeline.common.enums.OverLimitPolicyEnum;
import com.ci.pipeline.service.concurrency.policy.OverLimitPolicyStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 超限策略分发器。
 * <p>按 Spring Bean 名注入全部 {@link OverLimitPolicyStrategy} 实现，
 * 通过 {@link OverLimitPolicyEnum#getStrategyBeanName()} 路由。
 * 新增策略 = 加枚举项 + 同名 @Component 实现类，本类零改动（开闭原则）。
 */
@Component
public class OverLimitPolicyStrategyManager {

    @Autowired
    private Map<String, OverLimitPolicyStrategy> strategyMap;

    /**
     * 按策略编码取实现。
     *
     * @param policyCode 策略编码（Reject / ReplaceOldest），空或非法时兜底 Reject
     * @return 对应策略实现
     */
    public OverLimitPolicyStrategy getStrategy(String policyCode) {
        OverLimitPolicyEnum policy = OverLimitPolicyEnum.ofCode(policyCode);
        if (policy == null) {
            policy = OverLimitPolicyEnum.REJECT;
        }
        OverLimitPolicyStrategy strategy = strategyMap.get(policy.getStrategyBeanName());
        if (strategy == null) {
            throw new IllegalArgumentException(
                    "未找到超限策略实现: " + policy.getStrategyBeanName());
        }
        return strategy;
    }
}
