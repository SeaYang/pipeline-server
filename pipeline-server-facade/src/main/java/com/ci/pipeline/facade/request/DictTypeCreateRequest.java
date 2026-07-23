package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 字典类型新增入参
 */
@Data
public class DictTypeCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 字典类型（必填）
     */
    private String dictType;

    /**
     * 字典名称（必填）
     */
    private String dictName;

    /**
     * 备注信息
     */
    private String remark;
}
