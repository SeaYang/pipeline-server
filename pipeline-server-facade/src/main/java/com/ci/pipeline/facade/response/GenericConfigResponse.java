package com.ci.pipeline.facade.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class GenericConfigResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String configKey;

    /** 配置值，json 格式时返回解析后的对象 */
    private Object configValue;

    private String valueFormat;

    private String description;

    private String creator;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    private String updater;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
}
