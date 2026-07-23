package com.ci.pipeline.common.constants;

/**
 * Kubernetes / Argo 相关常量定义
 */
public final class KubernetesConstants {

    private KubernetesConstants() {
    }

    /**
     * Argo Workflow 所在命名空间
     */
    public static final String ARGO_NAMESPACE = "argo";

    /**
     * Pod 默认读取日志的容器名称
     */
    public static final String DEFAULT_LOG_CONTAINER = "main";
}
