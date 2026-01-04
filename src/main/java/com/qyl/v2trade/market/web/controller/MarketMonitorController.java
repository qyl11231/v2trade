package com.qyl.v2trade.market.web.controller;

import com.qyl.v2trade.common.Result;
import com.qyl.v2trade.market.subscription.infrastructure.monitor.MarketDataMonitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 行情监控控制器
 * 提供监控指标查询接口
 */
@RestController
@RequestMapping("/api/market/monitor")
public class MarketMonitorController {

    @Autowired
    private MarketDataMonitor marketDataMonitor;

    /**
     * 获取监控指标
     * GET /api/market/monitor/metrics
     */
    @GetMapping("/metrics")
    public Result<MarketDataMonitor.MonitorMetrics> getMetrics() {
        return Result.success(marketDataMonitor.getMetrics());
    }
}

