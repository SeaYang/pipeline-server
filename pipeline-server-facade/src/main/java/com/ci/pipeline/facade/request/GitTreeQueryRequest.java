package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;

/**
 * Git 目录树查询请求（懒加载）
 */
@Data
public class GitTreeQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 应用名称（反查 repoId） */
    private String appName;

    /** 查询路径，空串或 null 表示根目录 */
    private String path;
}
