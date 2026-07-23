package com.ci.pipeline.service.service.sse;

import com.ci.pipeline.common.constants.KubernetesConstants;
import com.ci.pipeline.common.enums.PipelineRunStatusEnum;
import com.ci.pipeline.dao.entity.PipelineRun;
import com.ci.pipeline.dao.entity.PipelineTaskRun;
import com.ci.pipeline.dao.repository.PipelineRunRepository;
import com.ci.pipeline.dao.repository.PipelineTaskRunRepository;
import com.ci.pipeline.facade.response.PipelineRunLogDTO;
import com.ci.pipeline.service.config.ArgoServerProperties;
import com.ci.pipeline.service.remote.KubernetesAgent;
import com.ci.pipeline.service.remote.PodLogQuery;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 流水线任务节点日志获取服务。
 * <p>根据流水线执行状态选择数据来源：
 * <ul>
 *   <li>非终态（运行中）：调 k8s API 实时获取 Pod 日志</li>
 *   <li>终态（成功/取消）：从 pipeline_task_run.log_content 取已落地的日志</li>
 * </ul>
 *
 * <h3>SSE 日志推送</h3>
 * <p>{@link #watchLog(String, String)} 返回 SseEmitter，在独立线程中持续推送日志：
 * <ul>
 *   <li>终态：从 DB 一次性取日志推送后关闭</li>
 *   <li>非终态：k8s follow=true 流式读取，批量推送，Pod 结束自动关闭</li>
 * </ul>
 */
@Slf4j
@Service
public class PipelineRunLogService {

    /** Pod 日志最多保留行数（终态一次性获取 / 非终态 follow 的 tailLines） */
    private static final int POD_LOG_TAIL_LINES = 5000;

    /** SSE 连接超时时间（30 分钟） */
    private static final long SSE_TIMEOUT = 30 * 60 * 1000L;

    /** 批量推送间隔（毫秒） */
    private static final long BATCH_INTERVAL_MS = 2000;

    /** 批量推送缓冲区上限（字节），超过则立即推送 */
    private static final int BATCH_BUFFER_LIMIT = 10_000;

    /** 心跳间隔（秒），防止代理超时断开 */
    private static final long HEARTBEAT_INTERVAL_SECONDS = 10;

    /** 可查看日志的节点状态：只有这些状态时 Pod 才已就绪，k8s 不会返回 400 */
    private static final Set<String> LOGGABLE_PHASES = new HashSet<>(
            Arrays.asList("Running", "Succeeded", "Failed", "Error"));

    /** 全局共享的心跳调度器（daemon 线程，所有 SSE 连接复用） */
    private static final ScheduledExecutorService HEARTBEAT_SCHEDULER;
    static {
        HEARTBEAT_SCHEDULER = new ScheduledThreadPoolExecutor(2, r -> {
            Thread t = new Thread(r, "sse-log-heartbeat");
            t.setDaemon(true);
            return t;
        });
        ((ScheduledThreadPoolExecutor) HEARTBEAT_SCHEDULER).setRemoveOnCancelPolicy(true);
    }

    @Autowired
    private PipelineRunRepository pipelineRunRepository;

    @Autowired
    private PipelineTaskRunRepository pipelineTaskRunRepository;

    @Autowired
    private KubernetesAgent kubernetesAgent;

    @Autowired
    private ArgoServerProperties argoServerProperties;

    @Autowired
    private PipelineRunSseService pipelineRunSseService;

    @Autowired
    @Qualifier("pipelineLogWatchExecutor")
    private ThreadPoolTaskExecutor logWatchExecutor;

    // ====== HTTP 一次性获取（保留给非 SSE 场景） ======

    /**
     * 获取任务节点日志（一次性返回全部）。
     *
     * @param pipelineRunName 流水线执行名称
     * @param taskCode        任务节点编码
     * @return 日志文本，无日志返回 null
     */
    public String getTaskLog(String pipelineRunName, String taskCode) {
        PipelineRun run = pipelineRunRepository.selectByName(pipelineRunName);
        if (run == null) {
            return null;
        }

        PipelineRunStatusEnum status = PipelineRunStatusEnum.ofCode(run.getStatus());
        boolean terminal = status != null && status.isTerminal();

        if (terminal) {
            PipelineTaskRun taskRun = pipelineTaskRunRepository.selectByRunIdAndTaskCode(run.getId(), taskCode);
            return taskRun != null ? taskRun.getLogContent() : null;
        }

        return fetchLiveLog(run, taskCode);
    }

    // ====== SSE 流式推送 ======
    /**
     * SSE 订阅任务节点日志，服务端持续推送增量日志。
     *
     * @param pipelineRunName 流水线执行名称
     * @param taskCode        任务节点编码
     * @return SseEmitter
     */
    public SseEmitter watchLog(String pipelineRunName, String taskCode) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        AtomicBoolean running = new AtomicBoolean(true);

        emitter.onCompletion(() -> running.set(false));
        emitter.onTimeout(() -> {
            running.set(false);
            emitter.complete();
        });
        emitter.onError(e -> {
            running.set(false);
            log.debug("日志 SSE 连接异常, pipelineRunName={}, taskCode={}", pipelineRunName, taskCode, e);
        });

        logWatchExecutor.execute(() -> pushLogLoop(pipelineRunName, taskCode, emitter, running));

        return emitter;
    }

    /**
     * 日志推送主循环。
     */
    private void pushLogLoop(String pipelineRunName, String taskCode, SseEmitter emitter, AtomicBoolean running) {
        InputStream logStream = null;
        log.info("日志 SSE 推送任务启动, pipelineRunName={}, taskCode={}", pipelineRunName, taskCode);
        try {
            // 先发一个注释事件，降低首字节延迟
            emitter.send(SseEmitter.event().comment("connected"));

            PipelineRun run = pipelineRunRepository.selectByName(pipelineRunName);
            if (run == null) {
                sendLogBatch(emitter, "流水线执行记录不存在", true);
                return;
            }

            PipelineRunStatusEnum status = PipelineRunStatusEnum.ofCode(run.getStatus());
            boolean terminal = status != null && status.isTerminal();

            if (terminal) {
                // 终态：从 DB 一次性取日志
                PipelineTaskRun taskRun = pipelineTaskRunRepository.selectByRunIdAndTaskCode(run.getId(), taskCode);
                String logContent = taskRun != null ? taskRun.getLogContent() : null;
                sendLogBatch(emitter, logContent != null ? logContent : "（暂无日志）", true);
                return;
            }

            // 非终态：从 snapshot 解析节点状态和 podName
            // 先检查节点 phase：只有 Running / Succeeded / Failed / Error 才有日志可查，
            // Pending 时 Pod 还在初始化（PodInitializing），k8s 会返回 400
            String nodePhase = resolveNodePhase(run, taskCode);
            if (!LOGGABLE_PHASES.contains(nodePhase)) {
                sendLogBatch(emitter, "节点尚未开始执行或正在初始化，暂无日志", true);
                return;
            }

            String podName = resolvePodName(run, taskCode);
            if (podName == null) {
                sendLogBatch(emitter, "节点尚未产生运行实例，暂无日志", true);
                return;
            }

            // k8s follow=true 流式获取
            String namespace = argoServerProperties.getNamespace();
            logStream = kubernetesAgent.streamPodLog(namespace, podName,
                    KubernetesConstants.DEFAULT_LOG_CONTAINER, POD_LOG_TAIL_LINES);

            // 逐行读取 + 批量推送 + 心跳
            streamAndPush(logStream, emitter, running);

        } catch (Exception e) {
            log.error("日志 SSE 推送异常, pipelineRunName={}, taskCode={}", pipelineRunName, taskCode, e);
            sendLogBatch(emitter, "获取日志失败: " + e.getMessage(), true);
        } finally {
            if (logStream != null) {
                try { logStream.close(); } catch (IOException e) { log.debug("关闭日志流异常", e); }
            }
            emitter.complete();
        }
    }

    /**
     * 逐行读取 k8s 日志流，批量推送到 SSE，同时定时发心跳。
     */
    private void streamAndPush(InputStream logStream, SseEmitter emitter, AtomicBoolean running) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(logStream, StandardCharsets.UTF_8));
        StringBuilder batchBuffer = new StringBuilder();
        long lastSendTime = System.currentTimeMillis();
        final long heartbeatIntervalMs = HEARTBEAT_INTERVAL_SECONDS * 1000L;
        final java.util.concurrent.atomic.AtomicLong lastActivityTime =
                new java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis());

        // 启动心跳定时任务
        java.util.concurrent.ScheduledFuture<?> heartbeatTask =
                HEARTBEAT_SCHEDULER.scheduleAtFixedRate(() -> {
                    try {
                        if (System.currentTimeMillis() - lastActivityTime.get() >= heartbeatIntervalMs) {
                            emitter.send(SseEmitter.event().comment("ping"));
                        }
                    } catch (Exception e) {
                        log.debug("日志 SSE 心跳发送失败（连接可能已断开）");
                    }
                }, heartbeatIntervalMs, heartbeatIntervalMs, TimeUnit.MILLISECONDS);

        try {
            String line;
            while (running.get() && (line = reader.readLine()) != null) {
                batchBuffer.append(line).append("\n");

                long now = System.currentTimeMillis();
                if (now - lastSendTime >= BATCH_INTERVAL_MS || batchBuffer.length() > BATCH_BUFFER_LIMIT) {
                    sendLogBatch(emitter, batchBuffer.toString(), false);
                    batchBuffer.setLength(0);
                    lastSendTime = now;
                    lastActivityTime.set(now);
                }
            }

            // 推送剩余日志并标记完成
            if (batchBuffer.length() > 0) {
                sendLogBatch(emitter, batchBuffer.toString(), true);
            } else {
                sendLogBatch(emitter, "", true);
            }
        } finally {
            heartbeatTask.cancel(false);
        }
    }

    // ====== 工具方法 ======

    /**
     * 发送一批日志到 SSE。
     */
    private void sendLogBatch(SseEmitter emitter, String content, boolean completed) {
        try {
            PipelineRunLogDTO dto = new PipelineRunLogDTO(content, completed);
            emitter.send(SseEmitter.event().name("log").data(dto));
        } catch (IOException e) {
            log.debug("日志 SSE 发送失败（连接可能已断开）");
        }
    }

    /**
     * 非终态：从 snapshot 中解析节点信息，拼出 podName，调 k8s 获取实时日志。
     */
    private String fetchLiveLog(PipelineRun run, String taskCode) {
        try {
            // Pending 时 Pod 还在初始化，k8s 会返回 400，先检查节点状态
            String nodePhase = resolveNodePhase(run, taskCode);
            if (!LOGGABLE_PHASES.contains(nodePhase)) {
                return null;
            }
            String podName = resolvePodName(run, taskCode);
            if (podName == null || podName.isEmpty()) {
                log.info("节点尚未产生运行实例，无法获取日志, pipelineRunName={}, taskCode={}", run.getName(), taskCode);
                return null;
            }
            return kubernetesAgent.getPodLog(argoServerProperties.getNamespace(), podName,
                    PodLogQuery.builder()
                            .container(KubernetesConstants.DEFAULT_LOG_CONTAINER)
                            .tailLines(POD_LOG_TAIL_LINES)
                            .build());
        } catch (Exception e) {
            log.warn("获取实时 Pod 日志失败, pipelineRunName={}, taskCode={}", run.getName(), taskCode, e);
            return null;
        }
    }

    /**
     * 从 workflowDetail（snapshot）中解析 taskCode 对应节点的 podName。
     */
    private String resolvePodName(PipelineRun run, String taskCode) {
        try {
            JsonNode workflowDetail = pipelineRunSseService.buildDetailDTO(run).getWorkflowDetail();
            if (workflowDetail == null) {
                return null;
            }
            JsonNode nodes = workflowDetail.path("status").path("nodes");
            for (JsonNode node : nodes) {
                String displayName = node.path("displayName").asText("");
                String nodeName = node.path("name").asText("");
                if (taskCode.equals(displayName) || nodeName.endsWith("." + taskCode)) {
                    return extractPodName(workflowDetail, node);
                }
            }
            return null;
        } catch (Exception e) {
            log.warn("解析 podName 失败, pipelineRunName={}, taskCode={}", run.getName(), taskCode, e);
            return null;
        }
    }

    /**
     * 从 workflowDetail（snapshot）中解析 taskCode 对应节点的 phase。
     * 用于判断节点是否已就绪（Pending 时 Pod 还在初始化，不能查日志）。
     */
    private String resolveNodePhase(PipelineRun run, String taskCode) {
        try {
            JsonNode workflowDetail = pipelineRunSseService.buildDetailDTO(run).getWorkflowDetail();
            if (workflowDetail == null) {
                return null;
            }
            JsonNode nodes = workflowDetail.path("status").path("nodes");
            for (JsonNode node : nodes) {
                String displayName = node.path("displayName").asText("");
                String nodeName = node.path("name").asText("");
                if (taskCode.equals(displayName) || nodeName.endsWith("." + taskCode)) {
                    return node.path("phase").asText(null);
                }
            }
            return null;
        } catch (Exception e) {
            log.warn("解析节点 phase 失败, pipelineRunName={}, taskCode={}", run.getName(), taskCode, e);
            return null;
        }
    }

    /**
     * 从 workflowDetail JSON 和节点 JSON 中，按 pod-name-format v2 规则拼出 podName。
     */
    private String extractPodName(JsonNode workflowDetail, JsonNode node) {
        String wfName = workflowDetail.path("metadata").path("name").asText("");
        String nodeId = node.path("id").asText("");
        if (wfName.isEmpty() || nodeId.isEmpty()) {
            return null;
        }
        String template = node.path("templateRef").path("template").asText(null);
        if (template == null || template.isEmpty()) {
            template = node.path("templateName").asText("");
        }
        String suffix = nodeId.startsWith(wfName) ? nodeId.substring(wfName.length()) : ("-" + nodeId);
        return wfName + "-" + template + suffix;
    }
}
