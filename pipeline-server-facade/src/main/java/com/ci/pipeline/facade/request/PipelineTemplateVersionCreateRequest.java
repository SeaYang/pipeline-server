package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 流水线模板版本新增入参
 */
@Data
public class PipelineTemplateVersionCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 流水线模板编码（必填）
     */
    private String pipelineTemplateCode;

    /**
     * 模板版本号，如 1.0.1（必填，三段点分数字，只能递增）
     */
    private String version;

    /**
     * 流水线模板详情，对应 argo WorkflowTemplate 的 json/yml 字符串（必填）
     */
    private String templateDetail;

    /**
     * 版本变更说明
     */
    private String changeNote;
}
