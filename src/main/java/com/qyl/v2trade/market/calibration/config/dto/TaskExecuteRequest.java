package com.qyl.v2trade.market.calibration.config.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务执行请求DTO
 * 注意：不使用@JsonFormat，让Jackson使用默认的ISO格式解析（yyyy-MM-ddTHH:mm:ss）
 * 前端需要发送ISO格式的时间字符串
 */
@Data
public class TaskExecuteRequest {

    /**
     * 开始时间（ISO格式：yyyy-MM-ddTHH:mm:ss）
     */
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    /**
     * 结束时间（ISO格式：yyyy-MM-ddTHH:mm:ss）
     */
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;
}

