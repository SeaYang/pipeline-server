package com.ci.pipeline.common.enums;

/**
 * 制品类型枚举
 */
public enum ArtifactTypeEnum {

    RAW("RAW", "原始制品，如 jar、go 二进制、前端 dist 等"),
    IMAGE("IMAGE", "镜像制品");

    private final String code;
    private final String description;

    ArtifactTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ArtifactTypeEnum ofCode(String code) {
        if (code == null) return null;
        for (ArtifactTypeEnum type : values()) {
            if (type.code.equals(code)) return type;
        }
        return null;
    }

    public static boolean isValidCode(String code) {
        return ofCode(code) != null;
    }
}
