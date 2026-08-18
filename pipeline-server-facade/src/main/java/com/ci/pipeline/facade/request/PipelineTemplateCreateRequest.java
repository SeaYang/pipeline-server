package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 流水线模板新增入参
 */
@Data
public class PipelineTemplateCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 模板编码（必填）
     */
    private String pipelineTemplateCode;

    /**
     * 模板名称（必填）
     */
    private String name;

    /**
     * 模板详细描述
     */
    private String description;

    /**
     * 流水线模板所属分组（必填）
     */
    private String pipelineTemplateGroup;

    /**
     * 候选执行集群（可选，空表示不限制集群）
     */
    private List<String> clusterNames;

    /**
     * 集群调度策略（可选，默认 Any：Any-任意集群 / PreferSelected-优先选中集群）
     */
    private String clusterSchedulePolicy;

    /**
     * 应用维度最大并发执行数（可选，默认 1 即不允许并发；同一 appName 使用本模板的未完成执行数上限，≥1 且 ≤1000）
     */
    private Integer appMaxRunningLimit;

    /**
     * 超限策略（可选，默认 Reject：Reject-拒绝新执行 / ReplaceOldest-终止最早执行腾位）
     */
    private String overLimitPolicy;
}
