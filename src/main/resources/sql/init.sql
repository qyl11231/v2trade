-- ==========================================
-- 用户表 (sys_user)
-- ==========================================
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（MD5加密）',
    nickname VARCHAR(100) COMMENT '昵称',
    email VARCHAR(100) COMMENT '邮箱',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用:0-禁用,1-启用',
    last_login_at DATETIME(3) COMMENT '最后登录时间',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除:0-未删除,1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_enabled (enabled)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- 插入默认管理员用户（用户名：admin，密码：admin123）
-- 密码MD5: 0192023a7bbd73250516f069df18b500
INSERT INTO sys_user (username, password, nickname, email, enabled)
VALUES ('admin', '0192023a7bbd73250516f069df18b500', '管理员', 'admin@okx-trade.com', 1)
    ON DUPLICATE KEY UPDATE username=username;

-- ==========================================
-- 用户交易所API Key表
-- ==========================================
CREATE TABLE `user_api_key` (
      `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'API Key ID',
      `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
      `exchange` VARCHAR(32) NOT NULL COMMENT '交易所，如 OKX',
      `api_key` VARCHAR(255) NOT NULL COMMENT 'API Key',
      `secret_key` VARCHAR(255) NOT NULL COMMENT 'Secret Key',
      `passphrase` VARCHAR(255) DEFAULT NULL COMMENT 'Passphrase',
      `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用 0-禁用',
      `remark` VARCHAR(128) DEFAULT NULL COMMENT '备注',
      `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
      PRIMARY KEY (`id`)
) ENGINE=InnoDB;

-- 添加api_key_name字段
ALTER TABLE `user_api_key` ADD COLUMN `api_key_name` VARCHAR(128) DEFAULT NULL COMMENT 'API Key名称';



CREATE TABLE `signal_config` (
                                 `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '信号配置ID',
                                 `user_id` bigint(20) NOT NULL COMMENT '用户ID',
                                 `api_key_id` bigint(20) NOT NULL COMMENT 'API Key ID',
                                 `signal_name` varchar(64) NOT NULL COMMENT '信号名称（TradingView strategy name）',
                                 `symbol` varchar(32) NOT NULL COMMENT '交易对，如 BTC-USDT',
                                 `enabled` tinyint(4) NOT NULL DEFAULT '1' COMMENT '是否启用：1-启用 0-禁用',
                                 `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `uk_signal_name_api` (`signal_name`,`api_key_id`),
                                 KEY `idx_user_id` (`user_id`),
                                 KEY `idx_api_key_id` (`api_key_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='信号配置表（白名单 & 路由）';

CREATE TABLE `signal` (
                          `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '信号ID',
                          `user_id` bigint(20) NOT NULL COMMENT '用户ID',
                          `api_key_id` bigint(20) NOT NULL COMMENT 'API Key ID',
                          `signal_config_id` bigint(20) NOT NULL COMMENT '信号配置ID',
                          `signal_source` varchar(255) DEFAULT NULL COMMENT '信号来源：tv / internal / manual',
                          `signal_name` varchar(64) NOT NULL COMMENT '信号名称',
                          `symbol` varchar(32) NOT NULL COMMENT '交易对',
                          `signal_event_type` varchar(32) DEFAULT NULL COMMENT '信号事件类型：BREAKOUT / CROSS / OVERSOLD / CUSTOM（非交易动作）',
                          `signal_direction_hint` varchar(16) NOT NULL COMMENT '信号方向提示：LONG / SHORT / NEUTRAL',
                          `price` decimal(18,8) DEFAULT NULL COMMENT '信号参考价格',
                          `quantity` decimal(18,8) DEFAULT NULL COMMENT '信号建议数量',
                          `raw_payload` json NOT NULL COMMENT '原始信号内容（完整保留）',
                          `received_at` datetime NOT NULL COMMENT '信号接收时间',
                          `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
                          PRIMARY KEY (`id`),
                          KEY `idx_user_api` (`user_id`,`api_key_id`),
                          KEY `idx_signal_cfg` (`signal_config_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='信号事实表（语义层，不含买卖）';



CREATE TABLE trading_pair (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,

  symbol VARCHAR(32) NOT NULL COMMENT 'BTC-USDT',
  base_currency VARCHAR(16) NOT NULL COMMENT 'BTC',
  quote_currency VARCHAR(16) NOT NULL COMMENT 'USDT',

  market_type VARCHAR(16) NOT NULL COMMENT 'SPOT / SWAP / FUTURES',

  enabled TINYINT NOT NULL DEFAULT 1,

  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  UNIQUE KEY uk_symbol_market (symbol, market_type)
) COMMENT='交易对主表（平台级，区分现货/合约）';




CREATE TABLE exchange_market_pair (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,

  exchange_code VARCHAR(32) NOT NULL COMMENT 'OKX / BINANCE',

  trading_pair_id BIGINT NOT NULL,

  symbol_on_exchange VARCHAR(64) NOT NULL COMMENT 'BTC-USDT / BTC-USDT-SWAP',

  status VARCHAR(16) NOT NULL COMMENT 'TRADING / SUSPENDED',

  price_precision INT NOT NULL,
  quantity_precision INT NOT NULL,

  min_order_qty DECIMAL(18,8) NOT NULL,
  min_order_amount DECIMAL(18,8) NOT NULL,
  max_order_qty DECIMAL(18,8) DEFAULT NULL,
  max_leverage INT DEFAULT NULL,

  raw_payload JSON NOT NULL,

  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  UNIQUE KEY uk_exchange_pair (exchange_code, trading_pair_id),
  KEY idx_pair (trading_pair_id)
) COMMENT='交易所交易规则表（规则唯一真相）';


-- 为 signal_config 添加 trading_pair_id 字段（关联交易对）
ALTER TABLE `signal_config` ADD COLUMN `trading_pair_id` BIGINT DEFAULT NULL COMMENT '关联交易对ID（系统内部引用）';
ALTER TABLE `signal_config` ADD INDEX `idx_trading_pair` (`trading_pair_id`);


-- ==========================================
-- 行情订阅配置表 (market_subscription_config)
-- 用于管理哪些交易对需要采集行情数据
-- ==========================================
CREATE TABLE IF NOT EXISTS `market_subscription_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `trading_pair_id` BIGINT NOT NULL COMMENT '交易对ID（关联trading_pair表）',
    `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用行情采集：1-启用 0-禁用',
    `cache_duration_minutes` INT NOT NULL DEFAULT 60 COMMENT 'Redis缓存时长（分钟），默认60分钟',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注说明',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_trading_pair` (`trading_pair_id`),
    KEY `idx_enabled` (`enabled`),
    KEY `idx_trading_pair` (`trading_pair_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='行情订阅配置表（管理行情采集）';
