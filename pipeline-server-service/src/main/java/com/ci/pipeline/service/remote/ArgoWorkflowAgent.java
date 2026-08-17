package com.ci.pipeline.service.remote;

import io.argoproj.workflow.models.IoArgoprojWorkflowV1alpha1Workflow;
import io.argoproj.workflow.models.IoArgoprojWorkflowV1alpha1WorkflowList;
import io.argoproj.workflow.models.IoArgoprojWorkflowV1alpha1WorkflowTemplate;

import java.util.List;

/**
 * Argo Workflow 操作接口（多集群版：所有方法以 clusterName 路由到对应集群的 Argo Server）
 */
public interface ArgoWorkflowAgent {

    /**
     * 验证 WorkflowTemplate 的正确性
     *
     * @param clusterName      集群标识
     * @param namespace        命名空间
     * @param workflowTemplate 工作流模板对象
     * @return 验证通过后的 WorkflowTemplate
     */
    IoArgoprojWorkflowV1alpha1WorkflowTemplate lintWorkflowTemplate(String clusterName,
                                                                     String namespace,
                                                                     IoArgoprojWorkflowV1alpha1WorkflowTemplate workflowTemplate);

    /**
     * 创建 WorkflowTemplate（提交前会先进行 lint 验证）
     *
     * @param clusterName      集群标识
     * @param namespace        命名空间
     * @param workflowTemplate 工作流模板对象
     * @return 创建成功后的 WorkflowTemplate
     */
    IoArgoprojWorkflowV1alpha1WorkflowTemplate createWorkflowTemplate(String clusterName,
                                                                       String namespace,
                                                                       IoArgoprojWorkflowV1alpha1WorkflowTemplate workflowTemplate);

    /**
     * 更新 WorkflowTemplate（提交前会先进行 lint 验证）
     *
     * @param clusterName      集群标识
     * @param namespace        命名空间
     * @param name             模板名称
     * @param workflowTemplate 工作流模板对象
     * @return 更新成功后的 WorkflowTemplate
     */
    IoArgoprojWorkflowV1alpha1WorkflowTemplate updateWorkflowTemplate(String clusterName,
                                                                      String namespace,
                                                                      String name,
                                                                      IoArgoprojWorkflowV1alpha1WorkflowTemplate workflowTemplate);

    /**
     * 删除 WorkflowTemplate
     *
     * @param clusterName 集群标识
     * @param namespace   命名空间
     * @param name        模板名称
     */
    void deleteWorkflowTemplate(String clusterName, String namespace, String name);

    /**
     * 保存 WorkflowTemplate：先按名称查询是否已存在，存在则更新，不存在则创建（统一入口）。
     * <p>
     * 注意：{@link #getWorkflowTemplate(String, String, String)} 在模板不存在时会抛出异常，
     * 本方法据此判定走创建还是更新。
     *
     * @param clusterName      集群标识
     * @param namespace        命名空间
     * @param workflowTemplate 工作流模板对象（名称取自 metadata.name）
     * @return 创建 / 更新后的 WorkflowTemplate
     */
    IoArgoprojWorkflowV1alpha1WorkflowTemplate saveWorkflowTemplate(String clusterName,
                                                                    String namespace,
                                                                    IoArgoprojWorkflowV1alpha1WorkflowTemplate workflowTemplate);

    /**
     * 根据名称获取 WorkflowTemplate
     *
     * @param clusterName 集群标识
     * @param namespace   命名空间
     * @param name        模板名称
     * @return WorkflowTemplate
     */
    IoArgoprojWorkflowV1alpha1WorkflowTemplate getWorkflowTemplate(String clusterName, String namespace, String name);

    /**
     * 提交工作流
     *
     * @param clusterName 集群标识
     * @param namespace   命名空间
     * @param workflow    工作流对象（从中提取 resourceName）
     * @param parameters  工作流参数列表，可为 null
     * @return 提交后的 Workflow
     */
    IoArgoprojWorkflowV1alpha1Workflow submitWorkflow(String clusterName,
                                                      String namespace,
                                                      IoArgoprojWorkflowV1alpha1Workflow workflow,
                                                      List<String> parameters);

