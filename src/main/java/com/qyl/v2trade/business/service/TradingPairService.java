package com.qyl.v2trade.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qyl.v2trade.business.model.entity.TradingPair;

import java.util.List;

/**
 * 交易对服务接口
 */
public interface TradingPairService extends IService<TradingPair> {

    /**
     * 根据symbol和marketType查询交易对
     * @param symbol 交易对标识
     * @param marketType 市场类型
     * @return 交易对，未找到返回null
     */
    TradingPair getBySymbolAndMarketType(String symbol, String marketType);

    /**
     * 查询所有启用的交易对
     * @return 启用的交易对列表
     */
    List<TradingPair> listEnabled();

    /**
     * 根据市场类型查询交易对列表
     * @param marketType 市场类型
     * @return 交易对列表
     */
    List<TradingPair> listByMarketType(String marketType);

    /**
     * 根据市场类型查询启用的交易对列表
     * @param marketType 市场类型
     * @return 启用的交易对列表
     */
    List<TradingPair> listEnabledByMarketType(String marketType);

    /**
     * 启用/禁用交易对
     * @param id 交易对ID
     * @param enabled 是否启用
     * @return 更新后的交易对
     */
    TradingPair toggleEnabled(Long id, Integer enabled);

    /**
     * 保存或更新交易对（根据symbol+marketType唯一键）
     * @param tradingPair 交易对
     * @return 保存/更新后的交易对
     */
    TradingPair saveOrUpdateBySymbol(TradingPair tradingPair);

    /**
     * 同步OKX交易对数据
     * @param marketType 市场类型（SPOT/SWAP）
     * @return 同步的交易对数量
     */
    int syncFromOkx(String marketType);
}

