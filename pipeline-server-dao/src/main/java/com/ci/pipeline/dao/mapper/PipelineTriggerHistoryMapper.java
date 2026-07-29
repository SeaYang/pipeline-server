package com.ci.pipeline.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ci.pipeline.dao.entity.PipelineTriggerHistory;
import org.apache.ibatis.annotations.Param;

public interface PipelineTriggerHistoryMapper extends BaseMapper<PipelineTriggerHistory> {

    IPage<PipelineTriggerHistory> pageQuery(IPage<PipelineTriggerHistory> page,
                                            @Param("pipelineId") Long pipelineId,
                                            @Param("appName") String appName,
                                            @Param("status") String status,
                                            @Param("type") String type);
}
