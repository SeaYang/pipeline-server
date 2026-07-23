package com.ci.pipeline.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ci.pipeline.dao.entity.Pipeline;
import org.apache.ibatis.annotations.Param;

/**
 * 流水线实例 Mapper
 */
public interface PipelineMapper extends BaseMapper<Pipeline> {

    /**
     * 分页查询流水线（仅支持 appName 精确过滤，支持按字段排序，默认按创建时间倒序）
     *
     * @param page      分页参数（由 MyBatis-Plus 分页插件自动改写）
     * @param appName   应用名称（精确，可为 null）
     * @param sortField 排序列名（snake_case，已在 Service 层白名单校验，可为 null）
     * @param sortOrder 排序方向（asc / desc，可为 null）
     * @return 分页结果
     */
    IPage<Pipeline> pageQuery(IPage<Pipeline> page,
                              @Param("appName") String appName,
                              @Param("sortField") String sortField,
                              @Param("sortOrder") String sortOrder);
}
