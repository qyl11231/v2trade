package com.qyl.v2trade.indicator.series;

import com.qyl.v2trade.business.system.model.entity.MarketSubscriptionConfig;
import com.qyl.v2trade.business.system.service.MarketSubscriptionConfigService;
import com.qyl.v2trade.indicator.domain.event.BarClosedEvent;
import com.qyl.v2trade.indicator.domain.event.BarClosedEventPublisher;
import com.qyl.v2trade.indicator.domain.model.NormalizedBar;
import com.qyl.v2trade.indicator.infrastructure.resolver.TradingPairResolver;
import com.qyl.v2trade.indicator.infrastructure.time.QuestDbTsSemanticsProbe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * BarSeries管理器
 * 
 * <p>按订阅建立和管理BarSeries
 * 
 * <p>规则：
 * - 订阅中出现的pair/timeframe才会被加载与维护
 * - 重放同一BarClosedEvent不产生重复bar
 *
 * @author qyl
 */
@Slf4j
@Component
public class BarSeriesManager {
    
    /**
     * 行情订阅配置服务（用于获取需要维护K线的交易对）
     */
    @Autowired
    private MarketSubscriptionConfigService marketSubscriptionConfigService;
    
    @Autowired
    private QuestDbKlineReader klineReader;
    
    @Autowired(required = false)
    private BarClosedEventPublisher barClosedEventPublisher;
    
    @Autowired(required = false)
    private TradingPairResolver tradingPairResolver;
    
    /**
     * 支持的周期列表（指标模块只维护这5个周期）
     */
    private static final String[] SUPPORTED_TIMEFRAMES = QuestDbTsSemanticsProbe.getSupportedTimeframes();
    
    /**
     * BarSeries存储：key = pairId:timeframe
     * 注意：不再按userId区分，因为K线数据是共享的（同一交易对同一周期的K线对所有用户相同）
     */
    private final Map<String, BarSeriesImpl> seriesMap = new ConcurrentHashMap<>();
    
    /**
     * BarClosedEvent订阅者
     */
    private Consumer<BarClosedEvent> eventConsumer;
    
    @PostConstruct
    public void init() {
        // 同时支持两种事件接收方式：
        // 1. Spring @EventListener（优先，已在onBarClosed方法上添加@EventListener注解）
        // 2. 自定义BarClosedEventPublisher订阅（向后兼容）
        if (barClosedEventPublisher != null) {
            eventConsumer = this::onBarClosed;
            barClosedEventPublisher.subscribe(eventConsumer);
            log.info("BarSeriesManager已订阅BarClosedEvent（自定义Publisher方式，作为备选）");
        } else {
            log.warn("BarClosedEventPublisher未注入，将仅使用Spring @EventListener机制接收事件。如果onBarClosed方法未被调用，请检查Spring事件发布配置");
        }
        log.info("BarSeriesManager初始化完成，已通过@EventListener监听BarClosedEvent");
    }
    
    @PreDestroy
    public void destroy() {
        // 取消订阅
        if (barClosedEventPublisher != null && eventConsumer != null) {
            barClosedEventPublisher.unsubscribe(eventConsumer);
            log.info("BarSeriesManager已取消订阅BarClosedEvent");
        }
    }
    
