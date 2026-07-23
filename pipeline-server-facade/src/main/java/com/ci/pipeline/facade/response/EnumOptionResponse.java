package com.ci.pipeline.facade.response;

import lombok.Data;

import java.io.Serializable;

/**
 * 枚举选项响应（通用），用于前端渲染下拉框。
 */
@Data
public class EnumOptionResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 枚举 code */
    private String code;

    /** 枚举描述 */
    private String description;

    public EnumOptionResponse() {
    }

    public EnumOptionResponse(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
