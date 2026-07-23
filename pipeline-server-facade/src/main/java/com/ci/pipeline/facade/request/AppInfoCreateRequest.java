package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 应用基础信息新增入参
 */
@Data
public class AppInfoCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 应用名称（必填，未删除记录中唯一），比如：go-web-demo
     */
    private String appName;

    /**
     * 所使用的编程语言或平台（必填）
     */
    private String programmingLanguage;

    /**
     * 应用描述
     */
    private String description;

    /**
     * git 仓库地址，ssh 格式（必填）
     */
    private String gitSshUrl;
}
