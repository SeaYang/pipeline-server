package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 任务模板修改入参
 */
@Data
public class TaskTemplateUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键（必填）
     */
    private Long id;

    /**
     * 任务模板编码（传入时需非空，参与唯一性校验）
     */
    private String taskTemplateCode;

    /**
     * 任务模板名称
     */
    private String name;

    /**
     * 详细描述内容
     */
    private String description;

    /**
     * 任务模板所属分组
     */
    private String taskTemplateGroup;
}
