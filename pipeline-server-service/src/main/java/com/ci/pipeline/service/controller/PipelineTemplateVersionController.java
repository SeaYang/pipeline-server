package com.ci.pipeline.service.controller;

import com.ci.pipeline.common.auth.RequireLogin;
import com.ci.pipeline.common.result.Result;
import com.ci.pipeline.facade.request.PipelineTemplateVersionCreateRequest;
import com.ci.pipeline.facade.request.PipelineTemplateVersionStatusRequest;
import com.ci.pipeline.facade.request.PipelineTemplateVersionUpdateRequest;
import com.ci.pipeline.facade.response.PipelineTemplateVersionResponse;
import com.ci.pipeline.facade.response.PipelineTemplateVersionSaveResponse;
import com.ci.pipeline.service.service.PipelineTemplateVersionService;
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
 * 流水线模板版本控制器
 */
@Slf4j
@RestController
@RequestMapping("/pipeline-template/version")
@RequireLogin
public class PipelineTemplateVersionController {

    @Autowired
    private PipelineTemplateVersionService pipelineTemplateVersionService;

    /**
     * 新增流水线模板版本
     */
    @PostMapping
    public Result<PipelineTemplateVersionSaveResponse> create(@RequestBody PipelineTemplateVersionCreateRequest request) {
        return Result.success(pipelineTemplateVersionService.create(request));
    }

    /**
     * 修改流水线模板版本（仅 templateDetail / changeNote）
     */
    @PutMapping
    public Result<PipelineTemplateVersionSaveResponse> update(@RequestBody PipelineTemplateVersionUpdateRequest request) {
        return Result.success(pipelineTemplateVersionService.update(request));
    }

    /**
     * 删除流水线模板版本
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        pipelineTemplateVersionService.deleteById(id);
        return Result.success();
    }

    /**
     * 根据流水线模板编码 + 版本号查询版本详情
     */
    @GetMapping("/detail")
    public Result<PipelineTemplateVersionResponse> detail(@RequestParam("pipelineTemplateCode") String pipelineTemplateCode,
                                                          @RequestParam("version") String version) {
        return Result.success(pipelineTemplateVersionService.getDetail(pipelineTemplateCode, version));
    }

    /**
     * 根据流水线模板编码查询版本列表（按创建时间倒序）
     */
    @GetMapping("/list")
    public Result<List<PipelineTemplateVersionResponse>> list(@RequestParam("pipelineTemplateCode") String pipelineTemplateCode) {
        return Result.success(pipelineTemplateVersionService.listByCode(pipelineTemplateCode));
    }

    /**
     * 变更版本状态（目标为生效中时，自动把其它生效中/草稿版本置为已失效）
     */
    @PutMapping("/status")
    public Result<PipelineTemplateVersionResponse> changeStatus(@RequestBody PipelineTemplateVersionStatusRequest request) {
        return Result.success(pipelineTemplateVersionService.changeStatus(request));
    }
}
