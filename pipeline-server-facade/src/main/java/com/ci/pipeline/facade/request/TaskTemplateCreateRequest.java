package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 任务模板新增入参
 */
@Data
public class TaskTemplateCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务模板编码（必填）
     */
    private String taskTemplateCode;

    /**
     * 任务模板名称（必填）
     */
    private String name;

    /**
     * 详细描述内容
     */
    private String description;

    /**
     * 任务模板所属分组（必填）
     */
    private String taskTemplateGroup;
}
