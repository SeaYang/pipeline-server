package com.ci.pipeline.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ci.pipeline.dao.entity.DictData;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 字典数据 Mapper
 */
public interface DictDataMapper extends BaseMapper<DictData> {

    /**
     * 根据字典类型 + 字典 key 查询（仅未删除）
     *
     * @param dictType 字典类型标识
     * @param dictKey  数据名称
     * @return 字典数据实体，不存在返回 null
     */
    DictData selectByTypeAndKey(@Param("dictType") String dictType, @Param("dictKey") String dictKey);

    /**
     * 统计指定字典类型 + key 的记录数（用于唯一性校验）
     *
     * @param dictType   字典类型标识
     * @param dictKey    数据名称
     * @param excludeId  排除的主键（更新校验时传入自身 id），可为 null
     * @return 未删除记录数
     */
    long countByTypeAndKey(@Param("dictType") String dictType,
                           @Param("dictKey") String dictKey,
                           @Param("excludeId") Long excludeId);

    /**
     * 统计指定字典类型下的数据条数（用于删除字典类型前的占用校验）
     *
     * @param dictType 字典类型标识
     * @return 未删除数据条数
     */
    long countByDictType(@Param("dictType") String dictType);

    /**
     * 查询指定字典类型下的全部数据（仅未删除，按排序值升序）
     *
     * @param dictType 字典类型标识
     * @return 字典数据列表
     */
    List<DictData> listByDictType(@Param("dictType") String dictType);

    /**
     * 分页查询字典数据（支持 dictType 精确、dictKey / dictValue 模糊，支持按字段排序）
     *
     * @param page      分页参数（由 MyBatis-Plus 分页插件自动改写）
     * @param dictType  字典类型标识（精确匹配，可为 null）
     * @param dictKey   数据名称（模糊匹配，可为 null）
     * @param dictValue 数据值（模糊匹配，可为 null）
     * @param sortField 排序列名（snake_case，已在 Service 层白名单校验，可为 null）
     * @param sortOrder 排序方向（asc / desc，可为 null）
     * @return 分页结果
     */
    IPage<DictData> pageQuery(IPage<DictData> page,
                              @Param("dictType") String dictType,
                              @Param("dictKey") String dictKey,
                              @Param("dictValue") String dictValue,
                              @Param("sortField") String sortField,
                              @Param("sortOrder") String sortOrder);
}
