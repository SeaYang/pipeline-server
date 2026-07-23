package com.ci.pipeline.common.enums;

/**
 * 前端组件类型枚举。
 * <p>对应 pipeline_parameter.component_type 字段，决定参数在执行弹框中的渲染方式。
 */
public enum ComponentTypeEnum {

    INPUT("input", "输入框"),
    SELECT("select", "下拉框"),
    RADIO("radio", "单选框组"),
    GIT_TREE("git-tree", "Git目录树"),
    DISABLED_INPUT("disabled-input", "只读输入框"),
    HIDDEN("hidden", "隐藏");

    private final String code;
    private final String description;

    ComponentTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ComponentTypeEnum ofCode(String code) {
        if (code == null) {
            return null;
        }
        for (ComponentTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }

    public static boolean isValidCode(String code) {
        return ofCode(code) != null;
    }
}
