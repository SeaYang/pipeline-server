package com.ci.pipeline.service.config;

import io.kubernetes.client.openapi.ApiClient;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

/**
 * Kubernetes ApiClient 配置类
 */
@Slf4j
@Configuration
public class KubernetesClientConfig {

    @Bean
    public ApiClient kubernetesApiClient(KubernetesClientProperties properties) {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(properties.getMasterUrl());
        // Bearer Token 认证：setApiKey + setApiKeyPrefix 最终生成 "Authorization: Bearer <token>"
        apiClient.setApiKey(properties.getToken());
        apiClient.setApiKeyPrefix("Bearer");
        apiClient.setVerifyingSsl(properties.isVerifyingSsl());

        // 信任所有证书，适用于本地开发自签名证书环境
        if (!properties.isVerifyingSsl()) {
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
                log.error("配置 Kubernetes ApiClient SSL 失败", e);
            }
        }

        log.info("Kubernetes ApiClient 初始化完成, masterUrl={}", properties.getMasterUrl());
        return apiClient;
    }
}
