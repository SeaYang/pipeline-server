-- 分布式锁记录表
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
