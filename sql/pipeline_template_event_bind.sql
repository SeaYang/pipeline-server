-- =============================================================================
-- 事件触发模块 DDL（pipeline_template_event_bind）
-- =============================================================================
-- 说明：
-- 1. 表为逻辑删除（deleted：0-未删除，1-已删除），由 MyBatis-Plus 全局配置接管。
-- 2. 用途：后台配置，定义"哪种事件可以触发哪些流水线模板"。
--    一个事件（event_type）可绑定多个模板（pipeline_template_code），适配不同编程语言。
-- 3. 唯一性约束（event_type + pipeline_template_code 在未删除记录中唯一）由应用层保证。
-- =============================================================================

CREATE TABLE `pipeline_template_event_bind` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_type` varchar(100) NOT NULL COMMENT '事件类型，对应字典 pipeline_event_type 的 dict_key',
  `pipeline_template_code` varchar(200) NOT NULL COMMENT '关联的流水线模板编码，对应 pipeline_template.pipeline_template_code',
  `creator` varchar(45) NOT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_event_type` (`event_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='事件与流水线模板的绑定关系表，后台配置，一个事件可绑定多个模板';
