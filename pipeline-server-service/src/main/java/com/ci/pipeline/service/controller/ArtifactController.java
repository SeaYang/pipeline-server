package com.ci.pipeline.service.controller;

import com.ci.pipeline.common.auth.RequireLogin;
import com.ci.pipeline.common.result.Result;
import com.ci.pipeline.facade.request.ArtifactQueryRequest;
import com.ci.pipeline.facade.request.ArtifactUploadRequest;
import com.ci.pipeline.facade.response.ArtifactResponse;
import com.ci.pipeline.facade.response.PageResponse;
import com.ci.pipeline.service.service.ArtifactService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 制品管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/artifact")
@RequireLogin
public class ArtifactController {

    @Autowired
    private ArtifactService artifactService;

    /**
     * 制品上传（内部接口，Argo pod 回传用，免登录）
     * 方法级 @RequireLogin(false) 覆盖类级注解，放开登录校验
     */
    @PostMapping("/upload")
    @RequireLogin(false)
    public Result<Long> upload(@RequestBody ArtifactUploadRequest request) {
        return Result.success(artifactService.upload(request));
    }

    /**
     * 制品详情
     */
    @GetMapping("/{id}")
    public Result<ArtifactResponse> get(@PathVariable("id") Long id) {
        return Result.success(artifactService.getById(id));
    }

    /**
     * 分页查询制品列表
     */
    @GetMapping("/page")
    public Result<PageResponse<ArtifactResponse>> page(ArtifactQueryRequest query) {
        return Result.success(artifactService.page(query));
    }

    /**
     * 根据流水线运行名称查询制品列表（流水线详情页用）
     */
    @GetMapping("/list-by-run/{pipelineRunName}")
    public Result<List<ArtifactResponse>> listByRun(@PathVariable("pipelineRunName") String pipelineRunName) {
        return Result.success(artifactService.listByPipelineRunName(pipelineRunName));
    }
}
