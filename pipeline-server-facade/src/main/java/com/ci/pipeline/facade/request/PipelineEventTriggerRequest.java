package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 流水线事件触发请求
 */
@Data
public class PipelineEventTriggerRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 事件类型编码（对应字典 pipeline_event_type 的 dict_key）
     */
    private String eventType;

    /**
     * 参数列表，每个元素对应一个应用的触发参数。
     * 不同事件要求的参数 key 不同，由策略类定义。
     * 例如 epTestApply 事件要求：app-name、git-branch
     */
    private List<Map<String, String>> paramList;
}
