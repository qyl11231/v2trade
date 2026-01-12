package com.qyl.v2trade.business.system.controller;

import com.qyl.v2trade.business.system.model.dto.MarketSubscriptionConfigRequest;
import com.qyl.v2trade.business.system.model.dto.MarketSubscriptionConfigVO;
import com.qyl.v2trade.business.system.model.entity.MarketSubscriptionConfig;
import com.qyl.v2trade.business.system.model.entity.TradingPair;
import com.qyl.v2trade.business.system.service.MarketSubscriptionConfigService;
import com.qyl.v2trade.business.system.service.TradingPairService;
import com.qyl.v2trade.market.model.NormalizedKline;
import com.qyl.v2trade.market.web.query.MarketQueryService;
import com.qyl.v2trade.common.Result;
import com.qyl.v2trade.common.constants.EnabledStatus;
import com.qyl.v2trade.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 行情订阅配置管理控制器
 */
@RestController
@RequestMapping("/api/market-subscription")
public class MarketSubscriptionConfigController {

    private static final Logger logger = LoggerFactory.getLogger(MarketSubscriptionConfigController.class);

    @Autowired
    private MarketSubscriptionConfigService configService;

    @Autowired
    private TradingPairService tradingPairService;

    @Autowired
    private MarketQueryService marketQueryService;

    /**
     * 查询行情订阅配置列表
     */
    @GetMapping("/list")
    public Result<List<MarketSubscriptionConfigVO>> list(
            @RequestParam(required = false) Integer enabled) {
        
        logger.debug("查询行情订阅配置列表: enabled={}", enabled);

        List<MarketSubscriptionConfig> configs;
        if (enabled != null && enabled.equals(EnabledStatus.ENABLED)) {
            configs = configService.listEnabled();
        } else {
            configs = configService.list();
        }

        // 转换为VO
        List<MarketSubscriptionConfigVO> voList = configs.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return Result.success(voList);
    }

    /**
     * 查询行情订阅配置详情
     */
    @GetMapping("/{id}")
    public Result<MarketSubscriptionConfigVO> getById(@PathVariable Long id) {
        logger.debug("查询行情订阅配置详情: id={}", id);

        MarketSubscriptionConfig config = configService.getById(id);
        if (config == null) {
            return Result.error(404, "配置不存在");
        }

        return Result.success(convertToVO(config));
    }

    /**
     * 根据交易对ID查询配置
     */
    @GetMapping("/by-trading-pair/{tradingPairId}")
    public Result<MarketSubscriptionConfigVO> getByTradingPairId(@PathVariable Long tradingPairId) {
        logger.debug("根据交易对ID查询配置: tradingPairId={}", tradingPairId);

        MarketSubscriptionConfig config = configService.getByTradingPairId(tradingPairId);
        if (config == null) {
            return Result.error(404, "配置不存在");
        }

        return Result.success(convertToVO(config));
    }

    /**
     * 创建行情订阅配置
     */
    @PostMapping
    public Result<MarketSubscriptionConfigVO> create(@Validated @RequestBody MarketSubscriptionConfigRequest request) {
        logger.info("创建行情订阅配置: tradingPairId={}", request.getTradingPairId());

        // 验证交易对是否存在
        TradingPair tradingPair = tradingPairService.getById(request.getTradingPairId());
        if (tradingPair == null) {
            return Result.error(404, "交易对不存在");
        }

        // 检查是否已存在配置
        MarketSubscriptionConfig existing = configService.getByTradingPairId(request.getTradingPairId());
        if (existing != null) {
            return Result.error(400, "该交易对已存在订阅配置，请使用更新接口");
        }

        try {
            MarketSubscriptionConfig config = new MarketSubscriptionConfig();
            BeanUtils.copyProperties(request, config);
            
            // 设置默认值
            if (config.getEnabled() == null) {
                config.setEnabled(EnabledStatus.ENABLED);
            }
            if (config.getCacheDurationMinutes() == null) {
                config.setCacheDurationMinutes(60);
            }

            configService.save(config);
            return Result.success("创建成功", convertToVO(config));
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            logger.error("创建行情订阅配置异常", e);
            return Result.error("创建失败: " + e.getMessage());
        }
    }

