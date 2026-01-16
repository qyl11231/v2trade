package com.qyl.v2trade.business.strategy.controller;

import com.qyl.v2trade.business.strategy.runtime.ingress.BarClosedIngressAdapter;
import com.qyl.v2trade.business.strategy.runtime.ingress.SignalReceivedIngressAdapter;
import com.qyl.v2trade.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 策略触发器测试接口
 * 
 * <p>【重要】此 Controller 仅在 dev/test 环境启用，生产环境必须禁用
 * 
 * <p>配置项：`strategy.trigger.test-endpoint.enabled=true`（默认 false，生产环境禁用）
 */
@RestController
@RequestMapping("/api/strategy/trigger/test")
@ConditionalOnProperty(name = "strategy.trigger.test-endpoint.enabled", havingValue = "true", matchIfMissing = false)
public class StrategyTriggerTestController {
    
    @Autowired
    private BarClosedIngressAdapter barClosedAdapter;
    
    @Autowired
    private SignalReceivedIngressAdapter signalReceivedAdapter;
    
    /**
     * 测试 BAR_CLOSE 事件
     * 
     * @param tradingPairId 交易对ID
     * @param timeframe 时间周期（如 "5m", "1h"）
     * @param barCloseTimeMillis K线闭合时间戳（毫秒，UTC），不传则使用当前时间
     * @param strategySymbol 策略交易对符号（可选）
     * @return 处理结果
     */
    @PostMapping("/bar-close")
    public Result<String> testBarClose(
            @RequestParam Long tradingPairId,
            @RequestParam String timeframe,
            @RequestParam(required = false) Long barCloseTimeMillis,
            @RequestParam(required = false) String strategySymbol) {
        try {
            long closeTime = barCloseTimeMillis != null ? barCloseTimeMillis : System.currentTimeMillis();
            barClosedAdapter.testBarClose(tradingPairId, timeframe, closeTime, strategySymbol);
            return Result.success("BAR_CLOSE 事件已发送");
        } catch (Exception e) {
            return Result.error("发送 BAR_CLOSE 事件失败: " + e.getMessage());
        }
    }
    
    /**
     * 测试 SIGNAL 事件
     * 
     * @param signalConfigId 信号配置ID
     * @param signalId 信号ID（必须唯一）
     * @param price 信号价格（可选）
     * @return 处理结果
     */
    @PostMapping("/signal")
    public Result<String> testSignal(
            @RequestParam Long signalConfigId,
            @RequestParam String signalId,
            @RequestParam(required = false) BigDecimal price) {
        try {
            signalReceivedAdapter.testSignal(signalConfigId, signalId, price);
            return Result.success("SIGNAL 事件已发送");
        } catch (Exception e) {
            return Result.error("发送 SIGNAL 事件失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量测试 BAR_CLOSE 事件（用于压测）
     * 
     * @param tradingPairId 交易对ID
     * @param timeframe 时间周期
     * @param count 发送数量
     * @param intervalMs 发送间隔（毫秒），默认100ms
     * @return 处理结果
     */
    @PostMapping("/bar-close/batch")
    public Result<String> testBarCloseBatch(
            @RequestParam Long tradingPairId,
            @RequestParam String timeframe,
            @RequestParam(defaultValue = "10") Integer count,
            @RequestParam(defaultValue = "100") Long intervalMs) {
        try {
            long baseTime = System.currentTimeMillis();
            int successCount = 0;
            for (int i = 0; i < count; i++) {
                long barCloseTime = baseTime + (i * intervalMs);
                barClosedAdapter.testBarClose(tradingPairId, timeframe, barCloseTime, null);
                successCount++;
                if (intervalMs > 0 && i < count - 1) {
                    Thread.sleep(intervalMs);
                }
            }
            return Result.success(String.format("批量发送 BAR_CLOSE 事件成功: %d/%d", successCount, count));
        } catch (Exception e) {
            return Result.error("批量发送 BAR_CLOSE 事件失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量测试 SIGNAL 事件（用于压测）
     * 
     * @param signalConfigId 信号配置ID
     * @param count 发送数量
     * @param intervalMs 发送间隔（毫秒），默认100ms
     * @return 处理结果
     */
    @PostMapping("/signal/batch")
    public Result<String> testSignalBatch(
            @RequestParam Long signalConfigId,
            @RequestParam(defaultValue = "10") Integer count,
            @RequestParam(defaultValue = "100") Long intervalMs) {
        try {
            int successCount = 0;
            for (int i = 0; i < count; i++) {
                String signalId = "test-signal-" + System.currentTimeMillis() + "-" + i;
                signalReceivedAdapter.testSignal(signalConfigId, signalId, null);
                successCount++;
                if (intervalMs > 0 && i < count - 1) {
                    Thread.sleep(intervalMs);
                }
            }
            return Result.success(String.format("批量发送 SIGNAL 事件成功: %d/%d", successCount, count));
        } catch (Exception e) {
            return Result.error("批量发送 SIGNAL 事件失败: " + e.getMessage());
        }
    }
}

