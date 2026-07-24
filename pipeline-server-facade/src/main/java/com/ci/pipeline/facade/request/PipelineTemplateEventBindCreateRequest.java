package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 事件-模板绑定 新增请求
 */
@Data
public class PipelineTemplateEventBindCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 事件类型（需存在于字典 pipeline_event_type 且 enabled=1）
     */
    private String eventType;

    /**
     * 流水线模板编码（需存在于 pipeline_template 表）
     */
    private String pipelineTemplateCode;
}
