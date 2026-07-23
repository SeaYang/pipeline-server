package com.ci.pipeline.service.strategy.defaultvalue.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ci.pipeline.common.enums.PipelineRunStatusEnum;
import com.ci.pipeline.dao.entity.PipelineRun;
import com.ci.pipeline.dao.repository.PipelineRunRepository;
import com.ci.pipeline.service.strategy.defaultvalue.DefaultValueStrategy;
import com.ci.pipeline.service.strategy.ParamResolveContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * LastSuccessfulRun 策略：从最近一次执行成功的记录读取参数值。
 * <p>查 pipeline_run 表，取该 pipeline 最近一次 Succeeded 记录的 arguments JSON，提取对应参数值。
 */
@Slf4j
@Component
public class LastSuccessfulRunStrategy implements DefaultValueStrategy {

    @Autowired
    private PipelineRunRepository pipelineRunRepository;

    @Override
    public String strategyType() {
        return "LastSuccessfulRun";
    }

    @Override
    public String getValue(String paramName, ParamResolveContext context) {
        if (context == null || context.getPipelineId() == null) {
            return null;
        }
        // 查最近一次成功的执行记录（使用上下文缓存，避免多个参数重复查询）
        PipelineRun latestRun = getLatestSuccessfulRun(context);
        if (latestRun == null || latestRun.getArguments() == null || latestRun.getArguments().isEmpty()) {
            return null;
        }
        try {
            JSONObject arguments = JSON.parseObject(latestRun.getArguments());
            if (arguments != null && arguments.containsKey(paramName)) {
                String value = arguments.getString(paramName);
                return value != null ? value : null;
            }
        } catch (Exception e) {
            log.warn("解析最近成功记录的 arguments 失败, pipelineId={}, arguments={}",
                    context.getPipelineId(), latestRun.getArguments(), e);
        }
        return null;
    }

    /**
     * 获取最近一次成功的执行记录（使用上下文缓存，同一批次只查一次）。
     */
    private PipelineRun getLatestSuccessfulRun(ParamResolveContext context) {
        if (context.isLatestRunLoaded()) {
            return context.getCachedLatestSuccessfulRun();
        }
        PipelineRun run = findLatestSuccessfulRun(context.getPipelineId());
        context.setCachedLatestSuccessfulRun(run);
        context.setLatestRunLoaded(true);
        return run;
    }

    /**
     * 查询指定流水线最近一次成功的执行记录。
     * <p>使用分页查询（pageSize=1, status=Succeeded, 按创建时间倒序）。
     */
    private PipelineRun findLatestSuccessfulRun(Long pipelineId) {
        try {
            com.baomidou.mybatisplus.core.metadata.IPage<PipelineRun> page =
                    pipelineRunRepository.pageQuery(1, 1, pipelineId, null,
                            PipelineRunStatusEnum.SUCCEEDED.getCode(), "create_time", "desc");
            if (page != null && page.getRecords() != null && !page.getRecords().isEmpty()) {
                return page.getRecords().get(0);
            }
        } catch (Exception e) {
            log.warn("查询最近成功执行记录失败, pipelineId={}", pipelineId, e);
        }
        return null;
    }
}
