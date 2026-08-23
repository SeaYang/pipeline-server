package com.ci.pipeline.dao.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ci.pipeline.dao.entity.PipelineCleanRun;
import com.ci.pipeline.dao.mapper.PipelineCleanRunMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * 流水线终态清理记录 Repository
 */
@Repository
public class PipelineCleanRunRepository {

    @Autowired
    private PipelineCleanRunMapper pipelineCleanRunMapper;

    public int insert(PipelineCleanRun cleanRun) {
        return pipelineCleanRunMapper.insert(cleanRun);
    }

    public PipelineCleanRun selectByRunName(String runName) {
        return pipelineCleanRunMapper.selectOne(new LambdaQueryWrapper<PipelineCleanRun>()
                .eq(PipelineCleanRun::getRunName, runName));
    }

    public List<PipelineCleanRun> listTimeoutRunning(Date deadline) {
        return pipelineCleanRunMapper.listTimeoutRunning(deadline);
    }

    public int casMarkSucceeded(Long id) {
        return pipelineCleanRunMapper.casMarkSucceeded(id);
    }

    public int casMarkFailed(Long id) {
        return pipelineCleanRunMapper.casMarkFailed(id);
    }

    public int updateCleanRunName(Long id, String cleanRunName) {
        return pipelineCleanRunMapper.updateCleanRunName(id, cleanRunName);
    }
}
