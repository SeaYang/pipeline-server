package com.ci.pipeline.service.remote.impl;

import com.ci.pipeline.common.constants.ClusterConstants;
import com.ci.pipeline.service.remote.ClusterClientRegistry;
import com.ci.pipeline.service.remote.KubernetesAgent;
import com.ci.pipeline.service.remote.PodLogQuery;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeList;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kubernetes 集群操作实现类（多集群版：按 clusterName 从注册表获取对应集群的 ApiClient）
 */
@Slf4j
@Component
public class KubernetesAgentImpl implements KubernetesAgent {

    @Autowired
    private ClusterClientRegistry clusterClientRegistry;

    /**
     * 获取指定集群的 CoreV1 API（随用随建）
     */
    private CoreV1Api coreV1Api(String clusterName) {
        ApiClient apiClient = clusterClientRegistry.getKubernetesApiClient(clusterName);
        return new CoreV1Api(apiClient);
    }

    @Override
    public String getPodLog(String clusterName, String namespace, String podName) {
        return getPodLog(clusterName, namespace, podName, (PodLogQuery) null);
    }

    @Override
    public String getPodLog(String clusterName, String namespace, String podName, String container) {
        PodLogQuery query = PodLogQuery.builder().container(container).build();
        return getPodLog(clusterName, namespace, podName, query);
    }

    @Override
    public String getPodLog(String clusterName, String namespace, String podName, PodLogQuery query) {
        PodLogQuery q = query != null ? query : PodLogQuery.builder().build();
        log.info("获取 Pod 日志, clusterName={}, namespace={}, podName={}, container={}, tailLines={}, previous={}",
                clusterName, namespace, podName, q.getContainer(), q.getTailLines(), q.getPrevious());
        try {
            // readNamespacedPodLog 参数顺序与 client-java 14.0.1 一致：
            // (name, namespace, container, follow, insecureSkipTLSVerifyBackend, limitBytes,
            //  pretty, previous, sinceSeconds, tailLines, timestamps)
            String logText = coreV1Api(clusterName).readNamespacedPodLog(
                    podName,
                    namespace,
                    q.getContainer(),
                    q.getFollow(),
                    q.getInsecureSkipTLSVerifyBackend(),
                    q.getLimitBytes(),
                    null,
                    q.getPrevious(),
                    q.getSinceSeconds(),
                    q.getTailLines(),
                    q.getTimestamps());
            log.info("获取 Pod 日志成功, clusterName={}, namespace={}, podName={}", clusterName, namespace, podName);
            return logText;
        } catch (ApiException e) {
            log.error("获取 Pod 日志失败, clusterName={}, namespace={}, podName={}, code={}, body={}",
                    clusterName, namespace, podName, e.getCode(), e.getResponseBody(), e);
            throw new RuntimeException("获取 Pod 日志失败: ", e);
        } catch (Exception e) {
            log.error("获取 Pod 日志失败, clusterName={}, namespace={}, podName={}", clusterName, namespace, podName, e);
            throw new RuntimeException("获取 Pod 日志失败: ", e);
        }
    }

    @Override
    public InputStream streamPodLog(String clusterName, String namespace, String podName, String container, Integer tailLines) {
        log.info("流式获取 Pod 日志, clusterName={}, namespace={}, podName={}, container={}, tailLines={}",
                clusterName, namespace, podName, container, tailLines);
        try {
            ApiClient apiClient = clusterClientRegistry.getKubernetesApiClient(clusterName);
            // 用 ApiClient 的 basePath 和认证信息构建 k8s log API 的 URL
            String basePath = apiClient.getBasePath();
            HttpUrl.Builder urlBuilder = HttpUrl.parse(basePath).newBuilder()
                    .addPathSegment("api")
                    .addPathSegment("v1")
                    .addPathSegment("namespaces")
                    .addPathSegment(namespace)
                    .addPathSegment("pods")
                    .addPathSegment(podName)
                    .addPathSegment("log");
            if (container != null && !container.isEmpty()) {
                urlBuilder.addQueryParameter("container", container);
            }
            urlBuilder.addQueryParameter("follow", "true");
            if (tailLines != null && tailLines > 0) {
                urlBuilder.addQueryParameter("tailLines", String.valueOf(tailLines));
            }

            // 构建 HTTP 请求，复用 ApiClient 的认证头和 SSL 配置
            // kubernetes client-java 的 ApiClient 认证信息存储在 Authentication 对象中
            Request.Builder requestBuilder = new Request.Builder().url(urlBuilder.build());
            io.kubernetes.client.openapi.auth.ApiKeyAuth apiKeyAuth =
                    (io.kubernetes.client.openapi.auth.ApiKeyAuth) apiClient.getAuthentication("BearerToken");
            if (apiKeyAuth != null && apiKeyAuth.getApiKey() != null) {
                String auth = apiKeyAuth.getApiKeyPrefix() != null
                        ? apiKeyAuth.getApiKeyPrefix() + " " + apiKeyAuth.getApiKey()
                        : apiKeyAuth.getApiKey();
                requestBuilder.header("Authorization", auth);
            }

            OkHttpClient httpClient = apiClient.getHttpClient();
            Response response = httpClient.newCall(requestBuilder.build()).execute();
            if (!response.isSuccessful()) {
                String body = response.body() != null ? response.body().string() : "";
                response.close();
                throw new RuntimeException("流式获取 Pod 日志失败: HTTP " + response.code() + ", body=" + body);
            }
            // 调用方负责关闭 InputStream（实际是 ResponseBody 的 source 流）
            return response.body().byteStream();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("流式获取 Pod 日志失败, clusterName={}, namespace={}, podName={}", clusterName, namespace, podName, e);
            throw new RuntimeException("流式获取 Pod 日志失败: ", e);
        }
    }

