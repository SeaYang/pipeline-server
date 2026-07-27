package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class GenericConfigCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 配置键，必填，全局唯一 */
    private String configKey;

    /** 配置值，必填 */
    private Object configValue;

    /** 值格式，必填：txt / json */
    private String valueFormat;

    /** 备注说明 */
    private String description;
}
