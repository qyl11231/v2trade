package com.qyl.v2trade.common.constants;

/**
 * 策略逻辑持仓方向枚举
 * 
 * <p>用于 strategy_logic_state 表的 logic_position_side 字段
 */
public enum LogicDirectionEnum {
    
    /**
     * 做多
     */
    LONG("LONG", "做多"),
    
    /**
     * 做空
     */
    SHORT("SHORT", "做空"),
    
    /**
     * 空仓
     */
    FLAT("FLAT", "空仓");

    private final String code;
    private final String description;

    LogicDirectionEnum(String code, String description) {
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
    public static LogicDirectionEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (LogicDirectionEnum direction : values()) {
            if (direction.code.equals(code)) {
                return direction;
            }
        }
        throw new IllegalArgumentException("Unknown logic direction code: " + code);
    }
}

