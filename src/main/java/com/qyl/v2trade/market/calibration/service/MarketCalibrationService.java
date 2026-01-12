package com.qyl.v2trade.market.calibration.service;

import java.time.Instant;

/**
 * 市场校准服务接口
 * 
 * <p>提供统一的补拉能力，可被定时任务和BackfillTrigger复用
 *
 * @author qyl
 */
public interface MarketCalibrationService {

    /**
     * 补拉最近1小时的数据
     * 
     * <p>此方法会被以下场景调用：
     * <ul>
     *   <li>定时任务（已有）</li>
     *   <li>BackfillTrigger（新增）</li>
     * </ul>
     * 
     * <p>补拉写入QuestDB必须幂等（允许重复写但不会造成脏数据/重复数据）
     * 
     * @param tradingPairId 交易对ID
     * @param endTime 结束时间（Instant，UTC），如果为null则使用当前时间
     */
    void backfillLastHour(Long tradingPairId, Instant endTime);
}

