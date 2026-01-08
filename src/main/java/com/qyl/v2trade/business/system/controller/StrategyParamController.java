package com.qyl.v2trade.business.system.controller;

import com.qyl.v2trade.business.system.model.dto.StrategyParamRequest;
import com.qyl.v2trade.business.system.model.dto.StrategyParamVO;
import com.qyl.v2trade.business.strategy.model.entity.StrategyParam;
import com.qyl.v2trade.business.strategy.service.StrategyParamService;
import com.qyl.v2trade.common.Result;
import com.qyl.v2trade.exception.BusinessException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 策略参数控制器
 */
@RestController
@RequestMapping("/api/strategy/param")
public class StrategyParamController {

    private static final Logger logger = LoggerFactory.getLogger(StrategyParamController.class);

    @Autowired
    private StrategyParamService strategyParamService;

    /**
     * 创建或更新策略参数
     */
    @PostMapping
    public Result<StrategyParamVO> saveOrUpdateParam(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Valid @RequestBody StrategyParamRequest request) {
        logger.info("用户保存策略参数: userId={}, strategyId={}", userId, request.getStrategyId());

        if (userId == null) {
            logger.warn("用户ID不能为空");
            return Result.error(400, "用户ID不能为空");
        }

        try {
            StrategyParam param = strategyParamService.saveOrUpdateParam(
                    userId,
                    request.getStrategyId(),
                    request.getInitialCapital(),
                    request.getBaseOrderRatio(),
                    request.getTakeProfitRatio(),
                    request.getStopLossRatio(),
                    request.getEntryCondition(),
                    request.getExitCondition()
            );

            return Result.success("保存成功", convertToVO(param));
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            logger.error("保存策略参数异常", e);
            return Result.error("保存失败: " + e.getMessage());
        }
    }

    /**
     * 根据策略ID查询参数
     */
    @GetMapping("/{strategyId}")
    public Result<StrategyParamVO> getParam(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long strategyId) {
        logger.debug("查询策略参数: userId={}, strategyId={}", userId, strategyId);

        if (userId == null) {
            logger.warn("用户ID不能为空");
            return Result.error(400, "用户ID不能为空");
        }

        StrategyParam param = strategyParamService.getByStrategyId(strategyId);
        if (param == null) {
            return Result.error(404, "策略参数不存在");
        }

        // 权限校验
        if (!param.getUserId().equals(userId)) {
            return Result.error(403, "无权访问该参数");
        }

        return Result.success(convertToVO(param));
    }

    /**
     * 删除策略参数
     */
    @DeleteMapping("/{strategyId}")
    public Result<Void> deleteParam(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long strategyId) {
        logger.info("用户删除策略参数: userId={}, strategyId={}", userId, strategyId);

        if (userId == null) {
            logger.warn("用户ID不能为空");
            return Result.error(400, "用户ID不能为空");
        }

        try {
            strategyParamService.deleteParam(strategyId, userId);
            return Result.success("删除成功", null);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            logger.error("删除策略参数异常", e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 转换为VO
     */
    private StrategyParamVO convertToVO(StrategyParam param) {
        if (param == null) {
            return null;
        }
        StrategyParamVO vo = new StrategyParamVO();
        BeanUtils.copyProperties(param, vo);
        return vo;
    }
}

