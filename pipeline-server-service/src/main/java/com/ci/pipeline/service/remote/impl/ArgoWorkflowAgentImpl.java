package com.ci.pipeline.service.remote.impl;

import com.ci.pipeline.service.remote.ArgoWorkflowAgent;
import com.ci.pipeline.service.remote.ClusterClientRegistry;
import io.argoproj.workflow.ApiClient;
import io.argoproj.workflow.ApiException;
import io.argoproj.workflow.apis.WorkflowServiceApi;
import io.argoproj.workflow.apis.WorkflowTemplateServiceApi;
import io.argoproj.workflow.models.IoArgoprojWorkflowV1alpha1Workflow;
import io.argoproj.workflow.models.IoArgoprojWorkflowV1alpha1WorkflowList;
import io.argoproj.workflow.models.IoArgoprojWorkflowV1alpha1WorkflowResumeRequest;
import io.argoproj.workflow.models.IoArgoprojWorkflowV1alpha1WorkflowRetryRequest;
import io.argoproj.workflow.models.IoArgoprojWorkflowV1alpha1WorkflowStopRequest;
import io.argoproj.workflow.models.IoArgoprojWorkflowV1alpha1WorkflowSubmitRequest;
import io.argoproj.workflow.models.IoArgoprojWorkflowV1alpha1WorkflowSuspendRequest;
import io.argoproj.workflow.models.IoArgoprojWorkflowV1alpha1WorkflowTerminateRequest;
import io.argoproj.workflow.models.IoArgoprojWorkflowV1alpha1WorkflowTemplate;
import io.argoproj.workflow.models.IoArgoprojWorkflowV1alpha1WorkflowTemplateCreateRequest;
import io.argoproj.workflow.models.IoArgoprojWorkflowV1alpha1WorkflowTemplateLintRequest;
import io.argoproj.workflow.models.IoArgoprojWorkflowV1alpha1WorkflowTemplateUpdateRequest;
import io.argoproj.workflow.models.IoArgoprojWorkflowV1alpha1SubmitOpts;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Argo Workflow 操作实现类（多集群版：按 clusterName 从注册表获取对应集群的 ApiClient，
 * API 对象随用随建，极轻量无性能问题）
 */
@Slf4j
@Component
public class ArgoWorkflowAgentImpl implements ArgoWorkflowAgent {

    private static final String DEFAULT_API_VERSION = "argoproj.io/v1alpha1";
    private static final String DEFAULT_KIND_WORKFLOW_TEMPLATE = "WorkflowTemplate";
    private static final String RESOURCE_KIND_WORKFLOW_TEMPLATE = "WorkflowTemplate";

    /** 列表固定只查询由 go-cicd-pipeline 模板创建的实例 */
    private static final String LABEL_TEMPLATE =
            "";
    /** 列表分页上限（对应 listOptions.limit，该 SDK 版本中类型为 String） */
    private static final String LIST_LIMIT = "50";
    /** 服务端字段裁剪，仅保留列表展示所需字段，显著减小返回体 */
    private static final String LIST_FIELDS = String.join(",",
            "metadata",
            "items.metadata.uid",
            "items.metadata.name",
            "items.metadata.namespace",
            "items.metadata.creationTimestamp",
            "items.metadata.labels",
            "items.metadata.annotations",
            "items.status.phase",
            "items.status.message",
            "items.status.finishedAt",
            "items.status.startedAt",
            "items.status.estimatedDuration",
            "items.status.progress",
            "items.spec.suspend");

    @Autowired
    private ClusterClientRegistry clusterClientRegistry;

    /**
     * 获取指定集群的 WorkflowTemplate API（随用随建）
     */
    private WorkflowTemplateServiceApi templateApi(String clusterName) {
        ApiClient apiClient = clusterClientRegistry.getArgoApiClient(clusterName);
        return new WorkflowTemplateServiceApi(apiClient);
    }

    /**
     * 获取指定集群的 Workflow API（随用随建）
     */
    private WorkflowServiceApi workflowApi(String clusterName) {
        ApiClient apiClient = clusterClientRegistry.getArgoApiClient(clusterName);
        return new WorkflowServiceApi(apiClient);
    }

