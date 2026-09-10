-- 流水线终态清理记录表
CREATE TABLE `pipeline_clean_run` (
    `id`             BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `run_name`       VARCHAR(200) NOT NULL COMMENT '业务流水线的 pipelineRunName（Argo Workflow 名称，唯一）',
    `clean_run_name` VARCHAR(200) DEFAULT NULL COMMENT '清理流水线的 pipelineRunName（提交成功后回填）',
    `status`         VARCHAR(45)  NOT NULL DEFAULT 'Running' COMMENT '状态，复用 PipelineRunStatusEnum：Running / Succeeded / Failed',
    `end_time`       DATETIME     DEFAULT NULL COMMENT '清理结束时间（回调/兜底回填）',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（即清理触发/开始时间）',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_run_name` (`run_name`),
    KEY `idx_status_create_time` (`status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线终态清理记录';

-- 兜底检查定时任务（由 CronJobScheduler 反射调用）
INSERT INTO `cron_job`
(`name`, `bean_name`, `method_name`, `method_params`, `cron_expr`, `enabled`, `misfire_policy`, `concurrent`)
VALUES
('流水线终态清理兜底检查', 'pipelineCleanGuardJob', 'execute', NULL, '0 */5 * * * ?', 1, 'skip', 0);
