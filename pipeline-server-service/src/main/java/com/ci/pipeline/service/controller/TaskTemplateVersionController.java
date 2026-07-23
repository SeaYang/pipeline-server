package com.ci.pipeline.service.controller;

import com.ci.pipeline.common.auth.RequireLogin;
import com.ci.pipeline.common.result.Result;
import com.ci.pipeline.facade.request.TaskTemplateVersionCreateRequest;
import com.ci.pipeline.facade.request.TaskTemplateVersionStatusRequest;
import com.ci.pipeline.facade.request.TaskTemplateVersionUpdateRequest;
import com.ci.pipeline.facade.response.TaskTemplateVersionResponse;
import com.ci.pipeline.service.service.TaskTemplateVersionService;
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
 * 任务模板版本控制器
 */
@Slf4j
@RestController
@RequestMapping("/task-template/version")
@RequireLogin
public class TaskTemplateVersionController {

    @Autowired
    private TaskTemplateVersionService taskTemplateVersionService;

    /**
     * 新增任务模板版本
     */
    @PostMapping
    public Result<TaskTemplateVersionResponse> create(@RequestBody TaskTemplateVersionCreateRequest request) {
        return Result.success(taskTemplateVersionService.create(request));
    }

    /**
     * 修改任务模板版本（仅 templateDetail / changeNote）
     */
    @PutMapping
    public Result<TaskTemplateVersionResponse> update(@RequestBody TaskTemplateVersionUpdateRequest request) {
        return Result.success(taskTemplateVersionService.update(request));
    }

    /**
     * 删除任务模板版本
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        taskTemplateVersionService.deleteById(id);
        return Result.success();
    }

    /**
     * 根据任务模板编码 + 版本号查询版本详情
     */
    @GetMapping("/detail")
    public Result<TaskTemplateVersionResponse> detail(@RequestParam("taskTemplateCode") String taskTemplateCode,
                                                      @RequestParam("version") String version) {
        return Result.success(taskTemplateVersionService.getDetail(taskTemplateCode, version));
    }

    /**
     * 根据任务模板编码查询版本列表（按创建时间倒序）
     */
    @GetMapping("/list")
    public Result<List<TaskTemplateVersionResponse>> list(@RequestParam("taskTemplateCode") String taskTemplateCode) {
        return Result.success(taskTemplateVersionService.listByCode(taskTemplateCode));
    }

    /**
     * 变更版本状态（目标为生效中时，自动把其它生效中版本置为已失效）
     */
    @PutMapping("/status")
    public Result<TaskTemplateVersionResponse> changeStatus(@RequestBody TaskTemplateVersionStatusRequest request) {
        return Result.success(taskTemplateVersionService.changeStatus(request));
    }
}
