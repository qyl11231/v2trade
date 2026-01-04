-- ==========================================
-- K线聚合表初始化脚本
-- 注意：QuestDB的约束实现方式，通过应用层保证唯一性
-- ==========================================

-- 5分钟K线表
-- 唯一约束：(symbol, ts) - 通过应用层实现
CREATE TABLE IF NOT EXISTS kline_5m (
    symbol SYMBOL,
    ts TIMESTAMP,
    open DOUBLE,
    high DOUBLE,
    low DOUBLE,
    close DOUBLE,
    volume DOUBLE,
    source_kline_count INT
) TIMESTAMP(ts) PARTITION BY DAY;

-- 15分钟K线表
-- 唯一约束：(symbol, ts) - 通过应用层实现
CREATE TABLE IF NOT EXISTS kline_15m (
    symbol SYMBOL,
    ts TIMESTAMP,
    open DOUBLE,
    high DOUBLE,
    low DOUBLE,
    close DOUBLE,
    volume DOUBLE,
    source_kline_count INT
) TIMESTAMP(ts) PARTITION BY DAY;

-- 30分钟K线表
-- 唯一约束：(symbol, ts) - 通过应用层实现
CREATE TABLE IF NOT EXISTS kline_30m (
    symbol SYMBOL,
    ts TIMESTAMP,
    open DOUBLE,
    high DOUBLE,
    low DOUBLE,
    close DOUBLE,
    volume DOUBLE,
    source_kline_count INT
) TIMESTAMP(ts) PARTITION BY DAY;

-- 1小时K线表
-- 唯一约束：(symbol, ts) - 通过应用层实现
CREATE TABLE IF NOT EXISTS kline_1h (
    symbol SYMBOL,
    ts TIMESTAMP,
    open DOUBLE,
    high DOUBLE,
    low DOUBLE,
    close DOUBLE,
    volume DOUBLE,
    source_kline_count INT
) TIMESTAMP(ts) PARTITION BY DAY;

-- 4小时K线表
-- 唯一约束：(symbol, ts) - 通过应用层实现
CREATE TABLE IF NOT EXISTS kline_4h (
    symbol SYMBOL,
    ts TIMESTAMP,
    open DOUBLE,
    high DOUBLE,
    low DOUBLE,
    close DOUBLE,
    volume DOUBLE,
    source_kline_count INT
) TIMESTAMP(ts) PARTITION BY DAY;

-- 注意：
-- 1. QuestDB可能不支持传统的UNIQUE约束语法
-- 2. 唯一性通过应用层保证：
--    - 写入前检查：SELECT COUNT(*) FROM table WHERE symbol = ? AND ts = ?
--    - 如果存在则跳过，不存在则插入
-- 3. 或者使用UPSERT语法（如果QuestDB支持）
-- 4. 时间戳(ts)必须对齐到周期起始时间（使用PeriodCalculator.alignTimestamp()确保对齐）

