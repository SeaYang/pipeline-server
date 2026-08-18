package com.ci.pipeline.service.concurrency.policy.impl;

import com.ci.pipeline.dao.entity.Pipeline;
import com.ci.pipeline.dao.entity.PipelineRun;
import com.ci.pipeline.dao.entity.PipelineTemplate;
import com.ci.pipeline.service.concurrency.policy.OverLimitPolicyStrategy;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Reject 策略：超限时直接拒绝新执行（默认策略）。
 */
@Component("RejectOverLimit")
public class RejectOverLimitStrategy implements OverLimitPolicyStrategy {

    @Override
    public boolean isBlock(Pipeline pipeline, PipelineTemplate template, int limit,
                           long occupying, long ownOccupying) {
        // 超限必阻断
        return occupying >= limit;
    }

    @Override
    public boolean beforeExecute(Pipeline pipeline, List<PipelineRun> ownRuns) {
        // Reject 无腾位动作
        return false;
    }
}
