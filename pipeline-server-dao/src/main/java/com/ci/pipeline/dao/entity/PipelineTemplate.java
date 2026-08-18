package com.ci.pipeline.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 流水线模板实体（保存流水线模板的基础字段定义，由多个任务模板组装而成，对应一个 argo WorkflowTemplate）
 */
@Data
@TableName("pipeline_template")
public class PipelineTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 模板编码
     */
    private String pipelineTemplateCode;

    /**
     * 模板名称
     */
    private String name;

    /**
     * 模板详细描述
     */
    private String description;

    /**
     * 流水线模板所属分组
     */
    private String pipelineTemplateGroup;

    /**
     * 候选执行集群，逗号分隔多个 clusterName；NULL/空 表示不限制集群
     */
    private String clusterNames;

    /**
     * 集群调度策略：Any-任意集群 / PreferSelected-优先选中集群
     */
    private String clusterSchedulePolicy;

    /**
     * 应用维度最大并发执行数：同一 appName 使用本模板的未完成执行数上限（统计 Pending/Running/Unknown），默认 1 即不允许并发
     */
    private Integer appMaxRunningLimit;

    /**
     * 超限策略：Reject-拒绝新执行 / ReplaceOldest-终止最早执行腾位
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
