CREATE TABLE IF NOT EXISTS `indicator_subscription` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '所属用户ID',

  `trading_pair_id` BIGINT NOT NULL COMMENT '交易对ID',
  `symbol` VARCHAR(64) NOT NULL COMMENT '交易对符号（冗余快照，便于查错）',
  `market_type` VARCHAR(16) NOT NULL DEFAULT 'SWAP' COMMENT '市场类型：SPOT/SWAP/FUTURES（冗余快照）',
  `timeframe` VARCHAR(16) NOT NULL COMMENT '周期：1m/5m/15m/30m/1h/4h...',

  `indicator_code` VARCHAR(64) NOT NULL COMMENT '指标编码（可含参数Key，如 RSI_14 或 RSI）',
  `indicator_version` VARCHAR(16) NOT NULL DEFAULT 'v1' COMMENT '指标版本',
  `params` JSON DEFAULT NULL COMMENT '指标参数快照（如 {"period":14}）',

  `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sub` (`user_id`,`trading_pair_id`,`timeframe`,`indicator_code`,`indicator_version`),
  KEY `idx_pair_tf` (`user_id`,`trading_pair_id`,`timeframe`),
  KEY `idx_ind` (`user_id`,`indicator_code`,`timeframe`),
  KEY `idx_enabled` (`user_id`,`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指标订阅表（决定哪些交易对/周期需要计算哪些指标）';

