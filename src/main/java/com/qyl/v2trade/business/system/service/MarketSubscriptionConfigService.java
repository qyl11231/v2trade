package com.qyl.v2trade.business.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qyl.v2trade.business.system.model.entity.MarketSubscriptionConfig;

import java.util.List;

/**
 * 行情订阅配置服务接口
 */
public interface MarketSubscriptionConfigService extends IService<MarketSubscriptionConfig> {

    /**
     * 根据交易对ID查询配置
     * @param tradingPairId 交易对ID
     * @return 配置，未找到返回null
     */
    MarketSubscriptionConfig getByTradingPairId(Long tradingPairId);

    /**
     * 查询所有启用的订阅配置
     * @return 启用的配置列表
     */
    List<MarketSubscriptionConfig> listEnabled();

    /**
     * 保存或更新配置（根据tradingPairId唯一键）
     * @param config 配置
     * @return 保存/更新后的配置
     */
    MarketSubscriptionConfig saveOrUpdateByTradingPairId(MarketSubscriptionConfig config);
}

