package com.ci.pipeline.service.service;

import com.ci.pipeline.facade.request.PipelineTriggerHistoryQueryRequest;
import com.ci.pipeline.facade.response.PageResponse;
import com.ci.pipeline.facade.response.PipelineTriggerHistoryResponse;

public interface PipelineTriggerHistoryService {

    /**
     * 记录触发历史
     *
     * @param appName              应用名称
     * @param pipelineId           流水线id
     * @param pipelineRunId        流水线执行记录id（触发失败时为 null）
     * @param pipelineEventBindId  事件绑定记录id（手动触发为 0）
     * @param status               触发状态（SUCCESS / FAILED）
     * @param type                 触发类型（user / eventType）
     * @param creator              触发人
     * @param requestBody          触发请求体（JSON 字符串）
     * @param errorMessage         失败信息（成功时为 null）
     * @param pipelineTemplateCode 流水线模板编码
     * @param pipelineTemplateVersion 流水线模板版本（可能为 null）
     */
    void add(String appName, Long pipelineId, Long pipelineRunId, Long pipelineEventBindId,
             String status, String type, String creator, String requestBody,
             String errorMessage, String pipelineTemplateCode, String pipelineTemplateVersion);

    /**
     * 分页查询触发历史
     */
    PageResponse<PipelineTriggerHistoryResponse> page(PipelineTriggerHistoryQueryRequest query);

    /**
     * 查询触发历史详情
     */
    PipelineTriggerHistoryResponse getById(Long id);
}
