package com.ci.pipeline.service.config;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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

    /**
     * 定时任务执行线程池。
     * <p>提交方是单线程的 {@code @Scheduled} 扫描线程（见 {@code CronJobScheduler#scan()}），
     * 若使用 {@link ThreadPoolExecutor.CallerRunsPolicy}，一旦线程池打满会导致扫描线程被迫
     * 同步执行任务、阻塞后续扫描，进而影响所有任务的调度；因此改用自定义拒绝策略：
     * 仅记录告警日志并丢弃本次触发，牺牲个别触发换取调度线程本身的可用性。
     */
    @Bean("cronJobExecutor")
    public ThreadPoolTaskExecutor cronJobExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(30);
        executor.setQueueCapacity(20);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("cron-job-");
        executor.setRejectedExecutionHandler(new LoggingDiscardPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setThreadGroupName("cronJobExecutor");
        executor.initialize();
        log.info("定时任务执行线程池初始化完成, core=10, max=30, queue=20");
        return executor;
    }

    /**
     * 集群模板同步线程池：模板发布/删除的多集群并行同步 + 新集群接入的全量模板同步。
     * <p>拒绝策略 CallerRuns：同步退化为调用方线程串行执行，可接受（管理类低频操作）。
     */
    @Bean("clusterSyncExecutor")
    public ThreadPoolTaskExecutor clusterSyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("cluster-sync-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setThreadGroupName("clusterSyncExecutor");
        executor.initialize();
        log.info("集群模板同步线程池初始化完成, core=4, max=8, queue=100");
        return executor;
    }

    /**
     * 内部服务间通信 OkHttpClient，供 {@code InternalHttpClient} 跨实例路由"停止任务"请求使用。
     * 连接超时/读写超时都设置得较短：目标是同机房内部调用，快速失败优于长时间等待。
     */
    @Bean("internalOkHttpClient")
    public OkHttpClient internalOkHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 仅记录日志、丢弃任务的拒绝策略，避免线程池打满时反压到调用方线程（尤其是单线程的调度线程）。
     */
    private static class LoggingDiscardPolicy implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            log.warn("定时任务执行线程池已满，本次触发被丢弃: activeCount={}, queueSize={}",
                    executor.getActiveCount(), executor.getQueue().size());
        }
    }
}
