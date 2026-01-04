-- ==========================================
-- 行情校准任务配置表 (market_calibration_task_config)
-- 用于管理行情校准任务配置
-- ==========================================
CREATE TABLE IF NOT EXISTS `market_calibration_task_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `task_name` VARCHAR(128) NOT NULL COMMENT '任务名称',
    `task_type` VARCHAR(32) NOT NULL COMMENT '任务类型：MISSING_DATA-缺失数据检测, DATA_VERIFY-数据核对',
    `trading_pair_id` BIGINT NOT NULL COMMENT '交易对ID（关联trading_pair表）',
    `execution_mode` VARCHAR(32) NOT NULL COMMENT '执行模式：AUTO-自动, MANUAL-手动',
    `interval_hours` INT DEFAULT NULL COMMENT '自动模式：检测周期（小时），如1表示检测最近1小时',
    `start_time` DATETIME DEFAULT NULL COMMENT '手动模式：开始时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '手动模式：结束时间',
    `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：1-启用 0-禁用',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注说明',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_trading_pair` (`trading_pair_id`),
    KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='行情校准任务配置表';

-- ==========================================
-- 行情校准任务执行日志表 (market_calibration_task_log)
-- 用于记录任务执行历史
-- ==========================================
CREATE TABLE IF NOT EXISTS `market_calibration_task_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `task_config_id` BIGINT NOT NULL COMMENT '任务配置ID',
    `task_name` VARCHAR(128) NOT NULL COMMENT '任务名称（冗余字段，便于查询）',
    `task_type` VARCHAR(32) NOT NULL COMMENT '任务类型',
    `trading_pair_id` BIGINT NOT NULL COMMENT '交易对ID',
    `symbol` VARCHAR(32) NOT NULL COMMENT '交易对符号（冗余字段）',
    `execution_mode` VARCHAR(32) NOT NULL COMMENT '执行模式',
    `detect_start_time` DATETIME NOT NULL COMMENT '检测开始时间',
    `detect_end_time` DATETIME NOT NULL COMMENT '检测结束时间',
    `status` VARCHAR(32) NOT NULL COMMENT '执行状态：RUNNING-执行中, SUCCESS-成功, FAILED-失败',
    `missing_count` INT DEFAULT 0 COMMENT '缺失K线数量（仅缺失检测任务）',
    `filled_count` INT DEFAULT 0 COMMENT '补全K线数量（仅缺失检测任务）',
    `duplicate_count` INT DEFAULT 0 COMMENT '重复数据数量（仅核对任务）',
    `error_count` INT DEFAULT 0 COMMENT '异常数据数量（仅核对任务）',
    `execute_duration_ms` BIGINT DEFAULT NULL COMMENT '执行耗时（毫秒）',
    `error_message` TEXT DEFAULT NULL COMMENT '错误信息',
    `execute_log` TEXT DEFAULT NULL COMMENT '执行日志详情（JSON格式）',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_task_config` (`task_config_id`),
    KEY `idx_trading_pair` (`trading_pair_id`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_task_config_created` (`task_config_id`, `created_at`) COMMENT '用于查询任务最新执行记录'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='行情校准任务执行日志表';

