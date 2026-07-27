package com.ci.pipeline.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 通用配置变更历史实体。
 * <p>对应 generic_config_history 表，记录每次创建、修改、删除操作。
 */
@Data
@TableName("generic_config_history")
public class GenericConfigHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联配置ID */
    private Long configId;

    /** 配置键快照 */
    private String configKey;

    /** 操作类型：CREATE-新建 / UPDATE-修改 / DELETE-删除 */
    private String action;

    /** 变更前值 */
    private String oldValue;

    /** 变更后值 */
    private String newValue;

    /** 变更前值格式 */
    private String oldValueFormat;

    /** 变更后值格式 */
    private String newValueFormat;

    /** 变更摘要，描述哪些字段发生了变化 */
    private String changeSummary;

    /** 操作人 */
    private String operator;

    /** 操作时间 */
    private Date operateTime;
}
