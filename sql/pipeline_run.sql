-- =============================================================================
-- 流水线执行记录模块 DDL（pipeline_run）
-- =============================================================================
-- 说明：
-- 1. 表为逻辑删除（deleted：0-未删除，1-已删除），由 MyBatis-Plus 全局配置
--    （logic-delete-field: deleted / logic-delete-value: 1 / logic-not-delete-value: 0）接管，
--    BaseMapper / Wrapper 查询自动追加 deleted = 0，deleteById 自动改为 UPDATE deleted = 1。
-- 2. 对应 pipeline 的一次具体执行：执行流水线时触发 Argo Workflow，落地一条执行记录，
--    之后由平台异步轮询 Argo 状态并回写 status / duration / fail_type / fail_message 等字段。
-- 3. revision 为应用层乐观锁版本号：状态回写时带 WHERE revision = ? 条件更新，并发场景下
--    只有一条更新命中（revision 自增），未命中者视为冲突放弃本次回写。
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 流水线执行记录表（对应 pipeline 的一次具体执行）
-- ---------------------------------------------------------------------------
CREATE TABLE `pipeline_run` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `pipeline_id` bigint NOT NULL COMMENT '流水线id，对应pipeline的id',
  `name` varchar(200) NOT NULL COMMENT '流水线执行名称',
  `app_name` varchar(200) NOT NULL COMMENT '服务的appName，比如：pipeline-server',
  `pipeline_template_code` varchar(200) NOT NULL COMMENT '流水线模板编码，和pipeline_template的对应',
  `pipeline_template_version` varchar(30) NOT NULL COMMENT '执行流水线时的模板版本，对应pipeline_template_version的version',
  `status` varchar(45) NOT NULL COMMENT '流水线执行状态',
  `git_branch` varchar(200) DEFAULT NULL COMMENT '流水线执行时的git分支',
  `commit_id` varchar(200) DEFAULT NULL COMMENT '流水线执行时的git分支的commitId',
  `arguments` longtext NOT NULL COMMENT '流水线执行时的参数',
  `fail_type` varchar(200) DEFAULT NULL COMMENT '流水线执行失败的类型',
  `fail_message` longtext COMMENT '流水线执行失败的详细信息',
  `duration` int DEFAULT NULL COMMENT '执行时长（秒）',
  `revision` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  `creator` varchar(45) NOT NULL COMMENT '创建人',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_pipeline_id` (`pipeline_id`),
  KEY `idx_app_name` (`app_name`),
  KEY `idx_status_update_time` (`status`, `update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='流水线执行记录表，对应pipeline的一次具体执行';
