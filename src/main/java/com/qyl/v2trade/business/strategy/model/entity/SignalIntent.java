package com.qyl.v2trade.business.strategy.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 信号意图实体
 * 
 * <p>映射 signal_intent 表
 * 
 * <p>职责：
 * <ul>
 *   <li>记录策略订阅的信号意图（LATEST_ONLY模型）</li>
 *   <li>用于策略决策时的信号读取</li>
 * </ul>
 */
@Data
@TableName("signal_intent")
public class SignalIntent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 意图ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 策略ID
     */
    private Long strategyId;

    /**
     * 交易对ID
     */
    private Long tradingPairId;

    /**
     * 信号唯一ID（如TV alert id）
     */
    private String signalId;

    /**
     * 意图方向：BUY / SELL / FLAT / REVERSE
     */
    private String intentDirection;

    /**
     * 意图状态：ACTIVE / CONSUMED / EXPIRED / IGNORED
     */
    private String intentStatus;

    /**
     * 信号产生时间
     */
    private LocalDateTime generatedAt;

    /**
     * 系统接收时间
     */
    private LocalDateTime receivedAt;

    /**
     * 失效时间
     */
    private LocalDateTime expiredAt;

    /**
     * 备注/调试信息
     */
    private String remark;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

