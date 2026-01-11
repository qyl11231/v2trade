package com.qyl.v2trade.business.strategy.decision.logic.condition.impl;

import com.qyl.v2trade.business.strategy.decision.context.DecisionContext;
import com.qyl.v2trade.business.strategy.decision.context.snapshot.SignalSnapshot;
import com.qyl.v2trade.business.strategy.decision.logic.condition.FactorResolver;
import com.qyl.v2trade.business.strategy.decision.logic.condition.TypedValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * 信号因子解析器实现
 * 
 * <p>支持因子key：
 * <ul>
 *   <li>SIG.DIRECTION：信号方向（BUY/SELL/FLAT/REVERSE）</li>
 *   <li>SIG.INTENT_ID：信号意图ID</li>
 * </ul>
 */
@Slf4j
@Component
public class SignalFactorResolver implements FactorResolver {

    @Override
    public String getSupportedPrefix() {
        return "SIG.";
    }

    @Override
    public Optional<TypedValue> resolve(String factorKey, DecisionContext ctx) {
        if (!factorKey.startsWith("SIG.")) {
            return Optional.empty();
        }

        SignalSnapshot signal = ctx.getSignalSnapshot();
        if (signal == null || !signal.isValid()) {
            return Optional.empty();
        }

        String field = factorKey.substring(4);
        switch (field) {
            case "DIRECTION":
                return Optional.of(TypedValue.ofString(signal.getIntentDirection()));
            case "INTENT_ID":
                if (signal.getSignalIntentId() != null) {
                    return Optional.of(TypedValue.ofNumber(
                        BigDecimal.valueOf(signal.getSignalIntentId())));
                }
                return Optional.empty();
            default:
                log.debug("不支持的信号因子字段: field={}", field);
                return Optional.empty();
        }
    }
}

