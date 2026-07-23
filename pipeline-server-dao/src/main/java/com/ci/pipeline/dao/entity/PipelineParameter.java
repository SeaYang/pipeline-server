package com.ci.pipeline.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 流水线参数定义实体。
 * <p>对应 pipeline_parameter 表，全局共享，规范化 Argo WorkflowTemplate 的参数管理。
 */
@Data
@TableName("pipeline_parameter")
public class PipelineParameter implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 参数名，全局唯一，对应 Argo yaml 中参数的 name 字段 */
    private String name;

    /** 参数中文名称，前端表单展示用 */
    private String label;

    /** 参数详细描述，可用于前端表单 tooltip 展示 */
    private String description;

    /** 前端组件类型（input / select / radio 等） */
    private String componentType;

    /** 参数类型：system-系统参数、user-用户参数 */
    private String paramType;

    /** 是否必填 */
    private Boolean required;

    /** 默认值，所有策略都未命中时兜底 */
    private String defaultValue;

    /** 是否需要系统内部处理（如值映射转换） */
    private Boolean needSystemProcess;

    /** 正则校验表达式 */
    private String regexPattern;

    /** 依赖的参数，JSON 数组格式，如 ["build-jdk-version"] */
    private String dependParams;

    /** 参数值变动后是否刷新整体参数 */
    private Boolean refreshOnChanged;

    /** 参数所属组别 */
    private String paramGroup;

    /** 参数在所属组别里的排序值 */
    private Integer paramGroupSort;

    /** 选项配置，JSON 数组格式，用于 select / radio 等场景 */
    private String optionConfig;

    /** 默认值计算策略配置，JSON 数组格式 */
    private String defaultValueStrategyConfig;

    /** 创建人 */
    private String creator;

    private Date createTime;
    private Date updateTime;

    /** 逻辑删除（0-未删除，1-已删除） */
    private Integer deleted;
}
