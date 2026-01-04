package com.qyl.v2trade.market.calibration.service.impl;

import com.qyl.v2trade.market.calibration.service.KlineDataFiller;
import com.qyl.v2trade.market.model.NormalizedKline;
import com.qyl.v2trade.market.subscription.persistence.storage.MarketStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 数据补全服务实现类
 */
@Slf4j
@Service
public class KlineDataFillerImpl implements KlineDataFiller {

    @Autowired
    private MarketStorageService marketStorageService;

    @Override
    public int fillMissingKlines(String symbol, List<NormalizedKline> klines) {
        log.info("开始填充缺失的K线数据: symbol={}, 数量={}", symbol, klines.size());

        if (klines == null || klines.isEmpty()) {
            return 0;
        }

        // 批量保存K线数据
        // MarketStorageService的batchSaveKlines方法内部已经处理了幂等性（检查是否存在）
        int savedCount = marketStorageService.batchSaveKlines(klines);

        log.info("K线数据填充完成: symbol={}, 总数={}, 成功插入={}", 
                symbol, klines.size(), savedCount);

        return savedCount;
    }
}

