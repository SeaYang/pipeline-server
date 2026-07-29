package com.ci.pipeline.service.controller;

import com.ci.pipeline.common.auth.RequireLogin;
import com.ci.pipeline.common.result.Result;
import com.ci.pipeline.facade.request.PipelineCreateRequest;
import com.ci.pipeline.facade.request.PipelineExecuteRequest;
import com.ci.pipeline.facade.request.PipelineParametersRefreshRequest;
import com.ci.pipeline.facade.request.PipelineParametersRequest;
import com.ci.pipeline.facade.request.PipelineQueryRequest;
import com.ci.pipeline.facade.request.PipelineUpdateRequest;
import com.ci.pipeline.facade.response.PageResponse;
import com.ci.pipeline.facade.response.PipelineExecuteResponse;
import com.ci.pipeline.facade.response.PipelineResponse;
import com.ci.pipeline.facade.response.PipelineTemplateOptionResponse;
import com.ci.pipeline.facade.response.PipelineRunParameterResponse;
import com.ci.pipeline.service.service.PipelineParameterService;
import com.ci.pipeline.service.service.PipelineService;
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
 * 流水线实例控制器
 */
@Slf4j
@RestController
@RequestMapping("/pipeline")
@RequireLogin
public class PipelineController {

    @Autowired
    private PipelineService pipelineService;

    @Autowired
    private PipelineParameterService pipelineParameterService;

    /**
     * 新增流水线
     */
    @PostMapping
    public Result<PipelineResponse> create(@RequestBody PipelineCreateRequest request) {
        return Result.success(pipelineService.create(request));
    }

    /**
     * 修改流水线（目前仅允许修改 name）
     */
    @PutMapping
    public Result<PipelineResponse> update(@RequestBody PipelineUpdateRequest request) {
        return Result.success(pipelineService.update(request));
    }

    /**
     * 删除流水线
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        pipelineService.deleteById(id);
        return Result.success();
    }

    /**
     * 根据主键查询流水线
     */
    @GetMapping("/{id}")
    public Result<PipelineResponse> get(@PathVariable("id") Long id) {
        return Result.success(pipelineService.getById(id));
    }

    /**
     * 分页查询流水线（appName 精确过滤，默认按创建时间倒序）
     */
    @GetMapping("/page")
    public Result<PageResponse<PipelineResponse>> page(PipelineQueryRequest query) {
        return Result.success(pipelineService.page(query));
    }

    /**
     * 新建流水线时的流水线模板下拉列表（按 app 所属编程语言过滤、仅含生效中版本的模板）
     */
    @GetMapping("/templates")
    public Result<List<PipelineTemplateOptionResponse>> templates(@RequestParam("appName") String appName) {
        return Result.success(pipelineService.listTemplates(appName));
    }

    /**
     * 流水线执行参数列表（从参数定义表解析，系统参数自动填充，只返回 user 参数）
     */
    @PostMapping("/parameters")
    public Result<List<PipelineRunParameterResponse>> parameters(@RequestBody PipelineParametersRequest request) {
        return Result.success(pipelineParameterService.listRunParameters(request));
    }

    /**
     * 刷新流水线执行参数（参数联动刷新，返回受影响的参数列表）
     */
    @PostMapping("/parameters/refresh")
    public Result<List<PipelineRunParameterResponse>> refreshParameters(
            @RequestBody PipelineParametersRefreshRequest request) {
        return Result.success(pipelineParameterService.refreshParameters(request));
    }

    /**
     * 执行流水线（按模板名拉起 Argo Workflow）
     */
    @PostMapping("/execute")
    public Result<PipelineExecuteResponse> execute(@RequestBody PipelineExecuteRequest request) {
        return Result.success(pipelineService.executeWithHistory(request));
    }
}
