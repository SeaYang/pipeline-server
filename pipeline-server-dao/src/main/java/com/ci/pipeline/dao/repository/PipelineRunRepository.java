package com.ci.pipeline.dao.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ci.pipeline.dao.entity.PipelineRun;
import com.ci.pipeline.dao.mapper.PipelineRunMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * 流水线执行记录数据访问，封装 Mapper 调用，为 Service 层提供数据访问能力
 */
@Repository
public class PipelineRunRepository {

    @Autowired
    private PipelineRunMapper pipelineRunMapper;

    /**
     * 根据主键查询（仅未删除）
     */
    public PipelineRun selectById(Long id) {
        return pipelineRunMapper.selectById(id);
    }

    /**
     * 根据执行名称（Argo Workflow 名称）查询（仅未删除），不存在返回 null
     */
    public PipelineRun selectByName(String name) {
        return pipelineRunMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PipelineRun>()
                        .eq(PipelineRun::getName, name));
    }

    /**
     * 分页查询（支持 pipelineId / appName / status 精确过滤，支持按字段排序，默认按创建时间倒序）
     *
     * @param pageNum    页码（从 1 开始）
     * @param pageSize   每页大小
     * @param pipelineId 流水线 id（精确，可为 null）
     * @param appName    应用名称（精确，可为 null）
     * @param status     执行状态（精确，可为 null）
     * @param sortField  排序列名（snake_case，已白名单校验，可为 null）
     * @param sortOrder  排序方向（asc / desc，可为 null）
     */
    public IPage<PipelineRun> pageQuery(long pageNum, long pageSize, Long pipelineId,
                                        String appName, String status,
                                        String sortField, String sortOrder) {
        Page<PipelineRun> page = new Page<>(pageNum, pageSize);
        return pipelineRunMapper.pageQuery(page, pipelineId, appName, status, sortField, sortOrder);
    }

    /**
     * 按流水线 id 查询最近一次执行记录（id 最大的一条），无记录返回 null
     */
    public PipelineRun selectLatestByPipelineId(Long pipelineId) {
        return pipelineRunMapper.selectLatestByPipelineId(pipelineId);
    }

    /**
     * 查询状态为指定值、且更新时间早于给定阈值的执行记录（仅未删除），供兜底同步定时任务扫描中断的执行记录使用
     *
     * @param status           执行状态编码（精确匹配）
     * @param beforeUpdateTime 阈值时间，update_time 早于该时间才返回
     * @return 命中的执行记录列表，无结果返回空列表
     */
    public List<PipelineRun> selectStaleRunning(String status, Date beforeUpdateTime) {
        return pipelineRunMapper.selectList(
                new LambdaQueryWrapper<PipelineRun>()
                        .eq(PipelineRun::getStatus, status)
                        .le(PipelineRun::getUpdateTime, beforeUpdateTime));
    }

    /**
     * 新增
     */
    public int insert(PipelineRun entity) {
        return pipelineRunMapper.insert(entity);
    }

    /**
     * 带乐观锁的状态回写（见 {@link com.ci.pipeline.dao.mapper.PipelineRunMapper#updateForSync}）
     *
     * @return 影响行数（1=成功，0=版本冲突或记录不存在）
     */
    public int updateForSync(PipelineRun entity) {
        return pipelineRunMapper.updateForSync(entity);
    }

    /**
     * 重试前状态重置（见 {@link com.ci.pipeline.dao.mapper.PipelineRunMapper#resetForRetry}）
     *
     * @param id       执行记录 id
     * @param revision 当前 revision（乐观锁）
     * @return 影响行数（1=成功，0=版本冲突或记录不存在）
     */
    public int resetForRetry(Long id, Integer revision) {
        return pipelineRunMapper.resetForRetry(id, revision);
    }

    /**
     * 根据主键逻辑删除
     */
    public int deleteById(Long id) {
        return pipelineRunMapper.deleteById(id);
    }
}
