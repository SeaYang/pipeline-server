package com.ci.pipeline.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ci.pipeline.dao.entity.PipelineParameter;
import org.apache.ibatis.annotations.Param;

public interface PipelineParameterMapper extends BaseMapper<PipelineParameter> {

    PipelineParameter selectByName(@Param("name") String name);

    long countByName(@Param("name") String name, @Param("excludeId") Long excludeId);

    IPage<PipelineParameter> pageQuery(IPage<PipelineParameter> page,
                                       @Param("name") String name,
                                       @Param("label") String label,
                                       @Param("paramType") String paramType,
                                       @Param("paramGroup") String paramGroup,
                                       @Param("sortField") String sortField,
                                       @Param("sortOrder") String sortOrder);
}
