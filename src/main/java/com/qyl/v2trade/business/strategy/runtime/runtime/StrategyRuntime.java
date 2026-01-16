package com.qyl.v2trade.business.strategy.runtime.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qyl.v2trade.business.signal.model.entity.Signal;
import com.qyl.v2trade.business.strategy.model.entity.StrategyInstance;
import com.qyl.v2trade.business.strategy.runtime.dispatch.StripedSerialExecutor;
import com.qyl.v2trade.business.strategy.runtime.snapshot.LatestBarSnapshot;
import com.qyl.v2trade.business.strategy.runtime.snapshot.LatestPriceSnapshot;
import com.qyl.v2trade.business.strategy.runtime.snapshot.LatestSignalSnapshot;
import com.qyl.v2trade.business.strategy.runtime.state.StrategyState;
import com.qyl.v2trade.business.strategy.runtime.state.StrategyStateMachine;
import com.qyl.v2trade.business.strategy.runtime.state.StrategyStateRepository;
import com.qyl.v2trade.business.strategy.runtime.trigger.StrategyTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 策略运行时
 * 
 * <p>每个 enabled 的 strategy_instance 对应一个 StrategyRuntime
 * 
 * <p>【核心保证】：所有事件处理都通过 StripedSerialExecutor（串行保证）
 *
 * @author qyl
 */
public class StrategyRuntime {
    
    private static final Logger log = LoggerFactory.getLogger(StrategyRuntime.class);
    
    // 实例基本信息（启动时注入，不查 DB）
    private final Long instanceId;
    private final Long userId;
    private final Long strategyId;        // 策略ID（必须保存，用于持久化）
    private final Long tradingPairId;
    private final Long signalConfigId;
    private final String strategySymbol;
    
    // 状态机
    private final StrategyStateMachine stateMachine;
    
    // 快照（只保存最新）
    private final LatestPriceSnapshot latestPrice;
    private final LatestBarSnapshot latestBar;
    private final LatestSignalSnapshot latestSignal;
    
    // 依赖注入（不查 DB）
    private final StrategyStateRepository stateRepo;
    private final StripedSerialExecutor executor;
    private final ObjectMapper objectMapper;
    
    // 处理序号（用于证明串行）
    private final AtomicLong sequenceNumber = new AtomicLong(0);
    
    public StrategyRuntime(StrategyInstance instance, StrategyState initialState,
                          StrategyStateRepository stateRepo, StripedSerialExecutor executor,
                          ObjectMapper objectMapper) {
        this.instanceId = instance.getId();
        this.userId = instance.getUserId();
        this.strategyId = instance.getStrategyId();  // 必须保存，用于持久化状态
        this.tradingPairId = instance.getTradingPairId();
        this.signalConfigId = instance.getSignalConfigId();
        this.strategySymbol = instance.getStrategySymbol();
        
        this.stateMachine = new StrategyStateMachine(initialState);
        this.latestPrice = new LatestPriceSnapshot();
        this.latestBar = new LatestBarSnapshot();
        this.latestSignal = new LatestSignalSnapshot();
        
        this.stateRepo = stateRepo;
        this.executor = executor;
        this.objectMapper = objectMapper != null ? objectMapper : new com.fasterxml.jackson.databind.ObjectMapper();
    }
    
    /**
     * 处理触发事件（入口方法，由 RuntimeManager 调用）
     * 
     * 【核心保证】：通过 StripedSerialExecutor 保证串行
     * 
     * @param trigger 触发事件
     */
    public void onTrigger(StrategyTrigger trigger) {
        executor.execute(instanceId, () -> {
            handle(trigger);
        });
    }
    
    /**
     * 实际处理逻辑（在串行执行器中运行）
     * 
     * 【硬约束】：
     * 1. 不查 DB
     * 2. 不抛异常（所有异常捕获并记录）
     * 3. 只在状态变化时写库
     */
    private void handle(StrategyTrigger trigger) {
        long seq = sequenceNumber.incrementAndGet();
        int stripeId = (int) (instanceId % executor.getStripeCount());
        
        try {
            // 1. 校验事件时间（可选：忽略过旧事件）
            StrategyState currentState = stateMachine.getCurrentState();
            if (shouldIgnore(trigger, currentState)) {
                log.debug("忽略过旧事件: instanceId={}, eventKey={}", instanceId, trigger.getEventKey());
                return;
            }
            
            // 2. 更新快照
            updateSnapshots(trigger);
            
            // 3. 更新状态机（N3 只更新 lastEventTime）
            stateMachine.onTrigger(trigger);
            
            // 4. 持久化状态（只在变化时写）
            currentState = stateMachine.getCurrentState();
            stateRepo.persistIfChanged(createInstanceProxy(), currentState);
            
            // 5. 打印 runtime 日志
            logRuntimeEvent(trigger, seq, stripeId, currentState);
            
        } catch (Exception e) {
            log.error("处理事件失败: instanceId={}, eventKey={}, seq={}", 
                instanceId, trigger.getEventKey(), seq, e);
            // 不抛异常，避免影响其他事件处理
        }
    }
    
