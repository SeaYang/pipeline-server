package com.ci.pipeline.facade.response;

import lombok.Data;

import java.io.Serializable;

/**
 * 流水线执行快照响应。
 * <p>包装 pipeline_run_snapshot.detail（JSON 字符串），供前端展示执行快照原始数据。
 */
@Data
public class PipelineRunSnapshotResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 流水线执行记录 id */
    private Long pipelineRunId;

    /** 执行详情快照 JSON 字符串（Argo Workflow 完整 CRD） */
    private String detail;
}
