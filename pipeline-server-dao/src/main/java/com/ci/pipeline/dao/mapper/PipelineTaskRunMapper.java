package com.ci.pipeline.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ci.pipeline.dao.entity.PipelineTaskRun;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 流水线执行-任务节点记录 Mapper
 */
public interface PipelineTaskRunMapper extends BaseMapper<PipelineTaskRun> {

    /**
     * 批量新增任务节点记录
     *
     * @param list 任务节点记录列表
     * @return 插入条数
     */
    int batchInsert(@Param("list") List<PipelineTaskRun> list);

    /**
     * 按流水线执行 id 物理删除任务节点记录（终态落地采用「先删后插」，不走逻辑删除）
     *
     * @param pipelineRunId 流水线执行 id
     * @return 删除条数
     */
    int deleteByPipelineRunId(@Param("pipelineRunId") Long pipelineRunId);
}
