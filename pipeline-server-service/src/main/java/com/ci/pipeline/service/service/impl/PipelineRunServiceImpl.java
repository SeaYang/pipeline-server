package com.ci.pipeline.service.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ci.pipeline.common.auth.UserContext;
import com.ci.pipeline.common.constants.CommonConstants;
import com.ci.pipeline.common.constants.DistributedLockConstants;
import com.ci.pipeline.common.constants.PipelineConcurrencyConstants;
import com.ci.pipeline.common.constants.PipelineConstants;
import com.ci.pipeline.common.enums.PipelineRunStatusEnum;
import com.ci.pipeline.common.exception.BusinessException;
import com.ci.pipeline.common.util.SortUtil;
import com.ci.pipeline.dao.entity.Pipeline;
import com.ci.pipeline.dao.entity.PipelineRun;
import com.ci.pipeline.dao.entity.PipelineRunSnapshot;
import com.ci.pipeline.dao.entity.PipelineTemplateVersion;
import com.ci.pipeline.dao.entity.TaskTemplate;
import com.ci.pipeline.dao.repository.PipelineRunRepository;
import com.ci.pipeline.dao.repository.PipelineRunSnapshotRepository;
import com.ci.pipeline.dao.repository.PipelineTemplateVersionRepository;
import com.ci.pipeline.dao.repository.TaskTemplateRepository;
import com.ci.pipeline.facade.request.PipelineRunQueryRequest;
import com.ci.pipeline.facade.response.PageResponse;
import com.ci.pipeline.facade.response.PipelineRunExecuteDetailResponse;
import com.ci.pipeline.facade.response.PipelineRunResponse;
import com.ci.pipeline.facade.response.PipelineRunSnapshotResponse;
import com.ci.pipeline.service.service.ClusterConfigService;
import com.ci.pipeline.service.config.PipelineRunSyncProperties;
import com.ci.pipeline.service.remote.ArgoWorkflowAgent;
import com.ci.pipeline.service.service.DistributedLockService;
import com.ci.pipeline.service.service.PipelineRunService;
import com.ci.pipeline.service.service.PipelineRunSyncService;
import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.argoproj.workflow.models.IoArgoprojWorkflowV1alpha1Workflow;
import io.argoproj.workflow.models.IoArgoprojWorkflowV1alpha1WorkflowStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 流水线执行记录业务实现
 */
@Slf4j
@Service
public class PipelineRunServiceImpl implements PipelineRunService {

    /**
     * 分页排序字段白名单：出参字段名（camelCase） → 数据库列名（snake_case）
     */
    private static final Map<String, String> SORT_FIELD_MAP;
    static {
        Map<String, String> m = new HashMap<>();
        m.put("id", "id");
        m.put("name", "name");
        m.put("appName", "app_name");
        m.put("pipelineTemplateCode", "pipeline_template_code");
        m.put("pipelineTemplateVersion", "pipeline_template_version");
        m.put("status", "status");
        m.put("duration", "duration");
        m.put("createTime", "create_time");
        m.put("updateTime", "update_time");
        SORT_FIELD_MAP = Collections.unmodifiableMap(m);
    }

    @Autowired
    private PipelineRunRepository pipelineRunRepository;

    @Autowired
    private PipelineRunSnapshotRepository pipelineRunSnapshotRepository;

    @Autowired
    private PipelineTemplateVersionRepository pipelineTemplateVersionRepository;

    @Autowired
    private TaskTemplateRepository taskTemplateRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PipelineRunSyncService pipelineRunSyncService;

    @Autowired
    private ArgoWorkflowAgent argoWorkflowAgent;

    @Autowired
    private ClusterConfigService clusterConfigService;

    @Autowired
    private PipelineRunSyncProperties pipelineRunSyncProperties;

    @Autowired
    private DistributedLockService distributedLockService;

    /**
     * 流水线执行状态异步同步线程池
     */
    @Autowired
    @Qualifier("pipelineRunSyncExecutor")
    private ThreadPoolTaskExecutor pipelineRunSyncExecutor;