    /**
     * 按 WorkflowTemplate 名称提交工作流（resourceKind 固定为 WorkflowTemplate）。
     * <p>用于「系统已将流水线模板保存到 argo，仅持有模板名，需要直接从模板拉起一个 Workflow」的场景，
     * 调用方无需构造完整的 {@link IoArgoprojWorkflowV1alpha1Workflow} 对象。
     *
     * @param clusterName  集群标识
     * @param namespace    命名空间
     * @param templateName WorkflowTemplate 名称（即 resourceName）
     * @param parameters   工作流参数列表（Argo submitOpts 格式，每项为 {@code name=value}），可为 null
     * @return 提交后的 Workflow
     */
    IoArgoprojWorkflowV1alpha1Workflow submitWorkflowByTemplate(String clusterName,
                                                                String namespace,
                                                                String templateName,
                                                                List<String> parameters);

    /**
     * 查询单个 Workflow 实例（可从返回结果的 status.phase 判断是运行中还是已结束）
     *
     * @param clusterName 集群标识
     * @param namespace   命名空间
     * @param name        Workflow 名称
     * @return Workflow 实例
     */
    IoArgoprojWorkflowV1alpha1Workflow getWorkflow(String clusterName, String namespace, String name);

    /**
     * 按状态（phase）查询 Workflow 实例列表，用于查询运行中/已结束的实例。
     * phases 为 Argo Workflow 的状态值，例如 Running、Succeeded、Failed、Error；为空时查询全部。
     *
     * @param clusterName 集群标识
     * @param namespace   命名空间
     * @param phases      状态过滤列表，为空时不按状态过滤
     * @return Workflow 实例列表
     */
    IoArgoprojWorkflowV1alpha1WorkflowList listWorkflows(String clusterName, String namespace, List<String> phases);

    /**
     * 重试 Workflow（失败的节点会重新执行）
     *
     * @param clusterName 集群标识
     * @param namespace   命名空间
     * @param name        Workflow 名称
     * @return 重试后的 Workflow 实例
     */
    IoArgoprojWorkflowV1alpha1Workflow retryWorkflow(String clusterName, String namespace, String name);

    /**
     * 停止 Workflow（优雅停止，正在运行的节点会被停止）
     *
     * @param clusterName 集群标识
     * @param namespace   命名空间
     * @param name        Workflow 名称
     * @return 停止后的 Workflow 实例
     */
    IoArgoprojWorkflowV1alpha1Workflow stopWorkflow(String clusterName, String namespace, String name);

    /**
     * 终止 Workflow（强制终止整个 Workflow，不会等待节点结束）
     *
     * @param clusterName 集群标识
     * @param namespace   命名空间
     * @param name        Workflow 名称
     * @return 终止后的 Workflow 实例
     */
    IoArgoprojWorkflowV1alpha1Workflow terminateWorkflow(String clusterName, String namespace, String name);

    /**
     * 恢复处于暂停状态的 Workflow
     *
     * @param clusterName 集群标识
     * @param namespace   命名空间
     * @param name        Workflow 名称
     * @return 恢复后的 Workflow 实例
     */
    IoArgoprojWorkflowV1alpha1Workflow resumeWorkflow(String clusterName, String namespace, String name);

    /**
     * 暂停 Workflow（暂停后可通过 resumeWorkflow 恢复）
     *
     * @param clusterName 集群标识
     * @param namespace   命名空间
     * @param name        Workflow 名称
     * @return 暂停后的 Workflow 实例
     */
    IoArgoprojWorkflowV1alpha1Workflow suspendWorkflow(String clusterName, String namespace, String name);

    /**
     * 删除 Workflow 实例
     *
     * @param clusterName 集群标识
     * @param namespace   命名空间
     * @param name        Workflow 名称
     */
    void deleteWorkflow(String clusterName, String namespace, String name);
}
