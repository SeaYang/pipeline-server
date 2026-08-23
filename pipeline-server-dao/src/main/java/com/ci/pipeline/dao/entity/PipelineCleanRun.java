package com.ci.pipeline.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 流水线终态清理记录（一条业务运行对应一条清理记录，run_name 唯一）
 */
@Data
@TableName("pipeline_clean_run")
public class PipelineCleanRun implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务流水线的 pipelineRunName（Argo Workflow 名称，唯一） */
    private String runName;

    /** 清理流水线的 pipelineRunName（提交成功后回填） */
    private String cleanRunName;

    /** 状态，复用 PipelineRunStatusEnum：Running / Succeeded / Failed */
    private String status;

    /** 清理结束时间（回调/兜底回填） */
    private Date endTime;

    /** 创建时间（即清理触发/开始时间） */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;
}
