package com.ci.pipeline.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 流水线执行记录实体（对应 pipeline 的一次具体执行）。
 * <p>执行流水线触发 Argo Workflow 后落地一条记录，随后由平台异步轮询 Argo 状态回写。
 */
@Data
@TableName("pipeline_run")
public class PipelineRun implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 流水线 id，对应 pipeline 的 id
     */
    private Long pipelineId;

    /**
     * 流水线执行名称（Argo 返回的 Workflow 名称）
     */
    private String name;

    /**
     * 执行集群标识（提交时选定的集群），日志/同步/重试/停止按此路由；存量为空时兜底默认集群
     */
    private String clusterName;

    /**
     * 服务的 appName，比如：pipeline-server
     */
    private String appName;

    /**
     * 流水线模板编码，和 pipeline_template 的对应
     */
    private String pipelineTemplateCode;

    /**
     * 执行流水线时的模板版本，对应 pipeline_template_version 的 version
     */
    private String pipelineTemplateVersion;

    /**
     * 流水线执行状态（见 PipelineRunStatus）
     */
    private String status;

    /**
     * 流水线执行时的 git 分支
     */
    private String gitBranch;

    /**
     * 流水线执行时的 git 分支的 commitId
     */
    private String commitId;

    /**
     * 流水线执行时的参数（入参 parameters 的 JSON 字符串）
     */
    private String arguments;

    /**
     * 流水线执行失败的类型
     */
    private String failType;

    /**
     * 流水线执行失败的详细信息
     */
    private String failMessage;

    /**
     * 执行时长（秒）
     */
    private Integer duration;

    /**
     * 乐观锁版本号（每次回写 WHERE revision = ? 并自增）
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

    /**
     * 逻辑删除标识（0-未删除，1-已删除），由 MyBatis-Plus 全局配置接管
     */
    private Integer deleted;
}
