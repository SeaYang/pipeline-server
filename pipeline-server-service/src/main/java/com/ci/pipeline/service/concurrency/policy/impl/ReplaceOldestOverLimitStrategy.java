package com.ci.pipeline.service.concurrency.policy.impl;

import com.ci.pipeline.common.constants.PipelineConcurrencyConstants;
import com.ci.pipeline.common.exception.BusinessException;
import com.ci.pipeline.dao.entity.Pipeline;
import com.ci.pipeline.dao.entity.PipelineRun;
import com.ci.pipeline.dao.entity.PipelineTemplate;
import com.ci.pipeline.service.concurrency.policy.OverLimitPolicyStrategy;
import com.ci.pipeline.service.service.PipelineRunService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ReplaceOldest 策略：超限时终止本流水线最早一条占用额度的执行腾位后放行。
 * <p>替换范围仅限当前流水线自己的执行——不能为了一条流水线的新执行去终止
 * 同应用下其他流水线的执行；本流水线无占用（额度被他人占满）时仍拒绝。
 */
@Slf4j
@Component("ReplaceOldestOverLimit")
public class ReplaceOldestOverLimitStrategy implements OverLimitPolicyStrategy {

    @Autowired
    private PipelineRunService pipelineRunService;

    @Override
    public boolean isBlock(Pipeline pipeline, PipelineTemplate template, int limit,
                           long occupying, long ownOccupying) {
        // 本流水线存在占用执行 → 可腾位放行；额度全被他人占用 → 阻断
        return ownOccupying <= 0;
    }

    @Override
    public boolean beforeExecute(Pipeline pipeline, List<PipelineRun> ownRuns) {
        if (ownRuns == null || ownRuns.isEmpty()) {
            // 理论上不会走到（isBlock 已拦截），防御性兜底
            throw new BusinessException(PipelineConcurrencyConstants.MSG_QUOTA_OCCUPIED_BY_OTHERS);
        }
        // ownRuns 按 id 升序，最早一条即第一条；该记录同时计入 L2 与 L3 额度，终止它即同时满足两层
        PipelineRun oldest = ownRuns.get(0);
        pipelineRunService.stopByConcurrencyReplace(oldest.getId());
        log.info("并发超限替换: pipelineId={}, 终止最早执行 runId={}, 新执行放行",
                pipeline.getId(), oldest.getId());
        return true;
    }
}