    /**
     * 启动时加载历史数据
     * 
     * <p>根据行情订阅配置获取交易对，为每个交易对的5个周期（5m、15m、30m、1h、4h）都维护K线数据
     * <p>K线数据维护独立于指标订阅，确保基础数据始终可用
     */
    public void bootstrap() {
        log.info("========== 开始加载BarSeries历史数据（从行情订阅配置获取交易对） ==========");
        
        // 1. 从行情订阅配置获取所有启用的交易对
        List<MarketSubscriptionConfig> marketConfigs;
        try {
            marketConfigs = marketSubscriptionConfigService.listEnabled();
        } catch (Exception e) {
            log.error("查询行情订阅配置失败", e);
            return;
        }
        
        if (marketConfigs == null || marketConfigs.isEmpty()) {
            log.warn("没有启用的行情订阅配置，跳过BarSeries加载。请检查数据库 market_subscription_config 表中是否有 enabled=1 的记录");
            return;
        }
        
        log.info("从行情订阅配置获取到{}个启用的交易对", marketConfigs.size());
        
        // 2. 为每个交易对的5个周期都加载历史数据
        int totalLoaded = 0;
        int totalSkipped = 0;
        int totalFailed = 0;
        int totalEmpty = 0;
        
        for (MarketSubscriptionConfig config : marketConfigs) {
            Long pairId = config.getTradingPairId();
            
            if (pairId == null) {
                log.warn("行情订阅配置的tradingPairId为null，跳过: configId={}", config.getId());
                totalFailed++;
                continue;
            }
            
            // 获取symbol用于日志显示
            String symbol = null;
            if (tradingPairResolver != null) {
                symbol = tradingPairResolver.tradingPairIdToSymbol(pairId);
            }
            
            log.info("处理交易对: pairId={}, symbol={}", pairId, symbol != null ? symbol : "未知");
            
            // 为每个支持的周期（5m、15m、30m、1h、4h）加载数据
            for (String timeframe : SUPPORTED_TIMEFRAMES) {
                try {
                    String seriesKey = buildSeriesKey(pairId, timeframe);
                    
                    // 检查是否已存在
                    if (seriesMap.containsKey(seriesKey)) {
                        log.debug("BarSeries已存在，跳过加载: pairId={}, timeframe={}", pairId, timeframe);
                        totalSkipped++;
                        continue;
                    }
                    
                    // 加载历史数据（最多365根）
                    log.debug("开始加载K线: pairId={}, symbol={}, timeframe={}", pairId, symbol != null ? symbol : "未知", timeframe);
                    List<NormalizedBar> bars = klineReader.loadLatestBars(pairId, timeframe, 365);
                    
                    if (bars == null || bars.isEmpty()) {
                        log.warn("加载的K线数据为空: pairId={}, symbol={}, timeframe={} (可能QuestDB中没有对应数据)", 
                                pairId, symbol, timeframe);
                        totalEmpty++;
                        // 即使为空也创建BarSeries，后续可以通过BarClosedEvent填充
                        BarSeriesImpl series = new BarSeriesImpl(pairId, timeframe, new ArrayList<>());
                        seriesMap.put(seriesKey, series);
                        log.info("创建空BarSeries: pairId={}, symbol={}, timeframe={} (等待后续BarClosedEvent填充)", 
                                pairId, symbol != null ? symbol : "未知", timeframe);
                    } else {
                        // 创建BarSeries（会自动维护365根的限制）
                        BarSeriesImpl series = new BarSeriesImpl(pairId, timeframe, bars);
                        seriesMap.put(seriesKey, series);
                        totalLoaded++;
                        log.info("✓ 加载BarSeries成功: pairId={}, symbol={}, timeframe={}, bars={}根", 
                                pairId, symbol != null ? symbol : "未知", timeframe, bars.size());
                    }
                    
                } catch (Exception e) {
                    log.error("✗ 加载BarSeries异常: pairId={}, symbol={}, timeframe={}", 
                            pairId, symbol != null ? symbol : "未知", timeframe, e);
                    totalFailed++;
                }
            }
        }
        
        log.info("========== BarSeries历史数据加载完成 ==========");
        log.info("统计: loadedSeries={}, emptySeries={}, skipped={}, failed={}, 支持的周期={}", 
                totalLoaded, totalEmpty, totalSkipped, totalFailed, String.join(", ", SUPPORTED_TIMEFRAMES));
        log.info("seriesMap当前大小: {}", seriesMap.size());
        
        if (seriesMap.isEmpty()) {
            log.warn("⚠️ seriesMap为空！可能的原因：");
            log.warn("1. 数据库market_subscription_config表中没有enabled=1的记录");
            log.warn("2. trading_pair_id字段为null");
            log.warn("3. QuestDB中没有对应的K线数据");
            log.warn("4. QuestDB连接失败或表不存在");
        }
    }
    
