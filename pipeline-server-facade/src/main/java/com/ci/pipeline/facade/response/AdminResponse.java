package com.ci.pipeline.facade.response;

import lombok.Data;

import java.io.Serializable;

/**
 * Admin 信息响应
 */
@Data
public class AdminResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String appId;
    private String status;
    private String version;
    private long timestamp;
}
