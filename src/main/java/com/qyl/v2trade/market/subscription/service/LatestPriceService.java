package com.qyl.v2trade.market.subscription.service;

import com.qyl.v2trade.market.model.event.PriceTick;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * 最新价格服务接口
 * 
 * <p>维护每个交易对的最新价格状态（仅内存）。
 *
 * @author qyl
 */
public interface LatestPriceService {
    
    /**
     * 更新价格（处理PriceTick事件）
     * 
     * <p>规则：
     * <ul>
     *   <li>时间戳旧于当前缓存 → 丢弃</li>
     *   <li>时间戳新于或等于当前缓存 → 更新</li>
     *   <li>更新后发布PriceChangedEvent</li>
     * </ul>
     * 
     * @param tick PriceTick事件
     */
    void updatePrice(PriceTick tick);
    
    /**
     * 获取最新价格（同步查询）
     * 
     * @param symbol 交易对符号
     * @return LatestPrice，如果不存在则返回Optional.empty()
     */
    Optional<LatestPrice> getLatestPrice(String symbol);
    
    /**
     * 获取最新价格（同步查询，简化版）
     * 
     * @param symbol 交易对符号
     * @return 价格，如果不存在则返回null
     */
    BigDecimal getPrice(String symbol);
    
    /**
     * 获取所有交易对的最新价格
     * 
     * @return 所有交易对的最新价格Map
     */
    Map<String, LatestPrice> getAllLatestPrices();
    
    /**
     * 清除指定交易对的价格（用于取消订阅）
     * 
     * @param symbol 交易对符号
     */
    void removePrice(String symbol);
}

