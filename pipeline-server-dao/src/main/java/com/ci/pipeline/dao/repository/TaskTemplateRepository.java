package com.ci.pipeline.dao.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ci.pipeline.dao.entity.TaskTemplate;
import com.ci.pipeline.dao.mapper.TaskTemplateMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 任务模板数据访问，封装 Mapper 调用，为 Service 层提供数据访问能力
 */
@Repository
public class TaskTemplateRepository {

    @Autowired
    private TaskTemplateMapper taskTemplateMapper;

    /**
     * 根据主键查询（仅未删除）
     */
    public TaskTemplate selectById(Long id) {
        return taskTemplateMapper.selectById(id);
    }

    /**
     * 根据任务模板编码查询（仅未删除）
     */
    public TaskTemplate selectByTaskTemplateCode(String taskTemplateCode) {
        return taskTemplateMapper.selectByTaskTemplateCode(taskTemplateCode);
    }

    /**
     * 统计指定任务模板编码的记录数（用于唯一性校验）
     *
     * @param taskTemplateCode 任务模板编码
     * @param excludeId        排除的主键，可为 null
     */
    public long countByTaskTemplateCode(String taskTemplateCode, Long excludeId) {
        return taskTemplateMapper.countByTaskTemplateCode(taskTemplateCode, excludeId);
    }

    /**
     * 分页查询（支持 taskTemplateCode / name 模糊、taskTemplateGroup 精确，支持按字段排序）
     *
     * @param pageNum           页码（从 1 开始）
     * @param pageSize          每页大小
     * @param taskTemplateCode  任务模板编码（模糊，可为 null）
     * @param name              任务模板名称（模糊，可为 null）
     * @param taskTemplateGroup 任务模板所属分组（精确，可为 null）
     * @param sortField         排序列名（snake_case，已白名单校验，可为 null）
     * @param sortOrder         排序方向（asc / desc，可为 null）
     */
    public IPage<TaskTemplate> pageQuery(long pageNum, long pageSize, String taskTemplateCode, String name,
                                         String taskTemplateGroup, String sortField, String sortOrder) {
        Page<TaskTemplate> page = new Page<>(pageNum, pageSize);
        return taskTemplateMapper.pageQuery(page, taskTemplateCode, name, taskTemplateGroup, sortField, sortOrder);
    }

    /**
     * 新增
     */
    public int insert(TaskTemplate entity) {
        return taskTemplateMapper.insert(entity);
    }

    /**
     * 根据主键更新（null 字段不参与更新）
     */
    public int updateById(TaskTemplate entity) {
        return taskTemplateMapper.updateById(entity);
    }

    /**
     * 根据主键逻辑删除
     */
    public int deleteById(Long id) {
        return taskTemplateMapper.deleteById(id);
    }

    /**
     * 根据任务模板编码集合批量查询（仅未删除）
     *
     * @param codes 任务模板编码集合，为空返回空列表
     */
    public List<TaskTemplate> listByCodes(Collection<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return Collections.emptyList();
        }
        return taskTemplateMapper.selectList(
                new LambdaQueryWrapper<TaskTemplate>().in(TaskTemplate::getTaskTemplateCode, codes));
    }
}
