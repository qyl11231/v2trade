package com.qyl.v2trade.indicator.infrastructure.resolver;

/**
 * 交易对解析器
 * 
 * <p>用于symbol和trading_pair_id之间的转换
 * 
 * <p>注意：这是基础设施层接口，具体实现可以缓存+DB回源
 *
 * @author qyl
 */
public interface TradingPairResolver {
    
    /**
     * symbol转trading_pair_id
     * 
     * @param symbol 交易对符号（如：BTC-USDT-SWAP）
     * @return trading_pair_id，如果不存在返回null
     */
    Long symbolToTradingPairId(String symbol);
    
    /**
     * trading_pair_id转symbol
     * 
     * @param tradingPairId 交易对ID
     * @return symbol，如果不存在返回null
     */
    String tradingPairIdToSymbol(Long tradingPairId);
}

