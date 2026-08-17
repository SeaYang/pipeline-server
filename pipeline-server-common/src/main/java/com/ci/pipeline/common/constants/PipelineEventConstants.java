package com.ci.pipeline.common.constants;

/**
 * 事件触发模块相关常量定义。
 * <p>
 * 业务类（Controller / Service / Strategy）中不允许直接出现魔法值字符串，
 * 所有事件触发模块的常量与提示信息统一在此维护。
 */
public final class PipelineEventConstants {

    private PipelineEventConstants() {
    }

    // ===== 字典类型 =====

    /**
     * 事件类型字典类型编码
     */
    public static final String DICT_TYPE_PIPELINE_EVENT_TYPE = "pipeline-event-type";

    // ===== 事件类型编码 =====

    /**
     * 效能平台提测事件
     */
    public static final String EVENT_TYPE_EP_TEST_APPLY = "epTestApply";

    /**
     * 系统标识（事件触发接口无认证时，creator 字段兜底值）
     */
    public static final String EVENT_SYSTEM = "system";

    // ===== 参数 key =====

    /**
     * 应用名称参数 key
     */
    public static final String PARAM_KEY_APP_NAME = "app-name";

    /**
     * Git 分支参数 key
     */
    public static final String PARAM_KEY_GIT_BRANCH = "git-branch";

    // ===== 提示信息 =====

    /**
     * 不支持的事件类型（参数：eventType）
     */
    public static final String MSG_EVENT_TYPE_NOT_SUPPORTED = "不支持的事件类型: %s";

    /**
     * paramList 不能为空
     */
    public static final String MSG_PARAM_LIST_REQUIRED = "paramList不能为空";

    /**
     * app-name 不能为空
     */
    public static final String MSG_APP_NAME_PARAM_REQUIRED = "app-name不能为空";

    /**
     * git-branch 不能为空
     */
    public static final String MSG_GIT_BRANCH_REQUIRED = "git-branch不能为空";

    /**
     * 事件类型不能为空
     */
    public static final String MSG_EVENT_TYPE_REQUIRED = "事件类型不能为空";

    /**
     * 流水线模板编码不能为空
     */
    public static final String MSG_TEMPLATE_CODE_REQUIRED = "流水线模板编码不能为空";

    /**
     * 事件-模板绑定记录不存在
     */
    public static final String MSG_EVENT_BIND_NOT_EXIST = "事件-模板绑定记录不存在";

    /**
     * 事件类型在字典中不存在或未启用（参数：eventType）
     */
    public static final String MSG_EVENT_TYPE_NOT_IN_DICT = "事件类型[%s]在字典中不存在或未启用";

    /**
     * 事件-模板绑定已存在（参数：eventType、pipelineTemplateCode）
     */
    public static final String MSG_EVENT_BIND_DUPLICATED = "事件类型[%s]与模板[%s]的绑定已存在";

    /**
     * 事件类型未配置模板绑定（参数：eventType）
     */
    public static final String MSG_EVENT_NO_TEMPLATE_BIND = "事件类型[%s]未配置模板绑定";

    /**
     * 应用不存在（参数：appName）
     */
    public static final String MSG_APP_NOT_EXIST = "应用[%s]不存在";

    /**
     * 应用的编程语言未找到匹配的模板（参数：appName、programmingLanguage）
     */
    public static final String MSG_NO_MATCHED_TEMPLATE = "应用[%s]的编程语言[%s]未找到匹配的模板";

    /**
     * 模板没有生效版本（参数：pipelineTemplateCode）
     */
    public static final String MSG_TEMPLATE_NO_EFFECTIVE_VERSION = "模板[%s]没有生效版本";
}
