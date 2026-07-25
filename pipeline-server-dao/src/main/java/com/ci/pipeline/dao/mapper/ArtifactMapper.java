package com.ci.pipeline.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ci.pipeline.dao.entity.Artifact;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 制品信息 Mapper
 */
public interface ArtifactMapper extends BaseMapper<Artifact> {

    /**
     * 分页查询制品信息（支持多条件筛选 + 字段排序）
     */
    IPage<Artifact> pageQuery(IPage<Artifact> page,
                              @Param("appName") String appName,
                              @Param("name") String name,
                              @Param("gitBranch") String gitBranch,
                              @Param("env") String env,
                              @Param("type") String type,
                              @Param("sortField") String sortField,
                              @Param("sortOrder") String sortOrder);

    /**
     * 根据流水线运行名称查询制品列表（流水线详情页用）
     */
    List<Artifact> selectByPipelineRunName(@Param("pipelineRunName") String pipelineRunName);
}
