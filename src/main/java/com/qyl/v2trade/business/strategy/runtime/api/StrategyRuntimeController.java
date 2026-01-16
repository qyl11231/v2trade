package com.qyl.v2trade.business.strategy.runtime.api;

import com.qyl.v2trade.business.strategy.runtime.manager.StrategyRuntimeManager;
import com.qyl.v2trade.business.strategy.runtime.runtime.StrategyRuntime;
import com.qyl.v2trade.business.strategy.runtime.snapshot.LatestBarSnapshot;
import com.qyl.v2trade.business.strategy.runtime.snapshot.LatestPriceSnapshot;
import com.qyl.v2trade.business.strategy.runtime.snapshot.LatestSignalSnapshot;
import com.qyl.v2trade.business.strategy.runtime.state.StrategyState;
import com.qyl.v2trade.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 策略运行时查询 API
 * 
 * <p>提供运行态查询接口，用于排障和监控
 *
 * @author qyl
 */
@RestController
@RequestMapping("/api/strategy/runtime")
public class StrategyRuntimeController {
    
    @Autowired
    private StrategyRuntimeManager runtimeManager;
    
    /**
     * 查询所有实例的运行态列表
     * 
     * @return 运行态列表
     */
    @GetMapping("/state/list")
    public Result<List<StrategyRuntimeVO>> listStates() {
        List<StrategyRuntime> runtimes = runtimeManager.getAllRuntimes();
        List<StrategyRuntimeVO> vos = runtimes.stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
        return Result.success(vos);
    }
    
    /**
     * 查询单个实例的运行态详情
     * 
     * @param instanceId 实例ID
     * @return 运行态详情
     */
    @GetMapping("/state/{instanceId}")
    public Result<StrategyRuntimeDetailVO> getState(@PathVariable Long instanceId) {
        StrategyRuntime runtime = runtimeManager.getRuntime(instanceId);
        if (runtime == null) {
            return Result.error(404, "Runtime 不存在: instanceId=" + instanceId);
        }
        return Result.success(convertToDetailVO(runtime));
    }
    
    /**
     * 转换为 VO（列表视图）
     */
    private StrategyRuntimeVO convertToVO(StrategyRuntime runtime) {
        StrategyRuntimeVO vo = new StrategyRuntimeVO();
        vo.setInstanceId(runtime.getInstanceId());
        vo.setUserId(runtime.getUserId());
        vo.setStrategySymbol(runtime.getStrategySymbol());
        
        StrategyState state = runtime.getState();
        vo.setPhase(state.getPhase() != null ? state.getPhase().toString() : "NULL");
        vo.setPositionSide(state.getPositionSide());
        vo.setPositionQty(state.getPositionQty());
        vo.setAvgEntryPrice(state.getAvgEntryPrice());
        vo.setLastEventTimeUtc(state.getLastEventTimeUtc());
        vo.setLatestPrice(runtime.getLatestPrice());
        vo.setLatestBar(runtime.getLatestBar());
        vo.setLatestSignal(runtime.getLatestSignal());
        return vo;
    }
    
    /**
     * 转换为 DetailVO（详情视图）
     */
    private StrategyRuntimeDetailVO convertToDetailVO(StrategyRuntime runtime) {
        StrategyRuntimeDetailVO vo = new StrategyRuntimeDetailVO();
        vo.setInstanceId(runtime.getInstanceId());
        vo.setUserId(runtime.getUserId());
        vo.setStrategyId(runtime.getStrategyId());
        vo.setTradingPairId(runtime.getTradingPairId());
        vo.setSignalConfigId(runtime.getSignalConfigId());
        vo.setStrategySymbol(runtime.getStrategySymbol());
        
        StrategyState state = runtime.getState();
        vo.setPhase(state.getPhase() != null ? state.getPhase().toString() : "NULL");
        vo.setPositionSide(state.getPositionSide());
        vo.setPositionQty(state.getPositionQty());
        vo.setAvgEntryPrice(state.getAvgEntryPrice());
        vo.setLastEventTimeUtc(state.getLastEventTimeUtc());
        
        // 快照信息
        LatestPriceSnapshot price = runtime.getLatestPrice();
        if (!price.isEmpty()) {
            vo.setLatestPrice(new PriceSnapshotVO(price.getPrice(), price.getAsOfTimeUtc(), price.getSource()));
        }
        
        LatestBarSnapshot bar = runtime.getLatestBar();
        if (!bar.isEmpty()) {
            vo.setLatestBar(new BarSnapshotVO(
                bar.getTimeframe(), bar.getOpen(), bar.getHigh(), bar.getLow(), 
                bar.getClose(), bar.getVolume(), bar.getBarCloseTimeUtc()
            ));
        }
        
        LatestSignalSnapshot signal = runtime.getLatestSignal();
        if (!signal.isEmpty()) {
            vo.setLatestSignal(new SignalSnapshotVO(
                signal.getSignalConfigId(), signal.getSignalId(), 
                signal.getSignalDirectionHint(), signal.getPrice(), signal.getReceivedTimeUtc()
            ));
        }
        
        return vo;
    }
    
