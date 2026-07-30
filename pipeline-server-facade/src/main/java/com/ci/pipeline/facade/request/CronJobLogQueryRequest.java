package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class CronJobLogQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 关联的定时任务ID，精确匹配 */
    private Long jobId;

    /** 执行状态：running / succeeded / failed，精确匹配 */
    private String status;

    /** 页码（从 1 开始） */
    private Long pageNum;

    /** 每页大小 */
    private Long pageSize;
}
