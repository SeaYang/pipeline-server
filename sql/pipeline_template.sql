-- =============================================================================
-- 流水线模板模块 DDL（pipeline_template + pipeline_template_version）
-- =============================================================================
-- 说明：
-- 1. 两张表均为逻辑删除（deleted：0-未删除，1-已删除），由 MyBatis-Plus 全局配置
--    （logic-delete-field: deleted / logic-delete-value: 1 / logic-not-delete-value: 0）接管，
--    BaseMapper / Wrapper 查询自动追加 deleted = 0，deleteById 自动改为 UPDATE deleted = 1。
-- 2. 唯一性约束由应用层保证（基于未删除记录）：
--    - pipeline_template.pipeline_template_code 在未删除记录中唯一；
--    - pipeline_template_version.(pipeline_template_code, version) 在未删除记录中唯一。
--    因此未在数据库层面添加 UNIQUE 索引，避免与逻辑删除产生冲突（删除后无法重建同名记录）。
-- 3. pipeline_template 仅保存流水线模板的基础元信息；模板详情（argo WorkflowTemplate 的 json
--    字符串，一个流水线模板由多个原子任务模板组成）跟随 pipeline_template_version 的每个版本存储。
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 流水线模板表（基础信息）
-- ---------------------------------------------------------------------------
CREATE TABLE `pipeline_template` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `pipeline_template_code` varchar(200) NOT NULL COMMENT '模板编码',
  `name` varchar(200) NOT NULL COMMENT '模板名称',
  `description` text COMMENT '模板详细描述',
  `pipeline_template_group` varchar(200) NOT NULL COMMENT '流水线模板所属分组，用于分类管理',
  `creator` varchar(45) NOT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_pipeline_template_code` (`pipeline_template_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='流水线模板的定义';

-- ---------------------------------------------------------------------------
-- 流水线模板版本表（版本详情：argo WorkflowTemplate json 字符串）
-- ---------------------------------------------------------------------------
CREATE TABLE `pipeline_template_version` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `pipeline_template_code` varchar(200) NOT NULL COMMENT '模板编码，和pipeline_template的对应',
  `version` varchar(30) NOT NULL COMMENT '模板版本号，比如：1.0.1',
  `status` varchar(45) NOT NULL COMMENT '模板状态',
  `template_detail` longtext NOT NULL COMMENT '流水线模板详情，对应argo WorkflowTemplate yml文件的json字符串，一个流水线模板由多个原子任务模板组成',
  `change_note` text COMMENT '版本变更说明',
  `creator` varchar(45) NOT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_pipeline_template_code` (`pipeline_template_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='流水线模板的版本管理表';
