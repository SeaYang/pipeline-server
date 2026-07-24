package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 事件-模板绑定 分页查询条件
 */
@Data
public class PipelineTemplateEventBindQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 事件类型（精确过滤，可为空）
     */
    private String eventType;

    /**
     * 排序字段（camelCase 出参字段名）
     */
    private String sortField;

    /**
     * 排序方向：asc / desc
     */
    private String sortOrder;

    /**
     * 页码，从 1 开始
     */
    private Long pageNum;

    /**
     * 每页条数
     */
    private Long pageSize;
}
