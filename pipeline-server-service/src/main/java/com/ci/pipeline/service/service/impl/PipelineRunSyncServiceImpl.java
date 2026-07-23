package com.ci.pipeline.service.service.impl;

import com.ci.pipeline.common.constants.KubernetesConstants;
import com.ci.pipeline.common.enums.PipelineRunStatusEnum;
import com.ci.pipeline.dao.entity.PipelineRun;
import com.ci.pipeline.dao.entity.PipelineTaskRun;
import com.ci.pipeline.dao.repository.PipelineRunRepository;
import com.ci.pipeline.dao.repository.PipelineRunSnapshotRepository;
import com.ci.pipeline.dao.repository.PipelineTaskRunRepository;
import com.ci.pipeline.service.config.ArgoServerProperties;
import com.ci.pipeline.service.config.PipelineRunSyncProperties;
import com.ci.pipeline.service.service.hook.PipelineRunStatusContext;
import com.ci.pipeline.service.service.hook.PipelineRunStatusHook;
import com.ci.pipeline.service.remote.ArgoWorkflowAgent;
import com.ci.pipeline.service.remote.KubernetesAgent;
import com.ci.pipeline.service.remote.PodLogQuery;
import com.ci.pipeline.service.service.PipelineRunSyncService;
import com.ci.pipeline.service.util.ArgoWorkflowUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.argoproj.workflow.models.IoArgoprojWorkflowV1alpha1NodeStatus;
import io.argoproj.workflow.models.IoArgoprojWorkflowV1alpha1Parameter;
import io.argoproj.workflow.models.IoArgoprojWorkflowV1alpha1Workflow;
import io.argoproj.workflow.models.IoArgoprojWorkflowV1alpha1WorkflowStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 流水线执行状态同步引擎实现。
 * <p>核心职责：查 Argo → 对比状态/generation → 乐观锁回写 pipeline_run → 刷新执行详情快照 →
 * 终态落地任务节点记录（含 pod 日志）→ 触发状态变化 Hook。
 */
@Slf4j
@Service
public class PipelineRunSyncServiceImpl implements PipelineRunSyncService {

    /** Pod 日志最多保留行数（避免 longtext 过大与拉取耗时） */
    private static final int POD_LOG_TAIL_LINES = 5000;

    @Autowired
    private PipelineRunRepository pipelineRunRepository;

    @Autowired
    private PipelineRunSnapshotRepository pipelineRunSnapshotRepository;

    @Autowired
    private PipelineTaskRunRepository pipelineTaskRunRepository;

    @Autowired
    private ArgoWorkflowAgent argoWorkflowAgent;

    @Autowired
    private KubernetesAgent kubernetesAgent;

    @Autowired
    private ArgoServerProperties argoServerProperties;

    @Autowired
    private PipelineRunSyncProperties pipelineRunSyncProperties;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlatformTransactionManager transactionManager;

    /**
     * 容器内所有状态变化 Hook；为空时不影响同步主流程
     */
    @Autowired(required = false)
    private List<PipelineRunStatusHook> hooks;

