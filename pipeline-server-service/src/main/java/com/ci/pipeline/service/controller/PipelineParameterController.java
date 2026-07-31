package com.ci.pipeline.service.controller;

import com.ci.pipeline.common.auth.RequireLogin;
import com.ci.pipeline.common.enums.ComponentTypeEnum;
import com.ci.pipeline.common.enums.DefaultValueStrategyTypeEnum;
import com.ci.pipeline.common.enums.ParamTypeEnum;
import com.ci.pipeline.common.result.Result;
import com.ci.pipeline.facade.request.PipelineParameterCreateRequest;
import com.ci.pipeline.facade.request.PipelineParameterQueryRequest;
import com.ci.pipeline.facade.request.PipelineParameterUpdateRequest;
import com.ci.pipeline.facade.response.AppParameterOptionResponse;
import com.ci.pipeline.facade.response.EnumOptionResponse;
import com.ci.pipeline.facade.response.PageResponse;
import com.ci.pipeline.facade.response.PipelineParameterResponse;
import com.ci.pipeline.service.service.PipelineParameterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/pipeline-parameter")
@RequireLogin
public class PipelineParameterController {

    @Autowired
    private PipelineParameterService pipelineParameterService;

    @PostMapping
    public Result<PipelineParameterResponse> create(@RequestBody PipelineParameterCreateRequest request) {
        return Result.success(pipelineParameterService.create(request));
    }

    @PutMapping
    public Result<PipelineParameterResponse> update(@RequestBody PipelineParameterUpdateRequest request) {
        return Result.success(pipelineParameterService.update(request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        pipelineParameterService.deleteById(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<PipelineParameterResponse> get(@PathVariable("id") Long id) {
        return Result.success(pipelineParameterService.getById(id));
    }

    /** 按参数名查询详情 */
    @GetMapping("/name/{name}")
    public Result<PipelineParameterResponse> getByName(@PathVariable("name") String name) {
        return Result.success(pipelineParameterService.getByName(name));
    }

    @GetMapping("/page")
    public Result<PageResponse<PipelineParameterResponse>> page(PipelineParameterQueryRequest query) {
        return Result.success(pipelineParameterService.page(query));
    }

    /** 查询全部参数（仅 name 和 label），用于依赖参数选择 */
    @GetMapping("/list-all")
    public Result<List<PipelineParameterResponse>> listAll() {
        return Result.success(pipelineParameterService.listAllSimple());
    }

    /** 查询可配置的参数列表（INPUT/SELECT/RADIO/GIT_TREE 类型的 user 参数），用于应用参数配置 */
    @GetMapping("/configurable-list")
    public Result<List<AppParameterOptionResponse>> configurableList() {
        return Result.success(pipelineParameterService.listConfigurableParameters());
    }

    /** 参数类型枚举列表 */
    @GetMapping("/enums/param-type")
    public Result<List<EnumOptionResponse>> paramTypeEnums() {
        return Result.success(toOptions(ParamTypeEnum.values()));
    }

    /** 组件类型枚举列表 */
    @GetMapping("/enums/component-type")
    public Result<List<EnumOptionResponse>> componentTypeEnums() {
        return Result.success(toOptions(ComponentTypeEnum.values()));
    }

    /** 默认值策略类型枚举列表 */
    @GetMapping("/enums/strategy-type")
    public Result<List<EnumOptionResponse>> strategyTypeEnums() {
        return Result.success(toOptions(DefaultValueStrategyTypeEnum.values()));
    }

    private List<EnumOptionResponse> toOptions(Enum<?>[] enums) {
        return Arrays.stream(enums)
                .map(e -> {
                    String code = null;
                    String desc = null;
                    try {
                        code = (String) e.getClass().getMethod("getCode").invoke(e);
                        desc = (String) e.getClass().getMethod("getDescription").invoke(e);
                    } catch (Exception ignored) {
                    }
                    return new EnumOptionResponse(code, desc);
                })
                .collect(Collectors.toList());
    }
}
