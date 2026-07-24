package com.ci.pipeline.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 事件与流水线模板的绑定关系（后台配置）。
 * <p>定义"哪种事件可以触发哪些流水线模板"，由管理员通过前端"模板事件配置"页面维护。
 */
@Data
@TableName("pipeline_template_event_bind")
public class PipelineTemplateEventBind implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 事件类型，对应字典 pipeline_event_type 的 dict_key
     */
    private String eventType;

    /**
     * 关联的流水线模板编码
     */
    private String pipelineTemplateCode;

    private String creator;
    private Date createTime;
    private Date updateTime;
    private Integer deleted;
}