    @Override
    public Long createRun(Pipeline pipeline, PipelineTemplateVersion effective,
                          IoArgoprojWorkflowV1alpha1Workflow workflow, Map<String, String> parameters,
                          String clusterName) {
        String workflowName = workflowName(workflow);
        PipelineRun run = new PipelineRun();
        run.setPipelineId(pipeline.getId());
        run.setName(workflowName);
        run.setClusterName(clusterName);
        run.setAppName(pipeline.getAppName());
        run.setPipelineTemplateCode(pipeline.getPipelineTemplateCode());
        run.setPipelineTemplateVersion(effective.getVersion());
        run.setStatus(PipelineRunStatusEnum.PENDING.getCode());
        run.setArguments(serializeArguments(parameters));
        run.setCreator(UserContext.getUserId());
        pipelineRunRepository.insert(run);
        Long runId = run.getId();
        // 紧接着插入首条执行详情快照（无需反查 Argo，直接用提交返回的 workflow）
        saveSnapshot(runId, workflow);
        log.info("流水线执行记录落地成功, pipelineRunId={}, pipelineId={}, name={}, clusterName={}",
                runId, pipeline.getId(), workflowName, clusterName);
        // 触发异步状态同步：insert 已自动提交，异步线程可读到该行
        pipelineRunSyncExecutor.execute(() -> pipelineRunSyncService.syncUntilTerminal(runId));
        return runId;
    }

    @Override
    public PipelineRunResponse getById(Long id) {
        PipelineRun run = pipelineRunRepository.selectById(id);
        if (run == null) {
            throw new BusinessException(PipelineConstants.MSG_PIPELINE_RUN_NOT_EXIST);
        }
        return toResponse(run);
    }

    @Override
    public PageResponse<PipelineRunResponse> page(PipelineRunQueryRequest query) {
        long pageNum = query.getPageNum() == null ? CommonConstants.DEFAULT_PAGE_NUM : query.getPageNum();
        long pageSize = query.getPageSize() == null ? CommonConstants.DEFAULT_PAGE_SIZE : query.getPageSize();
        String sortField = SortUtil.resolveField(query.getSortField(), SORT_FIELD_MAP);
        String sortOrder = sortField != null ? SortUtil.resolveOrder(query.getSortOrder()) : null;
        IPage<PipelineRun> pageResult = pipelineRunRepository.pageQuery(
                pageNum, pageSize, query.getPipelineId(), query.getAppName(), query.getStatus(), sortField, sortOrder);
        List<PipelineRunResponse> records = pageResult.getRecords().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return PageResponse.of(records, pageResult.getTotal(), pageResult.getCurrent(), pageResult.getSize(), pageResult.getPages());
    }

    @Override
    public PipelineRunResponse getLatestByPipelineId(Long pipelineId) {
        if (pipelineId == null) {
            throw new BusinessException(PipelineConstants.MSG_PIPELINE_ID_REQUIRED);
        }
        PipelineRun run = pipelineRunRepository.selectLatestByPipelineId(pipelineId);
        return toResponse(run);
    }

    @Override
    public PipelineRunResponse syncRun(Long id) {
        if (id == null) {
            throw new BusinessException(PipelineConstants.MSG_PIPELINE_RUN_ID_REQUIRED);
        }
        PipelineRun run = pipelineRunRepository.selectById(id);
        if (run == null) {
            throw new BusinessException(PipelineConstants.MSG_PIPELINE_RUN_NOT_EXIST);
        }
        // 1) Argo 侧已稳定（Succeeded/Cancelled/Failed/Error）：不会再自动变更，无需兜底同步
        PipelineRunStatusEnum status = PipelineRunStatusEnum.ofCode(run.getStatus());
        if (status != null && status.isArgoStable()) {
            return toResponse(run);
        }
        // 2) update_time 距今未超陈旧阈值：说明异步同步仍在正常推进，无需重复拉起，直接返回
        long thresholdMillis = pipelineRunSyncProperties.getStalenessThresholdSeconds() * 1000L;
        if (run.getUpdateTime() != null
                && (System.currentTimeMillis() - run.getUpdateTime().getTime()) < thresholdMillis) {
            return toResponse(run);
        }
        // 3) 超过阈值，认为原异步同步已失效（实例下线/发布），重新提交异步同步任务，直到终态
        // 非阻塞加锁，防止并发或连击重复提交同步任务
        String lockKey = DistributedLockConstants.LOCK_KEY_PIPELINE_RUN + id;
        String lockValue = distributedLockService.tryLock(
                lockKey, DistributedLockConstants.DEFAULT_LOCK_EXPIRE_SECONDS, "同步流水线运行状态");
        if (lockValue == null) {
            throw new BusinessException(PipelineConstants.MSG_OPERATION_LOCK_FAILED);
        }
        try {
            log.info("触发兜底状态同步（异步轮询直到终态）, pipelineRunId={}, lastUpdateTime={}", id, run.getUpdateTime());
            pipelineRunSyncExecutor.execute(() -> pipelineRunSyncService.syncUntilTerminal(id));
            return toResponse(run);
        } finally {
            distributedLockService.unlock(lockKey, lockValue);
        }
    }

