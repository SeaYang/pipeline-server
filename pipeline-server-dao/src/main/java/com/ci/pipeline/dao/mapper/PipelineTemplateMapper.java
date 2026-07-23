package com.ci.pipeline.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ci.pipeline.dao.entity.PipelineTemplate;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 流水线模板 Mapper
 */
public interface PipelineTemplateMapper extends BaseMapper<PipelineTemplate> {

    /**
     * 根据流水线模板编码查询（仅未删除）
     *
     * @param pipelineTemplateCode 流水线模板编码
     * @return 流水线模板实体，不存在返回 null
     */
    PipelineTemplate selectByPipelineTemplateCode(@Param("pipelineTemplateCode") String pipelineTemplateCode);

    /**
     * 统计指定流水线模板编码的记录数（用于唯一性校验）
     *
     * @param pipelineTemplateCode 流水线模板编码
     * @param excludeId            排除的主键（更新校验时传入自身 id），可为 null
     * @return 未删除记录数
     */
    long countByPipelineTemplateCode(@Param("pipelineTemplateCode") String pipelineTemplateCode,
                                     @Param("excludeId") Long excludeId);

    /**
     * 列表查询流水线模板（支持 pipelineTemplateGroup 精确筛选，支持按字段排序，不分页）。
     *
     * @param pipelineTemplateGroup 流水线模板所属分组（精确，可为 null）
     * @param sortField             排序列名（snake_case，已在 Service 层白名单校验，可为 null）
     * @param sortOrder             排序方向（asc / desc，可为 null）
     * @return 流水线模板列表
     */
    List<PipelineTemplate> listQuery(@Param("pipelineTemplateGroup") String pipelineTemplateGroup,
                                     @Param("sortField") String sortField,
                                     @Param("sortOrder") String sortOrder);
}
