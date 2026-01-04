package com.qyl.v2trade.market.calibration.config.dto;

import lombok.Data;

/**
 * 查询任务配置请求DTO
 */
@Data
public class TaskConfigQueryRequest {

    /**
     * 任务类型（可选）
     */
    private String taskType;

    /**
     * 交易对ID（可选）
     */
    private Long tradingPairId;

    /**
     * 执行模式（可选）
     */
    private String executionMode;

    /**
     * 是否启用（可选）
     */
    private Integer enabled;

    /**
     * 当前页码（默认1）
     */
    private Integer current = 1;

    /**
     * 每页数量（默认10）
     */
    private Integer size = 10;
}

