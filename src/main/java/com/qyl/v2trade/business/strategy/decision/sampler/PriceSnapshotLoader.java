package com.qyl.v2trade.business.strategy.decision.sampler;

import com.qyl.v2trade.business.strategy.decision.context.snapshot.PriceSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 价格快照加载器
 * 
 * <p>职责：
 * <ul>
 *   <li>从内存价格快照获取最新价格</li>
 *   <li>如果价格服务未就绪，返回null</li>
 * </ul>
 * 
 * <p>注意：当前实现为占位，后续需要对接价格服务
 */
@Slf4j
@Component
public class PriceSnapshotLoader {

    /**
     * 加载价格快照
     * 
     * @param tradingPairId 交易对ID
     * @return 价格快照，如果价格服务未就绪返回null
     */
    public PriceSnapshot load(Long tradingPairId) {
        // TODO: 对接价格服务，从内存快照获取最新价格
        // 当前实现：返回null，在GuardChain中校验
        log.debug("价格快照加载（占位实现）: tradingPairId={}", tradingPairId);
        return null;
    }

    /**
     * 加载价格快照（带默认值）
     * 
     * @param tradingPairId 交易对ID
     * @param defaultPrice 默认价格（如果价格服务未就绪）
     * @return 价格快照
     */
    public PriceSnapshot loadWithDefault(Long tradingPairId, BigDecimal defaultPrice) {
        PriceSnapshot snapshot = load(tradingPairId);
        if (snapshot == null || !snapshot.isAvailable()) {
            return PriceSnapshot.builder()
                .currentPrice(defaultPrice)
                .priceTime(LocalDateTime.now())
                .source("DEFAULT")
                .build();
        }
        return snapshot;
    }
}

