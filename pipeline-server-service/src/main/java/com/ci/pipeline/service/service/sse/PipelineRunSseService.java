package com.ci.pipeline.service.service.sse;

import com.ci.pipeline.common.enums.PipelineRunStatusEnum;
import com.ci.pipeline.dao.entity.Pipeline;
import com.ci.pipeline.dao.entity.PipelineRun;
import com.ci.pipeline.dao.entity.PipelineRunSnapshot;
import com.ci.pipeline.dao.entity.PipelineTaskRun;
import com.ci.pipeline.dao.entity.PipelineTemplate;
import com.ci.pipeline.dao.entity.PipelineTemplateVersion;
import com.ci.pipeline.dao.entity.TaskTemplate;
import com.ci.pipeline.dao.repository.PipelineRepository;
import com.ci.pipeline.dao.repository.PipelineRunRepository;
import com.ci.pipeline.dao.repository.PipelineRunSnapshotRepository;
import com.ci.pipeline.dao.repository.PipelineTaskRunRepository;
import com.ci.pipeline.dao.repository.PipelineTemplateRepository;
import com.ci.pipeline.dao.repository.PipelineTemplateVersionRepository;
import com.ci.pipeline.dao.repository.TaskTemplateRepository;
import com.ci.pipeline.facade.response.PipelineRunDetailDTO;
import com.ci.pipeline.service.config.PipelineRunSyncProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 流水线执行详情 SSE 推送服务。
 * <p>核心职责：为每个 SSE 连接创建一个异步推送任务，定时（5s）轮询 DB，
 * 当 pipeline_run.revision 变化时推送最新执行详情，无变化时发心跳保持连接。
 *
 * <h3>数据来源</h3>
 * <ul>
 *   <li>非终态：pipeline_run_snapshot.detail（异步同步线程持续刷新）</li>
 *   <li>终态：由 pipeline_template_version.templateDetail（DAG 骨架）+ pipeline_task_run（运行数据）组装</li>
 * </ul>
 *
 * <h3>一致性保证</h3>
 * <p>异步同步线程在 applyWorkflow 中通过事务保证「revision 更新 + snapshot 刷新 + task_run 落地」原子提交，
 * 因此 SSE 查到终态时数据一定已就绪。
 */
@Slf4j
@Service
public class PipelineRunSseService {

    /** SSE 连接超时时间（30 分钟），防止僵尸连接 */
    private static final long SSE_TIMEOUT = 30 * 60 * 1000L;

    @Autowired
    private PipelineRunRepository pipelineRunRepository;

    @Autowired
    private PipelineRunSnapshotRepository pipelineRunSnapshotRepository;

    @Autowired
    private PipelineTaskRunRepository pipelineTaskRunRepository;

    @Autowired
    private PipelineTemplateVersionRepository pipelineTemplateVersionRepository;

    @Autowired
    private TaskTemplateRepository taskTemplateRepository;

    @Autowired
    private PipelineRepository pipelineRepository;

    @Autowired
    private PipelineTemplateRepository pipelineTemplateRepository;

    @Autowired
    private com.ci.pipeline.service.service.ClusterConfigService clusterConfigService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PipelineRunSyncProperties pipelineRunSyncProperties;

    @Autowired
    @Qualifier("pipelineRunSyncExecutor")
    private ThreadPoolTaskExecutor pipelineRunSyncExecutor;

    /**
     * 创建 SSE 连接并启动异步推送任务。
     *
     * @param pipelineRunName 流水线执行名称（Argo Workflow 名称）
     * @return SseEmitter
     */
    public SseEmitter subscribe(String pipelineRunName) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        AtomicBoolean running = new AtomicBoolean(true);

        // 连接结束/超时/异常时标记停止，让轮询线程退出
        emitter.onCompletion(() -> running.set(false));
        emitter.onTimeout(() -> {
            running.set(false);
            emitter.complete();
        });
        emitter.onError(e -> {
            running.set(false);
            // 客户端刷新/关闭页面会触发此回调（Broken pipe），属于正常情况，不打堆栈
            log.debug("SSE 连接异常（客户端可能已断开）, pipelineRunName={}", pipelineRunName);
        });

        // 提交异步推送任务
        pipelineRunSyncExecutor.execute(() -> pushLoop(pipelineRunName, emitter, running));

