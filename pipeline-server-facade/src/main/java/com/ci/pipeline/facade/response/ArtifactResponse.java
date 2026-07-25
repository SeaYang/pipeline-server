package com.ci.pipeline.facade.response;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 制品信息响应
 */
@Data
public class ArtifactResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String appName;
    private String name;
    private String type;
    private String gitBranch;
    private String commitId;
    private String env;
    private Date buildTime;
    private String buildUser;
    private Long pipelineRunId;
    private String pipelineRunName;
    private String artifactRepository;
    private String artifactRepositoryPath;
    private String artifactUrl;
    private Long size;
    private String sha256;
    private Date createTime;
    private Date updateTime;
}
