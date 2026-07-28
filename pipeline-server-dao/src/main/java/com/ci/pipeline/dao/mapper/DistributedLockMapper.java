package com.ci.pipeline.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ci.pipeline.dao.entity.DistributedLock;
import org.apache.ibatis.annotations.Param;

/**
 * 分布式锁 Mapper。
 * <p>CAS 抢占、释放、续期操作在 XML 中手写 SQL，
 * 不依赖 MyBatis-Plus 的 {@code @Version} 自动乐观锁机制。
 */
public interface DistributedLockMapper extends BaseMapper<DistributedLock> {

    /**
     * 抢占已过期的锁（CAS）。
     * <p>通过 WHERE revision = #{oldRevision} 保证并发安全，
     * 只有一个线程能更新成功。
     *
     * @param key            锁标识
     * @param newLockValue   新的持有者标识（UUID）
     * @param description    锁描述信息
     * @param expireSeconds  过期时间（秒）
     * @param oldRevision    查询时获取的旧版本号
     * @return 受影响行数，1 表示抢占成功，0 表示失败
     */
    int casAcquire(@Param("key") String key,
                   @Param("newLockValue") String newLockValue,
                   @Param("description") String description,
                   @Param("expireSeconds") int expireSeconds,
                   @Param("oldRevision") Integer oldRevision);

    /**
     * 释放锁（校验 lockValue 防误删）。
     * <p>将 expired_time 置为当前时间，使锁立即可被其他线程抢占。
     *
     * @param key       锁标识
     * @param lockValue 持有者标识
     * @return 受影响行数，1 表示释放成功，0 表示锁不存在或不属于当前持有者
     */
    int release(@Param("key") String key,
                @Param("lockValue") String lockValue);

    /**
     * 续期（延长锁的过期时间）。
     * <p>校验 lockValue 和未过期状态，已过期的锁不能续期。
     *
     * @param key            锁标识
     * @param lockValue      持有者标识
     * @param expireSeconds  新的过期时间（秒），从当前时刻起算
     * @return 受影响行数，1 表示续期成功，0 表示锁不存在/已过期/不属于当前持有者
     */
    int renew(@Param("key") String key,
              @Param("lockValue") String lockValue,
              @Param("expireSeconds") int expireSeconds);
}
