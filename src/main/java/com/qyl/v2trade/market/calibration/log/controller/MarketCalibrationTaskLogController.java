package com.qyl.v2trade.market.calibration.log.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qyl.v2trade.common.Result;
import com.qyl.v2trade.market.calibration.log.dto.TaskLogVO;
import com.qyl.v2trade.market.calibration.log.entity.MarketCalibrationTaskLog;
import com.qyl.v2trade.market.calibration.log.service.MarketCalibrationTaskLogService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

/**
 * 行情校准任务执行日志控制器
 */
@RestController
@RequestMapping("/api/market-calibration/log")
public class MarketCalibrationTaskLogController {

    @Autowired
    private MarketCalibrationTaskLogService taskLogService;

    /**
     * 查询执行日志列表
     */
    @GetMapping
    public Result<IPage<TaskLogVO>> list(
            @RequestParam(required = false) Long taskConfigId,
            @RequestParam(required = false) Long tradingPairId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "1") Integer current,
            @RequestParam(required = false, defaultValue = "10") Integer size) {

        // 构建查询条件
        LambdaQueryWrapper<MarketCalibrationTaskLog> wrapper = new LambdaQueryWrapper<>();
        if (taskConfigId != null) {
            wrapper.eq(MarketCalibrationTaskLog::getTaskConfigId, taskConfigId);
        }
        if (tradingPairId != null) {
            wrapper.eq(MarketCalibrationTaskLog::getTradingPairId, tradingPairId);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(MarketCalibrationTaskLog::getStatus, status);
        }
        wrapper.orderByDesc(MarketCalibrationTaskLog::getCreatedAt);

        // 分页查询
        Page<MarketCalibrationTaskLog> page = new Page<>(current, size);
        IPage<MarketCalibrationTaskLog> logPage = taskLogService.page(page, wrapper);

        // 转换为VO
        Page<TaskLogVO> voPage = new Page<>(logPage.getCurrent(), logPage.getSize(), logPage.getTotal());
        voPage.setRecords(logPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList()));

        return Result.success(voPage);
    }

    /**
     * 查询执行日志详情
     */
    @GetMapping("/{id}")
    public Result<TaskLogVO> getById(@PathVariable Long id) {
        MarketCalibrationTaskLog log = taskLogService.getById(id);
        if (log == null) {
            return Result.error(404, "执行日志不存在");
        }

        return Result.success(convertToVO(log));
    }

    /**
     * 转换为VO
     */
    private TaskLogVO convertToVO(MarketCalibrationTaskLog log) {
        TaskLogVO vo = new TaskLogVO();
        BeanUtils.copyProperties(log, vo);
        return vo;
    }
}

