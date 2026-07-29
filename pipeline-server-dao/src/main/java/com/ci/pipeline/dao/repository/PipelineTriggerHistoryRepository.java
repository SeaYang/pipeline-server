package com.ci.pipeline.dao.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ci.pipeline.dao.entity.PipelineTriggerHistory;
import com.ci.pipeline.dao.mapper.PipelineTriggerHistoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class PipelineTriggerHistoryRepository {

    @Autowired
    private PipelineTriggerHistoryMapper pipelineTriggerHistoryMapper;

    public int insert(PipelineTriggerHistory entity) {
        return pipelineTriggerHistoryMapper.insert(entity);
    }

    public PipelineTriggerHistory selectById(Long id) {
        return pipelineTriggerHistoryMapper.selectById(id);
    }

    public IPage<PipelineTriggerHistory> pageQuery(long pageNum, long pageSize, Long pipelineId,
                                                    String appName, String status, String type) {
        Page<PipelineTriggerHistory> page = new Page<>(pageNum, pageSize);
        return pipelineTriggerHistoryMapper.pageQuery(page, pipelineId, appName, status, type);
    }
}