    /**
     * 运行态列表 VO
     */
    public static class StrategyRuntimeVO {
        private Long instanceId;
        private Long userId;
        private String strategySymbol;
        private String phase;
        private String positionSide;
        private BigDecimal positionQty;
        private BigDecimal avgEntryPrice;
        private Instant lastEventTimeUtc;
        private LatestPriceSnapshot latestPrice;
        private LatestBarSnapshot latestBar;
        private LatestSignalSnapshot latestSignal;
        
        // Getters and Setters
        public Long getInstanceId() { return instanceId; }
        public void setInstanceId(Long instanceId) { this.instanceId = instanceId; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getStrategySymbol() { return strategySymbol; }
        public void setStrategySymbol(String strategySymbol) { this.strategySymbol = strategySymbol; }
        public String getPhase() { return phase; }
        public void setPhase(String phase) { this.phase = phase; }
        public String getPositionSide() { return positionSide; }
        public void setPositionSide(String positionSide) { this.positionSide = positionSide; }
        public BigDecimal getPositionQty() { return positionQty; }
        public void setPositionQty(BigDecimal positionQty) { this.positionQty = positionQty; }
        public BigDecimal getAvgEntryPrice() { return avgEntryPrice; }
        public void setAvgEntryPrice(BigDecimal avgEntryPrice) { this.avgEntryPrice = avgEntryPrice; }
        public Instant getLastEventTimeUtc() { return lastEventTimeUtc; }
        public void setLastEventTimeUtc(Instant lastEventTimeUtc) { this.lastEventTimeUtc = lastEventTimeUtc; }
    
        public LatestPriceSnapshot getLatestPrice() { return latestPrice; }
        public void setLatestPrice(LatestPriceSnapshot latestPrice) { this.latestPrice = latestPrice; }
        public LatestBarSnapshot getLatestBar() { return latestBar; }
        public void setLatestBar(LatestBarSnapshot latestBar) { this.latestBar = latestBar; }
        public LatestSignalSnapshot getLatestSignal() { return latestSignal; }
        public void setLatestSignal(LatestSignalSnapshot latestSignal) { this.latestSignal = latestSignal; }
    }
    
    /**
     * 运行态详情 VO
     */
    public static class StrategyRuntimeDetailVO {
        private Long instanceId;
        private Long userId;
        private Long strategyId;
        private Long tradingPairId;
        private Long signalConfigId;
        private String strategySymbol;
        private String phase;
        private String positionSide;
        private BigDecimal positionQty;
        private BigDecimal avgEntryPrice;
        private Instant lastEventTimeUtc;
        private PriceSnapshotVO latestPrice;
        private BarSnapshotVO latestBar;
        private SignalSnapshotVO latestSignal;
        
