package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class PipelineParameterQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 参数名，模糊匹配 */
    private String name;

    /** 参数中文名称，模糊匹配 */
    private String label;

    /** 参数类型，精确匹配 */
    private String paramType;

    /** 参数所属组别，精确匹配 */
    private String paramGroup;

    /** 排序字段（camelCase 出参字段名） */
    private String sortField;

    /** 排序方向：asc / desc */
    private String sortOrder;

    /** 页码（从 1 开始） */
    private Long pageNum;

    /** 每页大小 */
    private Long pageSize;
}
