package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 应用参数配置修改请求（仅支持修改 value）
 */
@Data
public class AppParameterConfigUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String value;
}
