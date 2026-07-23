package com.ci.pipeline.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ci.pipeline.dao.entity.PipelineRunSnapshot;
import org.apache.ibatis.annotations.Param;

/**
 * 流水线执行详情快照 Mapper
 */
public interface PipelineRunSnapshotMapper extends BaseMapper<PipelineRunSnapshot> {

    /**
     * 按流水线执行 id 查询快照（至多一条，由 upsert 保证 1:1）
     *
     * @param pipelineRunId 流水线执行 id（字符串形式）
     * @return 快照，不存在返回 null
     */
    PipelineRunSnapshot selectByPipelineRunId(@Param("pipelineRunId") String pipelineRunId);

    /**
     * 按主键更新详情
     *
     * @param id     主键
     * @param detail 详情 json
     * @return 影响行数
     */
    int updateDetail(@Param("id") Long id, @Param("detail") String detail);
}
