package com.ci.pipeline.common.enums;

/**
 * 通用配置值格式枚举。
 * <p>对应 generic_config.value_format 字段。
 * <ul>
 *     <li>{@link #TXT} — 纯文本；</li>
 *     <li>{@link #JSON} — JSON 对象或数组。</li>
 * </ul>
 */
public enum ConfigValueFormatEnum {

    TXT("txt", "纯文本"),
    JSON("json", "JSON");

    private final String code;
    private final String description;

    ConfigValueFormatEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ConfigValueFormatEnum ofCode(String code) {
        if (code == null) {
            return null;
        }
        for (ConfigValueFormatEnum format : values()) {
            if (format.code.equals(code)) {
                return format;
            }
        }
        return null;
    }

    public static boolean isValidCode(String code) {
        return ofCode(code) != null;
    }
}
