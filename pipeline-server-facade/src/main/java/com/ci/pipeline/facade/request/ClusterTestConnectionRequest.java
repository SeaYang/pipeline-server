package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 集群测试连接入参（用表单参数实时探测，不落库；id 有值时复用已保存的 token）
 */
@Data
public class ClusterTestConnectionRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 已保存集群的主键（编辑场景传 id 且 token 留空时，用库中 token 探测）
     */
    private Long id;

    /**
     * Argo Server 地址（必填）
     */
    private String argoUrl;

    /**
     * Argo 认证 token（含 Bearer 前缀；id 有值且此处为空时用库中 token）
     */
    private String argoToken;

    /**
     * K8s API Server 地址（必填）
     */
    private String k8sMasterUrl;

    /**
     * K8s 认证 token（不含 Bearer 前缀；id 有值且此处为空时用库中 token）
     */
    private String k8sToken;

    /**
     * 是否校验 K8s 证书
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
     * 调度准入水位（仅透传校验，不参与探测）
     */
    private BigDecimal freeMemoryThreshold;
}
