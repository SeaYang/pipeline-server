package com.ci.pipeline.service.remote;

import lombok.Builder;
import lombok.Data;

/**
 * Pod 日志查询参数，用于对 readNamespacedPodLog 的可选过滤项进行封装。
 * 所有字段均可选；未设置（null）的字段不会作为查询条件传递给 Kubernetes。
 */
@Data
@Builder
public class PodLogQuery {

    /**
     * 容器名称；为空时取 Pod 中唯一的容器（多容器 Pod 必须指定，否则 K8s 会报错）
     */
    private String container;

    /**
     * 是否返回已终止的上一个容器实例的日志（previous），常用于排查崩溃重启的容器
     */
    private Boolean previous;

    /**
     * 仅返回最后 N 行日志（tailLines）
     */
    private Integer tailLines;

    /**
     * 仅返回最近 N 秒的日志（sinceSeconds）。注意：sinceSeconds 与 sinceTime 互斥，此处仅支持 sinceSeconds
     */
    private Integer sinceSeconds;

    /**
     * 限制返回的最大字节数（limitBytes）
     */
    private Integer limitBytes;

    /**
     * 是否在每行日志前追加时间戳（timestamps）
     */
    private Boolean timestamps;

    /**
     * 是否以流式方式跟随日志输出（follow）。true 时会阻塞直到日志流结束，默认 false
     */
    @Builder.Default
    private Boolean follow = Boolean.FALSE;

    /**
     * 是否跳过对后端（kubelet）的 TLS 校验（insecureSkipTLSVerifyBackend），默认 false
     */
    @Builder.Default
    private Boolean insecureSkipTLSVerifyBackend = Boolean.FALSE;
}
