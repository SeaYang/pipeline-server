package com.ci.pipeline.common.constants;

/**
 * 流水线并发控制相关常量。
 * <p>三层并发控制（L1 全局 / L2 应用×模板 / L3 流水线）的兜底默认值与提示信息统一在此维护。
 */
public final class PipelineConcurrencyConstants {

    private PipelineConcurrencyConstants() {
    }

    // ===== 兜底默认值（DDL 已有默认值，此处仅用于代码可读性与运行时防御） =====

    /**
     * 未配置时的并发上限兜底值：1（不允许并发）
     */
    public static final int DEFAULT_MAX_RUNNING_LIMIT = 1;

    /**
     * 未配置时的超限策略兜底值：Reject（拒绝新执行）
     */
    public static final String DEFAULT_OVER_LIMIT_POLICY = "Reject";

    /**
     * 应用维度并发上限允许配置的最大值
     */
    public static final int MAX_RUNNING_LIMIT_UPPER_BOUND = 1000;

    /**
     * 全局最大并发数的 generic_config 配置键
     */
    public static final String CONFIG_KEY_MAX_RUNNING_LIMIT = "pipeline-max-running-limit";

    // ===== fail_type 扩展值 =====

    /**
     * 执行被新执行替换（ReplaceOldest 策略终止）的 fail_type
     */
    public static final String FAIL_TYPE_REPLACED_BY_NEW = "ReplacedByNew";

    // ===== 提示信息 =====

    /**
     * 平台执行数已达上限（参数：全局上限）
     */
    public static final String MSG_GLOBAL_LIMIT_EXCEEDED =
            "平台执行数已达上限（%d），请稍后重试";

    /**
     * 应用×模板维度执行数已达上限（参数：appName、模板编码、上限）
     */
    public static final String MSG_APP_TEMPLATE_LIMIT_EXCEEDED =
            "应用[%s]使用模板[%s]的执行数已达上限（%d），请等待执行完成或停止正在执行的流水线";

    /**
     * 流水线维度执行数已达上限（参数：流水线名称、上限）
     */
    public static final String MSG_PIPELINE_LIMIT_EXCEEDED =
            "流水线[%s]执行数已达上限（%d），请等待执行完成或停止正在执行的流水线";

    /**
     * 额度被同应用下其他流水线占用，无法替换（ReplaceOldest 不允许终止他人执行）
     */
    public static final String MSG_QUOTA_OCCUPIED_BY_OTHERS =
            "并发额度被同应用下其他流水线占用，无法替换最早执行，请稍后重试或停止相关流水线";

    /**
     * 应用维度并发上限非法（参数：上限值）
     */
    public static final String MSG_APP_LIMIT_INVALID =
            "应用维度最大并发执行数必须为 1~1000 的整数，当前值：%s";

    /**
     * 流水线维度并发上限非法（参数：上限值）
     */
    public static final String MSG_PIPELINE_LIMIT_INVALID =
            "流水线最大并发执行数必须为大于等于 1 的整数，当前值：%s";

    /**
     * 超限策略非法（参数：策略值）
     */
    public static final String MSG_OVER_LIMIT_POLICY_INVALID =
            "超限策略仅支持 Reject / ReplaceOldest，当前值：%s";

    /**
     * 被新执行替换的失败信息文案
     */
    public static final String MSG_RUN_REPLACED_MESSAGE = "已被新执行替换（并发超限策略 ReplaceOldest）";
}
