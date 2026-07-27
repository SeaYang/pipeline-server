package com.ci.pipeline.facade.response;

import lombok.Data;

import java.io.Serializable;

/**
 * Git 目录树节点响应
 */
@Data
public class GitTreeNodeResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 文件 / 目录名 */
    private String name;

    /** 完整路径 */
    private String path;

    /** 节点类型：tree（目录）/ blob（文件） */
    private String type;

    /** 文件模式 */
    private String mode;
}
