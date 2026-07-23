-- =============================================================================
-- 字典模块 DDL（dict_type + dict_data）
-- =============================================================================
-- 说明：
-- 1. 两张表均为逻辑删除（deleted：0-未删除，1-已删除），由 MyBatis-Plus 全局配置
--    （logic-delete-field: deleted / logic-delete-value: 1 / logic-not-delete-value: 0）接管，
--    BaseMapper / Wrapper 查询自动追加 deleted = 0，deleteById 自动改为 UPDATE deleted = 1。
-- 2. 唯一性约束由应用层保证（基于未删除记录）：
--    - dict_type.dict_type 在未删除记录中唯一；
--    - dict_data.(dict_type, dict_key) 在未删除记录中唯一。
--    因此未在数据库层面添加 UNIQUE 索引，避免与逻辑删除产生冲突（删除后无法重建同名记录）。
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 字典类型表
-- ---------------------------------------------------------------------------
CREATE TABLE `dict_type` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `dict_type` varchar(100) NOT NULL COMMENT '字典类型',
  `dict_name` varchar(200) NOT NULL COMMENT '字典名称',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注信息',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='字典类型表';

-- ---------------------------------------------------------------------------
-- 字典数据表
-- ---------------------------------------------------------------------------
CREATE TABLE `dict_data` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `dict_type` varchar(100) NOT NULL COMMENT '字典类型',
  `dict_key` varchar(200) NOT NULL COMMENT '数据名称',
  `dict_value` varchar(200) NOT NULL COMMENT '数据值',
  `dict_sort` int NOT NULL COMMENT '排序值',
  `remark` varchar(1000) NOT NULL COMMENT '备注信息',
  `enabled` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典数据表';