    /**
     * 判断是否应该忽略事件（过旧事件）
     */
    private boolean shouldIgnore(StrategyTrigger trigger, StrategyState state) {
        // 可选：如果事件时间比 lastEventTime 早太多（如 1 小时），忽略
        if (state.getLastEventTimeUtc() != null) {
            Duration diff = Duration.between(trigger.getAsOfTimeUtc(), state.getLastEventTimeUtc());
            if (diff.toMinutes() > 60) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 更新快照
     */
    private void updateSnapshots(StrategyTrigger trigger) {
        switch (trigger.getTriggerType()) {
            case PRICE:
                if (trigger.getPrice() != null) {
                    latestPrice.update(trigger.getPrice(), trigger.getAsOfTimeUtc(), "N2_ROUTER");
                }
                break;
            case BAR_CLOSE:
                // 更新 K 线快照（包含完整的 OHLC 数据）
                latestBar.update(
                    trigger.getTimeframe(), 
                    trigger.getBarOpen(),
                    trigger.getBarHigh(),
                    trigger.getBarLow(),
                    trigger.getBarClose(),
                    trigger.getBarVolume(),
                    trigger.getAsOfTimeUtc()
                );
                break;
            case SIGNAL:
                if (trigger.getSignalInfo() != null) {
                    Signal signal = trigger.getSignalInfo();
                    latestSignal.update(
                        trigger.getSignalConfigId(),
                        trigger.getSignalId(),
                        signal.getSignalDirectionHint(),
                        signal.getPrice(),
                        trigger.getAsOfTimeUtc()
                    );
                }
                break;
        }
    }
    
    /**
     * 打印 runtime 日志
     */
    private void logRuntimeEvent(StrategyTrigger trigger, long seq, int stripeId, StrategyState state) {
        // 使用专门的 logger
        Logger logger = LoggerFactory.getLogger("STRATEGY_RUNTIME");
        
        try {
            Map<String, Object> logData = new HashMap<>();
            logData.put("instanceId", instanceId);
            logData.put("userId", userId);
            logData.put("phase", state.getPhase() != null ? state.getPhase().toString() : "NULL");
            logData.put("stripe", stripeId);
            logData.put("seq", seq);
            logData.put("type", trigger.getTriggerType().name());
            logData.put("eventKey", trigger.getEventKey());
            logData.put("asOf", trigger.getAsOfTimeUtc().toString());
            logData.put("pairId", tradingPairId);
            
            // 可选字段
            if (trigger.getTimeframe() != null) {
                logData.put("tf", trigger.getTimeframe());
            }
            
            // 状态摘要
            logData.put("side", state.getPositionSide());
            logData.put("qty", state.getPositionQty());
            if (state.getAvgEntryPrice() != null) {
                logData.put("avgEntry", state.getAvgEntryPrice().toString());
            }
            
            String json = objectMapper.writeValueAsString(logData);
            logger.info("runtime_event {}", json);
        } catch (Exception e) {
            logger.error("序列化 runtime 日志失败: instanceId={}, eventKey={}", 
                instanceId, trigger.getEventKey(), e);
        }
    }
    
    // Getters
    public Long getInstanceId() { 
        return instanceId; 
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public Long getStrategyId() {
        return strategyId;
    }
    
    public Long getTradingPairId() {
        return tradingPairId;
    }
    
    public Long getSignalConfigId() {
        return signalConfigId;
    }
    
    public String getStrategySymbol() {
        return strategySymbol;
    }
    
    public StrategyState getState() { 
        return stateMachine.getCurrentState(); 
    }
    
    public LatestPriceSnapshot getLatestPrice() { 
        return latestPrice; 
    }
    
    public LatestBarSnapshot getLatestBar() { 
        return latestBar; 
    }
    
    public LatestSignalSnapshot getLatestSignal() { 
        return latestSignal; 
    }
    
    /**
     * 创建实例代理（用于持久化，避免持有完整 instance 对象）
     */
    private StrategyInstance createInstanceProxy() {
        // 创建轻量代理对象，只包含持久化所需的字段
        // 避免持有完整的 StrategyInstance 对象（减少内存占用）
        StrategyInstance instance = new StrategyInstance();
        instance.setId(instanceId);
        instance.setUserId(userId);
        instance.setStrategyId(strategyId);  // 使用保存的 strategyId
        instance.setTradingPairId(tradingPairId);
        instance.setSignalConfigId(signalConfigId);
        instance.setStrategySymbol(strategySymbol);
        return instance;
    }
}

