package com.ci.pipeline.facade.response;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 字典数据响应
 */
@Data
public class DictDataResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
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

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
