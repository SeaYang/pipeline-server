package com.ci.pipeline.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("pipeline_trigger_history")
public class PipelineTriggerHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 应用名称 */
    private String appName;

    /** 流水线id */
    private Long pipelineId;

    /** 流水线执行记录id；触发失败时为 null */
    private Long pipelineRunId;

    /** 事件绑定记录id；手动触发固定为 0 */
    private Long pipelineEventBindId;

    /** 触发状态：SUCCESS / FAILED */
    private String status;

    /** 触发类型：手动触发为 user，事件触发为 eventType */
    private String type;

    /** 触发人 */
    private String creator;

    /** 触发请求的请求体（JSON 字符串） */
    private String requestBody;

    /** 触发失败时的错误信息 */
    private String errorMessage;

    /** 流水线模板编码 */
    private String pipelineTemplateCode;

    /** 流水线模板版本 */
    private String pipelineTemplateVersion;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

    /** 逻辑删除：0-未删除，1-已删除 */
    private Integer deleted;
}
