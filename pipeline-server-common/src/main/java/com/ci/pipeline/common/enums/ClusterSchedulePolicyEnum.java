package com.ci.pipeline.common.enums;

/**
 * 集群调度策略枚举。
 * <p>对应 pipeline_template.cluster_schedule_policy 字段，决定执行流水线时如何从候选集群中选择执行集群：
 * <ul>
 *   <li>{@link #ANY}：任意集群，忽略模板上配置的候选集群，在全部可用集群中选择；</li>
 *   <li>{@link #PREFER_SELECTED}：优先选中集群，优先在模板配置的候选集群中选择，不可用时兜底其他可用集群。</li>
 * </ul>
 */
public enum ClusterSchedulePolicyEnum {

    /**
     * 任意集群
     */
    ANY("Any", "任意集群"),

    /**
     * 优先选中集群
     */
    PREFER_SELECTED("PreferSelected", "优先选中集群");

    /**
     * 策略编码，存 DB，同时与策略 Bean 名映射（code + "ClusterSchedule"）
     */
    private final String code;

    /**
     * 中文描述
     */
    private final String description;

    ClusterSchedulePolicyEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 策略对应的 Spring Bean 名（调度策略实现类 @Component 注解的名称）。
     *
     * @return Bean 名，如 "AnyClusterSchedule"
     */
    public String getStrategyBeanName() {
        return code + "ClusterSchedule";
    }

    /**
     * 根据编码解析为枚举。
     *
     * @param code 策略编码（大小写敏感）
     * @return 对应枚举，不存在返回 {@code null}
     */
    public static ClusterSchedulePolicyEnum ofCode(String code) {
        if (code == null) {
            return null;
        }
        for (ClusterSchedulePolicyEnum policy : values()) {
            if (policy.code.equals(code)) {
                return policy;
            }
        }
        return null;
    }

    /**
     * 编码是否合法。
     *
     * @param code 策略编码
     * @return 合法返回 true
     */
    public static boolean isValidCode(String code) {
        return ofCode(code) != null;
    }
}
