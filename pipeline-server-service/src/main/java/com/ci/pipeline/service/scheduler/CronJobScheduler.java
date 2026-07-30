package com.ci.pipeline.service.scheduler;

import com.ci.pipeline.common.constants.CronJobConstants;
import com.ci.pipeline.common.enums.MisfirePolicyEnum;
import com.ci.pipeline.common.util.CronUtils;
import com.ci.pipeline.common.util.IpUtils;
import com.ci.pipeline.dao.entity.CronJob;
import com.ci.pipeline.dao.entity.CronJobLog;
import com.ci.pipeline.dao.repository.CronJobLogRepository;
import com.ci.pipeline.dao.repository.CronJobRepository;
import com.ci.pipeline.service.service.DistributedLockService;
import com.ci.pipeline.service.util.JobInvokeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * 定时任务调度器。
 * <p>单实例内以固定频率扫描到期任务；多实例部署下通过 cron_job.revision 乐观锁抢占，
 * 保证同一次触发只会被一个实例执行；执行期间的并发互斥、跨实例停止依赖 {@link DistributedLockService}。
 */
@Slf4j
@Component
public class CronJobScheduler {

    /** 错过阈值（毫秒）：超过该时长才判定为"错过"，避免把扫描间隔内的正常延迟误判为 misfire */
    private static final long MISFIRE_THRESHOLD_MS = 60_000L;

    /** 执行锁过期时间下限（秒），覆盖触发间隔极短的任务，避免锁提前过期 */
    private static final int LOCK_EXPIRE_FLOOR_SECONDS = 120;

    @Autowired
    private CronJobRepository cronJobRepository;

    @Autowired
    private CronJobLogRepository cronJobLogRepository;

    @Autowired
    private DistributedLockService distributedLockService;

    @Autowired
    @Qualifier("cronJobExecutor")
    private ThreadPoolTaskExecutor cronJobExecutor;

    @Autowired
    private ApplicationContext applicationContext;

    /** logId -> Future，仅用于本实例内"停止任务"（跨实例停止走 InternalHttpClient 通知目标实例） */
    private final ConcurrentHashMap<Long, Future<?>> runningFutures = new ConcurrentHashMap<>();

    @Scheduled(fixedDelay = 1000)
    public void scan() {
        List<CronJob> dueJobs = cronJobRepository.listDueJobs(new Date());
        if (dueJobs.isEmpty()) {
            return;
        }

        for (CronJob job : dueJobs) {
            try {
                scanOne(job);
            } catch (Exception e) {
                log.error("扫描处理任务异常: jobId={}, name={}", job.getId(), job.getName(), e);
            }
        }
    }

    private void scanOne(CronJob job) {
        Date nextFireTime = CronUtils.getNextExecution(job.getCronExpr());
        if (nextFireTime == null) {
            log.warn("任务CRON表达式无法计算下次执行时间，跳过: jobId={}, cronExpr={}", job.getId(), job.getCronExpr());
            return;
        }

        // CAS 抢占：只有抢占成功（受影响行数>0）的实例才会执行本次触发
        int affected = cronJobRepository.claimAndSchedule(job.getId(), job.getRevision(), nextFireTime, new Date());
        if (affected == 0) {
            return;
        }

        if (MisfirePolicyEnum.SKIP.getCode().equals(job.getMisfirePolicy()) && isMisfired(job)) {
            log.warn("任务错过执行时间且策略为skip，跳过本次触发: jobId={}, name={}", job.getId(), job.getName());
            return;
        }

        submitExecution(job, nextFireTime);
    }

    private boolean isMisfired(CronJob job) {
        Date originalNextFireTime = job.getNextFireTime();
        return originalNextFireTime != null
                && System.currentTimeMillis() - originalNextFireTime.getTime() > MISFIRE_THRESHOLD_MS;
    }

    /**
     * 提交一次任务执行：先写入 running 状态的执行日志，再提交线程池异步执行，并登记 Future 供本实例停止使用。
     * 调度扫描（{@link #scan()}）与手动触发（CronJobService#triggerManually）共用此方法。
     *
     * @param job          任务定义
     * @param nextFireTime 本次触发对应的下一次触发时间，用于估算执行锁过期时间
     * @return 执行日志ID
     */
    public Long submitExecution(CronJob job, Date nextFireTime) {
        CronJobLog jobLog = new CronJobLog();
        jobLog.setJobId(job.getId());
        jobLog.setJobName(job.getName());
        jobLog.setBeanName(job.getBeanName());
        jobLog.setMethodName(job.getMethodName());
        jobLog.setMethodParams(job.getMethodParams());
        jobLog.setStatus(CronJobConstants.STATUS_RUNNING);
        jobLog.setInstanceIp(IpUtils.getLocalIp());
        jobLog.setStartTime(new Date());
        cronJobLogRepository.insert(jobLog);

        Future<?> future = cronJobExecutor.submit(() -> executeJob(job, nextFireTime, jobLog));
        runningFutures.put(jobLog.getId(), future);
        return jobLog.getId();
    }

    private void executeJob(CronJob job, Date nextFireTime, CronJobLog jobLog) {
        try {
            if (Integer.valueOf(0).equals(job.getConcurrent())
                    && cronJobLogRepository.countRunning(job.getId(), jobLog.getId()) > 0) {
                finishLog(jobLog, CronJobConstants.STATUS_FAILED, "已有执行中的记录，禁止并发");
                return;
            }

            String lockKey = "cron-job:execute:" + job.getId();
            String lockValue = null;
            if (Integer.valueOf(0).equals(job.getConcurrent())) {
                long expireSeconds = Math.max(
                        (nextFireTime.getTime() - System.currentTimeMillis()) / 1000, LOCK_EXPIRE_FLOOR_SECONDS);
                lockValue = distributedLockService.tryLock(lockKey, (int) expireSeconds, "cron-job:" + job.getName());
                if (lockValue == null) {
                    finishLog(jobLog, CronJobConstants.STATUS_FAILED, "未能获取执行锁，可能有其他实例正在执行");
                    return;
                }
            }

            try {
                JobInvokeUtil.invokeMethod(applicationContext, job.getBeanName(), job.getMethodName(), job.getMethodParams());
                finishLog(jobLog, CronJobConstants.STATUS_SUCCEEDED, null);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                // 已被"手动停止"路径 CAS 写过终态，这里只做兜底（大多数情况下 CAS 会因状态已非 running 而不生效）
                cronJobLogRepository.updateStatusIfRunning(jobLog.getId(), CronJobConstants.STATUS_FAILED,
                        "任务被中断", new Date(), System.currentTimeMillis() - jobLog.getStartTime().getTime());
            } catch (Exception e) {
                finishLog(jobLog, CronJobConstants.STATUS_FAILED, ExceptionUtils.getStackTrace(e));
            } finally {
                if (lockValue != null) {
                    distributedLockService.unlock(lockKey, lockValue);
                }
            }
        } finally {
            runningFutures.remove(jobLog.getId());
        }
    }

    private void finishLog(CronJobLog jobLog, String status, String message) {
        long costMs = System.currentTimeMillis() - jobLog.getStartTime().getTime();
        cronJobLogRepository.updateStatus(jobLog.getId(), status, message, new Date(), costMs);
    }

    /**
     * 本实例停止任务：通过 logId 找到对应 Future 并中断执行线程。
     * 找不到 Future（任务已结束/进程重启后 Future 已丢失）时返回 false。
     */
    public boolean stopLocal(Long logId) {
        Future<?> future = runningFutures.get(logId);
        return future != null && future.cancel(true);
    }
}
