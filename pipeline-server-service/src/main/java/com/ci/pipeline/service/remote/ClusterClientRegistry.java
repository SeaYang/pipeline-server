package com.ci.pipeline.service.remote;

import com.ci.pipeline.common.constants.ClusterConstants;
import com.ci.pipeline.dao.entity.ClusterInfo;
import com.ci.pipeline.dao.repository.ClusterInfoRepository;
import com.ci.pipeline.service.config.ArgoServerProperties;
import com.ci.pipeline.service.config.KubernetesClientProperties;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import io.argoproj.workflow.ApiClient;
import io.argoproj.workflow.JSON;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 集群客户端注册中心：clusterName → Argo / K8s ApiClient 实例注册表。
 * <p>替代原单例 Bean 模式（ArgoClientConfig / KubernetesClientConfig）：
 * <ul>
 *   <li>按需创建、按集群隔离；</li>
 *   <li>每次获取时按单集群配置指纹比对，变更自动重建，客户端生命周期跟随配置；</li>
 *   <li>不再调用 Configuration.setDefaultApiClient()（JVM 级静态全局，多实例会互相覆盖）。</li>
 * </ul>
 */
@Slf4j
@Component
public class ClusterClientRegistry {

    @Autowired
    private ClusterInfoRepository clusterInfoRepository;

    @Autowired(required = false)
    private ArgoServerProperties argoServerProperties;

    @Autowired(required = false)
    private KubernetesClientProperties kubernetesClientProperties;

    /**
     * clusterName → Argo ApiClient
     */
    private final Map<String, ApiClient> argoClients = new ConcurrentHashMap<>();

    /**
     * clusterName → K8s ApiClient
     */
    private final Map<String, io.kubernetes.client.openapi.ApiClient> k8sClients = new ConcurrentHashMap<>();

    /**
     * 构建各客户端所依据的集群配置指纹（clusterName → 配置内容 hash），用于变更检测
     */
    private final Map<String, String> argoConfigFingerprints = new ConcurrentHashMap<>();
    private final Map<String, String> k8sConfigFingerprints = new ConcurrentHashMap<>();

    /**
     * 获取（或创建）指定集群的 Argo ApiClient。
     *
     * @param clusterName 集群标识
     * @return Argo ApiClient
     */
    public ApiClient getArgoApiClient(String clusterName) {
        ClusterInfo cluster = requireCluster(clusterName);
        String fingerprint = argoFingerprint(cluster);
        return argoClients.compute(clusterName, (name, existing) -> {
            if (existing != null && Objects.equals(fingerprint, argoConfigFingerprints.get(name))) {
                return existing;
            }
            log.info("重建 Argo ApiClient, clusterName={}, fingerprintChanged={}", name, existing != null);
            ApiClient client = buildArgoClient(cluster);
            argoConfigFingerprints.put(name, fingerprint);
            return client;
        });
    }

    /**
     * 获取（或创建）指定集群的 K8s ApiClient。
     *
     * @param clusterName 集群标识
     * @return K8s ApiClient
     */
    public io.kubernetes.client.openapi.ApiClient getKubernetesApiClient(String clusterName) {
        ClusterInfo cluster = requireCluster(clusterName);
        String fingerprint = k8sFingerprint(cluster);
        return k8sClients.compute(clusterName, (name, existing) -> {
            if (existing != null && Objects.equals(fingerprint, k8sConfigFingerprints.get(name))) {
                return existing;
            }
            log.info("重建 Kubernetes ApiClient, clusterName={}, fingerprintChanged={}", name, existing != null);
            io.kubernetes.client.openapi.ApiClient client = buildKubernetesClient(cluster);
            k8sConfigFingerprints.put(name, fingerprint);
            return client;
        });
    }

    /**
     * 查询集群定义：优先查 cluster_info 表；表为空时用 yml 配置合成兜底集群（迁移期安全网）。
     */
    private ClusterInfo requireCluster(String clusterName) {
        List<ClusterInfo> all = clusterInfoRepository.listAll();
        if (all.isEmpty()) {
            return synthesizeFromYml(clusterName);
        }
        return all.stream()
                .filter(c -> clusterName.equals(c.getClusterName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("集群不存在, clusterName=%s", clusterName)));
    }

    /**
     * yml 兜底：cluster_info 表为空时，用 argo.server.* / kubernetes.client.* 合成名为 default 的集群。
     */
    private ClusterInfo synthesizeFromYml(String clusterName) {
        if (!ClusterConstants.FALLBACK_CLUSTER_NAME.equals(clusterName)
                || argoServerProperties == null || kubernetesClientProperties == null) {
            throw new IllegalArgumentException(
                    String.format("集群不存在且无可用的 yml 兜底配置, clusterName=%s", clusterName));
        }
        log.warn("cluster_info 表为空，使用 yml 配置合成兜底集群 default（请在集群管理页面完成集群录入）");
        ClusterInfo fallback = new ClusterInfo();
        fallback.setClusterName(ClusterConstants.FALLBACK_CLUSTER_NAME);
        fallback.setArgoUrl(argoServerProperties.getUrl());
        fallback.setArgoToken(argoServerProperties.getToken());
        fallback.setArgoNamespace(argoServerProperties.getNamespace());
        fallback.setK8sMasterUrl(kubernetesClientProperties.getMasterUrl());
        fallback.setK8sToken(kubernetesClientProperties.getToken());
        fallback.setK8sVerifyingSsl(kubernetesClientProperties.isVerifyingSsl() ? 1 : 0);
        fallback.setConnectTimeoutMs(ClusterConstants.DEFAULT_CONNECT_TIMEOUT_MS);
        fallback.setReadTimeoutMs(ClusterConstants.DEFAULT_READ_TIMEOUT_MS);
        return fallback;
    }

