package com.ci.pipeline.service.util;

import com.ci.pipeline.common.exception.BusinessException;
import com.ci.pipeline.common.constants.PipelineConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.argoproj.workflow.models.IoArgoprojWorkflowV1alpha1NodeStatus;
import io.argoproj.workflow.models.IoArgoprojWorkflowV1alpha1TemplateRef;
import io.argoproj.workflow.models.IoArgoprojWorkflowV1alpha1Workflow;
import io.argoproj.workflow.models.IoArgoprojWorkflowV1alpha1WorkflowStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Argo Workflow 模型遍历工具（纯模型读取，不依赖序列化框架）。
 */
public final class ArgoWorkflowUtil {

    /** Pod 类型节点（DAG 根节点为 DAG 类型，不对应可执行 Pod，需排除） */
    private static final String NODE_TYPE_POD = "Pod";

    private ArgoWorkflowUtil() {
    }

    /**
     * 获取 Workflow 的 metadata.generation（Argo 内部随状态/节点推进递增），用于判断是否有更新。
     *
     * @param workflow Workflow 对象
     * @return generation，缺失返回 {@code null}
     */
    public static Long getGeneration(IoArgoprojWorkflowV1alpha1Workflow workflow) {
        return Optional.ofNullable(workflow)
                .map(IoArgoprojWorkflowV1alpha1Workflow::getMetadata)
                .map(meta -> meta.getGeneration())
                .orElse(null);
    }

    /**
     * 列出 Workflow 中所有 {@code type=Pod} 的节点状态（即可执行的任务节点，排除 DAG 根节点）。
     *
     * @param workflow Workflow 对象
     * @return Pod 节点列表，无则空列表
     */
    public static List<IoArgoprojWorkflowV1alpha1NodeStatus> listPodNodes(IoArgoprojWorkflowV1alpha1Workflow workflow) {
        Map<String, IoArgoprojWorkflowV1alpha1NodeStatus> nodes = Optional.ofNullable(workflow)
                .map(IoArgoprojWorkflowV1alpha1Workflow::getStatus)
                .map(IoArgoprojWorkflowV1alpha1WorkflowStatus::getNodes)
                .orElse(Collections.emptyMap());
        if (nodes == null || nodes.isEmpty()) {
            return Collections.emptyList();
        }
        List<IoArgoprojWorkflowV1alpha1NodeStatus> podNodes = new ArrayList<>();
        for (IoArgoprojWorkflowV1alpha1NodeStatus node : nodes.values()) {
            if (node != null && NODE_TYPE_POD.equals(node.getType())) {
                podNodes.add(node);
            }
        }
        return podNodes;
    }

    /**
     * 取节点对应的任务模板编码：优先 {@code templateRef.name}（= task_template_code），
     * 回退 {@code templateName}，再回退 {@code displayName}。
     *
     * @param node 节点状态
     * @return 任务模板编码
     */
    public static String templateCodeOf(IoArgoprojWorkflowV1alpha1NodeStatus node) {
        if (node == null) {
            return null;
        }
        String code = Optional.of(node)
                .map(IoArgoprojWorkflowV1alpha1NodeStatus::getTemplateRef)
                .map(IoArgoprojWorkflowV1alpha1TemplateRef::getName)
                .orElse(null);
        if (code != null && !code.isEmpty()) {
            return code;
        }
        String templateName = node.getTemplateName();
        if (templateName != null && !templateName.isEmpty()) {
            return templateName;
        }
        return node.getDisplayName();
    }

    /**
     * 计算 Pod 节点对应的 Pod 名称。
     * <p>{@code workflows.argoproj.io/pod-name-format=v2} 下 Pod 名称与 node.id 不同：
     * node.id 形如 {@code {workflowName}-{suffix}}，而 Pod 名称形如
     * {@code {workflowName}-{template}-{suffix}}，比 node.id 多中间一段 template。
     * <p>template 取 {@code node.templateName}，为空时回退 {@code node.templateRef.template}
     * （如 entrypoint）；suffix 为 node.id 去掉 workflowName 前缀后的余部（含分隔符 {@code -}）。
     *
     * @param workflow Workflow 对象
     * @param node     Pod 节点状态
     * @return Pod 名称
     */
    public static String getPodName(IoArgoprojWorkflowV1alpha1Workflow workflow, IoArgoprojWorkflowV1alpha1NodeStatus node) {
        if (workflow == null || workflow.getMetadata() == null || node == null) {
            return null;
        }
        String workflowName = workflow.getMetadata().getName();
        String nodeId = node.getId();
        if (workflowName == null || workflowName.isEmpty() || nodeId == null || nodeId.isEmpty()) {
            return null;
        }
        String template = node.getTemplateName();
        if (template == null || template.isEmpty()) {
            template = Optional.ofNullable(node.getTemplateRef())
                    .map(IoArgoprojWorkflowV1alpha1TemplateRef::getTemplate)
                    .orElse("");
        }
        // node.id 去掉 workflowName 前缀后的余部（含前导 '-'），与 template 拼出 Pod 名
        String suffix = nodeId.startsWith(workflowName) ? nodeId.substring(workflowName.length()) : ("-" + nodeId);
        return workflowName + "-" + template + suffix;
    }

    // ============================== WorkflowTemplate 参数解析 ==============================

    /**
     * 从 WorkflowTemplate 详情 JSON 中提取 spec.arguments.parameters 下所有参数的 name。
     *
     * @param templateDetail WorkflowTemplate 的 json 字符串
     * @param objectMapper   Jackson ObjectMapper
     * @return 参数名列表（模板未声明 parameters 时返回空列表）
     * @throws BusinessException 模板 JSON 解析失败
     */
    public static List<String> extractParamNames(String templateDetail, ObjectMapper objectMapper) {
        try {
            JsonNode root = objectMapper.readTree(templateDetail);
            JsonNode parameters = root.path("spec").path("arguments").path("parameters");
            if (!parameters.isArray()) {
                return Collections.emptyList();
            }
            List<String> names = new ArrayList<>(parameters.size());
            for (JsonNode param : parameters) {
                JsonNode nameNode = param.get("name");
                if (nameNode != null && !nameNode.isNull() && !nameNode.asText().isEmpty()) {
                    names.add(nameNode.asText());
                }
            }
            return names;
        } catch (Exception e) {
            throw new BusinessException(String.format(
                    PipelineConstants.MSG_TEMPLATE_DETAIL_PARSE_FAILED, e.getMessage()));
        }
    }

    /**
     * 从 WorkflowTemplate 详情 JSON 中提取指定参数的 default 值。
     * <p>用于未定义参数降级时兜底（参数定义表中未配置，但模板 JSON 中声明了 default）。
     *
     * @param templateDetail WorkflowTemplate 的 json 字符串
     * @param paramName      参数名
     * @param objectMapper   Jackson ObjectMapper
     * @return default 值，不存在时返回 null
     */
    public static String extractParamDefault(String templateDetail, String paramName, ObjectMapper objectMapper) {
        try {
            JsonNode root = objectMapper.readTree(templateDetail);
            JsonNode parameters = root.path("spec").path("arguments").path("parameters");
            if (!parameters.isArray()) {
                return null;
            }
            for (JsonNode param : parameters) {
                JsonNode nameNode = param.get("name");
                if (nameNode != null && paramName.equals(nameNode.asText())) {
                    JsonNode defaultNode = param.get("default");
                    if (defaultNode != null && !defaultNode.isNull()) {
                        return defaultNode.asText();
                    }
                    return null;
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}

