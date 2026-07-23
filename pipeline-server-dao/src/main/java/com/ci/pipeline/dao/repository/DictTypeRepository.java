package com.ci.pipeline.dao.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ci.pipeline.dao.entity.DictType;
import com.ci.pipeline.dao.mapper.DictTypeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * 字典类型数据访问，封装 Mapper 调用，为 Service 层提供数据访问能力
 */
@Repository
public class DictTypeRepository {

    @Autowired
    private DictTypeMapper dictTypeMapper;

    /**
     * 根据主键查询（仅未删除）
     */
    public DictType selectById(Long id) {
        return dictTypeMapper.selectById(id);
    }

    /**
     * 根据字典类型标识查询（仅未删除）
     */
    public DictType selectByDictType(String dictType) {
        return dictTypeMapper.selectByDictType(dictType);
    }

    /**
     * 统计指定字典类型标识的记录数（用于唯一性校验）
     *
     * @param dictType  字典类型标识
     * @param excludeId 排除的主键，可为 null
     */
    public long countByDictType(String dictType, Long excludeId) {
        return dictTypeMapper.countByDictType(dictType, excludeId);
    }

    /**
     * 分页查询（支持按字段排序）
     *
     * @param pageNum   页码（从 1 开始）
     * @param pageSize  每页大小
     * @param dictType  字典类型标识（模糊，可为 null）
     * @param dictName  字典名称（模糊，可为 null）
     * @param sortField 排序列名（snake_case，已白名单校验，可为 null）
     * @param sortOrder 排序方向（asc / desc，可为 null）
     */
    public IPage<DictType> pageQuery(long pageNum, long pageSize, String dictType, String dictName,
                                     String sortField, String sortOrder) {
        Page<DictType> page = new Page<>(pageNum, pageSize);
        return dictTypeMapper.pageQuery(page, dictType, dictName, sortField, sortOrder);
    }

    /**
     * 新增
     */
    public int insert(DictType entity) {
        return dictTypeMapper.insert(entity);
    }

    /**
     * 根据主键更新（null 字段不参与更新）
     */
    public int updateById(DictType entity) {
        return dictTypeMapper.updateById(entity);
    }

    /**
     * 根据主键逻辑删除
     */
    public int deleteById(Long id) {
        return dictTypeMapper.deleteById(id);
    }
}
