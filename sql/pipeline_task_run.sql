-- =============================================================================
-- 流水线执行-任务节点记录模块 DDL（pipeline_task_run）
-- =============================================================================
-- 说明：
-- 1. 表为逻辑删除（deleted：0-未删除，1-已删除），由 MyBatis-Plus 全局配置接管。
-- 2. 对应 pipeline_run 的一个任务节点（Pod）的具体执行：仅当 pipeline_run 进入终态
--    （Succeeded / Cancelled）时，才由平台解析 Argo Workflow 的 status.nodes（type=Pod）
--    落地各节点记录；落地采用「按 pipeline_run_id 先删后插」保证幂等。
-- 3. log_content 取自该节点 Pod 的日志（kubernetes readNamespacedPodLog，按需截断）。
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 流水线执行-任务节点记录表（对应 pipeline_run 的一个任务节点的具体执行）
-- ---------------------------------------------------------------------------
CREATE TABLE `pipeline_task_run` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `pipeline_run_id` bigint NOT NULL COMMENT '流水线执行id，对应pipeline_run的id',
  `task_code` varchar(200) NOT NULL COMMENT '流水线任务节点编码，对应的是task_template的task_template_code',
  `status` varchar(45) NOT NULL COMMENT '执行状态',
  `inputs` longtext COMMENT '任务节点的执行入参json数组字符串，元素字段有name、value',
  `outputs` longtext COMMENT '任务节点的执行出参json数组字符串，元素字段有name、value',
  `pod_name` varchar(200) NOT NULL COMMENT '任务节点所在pod的名称',
  `log_content` longtext COMMENT '任务节点所在pod的日志',
  `start_time` datetime DEFAULT NULL COMMENT '开始执行时间',
  `end_time` datetime DEFAULT NULL COMMENT '执行结束时间',
  `duration` int DEFAULT NULL COMMENT '执行时长（秒）',
  `run_host_name` varchar(200) DEFAULT NULL COMMENT '任务pod执行时的k8s主机',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_pipeline_run_id` (`pipeline_run_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='流水线执行-任务节点记录表，对应pipeline_run的一个任务节点的具体执行';
