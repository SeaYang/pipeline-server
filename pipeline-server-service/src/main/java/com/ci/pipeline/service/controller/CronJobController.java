package com.ci.pipeline.service.controller;

import com.ci.pipeline.common.auth.RequireLogin;
import com.ci.pipeline.common.result.Result;
import com.ci.pipeline.common.util.CronUtils;
import com.ci.pipeline.facade.request.CronJobCreateRequest;
import com.ci.pipeline.facade.request.CronJobLogQueryRequest;
import com.ci.pipeline.facade.request.CronJobQueryRequest;
import com.ci.pipeline.facade.request.CronJobUpdateRequest;
import com.ci.pipeline.facade.response.CronJobLogResponse;
import com.ci.pipeline.facade.response.CronJobResponse;
import com.ci.pipeline.facade.response.PageResponse;
import com.ci.pipeline.service.service.CronJobService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@Slf4j
@RestController
@RequestMapping("/cron-job")
@RequireLogin
public class CronJobController {

    @Autowired
    private CronJobService cronJobService;

    @PostMapping
    public Result<CronJobResponse> create(@RequestBody CronJobCreateRequest request) {
        return Result.success(cronJobService.create(request));
    }

    @PutMapping
    public Result<CronJobResponse> update(@RequestBody CronJobUpdateRequest request) {
        return Result.success(cronJobService.update(request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        cronJobService.delete(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<CronJobResponse> get(@PathVariable("id") Long id) {
        return Result.success(cronJobService.getById(id));
    }

    @GetMapping("/page")
    public Result<PageResponse<CronJobResponse>> page(CronJobQueryRequest query) {
        return Result.success(cronJobService.page(query));
    }

    @PutMapping("/{id}/enable")
    public Result<Void> enable(@PathVariable("id") Long id) {
        cronJobService.enable(id);
        return Result.success();
    }

    @PutMapping("/{id}/disable")
    public Result<Void> disable(@PathVariable("id") Long id) {
        cronJobService.disable(id);
        return Result.success();
    }

    /** 手动触发一次任务立即执行，返回本次执行的日志ID */
    @PostMapping("/{id}/trigger")
    public Result<Long> trigger(@PathVariable("id") Long id) {
        return Result.success(cronJobService.triggerManually(id));
    }

    /** 预览 CRON 表达式的下一次触发时间，供新增/编辑页面实时校验展示 */
    @GetMapping("/next-fire-time")
    public Result<Date> nextFireTime(@RequestParam("cronExpr") String cronExpr) {
        return Result.success(CronUtils.getNextExecution(cronExpr));
    }

    @GetMapping("/log/{id}")
    public Result<CronJobLogResponse> getLog(@PathVariable("id") Long id) {
        return Result.success(cronJobService.getLogById(id));
    }

    @GetMapping("/log/page")
    public Result<PageResponse<CronJobLogResponse>> logPage(CronJobLogQueryRequest query) {
        return Result.success(cronJobService.logPage(query));
    }

    /** 停止指定执行日志对应的任务 */
    @PostMapping("/log/{id}/stop")
    public Result<Boolean> stopLog(@PathVariable("id") Long id) {
        return Result.success(cronJobService.stopLog(id));
    }
}
