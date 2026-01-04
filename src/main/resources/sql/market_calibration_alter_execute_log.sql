-- ==========================================
-- 修改 execute_log 字段类型为 LONGTEXT
-- 用于支持更大的执行日志内容
-- ==========================================
ALTER TABLE `market_calibration_task_log` 
MODIFY COLUMN `execute_log` LONGTEXT DEFAULT NULL COMMENT '执行日志详情（JSON格式）';

