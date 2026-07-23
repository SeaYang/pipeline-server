package com.ci.pipeline.dao.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ci.pipeline.dao.entity.Pipeline;
import com.ci.pipeline.dao.mapper.PipelineMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * 流水线实例数据访问，封装 Mapper 调用，为 Service 层提供数据访问能力
 */
@Repository
public class PipelineRepository {

    @Autowired
    private PipelineMapper pipelineMapper;

    /**
     * 根据主键查询（仅未删除）
     */
    public Pipeline selectById(Long id) {
        return pipelineMapper.selectById(id);
    }

    /**
     * 分页查询（支持 appName 精确过滤，支持按字段排序，默认按创建时间倒序）
     *
     * @param pageNum   页码（从 1 开始）
     * @param pageSize  每页大小
     * @param appName   应用名称（精确，可为 null）
     * @param sortField 排序列名（snake_case，已白名单校验，可为 null）
     * @param sortOrder 排序方向（asc / desc，可为 null）
     */
    public IPage<Pipeline> pageQuery(long pageNum, long pageSize, String appName, String sortField, String sortOrder) {
        Page<Pipeline> page = new Page<>(pageNum, pageSize);
        return pipelineMapper.pageQuery(page, appName, sortField, sortOrder);
    }

    /**
     * 新增
     */
    public int insert(Pipeline entity) {
        return pipelineMapper.insert(entity);
    }

    /**
     * 根据主键更新（null 字段不参与更新）
     */
    public int updateById(Pipeline entity) {
        return pipelineMapper.updateById(entity);
    }

    /**
     * 根据主键逻辑删除
     */
    public int deleteById(Long id) {
        return pipelineMapper.deleteById(id);
    }
}
