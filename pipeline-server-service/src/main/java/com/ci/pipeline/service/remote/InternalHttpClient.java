package com.ci.pipeline.service.remote;

import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 内部 HTTP 客户端：多实例部署下，用于把"停止任务"请求路由到执行任务的目标实例。
 * <p>只做尽力通知，调用失败仅记录日志，不影响主流程（DB 状态已在调用方通过 CAS 提前写好）。
 */
@Slf4j
@Component
public class InternalHttpClient {

    /** 内部通信固定端口，与 application.yml 的 server.port 保持一致 */
    private static final int TARGET_PORT = 9000;

    @Autowired
    @Qualifier("internalOkHttpClient")
    private OkHttpClient internalOkHttpClient;

    /**
     * 通知目标实例本地停止指定执行日志对应的任务。
     *
     * @param targetIp 目标实例 IP（来自 cron_job_log.instance_ip）
     * @param logId    执行日志ID
     */
    public void notifyStop(String targetIp, Long logId) throws IOException {
        String url = "http://" + targetIp + ":" + TARGET_PORT + "/internal/cron-job/stop/" + logId;
        RequestBody body = RequestBody.create("", MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder().url(url).post(body).build();
        try (Response response = internalOkHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.warn("通知远程实例停止任务失败: url={}, code={}", url, response.code());
            }
        }
    }
}
