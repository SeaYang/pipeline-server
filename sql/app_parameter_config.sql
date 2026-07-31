-- 应用参数配置表
CREATE TABLE `app_parameter_config` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `app_name`       VARCHAR(200) NOT NULL COMMENT '应用名称',
    `parameter_name` VARCHAR(100) NOT NULL COMMENT '参数名（关联 pipeline_parameter.name）',
    `value`          VARCHAR(200) NOT NULL COMMENT '参数值',
    `env`            VARCHAR(20)  NOT NULL DEFAULT 'default' COMMENT '环境，default 表示默认环境',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除（0-未删除，1-已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app_param_env` (`app_name`, `parameter_name`, `env`, `deleted`),
    KEY `idx_app_env` (`app_name`, `env`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='应用参数配置';
