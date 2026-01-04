package com.qyl.v2trade.market.calibration.config.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建任务配置请求DTO
 */
@Data
public class TaskConfigCreateRequest {

    /**
     * 任务名称
     */
    @NotBlank(message = "任务名称不能为空")
    private String taskName;

    /**
     * 任务类型：MISSING_DATA-缺失数据检测, DATA_VERIFY-数据核对
     */
    @NotBlank(message = "任务类型不能为空")
    private String taskType;

    /**
     * 交易对ID
     */
    @NotNull(message = "交易对ID不能为空")
    private Long tradingPairId;

    /**
     * 执行模式：AUTO-自动, MANUAL-手动
     */
    @NotBlank(message = "执行模式不能为空")
    private String executionMode;

    /**
     * 自动模式：检测周期（小时），如1表示检测最近1小时
     */
    @Min(value = 1, message = "检测周期必须大于0")
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
}

