package com.ci.pipeline.dao.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ci.pipeline.dao.entity.AppInfo;
import com.ci.pipeline.dao.mapper.AppInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * 应用基础信息数据访问，封装 Mapper 调用，为 Service 层提供数据访问能力
 */
@Repository
public class AppInfoRepository {

    @Autowired
    private AppInfoMapper appInfoMapper;

    /**
     * 根据主键查询（仅未删除）
     */
    public AppInfo selectById(Long id) {
        return appInfoMapper.selectById(id);
    }

    /**
     * 根据应用名称查询（仅未删除）
     */
    public AppInfo selectByAppName(String appName) {
        return appInfoMapper.selectByAppName(appName);
    }

    /**
     * 统计指定应用名称的记录数（用于唯一性校验）
     *
     * @param appName   应用名称
     * @param excludeId 排除的主键，可为 null
     */
    public long countByAppName(String appName, Long excludeId) {
        return appInfoMapper.countByAppName(appName, excludeId);
    }

    /**
     * 分页查询（支持 appName 模糊，支持按字段排序）
     *
     * @param pageNum   页码（从 1 开始）
     * @param pageSize  每页大小
     * @param appName   应用名称（模糊，可为 null）
     * @param sortField 排序列名（snake_case，已白名单校验，可为 null）
     * @param sortOrder 排序方向（asc / desc，可为 null）
     */
    public IPage<AppInfo> pageQuery(long pageNum, long pageSize, String appName, String sortField, String sortOrder) {
        Page<AppInfo> page = new Page<>(pageNum, pageSize);
        return appInfoMapper.pageQuery(page, appName, sortField, sortOrder);
    }

    /**
     * 新增
     */
    public int insert(AppInfo entity) {
        return appInfoMapper.insert(entity);
    }

    /**
     * 根据主键更新（null 字段不参与更新）
     */
    public int updateById(AppInfo entity) {
        return appInfoMapper.updateById(entity);
    }

    /**
     * 根据主键逻辑删除
     */
    public int deleteById(Long id) {
        return appInfoMapper.deleteById(id);
    }
}