    /**
     * 处理BarClosedEvent（V2：只维护K线输入，不触发指标计算）
     * 
     * <p>维护K线数据：当聚合完成时，将新的K线追加到对应的BarSeries
     * <p>如果对应的BarSeries不存在，会自动创建（支持动态添加新的交易对）
     * <p>自动维护最新365根，去除最旧的（防止内存泄漏）
     * 
     * <p>【V2 重要】此方法只做K线数据维护，不触发任何指标计算流程
     * <p>指标计算改为按需评估模式，由策略模块调用 evaluate() 接口触发
     * 
     * <p>支持两种事件接收方式：
     * <p>1. Spring @EventListener（优先，更可靠）
     * <p>2. 自定义BarClosedEventPublisher订阅（向后兼容）
     * 
     * @param event BarClosedEvent（包含 bar_close_time UTC 语义的时间）
     */
    @EventListener
    public void onBarClosed(BarClosedEvent event) {
        log.info("收到BarClosedEvent: symbol={}, timeframe={}, tradingPairId={}, barCloseTime={}",
                event.symbol(), event.timeframe(), event.tradingPairId(), event.barCloseTime());
            
        Long pairId = event.tradingPairId();
        String symbol = event.symbol();

        // 兜底解析 tradingPairId，避免因上游未注入 TradingPairResolver 导致事件被丢弃
        if (pairId == null && tradingPairResolver != null && symbol != null) {
            try {
                pairId = tradingPairResolver.symbolToTradingPairId(symbol);
                if (pairId != null) {
                    log.info("从 symbol 解析到 tradingPairId: symbol={}, pairId={}", symbol, pairId);
                }
            } catch (Exception e) {
                log.warn("解析 tradingPairId 失败，symbol={}", symbol, e);
            }
        }

        if (pairId == null || event.timeframe() == null) {
            log.warn("BarClosedEvent缺少必要信息，跳过: symbol={}, timeframe={}, tradingPairId={}",
                    symbol, event.timeframe(), pairId);
            return;
        }
            
        // 过滤：只处理支持的周期（5m、15m、30m、1h、4h），不处理1m
        if (!com.qyl.v2trade.indicator.infrastructure.time.QuestDbTsSemanticsProbe
                .isTimeframeSupported(event.timeframe())) {
            log.debug("跳过不支持的周期: pairId={}, timeframe={} (指标模块只支持5m/15m/30m/1h/4h)",
                    event.tradingPairId(), event.timeframe());
            return;
        }
        
        String timeframe = event.timeframe();
        String seriesKey = buildSeriesKey(pairId, timeframe);
        
        // 转换为NormalizedBar
        // 注意：event.barCloseTime() 是 bar_close_time UTC 语义（指标模块统一时间语义）
        NormalizedBar bar = NormalizedBar.of(
                event.tradingPairId(),
                event.symbol(),
                event.timeframe(),
                event.barCloseTime(),  // bar_close_time UTC
                event.open(),
                event.high(),
                event.low(),
                event.close(),
                event.volume()
        );
        
        // 获取或创建BarSeries
        BarSeriesImpl series = seriesMap.get(seriesKey);
        if (series == null) {
            // 如果不存在，创建新的BarSeries（支持动态添加）
            log.info("动态创建BarSeries: pairId={}, timeframe={} (收到BarClosedEvent但尚未初始化)",
                    pairId, timeframe);
            series = new BarSeriesImpl(pairId, timeframe, new ArrayList<>());
            seriesMap.put(seriesKey, series);
        }
        
        // 追加K线（内部会自动维护365根的限制，去重）
        series.append(bar);
        
        log.info("✓ BarSeries已更新: pairId={}, symbol={}, timeframe={}, barTime={}, totalBars={}",
                pairId, event.symbol(), timeframe, event.barCloseTime(), series.size());
        
        // ❌ V2: 禁止在此处触发任何指标计算流程
        // 指标计算改为按需评估模式，由策略模块调用 evaluate() 接口触发
    }
    
