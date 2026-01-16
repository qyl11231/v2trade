package com.qyl.v2trade.business.strategy.runtime.state;

import com.qyl.v2trade.business.strategy.mapper.StrategyLogicStateMapper;
import com.qyl.v2trade.business.strategy.model.entity.StrategyInstance;
import com.qyl.v2trade.business.strategy.model.entity.StrategyLogicState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 策略状态持久化仓库
 * 
 * <p>负责状态的加载、初始化和持久化
 * 
 * <p>【硬约束】：只有在关键字段变化时才写库（避免 PRICE 高频写库）
 *
 * @author qyl
 */
@Component
public class StrategyStateRepository {
    
    private static final Logger log = LoggerFactory.getLogger(StrategyStateRepository.class);
    
    @Autowired
    private StrategyLogicStateMapper stateMapper;
    
    /**
     * 加载或初始化状态（用于启动恢复）
     * 
     * @param instance 策略实例
     * @return 状态对象（内存模型）
     */
    public StrategyState loadOrInit(StrategyInstance instance) {
        // 1. 先查 DB
        StrategyLogicState dbState = stateMapper.selectByInstanceId(instance.getId());
        
        if (dbState == null) {
            // 2. 如果没有，插入初始状态
            log.info("实例状态不存在，创建初始状态: instanceId={}", instance.getId());
            StrategyLogicState initState = createInitialState(instance);
            stateMapper.insert(initState);
            dbState = initState;
        } else {
            log.debug("恢复实例状态: instanceId={}, phase={}, side={}, qty={}", 
                instance.getId(), dbState.getStatePhase(), dbState.getLogicPositionSide(), 
                dbState.getLogicPositionQty());
        }
        
        // 3. 转换为内存模型
        StrategyState state = convertToState(dbState);
        state.setPersistedStateHash(state.computeStateHash());
        
        return state;
    }
    
    /**
     * 持久化状态（只在变化时写）
     * 
     * 【硬约束】：只有以下字段变化才写库：
     * - logic_position_side
     * - logic_position_qty
     * - avg_entry_price
     * - state_phase
     * 
     * 否则：不写库（避免 updated_at 乱跳）
     * 
     * @param instance 策略实例（用于获取基本信息）
     * @param state 状态对象（内存模型）
     */
    public void persistIfChanged(StrategyInstance instance, StrategyState state) {
        // 1. 判断是否有变化
        if (!state.hasChanged()) {
            return;  // 无变化，不写库
        }
        
        // 2. 转换为 DB 模型
        StrategyLogicState dbState = convertToDbState(instance, state);
        
        // 3. UPSERT（按 instanceId 唯一）
        int rows = stateMapper.upsertState(dbState);
        
        if (rows > 0) {
            // 4. 更新内存的 persistedStateHash
            state.setPersistedStateHash(state.computeStateHash());
            
            log.debug("状态已持久化: instanceId={}, phase={}, side={}, qty={}", 
                instance.getId(), state.getPhase(), state.getPositionSide(), state.getPositionQty());
        } else {
            log.warn("状态持久化失败: instanceId={}", instance.getId());
        }
    }
    
    /**
     * 创建初始状态（IDLE/FLAT/0）
     */
    private StrategyLogicState createInitialState(StrategyInstance instance) {
        StrategyLogicState state = new StrategyLogicState();
        state.setUserId(instance.getUserId());
        state.setStrategyId(instance.getStrategyId());
        state.setStrategyInstanceId(instance.getId());
        state.setTradingPairId(instance.getTradingPairId());
        state.setStrategySymbol(instance.getStrategySymbol());
        state.setLogicPositionSide("FLAT");
        state.setLogicPositionQty(BigDecimal.ZERO);
        state.setAvgEntryPrice(null);
        state.setStatePhase("IDLE");
        state.setUnrealizedPnl(null);  // N3 不写
        state.setRealizedPnl(null);    // N3 不写
        return state;
    }
    
    /**
     * 将 DB 模型转换为内存模型
     */
    private StrategyState convertToState(StrategyLogicState dbState) {
        StrategyState state = new StrategyState();
        state.setPhase(StrategyPhase.fromString(dbState.getStatePhase()));
        state.setPositionSide(dbState.getLogicPositionSide());
        state.setPositionQty(dbState.getLogicPositionQty());
        state.setAvgEntryPrice(dbState.getAvgEntryPrice());
        // lastEventTimeUtc 不持久化，内存维护
        return state;
    }
    
    /**
     * 将内存模型转换为 DB 模型
     */
    private StrategyLogicState convertToDbState(StrategyInstance instance, StrategyState state) {
        StrategyLogicState dbState = new StrategyLogicState();
        dbState.setUserId(instance.getUserId());
        dbState.setStrategyId(instance.getStrategyId());
        dbState.setStrategyInstanceId(instance.getId());
        dbState.setTradingPairId(instance.getTradingPairId());
        dbState.setStrategySymbol(instance.getStrategySymbol());
        dbState.setLogicPositionSide(state.getPositionSide());
        dbState.setLogicPositionQty(state.getPositionQty());
        dbState.setAvgEntryPrice(state.getAvgEntryPrice());
        dbState.setStatePhase(state.getPhase() != null ? state.getPhase().toString() : "IDLE");
        // N3 不写 pnl
        dbState.setUnrealizedPnl(null);
        dbState.setRealizedPnl(null);
        return dbState;
    }
}

