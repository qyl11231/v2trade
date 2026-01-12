package com.qyl.v2trade.market.web.query;

import com.qyl.v2trade.market.model.NormalizedKline;

import java.time.Instant;
import java.util.List;

/**
 * 行情查询服务接口
 * 
 * 重构：按照时间管理约定，使用 Instant 作为参数类型
 */
public interface MarketQueryService {

    /**
     * 查询K线数据
     * 
     * 重构：按照时间管理约定，使用 Instant 作为参数类型
     * 在数据库查询边界（实现类内部）将 Instant 转换为 Timestamp
     * 
     * @param symbol 交易对符号（如：BTC-USDT）
     * @param interval K线周期（如：1m, 5m）
     * @param fromTime 开始时间（UTC Instant），可为null
     * @param toTime 结束时间（UTC Instant），可为null
     * @param limit 限制返回数量，可为null（默认1000，最大10000）
     * @return K线列表，按时间戳升序排列
     */
    List<NormalizedKline> queryKlines(String symbol, String interval, 
                                     Instant fromTime, Instant toTime, Integer limit);

    /**
     * 查询最新一根K线
     * 
     * @param symbol 交易对符号
     * @param interval K线周期
     * @return 最新K线，不存在返回null
     */
    NormalizedKline queryLatestKline(String symbol, String interval);

    /**
     * 查询指定时间点的K线
     * 
     * 重构：按照时间管理约定，使用 Instant 作为参数类型
     * 
     * @param symbol 交易对符号
     * @param interval K线周期
     * @param timestamp 时间（UTC Instant）
     * @return K线，不存在返回null
     */
    NormalizedKline queryKlineByTimestamp(String symbol, String interval, Instant timestamp);
}

