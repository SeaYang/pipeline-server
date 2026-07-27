package com.ci.pipeline.facade.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class GenericConfigHistoryResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long configId;

    private String configKey;

    private String action;

    /** 变更前值，json 格式时返回解析后的对象 */
    private Object oldValue;

    /** 变更后值，json 格式时返回解析后的对象 */
    private Object newValue;

    private String oldValueFormat;

    private String newValueFormat;

    private String changeSummary;

    private String operator;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date operateTime;
}
