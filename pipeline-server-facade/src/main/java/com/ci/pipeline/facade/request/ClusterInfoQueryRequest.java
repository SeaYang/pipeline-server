package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 集群列表查询条件（分页 GET + query 对象绑定）
 */
@Data
public class ClusterInfoQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 集群名（模糊匹配，可为空）
     */
    private String clusterName;

    /**
     * 是否启用（可为空：查全部）
     */
    private Integer enabled;

    /**
     * 是否在线（可为空：查全部）
     */
    private Integer online;

    /**
     * 页码（从 1 开始）
     */
    private Integer pageNum = 1;

    /**
     * 每页条数
     */
    private Integer pageSize = 10;
}
