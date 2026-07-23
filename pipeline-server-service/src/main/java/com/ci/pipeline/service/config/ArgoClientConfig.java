package com.ci.pipeline.service.config;

import io.argoproj.workflow.ApiClient;
import io.argoproj.workflow.Configuration;
import io.argoproj.workflow.JSON;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.time.Instant;

/**
 * Argo ApiClient 配置类
 */
@Slf4j
@org.springframework.context.annotation.Configuration
public class ArgoClientConfig {

    @Bean
    public ApiClient argoApiClient(ArgoServerProperties properties) {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(properties.getUrl());
        apiClient.setApiKey(properties.getToken());
        apiClient.setVerifyingSsl(false);

        // 信任所有证书，适用于本地开发自签名证书环境
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
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
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            OkHttpClient newHttpClient = apiClient.getHttpClient().newBuilder()
                    .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true)
                    .build();
            apiClient.setHttpClient(newHttpClient);
        } catch (Exception e) {
            log.error("配置 Argo ApiClient SSL 失败", e);
        }

        // 注册 Instant 类型适配器：Argo 返回的 status.startedAt / finishedAt 等 ISO 字符串
        // 对应 SDK 中的 java.time.Instant 字段，而 argo-client-java 默认未注册该类型的适配器，
        // 会导致反序列化报错 "Expected BEGIN_OBJECT but was STRING"。
        registerInstantTypeAdapter(apiClient);

        Configuration.setDefaultApiClient(apiClient);
        log.info("Argo ApiClient 初始化完成, basePath={}, namespace={}", properties.getUrl(), properties.getNamespace());
        return apiClient;
    }

    /**
     * 在 ApiClient 的 Gson 上注册 java.time.Instant 适配器。
     * 通过 newBuilder() 在保留 SDK 已有适配器（Date/OffsetDateTime/LocalDate/byte[]）的基础上追加。
     */
    private void registerInstantTypeAdapter(ApiClient apiClient) {
        JSON json = apiClient.getJSON();
        Gson gson = json.getGson().newBuilder()
                .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
                .create();
        json.setGson(gson);
    }

    /**
     * Argo 中 metav1.Time 字段（startedAt/finishedAt 等）以 RFC3339 字符串返回，
     * 但 SDK 模型将其声明为 java.time.Instant，这里负责在字符串与 Instant 之间转换。
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
            // 兼容极少数返回对象形式的情况，跳过避免反序列化失败
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
}
