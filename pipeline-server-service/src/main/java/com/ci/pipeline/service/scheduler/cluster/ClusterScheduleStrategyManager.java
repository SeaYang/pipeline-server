package com.ci.pipeline.service.scheduler.cluster;

import com.ci.pipeline.common.constants.ClusterConstants;
import com.ci.pipeline.common.enums.ClusterSchedulePolicyEnum;
import com.ci.pipeline.common.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 集群调度策略管理器：Spring 注入 Map&lt;beanName, bean&gt;，配合枚举 code → Bean 名映射零 if-else 路由。
 */
@Component
public class ClusterScheduleStrategyManager {

    @Autowired
    private Map<String, ClusterScheduler> strategyMap;

    /**
     * 按调度策略编码获取策略实例。
     *
     * @param policy 策略编码（Any / PreferSelected），空时兜底 Any
     */
    public ClusterScheduler getStrategy(String policy) {
        String code = StringUtils.isNotBlank(policy) ? policy : ClusterConstants.DEFAULT_SCHEDULE_POLICY;
        ClusterSchedulePolicyEnum policyEnum = ClusterSchedulePolicyEnum.ofCode(code);
        if (policyEnum == null) {
            throw new BusinessException(String.format("不支持的集群调度策略, policy=%s", policy));
        }
        ClusterScheduler strategy = strategyMap.get(policyEnum.getStrategyBeanName());
        if (strategy == null) {
            throw new BusinessException(String.format("集群调度策略实现未注册, policy=%s", policy));
        }
        return strategy;
    }
}
