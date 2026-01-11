package com.qyl.v2trade.business.strategy.decision.guard;

import com.qyl.v2trade.business.strategy.decision.context.DecisionContext;

/**
 * 门禁接口
 * 
 * <p>职责：
 * <ul>
 *   <li>定义门禁的校验逻辑</li>
 *   <li>返回校验结果（允许/拒绝）</li>
 * </ul>
 * 
 * <p>阶段2约束：
 * <ul>
 *   <li>只读校验，不修改数据</li>
 *   <li>校验失败时返回拒绝原因</li>
 * </ul>
 */
public interface Gate {

    /**
     * 校验决策上下文
     * 
     * @param ctx 决策上下文
     * @return 校验结果
     */
    GuardResult check(DecisionContext ctx);

    /**
     * 获取门禁名称（用于日志和metrics）
     * 
     * @return 门禁名称
     */
    String getName();
}

