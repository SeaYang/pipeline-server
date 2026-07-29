package com.ci.pipeline.service.service;

import com.ci.pipeline.facade.request.PipelineCreateRequest;
import com.ci.pipeline.facade.request.PipelineExecuteRequest;
import com.ci.pipeline.facade.request.PipelineQueryRequest;
import com.ci.pipeline.facade.request.PipelineUpdateRequest;
import com.ci.pipeline.facade.response.PageResponse;
import com.ci.pipeline.facade.response.PipelineExecuteResponse;
import com.ci.pipeline.facade.response.PipelineResponse;
import com.ci.pipeline.facade.response.PipelineTemplateOptionResponse;

import java.util.List;

/**
 * 流水线实例业务接口
 */
public interface PipelineService {

    /**
     * 新增流水线（校验 appName 与流水线模板均存在）
     */
    PipelineResponse create(PipelineCreateRequest request);

    /**
     * 修改流水线（目前仅允许修改 name）
     */
    PipelineResponse update(PipelineUpdateRequest request);

    /**
     * 根据主键删除流水线
     */
    void deleteById(Long id);

    /**
     * 根据主键查询流水线
     */
    PipelineResponse getById(Long id);

    /**
     * 分页查询流水线（appName 精确过滤，默认按创建时间倒序）
     */
    PageResponse<PipelineResponse> page(PipelineQueryRequest query);

    /**
     * 新建流水线时的流水线模板下拉列表：按 app 所属编程语言过滤、仅含生效中版本的模板。
     */
    List<PipelineTemplateOptionResponse> listTemplates(String appName);

    /**
     * 执行流水线：取生效中版本确认模板已同步，按模板名（= pipelineTemplateCode）拉起 Argo Workflow。
     */
    PipelineExecuteResponse execute(PipelineExecuteRequest request);

    /**
     * 执行流水线并记录触发历史（手动触发入口）
     */
    PipelineExecuteResponse executeWithHistory(PipelineExecuteRequest request);
}
