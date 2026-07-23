package com.ci.pipeline.dao.repository;

import com.ci.pipeline.dao.entity.PipelineRunSnapshot;
import com.ci.pipeline.dao.mapper.PipelineRunSnapshotMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * 流水线执行详情快照数据访问，封装 Mapper 调用，为 Service 层提供数据访问能力。
 * <p>upsert 语义：按 pipeline_run_id 查到则更新 detail，否则插入，保证与 pipeline_run 一对一。
 */
@Repository
public class PipelineRunSnapshotRepository {

    @Autowired
    private PipelineRunSnapshotMapper pipelineRunSnapshotMapper;

    /**
     * 按流水线执行 id 查询快照
     */
    public PipelineRunSnapshot selectByPipelineRunId(String pipelineRunId) {
        return pipelineRunSnapshotMapper.selectByPipelineRunId(pipelineRunId);
    }

    /**
     * 新增
     */
    public int insert(PipelineRunSnapshot entity) {
        return pipelineRunSnapshotMapper.insert(entity);
    }

    /**
     * 按主键更新详情
     */
    public int updateDetail(Long id, String detail) {
        return pipelineRunSnapshotMapper.updateDetail(id, detail);
    }

    /**
     * upsert：按 pipeline_run_id 存在则更新 detail，否则插入。返回最终快照主键 id。
     *
     * @param pipelineRunId 流水线执行 id（字符串形式）
     * @param detail        详情 json
     * @return 快照主键 id
     */
    public Long upsertDetail(String pipelineRunId, String detail) {
        PipelineRunSnapshot existing = pipelineRunSnapshotMapper.selectByPipelineRunId(pipelineRunId);
        if (existing != null) {
            pipelineRunSnapshotMapper.updateDetail(existing.getId(), detail);
            return existing.getId();
        }
        PipelineRunSnapshot snapshot = new PipelineRunSnapshot();
        snapshot.setPipelineRunId(pipelineRunId);
        snapshot.setDetail(detail);
        pipelineRunSnapshotMapper.insert(snapshot);
        return snapshot.getId();
    }
}