    private String argoFingerprint(ClusterInfo cluster) {
        return String.join("|",
                String.valueOf(cluster.getArgoUrl()),
                String.valueOf(cluster.getArgoToken()),
                String.valueOf(cluster.getConnectTimeoutMs()),
                String.valueOf(cluster.getReadTimeoutMs()));
    }

    private String k8sFingerprint(ClusterInfo cluster) {
        return String.join("|",
                String.valueOf(cluster.getK8sMasterUrl()),
                String.valueOf(cluster.getK8sToken()),
                String.valueOf(cluster.getK8sVerifyingSsl()),
                String.valueOf(cluster.getConnectTimeoutMs()),
                String.valueOf(cluster.getReadTimeoutMs()));
    }

    /**
     * 构建 Argo ApiClient（构建逻辑自原 ArgoClientConfig 迁移：trust-all SSL + Instant 适配器）。
     * 注意：不再调用 Configuration.setDefaultApiClient()。
     */
    private ApiClient buildArgoClient(ClusterInfo cluster) {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(cluster.getArgoUrl());
        apiClient.setApiKey(cluster.getArgoToken());
        apiClient.setVerifyingSsl(false);
        apiClient.setConnectTimeout(nvl(cluster.getConnectTimeoutMs(), ClusterConstants.DEFAULT_CONNECT_TIMEOUT_MS));
        apiClient.setReadTimeout(nvl(cluster.getReadTimeoutMs(), ClusterConstants.DEFAULT_READ_TIMEOUT_MS));

        try {
            TrustManager[] trustAllCerts = trustAllCerts();
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            OkHttpClient newHttpClient = apiClient.getHttpClient().newBuilder()
                    .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true)
                    .connectTimeout(java.time.Duration.ofMillis(
                            nvl(cluster.getConnectTimeoutMs(), ClusterConstants.DEFAULT_CONNECT_TIMEOUT_MS)))
                    .readTimeout(java.time.Duration.ofMillis(
                            nvl(cluster.getReadTimeoutMs(), ClusterConstants.DEFAULT_READ_TIMEOUT_MS)))
                    .build();
            apiClient.setHttpClient(newHttpClient);
        } catch (Exception e) {
            log.error("配置 Argo ApiClient SSL 失败, clusterName={}", cluster.getClusterName(), e);
        }

        registerInstantTypeAdapter(apiClient);
        log.info("Argo ApiClient 初始化完成, clusterName={}, basePath={}", cluster.getClusterName(), cluster.getArgoUrl());
        return apiClient;
    }

    /**
     * 构建 K8s ApiClient（构建逻辑自原 KubernetesClientConfig 迁移）。
     */
    private io.kubernetes.client.openapi.ApiClient buildKubernetesClient(ClusterInfo cluster) {
        io.kubernetes.client.openapi.ApiClient apiClient = new io.kubernetes.client.openapi.ApiClient();
        apiClient.setBasePath(cluster.getK8sMasterUrl());
        apiClient.setApiKey(cluster.getK8sToken());
        apiClient.setApiKeyPrefix("Bearer");
        boolean verifyingSsl = cluster.getK8sVerifyingSsl() != null && cluster.getK8sVerifyingSsl() == 1;
        apiClient.setVerifyingSsl(verifyingSsl);
        apiClient.setConnectTimeout(nvl(cluster.getConnectTimeoutMs(), ClusterConstants.DEFAULT_CONNECT_TIMEOUT_MS));
        apiClient.setReadTimeout(nvl(cluster.getReadTimeoutMs(), ClusterConstants.DEFAULT_READ_TIMEOUT_MS));

        if (!verifyingSsl) {
            try {
                TrustManager[] trustAllCerts = trustAllCerts();
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
                OkHttpClient newHttpClient = apiClient.getHttpClient().newBuilder()
                        .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
                        .hostnameVerifier((hostname, session) -> true)
                        .build();
                apiClient.setHttpClient(newHttpClient);
            } catch (Exception e) {
                log.error("配置 Kubernetes ApiClient SSL 失败, clusterName={}", cluster.getClusterName(), e);
            }
        }

        log.info("Kubernetes ApiClient 初始化完成, clusterName={}, masterUrl={}", cluster.getClusterName(), cluster.getK8sMasterUrl());
        return apiClient;
    }

    private TrustManager[] trustAllCerts() {
        return new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
        };
    }

    /**
     * 在 ApiClient 的 Gson 上注册 java.time.Instant 适配器（逻辑自原 ArgoClientConfig 迁移）。
     */
    private void registerInstantTypeAdapter(ApiClient apiClient) {
        JSON json = apiClient.getJSON();
        Gson gson = json.getGson().newBuilder()
                .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
                .create();
        json.setGson(gson);
    }

    /**
     * Argo 中 metav1.Time 字段以 RFC3339 字符串返回，但 SDK 模型声明为 java.time.Instant，
     * 该适配器负责字符串与 Instant 之间的转换。
     */
    private static class InstantTypeAdapter extends TypeAdapter<Instant> {

        @Override
        public void write(JsonWriter out, Instant value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value.toString());
            }
        }

        @Override
        public Instant read(JsonReader in) throws IOException {
            JsonToken token = in.peek();
            if (token == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            if (token == JsonToken.BEGIN_OBJECT) {
                in.skipValue();
                return null;
            }
            String value = in.nextString();
            try {
                return Instant.parse(value);
            } catch (Exception e) {
                log.warn("解析 Instant 失败, value={}", value, e);
                return null;
            }
        }
    }

    private int nvl(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }
}
