package com.ci.pipeline.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 事件与 pipeline 实例的绑定关系（系统自动维护）。
 * <p>记录"哪个应用的哪个事件实际绑定了哪条 pipeline"。
 * 事件首次触发某应用时自动创建，后续触发直接复用。
 */
@Data
@TableName("pipeline_event_bind")
public class PipelineEventBind implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联的 pipeline.id
     */
    private Long pipelineId;

    /**
     * 事件类型，对应字典 pipeline_event_type 的 dict_key
     */
    private String eventType;

    /**
     * 应用名称，对应 app_info.app_name
     */
    private String appName;

    /**
     * 流水线模板编码
     */
    private String pipelineTemplateCode;

    private String creator;
    private Date createTime;
    private Date updateTime;
    private Integer deleted;
}
