package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 应用参数配置查询请求（不分页）
 */
@Data
public class AppParameterConfigQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String appName;
    private String env;
}
