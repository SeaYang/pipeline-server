package com.ci.pipeline.facade.response;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 应用参数配置响应（列表展示用，关联 pipeline_parameter 信息）
 */
@Data
public class AppParameterConfigResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String appName;
    private String parameterName;
    private String value;
    private String env;

    /** 参数中文名（来自 pipeline_parameter.label） */
    private String label;

    /** 参数描述（来自 pipeline_parameter.description） */
    private String description;

    private Date createTime;
    private Date updateTime;
}
