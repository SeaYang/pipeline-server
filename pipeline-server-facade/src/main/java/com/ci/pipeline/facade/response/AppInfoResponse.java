package com.ci.pipeline.facade.response;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 应用基础信息响应
 */
@Data
public class AppInfoResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 应用名称
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

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
