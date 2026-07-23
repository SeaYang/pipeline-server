package com.ci.pipeline.service.controller;

import com.ci.pipeline.common.constants.CommonConstants;
import com.ci.pipeline.common.result.Result;
import com.ci.pipeline.facade.response.AdminResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin 控制器 - 用于健康检查和应用信息查询
 */
@Slf4j
@RestController
@RequestMapping("/admin")
public class AdminController {

    @GetMapping("/health")
    public Result<AdminResponse> health() {
        AdminResponse response = new AdminResponse();
        response.setAppId(CommonConstants.APP_ID);
        response.setStatus("UP");
        response.setVersion("1.0.0");
        response.setTimestamp(System.currentTimeMillis());
        return Result.success(response);
    }

    @GetMapping("/info")
    public Result<AdminResponse> info() {
        AdminResponse response = new AdminResponse();
        response.setAppId(CommonConstants.APP_ID);
        response.setStatus("RUNNING");
        response.setVersion("1.0.0");
        response.setTimestamp(System.currentTimeMillis());
        return Result.success(response);
    }
}
