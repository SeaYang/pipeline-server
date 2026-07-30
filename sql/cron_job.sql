-- 定时任务定义表
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
  KEY `idx_next_fire_time` (`enabled`, `next_fire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='定时任务定义表';

-- 定时任务执行日志表
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

-- 任务定义：流水线执行状态兜底同步（见 com.ci.pipeline.service.job.PipelineRunSyncGuardJob）
-- 每分钟扫描一次"运行中且更新时间超过陈旧阈值(默认60s)"的执行记录，触发兜底同步
INSERT INTO `cron_job`
(`name`, `bean_name`, `method_name`, `method_params`, `cron_expr`, `enabled`, `misfire_policy`, `concurrent`)
VALUES
('流水线执行状态兜底同步', 'pipelineRunSyncGuardJob', 'execute', NULL, '0 * * * * ?', 1, 'skip', 0);
