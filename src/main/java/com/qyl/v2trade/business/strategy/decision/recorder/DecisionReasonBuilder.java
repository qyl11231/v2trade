package com.qyl.v2trade.business.strategy.decision.recorder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qyl.v2trade.business.strategy.decision.context.DecisionContext;
import com.qyl.v2trade.business.strategy.decision.context.snapshot.*;
import com.qyl.v2trade.business.strategy.decision.logic.DecisionReason;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 决策原因构建器
 * 
 * <p>职责：
 * <ul>
 *   <li>将DecisionReason转换为结构化JSON字符串</li>
 *   <li>包含完整的决策上下文信息</li>
 *   <li>用于回放和审计</li>
 * </ul>
 * 
 * <p>阶段2约束：
 * <ul>
 *   <li>decision_reason必须是结构化JSON</li>
 *   <li>包含触发源、快照摘要、logic_state_before等信息</li>
 * </ul>
 */
@Slf4j
@Component
public class DecisionReasonBuilder {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 构建决策原因JSON
     * 
     * @param ctx 决策上下文
     * @param reason 决策原因
     * @return JSON字符串
     */
    public String build(DecisionContext ctx, DecisionReason reason) {
        try {
            Map<String, Object> json = new HashMap<>();

            // 1. 触发源信息
            Map<String, Object> trigger = new HashMap<>();
            trigger.put("type", ctx.getTriggerType() != null ? ctx.getTriggerType().getCode() : "UNKNOWN");
            trigger.put("source", reason.getTriggerSource());
            trigger.put("timestamp", reason.getTriggerTimestamp());
            json.put("trigger", trigger);

            // 2. 决策前的逻辑状态
            if (ctx.getLogicStateBefore() != null) {
                Map<String, Object> logicStateBefore = new HashMap<>();
                LogicStateSnapshot state = ctx.getLogicStateBefore();
                logicStateBefore.put("phase", state.getStatePhase());
                logicStateBefore.put("positionSide", state.getLogicPositionSide());
                logicStateBefore.put("positionQty", state.getLogicPositionQty());
                logicStateBefore.put("avgEntryPrice", state.getAvgEntryPrice());
                json.put("logicStateBefore", logicStateBefore);
            }

            // 3. 数据快照摘要
            Map<String, Object> snapshots = new HashMap<>();

            // 信号快照
            if (ctx.getSignalSnapshot() != null) {
                SignalSnapshot signal = ctx.getSignalSnapshot();
                Map<String, Object> signalSnapshot = new HashMap<>();
                signalSnapshot.put("intentId", signal.getSignalIntentId());
                signalSnapshot.put("direction", signal.getIntentDirection());
                signalSnapshot.put("activatedAt", signal.getReceivedAt());
                snapshots.put("signal", signalSnapshot);
            }

            // 指标快照
            if (ctx.getIndicatorSnapshot() != null) {
                IndicatorSnapshot indicator = ctx.getIndicatorSnapshot();
                Map<String, Object> indicatorSnapshot = new HashMap<>();
                indicatorSnapshot.put("code", indicator.getIndicatorCode());
                indicatorSnapshot.put("value", indicator.getValue());
                indicatorSnapshot.put("barTime", indicator.getBarTime());
                snapshots.put("indicator", indicatorSnapshot);
            }

            // K线快照
            if (ctx.getBarSnapshot() != null) {
                BarSnapshot bar = ctx.getBarSnapshot();
                Map<String, Object> barSnapshot = new HashMap<>();
                barSnapshot.put("timeframe", bar.getTimeframe());
                barSnapshot.put("closeTime", bar.getBarCloseTime());
                barSnapshot.put("close", bar.getClose());
                snapshots.put("bar", barSnapshot);
            }

            // 价格快照
            if (ctx.getPriceSnapshot() != null && ctx.getPriceSnapshot().isAvailable()) {
                PriceSnapshot price = ctx.getPriceSnapshot();
                Map<String, Object> priceSnapshot = new HashMap<>();
                priceSnapshot.put("current", price.getCurrentPrice());
                priceSnapshot.put("timestamp", price.getPriceTime());
                snapshots.put("price", priceSnapshot);
            }

            json.put("snapshots", snapshots);

            // 4. 参数摘要（可选）
            if (ctx.getParamSnapshot() != null) {
                ParamSnapshot param = ctx.getParamSnapshot();
                Map<String, Object> paramsDigest = new HashMap<>();
                paramsDigest.put("initialCapital", param.getInitialCapital());
                paramsDigest.put("baseOrderRatio", param.getBaseOrderRatio());
                paramsDigest.put("takeProfitRatio", param.getTakeProfitRatio());
                paramsDigest.put("stopLossRatio", param.getStopLossRatio());
                json.put("paramsDigest", paramsDigest);
            }

            // 5. 决策依据和状态变化
            json.put("decisionBasis", reason.getDecisionBasis());
            json.put("stateChange", reason.getStateChange());

            // 6. 转换为JSON字符串
            return objectMapper.writeValueAsString(json);

        } catch (Exception e) {
            log.error("构建决策原因JSON失败: strategyId={}, tradingPairId={}",
                ctx.getStrategyId(), ctx.getTradingPairId(), e);
            // 返回简化版本
            return buildSimpleJson(ctx, reason);
        }
    }

    /**
     * 构建简化JSON（fallback）
     */
    private String buildSimpleJson(DecisionContext ctx, DecisionReason reason) {
        try {
            Map<String, Object> json = new HashMap<>();
            json.put("trigger", Map.of("type", ctx.getTriggerType() != null ? ctx.getTriggerType().getCode() : "UNKNOWN"));
            json.put("reason", reason.getStateChange());
            return objectMapper.writeValueAsString(json);
        } catch (Exception e) {
            log.error("构建简化JSON也失败", e);
            return "{\"error\":\"构建JSON失败\"}";
        }
    }
}

