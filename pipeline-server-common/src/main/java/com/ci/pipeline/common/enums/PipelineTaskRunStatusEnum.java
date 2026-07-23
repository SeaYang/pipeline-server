package com.ci.pipeline.common.enums;

/**
 * 流水线任务节点执行状态枚举。
 * <p>对应 pipeline_task_run.status 字段，编码与 Argo Workflow 节点（NodeStatus）的 phase 一致，
 * 包含节点级特有的 Skipped / Omitted。DB 存英文编码，对外展示用中文描述。
 */
public enum PipelineTaskRunStatusEnum {

    /**
     * 排队中
     */
    PENDING("Pending", "排队中"),

    /**
     * 运行中
     */
    RUNNING("Running", "运行中"),

    /**
     * 成功
     */
    SUCCEEDED("Succeeded", "成功"),

    /**
     * 失败
     */
    FAILED("Failed", "失败"),

    /**
     * 错误
     */
    ERROR("Error", "错误"),

    /**
     * 跳过（DAG 中未执行的节点）
     */
    SKIPPED("Skipped", "跳过"),

    /**
     * 省略（被 omit 阶段略过）
     */
    OMITTED("Omitted", "省略");

    private final String code;
    private final String description;

    PipelineTaskRunStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 是否终态（节点视角）：成功 / 失败 / 错误 / 跳过 / 省略。
     *
     * @return 终态返回 true
     */
    public boolean isTerminal() {
        return this != PENDING && this != RUNNING;
    }

    /**
     * 根据编码解析为枚举。
     *
     * @param code 状态编码（大小写敏感）
     * @return 对应枚举，不存在返回 {@code null}
     */
    public static PipelineTaskRunStatusEnum ofCode(String code) {
        if (code == null) {
            return null;
        }
        for (PipelineTaskRunStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}