    @Override
    public PipelineRunResponse retry(Long id) {
        if (id == null) {
            throw new BusinessException(PipelineConstants.MSG_PIPELINE_RUN_ID_REQUIRED);
        }
        PipelineRun run = pipelineRunRepository.selectById(id);
        if (run == null) {
            throw new BusinessException(PipelineConstants.MSG_PIPELINE_RUN_NOT_EXIST);
        }
        // 1) DB 状态必须为失败（Failed / Error）
        PipelineRunStatusEnum dbStatus = PipelineRunStatusEnum.ofCode(run.getStatus());
        if (dbStatus == null || !dbStatus.isFailure()) {
            throw new BusinessException(PipelineConstants.MSG_RUN_RETRY_NOT_FAILED);
        }
        // 2) 实时查询 Argo，状态也必须为失败（Failed / Error）
        String argoPhase = getArgoPhase(run);
        PipelineRunStatusEnum argoStatus = PipelineRunStatusEnum.ofCode(argoPhase);
        if (argoStatus == null || !argoStatus.isFailure()) {
            throw new BusinessException(String.format(PipelineConstants.MSG_RUN_ARGO_RETRY_NOT_FAILED, argoPhase));
        }
        // 非阻塞加锁，防止并发或连击重复重试
        String lockKey = DistributedLockConstants.LOCK_KEY_PIPELINE_RUN + id;
        String lockValue = distributedLockService.tryLock(
                lockKey, DistributedLockConstants.DEFAULT_LOCK_EXPIRE_SECONDS, "重试流水线运行");
        if (lockValue == null) {
            throw new BusinessException(PipelineConstants.MSG_OPERATION_LOCK_FAILED);
        }
        try {
            // 3) 调 Argo 重试（按 run 记录的集群路由）
            try {
                argoWorkflowAgent.retryWorkflow(clusterConfigService.resolveRunClusterName(run), clusterConfigService.resolveRunNamespace(run), run.getName());
            } catch (RuntimeException e) {
                log.error("重试流水线失败, pipelineRunId={}, name={}", id, run.getName(), e);
                throw new BusinessException(String.format(PipelineConstants.MSG_RUN_RETRY_FAILED, e.getMessage()));
            }
            // 4) 状态重置为 Pending（清失败信息），乐观锁
            if (pipelineRunRepository.resetForRetry(run.getId(), run.getRevision()) != 1) {
                throw new BusinessException(PipelineConstants.MSG_RUN_STATE_CHANGED);
            }
            log.info("流水线已重试, pipelineRunId={}, name={}", id, run.getName());
            // 5) 进入异步同步状态逻辑（Pending → Running → 终态）
            pipelineRunSyncExecutor.execute(() -> pipelineRunSyncService.syncUntilTerminal(id));
            return toResponse(pipelineRunRepository.selectById(id));
        } finally {
            distributedLockService.unlock(lockKey, lockValue);
        }
    }

