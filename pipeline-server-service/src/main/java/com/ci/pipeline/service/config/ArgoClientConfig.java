package com.ci.pipeline.service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * Argo ApiClient 配置类（已废弃）。
 * <p>多集群改造后 Argo ApiClient 由 {@code ClusterClientRegistry} 按集群构建与管理，
 * 本类不再注册单例 Bean（原 {@code Configuration.setDefaultApiClient()} 静态全局调用
 * 在多实例场景下会互相覆盖，已随单例模式一并移除）。
 * <p>保留 {@link ArgoServerProperties}（yml 配置）作为 cluster_info 表为空时的兜底配置来源。
 */
@Slf4j
@Deprecated
@Configuration
public class ArgoClientConfig {
}
