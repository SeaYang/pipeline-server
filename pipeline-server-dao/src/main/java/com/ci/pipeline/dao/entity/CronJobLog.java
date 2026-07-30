package com.ci.pipeline.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 定时任务执行日志表实体。
 */
@Data
@TableName("cron_job_log")
public class CronJobLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联 cron_job.id */
    private Long jobId;

    /** 任务名称快照 */
    private String jobName;

    /** Bean 名称快照 */
    private String beanName;

    /** 方法名称快照 */
    private String methodName;

    /** 方法参数快照 */
    private String methodParams;

    /** 执行状态：running / succeeded / failed，见 {@link com.ci.pipeline.common.constants.CronJobConstants} */
    private String status;

    /** 结果信息：失败异常堆栈 / 停止原因等 */
    private String message;

    /** 执行实例 IP，用于跨实例路由"停止任务"请求 */
    private String instanceIp;

    private Date startTime;

    private Date endTime;

    /** 执行耗时（毫秒） */
    private Long costMs;

    private Date createTime;

    private Integer deleted;
}
