package com.ci.pipeline.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ci.pipeline.dao.entity.PipelineCleanRun;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 流水线终态清理记录 Mapper
 */
public interface PipelineCleanRunMapper extends BaseMapper<PipelineCleanRun> {

    /**
     * 超时兜底扫描：Running 且创建时间早于阈值（走 idx_status_create_time）
     */
    List<PipelineCleanRun> listTimeoutRunning(@Param("deadline") Date deadline);

    /**
     * CAS 置成功：仅 Running → Succeeded，终态不可再变（幂等核心）
     */
    int casMarkSucceeded(@Param("id") Long id);

    /**
     * CAS 置失败：仅 Running → Failed，终态不可再变（幂等核心）
     */
    int casMarkFailed(@Param("id") Long id);

    /**
     * 回填清理流水线名称（提交成功后）
     */
    int updateCleanRunName(@Param("id") Long id, @Param("cleanRunName") String cleanRunName);
}
