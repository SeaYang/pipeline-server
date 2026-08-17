package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 集群新增入参
 */
@Data
public class ClusterInfoCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 集群唯一标识（必填，小写字母数字中划线，创建后不可修改）
     */
    private String clusterName;

    /**
     * 集群描述
     */
    private String description;

    /**
     * Argo Server 地址（必填）
     */
    private String argoUrl;

    /**
     * Argo 认证 token（必填，含 Bearer 前缀）
     */
    private String argoToken;

    /**
     * Workflow / WorkflowTemplate 所在命名空间（默认 argo）
     */
    private String argoNamespace;

    /**
     * K8s API Server 地址（必填）
     */
    private String k8sMasterUrl;

    /**
     * K8s 认证 token（必填，不含 Bearer 前缀）
     */
    private String k8sToken;

    /**
     * 是否校验 K8s 证书（默认 false）
     */
    private Boolean k8sVerifyingSsl;

    /**
     * 连接超时毫秒（默认 5000）
     */
    private Integer connectTimeoutMs;

    /**
     * 读取超时毫秒（默认 10000）
     */
    private Integer readTimeoutMs;

    /**
     * 调度准入水位：平均空闲内存占比低于该值不参与调度（默认 0.2）
     */
    private BigDecimal freeMemoryThreshold;

    /**
     * 运行中 Workflow 数硬上限（可空，不启用）
     */
    private Integer maxRunningWorkflows;

    /**
     * 是否启用（默认 true；false=下线，不调度不同步模板）
     */
    private Boolean enabled;

    /**
     * 是否在线（默认 true；false=摘流，不调度但模板继续同步）
     */
    private Boolean online;

    /**
     * 是否设为默认集群（默认 false，全局唯一）
     */
    private Boolean isDefault;

    /**
     * 是否同步已有模板到新集群（默认 true，保存成功后异步执行）
     */
    private Boolean autoSyncTemplates;
}
