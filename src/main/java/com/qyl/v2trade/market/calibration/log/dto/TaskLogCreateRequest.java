package com.qyl.v2trade.market.calibration.log.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建任务执行日志请求DTO
 */
@Data
public class TaskLogCreateRequest {

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
}

