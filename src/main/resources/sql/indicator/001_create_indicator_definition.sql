CREATE TABLE IF NOT EXISTS `indicator_definition` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '所属用户ID（0=系统内置；>0=用户自定义/企业定制预留）',

  `indicator_code` VARCHAR(64) NOT NULL COMMENT '指标编码（主名，如 RSI、MACD、BOLL）',
  `indicator_name` VARCHAR(128) NOT NULL COMMENT '指标名称（展示用）',
  `indicator_version` VARCHAR(16) NOT NULL DEFAULT 'v1' COMMENT '指标版本：v1（v1.2.1固定）',
  `category` VARCHAR(32) NOT NULL DEFAULT 'GENERAL' COMMENT '分类：TREND/MOMENTUM/VOLATILITY/VOLUME/GENERAL',
  `engine` VARCHAR(32) NOT NULL DEFAULT 'ta4j' COMMENT '计算引擎：ta4j/custom',

  `param_schema` JSON DEFAULT NULL COMMENT '参数Schema（ParameterSpec）',
  `return_schema` JSON DEFAULT NULL COMMENT '返回Schema（ReturnSpec）',
  `min_required_bars` INT NOT NULL DEFAULT 1 COMMENT '最小所需bar数量',
  `supported_timeframes` JSON DEFAULT NULL COMMENT '支持周期列表，如 ["1m","5m","1h"]',

  `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ind_def` (`user_id`,`indicator_code`,`indicator_version`),
  KEY `idx_ind_enabled` (`user_id`,`enabled`),
  KEY `idx_ind_code` (`user_id`,`indicator_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指标定义表（元数据/文档/UI/版本预留）';

