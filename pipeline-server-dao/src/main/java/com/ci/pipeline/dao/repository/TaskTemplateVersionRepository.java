package com.ci.pipeline.dao.repository;

import com.ci.pipeline.dao.entity.TaskTemplateVersion;
import com.ci.pipeline.dao.mapper.TaskTemplateVersionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 任务模板版本数据访问，封装 Mapper 调用，为 Service 层提供数据访问能力
 */
@Repository
public class TaskTemplateVersionRepository {

    @Autowired
    private TaskTemplateVersionMapper taskTemplateVersionMapper;

    /**
     * 根据主键查询（仅未删除）
     */
    public TaskTemplateVersion selectById(Long id) {
        return taskTemplateVersionMapper.selectById(id);
    }

    /**
     * 根据任务模板编码 + 版本号查询（仅未删除）
     */
    public TaskTemplateVersion selectByCodeAndVersion(String taskTemplateCode, String version) {
        return taskTemplateVersionMapper.selectByCodeAndVersion(taskTemplateCode, version);
    }

    /**
     * 统计指定任务模板编码下的版本记录数（用于删除模板前的占用校验）
     */
    public long countByCode(String taskTemplateCode) {
        return taskTemplateVersionMapper.countByCode(taskTemplateCode);
    }

    /**
     * 查询指定任务模板编码下的全部版本号（仅 version 列，用于递增校验求最大值）
     */
    public List<String> listVersionsByCode(String taskTemplateCode) {
        return taskTemplateVersionMapper.listVersionsByCode(taskTemplateCode);
    }

    /**
     * 查询指定任务模板编码下的全部版本（含完整字段，用于版本列表）。SQL 不排序。
     */
    public List<TaskTemplateVersion> listByCode(String taskTemplateCode) {
        return taskTemplateVersionMapper.listByCode(taskTemplateCode);
    }

    /**
     * 更新单个版本的状态（按任务模板编码 + 版本号定位）
     */
    public int updateStatusByCodeAndVersion(String taskTemplateCode, String version, String status) {
        return taskTemplateVersionMapper.updateStatusByCodeAndVersion(taskTemplateCode, version, status);
    }

    /**
     * 某版本生效时，把指定模板下、除指定版本外、尚未失效的其它版本（生效中/草稿）统一置为已失效。
     */
    public int updateOtherStatusToExpired(String taskTemplateCode, String excludeVersion) {
        return taskTemplateVersionMapper.updateOtherStatusToExpired(taskTemplateCode, excludeVersion);
    }

    /**
     * 新增
     */
    public int insert(TaskTemplateVersion entity) {
        return taskTemplateVersionMapper.insert(entity);
    }

    /**
     * 根据主键更新（null 字段不参与更新）
     */
    public int updateById(TaskTemplateVersion entity) {
        return taskTemplateVersionMapper.updateById(entity);
    }

    /**
     * 根据主键逻辑删除
     */
    public int deleteById(Long id) {
        return taskTemplateVersionMapper.deleteById(id);
    }
}
