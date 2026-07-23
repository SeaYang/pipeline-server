package com.ci.pipeline.common.enums;

/**
 * 流水线参数类型枚举。
 * <p>对应 pipeline_parameter.param_type 字段。
 * <ul>
 *     <li>{@link #SYSTEM} — 系统参数，用户不可见，由平台从流水线上下文自动填充；</li>
 *     <li>{@link #USER} — 用户参数，用户可见，需在执行弹框中填写或选择。</li>
 * </ul>
 */
public enum ParamTypeEnum {

    SYSTEM("system", "系统参数"),
    USER("user", "用户参数");

    private final String code;
    private final String description;

    ParamTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ParamTypeEnum ofCode(String code) {
        if (code == null) {
            return null;
        }
        for (ParamTypeEnum type : values()) {
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
