package com.ci.pipeline.service.service;

/**
 * 基于 DB 的分布式锁服务。
 * <p>通过 MySQL 唯一索引 + 乐观锁（CAS）实现，无需引入 Redis 等额外中间件。
 * <p>使用示例：
 * <pre>
 * String lockKey = "pipeline:run:sync-log:" + runId;
 * String lockValue = distributedLockService.lock(lockKey, 30, 300, "同步流水线运行日志");
 * if (lockValue == null) {
 *     log.warn("获取锁失败: key={}", lockKey);
 *     return;
 * }
 * try {
 *     // 执行业务逻辑
 * } finally {
 *     distributedLockService.unlock(lockKey, lockValue);
 * }
 * </pre>
 */
public interface DistributedLockService {

    /**
     * 非阻塞尝试获取锁（尝试一次）。
     *
     * @param key           锁标识
     * @param expireSeconds 锁过期时间（秒）
     * @param description   锁描述信息
     * @return 加锁成功返回 lockValue（UUID），失败返回 null
     */
    String tryLock(String key, int expireSeconds, String description);

    /**
     * 阻塞式获取锁（在超时时间内自旋重试）。
     *
     * @param key           锁标识
     * @param waitSeconds   最大等待时间（秒）
     * @param expireSeconds 锁过期时间（秒）
     * @param description   锁描述信息
     * @return 加锁成功返回 lockValue（UUID），超时返回 null
     */
    String lock(String key, int waitSeconds, int expireSeconds, String description);

    /**
     * 释放锁。
     * <p>校验 lockValue 防止误删别人的锁。
     *
     * @param key       锁标识
     * @param lockValue 持有者标识（tryLock/lock 的返回值）
     * @return 释放成功返回 true，锁不存在或不属于当前持有者返回 false
     */
    boolean unlock(String key, String lockValue);

    /**
     * 续期（延长锁的过期时间）。
     * <p>只能续未过期的锁，已过期的锁需重新加锁。
     *
     * @param key           锁标识
     * @param lockValue     持有者标识
     * @param expireSeconds 新的过期时间（秒），从当前时刻起算
     * @return 续期成功返回 true，锁不存在/已过期/不属于当前持有者返回 false
     */
    boolean renew(String key, String lockValue, int expireSeconds);
}
