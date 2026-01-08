package com.qyl.v2trade.business.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qyl.v2trade.business.system.mapper.TradingPairMapper;
import com.qyl.v2trade.business.system.model.entity.ExchangeMarketPair;
import com.qyl.v2trade.business.system.model.entity.TradingPair;
import com.qyl.v2trade.business.system.service.ExchangeMarketPairService;
import com.qyl.v2trade.business.system.service.TradingPairService;
import com.qyl.v2trade.client.OkxApiClient;
import com.qyl.v2trade.common.constants.EnabledStatus;
import com.qyl.v2trade.common.constants.ExchangeCode;
import com.qyl.v2trade.common.constants.TradingStatus;
import com.qyl.v2trade.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 交易对服务实现类
 */
@Service
public class TradingPairServiceImpl extends ServiceImpl<TradingPairMapper, TradingPair> 
        implements TradingPairService {

    private static final Logger logger = LoggerFactory.getLogger(TradingPairServiceImpl.class);

    @Autowired
    private OkxApiClient okxApiClient;

    @Autowired
    private ExchangeMarketPairService exchangeMarketPairService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public TradingPair getBySymbolAndMarketType(String symbol, String marketType) {
        logger.debug("查询交易对: symbol={}, marketType={}", symbol, marketType);
        
        return getOne(new LambdaQueryWrapper<TradingPair>()
                .eq(TradingPair::getSymbol, symbol)
                .eq(TradingPair::getMarketType, marketType));
    }

    @Override
    public List<TradingPair> listEnabled() {
        logger.debug("查询所有启用的交易对");
        
        return list(new LambdaQueryWrapper<TradingPair>()
                .eq(TradingPair::getEnabled, EnabledStatus.ENABLED)
                .orderByAsc(TradingPair::getSymbol));
    }

    @Override
    public List<TradingPair> listByMarketType(String marketType) {
        logger.debug("查询交易对列表: marketType={}", marketType);
        
        return list(new LambdaQueryWrapper<TradingPair>()
                .eq(TradingPair::getMarketType, marketType)
                .orderByAsc(TradingPair::getSymbol));
    }

    @Override
    public List<TradingPair> listEnabledByMarketType(String marketType) {
        logger.debug("查询启用的交易对列表: marketType={}", marketType);
        
        return list(new LambdaQueryWrapper<TradingPair>()
                .eq(TradingPair::getMarketType, marketType)
                .eq(TradingPair::getEnabled, EnabledStatus.ENABLED)
                .orderByAsc(TradingPair::getSymbol));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TradingPair toggleEnabled(Long id, Integer enabled) {
        logger.info("切换交易对启用状态: id={}, enabled={}", id, enabled);

        TradingPair tradingPair = getById(id);
        if (tradingPair == null) {
            throw new BusinessException(404, "交易对不存在");
        }

        tradingPair.setEnabled(enabled);
        updateById(tradingPair);

        logger.info("交易对启用状态更新成功: id={}, enabled={}", id, enabled);
        return tradingPair;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TradingPair saveOrUpdateBySymbol(TradingPair tradingPair) {
        logger.info("保存或更新交易对: symbol={}, marketType={}", 
                tradingPair.getSymbol(), tradingPair.getMarketType());

        TradingPair existing = getBySymbolAndMarketType(
                tradingPair.getSymbol(), 
                tradingPair.getMarketType());

        if (existing != null) {
            tradingPair.setId(existing.getId());
            tradingPair.setEnabled(existing.getEnabled()); // 保留原启用状态
            updateById(tradingPair);
            logger.info("更新交易对成功: id={}", existing.getId());
        } else {
            tradingPair.setEnabled(EnabledStatus.ENABLED); // 新增默认启用
            save(tradingPair);
            logger.info("新增交易对成功: id={}", tradingPair.getId());
        }

        return tradingPair;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int syncFromOkx(String marketType) {
        logger.info("开始从OKX同步交易对数据: marketType={}", marketType);

        try {
            // 调用OKX接口获取产品列表
            String response = okxApiClient.getInstruments(marketType);
            JsonNode root = objectMapper.readTree(response);

            if (!"0".equals(root.path("code").asText())) {
                String msg = root.path("msg").asText("OKX API调用失败");
                logger.error("OKX API返回错误: {}", msg);
                throw new BusinessException(500, "OKX API调用失败: " + msg);
            }

            JsonNode dataArray = root.path("data");
            if (!dataArray.isArray()) {
                throw new BusinessException(500, "OKX API响应格式异常");
            }

            int count = 0;
            for (JsonNode item : dataArray) {
                try {
                    // 解析交易对信息
                    String instId = item.path("instId").asText();
                    String baseCcy = item.path("baseCcy").asText();
                    String quoteCcy = item.path("quoteCcy").asText();
                    String state = item.path("state").asText();

                    // 构建标准化symbol（去掉-SWAP后缀）
                    String symbol = baseCcy + "-" + quoteCcy;
                    if (baseCcy.isEmpty() || quoteCcy.isEmpty()) {
                        // SWAP合约可能没有baseCcy/quoteCcy，从instId解析
                        String[] parts = instId.split("-");
                        if (parts.length >= 2) {
                            baseCcy = parts[0];
                            quoteCcy = parts[1];
                            symbol = baseCcy + "-" + quoteCcy;
                        } else {
                            logger.warn("无法解析instId: {}", instId);
                            continue;
                        }
                    }

                    // 保存/更新交易对
                    TradingPair tradingPair = new TradingPair();
                    tradingPair.setSymbol(symbol);
                    tradingPair.setBaseCurrency(baseCcy);
                    tradingPair.setQuoteCurrency(quoteCcy);
                    tradingPair.setMarketType(marketType);
                    tradingPair = saveOrUpdateBySymbol(tradingPair);

                    // 保存/更新交易规则
                    ExchangeMarketPair exchangeMarketPair = new ExchangeMarketPair();
                    exchangeMarketPair.setExchangeCode(ExchangeCode.OKX);
                    exchangeMarketPair.setTradingPairId(tradingPair.getId());
                    exchangeMarketPair.setSymbolOnExchange(instId);
                    exchangeMarketPair.setStatus("live".equals(state) ? TradingStatus.TRADING : TradingStatus.SUSPENDED);
                    
                    // 解析精度
                    String tickSz = item.path("tickSz").asText("0");
                    String lotSz = item.path("lotSz").asText("0");
                    exchangeMarketPair.setPricePrecision(getPrecision(tickSz));
                    exchangeMarketPair.setQuantityPrecision(getPrecision(lotSz));
                    
                    // 解析下单限制
                    String minSz = item.path("minSz").asText("0");
                    exchangeMarketPair.setMinOrderQty(new BigDecimal(minSz));
                    exchangeMarketPair.setMinOrderAmount(BigDecimal.ZERO); // OKX不直接提供
                    
                    String maxMktSz = item.path("maxMktSz").asText();
                    if (!maxMktSz.isEmpty()) {
                        exchangeMarketPair.setMaxOrderQty(new BigDecimal(maxMktSz));
                    }
                    
                    String lever = item.path("lever").asText();
                    if (!lever.isEmpty()) {
                        try {
                            exchangeMarketPair.setMaxLeverage(Integer.parseInt(lever));
                        } catch (NumberFormatException ignored) {}
                    }

                    // 保存原始数据
                    Map<String, Object> rawPayload = objectMapper.convertValue(item, 
                            objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, Object.class));
                    exchangeMarketPair.setRawPayload(rawPayload);

                    exchangeMarketPairService.saveOrUpdateByKey(exchangeMarketPair);
                    count++;

                } catch (Exception e) {
                    logger.warn("解析交易对数据失败: {}", item, e);
                }
            }

            logger.info("OKX交易对同步完成: marketType={}, count={}", marketType, count);
            return count;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("同步OKX交易对数据失败", e);
            throw new BusinessException(500, "同步失败: " + e.getMessage());
        }
    }

    /**
     * 根据精度字符串计算小数位数
     * 如 "0.001" -> 3, "0.00001" -> 5
     */
    private int getPrecision(String tickSize) {
        if (tickSize == null || tickSize.isEmpty()) {
            return 0;
        }
        try {
            BigDecimal bd = new BigDecimal(tickSize);
            return Math.max(0, bd.stripTrailingZeros().scale());
        } catch (Exception e) {
            return 0;
        }
    }
}

