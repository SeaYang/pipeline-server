package com.ci.pipeline.common.constants;

/**
 * 字典模块相关常量定义。
 * <p>
 * 业务类（Controller / Service）中不允许直接出现魔法值字符串，所有字典模块的
 * 常量与提示信息统一在此维护。
 */
public final class DictConstants {

    private DictConstants() {
    }

    /**
     * 默认排序值
     */
    public static final int DEFAULT_SORT = 0;

    // ===== 排序相关常量 =====

    /**
     * 升序
     */
    public static final String SORT_ORDER_ASC = "asc";

    /**
     * 降序（默认排序方向）
     */
    public static final String SORT_ORDER_DESC = "desc";

    /**
     * 不支持的排序字段（参数：传入的字段名、支持的字段集合）
     */
    public static final String MSG_SORT_FIELD_INVALID = "不支持的排序字段：%s，支持的排序字段有：%s";

    /**
     * 排序方向非法（仅支持 asc / desc）
     */
    public static final String MSG_SORT_ORDER_INVALID = "排序方向非法，仅支持 asc / desc";

    // ===== 字典类型提示信息 =====

    /**
     * 字典类型标识不能为空
     */
    public static final String MSG_DICT_TYPE_CODE_REQUIRED = "字典类型标识不能为空";

    /**
     * 字典名称不能为空
     */
    public static final String MSG_DICT_TYPE_NAME_REQUIRED = "字典名称不能为空";

    /**
     * 字典类型 id 不能为空
     */
    public static final String MSG_DICT_TYPE_ID_REQUIRED = "字典类型 id 不能为空";

    /**
     * 字典类型不存在
     */
    public static final String MSG_DICT_TYPE_NOT_EXIST = "字典类型不存在";

    /**
     * 字典类型已存在（参数：dict_type）
     */
    public static final String MSG_DICT_TYPE_DUPLICATED = "字典类型已存在：%s";

    /**
     * 字典类型下存在字典数据，不能删除（参数：dict_type）
     */
    public static final String MSG_DICT_TYPE_HAS_DATA = "字典类型[%s]下存在字典数据，不能删除";

    // ===== 字典数据提示信息 =====

    /**
     * 字典数据 id 不能为空
     */
    public static final String MSG_DICT_DATA_ID_REQUIRED = "字典数据 id 不能为空";

    /**
     * 字典数据名称（dict_key）不能为空
     */
    public static final String MSG_DICT_KEY_REQUIRED = "字典数据名称不能为空";

    /**
     * 字典数据值（dict_value）不能为空
     */
    public static final String MSG_DICT_VALUE_REQUIRED = "字典数据值不能为空";

    /**
     * 字典数据不存在
     */
    public static final String MSG_DICT_DATA_NOT_EXIST = "字典数据不存在";

    /**
     * 字典数据已存在（参数：dict_type、dict_key）
     */
    public static final String MSG_DICT_DATA_DUPLICATED = "字典类型[%s]下字典数据[%s]已存在";
}
