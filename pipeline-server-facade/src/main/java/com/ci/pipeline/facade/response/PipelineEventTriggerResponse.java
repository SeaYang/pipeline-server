package com.ci.pipeline.facade.response;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 流水线事件触发响应
 */
@Data
@Builder
public class PipelineEventTriggerResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 每个应用的触发结果列表，顺序与入参 paramList 一致
     */
    private List<PipelineEventTriggerResult> resultList;
}
