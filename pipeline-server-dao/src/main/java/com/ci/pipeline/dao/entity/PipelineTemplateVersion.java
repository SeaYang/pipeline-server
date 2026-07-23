package com.ci.pipeline.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 流水线模板版本实体（保存流水线模板某个版本的详情，对应 argo WorkflowTemplate 的 json 字符串）
 */
@Data
@TableName("pipeline_template_version")
public class PipelineTemplateVersion implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 模板编码，和 pipeline_template 的 pipeline_template_code 对应
     */
    private String pipelineTemplateCode;

    /**
     * 模板版本号，如 1.0.1
     */
    private String version;

    /**
     * 模板状态：DRAFT / EFFECTIVE / EXPIRED
     */
    private String status;

    /**
     * 流水线模板详情，对应 argo WorkflowTemplate 的 json/yml 字符串
     */
    private String templateDetail;

    /**
     * 版本变更说明
     */
    private String changeNote;

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