        return emitter;
    }

    /**
     * SSE 推送主循环：定时轮询 DB，revision 变化推数据，无变化发心跳。
     */
    private void pushLoop(String pipelineRunName, SseEmitter emitter, AtomicBoolean running) {
        long intervalMillis = Math.max(1, pipelineRunSyncProperties.getSyncIntervalSeconds()) * 1000L;
        int lastRevision = -1;
        log.info("SSE 推送任务启动, pipelineRunName={}", pipelineRunName);

        try {
            while (running.get()) {
                // 按 name 查 pipeline_run
                PipelineRun run = pipelineRunRepository.selectByName(pipelineRunName);
                if (run == null) {
                    sendEvent(emitter, "error", "流水线执行记录不存在");
                    break;
                }

                PipelineRunStatusEnum status = PipelineRunStatusEnum.ofCode(run.getStatus());
                boolean terminal = status != null && status.isTerminal();
                boolean revisionChanged = run.getRevision() != null && run.getRevision() != lastRevision;

                boolean sendOk;
                if (revisionChanged || lastRevision == -1) {
                    // revision 变化或首次：组装并推送完整数据
                    PipelineRunDetailDTO dto = buildDetailDTO(run);
                    if (dto != null) {
                        sendOk = sendEvent(emitter, "detail", dto);
                    } else {
                        sendOk = sendHeartbeat(emitter);
                    }
                    lastRevision = run.getRevision() != null ? run.getRevision() : 0;
                } else {
                    // 无变化：发心跳（SSE 注释行，前端不触发 onmessage，但保持连接活跃）
                    sendOk = sendHeartbeat(emitter);
                }

                // 发送失败（客户端刷新/关闭页面导致 Broken pipe）：立即退出，不再重试
                if (!sendOk) {
                    log.info("SSE 客户端已断开，停止推送, pipelineRunName={}", pipelineRunName);
                    break;
                }

                // 终态：推送最后一次后结束
                if (terminal) {
                    log.info("SSE 推送任务结束（终态）, pipelineRunName={}, status={}", pipelineRunName, run.getStatus());
                    break;
                }

                // 等待下一轮
                Thread.sleep(intervalMillis);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.info("SSE 推送任务被中断, pipelineRunName={}", pipelineRunName);
        } catch (Exception e) {
            log.error("SSE 推送任务异常, pipelineRunName={}", pipelineRunName, e);
        } finally {
            emitter.complete();
        }
    }

    // ====== 数据组装 ======

    /**
     * 组装执行详情 DTO。
     * <p>非终态：从 snapshot 取 workflowDetail；终态：从模板+task_run 组装。
     * taskCodeNameMap 始终从 templateDetail 解析。
     */
    public PipelineRunDetailDTO buildDetailDTO(PipelineRun run) {
        if (run == null) {
            return null;
        }

        // 查模板版本（用于解析 taskCodeNameMap + 终态组装 DAG 骨架）
        PipelineTemplateVersion version = pipelineTemplateVersionRepository.selectByCodeAndVersion(
                run.getPipelineTemplateCode(), run.getPipelineTemplateVersion());

        // taskCodeNameMap：始终从 templateDetail 解析
        Map<String, String> taskCodeNameMap = buildTaskCodeNameMap(version);

        PipelineRunStatusEnum status = PipelineRunStatusEnum.ofCode(run.getStatus());
        boolean terminal = status != null && status.isTerminal();

        JsonNode workflowDetail;
        if (terminal) {
            // 终态：由模板 DAG 骨架 + pipeline_task_run 运行数据组装
            workflowDetail = buildTerminalWorkflowDetail(run, version);
        } else {
            // 非终态：从 snapshot 取（异步同步线程持续刷新）
            workflowDetail = buildSnapshotWorkflowDetail(run.getId(), version);
        }

        PipelineRunDetailDTO dto = new PipelineRunDetailDTO();
        dto.setPipelineRunName(run.getName());
        dto.setPipelineRunId(run.getId());
        dto.setStatus(run.getStatus());
        dto.setStartedAt(formatDate(run.getStartTime()));
        dto.setFinishedAt(formatDate(run.getEndTime()));
        dto.setDuration(run.getDuration());
        dto.setFailMessage(run.getFailMessage());
        dto.setAppName(run.getAppName());
        dto.setPipelineTemplateCode(run.getPipelineTemplateCode());
        dto.setCreator(run.getCreator());
        dto.setClusterName(clusterConfigService.resolveRunClusterName(run));
        dto.setArguments(run.getArguments());
        // 流水线模板名称：只查一个字段
        if (run.getPipelineTemplateCode() != null) {
            PipelineTemplate template = pipelineTemplateRepository.selectByPipelineTemplateCode(run.getPipelineTemplateCode());
            if (template != null) {
                dto.setPipelineTemplateName(template.getName());
            }
        }
        // 流水线名称：根据 pipelineId 查，只查一个字段
        if (run.getPipelineId() != null) {
            Pipeline pipeline = pipelineRepository.selectById(run.getPipelineId());
            if (pipeline != null) {
                dto.setPipelineName(pipeline.getName());
            }
        }
        dto.setWorkflowDetail(workflowDetail);
        dto.setTaskCodeNameMap(taskCodeNameMap);
        return dto;
    }

    /**
     * 非终态：从 pipeline_run_snapshot 取 workflowDetail。
     * <p>刚提交时 Argo 尚未回填 storedWorkflowTemplateSpec（DAG 定义），导致前端拿不到节点全集。
     * 此时用 templateDetail（静态模板定义）的 spec 补到 status.storedWorkflowTemplateSpec 中，
     * 保证前端任何时刻都能渲染完整的 DAG 节点。
     *
     * @param pipelineRunId 流水线执行记录 id
     * @param version       模板版本（提供静态 DAG 定义作为兜底）
     */
    private JsonNode buildSnapshotWorkflowDetail(Long pipelineRunId, PipelineTemplateVersion version) {
        PipelineRunSnapshot snapshot = pipelineRunSnapshotRepository.selectByPipelineRunId(String.valueOf(pipelineRunId));
        if (snapshot == null || snapshot.getDetail() == null) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(snapshot.getDetail());
            // 检查 status.storedWorkflowTemplateSpec 是否存在
            JsonNode status = root.path("status");
            JsonNode storedSpec = status.path("storedWorkflowTemplateSpec");
            if (storedSpec.isMissingNode() && version != null && version.getTemplateDetail() != null) {
                // Argo 尚未回填，用 templateDetail 的 spec 补上（前端 workflowToFlow 依赖此字段渲染节点全集）
                JsonNode tplSpec = objectMapper.readTree(version.getTemplateDetail()).path("spec");
                if (!tplSpec.isMissingNode()) {
                    ((ObjectNode) status).set("storedWorkflowTemplateSpec", tplSpec);
                }
            }
            return root;
        } catch (Exception e) {
            log.warn("解析 snapshot detail 失败, pipelineRunId={}", pipelineRunId, e);
            return null;
        }
    }

    /**
     * 终态：由 templateDetail（DAG 骨架）+ pipeline_task_run（运行数据）组装 workflowDetail。
     * <p>组装出的 JSON 结构兼容前端 ArgoWorkflowDetail，包含：
     * metadata.name、status.phase/nodes、storedWorkflowTemplateSpec（DAG 定义）。
     */
    private JsonNode buildTerminalWorkflowDetail(PipelineRun run, PipelineTemplateVersion version) {
        ObjectNode root = objectMapper.createObjectNode();

        // metadata
        ObjectNode metadata = root.putObject("metadata");
        metadata.put("name", run.getName());

        // status
        ObjectNode status = root.putObject("status");
        status.put("phase", run.getStatus());
        if (run.getStartTime() != null) {
            status.put("startedAt", formatDate(run.getStartTime()));
        }
        if (run.getEndTime() != null) {
            status.put("finishedAt", formatDate(run.getEndTime()));
        }

        // 从 templateDetail 提取 storedWorkflowTemplateSpec（DAG 定义，前端节点全集来源）
        if (version != null && version.getTemplateDetail() != null) {
            try {
                JsonNode tplRoot = objectMapper.readTree(version.getTemplateDetail());
                JsonNode tplSpec = tplRoot.path("spec");
                if (!tplSpec.isMissingNode()) {
                    // 前端 workflowToFlow 从 status.storedWorkflowTemplateSpec 取 DAG 定义
                    status.set("storedWorkflowTemplateSpec", tplSpec);
                }
            } catch (Exception e) {
                log.warn("解析 templateDetail 失败, pipelineRunId={}", run.getId(), e);
            }
        }

        // 从 pipeline_task_run 组装 status.nodes（运行时节点状态）
        List<PipelineTaskRun> taskRuns = pipelineTaskRunRepository.selectByPipelineRunId(run.getId());
        ObjectNode nodes = status.putObject("nodes");
        for (PipelineTaskRun task : taskRuns) {
            ObjectNode node = nodes.putObject(task.getTaskCode());
            node.put("id", task.getTaskCode());
            node.put("name", run.getName() + "." + task.getTaskCode());
            node.put("displayName", task.getTaskCode());
            node.put("type", "Pod");
            node.put("phase", task.getStatus());
            if (task.getRunHostName() != null) {
                node.put("hostNodeName", task.getRunHostName());
            }
            if (task.getStartTime() != null) {
                node.put("startedAt", formatDate(task.getStartTime()));
            }
            if (task.getEndTime() != null) {
                node.put("finishedAt", formatDate(task.getEndTime()));
            }
            // 入参/出参
            parseParameters(node, "inputs", task.getInputs());
            parseParameters(node, "outputs", task.getOutputs());
        }

        return root;
    }

    /**
     * 把 JSON 字符串形式的参数（[{"name":"x","value":"y"}]）解析为 Argo 节点的 inputs/outputs 结构。
     */
    private void parseParameters(ObjectNode node, String field, String json) {
        if (json == null || json.isEmpty()) {
            return;
        }
        try {
            JsonNode arr = objectMapper.readTree(json);
            if (arr.isArray() && arr.size() > 0) {
                ObjectNode container = node.putObject(field);
                ArrayNode params = container.putArray("parameters");
                for (JsonNode item : arr) {
                    ObjectNode param = params.addObject();
                    param.put("name", item.path("name").asText());
                    String value = item.path("value").asText(null);
                    if (value != null) {
                        param.put("value", value);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("解析参数 JSON 失败, field={}, json={}", field, json, e);
        }
    }

    /**
     * 从 templateDetail 解析 dag.tasks[].name（任务编码全集），批量查 task_template 得中文名。
     */
    private Map<String, String> buildTaskCodeNameMap(PipelineTemplateVersion version) {
        if (version == null || version.getTemplateDetail() == null) {
            return new LinkedHashMap<>();
        }
        try {
            JsonNode root = objectMapper.readTree(version.getTemplateDetail());
            JsonNode templates = root.path("spec").path("templates");
            Set<String> taskCodes = new java.util.LinkedHashSet<>();
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
                return new LinkedHashMap<>();
            }
            List<TaskTemplate> taskTemplates = taskTemplateRepository.listByCodes(taskCodes);
            Map<String, String> map = new LinkedHashMap<>();
            for (TaskTemplate tt : taskTemplates) {
                map.put(tt.getTaskTemplateCode(), tt.getName());
            }
            return map;
        } catch (Exception e) {
            log.warn("解析模板详情失败, code={}, version={}",
                    version.getPipelineTemplateCode(), version.getVersion(), e);
            return new LinkedHashMap<>();
        }
    }

    // ====== SSE 工具方法 ======

    /**
     * 发送数据事件。
     *
     * @return true=发送成功；false=发送失败（客户端已断开），调用方应停止推送
     */
    private boolean sendEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
            return true;
        } catch (Exception e) {
            // 客户端刷新/关闭页面会导致 Broken pipe / ClientAbortException，属于正常情况，静默处理
            log.debug("SSE 发送事件失败（客户端可能已断开）, event={}", eventName);
            return false;
        }
    }

    /**
     * 发送心跳（SSE 注释行，前端不触发 onmessage）。
     *
     * @return true=发送成功；false=发送失败（客户端已断开）
     */
    private boolean sendHeartbeat(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().comment("heartbeat"));
            return true;
        } catch (Exception e) {
            log.debug("SSE 发送心跳失败（客户端可能已断开）");
            return false;
        }
    }

    /** Date → ISO 8601 字符串 */
    private String formatDate(Date date) {
        return date != null ? date.toInstant().toString() : null;
    }
}
