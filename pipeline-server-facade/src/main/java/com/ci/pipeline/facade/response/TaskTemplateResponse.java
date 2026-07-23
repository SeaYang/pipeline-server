package com.ci.pipeline.facade.response;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 任务模板响应
 */
@Data
public class TaskTemplateResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 任务模板编码
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

    /**
     * 创建人
     */
    private String creator;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
