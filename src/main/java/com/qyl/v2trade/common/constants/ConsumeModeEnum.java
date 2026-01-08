package com.qyl.v2trade.common.constants;

/**
 * 信号消费模式枚举
 * 
 * <p>用于 strategy_signal_subscription 表的 consume_mode 字段
 * 
 * <p>消费模式说明：
 * <ul>
 *   <li>LATEST_ONLY: 只取最新有效信号，不排队消费</li>
 * </ul>
 */
public enum ConsumeModeEnum {
    
    /**
     * 只取最新有效信号
     * 
     * <p>策略只关注当前最新的有效信号意图，不进行历史信号的回放或排队消费
     */
    LATEST_ONLY("LATEST_ONLY", "只取最新有效信号");

    private final String code;
    private final String description;

    ConsumeModeEnum(String code, String description) {
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
    public static ConsumeModeEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ConsumeModeEnum mode : values()) {
            if (mode.code.equals(code)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown consume mode code: " + code);
    }
}

