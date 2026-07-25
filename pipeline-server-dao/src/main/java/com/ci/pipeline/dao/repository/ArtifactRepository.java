package com.ci.pipeline.dao.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ci.pipeline.dao.entity.Artifact;
import com.ci.pipeline.dao.mapper.ArtifactMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 制品信息数据访问，封装 Mapper 调用
 */
@Repository
public class ArtifactRepository {

    @Autowired
    private ArtifactMapper artifactMapper;

    public int insert(Artifact entity) {
        return artifactMapper.insert(entity);
    }

    public Artifact selectById(Long id) {
        return artifactMapper.selectById(id);
    }

    public IPage<Artifact> pageQuery(long pageNum, long pageSize, String appName, String name,
                                     String gitBranch, String env, String type,
                                     String sortField, String sortOrder) {
        Page<Artifact> page = new Page<>(pageNum, pageSize);
        return artifactMapper.pageQuery(page, appName, name, gitBranch, env, type, sortField, sortOrder);
    }

    public List<Artifact> selectByPipelineRunName(String pipelineRunName) {
        return artifactMapper.selectByPipelineRunName(pipelineRunName);
    }
}
