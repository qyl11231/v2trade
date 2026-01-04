package com.qyl.v2trade.market.aggregation.persistence;

import com.qyl.v2trade.market.aggregation.event.AggregatedKLine;

import java.util.List;

/**
 * 聚合K线存储服务接口
 * 
 * <p>负责将聚合完成的K线数据持久化到QuestDB
 *
 * @author qyl
 */
public interface AggregatedKLineStorageService {
    
    /**
     * 写入聚合K线（幂等）
     * 
     * <p>【重要】数据唯一性保证：
     * <ul>
     *   <li>写入前必须检查该周期在该级别是否已存在</li>
     *   <li>如果已存在，绝不写入，返回false（保证数据唯一性，不重复插入）</li>
     *   <li>如果不存在，执行写入，返回true</li>
     * </ul>
     * 
     * <p>【重要】时间对齐保证：
     * <ul>
     *   <li>传入的aggregatedKLine的时间戳必须已对齐到周期起始时间</li>
     *   <li>使用 PeriodCalculator.alignTimestamp() 确保对齐</li>
     * </ul>
     * 
     * @param aggregatedKLine 聚合K线（时间戳必须已对齐到周期起始时间）
     * @return 是否成功写入（如果已存在返回false，不重复插入）
     */
    boolean save(AggregatedKLine aggregatedKLine);
    
    /**
     * 批量写入（用于历史数据回放）
     * 
     * @param aggregatedKLines 聚合K线列表
     * @return 成功写入的数量
     */
    int batchSave(List<AggregatedKLine> aggregatedKLines);
    
    /**
     * 检查是否存在
     * 
     * @param symbol 交易对符号
     * @param period 周期（如：5m, 15m, 1h）
     * @param timestamp 时间戳（毫秒，必须已对齐到周期起始时间）
     * @return 如果存在返回true
     */
    boolean exists(String symbol, String period, long timestamp);
}

