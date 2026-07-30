package com.ci.pipeline.dao.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ci.pipeline.dao.entity.CronJob;
import com.ci.pipeline.dao.mapper.CronJobMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public class CronJobRepository {

    @Autowired
    private CronJobMapper cronJobMapper;

    public CronJob selectById(Long id) {
        return cronJobMapper.selectById(id);
    }

    public int insert(CronJob entity) {
        return cronJobMapper.insert(entity);
    }

    public int deleteById(Long id) {
        return cronJobMapper.deleteById(id);
    }

    /**
     * 查询所有到期待调度任务：enabled=1 且 next_fire_time &lt;= now。
     * 走 MyBatis-Plus wrapper 查询，全局逻辑删除配置会自动追加 deleted=0 条件。
     */
    public List<CronJob> listDueJobs(Date now) {
        return cronJobMapper.selectList(
                new LambdaQueryWrapper<CronJob>()
                        .eq(CronJob::getEnabled, 1)
                        .le(CronJob::getNextFireTime, now));
    }

    public int claimAndSchedule(Long id, Integer oldRevision, Date nextFireTime, Date now) {
        return cronJobMapper.claimAndSchedule(id, oldRevision, nextFireTime, now);
    }

    /**
     * 更新任务定义字段，同时把 revision 重置为 0（编辑后旧 revision 语义失效，避免与调度扫描的 CAS 产生歧义）。
     * 用显式 set 而非 updateById，是为了在 methodParams 被清空为 null 时也能正确写回数据库
     * （updateById 对 null 字段会跳过更新，导致清空操作实际不生效）。
     */
    public int updateDefinition(CronJob entity) {
        return cronJobMapper.update(null, new LambdaUpdateWrapper<CronJob>()
                .eq(CronJob::getId, entity.getId())
                .set(CronJob::getName, entity.getName())
                .set(CronJob::getBeanName, entity.getBeanName())
                .set(CronJob::getMethodName, entity.getMethodName())
                .set(CronJob::getMethodParams, entity.getMethodParams())
                .set(CronJob::getCronExpr, entity.getCronExpr())
                .set(CronJob::getMisfirePolicy, entity.getMisfirePolicy())
                .set(CronJob::getConcurrent, entity.getConcurrent())
                .set(CronJob::getNextFireTime, entity.getNextFireTime())
                .set(CronJob::getRevision, 0));
    }

    /**
     * 启用/停用任务，同时更新 next_fire_time（停用时置空）并重置 revision。
     */
    public int updateEnabled(Long id, Integer enabled, Date nextFireTime) {
        return cronJobMapper.update(null, new LambdaUpdateWrapper<CronJob>()
                .eq(CronJob::getId, id)
                .set(CronJob::getEnabled, enabled)
                .set(CronJob::getNextFireTime, nextFireTime)
                .set(CronJob::getRevision, 0));
    }

    public IPage<CronJob> pageQuery(long pageNum, long pageSize, String name, Integer enabled) {
        IPage<CronJob> page = new Page<>(pageNum, pageSize);
        return cronJobMapper.pageQuery(page, name, enabled);
    }
}
