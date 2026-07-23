package com.ci.pipeline.service.service;

import com.ci.pipeline.facade.request.TaskTemplateVersionCreateRequest;
import com.ci.pipeline.facade.request.TaskTemplateVersionStatusRequest;
import com.ci.pipeline.facade.request.TaskTemplateVersionUpdateRequest;
import com.ci.pipeline.facade.response.TaskTemplateVersionResponse;

import java.util.List;

/**
 * 任务模板版本业务接口
 */
public interface TaskTemplateVersionService {

    /**
     * 新增任务模板版本
     */
    TaskTemplateVersionResponse create(TaskTemplateVersionCreateRequest request);

    /**
     * 修改任务模板版本（仅允许改 templateDetail / changeNote）
     */
    TaskTemplateVersionResponse update(TaskTemplateVersionUpdateRequest request);

    /**
     * 根据主键删除任务模板版本
     */
    void deleteById(Long id);

    /**
     * 根据任务模板编码 + 版本号查询版本详情
     */
    TaskTemplateVersionResponse getDetail(String taskTemplateCode, String version);

    /**
     * 根据任务模板编码查询版本列表（按创建时间倒序，代码内排序）
     */
    List<TaskTemplateVersionResponse> listByCode(String taskTemplateCode);

    /**
     * 变更版本状态（目标为生效中时，自动把其它生效中版本置为已失效）
     */
    TaskTemplateVersionResponse changeStatus(TaskTemplateVersionStatusRequest request);
}
