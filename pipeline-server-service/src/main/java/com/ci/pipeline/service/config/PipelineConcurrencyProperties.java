package com.ci.pipeline.service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 流水线并发控制配置属性（yml 兜底默认值）。
 * <p>读取优先级：generic_config 表（pipeline-max-running-limit） &gt; 此处 yml 默认值。
 */
@Data
@Component
@ConfigurationProperties(prefix = "pipeline.concurrency")
public class PipelineConcurrencyProperties {

    /**
     * 全局最大运行数兜底默认值（generic_config 中 pipeline-max-running-limit 未配置或非法时生效）
     */
    private int maxRunningLimit = 1000;
}
