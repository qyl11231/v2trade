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