    @Override
    public IoArgoprojWorkflowV1alpha1WorkflowTemplate lintWorkflowTemplate(String clusterName,
                                                                            String namespace,
                                                                            IoArgoprojWorkflowV1alpha1WorkflowTemplate workflowTemplate) {
        log.info("Lint WorkflowTemplate, clusterName={}, namespace={}", clusterName, namespace);
        buildDefaultForWorkflowTemplate(workflowTemplate);
        try {
            IoArgoprojWorkflowV1alpha1WorkflowTemplateLintRequest lintRequest =
                    new IoArgoprojWorkflowV1alpha1WorkflowTemplateLintRequest();
            lintRequest.setNamespace(namespace);
            lintRequest.setTemplate(workflowTemplate);

            IoArgoprojWorkflowV1alpha1WorkflowTemplate result =
                    templateApi(clusterName).workflowTemplateServiceLintWorkflowTemplate(namespace, lintRequest);
            log.info("WorkflowTemplate lint 验证通过, clusterName={}, namespace={}, name={}",
                    clusterName, namespace, result.getMetadata() != null ? result.getMetadata().getName() : "unknown");
            return result;
        } catch (ApiException e) {
            log.error("Lint WorkflowTemplate 失败, clusterName={}, namespace={}, code={}, body={}",
                    clusterName, namespace, e.getCode(), e.getResponseBody(), e);
            throw new RuntimeException("Lint WorkflowTemplate 失败: " + e.getResponseBody(), e);
        }
    }

    @Override
    public IoArgoprojWorkflowV1alpha1WorkflowTemplate createWorkflowTemplate(String clusterName,
                                                                              String namespace,
                                                                              IoArgoprojWorkflowV1alpha1WorkflowTemplate workflowTemplate) {
        log.info("创建 WorkflowTemplate, clusterName={}, namespace={}", clusterName, namespace);
        buildDefaultForWorkflowTemplate(workflowTemplate);
        // 创建前清除服务端管理的 metadata 字段：templateDetail 可能来自其他集群的导出/回显 JSON，
        // 携带 resourceVersion 会报 "resourceVersion should not be set on objects to be created"
        clearServerManagedMetadata(workflowTemplate);

        // 提交前先进行 lint 验证
        lintWorkflowTemplate(clusterName, namespace, workflowTemplate);

        try {
            IoArgoprojWorkflowV1alpha1WorkflowTemplateCreateRequest createRequest =
                    new IoArgoprojWorkflowV1alpha1WorkflowTemplateCreateRequest();
            createRequest.setNamespace(namespace);
            createRequest.setTemplate(workflowTemplate);

            IoArgoprojWorkflowV1alpha1WorkflowTemplate result =
                    templateApi(clusterName).workflowTemplateServiceCreateWorkflowTemplate(namespace, createRequest);
            log.info("WorkflowTemplate 创建成功, clusterName={}, namespace={}, name={}",
                    clusterName, namespace, result.getMetadata() != null ? result.getMetadata().getName() : "unknown");
            return result;
        } catch (ApiException e) {
            log.error("创建 WorkflowTemplate 失败, clusterName={}, namespace={}, code={}, body={}",
                    clusterName, namespace, e.getCode(), e.getResponseBody(), e);
            throw new RuntimeException("创建 WorkflowTemplate 失败: " + e.getResponseBody(), e);
        }
    }

