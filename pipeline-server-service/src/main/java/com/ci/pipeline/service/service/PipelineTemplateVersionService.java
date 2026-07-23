package com.ci.pipeline.service.service;

import com.ci.pipeline.facade.request.PipelineTemplateVersionCreateRequest;
import com.ci.pipeline.facade.request.PipelineTemplateVersionStatusRequest;
import com.ci.pipeline.facade.request.PipelineTemplateVersionUpdateRequest;
import com.ci.pipeline.facade.response.PipelineTemplateVersionResponse;
import com.ci.pipeline.facade.response.PipelineTemplateVersionSaveResponse;

import java.util.List;

/**
 * 流水线模板版本业务接口
 */
public interface PipelineTemplateVersionService {

    /**
     * 新增流水线模板版本
     */
    PipelineTemplateVersionSaveResponse create(PipelineTemplateVersionCreateRequest request);

    /**
     * 修改流水线模板版本（仅允许改 templateDetail / changeNote）
     */
    PipelineTemplateVersionSaveResponse update(PipelineTemplateVersionUpdateRequest request);

    /**
     * 根据主键删除流水线模板版本
     */
    void deleteById(Long id);

    /**
     * 根据流水线模板编码 + 版本号查询版本详情
     */
    PipelineTemplateVersionResponse getDetail(String pipelineTemplateCode, String version);

    /**
     * 根据流水线模板编码查询版本列表（按创建时间倒序，代码内排序）
     */
    List<PipelineTemplateVersionResponse> listByCode(String pipelineTemplateCode);

    /**
     * 变更版本状态（目标为生效中时，自动把其它生效中/草稿版本置为已失效）
     */
    PipelineTemplateVersionResponse changeStatus(PipelineTemplateVersionStatusRequest request);
}
