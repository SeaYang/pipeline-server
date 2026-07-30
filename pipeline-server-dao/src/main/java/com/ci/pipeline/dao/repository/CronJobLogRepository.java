package com.ci.pipeline.dao.repository;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ci.pipeline.dao.entity.CronJobLog;
import com.ci.pipeline.dao.mapper.CronJobLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Date;

@Repository
public class CronJobLogRepository {

    @Autowired
    private CronJobLogMapper cronJobLogMapper;

    public CronJobLog selectById(Long id) {
        return cronJobLogMapper.selectById(id);
    }

    public int insert(CronJobLog entity) {
        return cronJobLogMapper.insert(entity);
    }

    /**
     * 非 CAS 更新：仅供执行线程自己写终态（正常结束/异常/中断）时使用，
     * 调用方需保证不存在与"手动停止"路径并发覆盖的风险（该场景走 {@link #updateStatusIfRunning}）。
     */
    public int updateStatus(Long id, String status, String message, Date endTime, Long costMs) {
        return cronJobLogMapper.update(null, new LambdaUpdateWrapper<CronJobLog>()
                .eq(CronJobLog::getId, id)
                .set(CronJobLog::getStatus, status)
                .set(message != null, CronJobLog::getMessage, message)
                .set(CronJobLog::getEndTime, endTime)
                .set(CronJobLog::getCostMs, costMs));
    }

    /**
     * CAS 更新：仅当当前仍是 running 才生效，用于"手动停止"路径，避免覆盖任务线程自然结束时已写入的终态。
     */
    public int updateStatusIfRunning(Long id, String status, String message, Date endTime, Long costMs) {
        return cronJobLogMapper.updateStatusIfRunning(id, status, message, endTime, costMs);
    }

    public long countRunning(Long jobId, Long excludeId) {
        return cronJobLogMapper.countRunning(jobId, excludeId);
    }

    public IPage<CronJobLog> pageQuery(long pageNum, long pageSize, Long jobId, String status) {
        IPage<CronJobLog> page = new Page<>(pageNum, pageSize);
        return cronJobLogMapper.pageQuery(page, jobId, status);
    }
}
