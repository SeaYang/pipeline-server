package com.ci.pipeline.service.controller;

import com.ci.pipeline.common.auth.RequireLogin;
import com.ci.pipeline.common.result.Result;
import com.ci.pipeline.facade.request.PipelineTemplateEventBindCreateRequest;
import com.ci.pipeline.facade.request.PipelineTemplateEventBindQueryRequest;
import com.ci.pipeline.facade.request.PipelineTemplateEventBindUpdateRequest;
import com.ci.pipeline.facade.response.PageResponse;
import com.ci.pipeline.facade.response.PipelineTemplateEventBindResponse;
import com.ci.pipeline.service.service.PipelineTemplateEventBindService;
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

/**
 * 事件-模板绑定管理控制器（后台配置，需认证）。
 */
@Slf4j
@RestController
@RequestMapping("/pipeline-template-event-bind")
@RequireLogin
public class PipelineTemplateEventBindController {

    @Autowired
    private PipelineTemplateEventBindService pipelineTemplateEventBindService;

    /**
     * 新增绑定
     */
    @PostMapping
    public Result<PipelineTemplateEventBindResponse> create(
            @RequestBody PipelineTemplateEventBindCreateRequest request) {
        return Result.success(pipelineTemplateEventBindService.create(request));
    }

    /**
     * 修改绑定
     */
    @PutMapping
    public Result<PipelineTemplateEventBindResponse> update(
            @RequestBody PipelineTemplateEventBindUpdateRequest request) {
        return Result.success(pipelineTemplateEventBindService.update(request));
    }

    /**
     * 根据主键删除绑定
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        pipelineTemplateEventBindService.deleteById(id);
        return Result.success();
    }

    /**
     * 根据主键查询绑定
     */
    @GetMapping("/{id}")
    public Result<PipelineTemplateEventBindResponse> get(@PathVariable("id") Long id) {
        return Result.success(pipelineTemplateEventBindService.getById(id));
    }

    /**
     * 分页查询绑定列表
     */
    @GetMapping("/page")
    public Result<PageResponse<PipelineTemplateEventBindResponse>> page(
            PipelineTemplateEventBindQueryRequest query) {
        return Result.success(pipelineTemplateEventBindService.page(query));
    }
}
