package com.ci.pipeline.facade.response;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class PipelineTriggerHistoryResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 应用名称 */
    private String appName;

    /** 流水线id */
    private Long pipelineId;

    /** 流水线执行记录id */
    private Long pipelineRunId;

    /** 事件绑定记录id；手动触发为 0 */
    private Long pipelineEventBindId;

    /** 触发状态：SUCCESS / FAILED */
    private String status;

    /** 触发类型 */
    private String type;

    /** 触发人 */
    private String creator;

    /** 触发请求的请求体 */
    private String requestBody;

    /** 触发失败时的错误信息 */
    private String errorMessage;

    /** 流水线模板编码 */
    private String pipelineTemplateCode;

    /** 流水线模板版本 */
    private String pipelineTemplateVersion;

    /** 创建时间 */
    private Date createTime;
}
