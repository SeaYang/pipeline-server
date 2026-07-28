package com.ci.pipeline.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 分布式锁实体。
 * <p>对应 distributed_lock 表，基于 DB 实现的轻量级分布式锁。
 */
@Data
@TableName("distributed_lock")
public class DistributedLock implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 锁唯一标识，业务语义化命名 */
    private String lockKey;

    /** 持有者标识（UUID），用于校验锁的归属，防止误删 */
    private String lockValue;

    /** 锁描述信息，方便排查 */
    private String description;

    /** 锁过期时间，超过此时间视为已释放 */
    private Date expiredTime;

    /** 乐观锁版本号，每次更新+1 */
    private Integer revision;

    private Date createTime;

    private Date updateTime;
}