    @Override
    public void syncUntilTerminal(Long pipelineRunId) {
        int maxAttempts = pipelineRunSyncProperties.getMaxSyncAttempts();
        long intervalMillis = Math.max(0, pipelineRunSyncProperties.getSyncIntervalSeconds()) * 1000L;
        // 内存维护上次同步的 generation，初值 -1 表示首轮必刷新
        long lastGeneration = -1L;
        try {
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                boolean terminal;
                try {
                    PipelineRun run = pipelineRunRepository.selectById(pipelineRunId);
                    if (run == null) {
                        log.warn("流水线执行记录不存在，停止同步, pipelineRunId={}", pipelineRunId);
                        return;
                    }
                    PipelineRunStatusEnum current = PipelineRunStatusEnum.ofCode(run.getStatus());
                    if (current != null && current.isArgoStable()) {
                        // Argo 侧已稳定（Succeeded/Cancelled/Failed/Error），不会再自动变更，停止同步
                        // 注意：Failed/Error 虽然不是平台终态（可重试），但 Argo 不会自动变更，
                        //       重试时会重新拉起 syncUntilTerminal
                        return;
                    }
                    if (!StringUtils.hasText(run.getName())) {
                        log.warn("流水线执行记录缺少 Workflow 名称，停止同步, pipelineRunId={}", pipelineRunId);
                        return;
                    }
                    IoArgoprojWorkflowV1alpha1Workflow workflow = argoWorkflowAgent.getWorkflow(
                            argoServerProperties.getNamespace(), run.getName());
                    Long generation = ArgoWorkflowUtil.getGeneration(workflow);
                    log.info("同步轮询 pipelineRunId={}, attempt={}, dbStatus={}, revision={}, argoGeneration={}, lastGeneration={}",
                            pipelineRunId, attempt, run.getStatus(), run.getRevision(), generation, lastGeneration);
                    terminal = applyWorkflow(run, workflow, lastGeneration == -1L ? null : lastGeneration);
                    if (generation != null) {
                        lastGeneration = generation;
                    }
                } catch (Exception e) {
                    // 单次轮询异常（如 Argo 短暂不可用）不中断整个轮询，下一轮重试
                    log.warn("流水线执行状态同步单次轮询异常, pipelineRunId={}, attempt={}/{}, 将重试",
                            pipelineRunId, attempt, maxAttempts, e);
                    terminal = false;
                }
                if (terminal) {
                    log.info("流水线执行状态同步结束（已到终态）, pipelineRunId={}, attempt={}", pipelineRunId, attempt);
                    return;
                }
                try {
                    Thread.sleep(intervalMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("流水线执行状态同步线程被中断, pipelineRunId={}", pipelineRunId);
                    return;
                }
            }
            log.warn("流水线执行状态同步达到最大尝试次数仍未终态, pipelineRunId={}, maxAttempts={}",
                    pipelineRunId, maxAttempts);
        } catch (Throwable t) {
            // 兜底：任何未预期异常都打日志，避免异步线程静默死亡
            log.error("流水线执行状态同步异常退出, pipelineRunId={}", pipelineRunId, t);
        }
    }

