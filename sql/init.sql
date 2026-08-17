-- =============================================================================
-- pipeline 数据库初始化脚本
-- =============================================================================
-- 使用方式：
--   方式一（命令行导入）：
--     mysql -u root -p pipeline < sql/init.sql
--
--   方式二（MySQL 客户端内执行）：
--     USE pipeline;
--     SOURCE /path/to/sql/init.sql;
--
-- 说明：
--   本文件聚合了所有建表 DDL，从 MySQL 导出，用户只需执行本文件即可完成库表初始化。
--   如需查看单表定义，请参考 sql/ 目录下的各独立 .sql 文件。
--   文件末尾附带 cluster_info 存量集群种子数据（token 需替换为真实值）。
-- =============================================================================
-- MySQL dump 10.13  Distrib 8.0.46, for macos15 (arm64)
--
-- Host: localhost    Database: pipeline
-- ------------------------------------------------------
-- Server version	8.4.9

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `app_info`
--

DROP TABLE IF EXISTS `app_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `app_info` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `app_name` varchar(200) NOT NULL COMMENT '应用名称，比如：go-web-demo',
  `programming_language` varchar(50) NOT NULL COMMENT '所使用的编程语言或平台',
  `description` varchar(500) DEFAULT NULL COMMENT '应用描述，比如是干嘛的，什么领域',
  `git_ssh_url` varchar(500) NOT NULL COMMENT 'git仓库地址，ssh格式',
  `repo_id` bigint DEFAULT NULL COMMENT 'GitLab仓库ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='应用基础信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `app_parameter_config`
--

DROP TABLE IF EXISTS `app_parameter_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `app_parameter_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `app_name` varchar(200) NOT NULL COMMENT '应用名称',
  `parameter_name` varchar(100) NOT NULL COMMENT '参数名（关联 pipeline_parameter.name）',
  `value` varchar(200) NOT NULL COMMENT '参数值',
  `env` varchar(20) NOT NULL DEFAULT 'default' COMMENT '环境，default 表示默认环境',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除（0-未删除，1-已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_param_env` (`app_name`,`parameter_name`,`env`,`deleted`),
  KEY `idx_app_env` (`app_name`,`env`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='应用参数配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `artifact`
--

DROP TABLE IF EXISTS `artifact`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `artifact` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `app_name` varchar(200) NOT NULL COMMENT '应用名称，比如：go-web-demo',
  `name` varchar(255) NOT NULL COMMENT '制品名称，原始制品为二进制名，镜像制品为 repo 路径',
  `type` varchar(20) NOT NULL COMMENT '制品类型：RAW-原始制品，IMAGE-镜像制品',
  `git_branch` varchar(255) DEFAULT NULL COMMENT '构建时的 git 分支',
  `commit_id` varchar(64) DEFAULT NULL COMMENT '构建时的 commit id',
  `env` varchar(30) DEFAULT NULL COMMENT '环境标识（dev/test/prod）',
  `build_time` datetime DEFAULT NULL COMMENT '构建时间',
  `build_user` varchar(255) DEFAULT NULL COMMENT '构建人（触发流水线的用户）',
  `pipeline_run_id` bigint DEFAULT NULL COMMENT '流水线运行ID，对应 pipeline_run.id',
  `pipeline_run_name` varchar(200) DEFAULT NULL COMMENT '流水线运行名称，对应 pipeline_run.name（即 Argo Workflow name），用于跳转流水线详情',
  `artifact_repository` varchar(255) DEFAULT NULL COMMENT '制品仓库名，如 raw-go / go-web-demo',
  `artifact_repository_path` varchar(512) DEFAULT NULL COMMENT '仓库内相对路径',
  `artifact_url` varchar(1024) DEFAULT NULL COMMENT '制品完整地址，镜像可用于 docker pull，原始制品可用于下载',
  `size` bigint DEFAULT NULL COMMENT '制品大小（字节）',
  `sha256` varchar(100) DEFAULT NULL COMMENT '制品 sha256（镜像为 digest）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_app_name` (`app_name`),
  KEY `idx_pipeline_run_id` (`pipeline_run_id`),
  KEY `idx_pipeline_run_name` (`pipeline_run_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='制品信息表，记录流水线构建产出的制品信息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cron_job`
--

DROP TABLE IF EXISTS `cron_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cron_job` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(100) NOT NULL COMMENT '任务名称',
  `bean_name` varchar(200) NOT NULL COMMENT '目标 Spring Bean 名称',
  `method_name` varchar(100) NOT NULL COMMENT '目标方法名称',
  `method_params` varchar(500) DEFAULT NULL COMMENT '方法参数，JSON数组字符串，如["daily",500]，无参为NULL',
  `cron_expr` varchar(128) NOT NULL COMMENT 'CRON表达式（6位：秒 分 时 日 月 周）',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用：0-停用 1-启用',
  `misfire_policy` varchar(20) NOT NULL DEFAULT 'fire_now' COMMENT '错过执行策略：fire_now-立即执行 / fire_once-仅补偿一次 / skip-跳过',
  `concurrent` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否允许并发执行：0-禁止 1-允许',
  `next_fire_time` datetime DEFAULT NULL COMMENT '下一次触发时间，停用后为NULL',
  `last_fire_time` datetime DEFAULT NULL COMMENT '上一次触发时间',
  `revision` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号，用于多实例抢占调度',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_next_fire_time` (`enabled`,`next_fire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='定时任务定义表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cron_job_log`
--

DROP TABLE IF EXISTS `cron_job_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cron_job_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `job_id` bigint NOT NULL COMMENT '关联 cron_job.id',
  `job_name` varchar(100) NOT NULL COMMENT '任务名称快照',
  `bean_name` varchar(200) NOT NULL COMMENT 'Bean名称快照',
  `method_name` varchar(100) NOT NULL COMMENT '方法名称快照',
  `method_params` varchar(500) DEFAULT NULL COMMENT '方法参数快照',
  `status` varchar(20) NOT NULL COMMENT '执行状态：running-执行中 / succeeded-成功 / failed-失败',
  `message` varchar(2000) DEFAULT NULL COMMENT '结果信息：失败异常堆栈 / 停止原因等',
  `instance_ip` varchar(64) DEFAULT NULL COMMENT '执行实例IP，用于多实例部署下跨实例路由"停止任务"请求',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间，执行中为NULL',
  `cost_ms` bigint DEFAULT NULL COMMENT '执行耗时（毫秒），执行中为NULL',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_job_id` (`job_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='定时任务执行日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dict_data`
--

DROP TABLE IF EXISTS `dict_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='字典数据表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dict_type`
--

DROP TABLE IF EXISTS `dict_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `distributed_lock`
--

DROP TABLE IF EXISTS `distributed_lock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `distributed_lock` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `lock_key` varchar(128) NOT NULL COMMENT '锁唯一标识，业务语义化命名',
  `lock_value` varchar(64) NOT NULL COMMENT '持有者标识（UUID），用于校验锁的归属，防止误删',
  `description` varchar(256) DEFAULT NULL COMMENT '锁描述信息，方便排查',
  `expired_time` datetime NOT NULL COMMENT '锁过期时间，超过此时间视为已释放',
  `revision` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号，每次更新+1',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_lock_key` (`lock_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='分布式锁记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `generic_config`
--

DROP TABLE IF EXISTS `generic_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `generic_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_key` varchar(200) NOT NULL COMMENT '配置键，全局唯一（业务层校验）',
  `config_value` longtext COMMENT '配置值，json格式时存序列化字符串',
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `generic_config_history`
--

DROP TABLE IF EXISTS `generic_config_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `generic_config_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_id` bigint NOT NULL COMMENT '关联配置ID',
  `config_key` varchar(200) NOT NULL COMMENT '配置键快照',
  `action` varchar(20) NOT NULL COMMENT '操作类型：CREATE-新建 / UPDATE-修改 / DELETE-删除',
  `old_value` longtext COMMENT '变更前值',
  `new_value` longtext COMMENT '变更后值',
  `old_value_format` varchar(20) DEFAULT NULL COMMENT '变更前值格式',
  `new_value_format` varchar(20) DEFAULT NULL COMMENT '变更后值格式',
  `change_summary` varchar(500) DEFAULT NULL COMMENT '变更摘要，描述哪些字段发生了变化',
  `operator` varchar(45) NOT NULL COMMENT '操作人',
  `operate_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_config_id` (`config_id`),
  KEY `idx_operate_time` (`operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='通用配置变更历史表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pipeline`
--

DROP TABLE IF EXISTS `pipeline`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pipeline_event_bind`
--

DROP TABLE IF EXISTS `pipeline_event_bind`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
  KEY `idx_app_event_template` (`app_name`,`event_type`,`pipeline_template_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='事件与pipeline实例的绑定关系表，事件首次触发时自动创建';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pipeline_parameter`
--

DROP TABLE IF EXISTS `pipeline_parameter`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pipeline_parameter` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL COMMENT '参数名，全局唯一，对应argo yaml中参数的name字段，例如：app-name、env等等',
  `label` varchar(100) NOT NULL COMMENT '参数中文名称，前端表单展示的时候用，用于表单的label值',
  `description` varchar(500) DEFAULT NULL COMMENT '参数详细描述，可以用于前端表单tooltip展示',
  `component_type` varchar(50) DEFAULT NULL COMMENT '前端组件类型，参数的展示方式，比如：输入框、下拉框、单选框等等',
  `param_type` varchar(50) NOT NULL COMMENT '参数类型，有些参数不需要展示给用户或者无需用户填写，系统内部自动处理；有些参数需要用户填写；因此可以分为系统参数和用户参数，分别为：system、user',
  `required` tinyint(1) NOT NULL DEFAULT '0' COMMENT '参数是否必填',
  `default_value` varchar(200) DEFAULT NULL COMMENT '参数的默认值，如果其它所有的数据源都获取不到默认值，则使用该默认值',
  `need_system_process` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否需要系统内部处理，有些场景下，用户/外部传入的参数需要经过系统额外处理',
  `regex_pattern` varchar(100) DEFAULT NULL COMMENT '参数需要满足的正则表达式，定义了对参数的校验规则',
  `depend_params` varchar(500) DEFAULT NULL COMMENT '依赖的参数，字符串数组格式；参数与参数之间可能有前后依赖关系，比如说java maven流水线中有jdk版本、maven版本这两个参数，显然maven版本参数依赖jdk版本参数，需要根据不同的jdk版本去计算对应的maven版本',
  `refresh_on_changed` tinyint(1) NOT NULL DEFAULT '0' COMMENT '参数值变动后是否刷新整体的参数，一般是参数被其它参数依赖的时候，当参数值变动了，依赖该参数的其它参数也要重新计算',
  `param_group` varchar(50) NOT NULL COMMENT '参数所属的组别，用于分类展示参数',
  `param_group_sort` int NOT NULL DEFAULT '0' COMMENT '参数在所属组别里面的排序值，定义了展示顺序',
  `option_config` text COMMENT '参数选项配置，一般用于下拉选择、单选按钮等场景，比如env选择',
  `default_value_strategy_config` text COMMENT '默认值计算策略配置，比如默认从app配置读取默认值，比如默认从最近一次执行成功记录读取默认值',
  `creator` varchar(45) NOT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_param_code` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='流水线参数定义表，argo workflow template的yaml在平台上管理，yaml本身可以有各种输入参数，为了规范化参数管理，这些参数需要在平台上定义好';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pipeline_run`
--

DROP TABLE IF EXISTS `pipeline_run`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pipeline_run` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `pipeline_id` bigint NOT NULL COMMENT '流水线id，对应pipeline的id',
  `name` varchar(200) NOT NULL COMMENT '流水线执行名称',
  `cluster_name` varchar(100) DEFAULT NULL COMMENT '执行集群标识（提交时选定的集群），日志/同步/重试/停止按此路由；存量为空时兜底默认集群',
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
  KEY `idx_name` (`name`),
  KEY `idx_cluster_name` (`cluster_name`),
  KEY `idx_status_update_time` (`status`,`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='流水线执行记录表，对应pipeline的一次具体执行';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pipeline_run_snapshot`
--

DROP TABLE IF EXISTS `pipeline_run_snapshot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pipeline_run_snapshot` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `pipeline_run_id` varchar(45) NOT NULL COMMENT '流水线执行id',
  `detail` longtext NOT NULL COMMENT '流水线执行详情json字符串，从argo查询得到的',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_pipeline_run_id` (`pipeline_run_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流水线执行详情快照，用于执行过程中的临时存储，以及执行结束后的快照';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pipeline_task_run`
--

DROP TABLE IF EXISTS `pipeline_task_run`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pipeline_task_run` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `pipeline_run_id` bigint NOT NULL COMMENT '流水线执行id，对应pipeline_run的id',
  `task_code` varchar(200) NOT NULL COMMENT '流水线任务节点编码，对应的是task_template的task_template_code',
  `status` varchar(45) NOT NULL COMMENT '执行状态',
  `inputs` longtext COMMENT '任务节点的执行入参json数组字符串，元素字段有name、value',
  `outputs` longtext COMMENT '任务节点的执行出参json数组字符串，元素字段有name、value',
  `pod_name` varchar(200) NOT NULL COMMENT '流水线执行时的参数',
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pipeline_template`
--

DROP TABLE IF EXISTS `pipeline_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pipeline_template` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `pipeline_template_code` varchar(200) NOT NULL COMMENT '模板编码',
  `name` varchar(200) NOT NULL COMMENT '模板名称',
  `description` text COMMENT '模板详细描述',
  `pipeline_template_group` varchar(200) NOT NULL COMMENT '流水线模板所属分组，用于分类管理',
  `cluster_names` varchar(500) DEFAULT NULL COMMENT '候选执行集群，逗号分隔多个 clusterName；NULL/空 表示不限制集群',
  `cluster_schedule_policy` varchar(45) NOT NULL DEFAULT 'Any' COMMENT '集群调度策略：Any-任意集群 / PreferSelected-优先选中集群',
  `creator` varchar(45) NOT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='流水线模板的定义';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pipeline_template_event_bind`
--

DROP TABLE IF EXISTS `pipeline_template_event_bind`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pipeline_template_event_bind` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_type` varchar(100) NOT NULL COMMENT '事件类型，对应字典 pipeline-event-type 的 dict_key',
  `pipeline_template_code` varchar(200) NOT NULL COMMENT '关联的流水线模板编码，对应 pipeline_template.pipeline_template_code',
  `creator` varchar(45) NOT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_event_type` (`event_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='事件与流水线模板的绑定关系表，后台配置，一个事件可绑定多个模板';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pipeline_template_version`
--

DROP TABLE IF EXISTS `pipeline_template_version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='流水线模板的版本管理表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pipeline_trigger_history`
--

DROP TABLE IF EXISTS `pipeline_trigger_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pipeline_trigger_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `app_name` varchar(200) NOT NULL COMMENT '应用名称，对应 app_info.app_name',
  `pipeline_id` bigint NOT NULL COMMENT '流水线id，对应 pipeline.id',
  `pipeline_run_id` bigint DEFAULT NULL COMMENT '流水线执行记录id，对应 pipeline_run.id；触发失败时为 NULL',
  `pipeline_event_bind_id` bigint NOT NULL DEFAULT '0' COMMENT '事件绑定记录id，对应 pipeline_event_bind.id；手动触发固定为 0',
  `status` varchar(30) NOT NULL COMMENT '触发状态：SUCCESS-成功，FAILED-失败',
  `type` varchar(100) NOT NULL COMMENT '触发类型：手动触发为 user，事件触发为对应的事件类型 eventType',
  `creator` varchar(45) NOT NULL COMMENT '触发人；手动触发取登录用户，事件触发优先取 operator 参数，无则取 eventType',
  `request_body` longtext COMMENT '触发请求的请求体（JSON 字符串）',
  `error_message` text COMMENT '触发失败时的错误信息',
  `pipeline_template_code` varchar(200) NOT NULL COMMENT '触发的流水线模板编码',
  `pipeline_template_version` varchar(30) DEFAULT NULL COMMENT '触发的流水线模板版本；触发失败时可能为 NULL',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_pipeline_id` (`pipeline_id`),
  KEY `idx_app_name` (`app_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='流水线触发历史记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `task_template`
--

DROP TABLE IF EXISTS `task_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='流水线任务模板表，保存任务模板的基础字段定义，一个任务模板也对应了一个argo WorkflowTemplate';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `task_template_version`
--

DROP TABLE IF EXISTS `task_template_version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='流水线任务模板的版本表，管理任务模板的版本详情（argo workflow template json字符串）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cluster_info`
--

DROP TABLE IF EXISTS `cluster_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cluster_info` (
  `id`                     bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `cluster_name`           varchar(100) NOT NULL COMMENT '集群唯一标识，小写字母数字中划线',
  `description`            varchar(500) DEFAULT NULL COMMENT '集群描述',
  `argo_url`               varchar(500) NOT NULL COMMENT 'Argo Server 地址',
  `argo_token`             varchar(2000) NOT NULL COMMENT 'Argo 认证 token（含 Bearer 前缀）',
  `argo_namespace`         varchar(100) NOT NULL DEFAULT 'argo' COMMENT 'Workflow/WorkflowTemplate 所在命名空间',
  `k8s_master_url`         varchar(500) NOT NULL COMMENT 'K8s API Server 地址',
  `k8s_token`              varchar(2000) NOT NULL COMMENT 'K8s 认证 token（不含 Bearer 前缀）',
  `k8s_verifying_ssl`      tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否校验 K8s 证书：0-不校验 1-校验',
  `connect_timeout_ms`     int NOT NULL DEFAULT 5000 COMMENT '连接超时（毫秒）',
  `read_timeout_ms`        int NOT NULL DEFAULT 10000 COMMENT '读取超时（毫秒）',
  `free_memory_threshold`  decimal(4,2) NOT NULL DEFAULT 0.20 COMMENT '调度准入水位：平均空闲内存占比低于该值不参与调度',
  `max_running_workflows`  int DEFAULT NULL COMMENT '运行中 Workflow 数硬上限，NULL 不启用',
  `enabled`                tinyint(1) NOT NULL DEFAULT 1 COMMENT '集群生命周期：1-启用 0-下线（不调度、不同步模板）',
  `online`                 tinyint(1) NOT NULL DEFAULT 1 COMMENT '调度摘流开关：0-摘流（不调度但模板继续同步）',
  `is_default`             tinyint(1) NOT NULL DEFAULT 0 COMMENT '默认集群标记，全局唯一；存量 run 路由兜底',
  `revision`               int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `creator`                varchar(45) NOT NULL COMMENT '创建人',
  `create_time`            datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`                varchar(45) DEFAULT NULL COMMENT '最后修改人',
  `update_time`            datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`                tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_cluster_name` (`cluster_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='执行集群定义表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- 种子数据：存量集群（token 由运维替换为真实值，即原 application-local.yml 中的配置）
--

INSERT INTO `cluster_info`
  (`cluster_name`, `description`, `argo_url`, `argo_token`, `argo_namespace`,
   `k8s_master_url`, `k8s_token`, `k8s_verifying_ssl`,
   `connect_timeout_ms`, `read_timeout_ms`, `free_memory_threshold`,
   `enabled`, `online`, `is_default`, `creator`)
VALUES
  ('default', '默认集群（原 192.168.10.130）',
   'https://192.168.10.130:2746', 'Bearer REPLACE_ME', 'argo',
   'https://192.168.10.130:6443', 'REPLACE_ME', 0,
   5000, 10000, 0.20,
   1, 1, 1, 'admin');

/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-08-12 12:13:45

-- 通用配置kv数据初始化
INSERT INTO `generic_config` (`config_key`, `config_value`, `value_format`, `description`, `creator`)
VALUES ('gitlab.api.token','你的token','txt','访问gitlab.com的api所需要的token。','admin')
,('gitlab.api.url','https://gitlab.com','txt','','admin');

-- 字典数据初始化
INSERT INTO `dict_type` (`dict_type`, `dict_name`, `remark`) VALUES
  ('task-template-group', '任务模板分组', ''),
  ('programming-language', '语言平台', ''),
  ('pipeline-parameter-group', '流水线参数分组', ''),
  ('pipeline-event-type', '流水线触发事件类型', '');

INSERT INTO `dict_data` (`dict_type`, `dict_key`, `dict_value`, `dict_sort`, `remark`, `enabled`) VALUES
  ('task-template-group', 'code', '代码', 0, '', 1),
  ('task-template-group', 'build', '构建', 0, '', 1),
  ('programming-language', 'go', 'go', 0, '', 1),
  ('task-template-group', 'image', '镜像', 0, '', 1),
  ('task-template-group', 'artifact', '制品', 0, '', 1),
  ('task-template-group', 'deploy', '部署', 0, '', 1),
  ('task-template-group', 'for-test', '自测', 0, '', 1),
  ('pipeline-parameter-group', 'default', '默认分组', 0, '', 1),
  ('pipeline-parameter-group', 'build', '构建', 5, '', 1),
  ('pipeline-parameter-group', 'deploy', '部署', 4, '', 1);

