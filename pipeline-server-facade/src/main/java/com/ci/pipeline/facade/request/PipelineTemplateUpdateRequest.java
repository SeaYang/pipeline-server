package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 流水线模板修改入参
 */
@Data
public class PipelineTemplateUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键（必填）
     */
    private Long id;

    /**
     * 模板编码（传入时需非空，参与唯一性校验）
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
     * 候选执行集群（可选，空表示不限制集群）
     */
    private List<String> clusterNames;

    /**
     * 集群调度策略（可选，默认 Any：Any-任意集群 / PreferSelected-优先选中集群）
     */
    private String clusterSchedulePolicy;
}
