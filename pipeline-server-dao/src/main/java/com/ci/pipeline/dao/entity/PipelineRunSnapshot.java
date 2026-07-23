package com.ci.pipeline.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 流水线执行详情快照实体。
 * <p>detail 为从 Argo 查询得到的 Workflow 执行详情 JSON（结构同前端 go-cicd-workflow.json），
 * 与 pipeline_run 一对一（按 pipeline_run_id upsert）。
 */
@Data
@TableName("pipeline_run_snapshot")
public class PipelineRunSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 流水线执行 id（存 pipeline_run 主键的字符串形式）
     */
    private String pipelineRunId;

    /**
     * 流水线执行详情 json 字符串，从 argo 查询得到的
     */
    private String detail;

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
