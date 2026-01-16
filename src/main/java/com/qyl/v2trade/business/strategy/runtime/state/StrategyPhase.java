package com.qyl.v2trade.business.strategy.runtime.state;

/**
 * 策略阶段枚举
 * 
 * <p>定义策略运行的所有可能阶段
 *
 * @author qyl
 */
public enum StrategyPhase {
    /**
     * 空闲状态（等待）
     */
    IDLE,
    
    /**
     * 已决策待开仓（N6 阶段）
     */
    OPEN_PENDING,
    
    /**
     * 已开仓（持仓中）
     */
    OPENED,
    
    /**
     * 部分减仓（N6/N7 阶段）
     */
    PARTIAL_EXIT,
    
    /**
     * 已决策待平仓（N6 阶段）
     */
    EXIT_PENDING,
    
    /**
     * 部分加仓（N6/N7 阶段）
     */
    ADD_PENDING,
    
    /**
     * 已平仓（N7 阶段）
     */
    CLOSED;
    
    /**
     * 从字符串转换为枚举
     * 
     * @param phase 阶段字符串
     * @return 枚举值，如果不存在则返回 IDLE
     */
    public static StrategyPhase fromString(String phase) {
        if (phase == null || phase.isEmpty()) {
            return IDLE;
        }
        try {
            return valueOf(phase.toUpperCase());
        } catch (IllegalArgumentException e) {
            return IDLE;
        }
    }
    
    /**
     * 转换为字符串（用于持久化）
     * 
     * @return 枚举名称
     */
    @Override
    public String toString() {
        return name();
    }
}

