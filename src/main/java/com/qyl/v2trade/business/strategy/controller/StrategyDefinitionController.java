package com.qyl.v2trade.business.strategy.controller;

import com.qyl.v2trade.business.strategy.model.dto.StrategyDefinitionCreateRequest;
import com.qyl.v2trade.business.strategy.model.dto.StrategyDefinitionDetailVO;
import com.qyl.v2trade.business.strategy.model.dto.StrategyDefinitionUpdateRequest;
import com.qyl.v2trade.business.strategy.model.dto.StrategyDefinitionVO;
import com.qyl.v2trade.business.strategy.model.entity.StrategyDefinition;
import com.qyl.v2trade.business.strategy.service.StrategyDefinitionService;
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
 * 策略定义控制器
 */
@RestController
@RequestMapping("/api/strategy/definition")
public class StrategyDefinitionController {

    private static final Logger logger = LoggerFactory.getLogger(StrategyDefinitionController.class);

    @Autowired
    private StrategyDefinitionService strategyDefinitionService;

    /**
     * 创建策略定义
     */
    @PostMapping
    public Result<StrategyDefinitionVO> create(@RequestBody @Valid StrategyDefinitionCreateRequest request,
                                                @RequestHeader("X-User-Id") Long userId) {
        logger.info("创建策略定义: userId={}, strategyName={}", userId, request.getStrategyName());

        StrategyDefinition definition = strategyDefinitionService.create(request, userId);
        StrategyDefinitionVO vo = new StrategyDefinitionVO();
        BeanUtils.copyProperties(definition, vo);

        return Result.success("创建成功", vo);
    }

    /**
     * 更新策略定义
     */
    @PutMapping("/{id}")
    public Result<StrategyDefinitionVO> update(@PathVariable Long id,
                                                @RequestBody @Valid StrategyDefinitionUpdateRequest request,
                                                @RequestHeader("X-User-Id") Long userId) {
        logger.info("更新策略定义: id={}, userId={}", id, userId);

        StrategyDefinition definition = strategyDefinitionService.update(id, request, userId);
        StrategyDefinitionVO vo = new StrategyDefinitionVO();
        BeanUtils.copyProperties(definition, vo);

        return Result.success("更新成功", vo);
    }

    /**
     * 查询策略详情（含实例列表）
     */
    @GetMapping("/{id}")
    public Result<StrategyDefinitionDetailVO> getDetail(@PathVariable Long id,
                                                       @RequestHeader("X-User-Id") Long userId) {
        logger.debug("查询策略定义详情: id={}, userId={}", id, userId);

        StrategyDefinitionDetailVO detailVO = strategyDefinitionService.getDetail(id, userId);
        return Result.success(detailVO);
    }

    /**
     * 查询策略定义列表
     */
    @GetMapping("/list")
    public Result<List<StrategyDefinitionVO>> list(@RequestParam(required = false) Integer enabled,
                                                    @RequestHeader("X-User-Id") Long userId) {
        logger.debug("查询策略定义列表: userId={}, enabled={}", userId, enabled);

        List<StrategyDefinitionVO> list = strategyDefinitionService.listByUserId(userId, enabled);
        return Result.success(list);
    }

    /**
     * 启用/禁用策略定义
     */
    @PutMapping("/{id}/toggle")
    public Result<StrategyDefinitionVO> toggleEnabled(@PathVariable Long id,
                                                       @RequestBody Map<String, Integer> body,
                                                       @RequestHeader("X-User-Id") Long userId) {
        Integer enabled = body.get("enabled");
        if (enabled == null) {
            return Result.error(400, "enabled参数不能为空");
        }

        logger.info("切换策略定义状态: id={}, userId={}, enabled={}", id, userId, enabled);

        StrategyDefinition definition = strategyDefinitionService.toggleEnabled(id, enabled, userId);
        StrategyDefinitionVO vo = new StrategyDefinitionVO();
        BeanUtils.copyProperties(definition, vo);

        return Result.success("操作成功", vo);
    }
}

