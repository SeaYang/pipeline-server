package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 流水线实例修改入参（目前仅允许修改 name）
 */
@Data
public class PipelineUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键（必填）
     */
    private Long id;

    /**
     * 流水线名称（仅允许修改该字段）
     */
    private String name;
}
