package com.qyl.v2trade.common.constants;

/**
 * 策略逻辑阶段枚举
 * 
 * <p>用于 strategy_logic_state 表的 state_phase 字段
 * 
 * <p>阶段说明：
 * <ul>
 *   <li>IDLE: 空闲状态，无持仓</li>
 *   <li>OPEN_PENDING: 已决策待执行，等待开仓</li>
 *   <li>OPENED: 已开仓，有持仓</li>
 *   <li>PARTIAL_EXIT: 部分减仓</li>
 *   <li>EXIT_PENDING: 已决策待平仓</li>
 *   <li>CLOSED: 已平仓</li>
 * </ul>
 */
public enum LogicPhaseEnum {
    
    /**
     * 空闲（无持仓）
     */
    IDLE("IDLE", "空闲"),
    
    /**
     * 已决策待执行（等待开仓）
     */
    OPEN_PENDING("OPEN_PENDING", "已决策待执行"),
    
    /**
     * 已开仓（有持仓）
     */
    OPENED("OPENED", "已开仓"),
    
    /**
     * 部分减仓
     */
    PARTIAL_EXIT("PARTIAL_EXIT", "部分减仓"),
    
    /**
     * 已决策待平仓
     */
    EXIT_PENDING("EXIT_PENDING", "已决策待平仓"),
    
    /**
     * 已平仓
     */
    CLOSED("CLOSED", "已平仓");

    private final String code;
    private final String description;

    LogicPhaseEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据代码获取枚举
     * 
     * @param code 代码
     * @return 枚举值
     * @throws IllegalArgumentException 如果代码不存在
     */
    public static LogicPhaseEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (LogicPhaseEnum phase : values()) {
            if (phase.code.equals(code)) {
                return phase;
            }
        }
        throw new IllegalArgumentException("Unknown logic phase code: " + code);
    }
}

