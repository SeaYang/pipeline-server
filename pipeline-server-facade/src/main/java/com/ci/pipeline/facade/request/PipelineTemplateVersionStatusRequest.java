package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 流水线模板版本状态变更入参
 */
@Data
public class PipelineTemplateVersionStatusRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 流水线模板编码
     */
    private String pipelineTemplateCode;

    /**
     * 模板版本号
     */
    private String version;

    /**
     * 目标状态：DRAFT / EFFECTIVE / EXPIRED
     */
    private String status;
}
