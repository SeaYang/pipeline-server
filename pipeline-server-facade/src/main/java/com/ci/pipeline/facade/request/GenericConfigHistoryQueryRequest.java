package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class GenericConfigHistoryQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 配置键，可选，模糊过滤 */
    private String configKey;

    /** 操作类型，可选：CREATE / UPDATE / DELETE */
    private String action;

    /** 操作人，可选，模糊过滤 */
    private String operator;

    /** 页码（从 1 开始） */
    private Long pageNum;

    /** 每页大小 */
    private Long pageSize;
}
