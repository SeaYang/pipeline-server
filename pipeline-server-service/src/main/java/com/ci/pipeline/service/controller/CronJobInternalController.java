package com.ci.pipeline.service.controller;

import com.ci.pipeline.common.auth.RequireLogin;
import com.ci.pipeline.common.result.Result;
import com.ci.pipeline.service.scheduler.CronJobScheduler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 定时任务内部接口：仅供实例间互相调用，不面向前端 / 外部用户。
 * <p><b>暂不鉴权</b>：当前阶段不校验调用方身份，见 docs/techdesign/cron-job-design.md 6.4 节说明——
 * 这是已知取舍（依赖内网隔离），非遗漏。若未来暴露到公网，需要补充调用方白名单或签名校验。
 */
@Slf4j
@RestController
@RequestMapping("/internal/cron-job")
@RequireLogin(false)
public class CronJobInternalController {

    @Autowired
    private CronJobScheduler cronJobScheduler;

    /** 停止本实例正在执行的、指定执行日志对应的任务 */
    @PostMapping("/stop/{logId}")
    public Result<Boolean> stop(@PathVariable("logId") Long logId) {
        return Result.success(cronJobScheduler.stopLocal(logId));
    }
}
