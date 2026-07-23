package com.ci.pipeline.facade.response;

import lombok.Data;

import java.io.Serializable;

/**
 * 新建流水线时的流水线模板下拉项响应（仅包含「生效中」版本的模板）
 */
@Data
public class PipelineTemplateOptionResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 流水线模板编码
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
     * 生效中版本的模板详情（argo WorkflowTemplate 的 json 字符串）
     */
    private String templateDetail;
}
