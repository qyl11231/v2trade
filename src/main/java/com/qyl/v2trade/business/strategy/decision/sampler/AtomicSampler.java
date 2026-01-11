package com.qyl.v2trade.business.strategy.decision.sampler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qyl.v2trade.business.strategy.decision.context.DecisionContext;
import com.qyl.v2trade.business.strategy.decision.context.snapshot.*;
import com.qyl.v2trade.business.strategy.decision.event.*;
import com.qyl.v2trade.business.strategy.factory.model.StrategyInstance;
import com.qyl.v2trade.business.strategy.mapper.*;
import com.qyl.v2trade.business.strategy.model.entity.*;
import com.qyl.v2trade.common.constants.DecisionTriggerTypeEnum;
import com.qyl.v2trade.indicator.domain.event.BarClosedEvent;
import com.qyl.v2trade.indicator.repository.IndicatorValueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 原子采样器
 * 
 * <p>职责：
 * <ul>
 *   <li>在决策时点采集所有必要的上下文数据</li>
 *   <li>保证数据在同一时间点采集（原子性）</li>
 *   <li>创建不可变的DecisionContext</li>
 * </ul>
 * 
 * <p>阶段2约束：
 * <ul>
 *   <li>只读操作，不修改任何数据</li>
 *   <li>不缓存数据，每次决策都重新采集</li>
 *   <li>允许部分数据为空（在GuardChain中校验）</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AtomicSampler {

    private final StrategyParamMapper paramMapper;
    private final SignalIntentMapper signalIntentMapper;
    private final StrategyDefinitionMapper definitionMapper;
    private final IndicatorValueRepository indicatorValueRepository;
    private final PriceSnapshotLoader priceSnapshotLoader;

    /**
     * 采样决策上下文（信号触发）
     * 
     * @param instance 策略实例
     * @param event 信号意图激活事件
     * @return 决策上下文
     */
    public DecisionContext sample(StrategyInstance instance, SignalIntentActivatedEvent event) {
        log.debug("开始采样决策上下文（信号触发）: strategyId={}, tradingPairId={}",
            instance.getStrategyId(), instance.getTradingPairId());

        LocalDateTime now = LocalDateTime.now();

        // 1. 读取逻辑状态
        LogicStateSnapshot logicState = loadLogicState(instance);

        // 2. 读取策略参数
        ParamSnapshot param = loadParam(instance);

        // 3. 读取策略定义（获取strategyType）
        String strategyType = loadStrategyType(instance.getStrategyId());

        // 4. 读取信号快照（从事件中获取，但需要验证时效）
        SignalSnapshot signal = buildSignalSnapshot(event);

        // 5. 读取指标快照（如果有订阅）
        IndicatorSnapshot indicator = loadIndicatorSnapshot(instance, null);

        // 6. K线快照为空（信号触发不需要K线）

        // 7. 读取价格快照
        PriceSnapshot price = priceSnapshotLoader.load(instance.getTradingPairId());

        return DecisionContext.builder()
            .userId(instance.getLogicState().getUserId())
            .strategyId(instance.getStrategyId())
            .tradingPairId(instance.getTradingPairId())
            .strategyType(strategyType)
            .triggerType(DecisionTriggerTypeEnum.SIGNAL)
            .triggeredAt(now)
            .logicStateBefore(logicState)
            .paramSnapshot(param)
            .signalSnapshot(signal)
            .indicatorSnapshot(indicator)
            .barSnapshot(null)
            .priceSnapshot(price)
            .triggerEvent(event)
            .build();
    }

    /**
     * 采样决策上下文（指标触发）
     * 
     * @param instance 策略实例
     * @param event 指标计算完成事件
     * @return 决策上下文
     */
    public DecisionContext sample(StrategyInstance instance, IndicatorComputedEvent event) {
        log.debug("开始采样决策上下文（指标触发）: strategyId={}, tradingPairId={}, indicatorCode={}",
            instance.getStrategyId(), instance.getTradingPairId(), event.getIndicatorCode());

        LocalDateTime now = LocalDateTime.now();

        // 1. 读取逻辑状态
        LogicStateSnapshot logicState = loadLogicState(instance);

        // 2. 读取策略参数
        ParamSnapshot param = loadParam(instance);

        // 3. 读取策略定义
        String strategyType = loadStrategyType(instance.getStrategyId());

        // 4. 读取信号快照（LATEST_ONLY）
        SignalSnapshot signal = loadSignalSnapshot(instance);

        // 5. 读取指标快照（从事件中构建）
        IndicatorSnapshot indicator = buildIndicatorSnapshot(event);

        // 6. K线快照为空

        // 7. 读取价格快照
        PriceSnapshot price = priceSnapshotLoader.load(instance.getTradingPairId());

        return DecisionContext.builder()
            .userId(instance.getLogicState().getUserId())
            .strategyId(instance.getStrategyId())
            .tradingPairId(instance.getTradingPairId())
            .strategyType(strategyType)
            .triggerType(DecisionTriggerTypeEnum.INDICATOR)
            .triggeredAt(now)
            .logicStateBefore(logicState)
            .paramSnapshot(param)
            .signalSnapshot(signal)
            .indicatorSnapshot(indicator)
            .barSnapshot(null)
            .priceSnapshot(price)
            .triggerEvent(event)
            .build();
    }

    /**
     * 采样决策上下文（K线触发）
     * 
     * @param instance 策略实例
     * @param event K线闭合事件
     * @return 决策上下文
     */
    public DecisionContext sample(StrategyInstance instance, BarClosedEvent event) {
        log.debug("开始采样决策上下文（K线触发）: strategyId={}, tradingPairId={}, timeframe={}",
            instance.getStrategyId(), instance.getTradingPairId(), event.timeframe());

        LocalDateTime now = LocalDateTime.now();

        // 1. 读取逻辑状态
        LogicStateSnapshot logicState = loadLogicState(instance);

        // 2. 读取策略参数
        ParamSnapshot param = loadParam(instance);

        // 3. 读取策略定义
        String strategyType = loadStrategyType(instance.getStrategyId());

        // 4. 读取信号快照
        SignalSnapshot signal = loadSignalSnapshot(instance);

        // 5. 读取指标快照（如果有订阅）
        IndicatorSnapshot indicator = loadIndicatorSnapshot(instance, event.timeframe());

        // 6. 构建K线快照
        BarSnapshot bar = buildBarSnapshot(event);

        // 7. 读取价格快照
        PriceSnapshot price = priceSnapshotLoader.load(instance.getTradingPairId());

        return DecisionContext.builder()
            .userId(instance.getLogicState().getUserId())
            .strategyId(instance.getStrategyId())
            .tradingPairId(instance.getTradingPairId())
            .strategyType(strategyType)
            .triggerType(DecisionTriggerTypeEnum.BAR)
            .triggeredAt(now)
            .logicStateBefore(logicState)
            .paramSnapshot(param)
            .signalSnapshot(signal)
            .indicatorSnapshot(indicator)
            .barSnapshot(bar)
            .priceSnapshot(price)
            .triggerEvent(event)
            .build();
    }

    /**
     * 采样决策上下文（价格触发）
     * 
     * @param instance 策略实例
     * @param event 价格阈值穿越事件
     * @return 决策上下文
     */
    public DecisionContext sample(StrategyInstance instance, PriceTriggeredDecisionEvent event) {
        log.debug("开始采样决策上下文（价格触发）: strategyId={}, tradingPairId={}, triggerType={}",
            instance.getStrategyId(), instance.getTradingPairId(), event.getTriggerType());

        LocalDateTime now = LocalDateTime.now();

        // 1. 读取逻辑状态
        LogicStateSnapshot logicState = loadLogicState(instance);

        // 2. 读取策略参数
        ParamSnapshot param = loadParam(instance);

        // 3. 读取策略定义
        String strategyType = loadStrategyType(instance.getStrategyId());

        // 4. 读取信号快照
        SignalSnapshot signal = loadSignalSnapshot(instance);

        // 5. 读取指标快照（如果有订阅）
        IndicatorSnapshot indicator = loadIndicatorSnapshot(instance, null);

        // 6. K线快照为空

        // 7. 构建价格快照（从事件中获取）
        PriceSnapshot price = PriceSnapshot.builder()
            .currentPrice(event.getCurrentPrice())
            .priceTime(event.getTriggeredAt())
            .source("PRICE_TRIGGER")
            .build();

        return DecisionContext.builder()
            .userId(instance.getLogicState().getUserId())
            .strategyId(instance.getStrategyId())
            .tradingPairId(instance.getTradingPairId())
            .strategyType(strategyType)
            .triggerType(DecisionTriggerTypeEnum.PRICE)
            .triggeredAt(now)
            .logicStateBefore(logicState)
            .paramSnapshot(param)
            .signalSnapshot(signal)
            .indicatorSnapshot(indicator)
            .barSnapshot(null)
            .priceSnapshot(price)
            .triggerEvent(event)
            .build();
    }

    // ========== 私有方法：数据加载 ==========

    /**
     * 加载逻辑状态快照
     */
    private LogicStateSnapshot loadLogicState(StrategyInstance instance) {
        // 从instance中获取（已在阶段1加载）
        StrategyLogicState state = instance.getLogicState();
        if (state == null) {
            log.warn("逻辑状态为空: strategyId={}, tradingPairId={}",
                instance.getStrategyId(), instance.getTradingPairId());
            return null;
        }

        return LogicStateSnapshot.builder()
            .logicPositionSide(state.getLogicPositionSide())
            .logicPositionQty(state.getLogicPositionQty())
            .avgEntryPrice(state.getAvgEntryPrice())
            .statePhase(state.getStatePhase())
            .lastSignalIntentId(state.getLastSignalIntentId())
            .unrealizedPnl(state.getUnrealizedPnl())
            .realizedPnl(state.getRealizedPnl())
            .build();
    }

    /**
     * 加载策略参数快照
     */
    private ParamSnapshot loadParam(StrategyInstance instance) {
        // 从StrategyRuntime中获取（已在阶段1加载）
        // 这里需要从数据库重新读取，确保是最新的
        StrategyParam param = paramMapper.selectOne(
            new LambdaQueryWrapper<StrategyParam>()
                .eq(StrategyParam::getStrategyId, instance.getStrategyId())
                .last("LIMIT 1")
        );

        if (param == null) {
            log.warn("策略参数不存在: strategyId={}", instance.getStrategyId());
            return null;
        }

        return ParamSnapshot.builder()
            .initialCapital(param.getInitialCapital())
            .baseOrderRatio(param.getBaseOrderRatio())
            .takeProfitRatio(param.getTakeProfitRatio())
            .stopLossRatio(param.getStopLossRatio())
            .entryCondition(param.getEntryCondition())
            .exitCondition(param.getExitCondition())
            .extraParams(null)  // StrategyParam表中没有此字段
            .build();
    }

    /**
     * 加载策略类型
     */
    private String loadStrategyType(Long strategyId) {
        StrategyDefinition definition = definitionMapper.selectById(strategyId);
        if (definition == null) {
            log.warn("策略定义不存在: strategyId={}", strategyId);
            return null;
        }
        return definition.getStrategyType();
    }

    /**
     * 加载信号快照（LATEST_ONLY）
     */
    private SignalSnapshot loadSignalSnapshot(StrategyInstance instance) {
        // 查询最新的ACTIVE信号
        SignalIntent signalIntent = signalIntentMapper.selectOne(
            new LambdaQueryWrapper<SignalIntent>()
                .eq(SignalIntent::getStrategyId, instance.getStrategyId())
                .eq(SignalIntent::getTradingPairId, instance.getTradingPairId())
                .eq(SignalIntent::getIntentStatus, "ACTIVE")
                .orderByDesc(SignalIntent::getReceivedAt)
                .last("LIMIT 1")
        );

        if (signalIntent == null) {
            return null;
        }

        return SignalSnapshot.builder()
            .signalIntentId(signalIntent.getId())
            .signalId(signalIntent.getSignalId())
            .intentDirection(signalIntent.getIntentDirection())
            .intentStatus(signalIntent.getIntentStatus())
            .generatedAt(signalIntent.getGeneratedAt())
            .receivedAt(signalIntent.getReceivedAt())
            .expiredAt(signalIntent.getExpiredAt())
            .build();
    }

    /**
     * 从事件构建信号快照
     */
    private SignalSnapshot buildSignalSnapshot(SignalIntentActivatedEvent event) {
        return SignalSnapshot.builder()
            .signalIntentId(event.getSignalIntentId())
            .signalId(event.getSignalId())
            .intentDirection(event.getIntentDirection())
            .intentStatus("ACTIVE")  // 事件触发时肯定是ACTIVE
            .generatedAt(null)  // 事件中没有，需要从数据库查询
            .receivedAt(event.getActivatedAt())
            .expiredAt(null)
            .build();
    }

    /**
     * 加载指标快照（最新值）
     */
    private IndicatorSnapshot loadIndicatorSnapshot(StrategyInstance instance, String timeframe) {
        // TODO: 需要知道策略订阅了哪些指标
        // 当前实现：返回null，后续在GuardChain中校验
        // 如果是指标触发的事件，指标值会从事件中获取
        return null;
    }

    /**
     * 从事件构建指标快照
     */
    private IndicatorSnapshot buildIndicatorSnapshot(IndicatorComputedEvent event) {
        return IndicatorSnapshot.builder()
            .indicatorCode(event.getIndicatorCode())
            .indicatorVersion(event.getIndicatorVersion())
            .timeframe(null)  // 事件中没有，需要从数据库查询
            .barTime(event.getBarTime())
            .value(null)  // 事件中是Map，需要提取主值
            .extraValues(event.getIndicatorValues())
            .dataQuality(null)
            .computedAt(event.getComputedAt())
            .build();
    }

    /**
     * 构建K线快照
     */
    private BarSnapshot buildBarSnapshot(BarClosedEvent event) {
        // BarClosedEvent.barCloseTime 是 LocalDateTime (UTC)，需要转换为 Instant
        java.time.Instant barCloseTime = event.barCloseTime().atZone(java.time.ZoneId.of("UTC")).toInstant();
        // BarClosedEvent.eventTime 是 LocalDateTime，需要转换为 Instant
        java.time.Instant eventTime = event.eventTime() != null 
            ? event.eventTime().atZone(java.time.ZoneId.of("UTC")).toInstant() 
            : java.time.Instant.now();
        
        return BarSnapshot.builder()
            .timeframe(event.timeframe())
            .barCloseTime(barCloseTime)
            .open(event.open())
            .high(event.high())
            .low(event.low())
            .close(event.close())
            .volume(event.volume())
            .sourceCount(event.sourceCount())
            .eventTime(eventTime)
            .build();
    }
}

