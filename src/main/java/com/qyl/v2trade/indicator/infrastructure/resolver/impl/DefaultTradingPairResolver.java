package com.qyl.v2trade.indicator.infrastructure.resolver.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qyl.v2trade.business.system.model.entity.TradingPair;
import com.qyl.v2trade.business.system.mapper.TradingPairMapper;
import com.qyl.v2trade.indicator.infrastructure.resolver.TradingPairResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认交易对解析器实现
 * 
 * <p>简单的内存缓存实现，适合v1.2.1阶段
 *
 * @author qyl
 */
@Slf4j
@Component
public class DefaultTradingPairResolver implements TradingPairResolver {
    
    @Autowired(required = false)
    private TradingPairMapper tradingPairMapper;
    
    /**
     * symbol -> trading_pair_id 缓存
     */
    private final Map<String, Long> symbolToIdCache = new ConcurrentHashMap<>();
    
    /**
     * trading_pair_id -> symbol 缓存
     */
    private final Map<Long, String> idToSymbolCache = new ConcurrentHashMap<>();
    
    @Override
    public Long symbolToTradingPairId(String symbol) {
        if (symbol == null || symbol.isEmpty()) {
            return null;
        }
        
        // 先查缓存
        Long cached = symbolToIdCache.get(symbol);
        if (cached != null) {
            return cached;
        }
        
        // 查询数据库
        if (tradingPairMapper == null) {
            log.warn("TradingPairMapper未注入，无法解析tradingPairId: symbol={}", symbol);
            return null;
        }
        
        try {
            // 解析symbol和市场类型
            // symbol可能是：BTC-USDT 或 BTC-USDT-SWAP
            String baseSymbol = symbol;
            String marketType = null;
            
            // 尝试提取市场类型（SWAP/FUTURES）
            if (symbol.endsWith("-SWAP")) {
                baseSymbol = symbol.substring(0, symbol.length() - 5); // 移除 "-SWAP"
                marketType = "SWAP";
            } else if (symbol.endsWith("-FUTURES")) {
                baseSymbol = symbol.substring(0, symbol.length() - 8); // 移除 "-FUTURES"
                marketType = "FUTURES";
            }
            
            // 策略1：如果提取到市场类型，先尝试精确匹配
            if (marketType != null) {
                TradingPair pair = tradingPairMapper.selectOne(
                    new LambdaQueryWrapper<TradingPair>()
                        .eq(TradingPair::getSymbol, baseSymbol)
                        .eq(TradingPair::getMarketType, marketType)
                        .last("LIMIT 1")
                );
                
                if (pair != null) {
                    Long id = pair.getId();
                    symbolToIdCache.put(symbol, id);
                    idToSymbolCache.put(id, symbol);
                    log.debug("成功解析tradingPairId: symbol={} -> id={} (marketType={})", symbol, id, marketType);
                    return id;
                }
            }
            
            // 策略2：直接匹配symbol（可能symbol就是标准格式，如 BTC-USDT）
            TradingPair pair = tradingPairMapper.selectOne(
                new LambdaQueryWrapper<TradingPair>()
                    .eq(TradingPair::getSymbol, symbol)
                    .last("LIMIT 1")
            );
            
            if (pair != null) {
                Long id = pair.getId();
                symbolToIdCache.put(symbol, id);
                idToSymbolCache.put(id, symbol);
                log.debug("成功解析tradingPairId: symbol={} -> id={} (直接匹配)", symbol, id);
                return id;
            }
            
            // 策略3：如果提取了baseSymbol，尝试匹配baseSymbol（可能有多个marketType，取第一个）
            if (marketType != null && !baseSymbol.equals(symbol)) {
                pair = tradingPairMapper.selectOne(
                    new LambdaQueryWrapper<TradingPair>()
                        .eq(TradingPair::getSymbol, baseSymbol)
                        .last("LIMIT 1")
                );
                
                if (pair != null) {
                    Long id = pair.getId();
                    symbolToIdCache.put(symbol, id);
                    idToSymbolCache.put(id, symbol);
                    log.debug("成功解析tradingPairId: symbol={} -> id={} (通过baseSymbol={})", symbol, id, baseSymbol);
                    return id;
                }
            }
            
            log.warn("未找到tradingPairId: symbol={}, baseSymbol={}, marketType={}", symbol, baseSymbol, marketType);
            return null;
            
        } catch (Exception e) {
            log.error("查询tradingPairId失败: symbol={}", symbol, e);
            return null;
        }
    }
    
    @Override
    public String tradingPairIdToSymbol(Long tradingPairId) {
        if (tradingPairId == null) {
            return null;
        }
        
        // 先查缓存
        String cached = idToSymbolCache.get(tradingPairId);
        if (cached != null) {
            return cached;
        }
        
        // 查询数据库
        if (tradingPairMapper == null) {
            log.warn("TradingPairMapper未注入，无法解析symbol: tradingPairId={}", tradingPairId);
            return null;
        }
        
        try {
            TradingPair pair = tradingPairMapper.selectById(tradingPairId);
            
            if (pair != null) {
                String symbol = pair.getSymbol();
                // 更新缓存
                idToSymbolCache.put(tradingPairId, symbol);
                symbolToIdCache.put(symbol, tradingPairId);
                return symbol;
            }
            
            log.debug("未找到symbol: tradingPairId={}", tradingPairId);
            return null;
            
        } catch (Exception e) {
            log.error("查询symbol失败: tradingPairId={}", tradingPairId, e);
            return null;
        }
    }
}

