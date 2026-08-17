-- =============================================================================
-- 多集群调度模块 DDL（cluster_info + pipeline_template/pipeline_run 加列）
-- =============================================================================
-- 说明：
-- 1. cluster_info 为执行集群定义表，每个集群对应一套 K8s + Argo Workflows；
--    配置运行时热生效（读取侧带内容指纹缓存，配置变更后客户端自动重建）。
-- 2. enabled 与 online 语义分离：
--    - enabled=0 彻底下线：不参与调度、模板发布/删除不再同步到它；
--    - online=0 临时摘流（维修窗口）：不参与调度，但模板同步照常，恢复上线立即可用。
-- 3. is_default 全局唯一（应用层事务内"先清后设"保证），用于存量 pipeline_run.cluster_name
--    为空时的路由兜底。
-- 4. pipeline_template 新增 cluster_names（逗号分隔候选集群）+ cluster_schedule_policy
--    （Any / PreferSelected）；pipeline_run 新增 cluster_name（提交时选定的集群）。
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 执行集群定义表
-- ---------------------------------------------------------------------------
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

-- ---------------------------------------------------------------------------
-- 流水线模板表加列（候选集群 + 调度策略）
-- ---------------------------------------------------------------------------
ALTER TABLE `pipeline_template`
  ADD COLUMN `cluster_names` varchar(500) DEFAULT NULL
    COMMENT '候选执行集群，逗号分隔多个 clusterName；NULL/空 表示不限制集群',
  ADD COLUMN `cluster_schedule_policy` varchar(45) NOT NULL DEFAULT 'Any'
    COMMENT '集群调度策略：Any-任意集群 / PreferSelected-优先选中集群';

-- ---------------------------------------------------------------------------
-- 流水线执行记录表加列（执行集群标识）
-- ---------------------------------------------------------------------------
ALTER TABLE `pipeline_run`
  ADD COLUMN `cluster_name` varchar(100) DEFAULT NULL
    COMMENT '执行集群标识（提交时选定的集群），日志/同步/重试/停止按此路由；存量为空时兜底默认集群',
  ADD KEY `idx_cluster_name` (`cluster_name`);

-- ---------------------------------------------------------------------------
-- 存量集群种子数据（token 由运维替换为真实值，即原 application-local.yml 中的配置）
-- ---------------------------------------------------------------------------
INSERT INTO `cluster_info`
  (`cluster_name`, `description`, `argo_url`, `argo_token`, `argo_namespace`,
   `k8s_master_url`, `k8s_token`, `k8s_verifying_ssl`,
   `connect_timeout_ms`, `read_timeout_ms`, `free_memory_threshold`,
   `enabled`, `online`, `is_default`, `creator`)
VALUES
  ('cluster-a', '默认集群（原 192.168.10.130）',
   'https://192.168.10.130:2746', 'Bearer REPLACE_ME', 'argo',
   'https://192.168.10.130:6443', 'REPLACE_ME', 0,
   5000, 10000, 0.20,
   1, 1, 1, 'admin');
