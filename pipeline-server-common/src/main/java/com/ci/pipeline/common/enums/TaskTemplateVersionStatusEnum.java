package com.ci.pipeline.common.enums;

/**
 * 任务模板版本状态枚举。
 * <p>对应 task_template_version.status 字段，DB 存英文编码，对外展示用中文描述。
 */
public enum TaskTemplateVersionStatusEnum {

    /**
     * 草稿
     */
    DRAFT("DRAFT", "草稿"),

    /**
     * 生效中
     */
    EFFECTIVE("EFFECTIVE", "生效中"),

    /**
     * 已失效
     */
    EXPIRED("EXPIRED", "已失效");

    private final String code;
    private final String description;

    TaskTemplateVersionStatusEnum(String code, String description) {
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
     * 根据编码解析为枚举。
     *
     * @param code 状态编码（大小写敏感）
     * @return 对应枚举，不存在返回 {@code null}
     */
    public static TaskTemplateVersionStatusEnum ofCode(String code) {
        if (code == null) {
            return null;
        }
        for (TaskTemplateVersionStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 编码是否合法。
     *
     * @param code 状态编码
     * @return 合法返回 true
     */
    public static boolean isValidCode(String code) {
        return ofCode(code) != null;
    }
}
