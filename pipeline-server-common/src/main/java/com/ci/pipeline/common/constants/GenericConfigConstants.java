package com.ci.pipeline.common.constants;

/**
 * 通用配置相关常量。
 */
public final class GenericConfigConstants {

    private GenericConfigConstants() {
    }

    /** 配置键长度上限 */
    public static final int KEY_MAX_LENGTH = 200;

    /** 备注长度上限 */
    public static final int DESCRIPTION_MAX_LENGTH = 500;

    // ---- 校验提示信息 ----
    public static final String MSG_KEY_REQUIRED = "配置键不能为空";
    public static final String MSG_KEY_TOO_LONG = "配置键长度不能超过%d个字符";
    public static final String MSG_KEY_DUPLICATED = "已存在相同配置键[%s]，请修改";
    public static final String MSG_NOT_FOUND = "配置项不存在";
    public static final String MSG_VALUE_REQUIRED = "配置值不能为空";
    public static final String MSG_FORMAT_UNSUPPORTED = "不支持的值格式[%s]，仅支持 txt / json";
    public static final String MSG_JSON_INVALID = "配置值必须是合法的JSON对象或数组";
    public static final String MSG_NO_CHANGE = "配置内容无变化";
    public static final String MSG_ID_REQUIRED = "配置ID不能为空";
}
