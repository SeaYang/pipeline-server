package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 流水线实例新增入参
 */
@Data
public class PipelineCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 流水线名称（必填）
     */
    private String name;

    /**
     * 服务的 appName（必填），比如：pipeline-server
     */
    private String appName;

    /**
     * 流水线模板编码（必填），和 pipeline_template 的对应
     */
    private String pipelineTemplateCode;
}
