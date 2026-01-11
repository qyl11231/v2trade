package com.qyl.v2trade.business.strategy.decision.logic.condition;

import com.qyl.v2trade.business.strategy.decision.context.DecisionContext;

import java.util.Optional;

/**
 * 因子解析器接口
 * 
 * <p>职责：
 * <ul>
 *   <li>解析因子值（从DecisionContext中提取）</li>
 *   <li>支持白名单机制（通过prefix）</li>
 * </ul>
 */
public interface FactorResolver {
    
    /**
     * 解析因子值
     * 
     * @param factorKey 因子key（如 "IND.RSI_14"）
     * @param ctx 决策上下文
     * @return 因子值（如果不存在返回Optional.empty()）
     */
    Optional<TypedValue> resolve(String factorKey, DecisionContext ctx);
    
    /**
     * 支持的因子前缀（用于白名单校验）
     * 
     * @return 前缀（如 "IND."）
     */
    String getSupportedPrefix();
}