    /**
     * 更新行情订阅配置
     */
    @PutMapping("/{id}")
    public Result<MarketSubscriptionConfigVO> update(
            @PathVariable Long id,
            @Validated @RequestBody MarketSubscriptionConfigRequest request) {
        
        logger.info("更新行情订阅配置: id={}", id);

        MarketSubscriptionConfig config = configService.getById(id);
        if (config == null) {
            return Result.error(404, "配置不存在");
        }

        try {
            // 更新字段
            if (request.getEnabled() != null) {
                config.setEnabled(request.getEnabled());
            }
            if (request.getCacheDurationMinutes() != null) {
                config.setCacheDurationMinutes(request.getCacheDurationMinutes());
            }
            if (request.getRemark() != null) {
                config.setRemark(request.getRemark());
            }

            configService.updateById(config);
            return Result.success("更新成功", convertToVO(config));
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            logger.error("更新行情订阅配置异常", e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    /**
     * 删除行情订阅配置
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        logger.info("删除行情订阅配置: id={}", id);

        MarketSubscriptionConfig config = configService.getById(id);
        if (config == null) {
            return Result.error(404, "配置不存在");
        }

        try {
            configService.removeById(id);
            return Result.success("删除成功", null);
        } catch (Exception e) {
            logger.error("删除行情订阅配置异常", e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 启用/禁用行情订阅配置
     */
    @PutMapping("/{id}/toggle")
    public Result<MarketSubscriptionConfigVO> toggleEnabled(
            @PathVariable Long id,
            @RequestBody MarketSubscriptionConfigRequest request) {
        
        logger.info("切换行情订阅配置状态: id={}, enabled={}", id, request.getEnabled());

        MarketSubscriptionConfig config = configService.getById(id);
        if (config == null) {
            return Result.error(404, "配置不存在");
        }

        if (request.getEnabled() == null) {
            return Result.error(400, "enabled参数不能为空");
        }

        try {
            config.setEnabled(request.getEnabled());
            configService.updateById(config);
            return Result.success("操作成功", convertToVO(config));
        } catch (Exception e) {
            logger.error("切换行情订阅配置状态异常", e);
            return Result.error("操作失败: " + e.getMessage());
        }
    }

    /**
     * 转换为VO
     */
    private MarketSubscriptionConfigVO convertToVO(MarketSubscriptionConfig config) {
        if (config == null) {
            return null;
        }
        MarketSubscriptionConfigVO vo = new MarketSubscriptionConfigVO();
        BeanUtils.copyProperties(config, vo);

        // 关联交易对信息
        TradingPair tradingPair = tradingPairService.getById(config.getTradingPairId());
        if (tradingPair != null) {
            vo.setSymbol(tradingPair.getSymbol());
            vo.setMarketType(tradingPair.getMarketType());
            
            // 获取今日统计信息（使用today-stats逻辑获取完整统计数据）
            try {
                // 获取今日0点时间（UTC Instant）
                // 重构：按照时间管理约定，使用 Instant 进行计算
                Instant now = Instant.now();
                long nowMillis = now.toEpochMilli();
                long dayMillis = 24 * 60 * 60 * 1000L;
                long todayStartMillis = (nowMillis / dayMillis) * dayMillis;
                Instant todayStart = Instant.ofEpochMilli(todayStartMillis);
                
                // 查询今日所有1m K线
                // 重构：按照时间管理约定，直接传递 Instant 参数
                List<NormalizedKline> todayKlines = marketQueryService.queryKlines(
                    tradingPair.getSymbol(), "1m", todayStart, now, null
                );

                if (!todayKlines.isEmpty()) {
                    // 计算今日统计
                    double todayHigh = todayKlines.stream().mapToDouble(NormalizedKline::getHigh).max().orElse(0.0);
                    double todayLow = todayKlines.stream().mapToDouble(NormalizedKline::getLow).min().orElse(0.0);
                    double todayVolume = todayKlines.stream().mapToDouble(NormalizedKline::getVolume).sum();
                    
                    // 计算涨跌幅：今日收盘价 vs 今日开盘价
                    double todayOpen = todayKlines.get(0).getOpen();
                    double todayClose = todayKlines.get(todayKlines.size() - 1).getClose();
                    double todayChange = todayOpen != 0 ? ((todayClose - todayOpen) / todayOpen) * 100 : 0.0;
                    
                    // 当前价格使用最新K线的收盘价
                    double currentPrice = todayClose;

                    vo.setCurrentPrice(currentPrice);
                    vo.setTodayChange(todayChange);
                    vo.setTodayHigh(todayHigh);
                    vo.setTodayLow(todayLow);
                    vo.setTodayVolume(todayVolume);
                } else {
                    // 如果没有今日数据，尝试获取最新K线
                    NormalizedKline latest = marketQueryService.queryLatestKline(
                        tradingPair.getSymbol(), "1m"
                    );
                    if (latest != null) {
                        vo.setCurrentPrice(latest.getClose());
                        // 其他字段保持为null，前端会显示 "-"
                    }
                }
            } catch (Exception e) {
                logger.warn("获取交易对统计信息失败: tradingPairId={}", config.getTradingPairId(), e);
            }
        }

        return vo;
    }
}