    @Override
    public List<V1Node> listNodes(String clusterName) {
        log.info("查询集群节点列表, clusterName={}", clusterName);
        try {
            V1NodeList nodeList = coreV1Api(clusterName).listNode(
                    null, null, null, null, null, null, null, null, null, null);
            List<V1Node> nodes = nodeList != null && nodeList.getItems() != null
                    ? nodeList.getItems() : Collections.emptyList();
            log.info("查询集群节点列表成功, clusterName={}, count={}", clusterName, nodes.size());
            return nodes;
        } catch (ApiException e) {
            log.error("查询集群节点列表失败, clusterName={}, code={}, body={}",
                    clusterName, e.getCode(), e.getResponseBody(), e);
            throw new RuntimeException("查询集群节点列表失败: " + e.getResponseBody(), e);
        }
    }

    @Override
    public Map<String, Long> getNodeMemoryUsageBytes(String clusterName) {
        log.info("查询节点内存用量, clusterName={}", clusterName);
        try {
            ApiClient apiClient = clusterClientRegistry.getKubernetesApiClient(clusterName);
            String basePath = apiClient.getBasePath();
            HttpUrl url = HttpUrl.parse(basePath).newBuilder()
                    .addPathSegments(ClusterConstants.NODE_METRICS_API_PATH)
                    .addPathSegment(ClusterConstants.NODE_METRICS_RESOURCE_PLURAL)
                    .build();

            Request.Builder requestBuilder = new Request.Builder().url(url);
            io.kubernetes.client.openapi.auth.ApiKeyAuth apiKeyAuth =
                    (io.kubernetes.client.openapi.auth.ApiKeyAuth) apiClient.getAuthentication("BearerToken");
            if (apiKeyAuth != null && apiKeyAuth.getApiKey() != null) {
                String auth = apiKeyAuth.getApiKeyPrefix() != null
                        ? apiKeyAuth.getApiKeyPrefix() + " " + apiKeyAuth.getApiKey()
                        : apiKeyAuth.getApiKey();
                requestBuilder.header("Authorization", auth);
            }

            OkHttpClient httpClient = apiClient.getHttpClient();
            Response response = httpClient.newCall(requestBuilder.build()).execute();
            if (!response.isSuccessful()) {
                String body = response.body() != null ? response.body().string() : "";
                response.close();
                throw new RuntimeException("查询节点内存用量失败: HTTP " + response.code() + ", body=" + body);
            }
            String body = response.body() != null ? response.body().string() : "";
            response.close();
            return parseNodeMetrics(body);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询节点内存用量失败, clusterName={}", clusterName, e);
            throw new RuntimeException("查询节点内存用量失败: ", e);
        }
    }

    /**
     * 解析 metrics.k8s.io NodeMetricsList JSON：
     * { "items": [ { "metadata": { "name": "node1" }, "usage": { "memory": "1234567Ki" } } ] }
     */
    private Map<String, Long> parseNodeMetrics(String body) {
        Map<String, Long> result = new HashMap<>();
        if (body == null || body.isEmpty()) {
            return result;
        }
        com.alibaba.fastjson.JSONObject root = com.alibaba.fastjson.JSON.parseObject(body);
        com.alibaba.fastjson.JSONArray items = root.getJSONArray("items");
        if (items == null) {
            return result;
        }
        for (int i = 0; i < items.size(); i++) {
            com.alibaba.fastjson.JSONObject item = items.getJSONObject(i);
            com.alibaba.fastjson.JSONObject metadata = item.getJSONObject("metadata");
            com.alibaba.fastjson.JSONObject usage = item.getJSONObject("usage");
            if (metadata == null || usage == null) {
                continue;
            }
            String nodeName = metadata.getString("name");
            String memory = usage.getString("memory");
            if (nodeName == null || memory == null) {
                continue;
            }
            result.put(nodeName, parseK8sMemoryQuantity(memory));
        }
        return result;
    }

    /**
     * 解析 K8s 内存数量格式（如 1234567Ki、1Gi、1024）为字节数
     */
    private long parseK8sMemoryQuantity(String quantity) {
        String value = quantity.trim();
        try {
            if (value.endsWith("Ki")) {
                return (long) (Double.parseDouble(value.substring(0, value.length() - 2)) * 1024);
            }
            if (value.endsWith("Mi")) {
                return (long) (Double.parseDouble(value.substring(0, value.length() - 2)) * 1024 * 1024);
            }
            if (value.endsWith("Gi")) {
                return (long) (Double.parseDouble(value.substring(0, value.length() - 2)) * 1024 * 1024 * 1024);
            }
            if (value.endsWith("Ti")) {
                return (long) (Double.parseDouble(value.substring(0, value.length() - 2)) * 1024L * 1024 * 1024 * 1024);
            }
            if (value.endsWith("K") || value.endsWith("k")) {
                return (long) (Double.parseDouble(value.substring(0, value.length() - 1)) * 1000);
            }
            if (value.endsWith("M")) {
                return (long) (Double.parseDouble(value.substring(0, value.length() - 1)) * 1000 * 1000);
            }
            if (value.endsWith("G")) {
                return (long) (Double.parseDouble(value.substring(0, value.length() - 1)) * 1000 * 1000 * 1000);
            }
            return (long) Double.parseDouble(value);
        } catch (NumberFormatException e) {
            log.warn("解析 K8s 内存数量失败, quantity={}", quantity);
            return 0L;
        }
    }
}
