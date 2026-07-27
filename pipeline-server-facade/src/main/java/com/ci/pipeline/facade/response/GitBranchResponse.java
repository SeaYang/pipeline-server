package com.ci.pipeline.facade.response;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Git 分支信息响应
 */
@Data
public class GitBranchResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 分支名 */
    private String name;

    /** 最近 commit SHA */
    private String commitId;

    /** 最近 commit message */
    private String commitMessage;

    /** commit 作者 */
    private String authorName;

    /** commit 时间 */
    private Date committedDate;
}
