package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 任务模板分页查询条件
 */
@Data
public class TaskTemplateQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务模板编码（模糊匹配，可为空）
     */
    private String taskTemplateCode;

    /**
     * 任务模板名称（模糊匹配，可为空）
     */
    private String name;

    /**
     * 任务模板所属分组（精确匹配，可为空）
     */
    private String taskTemplateGroup;

    /**
     * 排序字段（对应出参字段名，可为空）。
     * 支持：id / taskTemplateCode / name / description / taskTemplateGroup / creator / createTime / updateTime
     */
    private String sortField;

    /**
     * 排序方向（asc / desc，大小写不敏感；为空时默认 desc）
     */
    private String sortOrder;

    /**
     * 页码（从 1 开始）
     */
    private Long pageNum;

    /**
     * 每页大小
     */
    private Long pageSize;
}
