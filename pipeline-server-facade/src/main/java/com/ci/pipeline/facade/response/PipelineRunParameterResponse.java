package com.ci.pipeline.facade.response;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 执行弹框参数项响应，前端按此结构渲染参数表单。
 */
@Data
public class PipelineRunParameterResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String label;
    private String description;
    private String componentType;
    private String paramType;
    private Boolean required;
    private Boolean refreshOnChanged;
    private String regexPattern;
    private String paramGroup;
    private Integer paramGroupSort;

    /** 当前值（已计算） */
    private String value;

    /** 过滤后的可见选项（select / radio 用） */
    private List<OptionItem> options;

    /** 是否隐藏（条件不满足时） */
    private Boolean hidden;

    /**
     * 选项项。
     */
    @Data
    public static class OptionItem implements Serializable {

        private static final long serialVersionUID = 1L;

        private String value;
        private String label;
        private Boolean asDefault;
        /** 选项显示条件，为 null 表示无条件显示；非 null 时需所有条件匹配才显示 */
        private List<OptionDepend> parameterDepends;
    }

    /**
     * 选项显示条件项。
     */
    @Data
    public static class OptionDepend implements Serializable {

        private static final long serialVersionUID = 1L;

        /** 依赖的参数名 */
        private String name;
        /** 依赖参数需等于该值时，本选项才显示 */
        private String value;
    }
}
