package com.ci.pipeline.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ci.pipeline.dao.entity.DictType;
import org.apache.ibatis.annotations.Param;

/**
 * 字典类型 Mapper
 */
public interface DictTypeMapper extends BaseMapper<DictType> {

    /**
     * 根据字典类型标识查询（仅未删除）
     *
     * @param dictType 字典类型标识
     * @return 字典类型实体，不存在返回 null
     */
    DictType selectByDictType(@Param("dictType") String dictType);

    /**
     * 统计指定字典类型标识的记录数（用于唯一性校验）
     *
     * @param dictType   字典类型标识
     * @param excludeId  排除的主键（更新校验时传入自身 id），可为 null
     * @return 未删除记录数
     */
    long countByDictType(@Param("dictType") String dictType, @Param("excludeId") Long excludeId);

    /**
     * 分页查询字典类型（支持按 dictType / dictName 模糊过滤，支持按字段排序）
     *
     * @param page      分页参数（由 MyBatis-Plus 分页插件自动改写）
     * @param dictType  字典类型标识（模糊）
     * @param dictName  字典名称（模糊）
     * @param sortField 排序列名（snake_case，已在 Service 层白名单校验，可为 null）
     * @param sortOrder 排序方向（asc / desc，可为 null）
     * @return 分页结果
     */
    IPage<DictType> pageQuery(IPage<DictType> page,
                              @Param("dictType") String dictType,
                              @Param("dictName") String dictName,
                              @Param("sortField") String sortField,
                              @Param("sortOrder") String sortOrder);
}
