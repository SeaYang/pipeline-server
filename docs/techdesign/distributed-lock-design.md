# 基于 DB 的分布式锁 - 技术设计方案

## 一、背景与目标

### 1.1 现状

pipeline-server 在部分业务场景下存在并发问题，需要通过分布式锁来保证操作的互斥性。典型场景包括：

- 流水线执行结束后同步 Pod 日志到 DB（同一流水线可能并发触发多次）
- 新增某些类型资源时防止重复创建
- 其他需要对共享资源做互斥控制的场景

业界通常使用 Redis（如 Redisson）来实现分布式锁，但引入 Redis 会增加基础设施的部署和运维成本。本项目为了保持架构简单，决定基于现有 MySQL 实现一个轻量级的分布式锁。

### 1.2 目标

基于数据库实现一个分布式锁模块，满足以下诉求：

1. **互斥性**：同一把锁在同一时刻只能被一个持有者获取。
2. **防死锁**：锁设置过期时间，持有者宕机后锁能自动释放，避免死锁。
3. **防误删**：释放锁时校验持有者标识，不会误删别人的锁。
4. **非阻塞与阻塞**：同时支持 `tryLock`（尝试一次）和 `lock(waitTime)`（超时内自旋等待）两种获取方式。
5. **手动续期**：提供 `renew` 方法，业务可在锁快过期时主动延长。
6. **零外部依赖**：仅依赖现有 MySQL，不引入额外中间件。

### 1.3 非目标

- **不做可重入**：同一持有者重复获取同一把锁会失败，保持实现简单。
- **不做自动续期（watchdog）**：不启动后台线程自动续期，由业务自行保证在过期时间内完成操作，必要时手动调用 `renew`。
- **不做注解 AOP**：仅提供编程式 Service，调用方手动控制加锁/释放，逻辑透明可控。

---

## 二、整体架构

### 2.1 分层职责

```
┌─────────────────────────────────────────────────────┐
│  业务层 (pipeline-server-service)                     │
│  各业务 Service（如 PipelineRunSyncService 等）         │
│  调用 DistributedLockService.tryLock / lock / unlock  │
├─────────────────────────────────────────────────────┤
│  分布式锁服务                                           │
│  DistributedLockService（接口）                        │
│    ├── tryLock(key, expireSeconds, description)       │
│    ├── lock(key, waitSeconds, expireSeconds, desc)    │
│    ├── unlock(key, lockValue)                         │
│    └── renew(key, lockValue, expireSeconds)           │
│  DistributedLockServiceImpl（实现）                    │
│    ├── 基于 UPDATE CAS 乐观锁实现抢占                   │
│    └── 阻塞模式内部自旋 + Thread.sleep                  │
├─────────────────────────────────────────────────────┤
│  数据层                                                │
│    distributed_lock（锁记录表）                         │
└─────────────────────────────────────────────────────┘
```

### 2.2 模块归属

| 层 | 模块 | 包路径 |
|----|------|--------|
| Service 接口 + 实现 | pipeline-server-service | `com.ci.pipeline.service.service` / `service.impl` |
| Entity | pipeline-server-dao | `com.ci.pipeline.dao.entity` |
| Mapper | pipeline-server-dao | `com.ci.pipeline.dao.mapper` |
| Mapper XML | pipeline-server-dao | `resources/mapper/DistributedLockMapper.xml` |
| Constants | pipeline-server-common | `com.ci.pipeline.common.constants` |

---

## 三、数据模型设计

### 3.1 锁记录表 `distributed_lock`

