CREATE TABLE IF NOT EXISTS `indicator_value` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '所属用户ID（多租户隔离）',

  `trading_pair_id` BIGINT NOT NULL COMMENT '交易对ID',
  `symbol` VARCHAR(64) NOT NULL COMMENT '交易对符号（冗余快照，便于排查）',
  `market_type` VARCHAR(16) NOT NULL DEFAULT 'SWAP' COMMENT '市场类型：SPOT/SWAP/FUTURES（冗余快照）',

  `timeframe` VARCHAR(16) NOT NULL COMMENT 'K线周期：1m/5m/15m/30m/1h/4h...',
  `bar_time` DATETIME NOT NULL COMMENT 'K线收盘时间（UTC，bar_close_time语义）',

  `indicator_code` VARCHAR(64) NOT NULL COMMENT '指标编码：RSI/MACD/BOLL/MA 等',
  `indicator_version` VARCHAR(16) NOT NULL DEFAULT 'v1' COMMENT '指标版本：v1（v1.2.1固定）',

  `value` DECIMAL(28,12) DEFAULT NULL COMMENT '单值指标结果',
  `extra_values` JSON DEFAULT NULL COMMENT '多值指标结果（如BOLL上中下，MACD三线等）',

  `data_quality` VARCHAR(16) NOT NULL DEFAULT 'OK' COMMENT '数据质量：OK/PARTIAL/INVALID',
  `calc_engine` VARCHAR(32) NOT NULL DEFAULT 'ta4j' COMMENT '计算引擎：ta4j/custom',
  `calc_fingerprint` CHAR(64) NOT NULL COMMENT '计算指纹：hash(code+version+params+engine)',
  `calc_cost_ms` INT DEFAULT NULL COMMENT '计算耗时毫秒（用于观测）',

  `source` VARCHAR(32) NOT NULL DEFAULT 'OKX' COMMENT '数据源：OKX（预留多交易所）',

  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ind_value` (
    `user_id`,`trading_pair_id`,`timeframe`,`bar_time`,`indicator_code`,`indicator_version`
  ),
  KEY `idx_pair_tf_time` (`user_id`,`trading_pair_id`,`timeframe`,`bar_time`),
  KEY `idx_code_time` (`user_id`,`indicator_code`,`timeframe`,`bar_time`),
  KEY `idx_symbol_time` (`user_id`,`symbol`,`timeframe`,`bar_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指标事实表（每根bar闭合后的指标结果）';

