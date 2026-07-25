package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 制品分页查询请求
 */
@Data
public class ArtifactQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String appName;
    private String name;
    private String gitBranch;
    private String env;
    private String type;
    private String sortField;
    private String sortOrder;
    private Long pageNum;
    private Long pageSize;
}
