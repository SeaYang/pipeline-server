package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 任务模板版本状态变更入参
 */
@Data
public class TaskTemplateVersionStatusRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务模板编码
     */
    private String taskTemplateCode;

    /**
     * 任务版本号
     */
    private String version;

    /**
     * 目标状态：DRAFT / EFFECTIVE / EXPIRED
     */
    private String status;
}
