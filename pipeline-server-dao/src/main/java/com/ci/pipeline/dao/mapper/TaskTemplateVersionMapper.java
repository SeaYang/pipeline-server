package com.ci.pipeline.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ci.pipeline.dao.entity.TaskTemplateVersion;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 任务模板版本 Mapper
 */
public interface TaskTemplateVersionMapper extends BaseMapper<TaskTemplateVersion> {

    /**
     * 根据任务模板编码 + 版本号查询（仅未删除）
     *
     * @param taskTemplateCode 任务模板编码
     * @param version          版本号
     * @return 版本实体，不存在返回 null
     */
    TaskTemplateVersion selectByCodeAndVersion(@Param("taskTemplateCode") String taskTemplateCode,
                                               @Param("version") String version);

    /**
     * 统计指定任务模板编码下的版本记录数（用于删除模板前的占用校验）
     *
     * @param taskTemplateCode 任务模板编码
     * @return 未删除版本记录数
     */
    long countByCode(@Param("taskTemplateCode") String taskTemplateCode);

    /**
     * 查询指定任务模板编码下的全部版本号（仅未删除，仅返回 version 列，用于递增校验求最大值，避免拉取 longtext）。
     * <p>不排序，最大值由调用方在内存中解析比对。
     *
     * @param taskTemplateCode 任务模板编码
     * @return 版本号字符串列表
     */
    List<String> listVersionsByCode(@Param("taskTemplateCode") String taskTemplateCode);

    /**
     * 查询指定任务模板编码下的全部版本（仅未删除，包含完整字段，用于版本列表）。
     * <p>SQL 不排序，由调用方在内存中按创建时间倒序。
     *
     * @param taskTemplateCode 任务模板编码
     * @return 版本实体列表
     */
    List<TaskTemplateVersion> listByCode(@Param("taskTemplateCode") String taskTemplateCode);

    /**
     * 更新单个版本的状态（按任务模板编码 + 版本号定位）
     *
     * @param taskTemplateCode 任务模板编码
     * @param version          版本号
     * @param status           目标状态
     * @return 受影响行数
     */
    int updateStatusByCodeAndVersion(@Param("taskTemplateCode") String taskTemplateCode,
                                     @Param("version") String version,
                                     @Param("status") String status);

    /**
     * 某版本生效时，把指定模板下、除指定版本外、尚未失效的其它版本（生效中/草稿）统一置为已失效。
     * <p>用于「某版本生效时，其它生效中/草稿版本自动失效」，处理已失效的行保证幂等。
     *
     * @param taskTemplateCode 任务模板编码
     * @param excludeVersion   排除的版本号（生效的那一个）
     * @return 受影响行数
     */
    int updateOtherStatusToExpired(@Param("taskTemplateCode") String taskTemplateCode,
                                   @Param("excludeVersion") String excludeVersion);
}
