package com.ci.pipeline.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ci.pipeline.dao.entity.TaskTemplate;
import org.apache.ibatis.annotations.Param;

/**
 * 任务模板 Mapper
 */
public interface TaskTemplateMapper extends BaseMapper<TaskTemplate> {

    /**
     * 根据任务模板编码查询（仅未删除）
     *
     * @param taskTemplateCode 任务模板编码
     * @return 任务模板实体，不存在返回 null
     */
    TaskTemplate selectByTaskTemplateCode(@Param("taskTemplateCode") String taskTemplateCode);

    /**
     * 统计指定任务模板编码的记录数（用于唯一性校验）
     *
     * @param taskTemplateCode 任务模板编码
     * @param excludeId        排除的主键（更新校验时传入自身 id），可为 null
     * @return 未删除记录数
     */
    long countByTaskTemplateCode(@Param("taskTemplateCode") String taskTemplateCode,
                                 @Param("excludeId") Long excludeId);

    /**
     * 分页查询任务模板（支持 taskTemplateCode / name 模糊、taskTemplateGroup 精确，支持按字段排序）
     *
     * @param page              分页参数（由 MyBatis-Plus 分页插件自动改写）
     * @param taskTemplateCode  任务模板编码（模糊）
     * @param name              任务模板名称（模糊）
     * @param taskTemplateGroup 任务模板所属分组（精确）
     * @param sortField         排序列名（snake_case，已在 Service 层白名单校验，可为 null）
     * @param sortOrder         排序方向（asc / desc，可为 null）
     * @return 分页结果
     */
    IPage<TaskTemplate> pageQuery(IPage<TaskTemplate> page,
                                  @Param("taskTemplateCode") String taskTemplateCode,
                                  @Param("name") String name,
                                  @Param("taskTemplateGroup") String taskTemplateGroup,
                                  @Param("sortField") String sortField,
                                  @Param("sortOrder") String sortOrder);
}
