package com.ci.pipeline.service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Argo Workflows Server 配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "argo.server")
public class ArgoServerProperties {

    /**
     * Argo Server 地址，例如 https://192.168.10.128:2746
     */
    private String url;

    /**
     * Authorization Token，例如 Bearer xxx
     */
    private String token;

    /**
     * Kubernetes 命名空间，默认 argo
     */
    private String namespace = "argo";
}
