-- =============================================================================
-- 应用基础信息模块 DDL（app_info）
-- =============================================================================
-- 说明：
-- 1. 表为逻辑删除（deleted：0-未删除，1-已删除），由 MyBatis-Plus 全局配置
--    （logic-delete-field: deleted / logic-delete-value: 1 / logic-not-delete-value: 0）接管，
--    BaseMapper / Wrapper 查询自动追加 deleted = 0，deleteById 自动改为 UPDATE deleted = 1。
-- 2. app_name 的唯一性约束由应用层保证（基于未删除记录）：app_info.app_name 在未删除记录中唯一。
--    因此未在数据库层面添加 UNIQUE 索引，避免与逻辑删除产生冲突（删除后无法重建同名应用）。
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 应用基础信息表
-- ---------------------------------------------------------------------------
CREATE TABLE `app_info` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `app_name` varchar(200) NOT NULL COMMENT '应用名称，比如：go-web-demo',
  `programming_language` varchar(50) NOT NULL COMMENT '所使用的编程语言或平台',
  `description` varchar(500) DEFAULT NULL COMMENT '应用描述，比如是干嘛的，什么领域',
  `git_ssh_url` varchar(500) NOT NULL COMMENT 'git仓库地址，ssh格式',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_app_name` (`app_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='应用基础信息表';
