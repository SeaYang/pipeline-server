package com.ci.pipeline.common.constants;

/**
 * 流水线参数定义相关常量。
 */
public final class PipelineParameterConstants {

    private PipelineParameterConstants() {
    }

    // ---- 参数名格式 ----
    public static final String NAME_REGEX = "^[a-z][a-z0-9-]*$";

    // ---- 校验提示信息 ----
    public static final String MSG_NAME_REQUIRED = "参数名不能为空";
    public static final String MSG_NAME_FORMAT = "参数名格式不正确，需符合 ^[a-z][a-z0-9-]*$";
    public static final String MSG_NAME_DUPLICATED = "参数名已存在：%s";
    public static final String MSG_LABEL_REQUIRED = "参数中文名称不能为空";
    public static final String MSG_PARAM_TYPE_REQUIRED = "参数类型不能为空";
    public static final String MSG_PARAM_TYPE_INVALID = "参数类型不合法：%s";
    public static final String MSG_COMPONENT_TYPE_INVALID = "组件类型不合法：%s";
    public static final String MSG_PARAM_GROUP_REQUIRED = "参数所属组别不能为空";
    public static final String MSG_ID_REQUIRED = "参数 id 不能为空";
    public static final String MSG_PARAM_NOT_EXIST = "参数定义不存在";
    public static final String MSG_OPTION_CONFIG_INVALID = "选项配置格式不正确";
    public static final String MSG_STRATEGY_CONFIG_INVALID = "默认值策略配置格式不正确";
    public static final String MSG_DEPEND_PARAMS_INVALID = "依赖参数配置格式不正确，需为 JSON 字符串数组";
    public static final String MSG_DEPEND_PARAM_NOT_EXIST = "依赖的参数不存在：%s";
    public static final String MSG_DEPEND_CIRCULAR = "参数依赖存在循环：%s";

    // ---- 参数刷新 ----
    public static final String MSG_CHANGED_PARAM_NAME_REQUIRED = "变动的参数名不能为空";

    // ---- 执行参数校验 ----
    public static final String MSG_PARAM_REQUIRED = "参数[%s]不能为空";
    public static final String MSG_PARAM_REGEX_FAIL = "参数[%s]格式不正确";
    public static final String MSG_PARAM_UNDEFINED = "流水线模板中含有未定义的参数配置，请先在参数定义页面补充：%s";
}
