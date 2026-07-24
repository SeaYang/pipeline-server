package com.ci.pipeline.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ci.pipeline.dao.entity.PipelineTemplateEventBind;
import org.apache.ibatis.annotations.Mapper;

/**
 * 事件-模板绑定 Mapper
 */
@Mapper
public interface PipelineTemplateEventBindMapper extends BaseMapper<PipelineTemplateEventBind> {
}
