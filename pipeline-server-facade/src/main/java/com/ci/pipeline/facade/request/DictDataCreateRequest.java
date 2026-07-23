package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 字典数据新增入参
 */
@Data
public class DictDataCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 字典类型（必填）
     */
    private String dictType;

    /**
     * 数据名称（必填）
     */
    private String dictKey;

    /**
     * 数据值（必填）
     */
    private String dictValue;

    /**
     * 排序值
     */
    private Integer dictSort;

    /**
     * 备注信息
     */
    private String remark;

    /**
     * 是否启用
     */
    private Boolean enabled;
}
