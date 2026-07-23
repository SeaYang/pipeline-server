package com.ci.pipeline.facade.response;

import lombok.Data;

import java.io.Serializable;

/**
 * 执行流水线响应
 */
@Data
public class PipelineExecuteResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 落地的流水线执行记录 id（pipeline_run 主键）
     */
    private Long pipelineRunId;

    /**
     * 提交后 Argo 返回的 Workflow 名称
     */
    private String workflowName;

    public PipelineExecuteResponse() {
    }

    public PipelineExecuteResponse(String workflowName) {
        this.workflowName = workflowName;
    }

    public PipelineExecuteResponse(Long pipelineRunId, String workflowName) {
        this.pipelineRunId = pipelineRunId;
        this.workflowName = workflowName;
    }
}
