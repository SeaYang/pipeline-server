package com.ci.pipeline.service.service.impl;

import com.ci.pipeline.common.constants.DistributedLockConstants;
import com.ci.pipeline.dao.entity.DistributedLock;
import com.ci.pipeline.dao.repository.DistributedLockRepository;
import com.ci.pipeline.service.service.DistributedLockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.UUID;

/**
 * 基于 DB 的分布式锁实现。
 * <p>加锁原理：先查后更，CAS 抢占。
 * <ul>
 *   <li>记录不存在 → INSERT（唯一索引保证并发安全）</li>
 *   <li>记录已过期 → UPDATE WHERE revision=? CAS 抢占</li>
 *   <li>记录未过期 → 抢锁失败</li>
 * </ul>
 */
@Slf4j
@Service
public class DistributedLockServiceImpl implements DistributedLockService {

    @Resource
    private DistributedLockRepository distributedLockRepository;

    @Override
    public String tryLock(String key, int expireSeconds, String description) {
        String lockValue = UUID.randomUUID().toString();

        // 1. 查询锁记录
        DistributedLock lock = distributedLockRepository.getByLockKey(key);

        // 2. 记录不存在 → INSERT
        if (lock == null) {
            return insertLock(key, lockValue, description, expireSeconds);
        }

        // 3. 记录存在但已过期 → UPDATE CAS 抢占
        if (lock.getExpiredTime().before(new Date())) {
            int rows = distributedLockRepository.casAcquire(
                    key, lockValue, description, expireSeconds, lock.getRevision());
            if (rows > 0) {
                return lockValue;
            }
            // CAS 失败，说明被其他线程抢先
            return null;
        }

        // 4. 记录存在且未过期 → 抢锁失败
        return null;
    }

    @Override
    public String lock(String key, int waitSeconds, int expireSeconds, String description) {
        long deadline = System.currentTimeMillis() + waitSeconds * 1000L;
        while (true) {
            String lockValue = tryLock(key, expireSeconds, description);
            if (lockValue != null) {
                return lockValue;
            }
            if (System.currentTimeMillis() >= deadline) {
                log.warn("阻塞获取锁超时: key={}, waitSeconds={}", key, waitSeconds);
                return null;
            }
            try {
                Thread.sleep(DistributedLockConstants.DEFAULT_RETRY_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("阻塞获取锁被中断: key={}", key);
                return null;
            }
        }
    }

    @Override
    public boolean unlock(String key, String lockValue) {
        int rows = distributedLockRepository.release(key, lockValue);
        if (rows == 0) {
            log.warn("释放锁失败，锁可能已过期或不属于当前持有者: key={}", key);
        }
        return rows > 0;
    }

    @Override
    public boolean renew(String key, String lockValue, int expireSeconds) {
        int rows = distributedLockRepository.renew(key, lockValue, expireSeconds);
        if (rows == 0) {
            log.warn("续期失败，锁可能已过期或不属于当前持有者: key={}", key);
        }
        return rows > 0;
    }

    /**
     * INSERT 加锁，处理唯一索引冲突。
     * <p>并发情况下两个线程同时 INSERT，唯一索引保证只有一个成功，
     * 另一个抛 {@link DuplicateKeyException}，捕获后视为抢锁失败。
     *
     * @param key           锁标识
     * @param lockValue     持有者标识
     * @param description   锁描述信息
     * @param expireSeconds 过期时间（秒）
     * @return 加锁成功返回 lockValue，失败返回 null
     */
    private String insertLock(String key, String lockValue, String description, int expireSeconds) {
        try {
            DistributedLock entity = new DistributedLock();
            entity.setLockKey(key);
            entity.setLockValue(lockValue);
            entity.setDescription(description);
            entity.setExpiredTime(new Date(System.currentTimeMillis() + expireSeconds * 1000L));
            entity.setRevision(0);
            distributedLockRepository.insert(entity);
            return lockValue;
        } catch (DuplicateKeyException e) {
            // 并发 INSERT，唯一索引冲突，抢锁失败
            return null;
        }
    }
}
