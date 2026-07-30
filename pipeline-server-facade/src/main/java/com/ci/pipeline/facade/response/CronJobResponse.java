package com.ci.pipeline.facade.response;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class CronJobResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private String beanName;

    private String methodName;

    private String methodParams;

    private String cronExpr;

    private Integer enabled;

    private String misfirePolicy;

    private Integer concurrent;

    private Date nextFireTime;

    private Date lastFireTime;

    private Date createTime;

    private Date updateTime;
}
