package com.ci.pipeline.facade.response;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 单个应用的触发结果
 */
@Data
@Builder
public class PipelineEventTriggerResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 应用名称（原样返回入参中的 app-name）
     */
    private String appName;

    /**
     * 流水线运行记录 ID，触发失败时为 null
     */
    private Long pipelineRunId;

    /**
     * 错误信息，触发成功时为 null
     */
    private String errorMessage;

    /**
     * 原始请求参数（原样回传，方便调用方核对）
     */
    private Map<String, String> requestParams;
}
