package com.ci.pipeline.dao.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ci.pipeline.dao.entity.PipelineEventBind;
import com.ci.pipeline.dao.mapper.PipelineEventBindMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * 事件-pipeline 绑定数据访问，封装 Mapper 调用。
 */
@Repository
public class PipelineEventBindRepository {

    @Autowired
    private PipelineEventBindMapper mapper;

    /**
     * 新增
     */
    public int insert(PipelineEventBind entity) {
        return mapper.insert(entity);
    }

    /**
     * 根据 appName + eventType + pipelineTemplateCode 查询绑定记录（仅未删除）
     */
    public PipelineEventBind selectByUniqueKey(String appName, String eventType, String pipelineTemplateCode) {
        return mapper.selectOne(
                new LambdaQueryWrapper<PipelineEventBind>()
                        .eq(PipelineEventBind::getAppName, appName)
                        .eq(PipelineEventBind::getEventType, eventType)
                        .eq(PipelineEventBind::getPipelineTemplateCode, pipelineTemplateCode));
    }
}
