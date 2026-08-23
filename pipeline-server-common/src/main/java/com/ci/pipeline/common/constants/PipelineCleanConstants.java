package com.ci.pipeline.common.constants;

/**
 * 流水线终态清理相关常量
 */
public final class PipelineCleanConstants {

    /** 清理流水线模板编码（Argo WorkflowTemplate 名称） */
    public static final String CLEAN_TEMPLATE_NAME = "pipeline-clean";

    /** 清理流水线入参名：业务流水线的 pipelineRunName */
    public static final String PARAM_PIPELINE_RUN_NAME = "pipeline-run-name";

    /** 清理流水线入参名：删除前等待秒数（Argo submit API 传参时模板 default 不生效，必须显式传值） */
    public static final String PARAM_CLEAN_WAIT_SECONDS = "clean-wait-seconds";

    /** 删除前等待秒数：给 Pod 终止与日志收敛留缓冲，与模板 default 保持一致 */
    public static final String CLEAN_WAIT_SECONDS_VALUE = "30";

    private PipelineCleanConstants() {
    }
}
