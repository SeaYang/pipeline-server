package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class PipelineParameterUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 参数 id，必填 */
    private Long id;

    /** 参数名，必填，全局唯一 */
    private String name;

    /** 参数中文名称，必填 */
    private String label;

    /** 参数详细描述 */
    private String description;

    /** 前端组件类型，对应 ComponentTypeEnum */
    private String componentType;

    /** 参数类型，必填，对应 ParamTypeEnum */
    private String paramType;

    /** 是否必填，默认 false */
    private Boolean required;

    /** 默认值 */
    private String defaultValue;

    /** 是否需要系统内部处理，默认 false */
    private Boolean needSystemProcess;

    /** 正则校验表达式 */
    private String regexPattern;

    /** 依赖的参数，JSON 数组字符串 */
    private String dependParams;

    /** 参数值变动后是否刷新整体参数，默认 false */
    private Boolean refreshOnChanged;

    /** 参数所属组别，必填 */
    private String paramGroup;

    /** 参数在所属组别里的排序值，默认 0 */
    private Integer paramGroupSort;

    /** 选项配置，JSON 数组字符串 */
    private String optionConfig;

    /** 默认值计算策略配置，JSON 数组字符串 */
    private String defaultValueStrategyConfig;
}
