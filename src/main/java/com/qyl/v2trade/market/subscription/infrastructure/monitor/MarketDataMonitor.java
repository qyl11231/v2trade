package com.qyl.v2trade.market.subscription.infrastructure.monitor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 行情数据监控服务
 * 记录关键指标，用于监控和告警
 */
@Slf4j
@Component
public class MarketDataMonitor {

    // 监控指标
    private final AtomicLong totalKlinesReceived = new AtomicLong(0);
    private final AtomicLong totalKlinesSaved = new AtomicLong(0);

    /**
     * 记录收到K线
     */
    public void recordKlineReceived() {
        totalKlinesReceived.incrementAndGet();
    }

    /**
     * 记录保存K线
     */
    public void recordKlineSaved() {
        totalKlinesSaved.incrementAndGet();
    }


    /**
     * 定时输出监控指标（每5分钟）
     */
    @Scheduled(fixedRate = 300000) // 5分钟
    public void logMetrics() {
        log.info("行情数据监控指标 - " +
                "收到K线: {}, " +
                "保存K线: {}, " ,
                totalKlinesReceived.get(),
                totalKlinesSaved.get());
    }

    /**
     * 获取收到的K线数量
     */
    public long getKlinesReceived() {
        return totalKlinesReceived.get();
    }
    
    /**
     * 获取监控指标
     */
    public MonitorMetrics getMetrics() {
        return new MonitorMetrics(
            totalKlinesReceived.get(),
            totalKlinesSaved.get()
        );
    }

    /**
     * 监控指标数据类
     */
    public static class MonitorMetrics {
        private final long totalKlinesReceived;
        private final long totalKlinesSaved;


        public MonitorMetrics(long totalKlinesReceived, long totalKlinesSaved) {
            this.totalKlinesReceived = totalKlinesReceived;
            this.totalKlinesSaved = totalKlinesSaved;

        }

        public long getTotalKlinesReceived() {
            return totalKlinesReceived;
        }

        public long getTotalKlinesSaved() {
            return totalKlinesSaved;
        }


    }
}