    @Override
    public PipelineRunResponse stop(Long id) {
        if (id == null) {
            throw new BusinessException(PipelineConstants.MSG_PIPELINE_RUN_ID_REQUIRED);
        }
        PipelineRun run = pipelineRunRepository.selectById(id);
        if (run == null) {
            throw new BusinessException(PipelineConstants.MSG_PIPELINE_RUN_NOT_EXIST);
        }
        // 1) DB 状态若已到终态（Succeeded / Cancelled）则不可停止
        PipelineRunStatusEnum dbStatus = PipelineRunStatusEnum.ofCode(run.getStatus());
        if (dbStatus == PipelineRunStatusEnum.SUCCEEDED || dbStatus == PipelineRunStatusEnum.CANCELLED) {
            throw new BusinessException(PipelineConstants.MSG_RUN_STOP_ALREADY_TERMINAL);
        }
        // 2) 实时查询 Argo phase，若已 Succeeded 则不可停止（成功的不允许取消）
        String argoPhase = getArgoPhase(run);
        if (PipelineRunStatusEnum.SUCCEEDED.getCode().equals(argoPhase)) {
            throw new BusinessException(String.format(PipelineConstants.MSG_RUN_ARGO_STOP_NOT_RUNNING, argoPhase));
        }
        // 非阻塞加锁，防止并发或连击重复停止
        String lockKey = DistributedLockConstants.LOCK_KEY_PIPELINE_RUN + id;
        String lockValue = distributedLockService.tryLock(
                lockKey, DistributedLockConstants.DEFAULT_LOCK_EXPIRE_SECONDS, "停止流水线运行");
        if (lockValue == null) {
            throw new BusinessException(PipelineConstants.MSG_OPERATION_LOCK_FAILED);
        }
        try {
            // 3) Argo 未终态（Pending / Running / Unknown）：调 Argo terminate 终止 Pod
            //    Argo 已终态（Failed / Error）：workflow 已 completed，terminate API 会报
            //    "cannot shutdown a completed workflow"，跳过即可——Cancelled 是平台扩展态，
            //    只要 Argo 不是 Succeeded，平台侧都有权将 DB 状态改为 Cancelled。
            PipelineRunStatusEnum argoStatus = PipelineRunStatusEnum.ofCode(argoPhase);
            if (argoStatus == null || !argoStatus.isArgoStable()) {
                try {
                    argoWorkflowAgent.terminateWorkflow(clusterConfigService.resolveRunClusterName(run), clusterConfigService.resolveRunNamespace(run), run.getName());
                } catch (RuntimeException e) {
                    log.error("停止流水线失败, pipelineRunId={}, name={}", id, run.getName(), e);
                    throw new BusinessException(String.format(PipelineConstants.MSG_RUN_STOP_FAILED, e.getMessage()));
                }
            } else {
                log.info("Argo Workflow 已终态({}), 跳过 terminate 直接置 Cancelled, pipelineRunId={}, name={}",
                        argoPhase, id, run.getName());
            }
            // 4) 平台直接置 Cancelled（Cancelled 是平台扩展态，覆盖 Argo 的 Failed/Error 语义为用户主动取消）
            PipelineRun update = new PipelineRun();
            update.setId(run.getId());
            update.setRevision(run.getRevision());
            update.setStatus(PipelineRunStatusEnum.CANCELLED.getCode());
            update.setFailMessage(PipelineConstants.MSG_RUN_STOP_MESSAGE);
            update.setEndTime(new Date());
            if (pipelineRunRepository.updateForSync(update) != 1) {
                throw new BusinessException(PipelineConstants.MSG_RUN_STATE_CHANGED);
            }
            log.info("流水线已停止, pipelineRunId={}, name={}", id, run.getName());
            // 5) 终态处理：再拉一次详情，落地任务节点记录 + 刷新快照
            pipelineRunSyncService.handleTerminal(id);
            return toResponse(pipelineRunRepository.selectById(id));
        } finally {
            distributedLockService.unlock(lockKey, lockValue);
        }
    }

