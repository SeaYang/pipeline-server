package com.ci.pipeline.facade.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 制品上传请求（Argo pod 回传用）
 */
@Data
public class ArtifactUploadRequest implements Serializable {

    private static final long serialVersionUID = 1L;

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

    /** 构建时间（ISO 格式：2026-07-24T10:30:00+08:00） */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "GMT+8")
    private Date buildTime;

    /** 流水线运行名称（Argo Workflow name，pod 回传） */
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
}
