package com.ci.pipeline.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 流水线实例实体（保存 appName 和流水线模板之间的关联关系，一个流水线模板可被多个 appName 使用，
 * 一条流水线属于一个特定的 appName）
 */
@Data
@TableName("pipeline")
public class Pipeline implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 流水线名称
     */
    private String name;

    /**
     * 服务的 appName，比如：pipeline-server
     */
    private String appName;

    /**
     * 流水线模板编码，和 pipeline_template 的对应
     */
    private String pipelineTemplateCode;

    /**
     * 本流水线最大并发执行数；NULL 表示未配置，fallback 到模板的 appMaxRunningLimit；配置值超过模板值时按模板值生效（clamp）
     */
    private Integer maxRunningLimit;

    /**
     * 超限策略：Reject / ReplaceOldest；NULL 表示未配置，fallback 到模板的 overLimitPolicy
     */
    private String overLimitPolicy;

    /**
     * 创建人
     */
    private String creator;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 逻辑删除标识（0-未删除，1-已删除），由 MyBatis-Plus 全局配置接管
     */
    private Integer deleted;
}
