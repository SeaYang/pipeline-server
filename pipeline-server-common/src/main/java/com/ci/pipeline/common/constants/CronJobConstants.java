package com.ci.pipeline.common.constants;

/**
 * 定时任务相关常量。
 */
public final class CronJobConstants {

    private CronJobConstants() {
    }

    // ---- cron_job_log.status 状态常量 ----
    public static final String STATUS_RUNNING = "running";
    public static final String STATUS_SUCCEEDED = "succeeded";
    public static final String STATUS_FAILED = "failed";

    // ---- 字段长度上限 ----
    public static final int NAME_MAX_LENGTH = 100;
    public static final int BEAN_NAME_MAX_LENGTH = 200;
    public static final int METHOD_NAME_MAX_LENGTH = 100;
    public static final int METHOD_PARAMS_MAX_LENGTH = 500;
    public static final int CRON_EXPR_MAX_LENGTH = 128;

    // ---- 校验提示信息 ----
    public static final String MSG_NAME_REQUIRED = "任务名称不能为空";
    public static final String MSG_NAME_TOO_LONG = "任务名称长度不能超过%d个字符";
    public static final String MSG_BEAN_NAME_REQUIRED = "Bean名称不能为空";
    public static final String MSG_BEAN_NAME_TOO_LONG = "Bean名称长度不能超过%d个字符";
    public static final String MSG_METHOD_NAME_REQUIRED = "方法名称不能为空";
    public static final String MSG_METHOD_NAME_TOO_LONG = "方法名称长度不能超过%d个字符";
    public static final String MSG_METHOD_PARAMS_TOO_LONG = "方法参数长度不能超过%d个字符";
    public static final String MSG_CRON_EXPR_REQUIRED = "CRON表达式不能为空";
    public static final String MSG_CRON_EXPR_TOO_LONG = "CRON表达式长度不能超过%d个字符";
    public static final String MSG_CRON_EXPR_INVALID = "无效的CRON表达式[%s]";
    public static final String MSG_MISFIRE_POLICY_INVALID = "无效的错过执行策略[%s]";
    public static final String MSG_BEAN_METHOD_INVALID = "%s";
    public static final String MSG_JOB_NOT_FOUND = "定时任务不存在";
    public static final String MSG_LOG_NOT_FOUND = "执行日志不存在";
    public static final String MSG_ID_REQUIRED = "任务ID不能为空";
}
