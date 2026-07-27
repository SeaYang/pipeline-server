package com.ci.pipeline.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 应用基础信息实体（保存应用名称、编程语言、git 仓库地址等基础信息）
 */
@Data
@TableName("app_info")
public class AppInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 应用名称（未删除记录中唯一），比如：go-web-demo
     */
    private String appName;

    /**
     * 所使用的编程语言或平台
     */
    private String programmingLanguage;

    /**
     * 应用描述，比如是干嘛的，什么领域
     */
    private String description;

    /**
     * git 仓库地址，ssh 格式
     */
    private String gitSshUrl;

    /**
     * GitLab 仓库 ID
     */
    private Long repoId;

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
