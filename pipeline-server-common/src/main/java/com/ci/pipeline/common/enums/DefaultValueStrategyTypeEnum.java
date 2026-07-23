package com.ci.pipeline.common.enums;

/**
 * 默认值策略类型枚举。
 * <p>对应 pipeline_parameter.default_value_strategy_config 中每个策略项的 strategyType 字段。
 * <ul>
 *     <li>{@link #APP_CONFIG} — 从应用配置读取（本期空实现，预留扩展）；</li>
 *     <li>{@link #LAST_SUCCESSFUL_RUN} — 从最近一次执行成功的记录读取参数值。</li>
 * </ul>
 */
public enum DefaultValueStrategyTypeEnum {

    APP_CONFIG("AppConfig", "应用配置"),
    LAST_SUCCESSFUL_RUN("LastSuccessfulRun", "最近成功记录");

    private final String code;
    private final String description;

    DefaultValueStrategyTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static DefaultValueStrategyTypeEnum ofCode(String code) {
        if (code == null) {
            return null;
        }
        for (DefaultValueStrategyTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }

    public static boolean isValidCode(String code) {
        return ofCode(code) != null;
    }
}
