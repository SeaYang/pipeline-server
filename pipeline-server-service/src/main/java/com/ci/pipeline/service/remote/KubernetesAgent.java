package com.ci.pipeline.service.remote;

import io.kubernetes.client.openapi.models.V1Node;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Kubernetes 集群操作接口（多集群版：所有方法以 clusterName 路由到对应集群的 API Server）
 */
public interface KubernetesAgent {

    /**
     * 根据 namespace 和 Pod 名称获取日志（取 Pod 中唯一的容器，多容器 Pod 请使用带 container 的重载）
     *
     * @param clusterName 集群标识
     * @param namespace   命名空间
     * @param podName     Pod 名称
     * @return Pod 日志内容
     */
    String getPodLog(String clusterName, String namespace, String podName);

    /**
     * 根据 namespace、Pod 名称和容器名称获取日志
     *
     * @param clusterName 集群标识
     * @param namespace   命名空间
     * @param podName     Pod 名称
     * @param container   容器名称
     * @return Pod 日志内容
     */
    String getPodLog(String clusterName, String namespace, String podName, String container);

    /**
     * 根据 namespace、Pod 名称和查询条件获取日志（支持 tailLines / previous / sinceSecond 等）
     *
     * @param clusterName 集群标识
     * @param namespace   命名空间
     * @param podName     Pod 名称
     * @param query       日志查询条件，为 null 时等价于 {@link #getPodLog(String, String, String)}
     * @return Pod 日志内容
     */
    String getPodLog(String clusterName, String namespace, String podName, PodLogQuery query);

    /**
     * 流式获取 Pod 日志（follow=true），返回 InputStream 供调用方逐行读取。
     * <p>底层通过 OkHttp 直接请求 k8s API 的 log endpoint，保持长连接持续接收日志。
     * 调用方负责在读取完毕或发生异常时关闭 InputStream。
     *
     * @param clusterName 集群标识
     * @param namespace   命名空间
     * @param podName     Pod 名称
     * @param container   容器名称
     * @param tailLines   先获取最后 N 行历史日志，再 follow；null 表示不限制
     * @return 日志输入流
     */
    InputStream streamPodLog(String clusterName, String namespace, String podName, String container, Integer tailLines);

    /**
     * 查询集群全部节点（供调度打分使用）
     *
     * @param clusterName 集群标识
     * @return 节点列表
     */
    List<V1Node> listNodes(String clusterName);

    /**
     * 查询各节点内存用量（metrics.k8s.io，由 metrics-server 提供）。
     * <p>metrics 不可用（未部署 / 超时 / 404）时抛出异常，由调用方降级处理。
     *
     * @param clusterName 集群标识
     * @return nodeName → 内存用量字节数
     */
    Map<String, Long> getNodeMemoryUsageBytes(String clusterName);
}
