package com.ci.pipeline.service.service;

import com.ci.pipeline.facade.request.TaskTemplateCreateRequest;
import com.ci.pipeline.facade.request.TaskTemplateQueryRequest;
import com.ci.pipeline.facade.request.TaskTemplateUpdateRequest;
import com.ci.pipeline.facade.response.DictDataResponse;
import com.ci.pipeline.facade.response.PageResponse;
import com.ci.pipeline.facade.response.TaskTemplateResponse;

import java.util.List;

/**
 * 任务模板业务接口
 */
public interface TaskTemplateService {

    /**
     * 新增任务模板
     */
    TaskTemplateResponse create(TaskTemplateCreateRequest request);

    /**
     * 修改任务模板
     */
    TaskTemplateResponse update(TaskTemplateUpdateRequest request);

    /**
     * 根据主键删除任务模板（若存在版本则禁止删除）
     */
    void deleteById(Long id);

    /**
     * 根据主键查询任务模板
     */
    TaskTemplateResponse getById(Long id);

    /**
     * 分页查询任务模板
     */
    PageResponse<TaskTemplateResponse> page(TaskTemplateQueryRequest query);

    /**
     * 任务模板所属分组下拉列表（从字典 task-template-group 查询，按 sort 升序）
     */
    List<DictDataResponse> listGroups();
}
