package com.ci.pipeline.common.constants;

/**
 * 分布式锁相关常量
 */
public final class DistributedLockConstants {

    private DistributedLockConstants() {
    }

    /** 锁 key 前缀：流水线运行日志同步 */
    public static final String LOCK_KEY_PIPELINE_RUN_SYNC_LOG = "pipeline:run:sync-log:";

    /** 锁 key 前缀：任务模板操作（新增/编辑/发布） */
    public static final String LOCK_KEY_TASK_TEMPLATE = "lock:task-template:";

    /** 锁 key 前缀：流水线模板操作（新增/编辑/发布） */
    public static final String LOCK_KEY_PIPELINE_TEMPLATE = "lock:pipeline-template:";

    /** 锁 key 前缀：流水线参数定义操作（新增/编辑） */
    public static final String LOCK_KEY_PIPELINE_PARAMETER = "lock:pipeline-parameter:";

    /** 锁 key 前缀：流水线运行操作（同步/重试/停止） */
    public static final String LOCK_KEY_PIPELINE_RUN = "lock:pipeline-run:";

    /** 非阻塞加锁默认过期时间（秒） */
    public static final int DEFAULT_LOCK_EXPIRE_SECONDS = 30;

    /** 阻塞模式默认重试间隔（毫秒） */
    public static final long DEFAULT_RETRY_INTERVAL_MS = 200L;
}
