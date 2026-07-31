package com.ci.pipeline.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 应用参数配置实体（存储应用维度的参数默认值，区分环境）
 */
@Data
@TableName("app_parameter_config")
public class AppParameterConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 应用名称 */
    private String appName;

    /** 参数名（关联 pipeline_parameter.name） */
    private String parameterName;

    /** 参数值 */
    private String value;

    /** 环境，default 表示默认环境 */
    private String env;

    private Date createTime;
    private Date updateTime;

    /** 逻辑删除标识（0-未删除，1-已删除），由 MyBatis-Plus 全局配置接管 */
    private Integer deleted;
}
