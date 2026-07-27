package com.ci.pipeline.common.constants;

/**
 * GitLab 模块相关常量定义。
 * 业务类（Controller / Service）中不允许直接出现魔法值字符串，
 * 所有 GitLab 模块的常量与提示信息统一在此维护。
 */
public final class GitLabConstants {

    private GitLabConstants() {
    }

    // ===== GenericConfig 配置 key =====

    /** GenericConfig 配置 key：GitLab API 地址 */
    public static final String CONFIG_KEY_API_URL = "gitlab.api.url";

    /** GenericConfig 配置 key：GitLab access token */
    public static final String CONFIG_KEY_API_TOKEN = "gitlab.api.token";

    // ===== SSH URL 解析 =====

    /** SSH URL 解析正则：git@host:namespace/project.git */
    public static final String GIT_SSH_URL_REGEX = "^git@([^:]+):(.+?)(\\.git)?$";

    // ===== 目录树节点类型 =====

    /** 目录树节点类型：目录 */
    public static final String TREE_TYPE_TREE = "tree";

    /** 目录树节点类型：文件 */
    public static final String TREE_TYPE_BLOB = "blob";

    // ===== 提示信息 =====

    /** git 仓库地址格式不正确，仅支持 SSH 格式 */
    public static final String MSG_INVALID_GIT_SSH_URL = "git仓库地址格式不正确，仅支持SSH格式";

    /** 查询 GitLab 仓库信息失败（参数：错误详情） */
    public static final String MSG_GET_PROJECT_FAILED = "查询GitLab仓库信息失败：%s";

    /** 查询分支列表失败（参数：错误详情） */
    public static final String MSG_GET_BRANCHES_FAILED = "查询分支列表失败：%s";

    /** 查询目录树失败（参数：错误详情） */
    public static final String MSG_GET_TREE_FAILED = "查询目录树失败：%s";

    /** 应用未配置 GitLab 仓库，repoId 为空（参数：appName） */
    public static final String MSG_REPO_ID_NOT_FOUND = "应用[%s]未配置GitLab仓库";

    /** 应用不存在（参数：appName） */
    public static final String MSG_APP_NOT_FOUND = "应用[%s]不存在";
}
