-- =============================================================================
-- 任务模板模块 DDL（task_template + task_template_version）
-- =============================================================================
-- 说明：
-- 1. 两张表均为逻辑删除（deleted：0-未删除，1-已删除），由 MyBatis-Plus 全局配置
--    （logic-delete-field: deleted / logic-delete-value: 1 / logic-not-delete-value: 0）接管，
--    BaseMapper / Wrapper 查询自动追加 deleted = 0，deleteById 自动改为 UPDATE deleted = 1。
-- 2. 唯一性约束由应用层保证（基于未删除记录）：
--    - task_template.task_template_code 在未删除记录中唯一；
--    - task_template_version.(task_template_code, version) 在未删除记录中唯一。
--    因此未在数据库层面添加 UNIQUE 索引，避免与逻辑删除产生冲突（删除后无法重建同名记录）。
-- 3. task_template 仅保存任务模板的基础元信息；模板详情（argo WorkflowTemplate 的 json/yml
--    字符串）跟随 task_template_version 的每个版本存储。
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 任务模板表（基础信息）
-- ---------------------------------------------------------------------------
CREATE TABLE `task_template` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_template_code` varchar(200) NOT NULL COMMENT '任务模板编码',
  `name` varchar(200) NOT NULL COMMENT '任务模板名称',
  `description` text COMMENT '详细描述内容',
  `task_template_group` varchar(200) NOT NULL COMMENT '任务模板所属分组，用于分类展示',
  `creator` varchar(45) NOT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_task_template_code` (`task_template_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='流水线任务模板表，保存任务模板的基础字段定义，一个任务模板也对应了一个argo WorkflowTemplate';

-- ---------------------------------------------------------------------------
-- 任务模板版本表（版本详情：argo WorkflowTemplate json 字符串）
-- ---------------------------------------------------------------------------
CREATE TABLE `task_template_version` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_template_code` varchar(200) NOT NULL COMMENT '任务模板编码，和task_template的task_template_code对应',
  `version` varchar(30) NOT NULL COMMENT '任务版本号，比如：1.0.1',
  `status` varchar(45) NOT NULL COMMENT '任务版本状态：草稿、生效中、已失效',
  `template_detail` longtext NOT NULL COMMENT '任务模板详情，对应argo WorkflowTemplate yml文件的json字符串',
  `change_note` text COMMENT '版本变更说明',
  `creator` varchar(45) NOT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_task_template_code` (`task_template_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='流水线任务模板的版本表，管理任务模板的版本详情（argo workflow template json字符串）';
