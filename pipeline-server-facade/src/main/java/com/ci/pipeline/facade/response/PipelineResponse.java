package com.ci.pipeline.facade.response;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 流水线实例响应
 */
@Data
public class PipelineResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 流水线名称
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
     * 本流水线最大并发执行数；NULL 表示未配置，fallback 到模板的 appMaxRunningLimit
     */
    private Integer maxRunningLimit;

    /**
     * 超限策略：Reject / ReplaceOldest；NULL 表示未配置，fallback 到模板的 overLimitPolicy
     */
    private String overLimitPolicy;

    /**
     * 生效的流水线并发上限（clamp 后：min(pipeline.maxRunningLimit, template.appMaxRunningLimit)，未配置时为模板值）
     */
    private Integer effectiveMaxRunningLimit;

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
