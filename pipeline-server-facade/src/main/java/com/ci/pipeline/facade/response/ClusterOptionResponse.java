package com.ci.pipeline.facade.response;

import lombok.Data;

import java.io.Serializable;

/**
 * 集群下拉选项响应（模板表单"执行集群"多选框数据源）
 */
@Data
public class ClusterOptionResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 集群名（存储值）
     */
    private String clusterName;

    /**
     * 集群描述（展示名，空时用 clusterName）
     */
    private String description;
}