```sql
CREATE TABLE `distributed_lock` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `lock_key` varchar(128) NOT NULL COMMENT '锁唯一标识，业务语义化命名',
  `lock_value` varchar(64) NOT NULL COMMENT '持有者标识（UUID），用于校验锁的归属，防止误删',
  `description` varchar(256) DEFAULT NULL COMMENT '锁描述信息，方便排查',
  `expired_time` datetime NOT NULL COMMENT '锁过期时间，超过此时间视为已释放',
  `revision` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号，每次更新+1',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_lock_key` (`lock_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='分布式锁记录表';
```

### 3.2 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | bigint PK | 自增主键 |
| `lock_key` | varchar(128) UNIQUE | 锁的唯一标识，业务语义化命名，如 `pipeline:run:sync-log:123` |
| `lock_value` | varchar(64) | 持有者标识，每次加锁生成一个 UUID，释放/续期时校验，防止误删别人的锁 |
| `description` | varchar(256) | 锁的描述信息，方便运维排查 |
| `expired_time` | datetime | 锁的过期时间，超过此时间视为已自动释放，防止死锁 |
| `revision` | int | 乐观锁版本号，配合 MyBatis-Plus `@Version` 注解，UPDATE 时自动 `WHERE revision=? AND revision=revision+1` |
| `create_time` | datetime | 创建时间 |
| `update_time` | datetime | 最后更新时间 |

### 3.3 设计要点

- **`lock_key` 唯一索引**：保证同一把锁全局只有一条记录，是互斥性的基础。
- **`lock_value`（UUID）**：每次加锁生成唯一标识，释放时必须匹配，防止 A 持有的锁因过期被 B 抢占后，A 又误删 B 的锁。
- **`revision`（乐观锁）**：UPDATE CAS 的核心，并发抢占时只有一个线程能匹配到旧 revision，保证原子性。
- **`expired_time`**：即使持有者宕机未释放，到达过期时间后其他线程也能抢占，避免死锁。

---

## 四、核心原理与实现

### 4.1 加锁原理（UPDATE CAS）

加锁的核心思路是：**先查后更，CAS 抢占**。

```
tryLock(key, expireSeconds, description):

  1. 生成 lockValue = UUID

  2. 查询记录：SELECT * FROM distributed_lock WHERE lock_key = #{key}

  3. 分支判断：
     ├── 记录不存在 → 走 INSERT 流程（见 4.1.1）
     ├── 记录存在但 expired_time <= now（已过期）→ 走 UPDATE 抢占流程（见 4.1.2）
     └── 记录存在且 expired_time > now（未过期）→ 抢锁失败，返回 null

  4. 返回 lockValue（成功）或 null（失败）
```

#### 4.1.1 记录不存在 → INSERT

首次加锁，锁记录尚未创建。直接 INSERT 一条新记录：

```sql
INSERT INTO distributed_lock (lock_key, lock_value, description, expired_time, revision)
VALUES (#{key}, #{lockValue}, #{description}, DATE_ADD(NOW(), INTERVAL #{expireSeconds} SECOND), 0)
```

> **并发安全**：`lock_key` 有唯一索引，如果两个线程同时 INSERT，只有一个成功，另一个抛 `DuplicateKeyException`，捕获后视为抢锁失败。

#### 4.1.2 记录已过期 → UPDATE CAS 抢占

锁记录存在但已过期（持有者宕机或业务超时未释放）。用乐观锁抢占：

```sql
UPDATE distributed_lock
SET lock_value = #{newLockValue},
    description = #{description},
    expired_time = DATE_ADD(NOW(), INTERVAL #{expireSeconds} SECOND),
    revision = revision + 1
WHERE lock_key = #{key}
  AND revision = #{oldRevision}
```

> **并发安全**：`WHERE revision = #{oldRevision}` 保证并发情况下只有一个线程更新成功（返回 affectedRows = 1），其他线程 affectedRows = 0，视为抢锁失败。

### 4.2 阻塞式加锁（lock）

在 `tryLock` 基础上增加自旋等待：

```
lock(key, waitSeconds, expireSeconds, description):

  1. 记录 startTime = now
  2. 循环：
     ├── result = tryLock(key, expireSeconds, description)
     ├── if result != null → 加锁成功，返回 result
     ├── if (now - startTime) >= waitSeconds → 超时，返回 null
     └── Thread.sleep(retryInterval)  // 默认 200ms，可配置
```

> **退避策略**：固定间隔重试（默认 200ms）。对于 DB 锁场景，持有时间通常较短，固定间隔足够简单有效。

### 4.3 释放锁（unlock）

释放锁时必须校验 `lock_value`，确保只能释放自己持有的锁：

```sql
UPDATE distributed_lock
SET expired_time = NOW(),       -- 立即过期
    revision = revision + 1
WHERE lock_key = #{key}
  AND lock_value = #{lockValue}
```

> **为什么不直接 DELETE**：保留记录方便下次加锁直接走 UPDATE CAS 分支，避免频繁 INSERT/DELETE 产生的碎片。设置 `expired_time = NOW()` 使其立即可被其他线程抢占。
>
> **防误删**：`WHERE lock_value = #{lockValue}` 确保即使锁已过期被别人抢占，也不会误删。

### 4.4 续期（renew）

业务执行时间可能超过锁的过期时间，提供手动续期：

```sql
UPDATE distributed_lock
SET expired_time = DATE_ADD(NOW(), INTERVAL #{expireSeconds} SECOND),
    revision = revision + 1
WHERE lock_key = #{key}
  AND lock_value = #{lockValue}
  AND expired_time > NOW()       -- 只能续未过期的锁
```

> **校验**：必须同时匹配 `lock_value`（防误续）和 `expired_time > NOW()`（已过期的锁不能续，需重新加锁）。

---

## 五、接口设计

### 5.1 DistributedLockService 接口

```java
public interface DistributedLockService {

    /**
     * 非阻塞尝试获取锁（尝试一次）
     *
     * @param key            锁标识
     * @param expireSeconds  锁过期时间（秒）
     * @param description    锁描述信息
     * @return 加锁成功返回 lockValue（UUID），失败返回 null
     */
    String tryLock(String key, int expireSeconds, String description);

    /**
     * 阻塞式获取锁（在超时时间内自旋重试）
     *
     * @param key            锁标识
     * @param waitSeconds    最大等待时间（秒）
     * @param expireSeconds  锁过期时间（秒）
     * @param description    锁描述信息
     * @return 加锁成功返回 lockValue（UUID），超时返回 null
     */
    String lock(String key, int waitSeconds, int expireSeconds, String description);

    /**
     * 释放锁
     *
     * @param key       锁标识
     * @param lockValue 持有者标识（tryLock/lock 的返回值）
     * @return 释放成功返回 true，锁不存在或不属于当前持有者返回 false
     */
    boolean unlock(String key, String lockValue);

    /**
     * 续期（延长锁的过期时间）
     *
     * @param key            锁标识
     * @param lockValue      持有者标识
     * @param expireSeconds  新的过期时间（秒），从当前时刻起算
     * @return 续期成功返回 true，锁不存在/已过期/不属于当前持有者返回 false
     */
    boolean renew(String key, String lockValue, int expireSeconds);
}
```

### 5.2 使用示例

```java
// 场景：流水线结束后同步 Pod 日志，防止同一流水线并发同步
String lockKey = "pipeline:run:sync-log:" + runId;
String lockValue = distributedLockService.lock(lockKey, 30, 300, "同步流水线运行日志");
if (lockValue == null) {
    log.warn("获取锁失败，可能有其他线程正在同步流水线[{}]的日志", runId);
    return;
}
try {
    // 执行同步日志业务逻辑
    syncPodLogs(runId);
} finally {
    distributedLockService.unlock(lockKey, lockValue);
}
```

---

## 六、实现步骤

### 6.1 数据层（pipeline-server-dao）

#### 步骤 1：建表 SQL

在 `sql/` 目录新增 `distributed_lock.sql`，内容见 [3.1 节](#31-锁记录表-distributed_lock)。

#### 步骤 2：Entity

```java
@Data
@TableName("distributed_lock")
public class DistributedLock {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String lockKey;

    private String lockValue;

    private String description;

    private Date expiredTime;

    @Version
    private Integer revision;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
```

#### 步骤 3：Mapper 接口

```java
@Mapper
public interface DistributedLockMapper extends BaseMapper<DistributedLock> {

    /**
     * 抢占已过期的锁（CAS）
     */
    int casAcquire(@Param("key") String key,
                   @Param("newLockValue") String newLockValue,
                   @Param("description") String description,
                   @Param("expireSeconds") int expireSeconds,
                   @Param("oldRevision") Integer oldRevision);

    /**
     * 释放锁（校验 lockValue）
     */
    int release(@Param("key") String key,
                @Param("lockValue") String lockValue);

    /**
     * 续期（校验 lockValue + 未过期）
     */
    int renew(@Param("key") String key,
              @Param("lockValue") String lockValue,
              @Param("expireSeconds") int expireSeconds);
}
```

#### 步骤 4：Mapper XML

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.ci.pipeline.dao.mapper.DistributedLockMapper">

    <!-- 抢占已过期的锁（CAS） -->
    <update id="casAcquire">
        UPDATE distributed_lock
        SET lock_value   = #{newLockValue},
            description  = #{description},
            expired_time = DATE_ADD(NOW(), INTERVAL #{expireSeconds} SECOND),
            revision     = revision + 1
        WHERE lock_key = #{key}
          AND revision = #{oldRevision}
    </update>

    <!-- 释放锁 -->
    <update id="release">
        UPDATE distributed_lock
        SET expired_time = NOW(),
            revision     = revision + 1
        WHERE lock_key   = #{key}
          AND lock_value = #{lockValue}
    </update>

    <!-- 续期 -->
    <update id="renew">
        UPDATE distributed_lock
        SET expired_time = DATE_ADD(NOW(), INTERVAL #{expireSeconds} SECOND),
            revision     = revision + 1
        WHERE lock_key   = #{key}
          AND lock_value = #{lockValue}
          AND expired_time > NOW()
    </update>

</mapper>
```

> **注意**：CAS 抢占、释放、续期这三个操作没有使用 MyBatis-Plus 的 `@Version` 自动乐观锁，而是在 XML 中手动编写 `WHERE revision = ?` 条件。原因是这三个操作的 WHERE 条件各不相同（抢占需要 `oldRevision`、释放需要 `lockValue`、续期需要 `lockValue + expired_time`），手写 SQL 更清晰可控。

### 6.2 业务层（pipeline-server-service）

#### 步骤 5：DistributedLockServiceImpl

核心实现逻辑：

```java
@Slf4j
@Service
public class DistributedLockServiceImpl implements DistributedLockService {

    @Resource
    private DistributedLockMapper distributedLockMapper;

    /** 阻塞模式重试间隔（毫秒） */
    private static final long RETRY_INTERVAL_MS = 200L;

    @Override
    public String tryLock(String key, int expireSeconds, String description) {
        String lockValue = UUID.randomUUID().toString();

        // 1. 查询锁记录
        DistributedLock lock = distributedLockMapper.selectOne(
                new LambdaQueryWrapper<DistributedLock>()
                        .eq(DistributedLock::getLockKey, key));

        // 2. 记录不存在 → INSERT
        if (lock == null) {
            return insertLock(key, lockValue, description, expireSeconds);
        }

        // 3. 记录存在但已过期 → UPDATE CAS 抢占
        if (lock.getExpiredTime().before(new Date())) {
            int rows = distributedLockMapper.casAcquire(
                    key, lockValue, description, expireSeconds, lock.getRevision());
            return rows > 0 ? lockValue : null;
        }

        // 4. 记录存在且未过期 → 抢锁失败
        return null;
    }

    /**
     * INSERT 加锁，处理唯一索引冲突
     */
    private String insertLock(String key, String lockValue,
                              String description, int expireSeconds) {
        try {
            DistributedLock lock = new DistributedLock();
            lock.setLockKey(key);
            lock.setLockValue(lockValue);
            lock.setDescription(description);
            lock.setExpiredTime(new Date(System.currentTimeMillis()
                    + expireSeconds * 1000L));
            lock.setRevision(0);
            distributedLockMapper.insert(lock);
            return lockValue;
        } catch (DuplicateKeyException e) {
            // 并发 INSERT，唯一索引冲突，抢锁失败
            return null;
        }
    }

    @Override
    public String lock(String key, int waitSeconds,
                       int expireSeconds, String description) {
        long deadline = System.currentTimeMillis() + waitSeconds * 1000L;
        while (true) {
            String lockValue = tryLock(key, expireSeconds, description);
            if (lockValue != null) {
                return lockValue;
            }
            if (System.currentTimeMillis() >= deadline) {
                return null;
            }
            try {
                Thread.sleep(RETRY_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
    }

    @Override
    public boolean unlock(String key, String lockValue) {
        int rows = distributedLockMapper.release(key, lockValue);
        if (rows == 0) {
            log.warn("释放锁失败，锁可能已过期或不属于当前持有者: key={}", key);
        }
        return rows > 0;
    }

    @Override
    public boolean renew(String key, String lockValue, int expireSeconds) {
        int rows = distributedLockMapper.renew(key, lockValue, expireSeconds);
        if (rows == 0) {
            log.warn("续期失败，锁可能已过期或不属于当前持有者: key={}", key);
        }
        return rows > 0;
    }
}
```

### 6.3 常量（pipeline-server-common）

在 `com.ci.pipeline.common.constants` 下新增 `DistributedLockConstants`：

```java
/**
 * 分布式锁相关常量
 */
public final class DistributedLockConstants {

    private DistributedLockConstants() {}

    /** 锁 key 前缀：流水线运行日志同步 */
    public static final String LOCK_KEY_PIPELINE_RUN_SYNC_LOG = "pipeline:run:sync-log:";

    /** 阻塞模式默认重试间隔（毫秒） */
    public static final long DEFAULT_RETRY_INTERVAL_MS = 200L;
}
```

---

## 七、并发安全性分析

### 7.1 两个线程同时加锁（记录不存在）

| 时序 | 线程 A | 线程 B | 结果 |
|------|--------|--------|------|
| T1 | SELECT → 记录不存在 | | |
| T2 | | SELECT → 记录不存在 | |
| T3 | INSERT → 成功 | | A 获得锁 |
| T4 | | INSERT → 唯一索引冲突 | B 抢锁失败 |

**结论**：`lock_key` 唯一索引保证只有一个 INSERT 成功。

### 7.2 两个线程同时抢占过期锁

| 时序 | 线程 A | 线程 B | 结果 |
|------|--------|--------|------|
| T1 | SELECT → revision=5, 已过期 | | |
| T2 | | SELECT → revision=5, 已过期 | |
| T3 | UPDATE WHERE revision=5 → 成功, revision→6 | | A 获得锁 |
| T4 | | UPDATE WHERE revision=5 → affectedRows=0 | B 抢锁失败 |

**结论**：CAS 乐观锁保证只有一个 UPDATE 成功。

### 7.3 持有者宕机后锁自动释放

```
T1: 线程 A 获得锁，expired_time = T1 + 300s
T2: 线程 A 宕机，未调用 unlock
T301: 线程 B 查询 → expired_time < now → 抢占成功
```

**结论**：过期时间机制保证死锁不会发生。

### 7.4 防误删场景

```
T1:   线程 A 获得锁，lock_value=UUID-A，过期时间 30s
T31:  线程 A 业务执行缓慢（超过 30s），锁自动过期
T32:  线程 B 抢占到锁，lock_value=UUID-B
T33:  线程 A 执行完毕，调用 unlock(key, UUID-A)
      → WHERE lock_value=UUID-A 匹配失败 → 不影响 B 的锁
```

**结论**：`lock_value` 校验防止误删。

---

## 八、使用注意事项

1. **过期时间设置**：`expireSeconds` 应略大于业务预期最大执行时间。如果业务执行时间不确定，可在执行过程中调用 `renew` 续期。
2. **必须使用 try-finally 释放锁**：业务逻辑放在 try 块中，`unlock` 放在 finally 块中，确保异常时也能释放。
3. **lock_value 必须保存**：`tryLock`/`lock` 返回的 `lockValue` 必须保存，释放/续期时需要传入。
4. **阻塞模式注意超时**：`lock(waitSeconds)` 在超时时间内会阻塞线程，不要在时间敏感的接口中长时间等待。
5. **锁粒度**：`lock_key` 应包含业务唯一标识（如 `pipeline:run:sync-log:{runId}`），粒度越细并发度越高。
6. **不保证公平性**：多个线程同时抢锁时，不保证先到先得。

---

## 九、后续扩展方向（本期不实现）

| 方向 | 说明 |
|------|------|
| 可重入 | 记录持有者标识和重入次数，同持有者可多次获取 |
| 自动续期 watchdog | 后台定时线程为持有的锁自动续期 |
| 注解 AOP | `@DistributedLock(key="xxx")` 声明式加锁 |
| 公平锁 | 记录等待队列，先到先得 |
| 锁监控 | 锁的持有时间、等待时间、竞争情况监控 |
