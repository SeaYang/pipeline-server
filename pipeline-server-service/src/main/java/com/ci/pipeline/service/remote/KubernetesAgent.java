package com.ci.pipeline.service.remote;

import java.io.InputStream;

/**
 * Kubernetes 集群操作接口
 */
public interface KubernetesAgent {

    /**
     * 根据 namespace 和 Pod 名称获取日志（取 Pod 中唯一的容器，多容器 Pod 请使用带 container 的重载）
     *
     * @param namespace 命名空间
     * @param podName   Pod 名称
     * @return Pod 日志内容
     */
    String getPodLog(String namespace, String podName);

    /**
     * 根据 namespace、Pod 名称和容器名称获取日志
     *
     * @param namespace 命名空间
     * @param podName   Pod 名称
     * @param container 容器名称
     * @return Pod 日志内容
     */
    String getPodLog(String namespace, String podName, String container);

    /**
     * 根据 namespace、Pod 名称和查询条件获取日志（支持 tailLines / previous / sinceSecond 等）
     *
     * @param namespace 命名空间
     * @param podName   Pod 名称
     * @param query     日志查询条件，为 null 时等价于 {@link #getPodLog(String, String)}
     * @return Pod 日志内容
     */
    String getPodLog(String namespace, String podName, PodLogQuery query);

    /**
     * 流式获取 Pod 日志（follow=true），返回 InputStream 供调用方逐行读取。
     * <p>底层通过 OkHttp 直接请求 k8s API 的 log endpoint，保持长连接持续接收日志。
     * 调用方负责在读取完毕或发生异常时关闭 InputStream。
     *
     * @param namespace 命名空间
     * @param podName   Pod 名称
     * @param container 容器名称
     * @param tailLines 先获取最后 N 行历史日志，再 follow；null 表示不限制
     * @return 日志输入流
     */
    InputStream streamPodLog(String namespace, String podName, String container, Integer tailLines);
}
