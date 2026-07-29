package com.ci.pipeline.service.service.impl;

import com.ci.pipeline.common.auth.UserContext;
import com.ci.pipeline.common.constants.PipelineEventConstants;
import com.ci.pipeline.dao.entity.PipelineEventBind;
import com.ci.pipeline.dao.repository.PipelineEventBindRepository;
import com.ci.pipeline.service.service.PipelineEventBindService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 事件-pipeline 绑定管理服务实现（系统自动维护）
 */
@Slf4j
@Service
public class PipelineEventBindServiceImpl implements PipelineEventBindService {

    @Autowired
    private PipelineEventBindRepository repository;

    @Override
    public PipelineEventBind getByAppNameAndEventTypeAndTemplateCode(
            String appName, String eventType, String pipelineTemplateCode) {
        return repository.selectByUniqueKey(appName, eventType, pipelineTemplateCode);
    }

    @Override
    public PipelineEventBind create(String appName, String eventType, String pipelineTemplateCode, Long pipelineId) {
        PipelineEventBind entity = new PipelineEventBind();
        entity.setAppName(appName);
        entity.setEventType(eventType);
        entity.setPipelineTemplateCode(pipelineTemplateCode);
        entity.setPipelineId(pipelineId);
        // 事件触发接口无认证，UserContext 可能为空，使用系统标识兜底
        String creator = UserContext.getUserId();
        entity.setCreator(creator != null ? creator : PipelineEventConstants.EVENT_SYSTEM);
        repository.insert(entity);
        log.info("创建事件-pipeline绑定成功, appName={}, eventType={}, templateCode={}, pipelineId={}, id={}",
                appName, eventType, pipelineTemplateCode, pipelineId, entity.getId());
        return entity;
    }
}
