package com.ci.pipeline.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ci.pipeline.dao.entity.CronJobLog;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

public interface CronJobLogMapper extends BaseMapper<CronJobLog> {

    /**
     * CAS 更新：仅当当前状态仍是 running 时才生效，避免"手动停止"与"任务线程自然结束"两条路径互相覆盖终态。
     *
     * @return 受影响行数，0 表示当前记录已不是 running 状态（已被其他路径写过终态）
     */
    int updateStatusIfRunning(@Param("id") Long id,
                               @Param("status") String status,
                               @Param("message") String message,
                               @Param("endTime") Date endTime,
                               @Param("costMs") Long costMs);

    /**
     * 统计某任务当前处于 running 状态的执行记录数（排除自身，用于禁止并发校验）。
     */
    long countRunning(@Param("jobId") Long jobId, @Param("excludeId") Long excludeId);

    IPage<CronJobLog> pageQuery(IPage<CronJobLog> page,
                                 @Param("jobId") Long jobId,
                                 @Param("status") String status);
}
