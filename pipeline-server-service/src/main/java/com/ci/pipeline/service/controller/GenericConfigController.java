package com.ci.pipeline.service.controller;

import com.ci.pipeline.common.auth.RequireLogin;
import com.ci.pipeline.common.result.Result;
import com.ci.pipeline.facade.request.GenericConfigCreateRequest;
import com.ci.pipeline.facade.request.GenericConfigHistoryQueryRequest;
import com.ci.pipeline.facade.request.GenericConfigUpdateRequest;
import com.ci.pipeline.facade.response.GenericConfigHistoryResponse;
import com.ci.pipeline.facade.response.GenericConfigResponse;
import com.ci.pipeline.facade.response.PageResponse;
import com.ci.pipeline.service.service.GenericConfigService;
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

import java.util.List;

/**
 * 通用配置管理 Controller。
 */
@Slf4j
@RestController
@RequestMapping("/generic-config")
@RequireLogin
public class GenericConfigController {

    @Autowired
    private GenericConfigService genericConfigService;

    @GetMapping("/list")
    public Result<List<GenericConfigResponse>> list(@RequestParam(required = false) String configKey) {
        return Result.success(genericConfigService.list(configKey));
    }

    @GetMapping("/{id}")
    public Result<GenericConfigResponse> get(@PathVariable Long id) {
        return Result.success(genericConfigService.getById(id));
    }

    @GetMapping("/value")
    public Result<Object> getValue(@RequestParam String configKey) {
        return Result.success(genericConfigService.getValueByKey(configKey));
    }

    @PostMapping
    public Result<GenericConfigResponse> create(@RequestBody GenericConfigCreateRequest request) {
        return Result.success(genericConfigService.create(request));
    }

    @PutMapping
    public Result<GenericConfigResponse> update(@RequestBody GenericConfigUpdateRequest request) {
        return Result.success(genericConfigService.update(request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        genericConfigService.delete(id);
        return Result.success();
    }

    @GetMapping("/{id}/history")
    public Result<List<GenericConfigHistoryResponse>> history(@PathVariable Long id) {
        return Result.success(genericConfigService.historyByConfigId(id));
    }

    @GetMapping("/history/page")
    public Result<PageResponse<GenericConfigHistoryResponse>> historyPage(GenericConfigHistoryQueryRequest query) {
        return Result.success(genericConfigService.historyPage(query));
    }
}
