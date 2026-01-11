package com.qyl.v2trade.business.strategy.decision.recorder;

import com.qyl.v2trade.business.strategy.decision.context.DecisionContext;
import com.qyl.v2trade.business.strategy.decision.logic.DecisionResult;
import com.qyl.v2trade.business.strategy.mapper.StrategyIntentRecordMapper;
import com.qyl.v2trade.business.strategy.model.entity.StrategyIntentRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 决策记录落库器
 * 
 * <p>职责：
 * <ul>
 *   <li>将决策结果写入strategy_intent_record表</li>
 *   <li>只写入动作意图（OPEN/CLOSE/ADD/REDUCE/REVERSE）</li>
 *   <li>HOLD不落库（只记录metrics/log）</li>
 * </ul>
 * 
 * <p>阶段2约束：
 * <ul>
 *   <li>只INSERT，不允许UPDATE/DELETE</li>
 *   <li>只写入动作意图</li>
 *   <li>decision_reason必须是结构化JSON</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IntentRecorder {

    private final StrategyIntentRecordMapper intentRecordMapper;
    private final DecisionReasonBuilder reasonBuilder;

    /**
     * 记录决策结果
     * 
     * @param ctx 决策上下文
     * @param result 决策结果
     */
    public void record(DecisionContext ctx, DecisionResult result) {
        // 只记录动作意图
        if (!result.isActionIntent()) {
            log.debug("决策结果为HOLD，不落库: strategyId={}, tradingPairId={}",
                ctx.getStrategyId(), ctx.getTradingPairId());
            return;
        }

        try {
            // 构建决策记录
            StrategyIntentRecord record = new StrategyIntentRecord();
            record.setUserId(ctx.getUserId());
            record.setStrategyId(ctx.getStrategyId());
            record.setTradingPairId(ctx.getTradingPairId());

            // 设置信号ID（如果由信号触发）
            if (ctx.getSignalSnapshot() != null) {
                record.setSignalId(ctx.getSignalSnapshot().getSignalIntentId());
            }

            // 设置决策意图和数量
            record.setIntentActionEnum(result.getAction());
            record.setCalculatedQty(result.getCalculatedQty());

            // 构建决策原因JSON
            String decisionReason = reasonBuilder.build(ctx, result.getReason());
            record.setDecisionReason(decisionReason);

            // 设置决策时间
            record.setCreatedAt(LocalDateTime.now());

            // 写入数据库
            int inserted = intentRecordMapper.insert(record);
            if (inserted > 0) {
                log.info("决策记录已落库: strategyId={}, tradingPairId={}, action={}, qty={}, recordId={}",
                    ctx.getStrategyId(), ctx.getTradingPairId(), result.getAction(),
                    result.getCalculatedQty(), record.getId());
            } else {
                log.warn("决策记录落库失败: strategyId={}, tradingPairId={}, action={}",
                    ctx.getStrategyId(), ctx.getTradingPairId(), result.getAction());
            }

        } catch (Exception e) {
            log.error("决策记录落库异常: strategyId={}, tradingPairId={}, action={}",
                ctx.getStrategyId(), ctx.getTradingPairId(), result.getAction(), e);
            // 不抛异常，允许继续处理其他决策
        }
    }
}

