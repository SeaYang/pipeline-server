package com.ci.pipeline.service.controller;

import com.ci.pipeline.common.auth.RequireLogin;
import com.ci.pipeline.common.result.Result;
import com.ci.pipeline.facade.request.PipelineTriggerHistoryQueryRequest;
import com.ci.pipeline.facade.response.PageResponse;
import com.ci.pipeline.facade.response.PipelineTriggerHistoryResponse;
import com.ci.pipeline.service.service.PipelineTriggerHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/pipeline/trigger-history")
@RequireLogin
public class PipelineTriggerHistoryController {

    @Autowired
    private PipelineTriggerHistoryService pipelineTriggerHistoryService;

    @GetMapping("/page")
    public Result<PageResponse<PipelineTriggerHistoryResponse>> page(
            PipelineTriggerHistoryQueryRequest query) {
        return Result.success(pipelineTriggerHistoryService.page(query));
    }

    @GetMapping("/{id}")
    public Result<PipelineTriggerHistoryResponse> get(@PathVariable("id") Long id) {
        return Result.success(pipelineTriggerHistoryService.getById(id));
    }
}
