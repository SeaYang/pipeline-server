package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 流水线终态清理回调入参（清理 Workflow 的 clean-notify 任务调用）
 */
@Data
public class PipelineCleanCallbackRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 业务流水线的 pipelineRunName（必填）
     */
    private String pipelineRunName;
}
