package com.qyl.v2trade.business.controller;

import com.qyl.v2trade.business.model.dto.StrategySymbolRequest;
import com.qyl.v2trade.business.model.dto.StrategySymbolVO;
import com.qyl.v2trade.business.model.entity.StrategySymbol;
import com.qyl.v2trade.business.model.entity.TradingPair;
import com.qyl.v2trade.business.service.StrategySymbolService;
import com.qyl.v2trade.business.service.TradingPairService;
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
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 策略交易对控制器
 */
@RestController
@RequestMapping("/api/strategy/symbol")
public class StrategySymbolController {

    private static final Logger logger = LoggerFactory.getLogger(StrategySymbolController.class);

    @Autowired
    private StrategySymbolService strategySymbolService;

    @Autowired
    private TradingPairService tradingPairService;

    /**
     * 创建策略交易对
     */
    @PostMapping
    public Result<StrategySymbolVO> createStrategySymbol(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Valid @RequestBody StrategySymbolRequest request) {
        logger.info("用户创建策略交易对: userId={}, strategyId={}, tradingPairId={}", 
                userId, request.getStrategyId(), request.getTradingPairId());

        if (userId == null) {
            logger.warn("用户ID不能为空");
            return Result.error(400, "用户ID不能为空");
        }

        try {
            StrategySymbol symbol = strategySymbolService.createStrategySymbol(
                    userId,
                    request.getStrategyId(),
                    request.getTradingPairId(),
                    request.getEnabled() != null ? request.getEnabled() : EnabledStatus.ENABLED
            );

            return Result.success("创建成功", convertToVO(symbol));
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            logger.error("创建策略交易对异常", e);
            return Result.error("创建失败: " + e.getMessage());
        }
    }

    /**
     * 更新策略交易对
     */
    @PutMapping("/{id}")
    public Result<StrategySymbolVO> updateStrategySymbol(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long id,
            @RequestBody StrategySymbolRequest request) {
        logger.info("用户更新策略交易对: userId={}, id={}", userId, id);

        if (userId == null) {
            logger.warn("用户ID不能为空");
            return Result.error(400, "用户ID不能为空");
        }

        try {
            StrategySymbol symbol = strategySymbolService.updateStrategySymbol(
                    id,
                    userId,
                    request.getEnabled()
            );

            return Result.success("更新成功", convertToVO(symbol));
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            logger.error("更新策略交易对异常", e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    /**
     * 删除策略交易对
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteStrategySymbol(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long id) {
        logger.info("用户删除策略交易对: userId={}, id={}", userId, id);

        if (userId == null) {
            logger.warn("用户ID不能为空");
            return Result.error(400, "用户ID不能为空");
        }

        try {
            strategySymbolService.deleteStrategySymbol(id, userId);
            return Result.success("删除成功", null);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            logger.error("删除策略交易对异常", e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 根据策略ID查询交易对列表
     */
    @GetMapping("/list")
    public Result<List<StrategySymbolVO>> listStrategySymbols(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam Long strategyId) {
        logger.debug("查询策略交易对列表: userId={}, strategyId={}", userId, strategyId);

        if (userId == null) {
            logger.warn("用户ID不能为空");
            return Result.error(400, "用户ID不能为空");
        }

        List<StrategySymbol> symbols = strategySymbolService.listByStrategyId(strategyId);

        // 获取所有交易对ID，批量查询交易对信息
        List<Long> tradingPairIds = symbols.stream()
                .map(StrategySymbol::getTradingPairId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, TradingPair> tradingPairMap = tradingPairService.listByIds(tradingPairIds)
                .stream()
                .collect(Collectors.toMap(TradingPair::getId, pair -> pair));

        List<StrategySymbolVO> voList = symbols.stream()
                .map(symbol -> {
                    StrategySymbolVO vo = convertToVO(symbol);
                    TradingPair pair = tradingPairMap.get(symbol.getTradingPairId());
                    if (pair != null) {
                        vo.setTradingPairName(pair.getSymbol());
                        vo.setMarketType(pair.getMarketType());
                    }
                    return vo;
                })
                .filter(vo -> vo != null)
                .collect(Collectors.toList());

        return Result.success(voList);
    }

    /**
     * 转换为VO
     */
    private StrategySymbolVO convertToVO(StrategySymbol symbol) {
        if (symbol == null) {
            return null;
        }
        StrategySymbolVO vo = new StrategySymbolVO();
        BeanUtils.copyProperties(symbol, vo);
        return vo;
    }
}