    @Override
    public void handleTerminal(Long pipelineRunId) {
        PipelineRun run = pipelineRunRepository.selectById(pipelineRunId);
        if (run == null || !StringUtils.hasText(run.getName())) {
            return;
        }
        try {
            // terminate 是异步操作，调用后 Argo 需要一点时间才把 workflow / 节点状态更新为终态。
            // 此处轮询等待 Argo 侧 phase 稳定（Failed/Error/Succeeded），确保落地时节点状态已更新。
            IoArgoprojWorkflowV1alpha1Workflow workflow = waitForArgoStable(run.getName());
            // 刷新快照 + 落地任务节点记录放在同一事务，保证数据一致性
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                upsertSnapshot(pipelineRunId, workflow);
                landTaskRuns(pipelineRunId, workflow);
            });
        } catch (Exception e) {
            log.warn("终态处理失败, pipelineRunId={}", pipelineRunId, e);
        }
        // 平台直接置终态（Cancelled）的场景：触发 onCancelled hook（argo 驱动的流转在 applyWorkflow 内已分发）
        if (PipelineRunStatusEnum.CANCELLED.getCode().equals(run.getStatus())) {
            dispatchHooks(run, null, PipelineRunStatusEnum.CANCELLED, run.getFailMessage(), run.getDuration());
        }
    }

    /**
     * 轮询等待 Argo Workflow 的 phase 进入稳定态（Failed/Error/Succeeded）。
     * <p>terminate 后 Argo 不会立即把 workflow/节点状态更新为终态，直接查会拿到 Running 的节点。
     * 此方法最多等待 {@code maxWaitSeconds}（默认 30s），每 {@code syncIntervalSeconds} 轮询一次。
     *
     * @param workflowName Argo Workflow 名称
     * @return 最终稳定态的 Workflow 对象
     */
    private IoArgoprojWorkflowV1alpha1Workflow waitForArgoStable(String workflowName) {
        long intervalMillis = Math.max(1, pipelineRunSyncProperties.getSyncIntervalSeconds()) * 1000L;
        int maxWaitSeconds = 30;
        long deadline = System.currentTimeMillis() + maxWaitSeconds * 1000L;
        String namespace = argoServerProperties.getNamespace();
        IoArgoprojWorkflowV1alpha1Workflow workflow = null;
        while (System.currentTimeMillis() < deadline) {
            workflow = argoWorkflowAgent.getWorkflow(namespace, workflowName);
            IoArgoprojWorkflowV1alpha1WorkflowStatus ws = workflow != null ? workflow.getStatus() : null;
            String phase = ws != null ? ws.getPhase() : null;
            PipelineRunStatusEnum status = PipelineRunStatusEnum.ofCode(phase);
            if (status != null && status.isArgoStable()) {
                log.info("Argo Workflow 已进入稳定态, name={}, phase={}", workflowName, phase);
                return workflow;
            }
            log.info("等待 Argo Workflow 进入稳定态, name={}, currentPhase={}", workflowName, phase);
            try {
                Thread.sleep(intervalMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        log.warn("等待 Argo Workflow 稳定态超时（{}s），使用最后一次查询结果, name={}", maxWaitSeconds, workflowName);
        return workflow;
    }

    /**
     * 基于已获取的 Workflow 处理一次：phase 变化则乐观锁回写 pipeline_run + 触发 hook + 终态落地任务节点；
     * phase 或 generation 变化则刷新执行详情快照。
     *
     * @param run           当前执行记录
     * @param workflow      Argo Workflow 对象
     * @param prevGeneration 上次同步的 generation；null 表示无历史、强制刷新
     * @return Argo 当前 phase 是否终态
     */
    private boolean applyWorkflow(PipelineRun run, IoArgoprojWorkflowV1alpha1Workflow workflow, Long prevGeneration) {
        IoArgoprojWorkflowV1alpha1WorkflowStatus status = workflow != null ? workflow.getStatus() : null;
        if (status == null) {
            // 刚提交，Argo 尚未回填 status
            return false;
        }
        PipelineRunStatusEnum target = PipelineRunStatusEnum.ofCode(status.getPhase());
        if (target == null) {
            log.warn("Argo 返回未知状态，跳过本次同步, pipelineRunId={}, phase={}", run.getId(), status.getPhase());
            return false;
        }
        PipelineRunStatusEnum current = PipelineRunStatusEnum.ofCode(run.getStatus());
        boolean phaseChanged = current != target;
        Long generation = ArgoWorkflowUtil.getGeneration(workflow);
        // generation 变化判定：无历史(prevGeneration=null)强制视为变化；否则要求 generation 非空且不等
        boolean genChanged = prevGeneration == null
                || (generation != null && !generation.equals(prevGeneration));

        if (!phaseChanged && !genChanged) {
            // 既无 phase 变化也无 generation 变化，跳过（不做任何回写）
            return target.isArgoStable();
        }

        // ====== 事务保证：快照刷新 + pipeline_run 回写 + 终态落地任务节点，三者原子提交 ======
        // 这样 SSE 线程查到 revision 变化时，snapshot 和 task_run 一定已就绪，不会出现数据不一致。
        PipelineRun update = new PipelineRun();
        update.setId(run.getId());
        update.setRevision(run.getRevision());
        Integer duration = null;
        if (phaseChanged) {
            update.setStatus(target.getCode());
            if (status.getStartedAt() != null) {
                update.setStartTime(Date.from(status.getStartedAt()));
            }
            if (target.isTerminal()) {
                duration = computeDurationSeconds(status.getStartedAt(), status.getFinishedAt());
                update.setDuration(duration);
                if (status.getFinishedAt() != null) {
                    update.setEndTime(Date.from(status.getFinishedAt()));
                }
            }
            if (target.isFailure()) {
                update.setFailType(target.getCode());
                update.setFailMessage(status.getMessage());
            }
        }
        // 用于事务内部传递 phaseChanged / target，lambda 要求 effectively final
        final boolean phaseChangedFinal = phaseChanged;
        final PipelineRunStatusEnum targetFinal = target;
        final Integer durationFinal = duration;
        int[] rowsHolder = new int[1];
        new TransactionTemplate(transactionManager).executeWithoutResult(txStatus -> {
            // 1) 刷新执行详情快照（phase 或 generation 变化都刷新）
            upsertSnapshot(run.getId(), workflow);
            // 2) 乐观锁回写 pipeline_run（WHERE revision = 旧值，命中后 revision 自增）
            rowsHolder[0] = pipelineRunRepository.updateForSync(update);
            // 3) 仅终态（Succeeded / Cancelled）时落地任务节点记录（含 pod 日志）
            if (rowsHolder[0] == 1 && phaseChangedFinal
                    && (targetFinal == PipelineRunStatusEnum.SUCCEEDED || targetFinal == PipelineRunStatusEnum.CANCELLED)) {
                landTaskRuns(run.getId(), workflow);
            }
        });
        int rows = rowsHolder[0];
        if (rows != 1) {
            log.info("流水线执行记录回写未命中（乐观锁冲突或记录已变）, pipelineRunId={}, revision={}",
                    run.getId(), run.getRevision());
            return current != null && current.isArgoStable();
        }
        if (phaseChanged) {
            log.info("流水线执行状态变更, pipelineRunId={}, {} -> {}, revision({} -> {}), generation={}",
                    run.getId(), current != null ? current.getCode() : "null", target.getCode(),
                    run.getRevision(), run.getRevision() + 1, generation);
            dispatchHooks(run, current, target, status.getMessage(), duration);
            // 仅 Succeeded / Cancelled 终态落地任务节点记录
            if (target == PipelineRunStatusEnum.SUCCEEDED || target == PipelineRunStatusEnum.CANCELLED) {
                landTaskRuns(run.getId(), workflow);
            }
        } else {
            log.info("流水线执行记录 generation 刷新, pipelineRunId={}, revision({} -> {}), generation={}",
                    run.getId(), run.getRevision(), run.getRevision() + 1, generation);
        }
        return target.isArgoStable();
    }

    /**
     * 刷新执行详情快照（detail 序列化 workflow，结构同前端 go-cicd-workflow.json）。
     */
    private void upsertSnapshot(Long pipelineRunId, IoArgoprojWorkflowV1alpha1Workflow workflow) {
        try {
            String detail = objectMapper.writeValueAsString(workflow);
            pipelineRunSnapshotRepository.upsertDetail(String.valueOf(pipelineRunId), detail);
        } catch (Exception e) {
            log.warn("执行详情快照刷新失败, pipelineRunId={}", pipelineRunId, e);
        }
    }

    /**
     * 终态落地任务节点记录：解析 Pod 节点 → 「先删后插」。
     * <p>不单独开事务，由调用方（applyWorkflow / handleTerminal）的外层事务统一管理。
     */
    private void landTaskRuns(Long pipelineRunId, IoArgoprojWorkflowV1alpha1Workflow workflow) {
        List<IoArgoprojWorkflowV1alpha1NodeStatus> podNodes = ArgoWorkflowUtil.listPodNodes(workflow);
        if (podNodes.isEmpty()) {
            log.info("无 Pod 节点，跳过任务节点落地, pipelineRunId={}", pipelineRunId);
            return;
        }
        String namespace = argoServerProperties.getNamespace();
        pipelineTaskRunRepository.deleteByPipelineRunId(pipelineRunId);
        List<PipelineTaskRun> list = new ArrayList<>(podNodes.size());
        for (IoArgoprojWorkflowV1alpha1NodeStatus node : podNodes) {
            list.add(buildTaskRun(pipelineRunId, namespace, workflow, node));
        }
        pipelineTaskRunRepository.batchInsert(list);
        log.info("任务节点记录落地完成, pipelineRunId={}, count={}", pipelineRunId, list.size());
    }

    /**
     * 由单个 Pod 节点构建任务节点记录。
     */
    private PipelineTaskRun buildTaskRun(Long pipelineRunId, String namespace,
                                         IoArgoprojWorkflowV1alpha1Workflow workflow, IoArgoprojWorkflowV1alpha1NodeStatus node) {
        PipelineTaskRun taskRun = new PipelineTaskRun();
        taskRun.setPipelineRunId(pipelineRunId);
        taskRun.setTaskCode(ArgoWorkflowUtil.templateCodeOf(node));
        taskRun.setStatus(node.getPhase());
        taskRun.setInputs(toNameValueJson(node.getInputs() != null ? node.getInputs().getParameters() : null));
        taskRun.setOutputs(toNameValueJson(node.getOutputs() != null ? node.getOutputs().getParameters() : null));
        // pod-name-format v2 下 Pod 名 ≠ node.id，需按 {workflowName}-{template}-{suffix} 拼接
        String podName = ArgoWorkflowUtil.getPodName(workflow, node);
        taskRun.setPodName(podName);
        taskRun.setLogContent(fetchPodLogBestEffort(namespace, podName));
        Instant startedAt = node.getStartedAt();
        Instant finishedAt = node.getFinishedAt();
        if (startedAt != null) {
            taskRun.setStartTime(Date.from(startedAt));
        }
        if (finishedAt != null) {
            taskRun.setEndTime(Date.from(finishedAt));
        }
        taskRun.setDuration(computeDurationSeconds(startedAt, finishedAt));
        taskRun.setRunHostName(node.getHostNodeName());
        return taskRun;
    }

    /**
     * best-effort 拉取 pod 日志（仅取最后 N 行）；失败返回 null，不阻断任务节点落地。
     */
    private String fetchPodLogBestEffort(String namespace, String podName) {
        if (!StringUtils.hasText(podName)) {
            return null;
        }
        try {
            // Argo Pod 含 main / wait 多容器，必须指定 container，否则 k8s 报 400（与 DemoController 一致：container=main）
            return kubernetesAgent.getPodLog(namespace, podName,
                    PodLogQuery.builder()
                            .container(KubernetesConstants.DEFAULT_LOG_CONTAINER)
                            .tailLines(POD_LOG_TAIL_LINES)
                            .build());
        } catch (Exception e) {
            log.warn("获取 Pod 日志失败（忽略），namespace={}, podName={}", namespace, podName, e);
            return null;
        }
    }

    /**
     * 把参数列表序列化为 {@code [{"name":..,"value":..}]} 的 JSON 字符串。
     */
    private String toNameValueJson(List<IoArgoprojWorkflowV1alpha1Parameter> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return null;
        }
        List<Map<String, String>> list = new ArrayList<>(parameters.size());
        for (IoArgoprojWorkflowV1alpha1Parameter param : parameters) {
            if (param == null) {
                continue;
            }
            Map<String, String> item = new LinkedHashMap<>(2);
            item.put("name", param.getName());
            item.put("value", param.getValue());
            list.add(item);
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * 按新状态分发到对应 Hook，逐个 try/catch，单个 Hook 异常不影响同步主流程。
     */
    private void dispatchHooks(PipelineRun run, PipelineRunStatusEnum previous, PipelineRunStatusEnum current,
                               String failMessage, Integer duration) {
        if (hooks == null || hooks.isEmpty()) {
            return;
        }
        PipelineRunStatusContext context = PipelineRunStatusContext.builder()
                .pipelineRunId(run.getId())
                .pipelineId(run.getPipelineId())
                .appName(run.getAppName())
                .name(run.getName())
                .previousStatus(previous)
                .currentStatus(current)
                .failMessage(failMessage)
                .duration(duration)
                .build();
        for (PipelineRunStatusHook hook : hooks) {
            try {
                switch (current) {
                    case PENDING:
                        hook.onPending(context);
                        break;
                    case RUNNING:
                        hook.onRunning(context);
                        break;
                    case SUCCEEDED:
                        hook.onSucceeded(context);
                        break;
                    case FAILED:
                        hook.onFailed(context);
                        break;
                    case ERROR:
                        hook.onError(context);
                        break;
                    case UNKNOWN:
                        hook.onUnknown(context);
                        break;
                    case CANCELLED:
                        hook.onCancelled(context);
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                log.warn("状态变化 Hook 执行异常, hook={}, pipelineRunId={}, status={}",
                        hook.getClass().getSimpleName(), run.getId(), current.getCode(), e);
            }
        }
    }

    /**
     * 由 startedAt / finishedAt 计算执行时长（秒）；任一缺失返回 null。
     */
    private Integer computeDurationSeconds(Instant startedAt, Instant finishedAt) {
        if (startedAt == null || finishedAt == null) {
            return null;
        }
        long seconds = Duration.between(startedAt, finishedAt).getSeconds();
        return seconds > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) seconds;
    }
}
