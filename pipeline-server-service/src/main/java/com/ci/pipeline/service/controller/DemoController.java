package com.ci.pipeline.service.controller;

import com.ci.pipeline.common.constants.KubernetesConstants;
import com.ci.pipeline.common.result.Result;
import com.ci.pipeline.service.remote.ArgoWorkflowAgent;
import com.ci.pipeline.service.remote.KubernetesAgent;
import com.ci.pipeline.service.remote.PodLogQuery;
import com.ci.pipeline.service.service.ClusterConfigService;
import io.argoproj.workflow.models.IoArgoprojWorkflowV1alpha1Workflow;
import io.argoproj.workflow.models.IoArgoprojWorkflowV1alpha1WorkflowList;
import io.argoproj.workflow.models.IoArgoprojWorkflowV1alpha1WorkflowTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Demo 控制器 - 用于测试 Argo Workflow 操作（多集群版：clusterName 可选参数，缺省默认集群）
 */
@Slf4j
@RestController
@RequestMapping("/demo")
public class DemoController {

    @Autowired
    private ArgoWorkflowAgent argoWorkflowAgent;

    @Autowired
    private KubernetesAgent kubernetesAgent;

    @Autowired
    private ClusterConfigService clusterConfigService;

    /**
     * 验证 WorkflowTemplate 的正确性
     */
    @PostMapping("/workflow-template/lint")
    public Result<IoArgoprojWorkflowV1alpha1WorkflowTemplate> lintWorkflowTemplate(
            @RequestBody IoArgoprojWorkflowV1alpha1WorkflowTemplate workflowTemplate,
            @RequestParam(value = "clusterName", required = false) String clusterName) {
        log.info("收到 lint WorkflowTemplate 请求, clusterName={}", clusterName);
        String resolved = resolveCluster(clusterName);
        IoArgoprojWorkflowV1alpha1WorkflowTemplate result =
                argoWorkflowAgent.lintWorkflowTemplate(resolved, clusterConfigService.getNamespace(resolved), workflowTemplate);
        return Result.success(result);
    }

    /**
     * 创建 WorkflowTemplate（会先 lint 验证再创建）
     */
    @PostMapping("/workflow-template/create")
    public Result<IoArgoprojWorkflowV1alpha1WorkflowTemplate> createWorkflowTemplate(
            @RequestBody IoArgoprojWorkflowV1alpha1WorkflowTemplate workflowTemplate,
            @RequestParam(value = "clusterName", required = false) String clusterName) {
        log.info("收到创建 WorkflowTemplate 请求, clusterName={}", clusterName);
        String resolved = resolveCluster(clusterName);
        IoArgoprojWorkflowV1alpha1WorkflowTemplate result =
                argoWorkflowAgent.createWorkflowTemplate(resolved, clusterConfigService.getNamespace(resolved), workflowTemplate);
        return Result.success(result);
    }

    /**
     * 根据名称获取 WorkflowTemplate
     */
    @GetMapping("/workflow-template/get")
    public Result<IoArgoprojWorkflowV1alpha1WorkflowTemplate> getWorkflowTemplate(
            @RequestParam("name") String name,
            @RequestParam(value = "clusterName", required = false) String clusterName) {
        log.info("收到获取 WorkflowTemplate 请求, name={}, clusterName={}", name, clusterName);
        String resolved = resolveCluster(clusterName);
        IoArgoprojWorkflowV1alpha1WorkflowTemplate result =
                argoWorkflowAgent.getWorkflowTemplate(resolved, clusterConfigService.getNamespace(resolved), name);
        return Result.success(result);
    }

    /**
     * 提交工作流
     *
     * @param workflow   工作流对象（包含 metadata.name 用于关联 WorkflowTemplate）
     * @param parameters 工作流参数列表，可选
     */
    @PostMapping("/workflow/submit")
    public Result<IoArgoprojWorkflowV1alpha1Workflow> submitWorkflow(
            @RequestBody IoArgoprojWorkflowV1alpha1Workflow workflow,
            @RequestParam(value = "parameters", required = false) List<String> parameters,
            @RequestParam(value = "clusterName", required = false) String clusterName) {
        log.info("收到提交工作流请求, clusterName={}", clusterName);
        String resolved = resolveCluster(clusterName);
        IoArgoprojWorkflowV1alpha1Workflow result =
                argoWorkflowAgent.submitWorkflow(resolved, clusterConfigService.getNamespace(resolved), workflow, parameters);
        return Result.success(result);
    }

    /**
     * 查询 Workflow 实例列表
     */
    @GetMapping("/workflow/list")
    public Result<IoArgoprojWorkflowV1alpha1WorkflowList> getWorkflowList(
            @RequestParam(value = "clusterName", required = false) String clusterName) {
        log.info("收到获取 WorkflowList 请求, clusterName={}", clusterName);
        String resolved = resolveCluster(clusterName);
        IoArgoprojWorkflowV1alpha1WorkflowList result =
                argoWorkflowAgent.listWorkflows(resolved, clusterConfigService.getNamespace(resolved), null);
        return Result.success(result);
    }

    /**
     * 根据名称获取 Workflow
     */
    @GetMapping("/workflow/get")
    public Result<IoArgoprojWorkflowV1alpha1Workflow> getWorkflow(
            @RequestParam("name") String name,
            @RequestParam(value = "clusterName", required = false) String clusterName) {
        log.info("收到获取 Workflow 请求, name={}, clusterName={}", name, clusterName);
        String resolved = resolveCluster(clusterName);
        IoArgoprojWorkflowV1alpha1Workflow result =
                argoWorkflowAgent.getWorkflow(resolved, clusterConfigService.getNamespace(resolved), name);
        return Result.success(result);
    }

    /**
     * 获取 Pod 日志（用于测试 Kubernetes 日志查询）
     * namespace 固定为 argo，container 固定为 main。
     *
     * @param podName   Pod 名称
     * @param tailLines 仅取最后 N 行，可选
     */
    @GetMapping("/pod/log")
    public Result<String> getPodLog(
            @RequestParam("podName") String podName,
            @RequestParam(value = "tailLines", required = false) Integer tailLines,
            @RequestParam(value = "clusterName", required = false) String clusterName) {
        log.info("收到获取 Pod 日志请求, namespace={}, podName={}, container={}, tailLines={}, clusterName={}",
                KubernetesConstants.ARGO_NAMESPACE, podName, KubernetesConstants.DEFAULT_LOG_CONTAINER, tailLines, clusterName);
        String resolved = resolveCluster(clusterName);
        PodLogQuery query = PodLogQuery.builder()
                .container(KubernetesConstants.DEFAULT_LOG_CONTAINER)
                .tailLines(tailLines)
                .build();
        String logText = kubernetesAgent.getPodLog(resolved, KubernetesConstants.ARGO_NAMESPACE, podName, query);
        return Result.success(logText);
    }

    /**
     * 集群解析：未指定时用默认集群
     */
    private String resolveCluster(String clusterName) {
        return clusterName != null && !clusterName.isEmpty()
                ? clusterName
                : clusterConfigService.getDefaultClusterName();
    }
}
