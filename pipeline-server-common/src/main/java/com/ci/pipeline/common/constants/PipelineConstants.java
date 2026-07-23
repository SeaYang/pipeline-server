package com.ci.pipeline.common.constants;

/**
 * 流水线实例模块相关常量定义。
 * <p>
 * 业务类（Controller / Service）中不允许直接出现魔法值字符串，所有流水线实例模块的
 * 常量与提示信息统一在此维护。
 */
public final class PipelineConstants {

    private PipelineConstants() {
    }

    // ===== 流水线实例提示信息 =====

    /**
     * 流水线名称不能为空
     */
    public static final String MSG_PIPELINE_NAME_REQUIRED = "流水线名称不能为空";

    /**
     * appName 不能为空
     */
    public static final String MSG_APP_NAME_REQUIRED = "appName 不能为空";

    /**
     * 流水线模板编码不能为空
     */
    public static final String MSG_PIPELINE_TEMPLATE_CODE_REQUIRED = "流水线模板编码不能为空";

    /**
     * 流水线 id 不能为空
     */
    public static final String MSG_PIPELINE_ID_REQUIRED = "流水线 id 不能为空";

    /**
     * 流水线不存在
     */
    public static final String MSG_PIPELINE_NOT_EXIST = "流水线不存在";

    // ===== 关联校验提示信息 =====

    /**
     * 应用不存在（参数：appName）
     */
    public static final String MSG_APP_NOT_EXIST = "应用[%s]不存在";

    /**
     * 流水线模板不存在（参数：pipelineTemplateCode）
     */
    public static final String MSG_TEMPLATE_NOT_EXIST = "流水线模板[%s]不存在";

    // ===== 生效版本 / 参数解析 / 执行 提示信息 =====

    /**
     * 流水线模板没有生效中的版本，无法操作（参数：pipelineTemplateCode）
     */
    public static final String MSG_TEMPLATE_NO_EFFECTIVE_VERSION = "流水线模板[%s]没有生效中的版本";

    /**
     * 流水线模板详情无法解析（参数：异常信息）
     */
    public static final String MSG_TEMPLATE_DETAIL_PARSE_FAILED = "流水线模板详情无法解析：%s";

    /**
     * 流水线参数对象不能为空
     */
    public static final String MSG_PARAMETERS_REQUIRED = "流水线参数对象不能为空";

    /**
     * 执行流水线失败（参数：Argo 异常信息）
     */
    public static final String MSG_EXECUTE_FAILED = "执行流水线失败：%s";

    // ===== 流水线执行记录提示信息 =====

    /**
     * 流水线执行记录 id 不能为空
     */
    public static final String MSG_PIPELINE_RUN_ID_REQUIRED = "流水线执行记录 id 不能为空";

    /**
     * 流水线执行记录不存在
     */
    public static final String MSG_PIPELINE_RUN_NOT_EXIST = "流水线执行记录不存在";

    /**
     * 流水线执行参数序列化失败（参数：异常信息）
     */
    public static final String MSG_RUN_ARGUMENTS_SERIALIZE_FAILED = "流水线执行参数序列化失败：%s";

    // ===== 重试 / 停止 操作提示信息 =====

    /**
     * 仅失败（Failed / Error）状态的执行记录可以重试
     */
    public static final String MSG_RUN_RETRY_NOT_FAILED = "仅失败（Failed / Error）状态的执行记录可以重试";

    /**
     * Argo 流水线当前状态不允许重试（参数：Argo phase）
     */
    public static final String MSG_RUN_ARGO_RETRY_NOT_FAILED = "Argo 流水线当前状态[%s]不允许重试，仅 Failed / Error 可重试";

    /**
     * 重试执行流水线失败（参数：异常信息）
     */
    public static final String MSG_RUN_RETRY_FAILED = "重试执行流水线失败：%s";

    /**
     * 已到终态（Succeeded / Cancelled）的执行记录不可停止
     */
    public static final String MSG_RUN_STOP_ALREADY_TERMINAL = "已到终态（Succeeded / Cancelled）的执行记录不可停止";

    /**
     * Argo 流水线当前状态不允许停止（参数：Argo phase）
     */
    public static final String MSG_RUN_ARGO_STOP_NOT_RUNNING = "Argo 流水线当前状态[%s]不可停止";

    /**
     * 停止执行流水线失败（参数：异常信息）
     */
    public static final String MSG_RUN_STOP_FAILED = "停止执行流水线失败：%s";

    /**
     * 用户手动停止的失败信息文案
     */
    public static final String MSG_RUN_STOP_MESSAGE = "用户手动停止";

    /**
     * 执行记录状态已被并发修改（乐观锁冲突），请刷新后重试
     */
    public static final String MSG_RUN_STATE_CHANGED = "执行记录状态已变更，请刷新后重试";

    /**
     * 执行详情快照不存在
     */
    public static final String MSG_RUN_SNAPSHOT_NOT_EXIST = "执行详情快照不存在";
}
