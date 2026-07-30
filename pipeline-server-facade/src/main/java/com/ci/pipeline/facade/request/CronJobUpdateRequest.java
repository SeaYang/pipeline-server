package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class CronJobUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private String beanName;

    private String methodName;

    /** 方法参数，JSON 数组字符串；无参传 null 或空字符串 */
    private String methodParams;

    private String cronExpr;

    private String misfirePolicy;

    private Integer concurrent;
}
