package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class CronJobCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 任务名称 */
    private String name;

    /** 目标 Spring Bean 名称 */
    private String beanName;

    /** 目标方法名称 */
    private String methodName;

    /** 方法参数，JSON 数组字符串，如 ["daily", 500]，无参可不填 */
    private String methodParams;

    /** CRON 表达式（6 位：秒 分 时 日 月 周） */
    private String cronExpr;

    /** 是否启用：0-停用 1-启用，默认 1 */
    private Integer enabled;

    /** 错过执行策略，见 MisfirePolicyEnum，默认 fire_now */
    private String misfirePolicy;

    /** 是否允许并发执行：0-禁止 1-允许，默认 0 */
    private Integer concurrent;
}
