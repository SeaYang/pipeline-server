package com.ci.pipeline.service.job;

import com.ci.pipeline.common.enums.PipelineRunStatusEnum;
import com.ci.pipeline.dao.entity.PipelineRun;
import com.ci.pipeline.dao.repository.PipelineRunRepository;
import com.ci.pipeline.service.config.PipelineRunSyncProperties;
import com.ci.pipeline.service.service.PipelineRunService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 流水线执行状态兜底同步任务。
 * <p>正常情况下，流水线执行状态由
 * {@link com.ci.pipeline.service.service.PipelineRunSyncService#syncUntilTerminal} 异步轮询直到终态回写；
 * 但服务发布、宕机等场景下该轮询可能中断，导致 pipeline_run 长时间停留在运行中状态不再更新。
 * <p>本任务定期扫描"状态为运行中、且更新时间早于陈旧阈值"的执行记录，逐条调用
 * {@link PipelineRunService#syncRun(Long)} 重新拉起兜底同步；syncRun 内部已自带状态判断、
 * 陈旧阈值判断与分布式锁防重复触发，本任务只负责找出候选记录并调用，不重复实现这些判断。
 * <p>本类作为 Spring Bean 被 cron_job 定时任务反射调用，需在 cron_job 表配置一条记录，
 * bean_name = {@code pipelineRunSyncGuardJob}，method_name = {@code execute}，无参数。
 */
@Slf4j
@Component("pipelineRunSyncGuardJob")
public class PipelineRunSyncGuardJob {

    @Autowired
    private PipelineRunRepository pipelineRunRepository;

    @Autowired
    private PipelineRunService pipelineRunService;

    @Autowired
    private PipelineRunSyncProperties pipelineRunSyncProperties;

    /**
     * 扫描疑似中断的流水线执行记录并逐条触发兜底同步。
     * <p>供 cron_job 反射调用，无参数；单条记录处理失败仅记录日志，不影响其余记录。
     */
    public void execute() {
        Date threshold = new Date(System.currentTimeMillis()
                - pipelineRunSyncProperties.getStalenessThresholdSeconds() * 1000L);
        List<PipelineRun> staleRuns = pipelineRunRepository.selectStaleRunning(
                PipelineRunStatusEnum.RUNNING.getCode(), threshold);
        if (staleRuns.isEmpty()) {
            log.info("[流水线状态兜底同步] 未发现中断的执行记录");
            return;
        }
        log.info("[流水线状态兜底同步] 发现{}条疑似中断的执行记录, ids={}", staleRuns.size(),
                staleRuns.stream().map(PipelineRun::getId).collect(Collectors.toList()));
        for (PipelineRun run : staleRuns) {
            try {
                pipelineRunService.syncRun(run.getId());
            } catch (Exception e) {
                log.error("[流水线状态兜底同步] 触发失败, pipelineRunId={}", run.getId(), e);
            }
        }
    }
}
