package com.qyl.v2trade.business.strategy.decision.logic.condition.impl;

import com.qyl.v2trade.business.strategy.decision.context.DecisionContext;
import com.qyl.v2trade.business.strategy.decision.context.snapshot.LogicStateSnapshot;
import com.qyl.v2trade.business.strategy.decision.context.snapshot.ParamSnapshot;
import com.qyl.v2trade.business.strategy.decision.logic.condition.FactorResolver;
import com.qyl.v2trade.business.strategy.decision.logic.condition.TypedValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * 状态因子解析器实现
 * 
 * <p>支持因子key：
 * <ul>
 *   <li>STATE.POSITION_SIDE：持仓方向（LONG/SHORT/FLAT）</li>
 *   <li>STATE.POSITION_QTY：持仓数量</li>
 *   <li>STATE.STOP_LOSS_PRICE：止损价格（计算值）</li>
 * </ul>
 */
@Slf4j
@Component
public class StateFactorResolver implements FactorResolver {

    @Override
    public String getSupportedPrefix() {
        return "STATE.";
    }

    @Override
    public Optional<TypedValue> resolve(String factorKey, DecisionContext ctx) {
        if (!factorKey.startsWith("STATE.")) {
            return Optional.empty();
        }

        LogicStateSnapshot state = ctx.getLogicStateBefore();
        if (state == null) {
            return Optional.empty();
        }

        String field = factorKey.substring(6);
        switch (field) {
            case "POSITION_SIDE":
                return Optional.of(TypedValue.ofString(state.getLogicPositionSide()));
            case "POSITION_QTY":
                return Optional.of(TypedValue.ofNumber(state.getLogicPositionQty()));
            case "STOP_LOSS_PRICE":
                // 从参数计算止损价
                ParamSnapshot param = ctx.getParamSnapshot();
                if (param != null && state.getAvgEntryPrice() != null && 
                    param.getStopLossRatio() != null) {
                    BigDecimal stopLossPrice = state.getAvgEntryPrice()
                        .multiply(BigDecimal.ONE.subtract(param.getStopLossRatio()));
                    return Optional.of(TypedValue.ofNumber(stopLossPrice));
                }
                return Optional.empty();
            default:
                log.debug("不支持的状态因子字段: field={}", field);
                return Optional.empty();
        }
    }
}

