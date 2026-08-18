-- ============================================================
-- 流水线并发控制（三层：L1 全局 / L2 应用×模板 / L3 流水线）
-- 设计文档：docs/techdesign/pipeline-concurrency-control-design.md
-- ============================================================

-- 1. pipeline_template 新增：应用维度并发上限 + 超限策略
ALTER TABLE `pipeline_template`
  ADD COLUMN `app_max_running_limit` int NOT NULL DEFAULT 1
    COMMENT '应用维度最大并发执行数：同一 appName 使用本模板的未完成执行数上限（统计 Pending/Running/Unknown），默认1即不允许并发' AFTER `cluster_schedule_policy`,
  ADD COLUMN `over_limit_policy` varchar(45) NOT NULL DEFAULT 'Reject'
    COMMENT '超限策略：Reject-拒绝新执行 / ReplaceOldest-终止最早执行腾位' AFTER `app_max_running_limit`;

-- 2. pipeline 新增：流水线维度并发上限 + 超限策略（NULL = 未配置，fallback 到模板）
ALTER TABLE `pipeline`
  ADD COLUMN `max_running_limit` int DEFAULT NULL
    COMMENT '本流水线最大并发执行数；NULL 表示未配置，fallback 到模板的 app_max_running_limit；配置值超过模板值时按模板值生效（clamp）' AFTER `pipeline_template_code`,
  ADD COLUMN `over_limit_policy` varchar(45) DEFAULT NULL
    COMMENT '超限策略：Reject / ReplaceOldest；NULL 表示未配置，fallback 到模板的 over_limit_policy' AFTER `max_running_limit`;

-- 3. 通用配置：全平台最大并发执行数（L1 全局限流）
INSERT INTO `generic_config` (`config_key`, `config_value`, `value_format`, `description`, `creator`)
VALUES ('pipeline-max-running-limit', '1000', 'txt',
        '全平台最大并发执行数（限流）：全平台 Pending/Running/Unknown 状态的流水线执行总数达到该值时，拒绝新的执行提交',
        'admin');