    @Override
    public IoArgoprojWorkflowV1alpha1WorkflowTemplate updateWorkflowTemplate(String clusterName,
                                                                             String namespace,
                                                                             String name,
                                                                             IoArgoprojWorkflowV1alpha1WorkflowTemplate workflowTemplate) {
        log.info("更新 WorkflowTemplate, clusterName={}, namespace={}, name={}", clusterName, namespace, name);
        buildDefaultForWorkflowTemplate(workflowTemplate);

        // 提交前先进行 lint 验证
        lintWorkflowTemplate(clusterName, namespace, workflowTemplate);

        try {
            // argo/K8s 的 Update（PUT）要求 metadata.resourceVersion（乐观锁），
            // 用户传入的模板 JSON 不含该字段（服务端分配），故先查询已存在模板取其 resourceVersion 回填，
            // 否则会报 "metadata.resourceVersion: must be specified for an update"
            IoArgoprojWorkflowV1alpha1WorkflowTemplate existing = getWorkflowTemplate(clusterName, namespace, name);
            V1ObjectMeta meta = workflowTemplate.getMetadata();
            if (meta == null) {
                meta = new V1ObjectMeta();
                workflowTemplate.setMetadata(meta);
            }
            if (existing.getMetadata() != null) {
                meta.setResourceVersion(existing.getMetadata().getResourceVersion());
            }

            IoArgoprojWorkflowV1alpha1WorkflowTemplateUpdateRequest updateRequest =
                    new IoArgoprojWorkflowV1alpha1WorkflowTemplateUpdateRequest();
            updateRequest.setNamespace(namespace);
            updateRequest.setName(name);
            updateRequest.setTemplate(workflowTemplate);

            IoArgoprojWorkflowV1alpha1WorkflowTemplate result =
                    templateApi(clusterName).workflowTemplateServiceUpdateWorkflowTemplate(namespace, name, updateRequest);
            log.info("WorkflowTemplate 更新成功, clusterName={}, namespace={}, name={}", clusterName, namespace, name);
            return result;
        } catch (ApiException e) {
            log.error("更新 WorkflowTemplate 失败, clusterName={}, namespace={}, name={}, code={}, body={}",
                    clusterName, namespace, name, e.getCode(), e.getResponseBody(), e);
            throw new RuntimeException("更新 WorkflowTemplate 失败: " + e.getResponseBody(), e);
        }
    }

    @Override
    public void deleteWorkflowTemplate(String clusterName, String namespace, String name) {
        log.info("删除 WorkflowTemplate, clusterName={}, namespace={}, name={}", clusterName, namespace, name);
        try {
            templateApi(clusterName).workflowTemplateServiceDeleteWorkflowTemplate(
                    namespace, name, null, null, null, null, null, null);
            log.info("WorkflowTemplate 删除成功, clusterName={}, namespace={}, name={}", clusterName, namespace, name);
        } catch (ApiException e) {
            log.error("删除 WorkflowTemplate 失败, clusterName={}, namespace={}, name={}, code={}, body={}",
                    clusterName, namespace, name, e.getCode(), e.getResponseBody(), e);
            throw new RuntimeException("删除 WorkflowTemplate 失败: " + e.getResponseBody(), e);
        }
    }

    @Override
    public IoArgoprojWorkflowV1alpha1WorkflowTemplate saveWorkflowTemplate(String clusterName,
                                                                            String namespace,
                                                                            IoArgoprojWorkflowV1alpha1WorkflowTemplate workflowTemplate) {
        String name = Optional.ofNullable(workflowTemplate)
                .map(IoArgoprojWorkflowV1alpha1WorkflowTemplate::getMetadata)
                .map(V1ObjectMeta::getName)
                .orElse(null);
        if (name == null || name.isEmpty()) {
            throw new RuntimeException("保存 WorkflowTemplate 失败：metadata.name 不能为空");
        }
        log.info("保存 WorkflowTemplate, clusterName={}, namespace={}, name={}", clusterName, namespace, name);
        // 先查是否已存在：getWorkflowTemplate 在模板不存在时会抛异常，据此判定走更新还是创建
        boolean exists;
        try {
            getWorkflowTemplate(clusterName, namespace, name);
            exists = true;
        } catch (Exception e) {
            log.info("WorkflowTemplate 不存在，将走创建流程, clusterName={}, namespace={}, name={}", clusterName, namespace, name);
            exists = false;
        }
        if (exists) {
            return updateWorkflowTemplate(clusterName, namespace, name, workflowTemplate);
        }
        return createWorkflowTemplate(clusterName, namespace, workflowTemplate);
    }

    @Override
    public IoArgoprojWorkflowV1alpha1WorkflowTemplate getWorkflowTemplate(String clusterName, String namespace, String name) {
        log.info("获取 WorkflowTemplate, clusterName={}, namespace={}, name={}", clusterName, namespace, name);
        try {
            IoArgoprojWorkflowV1alpha1WorkflowTemplate result =
                    templateApi(clusterName).workflowTemplateServiceGetWorkflowTemplate(namespace, name, null);
            log.info("获取 WorkflowTemplate 成功, clusterName={}, namespace={}, name={}", clusterName, namespace, name);
            return result;
        } catch (ApiException e) {
            log.error("获取 WorkflowTemplate 失败, clusterName={}, namespace={}, name={}, code={}, body={}",
                    clusterName, namespace, name, e.getCode(), e.getResponseBody(), e);
            throw new RuntimeException("获取 WorkflowTemplate 失败: " + e.getResponseBody(), e);
        }
    }

