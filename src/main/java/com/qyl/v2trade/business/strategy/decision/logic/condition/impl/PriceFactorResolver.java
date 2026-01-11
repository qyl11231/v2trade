package com.qyl.v2trade.business.strategy.decision.logic.condition.impl;

import com.qyl.v2trade.business.strategy.decision.context.DecisionContext;
import com.qyl.v2trade.business.strategy.decision.context.snapshot.PriceSnapshot;
import com.qyl.v2trade.business.strategy.decision.logic.condition.FactorResolver;
import com.qyl.v2trade.business.strategy.decision.logic.condition.TypedValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 价格因子解析器实现
 * 
 * <p>支持因子key：
 * <ul>
 *   <li>PX.LAST：最新价格</li>
 * </ul>
 */
@Slf4j
@Component
public class PriceFactorResolver implements FactorResolver {

    @Override
    public String getSupportedPrefix() {
        return "PX.";
    }

    @Override
    public Optional<TypedValue> resolve(String factorKey, DecisionContext ctx) {
        if (!factorKey.startsWith("PX.")) {
            return Optional.empty();
        }

        PriceSnapshot price = ctx.getPriceSnapshot();
        if (price == null || !price.isAvailable()) {
            return Optional.empty();
        }

        String field = factorKey.substring(3);
        switch (field) {
            case "LAST":
                return Optional.of(TypedValue.ofNumber(price.getCurrentPrice()));
            default:
                log.debug("不支持的价格因子字段: field={}", field);
                return Optional.empty();
        }
    }
}

