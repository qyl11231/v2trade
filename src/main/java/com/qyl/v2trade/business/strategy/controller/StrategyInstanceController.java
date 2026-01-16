package com.qyl.v2trade.business.strategy.controller;

import com.qyl.v2trade.business.strategy.model.dto.*;
import com.qyl.v2trade.business.strategy.model.entity.StrategyInstance;
import com.qyl.v2trade.business.strategy.service.StrategyInstanceService;
import com.qyl.v2trade.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 策略实例控制器
 */
@RestController
@RequestMapping("/api/strategy/instance")
public class StrategyInstanceController {

    private static final Logger logger = LoggerFactory.getLogger(StrategyInstanceController.class);

    @Autowired
    private StrategyInstanceService strategyInstanceService;

    /**
     * 创建策略实例
     */
    @PostMapping
    public Result<StrategyInstanceVO> create(@RequestBody @Valid StrategyInstanceCreateRequest request,
                                              @RequestHeader("X-User-Id") Long userId) {
        logger.info("创建策略实例: userId={}, strategyId={}, tradingPairId={}", 
                userId, request.getStrategyId(), request.getTradingPairId());

        StrategyInstance instance = strategyInstanceService.create(request, userId);
        StrategyInstanceVO vo = new StrategyInstanceVO();
        BeanUtils.copyProperties(instance, vo);

        return Result.success("创建成功", vo);
    }

    /**
     * 更新策略实例
     */
    @PutMapping("/{id}")
    public Result<StrategyInstanceVO> update(@PathVariable Long id,
                                                @RequestBody @Valid StrategyInstanceUpdateRequest request,
                                                @RequestHeader("X-User-Id") Long userId) {
        logger.info("更新策略实例: id={}, userId={}", id, userId);

        StrategyInstance instance = strategyInstanceService.update(id, request, userId);
        StrategyInstanceVO vo = new StrategyInstanceVO();
        BeanUtils.copyProperties(instance, vo);

        return Result.success("更新成功", vo);
    }

    /**
     * 查询策略实例详情
     */
    @GetMapping("/{id}")
    public Result<StrategyInstanceDetailVO> getDetail(@PathVariable Long id,
                                                      @RequestHeader("X-User-Id") Long userId) {
        logger.debug("查询策略实例详情: id={}, userId={}", id, userId);

        StrategyInstanceDetailVO detailVO = strategyInstanceService.getDetail(id, userId);
        return Result.success(detailVO);
    }

    /**
     * 查询策略实例列表（支持按strategyId过滤）
     */
    @GetMapping("/list")
    public Result<List<StrategyInstanceVO>> list(@RequestParam(required = false) Long strategyId,
                                                  @RequestHeader("X-User-Id") Long userId) {
        logger.debug("查询策略实例列表: userId={}, strategyId={}", userId, strategyId);

        List<StrategyInstanceVO> list;
        if (strategyId != null) {
            list = strategyInstanceService.listByStrategyId(strategyId, userId);
        } else {
            // 查询用户的所有实例
            list = strategyInstanceService.listByUserId(userId);
        }

        return Result.success(list);
    }

    /**
     * 启用/禁用策略实例
     */
    @PutMapping("/{id}/toggle")
    public Result<StrategyInstanceVO> toggleEnabled(@PathVariable Long id,
                                                     @RequestBody Map<String, Integer> body,
                                                     @RequestHeader("X-User-Id") Long userId) {
        Integer enabled = body.get("enabled");
        if (enabled == null) {
            return Result.error(400, "enabled参数不能为空");
        }

        logger.info("切换策略实例状态: id={}, userId={}, enabled={}", id, userId, enabled);

        StrategyInstance instance = strategyInstanceService.toggleEnabled(id, enabled, userId);
        StrategyInstanceVO vo = new StrategyInstanceVO();
        BeanUtils.copyProperties(instance, vo);

        return Result.success("操作成功", vo);
    }

    /**
     * 查询策略实例历史记录列表
     */
    @GetMapping("/{id}/history")
    public Result<List<StrategyInstanceHistoryVO>> listHistory(@PathVariable Long id,
                                                                @RequestHeader("X-User-Id") Long userId) {
        logger.debug("查询策略实例历史记录: instanceId={}, userId={}", id, userId);

        List<StrategyInstanceHistoryVO> list = strategyInstanceService.listHistory(id, userId);
        return Result.success(list);
    }

    /**
     * 查询策略实例历史记录详情
     */
    @GetMapping("/history/{historyId}")
    public Result<StrategyInstanceHistoryVO> getHistoryDetail(@PathVariable Long historyId,
                                                               @RequestHeader("X-User-Id") Long userId) {
        logger.debug("查询策略实例历史记录详情: historyId={}, userId={}", historyId, userId);

        StrategyInstanceHistoryVO historyVO = strategyInstanceService.getHistoryDetail(historyId, userId);
        return Result.success(historyVO);
    }
}

