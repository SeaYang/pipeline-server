package com.ci.pipeline.common.constants;

/**
 * 制品相关常量
 */
public final class ArtifactConstants {

    private ArtifactConstants() {}

    public static final String MSG_ARTIFACT_NOT_EXIST = "制品不存在";
    public static final String MSG_PIPELINE_RUN_NAME_REQUIRED = "流水线运行名称不能为空";
    public static final String MSG_ARTIFACT_NAME_REQUIRED = "制品名称不能为空";
    public static final String MSG_ARTIFACT_TYPE_REQUIRED = "制品类型不能为空";
    public static final String MSG_ARTIFACT_TYPE_INVALID = "制品类型无效，仅支持 RAW / IMAGE";
}
