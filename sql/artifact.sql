-- =============================================================================
-- 制品管理模块 DDL（artifact）
-- =============================================================================
-- 说明：
-- 1. 表为逻辑删除（deleted：0-未删除，1-已删除），由 MyBatis-Plus 全局配置接管。
-- 2. 记录流水线构建产出的制品信息，包括原始制品（Raw）和镜像制品（Image）。
-- 3. 每次流水线执行，原始制品和镜像制品各回传一条记录，通过 pipeline_run_id 关联。
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 制品信息表
-- ---------------------------------------------------------------------------
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
  `sha256` varchar(65) DEFAULT NULL COMMENT '制品 sha256（镜像为 digest）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_app_name` (`app_name`),
  KEY `idx_pipeline_run_id` (`pipeline_run_id`),
  KEY `idx_pipeline_run_name` (`pipeline_run_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='制品信息表，记录流水线构建产出的制品信息';
