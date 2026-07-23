-- =============================================================================
-- 流水线执行详情快照模块 DDL（pipeline_run_snapshot）
-- =============================================================================
-- 说明：
-- 1. 表为逻辑删除（deleted：0-未删除，1-已删除），由 MyBatis-Plus 全局配置接管。
-- 2. detail 为从 Argo 查询得到的 Workflow 执行详情 JSON（结构同前端 go-cicd-workflow.json，
--    含 metadata/spec/status），用于执行过程中前端 vue-flow 的实时展示，以及执行结束后的回看。
-- 3. 与 pipeline_run 一对一（按 pipeline_run_id upsert）：pipeline_run 落地后紧接着插入首条快照；
--    异步状态同步中，phase 或 generation 变化时刷新快照。
-- 4. pipeline_run_id 列类型为 varchar(45)（与 DDL 给定一致），实际存储 pipeline_run 主键的字符串形式。
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 流水线执行详情快照表（执行过程中的临时存储，以及执行结束后的快照）
-- ---------------------------------------------------------------------------
CREATE TABLE `pipeline_run_snapshot` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `pipeline_run_id` varchar(45) NOT NULL COMMENT '流水线执行id',
  `detail` longtext NOT NULL COMMENT '流水线执行详情json字符串，从argo查询得到的',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_pipeline_run_id` (`pipeline_run_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流水线执行详情快照，用于执行过程中的临时存储，以及执行结束后的快照';
