package com.ci.pipeline.service.controller;

import com.ci.pipeline.common.auth.RequireLogin;
import com.ci.pipeline.common.result.Result;
import com.ci.pipeline.facade.request.PipelineTemplateCreateRequest;
import com.ci.pipeline.facade.request.PipelineTemplateQueryRequest;
import com.ci.pipeline.facade.request.PipelineTemplateUpdateRequest;
import com.ci.pipeline.facade.response.ClusterSyncResultResponse;
import com.ci.pipeline.facade.response.DictDataResponse;
import com.ci.pipeline.facade.response.PipelineTemplateResponse;
import com.ci.pipeline.service.service.ClusterTemplateSyncService;
import com.ci.pipeline.service.service.PipelineTemplateService;
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
 * 流水线模板控制器
 */
@Slf4j
@RestController
@RequestMapping("/pipeline-template")
@RequireLogin
public class PipelineTemplateController {

    @Autowired
    private PipelineTemplateService pipelineTemplateService;

    @Autowired
    private ClusterTemplateSyncService clusterTemplateSyncService;

    /**
     * 新增流水线模板
     */
    @PostMapping
    public Result<PipelineTemplateResponse> create(@RequestBody PipelineTemplateCreateRequest request) {
        return Result.success(pipelineTemplateService.create(request));
    }

    /**
     * 修改流水线模板
     */
    @PutMapping
    public Result<PipelineTemplateResponse> update(@RequestBody PipelineTemplateUpdateRequest request) {
        return Result.success(pipelineTemplateService.update(request));
    }

    /**
     * 删除流水线模板（若存在版本则禁止删除）
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        pipelineTemplateService.deleteById(id);
        return Result.success();
    }

    /**
     * 根据主键查询流水线模板
     */
    @GetMapping("/{id}")
    public Result<PipelineTemplateResponse> get(@PathVariable("id") Long id) {
        return Result.success(pipelineTemplateService.getById(id));
    }

    /**
     * 列表查询流水线模板（支持所属分组精确筛选、字段排序，不分页）
     */
    @GetMapping("/list")
    public Result<List<PipelineTemplateResponse>> list(PipelineTemplateQueryRequest query) {
        return Result.success(pipelineTemplateService.list(query));
    }

    /**
     * 流水线模板所属分组下拉列表（字典 programming-language，按 sort 升序）
     */
    @GetMapping("/groups")
    public Result<List<DictDataResponse>> groups() {
        return Result.success(pipelineTemplateService.listGroups());
    }

    /**
     * 重推流水线模板到集群（clusterName 为空时重推所有 enabled 集群，幂等）
     */
    @PostMapping("/sync-clusters")
    public Result<List<ClusterSyncResultResponse>> syncClusters(
            @RequestParam("pipelineTemplateCode") String pipelineTemplateCode,
            @RequestParam(value = "clusterName", required = false) String clusterName) {
        return Result.success(clusterTemplateSyncService.resyncPipelineTemplate(pipelineTemplateCode, clusterName));
    }
}
