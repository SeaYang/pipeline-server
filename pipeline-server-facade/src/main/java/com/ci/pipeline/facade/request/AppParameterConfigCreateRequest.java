package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 应用参数配置新增请求
 */
@Data
public class AppParameterConfigCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String appName;
    private String parameterName;
    private String value;
    private String env;
}
