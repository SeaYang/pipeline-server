package com.ci.pipeline.facade.response;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 流水线模板响应
 */
@Data
public class PipelineTemplateResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 模板编码
     */
    private String pipelineTemplateCode;

    /**
     * 模板名称
     */
    private String name;

    /**
     * 模板详细描述
     */
    private String description;

    /**
     * 流水线模板所属分组
     */
    private String pipelineTemplateGroup;

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
