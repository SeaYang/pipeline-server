package com.ci.pipeline.service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Kubernetes API Server 连接配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "kubernetes.client")
public class KubernetesClientProperties {

    /**
     * Kubernetes API Server 地址，例如 https://192.168.10.130:6443
     */
    private String masterUrl;

    /**
     * Authorization Token（不含 "Bearer " 前缀），用于访问 K8s API。
     * 需保证对应 ServiceAccount 具备 pods/log 等权限。
     */
    private String token;

    /**
     * 是否校验 SSL 证书，本地自签名证书环境建议置为 false，默认 false
     */
    private boolean verifyingSsl = false;
}
