package com.qyl.v2trade.indicator.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qyl.v2trade.common.Result;
import com.qyl.v2trade.indicator.api.dto.SubscriptionCreateRequest;
import com.qyl.v2trade.indicator.api.dto.SubscriptionToggleRequest;
import com.qyl.v2trade.indicator.infrastructure.resolver.TradingPairResolver;
import com.qyl.v2trade.indicator.repository.IndicatorSubscriptionRepository;
import com.qyl.v2trade.indicator.repository.entity.IndicatorSubscription;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 指标订阅API控制器
 * 
 * <p>提供订阅的增删改查接口
 *
 * @author qyl
 */
@Slf4j
@RestController
@RequestMapping("/api/indicator/subscriptions")
public class IndicatorSubscriptionController {
    
    @Autowired
    private IndicatorSubscriptionRepository subscriptionRepository;
    
    @Autowired
    private TradingPairResolver tradingPairResolver;
    
    /**
     * 查询订阅列表（分页）
     * 
     * <p>GET /api/indicator/subscriptions?userId=&tradingPairId=&symbolKeyword=&timeframe=&indicatorCode=&enabled=&page=1&size=50
     */
    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long tradingPairId,
            @RequestParam(required = false) String symbolKeyword,
            @RequestParam(required = false) String timeframe,
            @RequestParam(required = false) String indicatorCode,
            @RequestParam(required = false) Integer enabled,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        try {
            log.debug("查询订阅列表: userId={}, tradingPairId={}, symbolKeyword={}, timeframe={}, enabled={}, page={}, size={}",
                    userId, tradingPairId, symbolKeyword, timeframe, enabled, page, size);
            
            Page<IndicatorSubscription> pageResult = subscriptionRepository.queryWithPagination(
                    userId, tradingPairId, symbolKeyword, timeframe, indicatorCode, enabled, page, size);
            
            Map<String, Object> data = new HashMap<>();
            data.put("records", pageResult.getRecords());
            data.put("total", pageResult.getTotal());
            data.put("current", pageResult.getCurrent());
            data.put("size", pageResult.getSize());
            
            return Result.success(data);
            
        } catch (Exception e) {
            log.error("查询订阅列表失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }
    
    /**
     * 创建订阅
     * 
     * <p>POST /api/indicator/subscriptions
     */
    @PostMapping
    public Result<IndicatorSubscription> create(@RequestBody SubscriptionCreateRequest request) {
        try {
            log.info("创建订阅: userId={}, tradingPairId={}, timeframe={}, indicatorCode={}",
                    request.getUserId(), request.getTradingPairId(), request.getTimeframe(), request.getIndicatorCode());
            
            // 参数校验
            if (request.getUserId() == null || request.getTradingPairId() == null || 
                request.getTimeframe() == null || request.getIndicatorCode() == null) {
                return Result.error(400, "参数不完整：userId、tradingPairId、timeframe、indicatorCode为必填项");
            }
            
            // 周期校验（不允许1m）
            if (!com.qyl.v2trade.indicator.infrastructure.time.QuestDbTsSemanticsProbe
                    .isTimeframeSupported(request.getTimeframe())) {
                return Result.error(400, "不支持的周期，只支持5m、15m、30m、1h、4h");
            }
            
            // 获取symbol和marketType
            String symbol = tradingPairResolver.tradingPairIdToSymbol(request.getTradingPairId());
            if (symbol == null) {
                return Result.error(404, "交易对不存在: tradingPairId=" + request.getTradingPairId());
            }
            
            // 构建订阅对象
            IndicatorSubscription subscription = new IndicatorSubscription();
            subscription.setUserId(request.getUserId());
            subscription.setTradingPairId(request.getTradingPairId());
            subscription.setSymbol(symbol);
            subscription.setMarketType("SWAP"); // TODO: 从trading_pair表获取
            subscription.setTimeframe(request.getTimeframe());
            subscription.setIndicatorCode(request.getIndicatorCode());
            subscription.setIndicatorVersion(request.getIndicatorVersion() != null ? request.getIndicatorVersion() : "v1");
            subscription.setParams(request.getParams());
            subscription.setEnabled(request.getEnabled() != null && request.getEnabled() ? 1 : 0);
            
            // 尝试插入（upsert会检查唯一键）
            subscriptionRepository.upsert(subscription);
            
            // 重新查询获取完整信息（包括ID）
            IndicatorSubscription saved = subscriptionRepository.findById(subscription.getId());
            if (saved == null) {
                // 如果找不到，可能是唯一键冲突，重新查询
                Page<IndicatorSubscription> existing = subscriptionRepository.queryWithPagination(
                        subscription.getUserId(), subscription.getTradingPairId(), null,
                        subscription.getTimeframe(), subscription.getIndicatorCode(), null, 1, 1);
                if (!existing.getRecords().isEmpty()) {
                    saved = existing.getRecords().get(0);
                }
            }
            
            return Result.success("订阅创建成功", saved);
            
        } catch (DuplicateKeyException e) {
            log.warn("创建订阅冲突: {}", e.getMessage());
            return Result.error(409, "已存在相同指标订阅");
        } catch (Exception e) {
            log.error("创建订阅失败", e);
            return Result.error("创建失败: " + e.getMessage());
        }
    }
    
    /**
     * 启停订阅
     * 
     * <p>PATCH /api/indicator/subscriptions/{id}
     */
    @PatchMapping("/{id}")
    public Result<IndicatorSubscription> toggle(@PathVariable Long id, @RequestBody SubscriptionToggleRequest request) {
        try {
            log.info("启停订阅: id={}, enabled={}", id, request.getEnabled());
            
            IndicatorSubscription subscription = subscriptionRepository.findById(id);
            if (subscription == null) {
                return Result.error(404, "订阅不存在");
            }
            
            if (request.getEnabled() == null) {
                return Result.error(400, "enabled参数不能为空");
            }
            
            subscription.setEnabled(request.getEnabled() ? 1 : 0);
            subscriptionRepository.upsert(subscription);
            
            IndicatorSubscription updated = subscriptionRepository.findById(id);
            return Result.success("操作成功", updated);
            
        } catch (Exception e) {
            log.error("启停订阅失败: id={}", id, e);
            return Result.error("操作失败: " + e.getMessage());
        }
    }
}

