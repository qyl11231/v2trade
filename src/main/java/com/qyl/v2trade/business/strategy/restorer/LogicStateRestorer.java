package com.qyl.v2trade.business.strategy.restorer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qyl.v2trade.business.strategy.mapper.StrategyLogicStateMapper;
import com.qyl.v2trade.business.strategy.model.entity.StrategyLogicState;
import com.qyl.v2trade.common.constants.LogicDirectionEnum;
import com.qyl.v2trade.common.constants.LogicPhaseEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 逻辑状态恢复器
 * 
 * <p>职责：
 * <ul>
 *   <li>恢复或初始化策略逻辑状态</li>
 *   <li>保证 1 strategy + 1 pair = 1 state</li>
 *   <li>状态不存在时创建默认状态并立即写回数据库</li>
 * </ul>
 * 
 * <p>阶段1约束：
 * <ul>
 *   <li>状态恢复必须幂等（多次调用结果一致）</li>
 *   <li>状态变更立即持久化</li>
 *   <li>不修改配置，不生成决策</li>
 * </ul>
 * 
 * <p>默认状态：
 * <ul>
 *   <li>logic_position_side = FLAT</li>
 *   <li>logic_position_qty = 0</li>
 *   <li>state_phase = IDLE</li>
 *   <li>last_signal_intent_id = null</li>
 * </ul>
 * 
 * @author 开发团队
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogicStateRestorer {

    private final StrategyLogicStateMapper logicStateMapper;

    /**
     * 恢复或初始化策略逻辑状态
     * 
     * <p>恢复逻辑：
     * <ol>
     *   <li>查询 strategy_logic_state</li>
     *   <li>如果存在，直接返回</li>
     *   <li>如果不存在，创建默认状态</li>
     *   <li>立即写回数据库</li>
     *   <li>返回状态</li>
     * </ol>
     * 
     * <p>幂等性保证：
     * <ul>
     *   <li>多次调用结果一致</li>
     *   <li>不会重复创建状态</li>
     * </ul>
     * 
     * @param userId 用户ID（用于创建新状态）
     * @param strategyId 策略ID
     * @param tradingPairId 交易对ID
     * @return 策略逻辑状态（已持久化）
     * @throws IllegalArgumentException 如果参数无效
     */
    public StrategyLogicState restoreOrInit(Long userId, Long strategyId, Long tradingPairId) {
        // 参数校验
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException(
                String.format("用户ID无效: %s，必须大于0", userId)
            );
        }

        if (strategyId == null || strategyId <= 0) {
            throw new IllegalArgumentException(
                String.format("策略ID无效: %s，必须大于0", strategyId)
            );
        }

        if (tradingPairId == null || tradingPairId <= 0) {
            throw new IllegalArgumentException(
                String.format("交易对ID无效: %s，必须大于0", tradingPairId)
            );
        }

        log.debug("开始恢复策略状态: strategyId={}, tradingPairId={}", strategyId, tradingPairId);

        try {
            // 1. 查询现有状态
            StrategyLogicState state = logicStateMapper.selectOne(
                new LambdaQueryWrapper<StrategyLogicState>()
                    .eq(StrategyLogicState::getStrategyId, strategyId)
                    .eq(StrategyLogicState::getTradingPairId, tradingPairId)
            );

            if (state != null) {
                // 2. 状态存在，直接返回
                log.debug("策略状态已存在，直接返回: strategyId={}, tradingPairId={}, phase={}",
                    strategyId, tradingPairId, state.getStatePhase());
                return state;
            }

            // 3. 状态不存在，创建默认状态
            log.info("策略状态不存在，创建默认状态: strategyId={}, tradingPairId={}",
                strategyId, tradingPairId);

            state = createDefaultState(userId, strategyId, tradingPairId);

            // 4. 立即写回数据库（防止中途崩溃导致状态丢失）
            logicStateMapper.insert(state);

            log.info("策略状态初始化完成: strategyId={}, tradingPairId={}, stateId={}",
                strategyId, tradingPairId, state.getId());

            return state;

        } catch (Exception e) {
            log.error("恢复策略状态失败: strategyId={}, tradingPairId={}",
                strategyId, tradingPairId, e);
            throw new RuntimeException(
                String.format("恢复策略状态失败: strategyId=%s, tradingPairId=%s",
                    strategyId, tradingPairId), e
            );
        }
    }

    /**
     * 创建默认状态
     * 
     * <p>默认值：
     * <ul>
     *   <li>logic_position_side = FLAT</li>
     *   <li>logic_position_qty = 0</li>
     *   <li>avg_entry_price = null</li>
     *   <li>state_phase = IDLE</li>
     *   <li>last_signal_intent_id = null</li>
     *   <li>unrealized_pnl = null</li>
     *   <li>realized_pnl = null</li>
     * </ul>
     * 
     * @param userId 用户ID
     * @param strategyId 策略ID
     * @param tradingPairId 交易对ID
     * @return 默认状态对象（未持久化）
     */
    private StrategyLogicState createDefaultState(Long userId, Long strategyId, Long tradingPairId) {
        StrategyLogicState state = new StrategyLogicState();
        
        state.setUserId(userId);
        
        state.setStrategyId(strategyId);
        state.setTradingPairId(tradingPairId);
        
        // 设置默认值
        state.setLogicPositionSideEnum(LogicDirectionEnum.FLAT);
        state.setLogicPositionQty(BigDecimal.ZERO);
        state.setAvgEntryPrice(null);
        state.setStatePhaseEnum(LogicPhaseEnum.IDLE);
        state.setLastSignalIntentId(null);
        state.setUnrealizedPnl(null);
        state.setRealizedPnl(null);

        return state;
    }
}