    @Override
    public IoArgoprojWorkflowV1alpha1Workflow submitWorkflow(String clusterName,
                                                             String namespace,
                                                             IoArgoprojWorkflowV1alpha1Workflow workflow,
                                                             List<String> parameters) {
        log.info("提交工作流, clusterName={}, namespace={}", clusterName, namespace);
        try {
            IoArgoprojWorkflowV1alpha1WorkflowSubmitRequest submitRequest = buildWorkflowSubmitRequest(namespace, workflow, parameters);

            IoArgoprojWorkflowV1alpha1Workflow result =
                    workflowApi(clusterName).workflowServiceSubmitWorkflow(namespace, submitRequest);
            log.info("工作流提交成功, clusterName={}, namespace={}, workflowName={}",
                    clusterName, namespace, result.getMetadata() != null ? result.getMetadata().getName() : "unknown");
            return result;
        } catch (ApiException e) {
            log.error("提交工作流失败, clusterName={}, namespace={}, code={}, body={}",
                    clusterName, namespace, e.getCode(), e.getResponseBody(), e);
            throw new RuntimeException("提交工作流失败: " + e.getResponseBody(), e);
        }
    }

    @Override
    public IoArgoprojWorkflowV1alpha1Workflow submitWorkflowByTemplate(String clusterName,
                                                                       String namespace,
                                                                       String templateName,
                                                                       List<String> parameters) {
        log.info("按 WorkflowTemplate 名称提交工作流, clusterName={}, namespace={}, templateName={}",
                clusterName, namespace, templateName);
        try {
            IoArgoprojWorkflowV1alpha1WorkflowSubmitRequest submitRequest =
                    buildWorkflowSubmitRequestByTemplate(namespace, templateName, parameters);

            IoArgoprojWorkflowV1alpha1Workflow result =
                    workflowApi(clusterName).workflowServiceSubmitWorkflow(namespace, submitRequest);
            log.info("工作流提交成功, clusterName={}, namespace={}, templateName={}, workflowName={}",
                    clusterName, namespace, templateName,
                    result.getMetadata() != null ? result.getMetadata().getName() : "unknown");
            return result;
        } catch (ApiException e) {
            log.error("按 WorkflowTemplate 名称提交工作流失败, clusterName={}, namespace={}, templateName={}, code={}, body={}",
                    clusterName, namespace, templateName, e.getCode(), e.getResponseBody(), e);
            throw new RuntimeException("提交工作流失败: " + e.getResponseBody(), e);
        }
    }

    @Override
    public IoArgoprojWorkflowV1alpha1Workflow getWorkflow(String clusterName, String namespace, String name) {
        log.info("获取 Workflow 实例, clusterName={}, namespace={}, name={}", clusterName, namespace, name);
        try {
            IoArgoprojWorkflowV1alpha1Workflow result =
                    workflowApi(clusterName).workflowServiceGetWorkflow(namespace, name, null, null);
            log.info("获取 Workflow 实例成功, clusterName={}, namespace={}, name={}", clusterName, namespace, name);
            return result;
        } catch (Exception e) {
            log.error("获取 Workflow 实例失败, clusterName={}, namespace={}, name={}",
                    clusterName, namespace, name, e);
            throw new RuntimeException("获取 Workflow 实例失败: ", e);
        }
    }

