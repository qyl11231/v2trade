package com.qyl.v2trade.business.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qyl.v2trade.business.system.mapper.MarketSubscriptionConfigMapper;
import com.qyl.v2trade.business.system.model.entity.MarketSubscriptionConfig;
import com.qyl.v2trade.business.system.service.MarketSubscriptionConfigService;
import com.qyl.v2trade.common.constants.EnabledStatus;
import com.qyl.v2trade.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 行情订阅配置服务实现
 */
@Service
public class MarketSubscriptionConfigServiceImpl extends ServiceImpl<MarketSubscriptionConfigMapper, MarketSubscriptionConfig>
        implements MarketSubscriptionConfigService {

    @Override
    public MarketSubscriptionConfig getByTradingPairId(Long tradingPairId) {
        LambdaQueryWrapper<MarketSubscriptionConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MarketSubscriptionConfig::getTradingPairId, tradingPairId);
        return getOne(wrapper);
    }

    @Override
    public List<MarketSubscriptionConfig> listEnabled() {
        LambdaQueryWrapper<MarketSubscriptionConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MarketSubscriptionConfig::getEnabled, EnabledStatus.ENABLED);
        return list(wrapper);
    }

    @Override
    public MarketSubscriptionConfig saveOrUpdateByTradingPairId(MarketSubscriptionConfig config) {
        if (config.getTradingPairId() == null) {
            throw new BusinessException("交易对ID不能为空");
        }

        // 查询是否已存在
        MarketSubscriptionConfig existing = getByTradingPairId(config.getTradingPairId());
        
        if (existing != null) {
            // 更新
            config.setId(existing.getId());
            updateById(config);
            return config;
        } else {
            // 新增
            // 设置默认值
            if (config.getEnabled() == null) {
                config.setEnabled(EnabledStatus.ENABLED);
            }
            if (config.getCacheDurationMinutes() == null) {
                config.setCacheDurationMinutes(60); // 默认60分钟
            }
            save(config);
            return config;
        }
    }
}

