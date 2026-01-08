package com.qyl.v2trade.business.system.controller;

import com.qyl.v2trade.business.system.model.dto.SyncResultVO;
import com.qyl.v2trade.business.system.model.dto.TradingPairVO;
import com.qyl.v2trade.business.system.model.entity.ExchangeMarketPair;
import com.qyl.v2trade.business.system.model.entity.TradingPair;
import com.qyl.v2trade.business.system.service.ExchangeMarketPairService;
import com.qyl.v2trade.business.system.service.TradingPairService;
import com.qyl.v2trade.common.Result;
import com.qyl.v2trade.common.constants.EnabledStatus;
import com.qyl.v2trade.common.constants.ExchangeCode;
import com.qyl.v2trade.common.constants.MarketType;
import com.qyl.v2trade.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 交易对管理控制器
 */
@RestController
@RequestMapping("/api/trading-pair")
public class TradingPairController {

    private static final Logger logger = LoggerFactory.getLogger(TradingPairController.class);

    @Autowired
    private TradingPairService tradingPairService;

    @Autowired
    private ExchangeMarketPairService exchangeMarketPairService;

    /**
     * 查询交易对列表
     * @param marketType 市场类型（可选）：SPOT / SWAP
     * @param enabled 是否启用（可选）：1 / 0
     */
    @GetMapping("/list")
    public Result<List<TradingPairVO>> list(
            @RequestParam(required = false) String marketType,
            @RequestParam(required = false) Integer enabled) {
        
        logger.debug("查询交易对列表: marketType={}, enabled={}", marketType, enabled);

        List<TradingPair> tradingPairs;
        
        if (marketType != null && !marketType.isEmpty()) {
            if (enabled != null && enabled.equals(EnabledStatus.ENABLED)) {
                tradingPairs = tradingPairService.listEnabledByMarketType(marketType);
            } else {
                tradingPairs = tradingPairService.listByMarketType(marketType);
            }
        } else {
            if (enabled != null && enabled.equals(EnabledStatus.ENABLED)) {
                tradingPairs = tradingPairService.listEnabled();
            } else {
                tradingPairs = tradingPairService.list();
            }
        }

        // 获取所有交易对的OKX规则
        List<Long> pairIds = tradingPairs.stream().map(TradingPair::getId).collect(Collectors.toList());
        Map<Long, ExchangeMarketPair> ruleMap = exchangeMarketPairService.listByExchangeCode(ExchangeCode.OKX)
                .stream()
                .filter(r -> pairIds.contains(r.getTradingPairId()))
                .collect(Collectors.toMap(ExchangeMarketPair::getTradingPairId, r -> r, (a, b) -> a));

        // 转换为VO
        List<TradingPairVO> voList = tradingPairs.stream()
                .map(pair -> convertToVO(pair, ruleMap.get(pair.getId())))
                .collect(Collectors.toList());

        return Result.success(voList);
    }

    /**
     * 查询交易对详情
     */
    @GetMapping("/{id}")
    public Result<TradingPairVO> getById(@PathVariable Long id) {
        logger.debug("查询交易对详情: id={}", id);

        TradingPair tradingPair = tradingPairService.getById(id);
        if (tradingPair == null) {
            return Result.error(404, "交易对不存在");
        }

        ExchangeMarketPair rule = exchangeMarketPairService
                .getByExchangeAndTradingPairId(ExchangeCode.OKX, id);

        return Result.success(convertToVO(tradingPair, rule));
    }

    /**
     * 启用/禁用交易对
     */
    @PutMapping("/{id}/toggle")
    public Result<TradingPairVO> toggleEnabled(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> body) {
        
        Integer enabled = body.get("enabled");
        if (enabled == null) {
            return Result.error(400, "enabled参数不能为空");
        }

        logger.info("切换交易对状态: id={}, enabled={}", id, enabled);

        try {
            TradingPair tradingPair = tradingPairService.toggleEnabled(id, enabled);
            ExchangeMarketPair rule = exchangeMarketPairService
                    .getByExchangeAndTradingPairId(ExchangeCode.OKX, id);
            
            return Result.success("操作成功", convertToVO(tradingPair, rule));
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        }
    }

    /**
     * 同步OKX交易对数据
     * @param marketType 市场类型：SPOT / SWAP，不传则同时同步两种
     */
    @PostMapping("/sync")
    public Result<List<SyncResultVO>> syncFromOkx(
            @RequestParam(required = false) String marketType) {
        
        logger.info("开始同步OKX交易对: marketType={}", marketType);

        List<SyncResultVO> results = new ArrayList<>();

        try {
            if (marketType == null || marketType.isEmpty()) {
                // 同步所有类型
                long start1 = System.currentTimeMillis();
                int count1 = tradingPairService.syncFromOkx(MarketType.SPOT);
                results.add(new SyncResultVO(MarketType.SPOT, count1, System.currentTimeMillis() - start1));

                long start2 = System.currentTimeMillis();
                int count2 = tradingPairService.syncFromOkx(MarketType.SWAP);
                results.add(new SyncResultVO(MarketType.SWAP, count2, System.currentTimeMillis() - start2));
            } else {
                long start = System.currentTimeMillis();
                int count = tradingPairService.syncFromOkx(marketType);
                results.add(new SyncResultVO(marketType, count, System.currentTimeMillis() - start));
            }

            return Result.success("同步成功", results);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            logger.error("同步OKX交易对异常", e);
            return Result.error("同步失败: " + e.getMessage());
        }
    }

    /**
     * 查询交易规则详情
     */
    @GetMapping("/{id}/rule")
    public Result<ExchangeMarketPair> getRule(@PathVariable Long id) {
        logger.debug("查询交易规则: tradingPairId={}", id);

        ExchangeMarketPair rule = exchangeMarketPairService
                .getByExchangeAndTradingPairId(ExchangeCode.OKX, id);
        
        if (rule == null) {
            return Result.error(404, "交易规则不存在");
        }

        return Result.success(rule);
    }

    /**
     * 转换为VO
     */
    private TradingPairVO convertToVO(TradingPair pair, ExchangeMarketPair rule) {
        TradingPairVO vo = new TradingPairVO();
        BeanUtils.copyProperties(pair, vo);

        if (rule != null) {
            vo.setSymbolOnExchange(rule.getSymbolOnExchange());
            vo.setTradingStatus(rule.getStatus());
            vo.setPricePrecision(rule.getPricePrecision());
            vo.setQuantityPrecision(rule.getQuantityPrecision());
            vo.setMinOrderQty(rule.getMinOrderQty() != null ? rule.getMinOrderQty().toPlainString() : null);
            vo.setMaxLeverage(rule.getMaxLeverage());
        }

        return vo;
    }
}

