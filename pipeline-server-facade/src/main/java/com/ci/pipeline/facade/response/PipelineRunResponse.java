package com.ci.pipeline.facade.response;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 流水线执行记录响应
 */
@Data
public class PipelineRunResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 流水线 id
     */
    private Long pipelineId;

    /**
     * 流水线执行名称（Argo Workflow 名称）
     */
    private String name;

    /**
     * 服务的 appName
     */
    private String appName;

    /**
     * 流水线模板编码
     */
    private String pipelineTemplateCode;

    /**
     * 执行时的模板版本
     */
    private String pipelineTemplateVersion;

    /**
     * 执行状态（见 PipelineRunStatus）
     */
    private String status;

    /**
     * 执行集群（pipeline_run.cluster_name，存量为空时由 Service 层兜底解析）
     */
    private String clusterName;

    /**
     * 执行时的 git 分支
     */
    private String gitBranch;

    /**
     * 执行时的 commitId
     */
    private String commitId;

    /**
     * 执行时的参数（JSON 字符串）
     */
    private String arguments;

    /**
     * 失败类型
     */
    private String failType;

    /**
     * 失败详细信息
     */
    private String failMessage;

    /**
     * 执行时长（秒）
     */
    private Integer duration;

    /**
     * 乐观锁版本号
     */
    private Integer revision;

    /**
     * 创建人
     */
    private String creator;

    /**
     * 开始时间
     */
    private Date startTime;

    /**
     * 结束时间
     */
    private Date endTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
