package com.qyl.v2trade.business.strategy.decision.logic.condition.impl;

import com.qyl.v2trade.business.strategy.decision.context.DecisionContext;
import com.qyl.v2trade.business.strategy.decision.context.snapshot.BarSnapshot;
import com.qyl.v2trade.business.strategy.decision.logic.condition.FactorResolver;
import com.qyl.v2trade.business.strategy.decision.logic.condition.TypedValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * K线因子解析器实现
 * 
 * <p>支持因子key：
 * <ul>
 *   <li>BAR.OPEN：开盘价</li>
 *   <li>BAR.HIGH：最高价</li>
 *   <li>BAR.LOW：最低价</li>
 *   <li>BAR.CLOSE：收盘价</li>
 *   <li>BAR.VOLUME：成交量</li>
 * </ul>
 */
@Slf4j
@Component
public class BarFactorResolver implements FactorResolver {

    @Override
    public String getSupportedPrefix() {
        return "BAR.";
    }

    @Override
    public Optional<TypedValue> resolve(String factorKey, DecisionContext ctx) {
        if (!factorKey.startsWith("BAR.")) {
            return Optional.empty();
        }

        BarSnapshot bar = ctx.getBarSnapshot();
        if (bar == null) {
            return Optional.empty();
        }

        String field = factorKey.substring(4);
        switch (field) {
            case "OPEN":
                return Optional.of(TypedValue.ofNumber(bar.getOpen()));
            case "HIGH":
                return Optional.of(TypedValue.ofNumber(bar.getHigh()));
            case "LOW":
                return Optional.of(TypedValue.ofNumber(bar.getLow()));
            case "CLOSE":
                return Optional.of(TypedValue.ofNumber(bar.getClose()));
            case "VOLUME":
                return Optional.of(TypedValue.ofNumber(bar.getVolume()));
            default:
                log.debug("不支持的K线因子字段: field={}", field);
                return Optional.empty();
        }
    }
}

