package com.qyl.v2trade.business.system.controller;

import com.qyl.v2trade.business.system.model.dto.StrategyCreateRequest;
import com.qyl.v2trade.business.system.model.dto.StrategyDefinitionRequest;
import com.qyl.v2trade.business.system.model.dto.StrategyDefinitionVO;
import com.qyl.v2trade.business.system.model.dto.StrategyDetailVO;
import com.qyl.v2trade.business.strategy.model.entity.StrategyDefinition;
import com.qyl.v2trade.business.strategy.service.StrategyDefinitionService;
import com.qyl.v2trade.common.Result;
import com.qyl.v2trade.common.constants.EnabledStatus;
import com.qyl.v2trade.exception.BusinessException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

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
    public Result<StrategyDefinitionVO> createStrategy(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Valid @RequestBody StrategyDefinitionRequest request) {
        logger.info("用户创建策略定义: userId={}, strategyName={}", userId, request.getStrategyName());

        if (userId == null) {
            logger.warn("用户ID不能为空");
            return Result.error(400, "用户ID不能为空");
        }

        try {
            StrategyDefinition strategy = strategyDefinitionService.createStrategy(
                    userId,
                    request.getStrategyName(),
                    request.getStrategyType(),
                    request.getDecisionMode(),
                    request.getEnabled() != null ? request.getEnabled() : EnabledStatus.ENABLED
            );

            return Result.success("创建成功", convertToVO(strategy));
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            logger.error("创建策略定义异常", e);
            return Result.error("创建失败: " + e.getMessage());
        }
    }

    /**
     * 更新策略定义
     */
    @PutMapping("/{strategyId}")
    public Result<StrategyDefinitionVO> updateStrategy(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long strategyId,
            @RequestBody StrategyDefinitionRequest request) {
        logger.info("用户更新策略定义: userId={}, strategyId={}", userId, strategyId);

        if (userId == null) {
            logger.warn("用户ID不能为空");
            return Result.error(400, "用户ID不能为空");
        }

        try {
            StrategyDefinition strategy = strategyDefinitionService.updateStrategy(
                    strategyId,
                    userId,
                    request.getStrategyName(),
                    request.getStrategyType(),
                    request.getDecisionMode(),
                    request.getEnabled()
            );

            return Result.success("更新成功", convertToVO(strategy));
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            logger.error("更新策略定义异常", e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    /**
     * 删除策略定义
     */
    @DeleteMapping("/{strategyId}")
    public Result<Void> deleteStrategy(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long strategyId) {
        logger.info("用户删除策略定义: userId={}, strategyId={}", userId, strategyId);

        if (userId == null) {
            logger.warn("用户ID不能为空");
            return Result.error(400, "用户ID不能为空");
        }

        try {
            strategyDefinitionService.deleteStrategy(strategyId, userId);
            return Result.success("删除成功", null);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            logger.error("删除策略定义异常", e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 查询用户的策略定义列表
     */
    @GetMapping("/list")
    public Result<List<StrategyDefinitionVO>> listStrategies(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        logger.debug("查询用户策略定义列表: userId={}", userId);

        if (userId == null) {
            logger.warn("用户ID不能为空");
            return Result.error(400, "用户ID不能为空");
        }

        List<StrategyDefinition> strategies = strategyDefinitionService.listByUserId(userId);

        List<StrategyDefinitionVO> voList = strategies.stream()
                .map(this::convertToVO)
                .filter(vo -> vo != null)
                .collect(Collectors.toList());

        return Result.success(voList);
    }

    /**
     * 根据ID查询策略定义详情
     */
    @GetMapping("/{strategyId}")
    public Result<StrategyDefinitionVO> getStrategy(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long strategyId) {
        logger.debug("查询策略定义详情: userId={}, strategyId={}", userId, strategyId);

        if (userId == null) {
            logger.warn("用户ID不能为空");
            return Result.error(400, "用户ID不能为空");
        }

        try {
            StrategyDefinition strategy = strategyDefinitionService.getStrategyById(strategyId, userId);
            return Result.success(convertToVO(strategy));
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            logger.error("查询策略定义异常", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 完整创建策略（事务性，包含所有配置）
     */
    @PostMapping("/create-complete")
    public Result<StrategyDefinitionVO> createStrategyComplete(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Valid @RequestBody StrategyCreateRequest request) {
        logger.info("用户完整创建策略: userId={}, strategyName={}", userId, request.getDefinition().getStrategyName());

        if (userId == null) {
            logger.warn("用户ID不能为空");
            return Result.error(400, "用户ID不能为空");
        }

        try {
            StrategyDefinition strategy = strategyDefinitionService.createStrategyComplete(userId, request);
            return Result.success("策略创建成功", convertToVO(strategy));
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            logger.error("完整创建策略异常", e);
            return Result.error("策略创建失败: " + e.getMessage());
        }
    }

    /**
     * 完整更新策略（事务性，包含所有配置）
     */
    @PutMapping("/update-complete/{strategyId}")
    public Result<StrategyDefinitionVO> updateStrategyComplete(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long strategyId,
            @Valid @RequestBody StrategyCreateRequest request) {
        logger.info("用户完整更新策略: userId={}, strategyId={}, strategyName={}", userId, strategyId, request.getDefinition().getStrategyName());

        if (userId == null) {
            logger.warn("用户ID不能为空");
            return Result.error(400, "用户ID不能为空");
        }

        try {
            StrategyDefinition strategy = strategyDefinitionService.updateStrategyComplete(strategyId, userId, request);
            return Result.success("策略更新成功", convertToVO(strategy));
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            logger.error("完整更新策略异常", e);
            return Result.error("策略更新失败: " + e.getMessage());
        }
    }

    /**
     * 获取策略完整详情（包含定义、参数、交易对、信号订阅）
     */
    @GetMapping("/detail/{strategyId}")
    public Result<StrategyDetailVO> getStrategyDetail(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long strategyId) {
        logger.debug("获取策略完整详情: userId={}, strategyId={}", userId, strategyId);

        if (userId == null) {
            logger.warn("用户ID不能为空");
            return Result.error(400, "用户ID不能为空");
        }

        try {
            StrategyDetailVO detail = strategyDefinitionService.getStrategyDetail(strategyId, userId);
            return Result.success(detail);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            logger.error("获取策略详情异常", e);
            return Result.error("获取策略详情失败: " + e.getMessage());
        }
    }

    /**
     * 转换为VO
     */
    private StrategyDefinitionVO convertToVO(StrategyDefinition strategy) {
        if (strategy == null) {
            return null;
        }
        StrategyDefinitionVO vo = new StrategyDefinitionVO();
        BeanUtils.copyProperties(strategy, vo);
        return vo;
    }
}

