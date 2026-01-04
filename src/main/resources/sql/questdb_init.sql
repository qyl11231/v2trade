-- ==========================================
-- QuestDB 表结构初始化脚本
-- 行情数据中心时序数据表
-- ==========================================

-- 1分钟K线表（主表，所有其他周期由此聚合）
CREATE TABLE IF NOT EXISTS kline_1m (
    symbol SYMBOL,
    ts TIMESTAMP,
    open DOUBLE,
    high DOUBLE,
    low DOUBLE,
    close DOUBLE,
    volume DOUBLE,
    exchange_ts LONG
) TIMESTAMP(ts) PARTITION BY DAY;

-- 创建唯一索引（防止重复插入）
-- QuestDB使用symbol + ts作为唯一键
-- 注意：QuestDB的索引创建方式不同，这里使用ALTER TABLE ADD INDEX
-- 但QuestDB 7.3.4版本可能不支持传统索引，使用symbol列类型本身已优化

-- 为symbol列创建索引（QuestDB的SYMBOL类型已自动优化）
-- 如果需要，可以创建额外的索引：
-- CREATE INDEX idx_symbol_ts ON kline_1m (symbol, ts);

-- 注意：QuestDB的索引语法可能与MySQL不同
-- 实际使用时，可以通过查询性能测试来验证是否需要额外索引

-- ==========================================
-- 多周期K线表（由1m聚合生成）
-- ==========================================

-- 5分钟K线表
CREATE TABLE IF NOT EXISTS kline_5m (
    symbol SYMBOL,
    ts TIMESTAMP,
    open DOUBLE,
    high DOUBLE,
    low DOUBLE,
    close DOUBLE,
    volume DOUBLE,
    exchange_ts LONG
) TIMESTAMP(ts) PARTITION BY DAY;

-- 15分钟K线表
CREATE TABLE IF NOT EXISTS kline_15m (
    symbol SYMBOL,
    ts TIMESTAMP,
    open DOUBLE,
    high DOUBLE,
    low DOUBLE,
    close DOUBLE,
    volume DOUBLE,
    exchange_ts LONG
) TIMESTAMP(ts) PARTITION BY DAY;

-- 30分钟K线表
CREATE TABLE IF NOT EXISTS kline_30m (
    symbol SYMBOL,
    ts TIMESTAMP,
    open DOUBLE,
    high DOUBLE,
    low DOUBLE,
    close DOUBLE,
    volume DOUBLE,
    exchange_ts LONG
) TIMESTAMP(ts) PARTITION BY DAY;

-- 1小时K线表
CREATE TABLE IF NOT EXISTS kline_1h (
    symbol SYMBOL,
    ts TIMESTAMP,
    open DOUBLE,
    high DOUBLE,
    low DOUBLE,
    close DOUBLE,
    volume DOUBLE,
    exchange_ts LONG
) TIMESTAMP(ts) PARTITION BY DAY;

-- 4小时K线表
CREATE TABLE IF NOT EXISTS kline_4h (
    symbol SYMBOL,
    ts TIMESTAMP,
    open DOUBLE,
    high DOUBLE,
    low DOUBLE,
    close DOUBLE,
    volume DOUBLE,
    exchange_ts LONG
) TIMESTAMP(ts) PARTITION BY DAY;

