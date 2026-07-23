package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 流水线模板列表查询条件（不分页）
 */
@Data
public class PipelineTemplateQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 流水线模板所属分组（精确匹配，可为空）
     */
    private String pipelineTemplateGroup;

    /**
     * 排序字段（对应出参字段名，可为空）。
     * 支持：id / pipelineTemplateCode / name / description / pipelineTemplateGroup / creator / createTime / updateTime
     */
    private String sortField;

    /**
     * 排序方向（asc / desc，大小写不敏感；为空时默认 desc）
     */
    private String sortOrder;
}
