-- 通用配置表
CREATE TABLE `generic_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_key` varchar(200) NOT NULL COMMENT '配置键，全局唯一（业务层校验）',
  `config_value` longtext DEFAULT NULL COMMENT '配置值，json格式时存序列化字符串',
  `value_format` varchar(20) NOT NULL DEFAULT 'txt' COMMENT '值格式：txt-纯文本 / json-JSON',
  `description` varchar(500) DEFAULT NULL COMMENT '备注说明',
  `creator` varchar(45) NOT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(45) DEFAULT NULL COMMENT '最后修改人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='通用配置表';

-- 通用配置变更历史表
CREATE TABLE `generic_config_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_id` bigint NOT NULL COMMENT '关联配置ID',
  `config_key` varchar(200) NOT NULL COMMENT '配置键快照',
  `action` varchar(20) NOT NULL COMMENT '操作类型：CREATE-新建 / UPDATE-修改 / DELETE-删除',
  `old_value` longtext DEFAULT NULL COMMENT '变更前值',
  `new_value` longtext DEFAULT NULL COMMENT '变更后值',
  `old_value_format` varchar(20) DEFAULT NULL COMMENT '变更前值格式',
  `new_value_format` varchar(20) DEFAULT NULL COMMENT '变更后值格式',
  `change_summary` varchar(500) DEFAULT NULL COMMENT '变更摘要，描述哪些字段发生了变化',
  `operator` varchar(45) NOT NULL COMMENT '操作人',
  `operate_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_config_id` (`config_id`),
  KEY `idx_operate_time` (`operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='通用配置变更历史表';
