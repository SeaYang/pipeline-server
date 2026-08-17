package com.ci.pipeline.facade.response;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 流水线模板响应
 */
@Data
public class PipelineTemplateResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
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
     * 候选执行集群（空列表表示不限制集群）
     */
    private List<String> clusterNames;

    /**
     * 集群调度策略：Any-任意集群 / PreferSelected-优先选中集群
     */
    private String clusterSchedulePolicy;

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
}
