package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 任务模板版本修改入参（按 taskTemplateCode + version 定位，仅草稿状态版本允许修改，
 * 且仅允许修改 templateDetail / changeNote）
 */
@Data
public class TaskTemplateVersionUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务模板编码（必填）
     */
    private String taskTemplateCode;

    /**
     * 任务版本号（必填）
     */
    private String version;

    /**
     * 任务模板详情，对应 argo WorkflowTemplate 的 json/yml 字符串
     */
    private String templateDetail;

    /**
     * 版本变更说明
     */
    private String changeNote;
}
