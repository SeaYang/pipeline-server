-- =============================================================================
-- 事件触发模块 DDL（pipeline_event_bind）
-- =============================================================================
-- 说明：
-- 1. 表为逻辑删除（deleted：0-未删除，1-已删除），由 MyBatis-Plus 全局配置接管。
-- 2. 用途：记录"哪个应用的哪个事件实际绑定了哪条 pipeline"。
--    事件首次触发某应用时自动创建，后续触发直接复用。
-- 3. 同一个 app_name + event_type + pipeline_template_code 组合，在未删除记录中只允许存在一条（应用层先查再插保证）。
-- 4. 本表由系统自动维护，前端不直接操作。
-- =============================================================================

CREATE TABLE `pipeline_event_bind` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `pipeline_id` bigint NOT NULL COMMENT '关联的 pipeline.id',
  `event_type` varchar(100) NOT NULL COMMENT '事件类型，对应字典 pipeline-event-type 的 dict_key',
  `app_name` varchar(200) NOT NULL COMMENT '应用名称，对应 app_info.app_name',
  `pipeline_template_code` varchar(200) NOT NULL COMMENT '流水线模板编码',
  `creator` varchar(45) NOT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_app_event_template` (`app_name`, `event_type`, `pipeline_template_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='事件与pipeline实例的绑定关系表，事件首次触发时自动创建';
