package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class CronJobQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 任务名称，模糊匹配 */
    private String name;

    /** 是否启用，精确匹配 */
    private Integer enabled;

    /** 页码（从 1 开始） */
    private Long pageNum;

    /** 每页大小 */
    private Long pageSize;
}