    @Override
    public void stopByConcurrencyReplace(Long id) {
        if (id == null) {
            throw new BusinessException(PipelineConstants.MSG_PIPELINE_RUN_ID_REQUIRED);
        }
        PipelineRun run = pipelineRunRepository.selectById(id);
        if (run == null) {
            throw new BusinessException(PipelineConstants.MSG_PIPELINE_RUN_NOT_EXIST);
        }
        // 已终态（Succeeded / Cancelled）无需替换（可能刚被并发操作终止，额度已释放）
        if (PipelineRunStatusEnum.isTerminalCode(run.getStatus())) {
            log.info("并发替换目标已终态({}), 跳过, pipelineRunId={}", run.getStatus(), id);
            return;
        }
        // 非阻塞加锁，防止与用户手动停止并发冲突
        String lockKey = DistributedLockConstants.LOCK_KEY_PIPELINE_RUN + id;
        String lockValue = distributedLockService.tryLock(
                lockKey, DistributedLockConstants.DEFAULT_LOCK_EXPIRE_SECONDS, "并发替换停止流水线运行");
        if (lockValue == null) {
            throw new BusinessException(PipelineConstants.MSG_OPERATION_LOCK_FAILED);
        }
        try {
            // Argo 未稳定则 terminate；已稳定（Failed / Error）跳过 terminate 直接置 Cancelled
            String argoPhase = getArgoPhase(run);
            PipelineRunStatusEnum argoStatus = PipelineRunStatusEnum.ofCode(argoPhase);
            if (!PipelineRunStatusEnum.SUCCEEDED.getCode().equals(argoPhase)
                    && (argoStatus == null || !argoStatus.isArgoStable())) {
                try {
                    argoWorkflowAgent.terminateWorkflow(clusterConfigService.resolveRunClusterName(run),
                            clusterConfigService.resolveRunNamespace(run), run.getName());
                } catch (RuntimeException e) {
                    // 终止 Argo 失败不阻塞新执行放行：平台侧置 Cancelled 后由兜底同步追赶
                    log.error("并发替换终止 Argo Workflow 失败, pipelineRunId={}, name={}", id, run.getName(), e);
                }
            }
            // 平台置 Cancelled，fail_type=ReplacedByNew 与用户手动停止区分，便于审计
            PipelineRun update = new PipelineRun();
            update.setId(run.getId());
            update.setRevision(run.getRevision());
            update.setStatus(PipelineRunStatusEnum.CANCELLED.getCode());
            update.setFailType(PipelineConcurrencyConstants.FAIL_TYPE_REPLACED_BY_NEW);
            update.setFailMessage(PipelineConcurrencyConstants.MSG_RUN_REPLACED_MESSAGE);
            update.setEndTime(new Date());
            if (pipelineRunRepository.updateForSync(update) != 1) {
                throw new BusinessException(PipelineConstants.MSG_RUN_STATE_CHANGED);
            }
            log.info("流水线已被新执行替换停止, pipelineRunId={}, name={}", id, run.getName());
            // 终态处理：落地任务节点记录 + 刷新快照
            pipelineRunSyncService.handleTerminal(id);
        } finally {
            distributedLockService.unlock(lockKey, lockValue);
        }
    }

    @Override
    public JsonNode getDetail(Long id) {
        if (id == null) {
            throw new BusinessException(PipelineConstants.MSG_PIPELINE_RUN_ID_REQUIRED);
        }
        PipelineRunSnapshot snapshot = pipelineRunSnapshotRepository.selectByPipelineRunId(String.valueOf(id));
        if (snapshot == null || snapshot.getDetail() == null) {
            return null;
        }
        try {
            return objectMapper.readTree(snapshot.getDetail());
        } catch (Exception e) {
            log.warn("解析执行详情快照失败, pipelineRunId={}", id, e);
            return null;
        }
    }

    @Override
    public PipelineRunExecuteDetailResponse getExecuteDetail(String pipelineRunName) {
        if (pipelineRunName == null || pipelineRunName.isEmpty()) {
            throw new BusinessException(PipelineConstants.MSG_PIPELINE_RUN_ID_REQUIRED);
        }
        // 1) 查 pipeline_run，拿到模板编码 + 版本
        PipelineRun run = pipelineRunRepository.selectByName(pipelineRunName);
        if (run == null) {
            throw new BusinessException(PipelineConstants.MSG_PIPELINE_RUN_NOT_EXIST);
        }
        // 2) 查模板版本详情 JSON，解析出所有任务编码
        PipelineTemplateVersion version = pipelineTemplateVersionRepository.selectByCodeAndVersion(
                run.getPipelineTemplateCode(), run.getPipelineTemplateVersion());
        Map<String, String> taskCodeNameMap = buildTaskCodeNameMap(version);

        // 3) 实时获取 Argo Workflow 详情（按 run 记录的集群路由）
        IoArgoprojWorkflowV1alpha1Workflow argoDetail = argoWorkflowAgent.getWorkflow(
                clusterConfigService.resolveRunClusterName(run), clusterConfigService.resolveRunNamespace(run), pipelineRunName);

        PipelineRunExecuteDetailResponse response = new PipelineRunExecuteDetailResponse();
        response.setClusterName(clusterConfigService.resolveRunClusterName(run));
        response.setArgoDetail(argoDetail);
        response.setTaskCodeNameMap(taskCodeNameMap);
        return response;
    }

    @Override
    public PipelineRunSnapshotResponse getSnapshot(Long pipelineRunId) {
        if (pipelineRunId == null) {
            throw new BusinessException(PipelineConstants.MSG_PIPELINE_RUN_ID_REQUIRED);
        }
        PipelineRunSnapshot snapshot = pipelineRunSnapshotRepository.selectByPipelineRunId(String.valueOf(pipelineRunId));
        if (snapshot == null || snapshot.getDetail() == null) {
            throw new BusinessException(PipelineConstants.MSG_RUN_SNAPSHOT_NOT_EXIST);
        }
        PipelineRunSnapshotResponse response = new PipelineRunSnapshotResponse();
        response.setPipelineRunId(pipelineRunId);
        response.setDetail(snapshot.getDetail());
        return response;
    }

