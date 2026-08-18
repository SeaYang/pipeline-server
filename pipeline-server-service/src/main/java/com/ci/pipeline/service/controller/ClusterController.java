package com.ci.pipeline.service.controller;

import com.ci.pipeline.common.enums.ClusterSchedulePolicyEnum;
import com.ci.pipeline.common.result.Result;
import com.ci.pipeline.facade.request.ClusterInfoCreateRequest;
import com.ci.pipeline.facade.request.ClusterInfoQueryRequest;
import com.ci.pipeline.facade.request.ClusterInfoUpdateRequest;
import com.ci.pipeline.facade.request.ClusterTestConnectionRequest;
import com.ci.pipeline.facade.response.ClusterInfoResponse;
import com.ci.pipeline.facade.response.ClusterOptionResponse;
import com.ci.pipeline.facade.response.ClusterSyncReportResponse;
import com.ci.pipeline.facade.response.ClusterTestConnectionResponse;
import com.ci.pipeline.facade.response.PageResponse;
import com.ci.pipeline.service.service.ClusterConfigService;
import com.ci.pipeline.service.service.ClusterInfoService;
import com.ci.pipeline.service.service.ClusterTemplateSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 集群管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/cluster")
public class ClusterController {

    @Autowired
    private ClusterInfoService clusterInfoService;

    @Autowired
    private ClusterConfigService clusterConfigService;

    @Autowired
    private ClusterTemplateSyncService clusterTemplateSyncService;

    /**
     * 分页查询集群列表
     */
    @GetMapping("/page")
    public Result<PageResponse<ClusterInfoResponse>> page(ClusterInfoQueryRequest query) {
        return Result.success(clusterInfoService.page(query));
    }

    /**
     * 新增集群（autoSyncTemplates 默认开启：保存成功后异步全量同步模板）
     */
    @PostMapping
    public Result<ClusterInfoResponse> create(@RequestBody ClusterInfoCreateRequest request) {
        return Result.success(clusterInfoService.create(request));
    }

    /**
     * 编辑集群（clusterName 不可改；token 留空表示不修改）
     */
    @PutMapping
    public Result<ClusterInfoResponse> update(@RequestBody ClusterInfoUpdateRequest request) {
        return Result.success(clusterInfoService.update(request));
    }

    /**
     * 删除集群（被 pipeline_run 引用时拦截）
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        clusterInfoService.delete(id);
        return Result.success(null);
    }

    /**
     * 摘流开关切换（online 0/1，行级更新立即生效）
     */
    @PostMapping("/{clusterName}/online")
    public Result<Void> toggleOnline(@PathVariable("clusterName") String clusterName,
                                     @RequestParam("online") boolean online) {
        clusterInfoService.toggleOnline(clusterName, online);
        return Result.success(null);
    }

    /**
     * 测试连接：用表单参数实时探测 Argo / K8s（不落库）
     */
    @PostMapping("/test-connection")
    public Result<ClusterTestConnectionResponse> testConnection(@RequestBody ClusterTestConnectionRequest request) {
        return Result.success(clusterInfoService.testConnection(request));
    }

    /**
     * 全量同步所有 EFFECTIVE 模板到指定集群（同步执行，返回报告）
     */
    @PostMapping("/{clusterName}/sync-templates")
    public Result<ClusterSyncReportResponse> syncTemplates(@PathVariable("clusterName") String clusterName) {
        return Result.success(clusterTemplateSyncService.syncAllTemplatesToCluster(clusterName));
    }

    /**
     * enabled 集群下拉选项（模板表单"执行集群"多选框数据源）
     */
    @GetMapping("/options")
    public Result<List<ClusterOptionResponse>> options() {
        List<ClusterOptionResponse> options = clusterConfigService.listEnabled().stream()
                .map(c -> {
                    ClusterOptionResponse option = new ClusterOptionResponse();
                    option.setClusterName(c.getClusterName());
                    option.setDescription(c.getDescription());
                    return option;
                })
                .collect(Collectors.toList());
        return Result.success(options);
    }

    /**
     * 调度策略下拉选项
     */
    @GetMapping("/schedule-policies")
    public Result<List<Map<String, String>>> schedulePolicies() {
        List<Map<String, String>> policies = Arrays.stream(ClusterSchedulePolicyEnum.values())
                .map(p -> {
                    Map<String, String> item = new java.util.LinkedHashMap<>();
                    item.put("code", p.getCode());
                    item.put("description", p.getDescription());
                    return item;
                })
                .collect(Collectors.toList());
        return Result.success(policies);
    }
}
