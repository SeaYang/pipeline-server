package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 流水线执行参数列表请求。
 * <p>支持前端/第三方传入已有参数值（currentValues），用于参数预填场景。
 */
@Data
public class PipelineParametersRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 流水线 id */
    private Long pipelineId;

    /**
     * 前端/第三方已传入的参数值（key=参数名，value=参数值）。
     * <p>非必填，页面手动执行时不传；第三方 API 触发时可传入预设值。
     */
    private Map<String, String> currentValues;
}
