package com.ci.pipeline.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 任务模板实体（保存任务模板的基础字段定义，对应一个 argo WorkflowTemplate）
 */
@Data
@TableName("task_template")
public class TaskTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 任务模板编码
     */
    private String taskTemplateCode;

    /**
     * 任务模板名称
     */
    private String name;

    /**
     * 详细描述内容
     */
    private String description;

    /**
     * 任务模板所属分组
     */
    private String taskTemplateGroup;

    /**
     * 创建人
     */
    private String creator;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 逻辑删除标识（0-未删除，1-已删除），由 MyBatis-Plus 全局配置接管
     */
    private Integer deleted;
}
