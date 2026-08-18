package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 流水线实例修改入参（目前允许修改 name / maxRunningLimit / overLimitPolicy）
 */
@Data
public class PipelineUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键（必填）
     */
    private Long id;

    /**
     * 流水线名称
     */
    private String name;

    /**
     * 本流水线最大并发执行数（可选；NULL 表示未配置，fallback 到模板的 appMaxRunningLimit；配置值超过模板值时按模板值生效）
     */
    private Integer maxRunningLimit;

    /**
     * 超限策略（可选：Reject-拒绝新执行 / ReplaceOldest-终止最早执行腾位；NULL 表示未配置，fallback 到模板的 overLimitPolicy）
     */
    private String overLimitPolicy;
}
