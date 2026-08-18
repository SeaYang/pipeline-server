package com.ci.pipeline.common.enums;

/**
 * 流水线执行状态枚举。
 * <p>对应 pipeline_run.status 字段，编码与 Argo Workflow 的 {@code status.phase} 完全一致
 * （大小写敏感），便于直接用 Argo 返回的 phase 反查本枚举。DB 存英文编码，对外展示用中文描述。
 */
public enum PipelineRunStatusEnum {

    /**
     * 排队中（Argo Pending）
     */
    PENDING("Pending", "排队中"),

    /**
     * 运行中（Argo Running）
     */
    RUNNING("Running", "运行中"),

    /**
     * 成功（Argo Succeeded）
     */
    SUCCEEDED("Succeeded", "成功"),

    /**
     * 失败（Argo Failed）
     */
    FAILED("Failed", "失败"),

    /**
     * 错误（Argo Error）
     */
    ERROR("Error", "错误"),

    /**
     * 未知（Argo Unknown，一般为短暂态，继续轮询）
     */
    UNKNOWN("Unknown", "未知"),

    /**
     * 已取消（平台扩展态：用户停止 / 终止后由平台写入；Argo 本身不产生该状态）
     */
    CANCELLED("Cancelled", "已取消");

    private final String code;
    private final String description;

    PipelineRunStatusEnum(String code, String description) {
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
     * 是否终态（平台视角不会再自动变更的状态）：成功 / 已取消。
     * <p>注意：Failed / Error 不算终态，因为用户可以重试使其重新进入 Running；
     * Unknown 为短暂态，也不算终态，需继续轮询。
     * <p>语义：用于 SSE 推送（终态才关闭连接）、日志来源选择（终态才从 DB 取）、
     * task_run 落地（终态才落地）等场景。
     *
     * @return 终态返回 true
     */
    public boolean isTerminal() {
        return this == SUCCEEDED || this == CANCELLED;
    }

    /**
     * 是否 Argo 侧已稳定（不会再自动变更）：终态 + 失败态。
     * <p>即 Succeeded / Cancelled / Failed / Error。这些状态下 Argo Workflow 自身不会再变更，
     * 异步状态同步线程遇到这些状态可以停止轮询（节省资源）。
     * <p>与 {@link #isTerminal()} 的区别：Failed / Error 在平台视角不是终态（可重试），
     * 但 Argo 侧已稳定，同步线程无需继续轮询——重试时会重新拉起同步逻辑。
     *
     * @return Argo 侧已稳定返回 true
     */
    public boolean isArgoStable() {
        return isTerminal() || isFailure();
    }

    /**
     * 是否失败态：失败 / 错误。失败态需要回写 fail_type / fail_message。
     *
     * @return 失败态返回 true
     */
    public boolean isFailure() {
        return this == FAILED || this == ERROR;
    }

    /**
     * 根据 Argo 的 phase（=本枚举编码）解析为枚举。
     *
     * @param code 状态编码（大小写敏感）
     * @return 对应枚举，不存在返回 {@code null}
     */
    public static PipelineRunStatusEnum ofCode(String code) {
        if (code == null) {
            return null;
        }
        for (PipelineRunStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 状态编码是否已到终态（Succeeded / Cancelled）。
     * <p>编码无法解析时返回 false（保守认为未终态，交由后续流程处理）。
     *
     * @param code 状态编码
     * @return 终态返回 true
     */
    public static boolean isTerminalCode(String code) {
        PipelineRunStatusEnum status = ofCode(code);
        return status != null && status.isTerminal();
    }
}
