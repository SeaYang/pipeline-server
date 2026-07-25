package com.ci.pipeline.service.service;

import com.ci.pipeline.facade.request.ArtifactQueryRequest;
import com.ci.pipeline.facade.request.ArtifactUploadRequest;
import com.ci.pipeline.facade.response.ArtifactResponse;
import com.ci.pipeline.facade.response.PageResponse;

import java.util.List;

/**
 * 制品管理 Service
 */
public interface ArtifactService {

    /**
     * 制品上传（Argo pod 回传），返回制品ID
     */
    Long upload(ArtifactUploadRequest request);

    /**
     * 根据ID查询制品详情
     */
    ArtifactResponse getById(Long id);

    /**
     * 分页查询制品列表
     */
    PageResponse<ArtifactResponse> page(ArtifactQueryRequest query);

    /**
     * 根据流水线运行名称查询制品列表（流水线详情页用）
     */
    List<ArtifactResponse> listByPipelineRunName(String pipelineRunName);
}
