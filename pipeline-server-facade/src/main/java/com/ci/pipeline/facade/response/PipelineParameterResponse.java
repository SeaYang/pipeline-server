package com.ci.pipeline.facade.response;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class PipelineParameterResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String label;
    private String description;
    private String componentType;
    private String paramType;
    private Boolean required;
    private String defaultValue;
    private Boolean needSystemProcess;
    private String regexPattern;
    private String dependParams;
    private Boolean refreshOnChanged;
    private String paramGroup;
    private Integer paramGroupSort;
    private String optionConfig;
    private String defaultValueStrategyConfig;
    private String creator;
    private Date createTime;
    private Date updateTime;
}
