package com.ci.pipeline.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 通用配置实体。
 * <p>对应 generic_config 表，管理 key-value 形式的静态配置。
 */
@Data
@TableName("generic_config")
public class GenericConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 配置键，全局唯一（业务层校验） */
    private String configKey;

    /** 配置值，json 格式时存序列化字符串 */
    private String configValue;

    /** 值格式：txt-纯文本 / json-JSON */
    private String valueFormat;

    /** 备注说明 */
    private String description;

    /** 创建人 */
    private String creator;

    private Date createTime;

    /** 最后修改人 */
    private String updater;

    private Date updateTime;

    /** 逻辑删除（0-未删除，1-已删除） */
    private Integer deleted;
}
