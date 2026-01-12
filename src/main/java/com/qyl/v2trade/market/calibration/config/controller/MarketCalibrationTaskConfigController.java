package com.qyl.v2trade.market.calibration.config.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qyl.v2trade.common.Result;
import com.qyl.v2trade.market.calibration.config.dto.TaskConfigCreateRequest;
import com.qyl.v2trade.market.calibration.config.dto.TaskConfigQueryRequest;
import com.qyl.v2trade.market.calibration.config.dto.TaskConfigUpdateRequest;
import com.qyl.v2trade.market.calibration.config.dto.TaskConfigVO;
import com.qyl.v2trade.market.calibration.config.dto.TaskExecuteRequest;
import com.qyl.v2trade.market.calibration.config.service.MarketCalibrationTaskConfigService;
import com.qyl.v2trade.market.calibration.executor.dto.TaskExecutionResult;
import com.qyl.v2trade.market.calibration.service.MarketCalibrationExecutionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 行情校准任务配置管理控制器
 */
@RestController
@RequestMapping("/api/market-calibration/config")
public class MarketCalibrationTaskConfigController {

    private static final Logger logger = LoggerFactory.getLogger(MarketCalibrationTaskConfigController.class);

    @Autowired
    private MarketCalibrationTaskConfigService taskConfigService;

    @Autowired
    private MarketCalibrationExecutionService executionService;

    /**
     * 创建任务配置
     */
    @PostMapping
    public Result<TaskConfigVO> create(@Valid @RequestBody TaskConfigCreateRequest request) {
        logger.info("创建任务配置: taskName={}, taskType={}, tradingPairId={}", 
                request.getTaskName(), request.getTaskType(), request.getTradingPairId());

        TaskConfigVO vo = taskConfigService.createTaskConfig(request);
        return Result.success("创建成功", vo);
    }

    /**
     * 更新任务配置
     */
    @PutMapping("/{id}")
    public Result<TaskConfigVO> update(
            @PathVariable Long id,
            @Valid @RequestBody TaskConfigUpdateRequest request) {
        logger.info("更新任务配置: id={}", id);

        TaskConfigVO vo = taskConfigService.updateTaskConfig(id, request);
        return Result.success("更新成功", vo);
    }

    /**
     * 删除任务配置
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        logger.info("删除任务配置: id={}", id);

        taskConfigService.deleteTaskConfig(id);
        return Result.success("删除成功", null);
    }

    /**
     * 查询任务配置列表
     */
    @GetMapping
    public Result<IPage<TaskConfigVO>> list(TaskConfigQueryRequest request) {
        logger.debug("查询任务配置列表: taskType={}, tradingPairId={}, executionMode={}, enabled={}", 
                request.getTaskType(), request.getTradingPairId(), request.getExecutionMode(), request.getEnabled());

        IPage<TaskConfigVO> page = taskConfigService.listTaskConfigs(request);
        return Result.success(page);
    }

    /**
     * 查询任务配置详情
     */
    @GetMapping("/{id}")
    public Result<TaskConfigVO> getById(@PathVariable Long id) {
        logger.debug("查询任务配置详情: id={}", id);

        TaskConfigVO vo = taskConfigService.getTaskConfigById(id);
        return Result.success(vo);
    }

    /**
     * 启用/禁用任务
     */
    @PostMapping("/{id}/toggle")
    public Result<TaskConfigVO> toggleEnabled(
            @PathVariable Long id,
            @RequestBody ToggleRequest request) {
        logger.info("启用/禁用任务: id={}, enabled={}", id, request.getEnabled());

        if (request.getEnabled() == null) {
            return Result.error(400, "enabled参数不能为空");
        }

        TaskConfigVO vo = taskConfigService.toggleEnabled(id, request.getEnabled());
        return Result.success("操作成功", vo);
    }

    /**
     * 手动执行任务
     * 
     * <p>重构：在 Controller 层将 LocalDateTime 转换为 Instant，遵循 UTC Everywhere 原则
     */
    @PostMapping("/{id}/execute")
    public Result<TaskExecutionResult> execute(
            @PathVariable Long id,
            @Valid @RequestBody TaskExecuteRequest request) {
        logger.info("手动执行任务: id={}, startTime={}, endTime={}", id, request.getStartTime(), request.getEndTime());

        // 重构：将 LocalDateTime 转换为 Instant（UTC）
        // 假设前端传入的 LocalDateTime 是 UTC 时间（无时区信息），直接转换为 Instant
        java.time.Instant startTime = request.getStartTime().atZone(java.time.ZoneOffset.UTC).toInstant();
        java.time.Instant endTime = request.getEndTime().atZone(java.time.ZoneOffset.UTC).toInstant();

        TaskExecutionResult result = executionService.executeTask(id, startTime, endTime);
        return Result.success("执行成功", result);
    }

    /**
     * 启用/禁用请求DTO
     */
    public static class ToggleRequest {
        private Boolean enabled;

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }
    }
}