    @Override
    public IoArgoprojWorkflowV1alpha1WorkflowList listWorkflows(String clusterName, String namespace, List<String> phases) {
        log.info("查询 Workflow 实例列表, clusterName={}, namespace={}, phases={}", clusterName, namespace, phases);
        // 固定只列出 go-cicd-pipeline 模板的实例；如传入 phases，再叠加 phase 过滤
        // 注意：LABEL_TEMPLATE 为空时不能以 "," 开头拼接，否则 label selector 解析报错
        List<String> selectors = new java.util.ArrayList<>();
        if (LABEL_TEMPLATE != null && !LABEL_TEMPLATE.isEmpty()) {
            selectors.add(LABEL_TEMPLATE);
        }
        if (phases != null && !phases.isEmpty()) {
            selectors.add("workflows.argoproj.io/phase in (" + String.join(",", phases) + ")");
        }
        String labelSelector = String.join(",", selectors);
        try {
            // listOptionsLimit（第9参）+ fields（第11参）做服务端裁剪，
            // 避免返回 managedFields / 完整 spec / status.nodes 等大字段
            IoArgoprojWorkflowV1alpha1WorkflowList result = workflowApi(clusterName).workflowServiceListWorkflows(
                    namespace, labelSelector, null, null, null, null, null, null,
                    LIST_LIMIT, null, LIST_FIELDS);
            int count = result.getItems() == null ? 0 : result.getItems().size();
            log.info("查询 Workflow 实例列表成功, clusterName={}, namespace={}, count={}", clusterName, namespace, count);
            return result;
        } catch (ApiException e) {
            log.error("查询 Workflow 实例列表失败, clusterName={}, namespace={}, code={}, body={}",
                    clusterName, namespace, e.getCode(), e.getResponseBody(), e);
            throw new RuntimeException("查询 Workflow 实例列表失败: " + e.getResponseBody(), e);
        }
    }

    @Override
    public IoArgoprojWorkflowV1alpha1Workflow retryWorkflow(String clusterName, String namespace, String name) {
        log.info("重试 Workflow, clusterName={}, namespace={}, name={}", clusterName, namespace, name);
        try {
            IoArgoprojWorkflowV1alpha1Workflow result =
                    workflowApi(clusterName).workflowServiceRetryWorkflow(namespace, name, buildRetryRequest(namespace, name));
            log.info("重试 Workflow 成功, clusterName={}, namespace={}, name={}", clusterName, namespace, name);
            return result;
        } catch (ApiException e) {
            log.error("重试 Workflow 失败, clusterName={}, namespace={}, name={}, code={}, body={}",
                    clusterName, namespace, name, e.getCode(), e.getResponseBody(), e);
            throw new RuntimeException("重试 Workflow 失败: " + e.getResponseBody(), e);
        }
    }

    @Override
    public IoArgoprojWorkflowV1alpha1Workflow stopWorkflow(String clusterName, String namespace, String name) {
        log.info("停止 Workflow, clusterName={}, namespace={}, name={}", clusterName, namespace, name);
        try {
            IoArgoprojWorkflowV1alpha1Workflow result =
                    workflowApi(clusterName).workflowServiceStopWorkflow(namespace, name, buildStopRequest(namespace, name));
            log.info("停止 Workflow 成功, clusterName={}, namespace={}, name={}", clusterName, namespace, name);
            return result;
        } catch (ApiException e) {
            log.error("停止 Workflow 失败, clusterName={}, namespace={}, name={}, code={}, body={}",
                    clusterName, namespace, name, e.getCode(), e.getResponseBody(), e);
            throw new RuntimeException("停止 Workflow 失败: " + e.getResponseBody(), e);
        }
    }

    @Override
    public IoArgoprojWorkflowV1alpha1Workflow terminateWorkflow(String clusterName, String namespace, String name) {
        log.info("终止 Workflow, clusterName={}, namespace={}, name={}", clusterName, namespace, name);
        try {
            IoArgoprojWorkflowV1alpha1Workflow result = workflowApi(clusterName).workflowServiceTerminateWorkflow(
                    namespace, name, buildTerminateRequest(namespace, name));
            log.info("终止 Workflow 成功, clusterName={}, namespace={}, name={}", clusterName, namespace, name);
            return result;
        } catch (ApiException e) {
            log.error("终止 Workflow 失败, clusterName={}, namespace={}, name={}, code={}, body={}",
                    clusterName, namespace, name, e.getCode(), e.getResponseBody(), e);
            throw new RuntimeException("终止 Workflow 失败: " + e.getResponseBody(), e);
        }
    }

