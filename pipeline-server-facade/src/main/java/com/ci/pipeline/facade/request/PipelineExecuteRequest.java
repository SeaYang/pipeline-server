package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 执行流水线入参
 */
@Data
public class PipelineExecuteRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 流水线 id（必填）
     */
    private Long pipelineId;

    /**
     * 流水线参数对象，key 为参数名，value 为参数值，例如：
     * <pre>
     * {
     *   "app-name": "go-web-demo",
     *   "branch": "master"
     * }
     * </pre>
     */
    private Map<String, String> parameters;
}
