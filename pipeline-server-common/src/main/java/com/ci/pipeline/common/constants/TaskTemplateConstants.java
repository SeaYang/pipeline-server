package com.ci.pipeline.common.constants;

/**
 * 任务模板模块相关常量定义。
 * <p>
 * 业务类（Controller / Service）中不允许直接出现魔法值字符串，所有任务模板模块的
 * 常量与提示信息统一在此维护。
 */
public final class TaskTemplateConstants {

    private TaskTemplateConstants() {
    }

    /**
     * 任务模板所属分组的字典类型标识（对应 dict_data.dict_type）。
     * <p>分组下拉列表固定从该字典类型查询，按 sort 值排序。
     */
    public static final String DICT_TYPE_TASK_TEMPLATE_GROUP = "task-template-group";

    /**
     * 任务模板编码格式：小写字母，多段用 - 连接，如 a、a-b、a-b-c
     */
    public static final String CODE_REGEX = "^[a-z]+(-[a-z]+)*$";

    // ===== 任务模板提示信息 =====

    /**
     * 任务模板编码不能为空
     */
    public static final String MSG_TEMPLATE_CODE_REQUIRED = "任务模板编码不能为空";

    /**
     * 任务模板名称不能为空
     */
    public static final String MSG_TEMPLATE_NAME_REQUIRED = "任务模板名称不能为空";

    /**
     * 任务模板所属分组不能为空
     */
    public static final String MSG_TEMPLATE_GROUP_REQUIRED = "任务模板所属分组不能为空";

    /**
     * 任务模板 id 不能为空
     */
    public static final String MSG_TEMPLATE_ID_REQUIRED = "任务模板 id 不能为空";

    /**
     * 任务模板不存在
     */
    public static final String MSG_TEMPLATE_NOT_EXIST = "任务模板不存在";

    /**
     * 任务模板编码已存在（参数：task_template_code）
     */
    public static final String MSG_TEMPLATE_CODE_DUPLICATED = "任务模板编码已存在：%s";

    /**
     * 任务模板下存在版本，不能删除（参数：task_template_code）
     */
    public static final String MSG_TEMPLATE_HAS_VERSION = "任务模板[%s]下存在版本，不能删除";

    // ===== 任务模板版本提示信息 =====

    /**
     * 版本号不能为空
     */
    public static final String MSG_VERSION_REQUIRED = "版本号不能为空";

    /**
     * 版本号格式非法（应为三段点分数字，如 0.0.1）
     */
    public static final String MSG_VERSION_FORMAT_INVALID = "版本号格式非法，应为三段点分数字，如 0.0.1";

    /**
     * 版本号递增非法（参数：当前最大版本、传入版本）
     */
    public static final String MSG_VERSION_INCREMENT_INVALID = "版本号只能递增且每个位置最多递增1，当前最大版本：%s，传入版本：%s";

    /**
     * 任务模板版本不存在
     */
    public static final String MSG_VERSION_NOT_EXIST = "任务模板版本不存在";

    /**
     * 仅草稿状态的版本允许修改（参数：当前状态）
     */
    public static final String MSG_VERSION_UPDATE_STATUS_INVALID = "仅草稿状态的版本允许修改，当前状态：%s";

    /**
     * 任务模板版本已存在（参数：task_template_code、version）
     */
    public static final String MSG_VERSION_DUPLICATED = "任务模板[%s]下版本[%s]已存在";

    /**
     * 任务模板详情（template_detail）不能为空
     */
    public static final String MSG_TEMPLATE_DETAIL_REQUIRED = "任务模板详情不能为空";

    /**
     * 任务模板详情无法解析为合法的 WorkflowTemplate（参数：解析异常信息）
     */
    public static final String MSG_TEMPLATE_DETAIL_INVALID = "任务模板详情无法解析为合法的 WorkflowTemplate：%s";

    /**
     * 任务模板详情 metadata.name 与任务模板编码不一致（参数：metadata.name、taskTemplateCode）
     */
    public static final String MSG_TEMPLATE_NAME_NOT_MATCH_CODE =
            "任务模板详情 metadata.name 必须与任务模板编码一致，metadata.name=%s，taskTemplateCode=%s";

    /**
     * 版本状态非法（参数：传入的状态值）
     */
    public static final String MSG_VERSION_STATUS_INVALID = "版本状态非法，仅支持 DRAFT / EFFECTIVE / EXPIRED，传入：%s";

    /**
     * 任务模板编码格式不正确（参数：task_template_code）
     */
    public static final String MSG_TEMPLATE_CODE_FORMAT_INVALID =
            "任务模板编码格式不正确，需为小写字母并用 - 连接的多段格式，如 a、a-b、a-b-c，传入：%s";

    /**
     * 任务模板编码与流水线模板编码冲突（参数：code）
     */
    public static final String MSG_TEMPLATE_CODE_CONFLICT_PIPELINE =
            "编码[%s]在流水线模板中已存在，任务模板与流水线模板编码需全局唯一";

    /**
     * 操作过于频繁，请稍后重试（分布式锁获取失败）
     */
    public static final String MSG_OPERATION_LOCK_FAILED = "操作过于频繁，请稍后重试";
}
