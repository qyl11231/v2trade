package com.qyl.v2trade.business.strategy.decision.logic.condition.impl;

import com.qyl.v2trade.business.strategy.decision.context.DecisionContext;
import com.qyl.v2trade.business.strategy.decision.context.snapshot.IndicatorSnapshot;
import com.qyl.v2trade.business.strategy.decision.logic.condition.FactorResolver;
import com.qyl.v2trade.business.strategy.decision.logic.condition.TypedValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * 指标因子解析器实现
 * 
 * <p>支持因子key：
 * <ul>
 *   <li>IND.{indicatorCode}：指标主值（如 IND.RSI_14）</li>
 *   <li>IND.{extraKey}：指标扩展值（如 IND.MACD.SIGNAL）</li>
 * </ul>
 */
@Slf4j
@Component
public class IndicatorFactorResolver implements FactorResolver {

    @Override
    public String getSupportedPrefix() {
        return "IND.";
    }

    @Override
    public Optional<TypedValue> resolve(String factorKey, DecisionContext ctx) {
        if (!factorKey.startsWith("IND.")) {
            return Optional.empty();
        }

        // 提取指标代码（如 "IND.RSI_14" -> "RSI_14"）
        String indicatorCode = factorKey.substring(4);

        IndicatorSnapshot indicator = ctx.getIndicatorSnapshot();
        if (indicator == null || !indicator.hasValue()) {
            return Optional.empty();
        }

        // 主值匹配
        if (indicatorCode.equals(indicator.getIndicatorCode())) {
            return Optional.of(TypedValue.ofNumber(indicator.getValue()));
        }

        // 扩展值匹配（如 "IND.MACD.SIGNAL"）
        if (indicator.getExtraValues() != null) {
            BigDecimal extraValue = indicator.getExtraValues().get(indicatorCode);
            if (extraValue != null) {
                return Optional.of(TypedValue.ofNumber(extraValue));
            }
        }

        return Optional.empty();
    }
}

