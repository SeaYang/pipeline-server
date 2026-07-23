package com.ci.pipeline.service.service;

import com.ci.pipeline.facade.request.PipelineTemplateCreateRequest;
import com.ci.pipeline.facade.request.PipelineTemplateQueryRequest;
import com.ci.pipeline.facade.request.PipelineTemplateUpdateRequest;
import com.ci.pipeline.facade.response.DictDataResponse;
import com.ci.pipeline.facade.response.PipelineTemplateResponse;

import java.util.List;

/**
 * 流水线模板业务接口
 */
public interface PipelineTemplateService {

    /**
     * 新增流水线模板
     */
    PipelineTemplateResponse create(PipelineTemplateCreateRequest request);

    /**
     * 修改流水线模板
     */
    PipelineTemplateResponse update(PipelineTemplateUpdateRequest request);

    /**
     * 根据主键删除流水线模板（若存在版本则禁止删除）
     */
    void deleteById(Long id);

    /**
     * 根据主键查询流水线模板
     */
    PipelineTemplateResponse getById(Long id);

    /**
     * 列表查询流水线模板（支持所属分组精确筛选、字段排序，不分页）
     */
    List<PipelineTemplateResponse> list(PipelineTemplateQueryRequest query);

    /**
     * 流水线模板所属分组下拉列表（从字典 programming-language 查询，按 sort 升序）
     */
    List<DictDataResponse> listGroups();
}
