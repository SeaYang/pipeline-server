package com.ci.pipeline.service.service;

import com.ci.pipeline.dao.entity.Pipeline;
import com.ci.pipeline.dao.entity.PipelineTemplateVersion;
import com.ci.pipeline.facade.request.PipelineRunQueryRequest;
import com.ci.pipeline.facade.response.PageResponse;
import com.ci.pipeline.facade.response.PipelineRunExecuteDetailResponse;
import com.ci.pipeline.facade.response.PipelineRunResponse;
import com.ci.pipeline.facade.response.PipelineRunSnapshotResponse;
import com.fasterxml.jackson.databind.JsonNode;
import io.argoproj.workflow.models.IoArgoprojWorkflowV1alpha1Workflow;

import java.util.Map;

/**
 * 流水线执行记录业务接口
 */
public interface PipelineRunService {

    /**
     * 执行流水线后落地执行记录：写入 pipeline_run + 首条执行详情快照，并触发异步状态同步。
     *
     * @param pipeline   流水线实例
     * @param effective  生效中的模板版本（提供执行时的 version）
     * @param workflow   Argo 返回的 Workflow 对象（取 name 与首条快照 detail）
     * @param parameters 执行入参（key=参数名，value=参数值），序列化为 JSON 存入 arguments
     * @return 新落地的执行记录 id
     */
    Long createRun(Pipeline pipeline, PipelineTemplateVersion effective,
                   IoArgoprojWorkflowV1alpha1Workflow workflow, Map<String, String> parameters);

    /**
     * 根据主键查询执行记录
     */
    PipelineRunResponse getById(Long id);

    /**
     * 分页查询执行记录
     */
    PageResponse<PipelineRunResponse> page(PipelineRunQueryRequest query);

    /**
     * 查询流水线最近一次执行记录（按 id 倒序取第一条），无执行记录返回 null
     *
     * @param pipelineId 流水线 id
     */
    PipelineRunResponse getLatestByPipelineId(Long pipelineId);

    /**
     * 兜底状态同步（手动触发）：已终态直接返回；update_time 未超陈旧阈值直接返回；
     * 否则执行一次 Argo 状态同步后返回最新记录。
     */
    PipelineRunResponse syncRun(Long id);

    /**
     * 重试执行：校验 DB 与 Argo 均为失败态后调 Argo retry，状态回 Pending 并重新异步同步。
     */
    PipelineRunResponse retry(Long id);

    /**
     * 停止执行：校验未终态后调 Argo terminate，状态置 Cancelled，落地任务节点与快照。
     */
    PipelineRunResponse stop(Long id);

    /**
     * 查询执行详情快照（结构同前端 go-cicd-workflow.json）；无快照返回 null。
     */
    JsonNode getDetail(Long id);

    /**
     * 查询执行详情（Argo Workflow 实时数据 + 任务编码→中文名映射）。
     *
     * @param pipelineRunName 流水线执行名称（Argo Workflow 名称）
     */
    PipelineRunExecuteDetailResponse getExecuteDetail(String pipelineRunName);

    /**
     * 查询流水线执行快照（pipeline_run_snapshot.detail 原始 JSON 字符串）。
     * <p>无快照或记录不存在时抛 BusinessException。
     *
     * @param pipelineRunId 流水线执行记录 id
     */
    PipelineRunSnapshotResponse getSnapshot(Long pipelineRunId);
}

