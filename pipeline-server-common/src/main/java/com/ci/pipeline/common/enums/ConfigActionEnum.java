package com.ci.pipeline.common.enums;

/**
 * 通用配置操作类型枚举。
 * <p>对应 generic_config_history.action 字段。
 */
public enum ConfigActionEnum {

    CREATE("CREATE", "新建"),
    UPDATE("UPDATE", "修改"),
    DELETE("DELETE", "删除");

    private final String code;
    private final String description;

    ConfigActionEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ConfigActionEnum ofCode(String code) {
        if (code == null) {
            return null;
        }
        for (ConfigActionEnum action : values()) {
            if (action.code.equals(code)) {
                return action;
            }
        }
        return null;
    }
}
