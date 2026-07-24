package com.ci.pipeline.service.service;

import com.ci.pipeline.dao.entity.PipelineEventBind;

/**
 * 事件-pipeline 绑定管理服务（系统自动维护）
 */
public interface PipelineEventBindService {

    /**
     * 根据 appName + eventType + pipelineTemplateCode 查询绑定记录
     */
    PipelineEventBind getByAppNameAndEventTypeAndTemplateCode(
            String appName, String eventType, String pipelineTemplateCode);

    /**
     * 创建绑定记录
     */
    void create(String appName, String eventType, String pipelineTemplateCode, Long pipelineId);
}
