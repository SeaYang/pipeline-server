package com.ci.pipeline.facade.response;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class CronJobLogResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long jobId;

    private String jobName;

    private String beanName;

    private String methodName;

    private String methodParams;

    private String status;

    private String message;

    private String instanceIp;

    private Date startTime;

    private Date endTime;

    private Long costMs;

    private Date createTime;
}
