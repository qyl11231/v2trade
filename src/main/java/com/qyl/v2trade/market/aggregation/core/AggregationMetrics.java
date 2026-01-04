package com.qyl.v2trade.market.aggregation.core;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 聚合监控指标
 * 
 * <p>用于收集和监控聚合器的运行指标
 *
 * @author qyl
 */
public class AggregationMetrics implements Serializable {
    
    /**
     * 总处理的K线事件数量
     */
    private final LongAdder totalEventCount = new LongAdder();
    
    /**
     * 总生成的聚合K线数量
     */
    private final LongAdder totalAggregatedCount = new LongAdder();
    
    /**
     * 聚合成功数量
     */
    private final LongAdder successCount = new LongAdder();
    
    /**
     * 聚合失败数量
     */
    private final LongAdder failCount = new LongAdder();
    
    /**
     * 写入成功数量
     */
    private final LongAdder writeSuccessCount = new LongAdder();
    
    /**
     * 写入失败数量
     */
    private final LongAdder writeFailCount = new LongAdder();
    
    /**
     * 写入跳过数量（已存在）
     */
    private final LongAdder writeSkipCount = new LongAdder();
    
    /**
     * 时间乱序拒绝数量
     */
    private final LongAdder outOfOrderRejectCount = new LongAdder();
    
    /**
     * 重复数据忽略数量
     */
    private final LongAdder duplicateIgnoreCount = new LongAdder();
    
    /**
     * 聚合延迟统计（纳秒）
     */
    private final AtomicLong totalAggregationLatencyNs = new AtomicLong(0);
    private final AtomicLong maxAggregationLatencyNs = new AtomicLong(0);
    private final AtomicLong minAggregationLatencyNs = new AtomicLong(Long.MAX_VALUE);
    
    /**
     * 增加事件计数
     */
    public void incrementEventCount() {
        totalEventCount.increment();
    }
    
    /**
     * 增加聚合计数
     */
    public void incrementAggregatedCount() {
        totalAggregatedCount.increment();
    }
    
    /**
     * 增加成功计数
     */
    public void incrementSuccessCount() {
        successCount.increment();
    }
    
    /**
     * 增加失败计数
     */
    public void incrementFailCount() {
        failCount.increment();
    }
    
    /**
     * 增加写入成功计数
     */
    public void incrementWriteSuccessCount() {
        writeSuccessCount.increment();
    }
    
    /**
     * 增加写入失败计数
     */
    public void incrementWriteFailCount() {
        writeFailCount.increment();
    }
    
    /**
     * 增加写入跳过计数
     */
    public void incrementWriteSkipCount() {
        writeSkipCount.increment();
    }
    
    /**
     * 增加时间乱序拒绝计数
     */
    public void incrementOutOfOrderRejectCount() {
        outOfOrderRejectCount.increment();
    }
    
    /**
     * 增加重复数据忽略计数
     */
    public void incrementDuplicateIgnoreCount() {
        duplicateIgnoreCount.increment();
    }
    
    /**
     * 记录聚合延迟
     */
    public void recordAggregationLatency(long latencyNs) {
        totalAggregationLatencyNs.addAndGet(latencyNs);
        maxAggregationLatencyNs.updateAndGet(current -> Math.max(current, latencyNs));
        minAggregationLatencyNs.updateAndGet(current -> Math.min(current, latencyNs));
    }
    
    /**
     * 获取聚合成功率
     */
    public double getSuccessRate() {
        long total = totalEventCount.sum();
        if (total == 0) {
            return 0.0;
        }
        return (double) successCount.sum() / total * 100.0;
    }
    
    /**
     * 获取写入成功率
     */
    public double getWriteSuccessRate() {
        long total = writeSuccessCount.sum() + writeFailCount.sum() + writeSkipCount.sum();
        if (total == 0) {
            return 0.0;
        }
        return (double) writeSuccessCount.sum() / total * 100.0;
    }
    
    /**
     * 获取平均聚合延迟（毫秒）
     */
    public double getAverageAggregationLatencyMs() {
        long count = totalAggregatedCount.sum();
        if (count == 0) {
            return 0.0;
        }
        return (double) totalAggregationLatencyNs.get() / count / 1_000_000.0;
    }
    
    /**
     * 获取最大聚合延迟（毫秒）
     */
    public double getMaxAggregationLatencyMs() {
        long maxNs = maxAggregationLatencyNs.get();
        return maxNs == 0 ? 0.0 : (double) maxNs / 1_000_000.0;
    }
    
    /**
     * 获取最小聚合延迟（毫秒）
     */
    public double getMinAggregationLatencyMs() {
        long minNs = minAggregationLatencyNs.get();
        return minNs == Long.MAX_VALUE ? 0.0 : (double) minNs / 1_000_000.0;
    }
    
    // Getter方法
    
    public long getTotalEventCount() {
        return totalEventCount.sum();
    }
    
    public long getTotalAggregatedCount() {
        return totalAggregatedCount.sum();
    }
    
    public long getSuccessCount() {
        return successCount.sum();
    }
    
    public long getFailCount() {
        return failCount.sum();
    }
    
    public long getWriteSuccessCount() {
        return writeSuccessCount.sum();
    }
    
    public long getWriteFailCount() {
        return writeFailCount.sum();
    }
    
    public long getWriteSkipCount() {
        return writeSkipCount.sum();
    }
    
    public long getOutOfOrderRejectCount() {
        return outOfOrderRejectCount.sum();
    }
    
    public long getDuplicateIgnoreCount() {
        return duplicateIgnoreCount.sum();
    }
    
    /**
     * 重置所有指标
     */
    public void reset() {
        totalEventCount.reset();
        totalAggregatedCount.reset();
        successCount.reset();
        failCount.reset();
        writeSuccessCount.reset();
        writeFailCount.reset();
        writeSkipCount.reset();
        outOfOrderRejectCount.reset();
        duplicateIgnoreCount.reset();
        totalAggregationLatencyNs.set(0);
        maxAggregationLatencyNs.set(0);
        minAggregationLatencyNs.set(Long.MAX_VALUE);
    }
}