    @Override
    public IoArgoprojWorkflowV1alpha1Workflow resumeWorkflow(String clusterName, String namespace, String name) {
        log.info("恢复 Workflow, clusterName={}, namespace={}, name={}", clusterName, namespace, name);
        try {
            IoArgoprojWorkflowV1alpha1Workflow result = workflowApi(clusterName).workflowServiceResumeWorkflow(
                    namespace, name, buildResumeRequest(namespace, name));
            log.info("恢复 Workflow 成功, clusterName={}, namespace={}, name={}", clusterName, namespace, name);
            return result;
        } catch (ApiException e) {
            log.error("恢复 Workflow 失败, clusterName={}, namespace={}, name={}, code={}, body={}",
                    clusterName, namespace, name, e.getCode(), e.getResponseBody(), e);
            throw new RuntimeException("恢复 Workflow 失败: " + e.getResponseBody(), e);
        }
    }

    @Override
    public IoArgoprojWorkflowV1alpha1Workflow suspendWorkflow(String clusterName, String namespace, String name) {
        log.info("暂停 Workflow, clusterName={}, namespace={}, name={}", clusterName, namespace, name);
        try {
            IoArgoprojWorkflowV1alpha1Workflow result = workflowApi(clusterName).workflowServiceSuspendWorkflow(
                    namespace, name, buildSuspendRequest(namespace, name));
            log.info("暂停 Workflow 成功, clusterName={}, namespace={}, name={}", clusterName, namespace, name);
            return result;
        } catch (ApiException e) {
            log.error("暂停 Workflow 失败, clusterName={}, namespace={}, name={}, code={}, body={}",
                    clusterName, namespace, name, e.getCode(), e.getResponseBody(), e);
            throw new RuntimeException("暂停 Workflow 失败: " + e.getResponseBody(), e);
        }
    }

    @Override
    public void deleteWorkflow(String clusterName, String namespace, String name) {
        log.info("删除 Workflow, clusterName={}, namespace={}, name={}", clusterName, namespace, name);
        try {
            workflowApi(clusterName).workflowServiceDeleteWorkflow(namespace, name,
                    null, null, null, null, null, null, null);
            log.info("删除 Workflow 成功, clusterName={}, namespace={}, name={}", clusterName, namespace, name);
        } catch (ApiException e) {
            log.error("删除 Workflow 失败, clusterName={}, namespace={}, name={}, code={}, body={}",
                    clusterName, namespace, name, e.getCode(), e.getResponseBody(), e);
            throw new RuntimeException("删除 Workflow 失败: " + e.getResponseBody(), e);
        }
    }

    /**
     * 构建工作流重试请求
     */
    private IoArgoprojWorkflowV1alpha1WorkflowRetryRequest buildRetryRequest(String namespace, String name) {
        IoArgoprojWorkflowV1alpha1WorkflowRetryRequest request = new IoArgoprojWorkflowV1alpha1WorkflowRetryRequest();
        request.setNamespace(namespace);
        request.setName(name);
        return request;
    }

    /**
     * 构建工作流停止请求
     */
    private IoArgoprojWorkflowV1alpha1WorkflowStopRequest buildStopRequest(String namespace, String name) {
        IoArgoprojWorkflowV1alpha1WorkflowStopRequest request = new IoArgoprojWorkflowV1alpha1WorkflowStopRequest();
        request.setNamespace(namespace);
        request.setName(name);
        return request;
    }

    /**
     * 构建工作流终止请求
     */
    private IoArgoprojWorkflowV1alpha1WorkflowTerminateRequest buildTerminateRequest(String namespace, String name) {
        IoArgoprojWorkflowV1alpha1WorkflowTerminateRequest request = new IoArgoprojWorkflowV1alpha1WorkflowTerminateRequest();
        request.setNamespace(namespace);
        request.setName(name);
        return request;
    }

    /**
     * 构建工作流恢复请求
     */
    private IoArgoprojWorkflowV1alpha1WorkflowResumeRequest buildResumeRequest(String namespace, String name) {
        IoArgoprojWorkflowV1alpha1WorkflowResumeRequest request = new IoArgoprojWorkflowV1alpha1WorkflowResumeRequest();
        request.setNamespace(namespace);
        request.setName(name);
        return request;
    }

    /**
     * 构建工作流暂停请求
     */
    private IoArgoprojWorkflowV1alpha1WorkflowSuspendRequest buildSuspendRequest(String namespace, String name) {
        IoArgoprojWorkflowV1alpha1WorkflowSuspendRequest request = new IoArgoprojWorkflowV1alpha1WorkflowSuspendRequest();
        request.setNamespace(namespace);
        request.setName(name);
        return request;
    }

