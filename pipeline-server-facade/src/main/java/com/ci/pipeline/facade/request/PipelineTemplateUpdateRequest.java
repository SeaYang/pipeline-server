package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 流水线模板修改入参
 */
@Data
public class PipelineTemplateUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键（必填）
     */
    private Long id;

    /**
     * 模板编码（传入时需非空，参与唯一性校验）
     */
    private String pipelineTemplateCode;

    /**
     * 模板名称
     */
    private String name;

    /**
     * 模板详细描述
     */
    private String description;

    /**
     * 流水线模板所属分组
     */
    private String pipelineTemplateGroup;
}
