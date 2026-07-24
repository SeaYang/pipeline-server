package com.ci.pipeline.facade.response;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 事件-模板绑定 响应
 */
@Data
public class PipelineTemplateEventBindResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /**
     * 事件类型编码
     */
    private String eventType;

    /**
     * 事件类型中文名（关联字典翻译）
     */
    private String eventTypeDesc;

    /**
     * 流水线模板编码
     */
    private String pipelineTemplateCode;

    /**
     * 模板名称（关联翻译）
     */
    private String pipelineTemplateName;

    private String creator;
    private Date createTime;
    private Date updateTime;
}
