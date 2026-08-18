package com.ci.pipeline.service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * Kubernetes ApiClient 配置类（已废弃）。
 * <p>多集群改造后 K8s ApiClient 由 {@code ClusterClientRegistry} 按集群构建与管理，
 * 本类不再注册单例 Bean。
 * <p>保留 {@link KubernetesClientProperties}（yml 配置）作为 cluster_info 表为空时的兜底配置来源。
 */
@Slf4j
@Deprecated
@Configuration
public class KubernetesClientConfig {
}
