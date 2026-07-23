package com.ci.pipeline.service.remote.impl;

import com.ci.pipeline.service.remote.KubernetesAgent;
import com.ci.pipeline.service.remote.PodLogQuery;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.InputStream;

/**
 * Kubernetes 集群操作实现类
 */
@Slf4j
@Component
public class KubernetesAgentImpl implements KubernetesAgent {

    @Autowired
    private ApiClient apiClient;

    private CoreV1Api coreV1Api;

    @PostConstruct
    public void init() {
        this.coreV1Api = new CoreV1Api(apiClient);
        log.info("KubernetesAgent 初始化完成");
    }

    @Override
    public String getPodLog(String namespace, String podName) {
        return getPodLog(namespace, podName, (PodLogQuery) null);
    }

    @Override
    public String getPodLog(String namespace, String podName, String container) {
        PodLogQuery query = PodLogQuery.builder().container(container).build();
        return getPodLog(namespace, podName, query);
    }

    @Override
    public String getPodLog(String namespace, String podName, PodLogQuery query) {
        PodLogQuery q = query != null ? query : PodLogQuery.builder().build();
        log.info("获取 Pod 日志, namespace={}, podName={}, container={}, tailLines={}, previous={}",
                namespace, podName, q.getContainer(), q.getTailLines(), q.getPrevious());
        try {
            // readNamespacedPodLog 参数顺序与 client-java 14.0.1 一致：
            // (name, namespace, container, follow, insecureSkipTLSVerifyBackend, limitBytes,
            //  pretty, previous, sinceSeconds, tailLines, timestamps)
            String logText = coreV1Api.readNamespacedPodLog(
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
            log.info("获取 Pod 日志成功, namespace={}, podName={}", namespace, podName);
            return logText;
        } catch (ApiException e) {
            log.error("获取 Pod 日志失败, namespace={}, podName={}, code={}, body={}",
                    namespace, podName, e.getCode(), e.getResponseBody(), e);
            throw new RuntimeException("获取 Pod 日志失败: ", e);
        } catch (Exception e) {
            log.error("获取 Pod 日志失败, namespace={}, podName={}", namespace, podName, e);
            throw new RuntimeException("获取 Pod 日志失败: ", e);
        }
    }

    @Override
    public InputStream streamPodLog(String namespace, String podName, String container, Integer tailLines) {
        log.info("流式获取 Pod 日志, namespace={}, podName={}, container={}, tailLines={}",
                namespace, podName, container, tailLines);
        try {
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
            log.error("流式获取 Pod 日志失败, namespace={}, podName={}", namespace, podName, e);
            throw new RuntimeException("流式获取 Pod 日志失败: ", e);
        }
    }
}
