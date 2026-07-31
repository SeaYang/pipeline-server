package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 应用参数配置批量新增请求
 */
@Data
public class AppParameterConfigBatchCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String appName;
    private String env;
    private List<ConfigItem> items;

    @Data
    public static class ConfigItem implements Serializable {

        private static final long serialVersionUID = 1L;

        private String parameterName;
        private String value;
    }
}