        // Getters and Setters
        public Long getInstanceId() { return instanceId; }
        public void setInstanceId(Long instanceId) { this.instanceId = instanceId; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public Long getStrategyId() { return strategyId; }
        public void setStrategyId(Long strategyId) { this.strategyId = strategyId; }
        public Long getTradingPairId() { return tradingPairId; }
        public void setTradingPairId(Long tradingPairId) { this.tradingPairId = tradingPairId; }
        public Long getSignalConfigId() { return signalConfigId; }
        public void setSignalConfigId(Long signalConfigId) { this.signalConfigId = signalConfigId; }
        public String getStrategySymbol() { return strategySymbol; }
        public void setStrategySymbol(String strategySymbol) { this.strategySymbol = strategySymbol; }
        public String getPhase() { return phase; }
        public void setPhase(String phase) { this.phase = phase; }
        public String getPositionSide() { return positionSide; }
        public void setPositionSide(String positionSide) { this.positionSide = positionSide; }
        public BigDecimal getPositionQty() { return positionQty; }
        public void setPositionQty(BigDecimal positionQty) { this.positionQty = positionQty; }
        public BigDecimal getAvgEntryPrice() { return avgEntryPrice; }
        public void setAvgEntryPrice(BigDecimal avgEntryPrice) { this.avgEntryPrice = avgEntryPrice; }
        public Instant getLastEventTimeUtc() { return lastEventTimeUtc; }
        public void setLastEventTimeUtc(Instant lastEventTimeUtc) { this.lastEventTimeUtc = lastEventTimeUtc; }
        public PriceSnapshotVO getLatestPrice() { return latestPrice; }
        public void setLatestPrice(PriceSnapshotVO latestPrice) { this.latestPrice = latestPrice; }
        public BarSnapshotVO getLatestBar() { return latestBar; }
        public void setLatestBar(BarSnapshotVO latestBar) { this.latestBar = latestBar; }
        public SignalSnapshotVO getLatestSignal() { return latestSignal; }
        public void setLatestSignal(SignalSnapshotVO latestSignal) { this.latestSignal = latestSignal; }
    }
    
    /**
     * 价格快照 VO
     */
    public static class PriceSnapshotVO {
        private BigDecimal price;
        private Instant asOfTimeUtc;
        private String source;
        
        public PriceSnapshotVO(BigDecimal price, Instant asOfTimeUtc, String source) {
            this.price = price;
            this.asOfTimeUtc = asOfTimeUtc;
            this.source = source;
        }
        
        // Getters
        public BigDecimal getPrice() { return price; }
        public Instant getAsOfTimeUtc() { return asOfTimeUtc; }
        public String getSource() { return source; }
    }
    
    /**
     * K线快照 VO
     */
    public static class BarSnapshotVO {
        private String timeframe;
        private BigDecimal open;
        private BigDecimal high;
        private BigDecimal low;
        private BigDecimal close;
        private BigDecimal volume;
        private Instant barCloseTimeUtc;
        
        public BarSnapshotVO(String timeframe, BigDecimal open, BigDecimal high, 
                           BigDecimal low, BigDecimal close, BigDecimal volume, 
                           Instant barCloseTimeUtc) {
            this.timeframe = timeframe;
            this.open = open;
            this.high = high;
            this.low = low;
            this.close = close;
            this.volume = volume;
            this.barCloseTimeUtc = barCloseTimeUtc;
        }
        
        // Getters
        public String getTimeframe() { return timeframe; }
        public BigDecimal getOpen() { return open; }
        public BigDecimal getHigh() { return high; }
        public BigDecimal getLow() { return low; }
        public BigDecimal getClose() { return close; }
        public BigDecimal getVolume() { return volume; }
        public Instant getBarCloseTimeUtc() { return barCloseTimeUtc; }
    }
    
    /**
     * 信号快照 VO
     */
    public static class SignalSnapshotVO {
        private Long signalConfigId;
        private String signalId;
        private String signalDirectionHint;
        private BigDecimal price;
        private Instant receivedTimeUtc;
        
        public SignalSnapshotVO(Long signalConfigId, String signalId, 
                               String signalDirectionHint, BigDecimal price, 
                               Instant receivedTimeUtc) {
            this.signalConfigId = signalConfigId;
            this.signalId = signalId;
            this.signalDirectionHint = signalDirectionHint;
            this.price = price;
            this.receivedTimeUtc = receivedTimeUtc;
        }
        
        // Getters
        public Long getSignalConfigId() { return signalConfigId; }
        public String getSignalId() { return signalId; }
        public String getSignalDirectionHint() { return signalDirectionHint; }
        public BigDecimal getPrice() { return price; }
        public Instant getReceivedTimeUtc() { return receivedTimeUtc; }
    }
}

