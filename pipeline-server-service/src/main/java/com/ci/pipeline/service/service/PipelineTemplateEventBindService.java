package com.ci.pipeline.service.service;

import com.ci.pipeline.facade.request.PipelineTemplateEventBindCreateRequest;
import com.ci.pipeline.facade.request.PipelineTemplateEventBindQueryRequest;
import com.ci.pipeline.facade.request.PipelineTemplateEventBindUpdateRequest;
import com.ci.pipeline.facade.response.PageResponse;
import com.ci.pipeline.facade.response.PipelineTemplateEventBindResponse;

import java.util.List;

/**
 * 事件-模板绑定管理服务（后台配置）
 */
public interface PipelineTemplateEventBindService {

    /**
     * 新增绑定
     */
    PipelineTemplateEventBindResponse create(PipelineTemplateEventBindCreateRequest request);

    /**
     * 修改绑定
     */
    PipelineTemplateEventBindResponse update(PipelineTemplateEventBindUpdateRequest request);

    /**
     * 根据主键删除绑定
     */
    void deleteById(Long id);

    /**
     * 根据主键查询绑定
     */
    PipelineTemplateEventBindResponse getById(Long id);

    /**
     * 分页查询绑定列表
     */
    PageResponse<PipelineTemplateEventBindResponse> page(PipelineTemplateEventBindQueryRequest query);

    /**
     * 根据事件类型查询所有绑定的模板编码列表
     */
    List<String> listTemplateCodesByEventType(String eventType);
}
