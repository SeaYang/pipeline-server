package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 流水线模板新增入参
 */
@Data
public class PipelineTemplateCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 模板编码（必填）
     */
    private String pipelineTemplateCode;

    /**
     * 模板名称（必填）
     */
    private String name;

    /**
     * 模板详细描述
     */
    private String description;

    /**
     * 流水线模板所属分组（必填）
     */
    private String pipelineTemplateGroup;
}
