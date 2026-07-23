package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 字典类型修改入参
 */
@Data
public class DictTypeUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键（必填）
     */
    private Long id;

    /**
     * 字典类型（传入时需非空，参与唯一性校验）
     */
    private String dictType;

    /**
     * 字典名称
     */
    private String dictName;

    /**
     * 备注信息
     */
    private String remark;
}
