package com.qyl.v2trade.business.strategy.decision.logic;

import com.qyl.v2trade.business.strategy.decision.context.DecisionContext;

/**
 * 策略逻辑接口
 * 
 * <p>职责：
 * <ul>
 *   <li>定义策略逻辑的抽象接口</li>
 *   <li>纯函数设计（无副作用）</li>
 *   <li>根据决策上下文计算决策结果</li>
 * </ul>
 * 
 * <p>阶段2约束：
 * <ul>
 *   <li>纯函数：不修改任何外部状态</li>
 *   <li>只读：不修改DecisionContext</li>
 *   <li>无副作用：不调用外部服务（除日志/metrics）</li>
 * </ul>
 */
public interface StrategyLogic {

    /**
     * 决策计算（纯函数，无副作用）
     * 
     * <p>根据决策上下文计算决策结果
     * 
     * @param ctx 决策上下文（不可变）
     * @return 决策结果
     */
    DecisionResult decide(DecisionContext ctx);

    /**
     * 支持的策略类型
     * 
     * <p>返回该实现类支持的策略类型（SIGNAL_DRIVEN / INDICATOR_DRIVEN / HYBRID）
     * 
     * @return 策略类型
     */
    String getSupportedStrategyType();
}

