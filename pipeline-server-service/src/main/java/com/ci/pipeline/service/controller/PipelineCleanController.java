package com.ci.pipeline.service.controller;

import com.ci.pipeline.common.result.Result;
import com.ci.pipeline.facade.request.PipelineCleanCallbackRequest;
import com.ci.pipeline.service.service.PipelineCleanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 流水线终态清理回调接口。
 * <p>供清理 Workflow 的 clean-notify 任务（集群内 Pod）回调，不携带用户会话，
 * 因此不加 @RequireLogin（无登录注解即不经过 LoginAspect 校验），仅限集群内网访问。
 */
@Slf4j
@RestController
@RequestMapping("/pipeline-clean")
public class PipelineCleanController {

    @Autowired
    private PipelineCleanService pipelineCleanService;

    /**
     * 清理完成回调（幂等）：入参 pipelineRunName，将 Running 记录置为 Succeeded。
     */
    @PostMapping("/callback")
    public Result<Void> callback(@RequestBody PipelineCleanCallbackRequest request) {
        pipelineCleanService.handleCallback(request.getPipelineRunName());
        return Result.success(null);
    }
}
