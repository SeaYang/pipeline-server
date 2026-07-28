package com.ci.pipeline.dao.repository;

import com.ci.pipeline.dao.entity.DistributedLock;
import com.ci.pipeline.dao.mapper.DistributedLockMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * 分布式锁 Repository。
 * <p>薄封装层，转发 Mapper 调用。
 */
@Repository
public class DistributedLockRepository {

    @Autowired
    private DistributedLockMapper distributedLockMapper;

    /**
     * 按 lock_key 查询锁记录。
     *
     * @param lockKey 锁标识
     * @return 锁记录，不存在返回 null
     */
    public DistributedLock getByLockKey(String lockKey) {
        return distributedLockMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DistributedLock>()
                        .eq(DistributedLock::getLockKey, lockKey));
    }

    /**
     * 插入锁记录。
     *
     * @param entity 锁实体
     * @return 受影响行数
     */
    public int insert(DistributedLock entity) {
        return distributedLockMapper.insert(entity);
    }

    /**
     * 抢占已过期的锁（CAS）。
     *
     * @param key            锁标识
     * @param newLockValue   新的持有者标识
     * @param description    锁描述信息
     * @param expireSeconds  过期时间（秒）
     * @param oldRevision    旧版本号
     * @return 受影响行数
     */
    public int casAcquire(String key, String newLockValue, String description,
                          int expireSeconds, Integer oldRevision) {
        return distributedLockMapper.casAcquire(key, newLockValue, description, expireSeconds, oldRevision);
    }

    /**
     * 释放锁。
     *
     * @param key       锁标识
     * @param lockValue 持有者标识
     * @return 受影响行数
     */
    public int release(String key, String lockValue) {
        return distributedLockMapper.release(key, lockValue);
    }

    /**
     * 续期。
     *
     * @param key            锁标识
     * @param lockValue      持有者标识
     * @param expireSeconds  新的过期时间（秒）
     * @return 受影响行数
     */
    public int renew(String key, String lockValue, int expireSeconds) {
        return distributedLockMapper.renew(key, lockValue, expireSeconds);
    }
}
