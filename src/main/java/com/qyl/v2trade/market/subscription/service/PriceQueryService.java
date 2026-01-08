package com.qyl.v2trade.market.subscription.service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 价格查询服务（对外接口）
 * 
 * <p>提供同步查询最新价格的接口，供策略、风控等模块使用。
 *
 * @author qyl
 */
public interface PriceQueryService {
    
    /**
     * 获取最新价格
     * 
     * @param symbol 交易对符号
     * @return 最新价格，如果不存在则返回Optional.empty()
     */
    Optional<BigDecimal> getLatestPrice(String symbol);
    
    /**
     * 获取最新价格（带时间戳）
     * 
     * @param symbol 交易对符号
     * @return LatestPrice，如果不存在则返回Optional.empty()
     */
    Optional<LatestPrice> getLatestPriceWithTimestamp(String symbol);
    
    /**
     * 批量获取最新价格
     * 
     * @param symbols 交易对符号集合
     * @return 价格Map（symbol -> price），不存在的交易对不会出现在Map中
     */
    Map<String, BigDecimal> getLatestPrices(Set<String> symbols);
    
    /**
     * 检查交易对是否有价格数据
     * 
     * @param symbol 交易对符号
     * @return true表示有价格数据
     */
    boolean hasPrice(String symbol);
}

