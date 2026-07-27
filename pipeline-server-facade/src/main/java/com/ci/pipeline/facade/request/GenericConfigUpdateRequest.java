package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class GenericConfigUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 配置ID，必填 */
    private Long id;

    /** 配置值，必填 */
    private Object configValue;

    /** 值格式，必填：txt / json */
    private String valueFormat;

    /** 备注说明 */
    private String description;
}
