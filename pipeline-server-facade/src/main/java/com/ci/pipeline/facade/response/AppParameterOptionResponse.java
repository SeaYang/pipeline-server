package com.ci.pipeline.facade.response;

import lombok.Data;

import java.io.Serializable;

/**
 * 可配置参数选项响应（用于新增弹框的参数名下拉）
 */
@Data
public class AppParameterOptionResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String label;
    private String componentType;
    private String paramType;

    /** 选项配置 JSON（SELECT/RADIO 用） */
    private String optionConfig;
}
