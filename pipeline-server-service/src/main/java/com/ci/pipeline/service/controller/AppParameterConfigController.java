package com.ci.pipeline.service.controller;

import com.ci.pipeline.common.auth.RequireLogin;
import com.ci.pipeline.common.result.Result;
import com.ci.pipeline.facade.request.AppParameterConfigBatchCreateRequest;
import com.ci.pipeline.facade.request.AppParameterConfigCreateRequest;
import com.ci.pipeline.facade.request.AppParameterConfigQueryRequest;
import com.ci.pipeline.facade.request.AppParameterConfigUpdateRequest;
import com.ci.pipeline.facade.response.AppParameterConfigResponse;
import com.ci.pipeline.service.service.AppParameterConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 应用参数配置控制器
 */
@Slf4j
@RestController
@RequestMapping("/app-parameter-config")
@RequireLogin
public class AppParameterConfigController {

    @Autowired
    private AppParameterConfigService appParameterConfigService;

    /**
     * 新增单条参数配置
     */
    @PostMapping
    public Result<AppParameterConfigResponse> create(@RequestBody AppParameterConfigCreateRequest request) {
        return Result.success(appParameterConfigService.create(request));
    }

    /**
     * 批量新增参数配置
     */
    @PostMapping("/batch")
    public Result<Void> batchCreate(@RequestBody AppParameterConfigBatchCreateRequest request) {
        appParameterConfigService.batchCreate(request);
        return Result.success();
    }

    /**
     * 修改参数配置（仅修改 value）
     */
    @PutMapping
    public Result<AppParameterConfigResponse> update(@RequestBody AppParameterConfigUpdateRequest request) {
        return Result.success(appParameterConfigService.update(request));
    }

    /**
     * 删除参数配置
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        appParameterConfigService.deleteById(id);
        return Result.success();
    }

    /**
     * 列表查询（不分页，按 appName + env 过滤）
     */
    @GetMapping("/list")
    public Result<List<AppParameterConfigResponse>> list(AppParameterConfigQueryRequest query) {
        return Result.success(appParameterConfigService.list(query));
    }

    /**
     * 获取环境列表（default + pipeline_parameter 中 env 参数的 option_config 值）
     */
    @GetMapping("/envs")
    public Result<List<String>> envs() {
        return Result.success(appParameterConfigService.listEnvs());
    }
}
