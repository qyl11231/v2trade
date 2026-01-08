package com.qyl.v2trade.market.subscription.service.impl;

import com.qyl.v2trade.market.subscription.service.LatestPrice;
import com.qyl.v2trade.market.subscription.service.LatestPriceService;
import com.qyl.v2trade.market.subscription.service.PriceQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 价格查询服务实现
 * 
 * <p>对外提供价格查询接口，委托给LatestPriceService实现。
 *
 * @author qyl
 */
@Slf4j
@Service
public class PriceQueryServiceImpl implements PriceQueryService {

    private final LatestPriceService latestPriceService;

    public PriceQueryServiceImpl(LatestPriceService latestPriceService) {
        this.latestPriceService = latestPriceService;
    }

    @Override
    public Optional<BigDecimal> getLatestPrice(String symbol) {
        if (symbol == null) {
            return Optional.empty();
        }

        BigDecimal price = latestPriceService.getPrice(symbol);
        return Optional.ofNullable(price);
    }

    @Override
    public Optional<LatestPrice> getLatestPriceWithTimestamp(String symbol) {
        if (symbol == null) {
            return Optional.empty();
        }

        return latestPriceService.getLatestPrice(symbol);
    }

    @Override
    public Map<String, BigDecimal> getLatestPrices(Set<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return Map.of();
        }

        Map<String, BigDecimal> result = new HashMap<>();
        for (String symbol : symbols) {
            BigDecimal price = latestPriceService.getPrice(symbol);
            if (price != null) {
                result.put(symbol, price);
            }
        }
        return result;
    }

    @Override
    public boolean hasPrice(String symbol) {
        if (symbol == null) {
            return false;
        }

        return latestPriceService.getPrice(symbol) != null;
    }
}

