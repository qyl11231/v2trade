package com.qyl.v2trade.market.calibration.config.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务配置视图对象
 */
@Data
public class TaskConfigVO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 任务类型
     */
    private String taskType;

    /**
     * 交易对ID
     */
    private Long tradingPairId;

    /**
     * 交易对符号（如：BTC-USDT）
     */
    private String symbol;

    /**
     * 市场类型（如：SWAP）
     */
    private String marketType;

    /**
     * 执行模式
     */
    private String executionMode;

    /**
     * 自动模式：检测周期（小时）
     */
    private Integer intervalHours;

    /**
     * 手动模式：开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * 手动模式：结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /**
     * 是否启用：1-启用 0-禁用
     */
    private Integer enabled;

    /**
     * 备注说明
     */
    private String remark;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}

