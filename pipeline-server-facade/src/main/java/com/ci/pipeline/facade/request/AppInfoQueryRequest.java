package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 应用基础信息分页查询条件
 */
@Data
public class AppInfoQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 应用名称（模糊匹配，可为空）
     */
    private String appName;

    /**
     * 排序字段（对应出参字段名，可为空）。
     * 支持：id / appName / programmingLanguage / description / gitSshUrl / createTime / updateTime
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
