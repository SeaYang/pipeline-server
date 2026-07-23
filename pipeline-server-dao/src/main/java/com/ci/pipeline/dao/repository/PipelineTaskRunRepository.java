package com.ci.pipeline.dao.repository;

import com.ci.pipeline.dao.entity.PipelineTaskRun;
import com.ci.pipeline.dao.mapper.PipelineTaskRunMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 流水线执行-任务节点记录数据访问，封装 Mapper 调用，为 Service 层提供数据访问能力
 */
@Repository
public class PipelineTaskRunRepository {

    @Autowired
    private PipelineTaskRunMapper pipelineTaskRunMapper;

    /**
     * 批量新增
     */
    public int batchInsert(List<PipelineTaskRun> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }
        return pipelineTaskRunMapper.batchInsert(list);
    }

    /**
     * 按流水线执行 id 物理删除（终态落地「先删后插」用）
     */
    public int deleteByPipelineRunId(Long pipelineRunId) {
        return pipelineTaskRunMapper.deleteByPipelineRunId(pipelineRunId);
    }

    /**
     * 按流水线执行 id 查询全部任务节点记录（终态数据组装用）
     */
    public List<PipelineTaskRun> selectByPipelineRunId(Long pipelineRunId) {
        return pipelineTaskRunMapper.selectList(
                new LambdaQueryWrapper<PipelineTaskRun>()
                        .eq(PipelineTaskRun::getPipelineRunId, pipelineRunId));
    }

    /**
     * 按流水线执行 id + 任务编码查询单条记录（日志获取用）
     */
    public PipelineTaskRun selectByRunIdAndTaskCode(Long pipelineRunId, String taskCode) {
        return pipelineTaskRunMapper.selectOne(
                new LambdaQueryWrapper<PipelineTaskRun>()
                        .eq(PipelineTaskRun::getPipelineRunId, pipelineRunId)
                        .eq(PipelineTaskRun::getTaskCode, taskCode));
    }
}
