package com.ci.pipeline.common.constants;

/**
 * 流水线触发历史相关常量
 */
public final class PipelineTriggerHistoryConstants {

    private PipelineTriggerHistoryConstants() {}

    /** 手动触发类型 */
    public static final String TRIGGER_TYPE_USER = "user";

    /** 触发状态 - 成功 */
    public static final String STATUS_SUCCESS = "SUCCESS";

    /** 触发状态 - 失败 */
    public static final String STATUS_FAILED = "FAILED";

    /** 手动触发的 pipelineEventBindId 固定值 */
    public static final long MANUAL_TRIGGER_ID = 0L;

    /** 提示信息 */
    public static final String MSG_TRIGGER_HISTORY_NOT_EXIST = "触发历史记录不存在";
    public static final String MSG_TRIGGER_HISTORY_QUERY_PARAM_REQUIRED =
            "pipelineId 和 appName 至少传一个";
}