    /**
     * 从模板版本详情 JSON 中解析出所有任务编码（dag.tasks[].name），再批量查 task_template 得到编码→中文名映射。
     */
    private Map<String, String> buildTaskCodeNameMap(PipelineTemplateVersion version) {
        if (version == null || version.getTemplateDetail() == null) {
            return Collections.emptyMap();
        }
        try {
            JsonNode root = objectMapper.readTree(version.getTemplateDetail());
            // WorkflowTemplate 结构：spec.templates[].dag.tasks[].name
            JsonNode templates = root.path("spec").path("templates");
            java.util.Set<String> taskCodes = new java.util.LinkedHashSet<>();
            for (JsonNode tpl : templates) {
                JsonNode tasks = tpl.path("dag").path("tasks");
                for (JsonNode task : tasks) {
                    String name = task.path("name").asText(null);
                    if (name != null && !name.isEmpty()) {
                        taskCodes.add(name);
                    }
                }
            }
            if (taskCodes.isEmpty()) {
                return Collections.emptyMap();
            }
            // 批量查 task_template，构建编码→中文名映射
            List<TaskTemplate> taskTemplates = taskTemplateRepository.listByCodes(taskCodes);
            Map<String, String> map = new java.util.LinkedHashMap<>();
            for (TaskTemplate tt : taskTemplates) {
                map.put(tt.getTaskTemplateCode(), tt.getName());
            }
            return map;
        } catch (Exception e) {
            log.warn("解析模板版本详情失败, code={}, version={}",
                    version.getPipelineTemplateCode(), version.getVersion(), e);
            return Collections.emptyMap();
        }
    }

    // ===== 私有工具方法 =====

    /**
     * 取 Argo Workflow 名称（metadata.name）
     */
    private String workflowName(IoArgoprojWorkflowV1alpha1Workflow workflow) {
        if (workflow == null || workflow.getMetadata() == null) {
            return null;
        }
        return workflow.getMetadata().getName();
    }

    /**
     * 实时查询 Argo Workflow 的 status.phase（按 run 记录的集群路由）
     */
    private String getArgoPhase(PipelineRun run) {
        if (run == null || run.getName() == null || run.getName().isEmpty()) {
            return null;
        }
        IoArgoprojWorkflowV1alpha1Workflow workflow = argoWorkflowAgent.getWorkflow(
                clusterConfigService.resolveRunClusterName(run), clusterConfigService.resolveRunNamespace(run), run.getName());
        IoArgoprojWorkflowV1alpha1WorkflowStatus status = workflow != null ? workflow.getStatus() : null;
        return status != null ? status.getPhase() : null;
    }

    /**
     * 保存（覆盖）执行详情快照。
     */
    private void saveSnapshot(Long runId, IoArgoprojWorkflowV1alpha1Workflow workflow) {
        if (workflow == null) {
            return;
        }
        try {
            String detail = objectMapper.writeValueAsString(workflow);
            pipelineRunSnapshotRepository.upsertDetail(String.valueOf(runId), detail);
        } catch (Exception e) {
            log.warn("保存执行详情快照失败, pipelineRunId={}", runId, e);
        }
    }

    /**
     * 将执行入参序列化为 JSON 字符串；入参为 null 时存 {@code {}}（arguments 列 NOT NULL）。
     */
    private String serializeArguments(Map<String, String> parameters) {
        Map<String, String> safe = parameters != null ? parameters : Collections.emptyMap();
        try {
            return objectMapper.writeValueAsString(safe);
        } catch (JsonProcessingException e) {
            throw new BusinessException(String.format(
                    PipelineConstants.MSG_RUN_ARGUMENTS_SERIALIZE_FAILED, e.getMessage()));
        }
    }

    private PipelineRunResponse toResponse(PipelineRun entity) {
        if (entity == null) {
            return null;
        }
        PipelineRunResponse response = new PipelineRunResponse();
        BeanUtils.copyProperties(entity, response);
        // 存量 run 的 cluster_name 可能为空，用兜底集群解析展示
        response.setClusterName(clusterConfigService.resolveRunClusterName(entity));
        return response;
    }
}