    /**
     * 获取BarSeries视图（只读）
     * 
     * <p>K线数据是共享的（同一交易对同一周期的K线对所有用户相同），所以不需要userId参数
     * 
     * @param pairId 交易对ID
     * @param timeframe 周期
     * @return BarSeriesView，如果不存在返回null
     */
    public BarSeriesView getSeries(long pairId, String timeframe) {
        String key = buildSeriesKey(pairId, timeframe);
        return seriesMap.get(key);
    }
    
    /**
     * 构建series key
     * 
     * @param pairId 交易对ID
     * @param timeframe 周期
     * @return key格式：pairId:timeframe
     */
    private String buildSeriesKey(long pairId, String timeframe) {
        return pairId + ":" + timeframe;
    }
    
    /**
     * BarSeries实现（内部类）
     * 
     * <p>K线数据是共享的，不区分用户
     */
    private static class BarSeriesImpl implements BarSeriesView {
        private final Long tradingPairId;
        private final String timeframe;
        private final List<NormalizedBar> bars;
        
        /**
         * 构造BarSeries
         * 
         * @param tradingPairId 交易对ID
         * @param timeframe 周期
         * @param bars 初始K线列表
         */
        public BarSeriesImpl(Long tradingPairId, String timeframe, List<NormalizedBar> bars) {
            this.tradingPairId = tradingPairId;
            this.timeframe = timeframe;
            this.bars = new ArrayList<>(bars != null ? bars : new ArrayList<>());
        }
        
        /**
         * 追加bar（去重），并维护最新365根，去除最旧的
         * 
         * <p>【时间语义】bar.barTime() 是 bar_close_time UTC 语义（指标模块统一时间语义）
         * 
         * @param bar 要追加的bar（bar.barTime() 是 bar_close_time UTC）
         */
        public synchronized void append(NormalizedBar bar) {
            // 检查是否已存在（按barTime判断，barTime是bar_close_time UTC语义）
            boolean exists = bars.stream()
                    .anyMatch(b -> b.barTime().equals(bar.barTime()));
            
            if (exists) {
                log.debug("Bar已存在，跳过append: pairId={}, timeframe={}, barTime={}",
                        tradingPairId, timeframe, bar.barTime());
                return;
            }
            
            // 追加并保持升序
            bars.add(bar);
            bars.sort(Comparator.comparing(NormalizedBar::barTime));
            
            // 维护最新365根，去除最旧的（防止内存泄漏）
            int maxBars = 365;
            if (bars.size() > maxBars) {
                int removeCount = bars.size() - maxBars;
                List<NormalizedBar> removed = new ArrayList<>(bars.subList(0, removeCount));
                bars.removeAll(removed);
                log.debug("BarSeries维护: pairId={}, timeframe={}, 移除了{}根最旧的bar, 当前数量={}",
                        tradingPairId, timeframe, removeCount, bars.size());
            }
            
            log.debug("Bar已追加: pairId={}, timeframe={}, barTime={}, totalBars={}",
                    tradingPairId, timeframe, bar.barTime(), bars.size());
        }
        
        @Override
        public List<NormalizedBar> getBars() {
            return new ArrayList<>(bars); // 返回副本
        }
        
        @Override
        public NormalizedBar getBar(int index) {
            if (index < 0 || index >= bars.size()) {
                return null;
            }
            return bars.get(index);
        }
        
        @Override
        public int size() {
            return bars.size();
        }
        
        @Override
        public NormalizedBar getLatestBar() {
            return bars.isEmpty() ? null : bars.get(bars.size() - 1);
        }
        
        @Override
        public List<NormalizedBar> getBarsBefore(LocalDateTime beforeTime) {
            return bars.stream()
                    .filter(b -> b.barTime().isBefore(beforeTime))
                    .collect(Collectors.toList());
        }
        
        @Override
        public Long getTradingPairId() {
            return tradingPairId;
        }
        
        @Override
        public String getTimeframe() {
            return timeframe;
        }
    }
}

