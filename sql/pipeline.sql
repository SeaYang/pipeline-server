-- =============================================================================
-- 流水线实例模块 DDL（pipeline）
-- =============================================================================
-- 说明：
-- 1. 表为逻辑删除（deleted：0-未删除，1-已删除），由 MyBatis-Plus 全局配置
--    （logic-delete-field: deleted / logic-delete-value: 1 / logic-not-delete-value: 0）接管，
--    BaseMapper / Wrapper 查询自动追加 deleted = 0，deleteById 自动改为 UPDATE deleted = 1。
-- 2. 关联关系由应用层校验（基于未删除记录）：
--    - pipeline.app_name 需存在于 app_info；
--    - pipeline.pipeline_template_code 需存在于 pipeline_template。
--    因此未在数据库层面添加外键约束，避免与逻辑删除、独立部署产生冲突。
-- 3. pipeline 仅保存 appName 与流水线模板之间的关联关系（一个流水线模板可被多个 appName 使用，
--    一条流水线属于一个特定的 appName）；可执行的模板详情跟随
--    pipeline_template_version 的「生效中」版本存储。
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 流水线实例表（保存 appName 和流水线模板之间的关联关系）
-- ---------------------------------------------------------------------------
CREATE TABLE `pipeline` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(200) NOT NULL COMMENT '流水线名称',
  `app_name` varchar(200) NOT NULL COMMENT '服务的appName，比如：pipeline-server',
  `pipeline_template_code` varchar(200) NOT NULL COMMENT '流水线模板编码，和pipeline_template的对应',
  `creator` varchar(45) NOT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_app` (`app_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='流水线实例表，保存appName和流水线模板之间的关联关系，一个流水线模板可以被多个appName使用';
