package com.ci.pipeline.facade.response;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 流水线模板版本响应
 */
@Data
public class PipelineTemplateVersionResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 流水线模板编码
     */
    private String pipelineTemplateCode;

    /**
     * 模板版本号，如 1.0.1
     */
    private String version;

    /**
     * 模板版本状态：DRAFT / EFFECTIVE / EXPIRED
     */
    private String status;

    /**
     * 流水线模板详情，对应 argo WorkflowTemplate 的 json/yml 字符串
     */
    private String templateDetail;

    /**
     * 版本变更说明
     */
    private String changeNote;

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
