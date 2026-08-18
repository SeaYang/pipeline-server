package com.ci.pipeline.common.enums;

/**
 * 流水线并发超限处理策略枚举。
 * <p>对应 pipeline_template.over_limit_policy / pipeline.over_limit_policy 字段。
 */
public enum OverLimitPolicyEnum {

    /**
     * 拒绝新执行，返回明确错误信息
     */
    REJECT("Reject", "拒绝新执行", "RejectOverLimit"),

    /**
     * 终止最早一条执行中的记录，腾出额度后放行本次执行
     */
    REPLACE_OLDEST("ReplaceOldest", "替换最早执行", "ReplaceOldestOverLimit");

    private final String code;
    private final String description;
    /** 策略实现类的 Spring Bean 名（策略模式路由键，实现类需 @Component 同名显式命名） */
    private final String strategyBeanName;

    OverLimitPolicyEnum(String code, String description, String strategyBeanName) {
        this.code = code;
        this.description = description;
        this.strategyBeanName = strategyBeanName;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public String getStrategyBeanName() {
        return strategyBeanName;
    }

    /**
     * 根据编码解析为枚举。
     *
     * @param code 策略编码（大小写敏感）
     * @return 对应枚举，不存在返回 {@code null}
     */
    public static OverLimitPolicyEnum ofCode(String code) {
        if (code == null) {
            return null;
        }
        for (OverLimitPolicyEnum policy : values()) {
            if (policy.code.equals(code)) {
                return policy;
            }
        }
        return null;
    }

    /**
     * 编码是否合法
     */
    public static boolean isValidCode(String code) {
        return ofCode(code) != null;
    }
}
