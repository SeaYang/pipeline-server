package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 流水线执行记录分页查询条件
 */
@Data
public class PipelineRunQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 流水线 id（精确匹配，可为空，为空时返回全部）
     */
    private Long pipelineId;

    /**
     * 应用名称（精确匹配，可为空，为空时返回全部）
     */
    private String appName;

    /**
     * 执行状态（精确匹配，可为空，为空时返回全部）
     */
    private String status;

    /**
     * 排序字段（对应出参字段名，可为空）。
     * 支持：id / name / appName / pipelineTemplateCode / pipelineTemplateVersion / status / duration / createTime / updateTime
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
