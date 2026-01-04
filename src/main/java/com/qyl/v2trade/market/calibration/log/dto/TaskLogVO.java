package com.qyl.v2trade.market.calibration.log.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务执行日志视图对象
 */
@Data
public class TaskLogVO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 任务配置ID
     */
    private Long taskConfigId;

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
     * 交易对符号
     */
    private String symbol;

    /**
     * 执行模式
     */
    private String executionMode;

    /**
     * 检测开始时间
     */
    private LocalDateTime detectStartTime;

    /**
     * 检测结束时间
     */
    private LocalDateTime detectEndTime;

    /**
     * 执行状态
     */
    private String status;

    /**
     * 缺失K线数量
     */
    private Integer missingCount;

    /**
     * 补全K线数量
     */
    private Integer filledCount;

    /**
     * 重复数据数量
     */
    private Integer duplicateCount;

    /**
     * 异常数据数量
     */
    private Integer errorCount;

    /**
     * 执行耗时（毫秒）
     */
    private Long executeDurationMs;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 执行日志详情（JSON格式）
     */
    private String executeLog;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}

