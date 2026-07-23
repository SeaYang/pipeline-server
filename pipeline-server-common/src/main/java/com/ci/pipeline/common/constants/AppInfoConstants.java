package com.ci.pipeline.common.constants;

/**
 * 应用基础信息模块相关常量定义。
 * <p>
 * 业务类（Controller / Service）中不允许直接出现魔法值字符串，所有应用基础信息模块的
 * 常量与提示信息统一在此维护。
 */
public final class AppInfoConstants {

    private AppInfoConstants() {
    }

    // ===== 应用基础信息提示信息 =====

    /**
     * 应用名称不能为空
     */
    public static final String MSG_APP_NAME_REQUIRED = "应用名称不能为空";

    /**
     * 编程语言/平台不能为空
     */
    public static final String MSG_PROGRAMMING_LANGUAGE_REQUIRED = "编程语言/平台不能为空";

    /**
     * git 仓库地址不能为空
     */
    public static final String MSG_GIT_SSH_URL_REQUIRED = "git仓库地址不能为空";

    /**
     * 应用 id 不能为空
     */
    public static final String MSG_APP_ID_REQUIRED = "应用 id 不能为空";

    /**
     * 应用不存在
     */
    public static final String MSG_APP_NOT_EXIST = "应用不存在";

    /**
     * 应用名称已存在（参数：app_name）
     */
    public static final String MSG_APP_NAME_DUPLICATED = "应用名称已存在：%s";
}
