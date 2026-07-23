package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 字典数据修改入参
 */
@Data
public class DictDataUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键（必填）
     */
    private Long id;

    /**
     * 字典类型
     */
    private String dictType;

    /**
     * 数据名称
     */
    private String dictKey;

    /**
     * 数据值
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
