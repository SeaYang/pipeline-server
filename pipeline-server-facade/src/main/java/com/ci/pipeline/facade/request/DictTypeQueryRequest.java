package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 字典类型分页查询条件
 */
@Data
public class DictTypeQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 字典类型（模糊匹配）
     */
    private String dictType;

    /**
     * 字典名称（模糊匹配）
     */
    private String dictName;

    /**
     * 排序字段（对应出参字段名，可为空）。支持：id / dictType / dictName / remark / createTime / updateTime
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
