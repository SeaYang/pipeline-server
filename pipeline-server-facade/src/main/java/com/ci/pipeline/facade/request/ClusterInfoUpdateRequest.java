package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 集群修改入参（clusterName 不可修改；token 类字段传空表示不修改）
 */
@Data
public class ClusterInfoUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键（必填）
     */
    private Long id;

    /**
     * 集群描述
     */
    private String description;

    /**
     * Argo Server 地址
     */
    private String argoUrl;

    /**
     * Argo 认证 token（含 Bearer 前缀；传空表示不修改）
     */
    private String argoToken;

    /**
     * Workflow / WorkflowTemplate 所在命名空间
     */
    private String argoNamespace;

    /**
     * K8s API Server 地址
     */
    private String k8sMasterUrl;

    /**
     * K8s 认证 token（不含 Bearer 前缀；传空表示不修改）
     */
    private String k8sToken;

    /**
     * 是否校验 K8s 证书
     */
    private Boolean k8sVerifyingSsl;

    /**
     * 连接超时毫秒
     */
    private Integer connectTimeoutMs;

    /**
     * 读取超时毫秒
     */
    private Integer readTimeoutMs;

    /**
     * 调度准入水位
     */
    private BigDecimal freeMemoryThreshold;

    /**
     * 运行中 Workflow 数硬上限（可空，不启用）
     */
    private Integer maxRunningWorkflows;

    /**
     * 是否启用（false=下线，不调度不同步模板）
     */
    private Boolean enabled;

    /**
     * 是否在线（false=摘流，不调度但模板继续同步）
     */
    private Boolean online;

    /**
     * 是否设为默认集群（全局唯一）
     */
    private Boolean isDefault;
}
