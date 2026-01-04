package com.qyl.v2trade.business.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 行情订阅配置实体
 * 用于管理哪些交易对需要采集行情数据
 */
@Data
@TableName("market_subscription_config")
public class MarketSubscriptionConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 交易对ID（关联trading_pair表）
     */
    private Long tradingPairId;

    /**
     * 是否启用行情采集：1-启用 0-禁用
     */
    private Integer enabled;

    /**
     * Redis缓存时长（分钟），默认60分钟
     */
    private Integer cacheDurationMinutes;

    /**
     * 备注说明
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

