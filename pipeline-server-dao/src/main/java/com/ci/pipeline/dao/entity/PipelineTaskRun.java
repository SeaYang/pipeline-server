package com.ci.pipeline.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 流水线执行-任务节点记录实体（对应 pipeline_run 的一个任务节点 Pod 的具体执行）。
 * <p>仅当 pipeline_run 进入终态（Succeeded / Cancelled）时，由平台解析 Argo Workflow 的
 * status.nodes（type=Pod）落地。
 */
@Data
@TableName("pipeline_task_run")
public class PipelineTaskRun implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 流水线执行 id，对应 pipeline_run 的 id
     */
    private Long pipelineRunId;

    /**
     * 任务节点编码，对应 task_template 的 task_template_code
     */
    private String taskCode;

    /**
     * 执行状态（见 PipelineTaskRunStatus）
     */
    private String status;

    /**
     * 执行入参 json 数组字符串，元素字段有 name、value
     */
    private String inputs;

    /**
     * 执行出参 json 数组字符串，元素字段有 name、value
     */
    private String outputs;

    /**
     * 任务节点所在 pod 的名称
     */
    private String podName;

    /**
     * 任务节点所在 pod 的日志
     */
    private String logContent;

    /**
     * 开始执行时间
     */
    private Date startTime;

    /**
     * 执行结束时间
     */
    private Date endTime;

    /**
     * 执行时长（秒）
     */
    private Integer duration;

    /**
     * 任务 pod 执行时的 k8s 主机
     */
    private String runHostName;

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
