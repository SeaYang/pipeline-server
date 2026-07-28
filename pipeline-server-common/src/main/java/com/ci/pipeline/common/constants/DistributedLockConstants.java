package com.ci.pipeline.common.constants;

/**
 * 分布式锁相关常量
 */
public final class DistributedLockConstants {

    private DistributedLockConstants() {
    }

    /** 锁 key 前缀：流水线运行日志同步 */
    public static final String LOCK_KEY_PIPELINE_RUN_SYNC_LOG = "pipeline:run:sync-log:";

    /** 阻塞模式默认重试间隔（毫秒） */
    public static final long DEFAULT_RETRY_INTERVAL_MS = 200L;
}
