package com.ci.pipeline.facade.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 流水线执行详情 DTO（SSE 推送 + HTTP 查询统一出参）。
 * <p>无论流水线是否终态，前端拿到的结构一致：
 * <ul>
 *   <li>workflowDetail：VueFlow 渲染数据（非终态来自 snapshot，终态由模板+task_run 组装）</li>
 *   <li>taskCodeNameMap：任务编码→中文名（始终从 pipeline_template_version.templateDetail 解析）</li>
 * </ul>
 */
@Data
public class PipelineRunDetailDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 流水线执行名称（Argo Workflow 名称） */
    private String pipelineRunName;

    /** 流水线执行记录主键 id */
    private Long pipelineRunId;

    /** 流水线执行状态（Pending/Running/Succeeded/Failed/Error/Cancelled） */
    private String status;

    /** 开始时间（ISO 8601 字符串） */
    private String startedAt;

    /** 结束时间（ISO 8601 字符串） */
    private String finishedAt;

    /** 执行时长（秒） */
    private Integer duration;

    /** 失败/错误信息 */
    private String failMessage;

    /** 服务 appName（pipeline_run.app_name） */
    private String appName;

    /** 流水线模板编码（pipeline_run.pipeline_template_code） */
    private String pipelineTemplateCode;

    /** 流水线模板名称（查 pipeline_template.name，只查一个字段） */
    private String pipelineTemplateName;

    /** 执行人（pipeline_run.creator） */
    private String creator;

    /** 流水线名称（根据 pipelineId 查 pipeline.name，只查一个字段） */
    private String pipelineName;

    /** 运行参数 JSON 字符串，如 {"loop-count":"30","loop-interval":"2"}（pipeline_run.arguments） */
    private String arguments;

    /**
     * VueFlow 渲染数据（结构兼容前端 ArgoWorkflowDetail）。
     * <p>非终态：来自 pipeline_run_snapshot.detail（Argo Workflow JSON）；
     * 终态：由 templateDetail（DAG 骨架）+ pipeline_task_run（运行数据）组装。
     */
    private JsonNode workflowDetail;

    /**
     * 任务编码→中文名映射。
     * <p>始终从 pipeline_template_version.templateDetail 解析 dag.tasks[].name，
     * 再批量查 task_template 得中文名。不依赖 pipeline_task_run（节点可能未全部执行）。
     */
    private Map<String, String> taskCodeNameMap;
}
