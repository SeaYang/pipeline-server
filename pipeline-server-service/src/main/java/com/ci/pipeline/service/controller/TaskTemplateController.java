package com.ci.pipeline.service.controller;

import com.ci.pipeline.common.auth.RequireLogin;
import com.ci.pipeline.common.result.Result;
import com.ci.pipeline.facade.request.TaskTemplateCreateRequest;
import com.ci.pipeline.facade.request.TaskTemplateQueryRequest;
import com.ci.pipeline.facade.request.TaskTemplateUpdateRequest;
import com.ci.pipeline.facade.response.DictDataResponse;
import com.ci.pipeline.facade.response.PageResponse;
import com.ci.pipeline.facade.response.TaskTemplateResponse;
import com.ci.pipeline.service.service.TaskTemplateService;
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
 * 任务模板控制器
 */
@Slf4j
@RestController
@RequestMapping("/task-template")
@RequireLogin
public class TaskTemplateController {

    @Autowired
    private TaskTemplateService taskTemplateService;

    /**
     * 新增任务模板
     */
    @PostMapping
    public Result<TaskTemplateResponse> create(@RequestBody TaskTemplateCreateRequest request) {
        return Result.success(taskTemplateService.create(request));
    }

    /**
     * 修改任务模板
     */
    @PutMapping
    public Result<TaskTemplateResponse> update(@RequestBody TaskTemplateUpdateRequest request) {
        return Result.success(taskTemplateService.update(request));
    }

    /**
     * 删除任务模板（若存在版本则禁止删除）
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        taskTemplateService.deleteById(id);
        return Result.success();
    }

    /**
     * 根据主键查询任务模板
     */
    @GetMapping("/{id}")
    public Result<TaskTemplateResponse> get(@PathVariable("id") Long id) {
        return Result.success(taskTemplateService.getById(id));
    }

    /**
     * 分页查询任务模板（支持编码/名称模糊、所属分组精确、字段排序）
     */
    @GetMapping("/page")
    public Result<PageResponse<TaskTemplateResponse>> page(TaskTemplateQueryRequest query) {
        return Result.success(taskTemplateService.page(query));
    }

    /**
     * 任务模板所属分组下拉列表（字典 task-template-group，按 sort 升序）
     */
    @GetMapping("/groups")
    public Result<List<DictDataResponse>> groups() {
        return Result.success(taskTemplateService.listGroups());
    }
}
