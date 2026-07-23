package com.ci.pipeline.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ci.pipeline.dao.entity.AppInfo;
import org.apache.ibatis.annotations.Param;

/**
 * 应用基础信息 Mapper
 */
public interface AppInfoMapper extends BaseMapper<AppInfo> {

    /**
     * 根据应用名称查询（仅未删除）
     *
     * @param appName 应用名称
     * @return 应用信息实体，不存在返回 null
     */
    AppInfo selectByAppName(@Param("appName") String appName);

    /**
     * 统计指定应用名称的记录数（用于唯一性校验）
     *
     * @param appName    应用名称
     * @param excludeId  排除的主键（更新校验时传入自身 id），可为 null
     * @return 未删除记录数
     */
    long countByAppName(@Param("appName") String appName,
                        @Param("excludeId") Long excludeId);

    /**
     * 分页查询应用信息（支持 appName 模糊，支持按字段排序）
     *
     * @param page      分页参数（由 MyBatis-Plus 分页插件自动改写）
     * @param appName   应用名称（模糊）
     * @param sortField 排序列名（snake_case，已在 Service 层白名单校验，可为 null）
     * @param sortOrder 排序方向（asc / desc，可为 null）
     * @return 分页结果
     */
    IPage<AppInfo> pageQuery(IPage<AppInfo> page,
                             @Param("appName") String appName,
                             @Param("sortField") String sortField,
                             @Param("sortOrder") String sortOrder);
}
