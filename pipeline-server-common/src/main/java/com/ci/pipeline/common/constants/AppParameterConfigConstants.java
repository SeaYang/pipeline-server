package com.ci.pipeline.common.constants;

/**
 * 应用参数配置模块相关常量定义。
 */
public final class AppParameterConfigConstants {

    private AppParameterConfigConstants() {
    }

    // ===== 提示信息 =====

    public static final String MSG_APP_NAME_REQUIRED = "应用名称不能为空";
    public static final String MSG_PARAMETER_NAME_REQUIRED = "参数名不能为空";
    public static final String MSG_VALUE_REQUIRED = "参数值不能为空";
    public static final String MSG_ENV_REQUIRED = "环境不能为空";
    public static final String MSG_ID_REQUIRED = "id 不能为空";
    public static final String MSG_NOT_EXIST = "应用参数配置不存在";
    public static final String MSG_DUPLICATED = "参数配置已存在，应用：%s，参数：%s，环境：%s";
    public static final String MSG_BATCH_ITEMS_EMPTY = "批量新增参数列表不能为空";
    public static final String MSG_BATCH_DUPLICATE_PARAM = "批量新增中存在重复参数名：%s";

    /** 默认环境标识 */
    public static final String DEFAULT_ENV = "default";

    /** env 参数名 */
    public static final String PARAM_NAME_ENV = "env";
}
