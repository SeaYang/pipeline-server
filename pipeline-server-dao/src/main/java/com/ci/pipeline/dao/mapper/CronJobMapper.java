package com.ci.pipeline.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ci.pipeline.dao.entity.CronJob;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

public interface CronJobMapper extends BaseMapper<CronJob> {

    /**
     * 乐观锁抢占：CAS 更新 next_fire_time / last_fire_time / revision。
     * WHERE id + revision + enabled=1 保证并发安全，多实例同时扫描时只有一个能抢占成功。
     *
     * @return 受影响行数，0 表示抢占失败（已被其他实例抢占，或任务已被停用/删除）
     */
    int claimAndSchedule(@Param("id") Long id,
                          @Param("oldRevision") Integer oldRevision,
                          @Param("nextFireTime") Date nextFireTime,
                          @Param("now") Date now);

    IPage<CronJob> pageQuery(IPage<CronJob> page,
                              @Param("name") String name,
                              @Param("enabled") Integer enabled);
}
