CREATE TABLE IF NOT EXISTS `indicator_calc_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '所属用户ID',

  `trading_pair_id` BIGINT NOT NULL COMMENT '交易对ID',
  `symbol` VARCHAR(64) NOT NULL COMMENT '交易对符号',
  `market_type` VARCHAR(16) NOT NULL DEFAULT 'SWAP' COMMENT '市场类型：SPOT/SWAP/FUTURES（冗余快照）',
  `timeframe` VARCHAR(16) NOT NULL COMMENT '周期',
  `bar_time` DATETIME NOT NULL COMMENT 'bar收盘时间（UTC）',

  `indicator_code` VARCHAR(64) NOT NULL COMMENT '指标编码',
  `indicator_version` VARCHAR(16) NOT NULL DEFAULT 'v1' COMMENT '版本',
  `calc_engine` VARCHAR(32) NOT NULL COMMENT '引擎：ta4j/custom',

  `status` VARCHAR(16) NOT NULL COMMENT '状态：SUCCESS/FAILED/SKIPPED',
  `cost_ms` INT DEFAULT NULL COMMENT '耗时毫秒',
  `error_msg` VARCHAR(512) DEFAULT NULL COMMENT '失败原因摘要',

  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

  PRIMARY KEY (`id`),
  KEY `idx_bar` (`user_id`,`trading_pair_id`,`timeframe`,`bar_time`),
  KEY `idx_status_time` (`user_id`,`status`,`created_at`),
  KEY `idx_ind_time` (`user_id`,`indicator_code`,`timeframe`,`bar_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指标计算日志表（审计与性能分析）';

