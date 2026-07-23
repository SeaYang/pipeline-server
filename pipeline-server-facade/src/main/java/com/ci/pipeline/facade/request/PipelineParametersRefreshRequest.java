package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 流水线执行参数刷新请求（参数联动刷新）。
 * <p>当某个标记了 refreshOnChanged 的参数值变动时，前端携带变动参数名和当前所有参数值，
 * 后端重新计算受影响的下游参数（值和选项），返回更新后的参数列表。
 */
@Data
public class PipelineParametersRefreshRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 流水线 id */
    private Long pipelineId;

    /** 变动的参数名 */
    private String changedParamName;

    /** 当前所有参数值（key=参数名，value=参数值） */
    private Map<String, String> currentValues;
}
