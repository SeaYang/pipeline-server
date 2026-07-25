package com.ci.pipeline.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 制品信息实体（记录流水线构建产出的制品信息）
 */
@Data
@TableName("artifact")
public class Artifact implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 应用名称 */
    private String appName;

    /** 制品名称 */
    private String name;

    /** 制品类型：RAW / IMAGE */
    private String type;

    /** 构建时的 git 分支 */
    private String gitBranch;

    /** 构建时的 commit id */
    private String commitId;

    /** 环境标识 */
    private String env;

    /** 构建时间 */
    private Date buildTime;

    /** 构建人 */
    private String buildUser;

    /** 流水线运行ID，对应 pipeline_run.id */
    private Long pipelineRunId;

    /** 流水线运行名称，对应 pipeline_run.name（Argo Workflow name） */
    private String pipelineRunName;

    /** 制品仓库名 */
    private String artifactRepository;

    /** 仓库内相对路径 */
    private String artifactRepositoryPath;

    /** 制品完整地址 */
    private String artifactUrl;

    /** 制品大小（字节） */
    private Long size;

    /** sha256 */
    private String sha256;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

    /** 逻辑删除标识（0-未删除，1-已删除） */
    private Integer deleted;
}
