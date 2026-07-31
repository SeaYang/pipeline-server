package com.ci.pipeline.service.controller;

import com.ci.pipeline.common.auth.RequireLogin;
import com.ci.pipeline.common.result.Result;
import com.ci.pipeline.facade.request.AppInfoCreateRequest;
import com.ci.pipeline.facade.request.AppInfoQueryRequest;
import com.ci.pipeline.facade.request.AppInfoUpdateRequest;
import com.ci.pipeline.facade.response.AppInfoResponse;
import com.ci.pipeline.facade.response.PageResponse;
import com.ci.pipeline.service.service.AppInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 应用基础信息控制器
 */
@Slf4j
@RestController
@RequestMapping("/app-info")
@RequireLogin
public class AppInfoController {

    @Autowired
    private AppInfoService appInfoService;

    /**
     * 新增应用
     */
    @PostMapping
    public Result<AppInfoResponse> create(@RequestBody AppInfoCreateRequest request) {
        return Result.success(appInfoService.create(request));
    }

    /**
     * 修改应用
     */
    @PutMapping
    public Result<AppInfoResponse> update(@RequestBody AppInfoUpdateRequest request) {
        return Result.success(appInfoService.update(request));
    }

    /**
     * 删除应用
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        appInfoService.deleteById(id);
        return Result.success();
    }

    /**
     * 根据主键查询应用
     */
    @GetMapping("/{id}")
    public Result<AppInfoResponse> get(@PathVariable("id") Long id) {
        return Result.success(appInfoService.getById(id));
    }

    /**
     * 分页查询应用（支持 appName 模糊、字段排序）
     */
    @GetMapping("/page")
    public Result<PageResponse<AppInfoResponse>> page(AppInfoQueryRequest query) {
        return Result.success(appInfoService.page(query));
    }

    /**
     * 根据应用名称查询应用详情
     */
    @GetMapping("/detail")
    public Result<AppInfoResponse> getByAppName(@RequestParam("appName") String appName) {
        return Result.success(appInfoService.getByAppName(appName));
    }
}
