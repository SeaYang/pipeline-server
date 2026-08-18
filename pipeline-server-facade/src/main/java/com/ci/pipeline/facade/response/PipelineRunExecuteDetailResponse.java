package com.ci.pipeline.facade.response;

import io.argoproj.workflow.models.IoArgoprojWorkflowV1alpha1Workflow;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 流水线执行详情响应（Argo Workflow 实时数据 + 任务编码→中文名映射）
 */
@Data
public class PipelineRunExecuteDetailResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Argo Workflow 实时详情（结构同前端 go-cicd-workflow.json）
     */
    private IoArgoprojWorkflowV1alpha1Workflow argoDetail;

    /**
     * 执行集群（pipeline_run.cluster_name，存量为空时兜底解析）
     */
    private String clusterName;

    /**
     * 任务节点编码 → 中文名 映射（task_template_code → name）
     */
    private Map<String, String> taskCodeNameMap;
}