    /**
     * 构建工作流提交请求
     */
    private IoArgoprojWorkflowV1alpha1WorkflowSubmitRequest buildWorkflowSubmitRequest(String namespace,
                                                                                       IoArgoprojWorkflowV1alpha1Workflow workflow,
                                                                                       List<String> parameters) {
        IoArgoprojWorkflowV1alpha1WorkflowSubmitRequest request = new IoArgoprojWorkflowV1alpha1WorkflowSubmitRequest();
        request.setNamespace(namespace);
        request.setResourceKind(RESOURCE_KIND_WORKFLOW_TEMPLATE);
        request.setResourceName(getWorkflowResourceName(workflow));

        IoArgoprojWorkflowV1alpha1SubmitOpts submitOptions = new IoArgoprojWorkflowV1alpha1SubmitOpts();
        if (parameters != null && !parameters.isEmpty()) {
            submitOptions.setParameters(parameters);
        }
        request.setSubmitOptions(submitOptions);

        return request;
    }

    /**
     * 构建按 WorkflowTemplate 名称提交工作流的请求（resourceKind 固定为 WorkflowTemplate，resourceName 为模板名）。
     */
    private IoArgoprojWorkflowV1alpha1WorkflowSubmitRequest buildWorkflowSubmitRequestByTemplate(String namespace,
                                                                                                 String templateName,
                                                                                                 List<String> parameters) {
        IoArgoprojWorkflowV1alpha1WorkflowSubmitRequest request = new IoArgoprojWorkflowV1alpha1WorkflowSubmitRequest();
        request.setNamespace(namespace);
        request.setResourceKind(RESOURCE_KIND_WORKFLOW_TEMPLATE);
        request.setResourceName(templateName);

        IoArgoprojWorkflowV1alpha1SubmitOpts submitOptions = new IoArgoprojWorkflowV1alpha1SubmitOpts();
        if (parameters != null && !parameters.isEmpty()) {
            submitOptions.setParameters(parameters);
        }
        request.setSubmitOptions(submitOptions);

        return request;
    }

    /**
     * 从 Workflow 对象中提取 resourceName（优先取 name，其次取 generateName 去掉末尾的 "-"）
     */
    private String getWorkflowResourceName(IoArgoprojWorkflowV1alpha1Workflow workflow) {
        String name = Optional.of(workflow)
                .map(IoArgoprojWorkflowV1alpha1Workflow::getMetadata)
                .map(V1ObjectMeta::getName)
                .orElse(null);
        if (name != null && !name.isEmpty()) {
            return name;
        }
        String generateName = Optional.of(workflow)
                .map(IoArgoprojWorkflowV1alpha1Workflow::getMetadata)
                .map(V1ObjectMeta::getGenerateName)
                .orElse("");
        // 去掉末尾的 "-"
        if (generateName.endsWith("-")) {
            return generateName.substring(0, generateName.length() - 1);
        }
        return generateName;
    }

    /**
     * 清除服务端管理的 metadata 字段（创建场景）：resourceVersion / uid / creationTimestamp。
     * 这些字段由服务端分配，跨集群同步/导入的模板 JSON 若携带会导致创建失败或数据错乱。
     */
    private void clearServerManagedMetadata(IoArgoprojWorkflowV1alpha1WorkflowTemplate template) {
        V1ObjectMeta meta = template.getMetadata();
        if (meta == null) {
            return;
        }
        meta.setResourceVersion(null);
        meta.setUid(null);
        meta.setCreationTimestamp(null);
    }

    /**
     * 为 WorkflowTemplate 设置默认的 apiVersion 和 kind
     */
    private void buildDefaultForWorkflowTemplate(IoArgoprojWorkflowV1alpha1WorkflowTemplate template) {
        if (template.getApiVersion() == null || template.getApiVersion().isEmpty()) {
            template.setApiVersion(DEFAULT_API_VERSION);
        }
        if (template.getKind() == null || template.getKind().isEmpty()) {
            template.setKind(DEFAULT_KIND_WORKFLOW_TEMPLATE);
        }
    }
}
