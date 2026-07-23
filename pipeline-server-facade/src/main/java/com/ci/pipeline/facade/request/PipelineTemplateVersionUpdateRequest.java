package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 流水线模板版本修改入参（按 pipelineTemplateCode + version 定位，仅草稿状态版本允许修改，
 * 且仅允许修改 templateDetail / changeNote）
 */
@Data
public class PipelineTemplateVersionUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 流水线模板编码（必填）
     */
    private String pipelineTemplateCode;

    /**
     * 模板版本号（必填）
     */
    private String version;

    /**
     * 流水线模板详情，对应 argo WorkflowTemplate 的 json/yml 字符串
     */
    private String templateDetail;

    /**
     * 版本变更说明
     */
    private String changeNote;
}
