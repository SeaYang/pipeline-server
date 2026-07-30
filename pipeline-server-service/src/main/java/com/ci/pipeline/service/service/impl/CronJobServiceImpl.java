package com.ci.pipeline.service.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ci.pipeline.common.constants.CronJobConstants;
import com.ci.pipeline.common.enums.MisfirePolicyEnum;
import com.ci.pipeline.common.exception.BusinessException;
import com.ci.pipeline.common.util.CronUtils;
import com.ci.pipeline.common.util.IpUtils;
import com.ci.pipeline.dao.entity.CronJob;
import com.ci.pipeline.dao.entity.CronJobLog;
import com.ci.pipeline.dao.repository.CronJobLogRepository;
import com.ci.pipeline.dao.repository.CronJobRepository;
import com.ci.pipeline.facade.request.CronJobCreateRequest;
import com.ci.pipeline.facade.request.CronJobLogQueryRequest;
import com.ci.pipeline.facade.request.CronJobQueryRequest;
import com.ci.pipeline.facade.request.CronJobUpdateRequest;
import com.ci.pipeline.facade.response.CronJobLogResponse;
import com.ci.pipeline.facade.response.CronJobResponse;
import com.ci.pipeline.facade.response.PageResponse;
import com.ci.pipeline.service.remote.InternalHttpClient;
import com.ci.pipeline.service.scheduler.CronJobScheduler;
import com.ci.pipeline.service.service.CronJobService;
import com.ci.pipeline.service.util.JobInvokeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CronJobServiceImpl implements CronJobService {

    private static final long DEFAULT_PAGE_NUM = 1;
    private static final long DEFAULT_PAGE_SIZE = 10;
    /** 触发时刻无法算出下次执行时间（理论上不会发生，因为 cronExpr 已在创建/编辑时校验过）时的执行锁时长兜底值 */
    private static final long MANUAL_TRIGGER_LOCK_FLOOR_MS = 120_000L;

    @Autowired
    private CronJobRepository cronJobRepository;

    @Autowired
    private CronJobLogRepository cronJobLogRepository;

    @Autowired
    private CronJobScheduler cronJobScheduler;

    @Autowired
    private InternalHttpClient internalHttpClient;

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public CronJobResponse create(CronJobCreateRequest request) {
        validateName(request.getName());
        validateBeanName(request.getBeanName());
        validateMethodName(request.getMethodName());
        validateMethodParamsLength(request.getMethodParams());
        String cronExpr = validateCronExpr(request.getCronExpr());
        String misfirePolicy = validateMisfirePolicy(request.getMisfirePolicy());
        validateBeanMethod(request.getBeanName(), request.getMethodName(), request.getMethodParams());

        CronJob job = new CronJob();
        job.setName(request.getName());
        job.setBeanName(request.getBeanName());
        job.setMethodName(request.getMethodName());
        job.setMethodParams(StringUtils.trimToNull(request.getMethodParams()));
        job.setCronExpr(cronExpr);
        job.setEnabled(request.getEnabled() != null ? request.getEnabled() : 1);
        job.setMisfirePolicy(misfirePolicy);
        job.setConcurrent(request.getConcurrent() != null ? request.getConcurrent() : 0);
        job.setNextFireTime(job.getEnabled() == 1 ? CronUtils.getNextExecution(cronExpr) : null);
        job.setRevision(0);
        cronJobRepository.insert(job);
        log.info("新建定时任务成功: id={}, name={}", job.getId(), job.getName());
        return toResponse(job);
    }

    @Override
    public CronJobResponse update(CronJobUpdateRequest request) {
        if (request.getId() == null) {
            throw new BusinessException(CronJobConstants.MSG_ID_REQUIRED);
        }
        CronJob existing = cronJobRepository.selectById(request.getId());
        if (existing == null) {
            throw new BusinessException(CronJobConstants.MSG_JOB_NOT_FOUND);
        }

        validateName(request.getName());
        validateBeanName(request.getBeanName());
        validateMethodName(request.getMethodName());
        validateMethodParamsLength(request.getMethodParams());
        String cronExpr = validateCronExpr(request.getCronExpr());
        String misfirePolicy = validateMisfirePolicy(request.getMisfirePolicy());
        validateBeanMethod(request.getBeanName(), request.getMethodName(), request.getMethodParams());

        CronJob job = new CronJob();
        job.setId(request.getId());
        job.setName(request.getName());
        job.setBeanName(request.getBeanName());
        job.setMethodName(request.getMethodName());
        job.setMethodParams(StringUtils.trimToNull(request.getMethodParams()));
        job.setCronExpr(cronExpr);
        job.setMisfirePolicy(misfirePolicy);
        job.setConcurrent(request.getConcurrent() != null ? request.getConcurrent() : 0);
        // 编辑后重新计算下次触发时间；revision 一并重置为 0（见 CronJobRepository#updateDefinition 说明）
        job.setNextFireTime(existing.getEnabled() == 1 ? CronUtils.getNextExecution(cronExpr) : null);
        cronJobRepository.updateDefinition(job);
        log.info("编辑定时任务成功: id={}, name={}", job.getId(), job.getName());
        return toResponse(cronJobRepository.selectById(job.getId()));
    }

    @Override
    public void delete(Long id) {
        CronJob existing = cronJobRepository.selectById(id);
        if (existing == null) {
            throw new BusinessException(CronJobConstants.MSG_JOB_NOT_FOUND);
        }
        cronJobRepository.deleteById(id);
        log.info("删除定时任务成功: id={}, name={}", id, existing.getName());
    }

    @Override
    public CronJobResponse getById(Long id) {
        CronJob job = cronJobRepository.selectById(id);
        if (job == null) {
            throw new BusinessException(CronJobConstants.MSG_JOB_NOT_FOUND);
        }
        return toResponse(job);
    }

    @Override
    public PageResponse<CronJobResponse> page(CronJobQueryRequest query) {
        long pageNum = query.getPageNum() != null ? query.getPageNum() : DEFAULT_PAGE_NUM;
        long pageSize = query.getPageSize() != null ? query.getPageSize() : DEFAULT_PAGE_SIZE;
        IPage<CronJob> pageResult = cronJobRepository.pageQuery(pageNum, pageSize, query.getName(), query.getEnabled());
        List<CronJobResponse> records = pageResult.getRecords().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return PageResponse.of(records, pageResult.getTotal(), pageResult.getCurrent(),
                pageResult.getSize(), pageResult.getPages());
    }

    @Override
    public void enable(Long id) {
        CronJob job = cronJobRepository.selectById(id);
        if (job == null) {
            throw new BusinessException(CronJobConstants.MSG_JOB_NOT_FOUND);
        }
        Date nextFireTime = CronUtils.getNextExecution(job.getCronExpr());
        cronJobRepository.updateEnabled(id, 1, nextFireTime);
        log.info("启用定时任务: id={}, name={}", id, job.getName());
    }

    @Override
    public void disable(Long id) {
        CronJob job = cronJobRepository.selectById(id);
        if (job == null) {
            throw new BusinessException(CronJobConstants.MSG_JOB_NOT_FOUND);
        }
        cronJobRepository.updateEnabled(id, 0, null);
        log.info("停用定时任务: id={}, name={}", id, job.getName());
    }

    @Override
    public Long triggerManually(Long id) {
        CronJob job = cronJobRepository.selectById(id);
        if (job == null) {
            throw new BusinessException(CronJobConstants.MSG_JOB_NOT_FOUND);
        }
        Date nextFireTime = CronUtils.getNextExecution(job.getCronExpr());
        if (nextFireTime == null) {
            nextFireTime = new Date(System.currentTimeMillis() + MANUAL_TRIGGER_LOCK_FLOOR_MS);
        }
        Long logId = cronJobScheduler.submitExecution(job, nextFireTime);
        log.info("手动触发定时任务: id={}, name={}, logId={}", id, job.getName(), logId);
        return logId;
    }

    @Override
    public boolean stopLog(Long logId) {
        CronJobLog jobLog = cronJobLogRepository.selectById(logId);
        if (jobLog == null || !CronJobConstants.STATUS_RUNNING.equals(jobLog.getStatus())) {
            return false;
        }

        // 先 CAS 标记终态：无论后续本地/远程停止是否成功，DB 状态都能立即正确反映"已停止"
        long costMs = System.currentTimeMillis() - jobLog.getStartTime().getTime();
        cronJobLogRepository.updateStatusIfRunning(logId, CronJobConstants.STATUS_FAILED, "手动停止", new Date(), costMs);

        String targetIp = jobLog.getInstanceIp();
        String localIp = IpUtils.getLocalIp();
        if (Objects.equals(targetIp, localIp)) {
            cronJobScheduler.stopLocal(logId);
        } else if (StringUtils.isNotBlank(targetIp)) {
            try {
                internalHttpClient.notifyStop(targetIp, logId);
            } catch (Exception e) {
                log.warn("通知远程实例[{}]停止任务[logId={}]失败: {}", targetIp, logId, e.getMessage());
            }
        }
        return true;
    }

    @Override
    public CronJobLogResponse getLogById(Long id) {
        CronJobLog jobLog = cronJobLogRepository.selectById(id);
        if (jobLog == null) {
            throw new BusinessException(CronJobConstants.MSG_LOG_NOT_FOUND);
        }
        return toLogResponse(jobLog);
    }

    @Override
    public PageResponse<CronJobLogResponse> logPage(CronJobLogQueryRequest query) {
        long pageNum = query.getPageNum() != null ? query.getPageNum() : DEFAULT_PAGE_NUM;
        long pageSize = query.getPageSize() != null ? query.getPageSize() : DEFAULT_PAGE_SIZE;
        IPage<CronJobLog> pageResult = cronJobLogRepository.pageQuery(pageNum, pageSize, query.getJobId(), query.getStatus());
        List<CronJobLogResponse> records = pageResult.getRecords().stream()
                .map(this::toLogResponse)
                .collect(Collectors.toList());
        return PageResponse.of(records, pageResult.getTotal(), pageResult.getCurrent(),
                pageResult.getSize(), pageResult.getPages());
    }

    // ============================== 校验辅助方法 ==============================

    private void validateName(String name) {
        if (StringUtils.isBlank(name)) {
            throw new BusinessException(CronJobConstants.MSG_NAME_REQUIRED);
        }
        if (name.length() > CronJobConstants.NAME_MAX_LENGTH) {
            throw new BusinessException(String.format(CronJobConstants.MSG_NAME_TOO_LONG, CronJobConstants.NAME_MAX_LENGTH));
        }
    }

    private void validateBeanName(String beanName) {
        if (StringUtils.isBlank(beanName)) {
            throw new BusinessException(CronJobConstants.MSG_BEAN_NAME_REQUIRED);
        }
        if (beanName.length() > CronJobConstants.BEAN_NAME_MAX_LENGTH) {
            throw new BusinessException(String.format(CronJobConstants.MSG_BEAN_NAME_TOO_LONG, CronJobConstants.BEAN_NAME_MAX_LENGTH));
        }
    }

    private void validateMethodName(String methodName) {
        if (StringUtils.isBlank(methodName)) {
            throw new BusinessException(CronJobConstants.MSG_METHOD_NAME_REQUIRED);
        }
        if (methodName.length() > CronJobConstants.METHOD_NAME_MAX_LENGTH) {
            throw new BusinessException(String.format(CronJobConstants.MSG_METHOD_NAME_TOO_LONG, CronJobConstants.METHOD_NAME_MAX_LENGTH));
        }
    }

    private void validateMethodParamsLength(String methodParams) {
        if (methodParams != null && methodParams.length() > CronJobConstants.METHOD_PARAMS_MAX_LENGTH) {
            throw new BusinessException(String.format(CronJobConstants.MSG_METHOD_PARAMS_TOO_LONG, CronJobConstants.METHOD_PARAMS_MAX_LENGTH));
        }
    }

    private String validateCronExpr(String cronExpr) {
        if (StringUtils.isBlank(cronExpr)) {
            throw new BusinessException(CronJobConstants.MSG_CRON_EXPR_REQUIRED);
        }
        if (cronExpr.length() > CronJobConstants.CRON_EXPR_MAX_LENGTH) {
            throw new BusinessException(String.format(CronJobConstants.MSG_CRON_EXPR_TOO_LONG, CronJobConstants.CRON_EXPR_MAX_LENGTH));
        }
        if (!CronUtils.isValid(cronExpr)) {
            throw new BusinessException(String.format(CronJobConstants.MSG_CRON_EXPR_INVALID, cronExpr));
        }
        return cronExpr;
    }

    private String validateMisfirePolicy(String misfirePolicy) {
        if (StringUtils.isBlank(misfirePolicy)) {
            return MisfirePolicyEnum.FIRE_NOW.getCode();
        }
        if (!MisfirePolicyEnum.isValidCode(misfirePolicy)) {
            throw new BusinessException(String.format(CronJobConstants.MSG_MISFIRE_POLICY_INVALID, misfirePolicy));
        }
        return misfirePolicy;
    }

    private void validateBeanMethod(String beanName, String methodName, String methodParams) {
        try {
            JobInvokeUtil.validateBeanMethod(applicationContext, beanName, methodName, methodParams);
        } catch (IllegalStateException e) {
            throw new BusinessException(e.getMessage());
        }
    }

    // ============================== 转换方法 ==============================

    private CronJobResponse toResponse(CronJob job) {
        CronJobResponse response = new CronJobResponse();
        response.setId(job.getId());
        response.setName(job.getName());
        response.setBeanName(job.getBeanName());
        response.setMethodName(job.getMethodName());
        response.setMethodParams(job.getMethodParams());
        response.setCronExpr(job.getCronExpr());
        response.setEnabled(job.getEnabled());
        response.setMisfirePolicy(job.getMisfirePolicy());
        response.setConcurrent(job.getConcurrent());
        response.setNextFireTime(job.getNextFireTime());
        response.setLastFireTime(job.getLastFireTime());
        response.setCreateTime(job.getCreateTime());
        response.setUpdateTime(job.getUpdateTime());
        return response;
    }

    private CronJobLogResponse toLogResponse(CronJobLog jobLog) {
        CronJobLogResponse response = new CronJobLogResponse();
        response.setId(jobLog.getId());
        response.setJobId(jobLog.getJobId());
        response.setJobName(jobLog.getJobName());
        response.setBeanName(jobLog.getBeanName());
        response.setMethodName(jobLog.getMethodName());
        response.setMethodParams(jobLog.getMethodParams());
        response.setStatus(jobLog.getStatus());
        response.setMessage(jobLog.getMessage());
        response.setInstanceIp(jobLog.getInstanceIp());
        response.setStartTime(jobLog.getStartTime());
        response.setEndTime(jobLog.getEndTime());
        response.setCostMs(jobLog.getCostMs());
        response.setCreateTime(jobLog.getCreateTime());
        return response;
    }
}
