package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 任务模板版本新增入参
 */
@Data
public class TaskTemplateVersionCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务模板编码（必填）
     */
    private String taskTemplateCode;

    /**
     * 任务版本号，如 1.0.1（必填）
     */
    private String version;

    /**
     * 任务模板详情，对应 argo WorkflowTemplate 的 json/yml 字符串（必填）
     */
    private String templateDetail;

    /**
     * 版本变更说明
     */
    private String changeNote;
}
