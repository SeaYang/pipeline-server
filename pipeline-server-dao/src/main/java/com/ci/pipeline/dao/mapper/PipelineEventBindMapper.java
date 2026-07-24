package com.ci.pipeline.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ci.pipeline.dao.entity.PipelineEventBind;
import org.apache.ibatis.annotations.Mapper;

/**
 * 事件-pipeline 绑定 Mapper
 */
@Mapper
public interface PipelineEventBindMapper extends BaseMapper<PipelineEventBind> {
}
