package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class PipelineTriggerHistoryQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 按流水线 id 过滤 */
    private Long pipelineId;

    /** 按应用名称过滤 */
    private String appName;

    /** 按触发状态过滤：SUCCESS / FAILED */
    private String status;

    /** 按触发类型过滤：如 user、epTestApply */
    private String type;

    /** 页码，从 1 开始 */
    private Long pageNum;

    /** 每页条数 */
    private Long pageSize;
}
