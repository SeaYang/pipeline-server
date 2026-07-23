package com.ci.pipeline.dao.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ci.pipeline.dao.entity.DictData;
import com.ci.pipeline.dao.mapper.DictDataMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 字典数据数据访问，封装 Mapper 调用，为 Service 层提供数据访问能力
 */
@Repository
public class DictDataRepository {

    @Autowired
    private DictDataMapper dictDataMapper;

    /**
     * 根据主键查询（仅未删除）
     */
    public DictData selectById(Long id) {
        return dictDataMapper.selectById(id);
    }

    /**
     * 根据字典类型 + 字典 key 查询（仅未删除）
     */
    public DictData selectByTypeAndKey(String dictType, String dictKey) {
        return dictDataMapper.selectByTypeAndKey(dictType, dictKey);
    }

    /**
     * 统计指定字典类型 + key 的记录数（用于唯一性校验）
     *
     * @param dictType  字典类型标识
     * @param dictKey   数据名称
     * @param excludeId 排除的主键，可为 null
     */
    public long countByTypeAndKey(String dictType, String dictKey, Long excludeId) {
        return dictDataMapper.countByTypeAndKey(dictType, dictKey, excludeId);
    }

    /**
     * 统计指定字典类型下的数据条数（用于删除字典类型前的占用校验）
     */
    public long countByDictType(String dictType) {
        return dictDataMapper.countByDictType(dictType);
    }

    /**
     * 查询指定字典类型下的全部数据（仅未删除，按排序值升序）
     */
    public List<DictData> listByDictType(String dictType) {
        return dictDataMapper.listByDictType(dictType);
    }

    /**
     * 分页查询（支持 dictType 精确、dictKey / dictValue 模糊，支持按字段排序）
     *
     * @param pageNum   页码（从 1 开始）
     * @param pageSize  每页大小
     * @param dictType  字典类型标识（精确，可为 null）
     * @param dictKey   数据名称（模糊，可为 null）
     * @param dictValue 数据值（模糊，可为 null）
     * @param sortField 排序列名（snake_case，已白名单校验，可为 null）
     * @param sortOrder 排序方向（asc / desc，可为 null）
     */
    public IPage<DictData> pageQuery(long pageNum, long pageSize, String dictType, String dictKey, String dictValue,
                                     String sortField, String sortOrder) {
        Page<DictData> page = new Page<>(pageNum, pageSize);
        return dictDataMapper.pageQuery(page, dictType, dictKey, dictValue, sortField, sortOrder);
    }

    /**
     * 新增
     */
    public int insert(DictData entity) {
        return dictDataMapper.insert(entity);
    }

    /**
     * 根据主键更新（null 字段不参与更新）
     */
    public int updateById(DictData entity) {
        return dictDataMapper.updateById(entity);
    }

    /**
     * 根据主键逻辑删除
     */
    public int deleteById(Long id) {
        return dictDataMapper.deleteById(id);
    }
}
