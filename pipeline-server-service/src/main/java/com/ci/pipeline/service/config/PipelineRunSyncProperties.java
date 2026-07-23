package com.ci.pipeline.service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 流水线执行状态同步配置属性。
 */
@Data
@Component
@ConfigurationProperties(prefix = "pipeline.run.sync")
public class PipelineRunSyncProperties {

    /**
     * 异步轮询 Argo 的间隔（秒），仅用于异步线程内的循环 sleep
     */
    private long syncIntervalSeconds = 5;

    /**
     * 兜底同步的陈旧阈值（秒）：执行记录的 update_time 距今超过该值才认为异步同步已失效，允许兜底追赶。
     * <p>实际当中任务运行一段时间不更新是正常的，本项目为简单调试取较小值。
     */
    private long stalenessThresholdSeconds = 60;

    /**
     * 单条执行记录异步轮询的最大尝试次数（防死循环安全帽），达到后停止轮询。
     * <p>默认 720 次，按 5s 间隔约合 1 小时。
     */
    private int maxSyncAttempts = 720;
}
