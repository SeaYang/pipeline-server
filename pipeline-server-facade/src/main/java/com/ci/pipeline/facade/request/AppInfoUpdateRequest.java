package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 应用基础信息修改入参
 */
@Data
public class AppInfoUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键（必填）
     */
    private Long id;

    /**
     * 应用名称（传入时需非空，参与唯一性校验）
     */
    private String appName;

    /**
     * 所使用的编程语言或平台
     */
    private String programmingLanguage;

    /**
     * 应用描述
     */
    private String description;

    /**
     * git 仓库地址，ssh 格式
     */
    private String gitSshUrl;
}
