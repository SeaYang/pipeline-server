package com.ci.pipeline.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 定时任务定义表实体。
 */
@Data
@TableName("cron_job")
public class CronJob implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务名称 */
    private String name;

    /** 目标 Spring Bean 名称 */
    private String beanName;

    /** 目标方法名称 */
    private String methodName;

    /** 方法参数，JSON 数组字符串，如 ["daily", 500]，无参为 null */
    private String methodParams;

    /** CRON 表达式（6 位：秒 分 时 日 月 周） */
    private String cronExpr;

    /** 是否启用：0-停用 1-启用 */
    private Integer enabled;

    /** 错过执行策略，见 {@link com.ci.pipeline.common.enums.MisfirePolicyEnum} */
    private String misfirePolicy;

    /** 是否允许并发执行：0-禁止并发 1-允许并发 */
    private Integer concurrent;

    /** 下一次触发时间 */
    private Date nextFireTime;

    /** 上一次触发时间 */
    private Date lastFireTime;

    /** 乐观锁版本号，用于多实例抢占调度 */
    private Integer revision;

    private Date createTime;

    private Date updateTime;

    private Integer deleted;
}
