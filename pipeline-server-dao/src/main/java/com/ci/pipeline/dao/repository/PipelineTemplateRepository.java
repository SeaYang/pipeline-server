package com.ci.pipeline.dao.repository;

import com.ci.pipeline.dao.entity.PipelineTemplate;
import com.ci.pipeline.dao.mapper.PipelineTemplateMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 流水线模板数据访问，封装 Mapper 调用，为 Service 层提供数据访问能力
 */
@Repository
public class PipelineTemplateRepository {

    @Autowired
    private PipelineTemplateMapper pipelineTemplateMapper;

    /**
     * 根据主键查询（仅未删除）
     */
    public PipelineTemplate selectById(Long id) {
        return pipelineTemplateMapper.selectById(id);
    }

    /**
     * 根据流水线模板编码查询（仅未删除）
     */
    public PipelineTemplate selectByPipelineTemplateCode(String pipelineTemplateCode) {
        return pipelineTemplateMapper.selectByPipelineTemplateCode(pipelineTemplateCode);
    }

    /**
     * 统计指定流水线模板编码的记录数（用于唯一性校验）
     *
     * @param pipelineTemplateCode 流水线模板编码
     * @param excludeId            排除的主键，可为 null
     */
    public long countByPipelineTemplateCode(String pipelineTemplateCode, Long excludeId) {
        return pipelineTemplateMapper.countByPipelineTemplateCode(pipelineTemplateCode, excludeId);
    }

    /**
     * 列表查询（支持 pipelineTemplateGroup 精确筛选，支持按字段排序，不分页）
     *
     * @param pipelineTemplateGroup 流水线模板所属分组（精确，可为 null）
     * @param sortField             排序列名（snake_case，已白名单校验，可为 null）
     * @param sortOrder             排序方向（asc / desc，可为 null）
     */
    public List<PipelineTemplate> listQuery(String pipelineTemplateGroup, String sortField, String sortOrder) {
        return pipelineTemplateMapper.listQuery(pipelineTemplateGroup, sortField, sortOrder);
    }

    /**
     * 新增
     */
    public int insert(PipelineTemplate entity) {
        return pipelineTemplateMapper.insert(entity);
    }

    /**
     * 根据主键更新（null 字段不参与更新）
     */
    public int updateById(PipelineTemplate entity) {
        return pipelineTemplateMapper.updateById(entity);
    }

    /**
     * 根据主键逻辑删除
     */
    public int deleteById(Long id) {
        return pipelineTemplateMapper.deleteById(id);
    }
}
