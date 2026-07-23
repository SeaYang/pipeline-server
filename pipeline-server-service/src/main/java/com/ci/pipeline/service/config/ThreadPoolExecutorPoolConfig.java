package com.ci.pipeline.service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置。
 * <p>参考 {@code flash-service} 的 {@code ThreadPoolExecutorPoolConfig}：统一使用
 * {@link ThreadPoolTaskExecutor} + {@link ThreadPoolExecutor.CallerRunsPolicy}。
 * 本场景下异步任务为「轮询 Argo 到终态」的长任务，但本项目为手动触发的 CI 工具，
 * 并发执行极少，core + queue 几乎不会被打满，CallerRunsPolicy 实际不会触发；
 * 一旦触发也会在线程池侧降级为调用方线程执行，配合兜底同步接口可保证状态最终一致。
 * <p>未引入参考实现里的 {@code TaskDecorator}：它依赖 tuhu MDC / {@code RequestContextHolder}，
 * 而状态同步在异步线程中执行、不持有请求上下文，无需拷贝。
 */
@Slf4j
@Configuration
public class ThreadPoolExecutorPoolConfig {

    /**
     * 流水线执行状态异步同步线程池。
     */
    @Bean("pipelineRunSyncExecutor")
    public ThreadPoolTaskExecutor pipelineRunSyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("pipeline-run-sync-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setThreadGroupName("pipelineRunSyncExecutor");
        executor.initialize();
        log.info("流水线执行状态同步线程池初始化完成, core=5, max=10, queue=200");
        return executor;
    }

    /**
     * 日志 SSE watch 线程池。
     * <p>每个日志 SSE 连接会占用一个线程持续读取 k8s 日志流（follow=true 阻塞），
     * 因此需要独立于状态同步线程池，避免占满影响状态同步。
     */
    @Bean("pipelineLogWatchExecutor")
    public ThreadPoolTaskExecutor pipelineLogWatchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(50);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("pipeline-log-watch-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setThreadGroupName("pipelineLogWatchExecutor");
        executor.initialize();
        log.info("日志 SSE watch 线程池初始化完成, core=10, max=20, queue=50");
        return executor;
    }
}
