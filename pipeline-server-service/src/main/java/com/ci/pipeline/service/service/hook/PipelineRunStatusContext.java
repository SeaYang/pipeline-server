package com.ci.pipeline.service.service.hook;

import com.ci.pipeline.common.enums.PipelineRunStatusEnum;
import lombok.Builder;
import lombok.Data;

/**
 * 流水线执行状态变化上下文，在状态发生变更并回写成功后传递给各 {@link PipelineRunStatusHook}。
 */
@Data
@Builder
public class PipelineRunStatusContext {

    /**
     * 执行记录 id
     */
    private Long pipelineRunId;

    /**
     * 流水线 id
     */
    private Long pipelineId;

    /**
     * 服务的 appName
     */
    private String appName;

    /**
     * 执行名称（Argo Workflow 名称）
     */
    private String name;

    /**
     * 变更前的状态（首次落地为 Pending 后的首次回写时，previous 通常为 Pending）
     */
    private PipelineRunStatusEnum previousStatus;

    /**
     * 变更后的状态
     */
    private PipelineRunStatusEnum currentStatus;

    /**
     * 失败详细信息（失败态时取自 Argo status.message）
     */
    private String failMessage;

    /**
     * 执行时长（秒，终态时回填）
     */
    private Integer duration;
}
