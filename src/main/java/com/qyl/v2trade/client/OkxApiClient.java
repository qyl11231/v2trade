package com.qyl.v2trade.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * OKX API 客户端
 * 基于 OKX API v5
 *
 * @author qyl
 */
@Slf4j
@Component
public class OkxApiClient {

    // OKX API 地址
    private static final String OKX_API_URL = "https://www.okx.com";
    private static final String OKX_DEMO_API_URL = "https://www.okx.cc"; // 模拟盘使用相同地址,通过flag区分

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 下单接口
     *
     * @param apiKey      API Key
     * @param secretKey   Secret Key
     * @param passphrase  Passphrase
     * @param orderParams 订单参数
     * @param isSimulated 是否模拟盘
     * @return API 响应
     */
    public JsonNode placeOrder(String apiKey, String secretKey, String passphrase,
                               Map<String, Object> orderParams, boolean isSimulated) {
        String endpoint = "/api/v5/trade/order";
        String method = "POST";

        try {
            String body = objectMapper.writeValueAsString(orderParams);
            String timestamp = getTimestamp();

            // 生成签名
            String sign = generateSign(timestamp, method, endpoint, body, secretKey);

            // 构建请求头
            HttpHeaders headers = buildHeaders(apiKey, sign, timestamp, passphrase, isSimulated);

            // 发送请求
            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            String url = getBaseUrl(isSimulated) + endpoint;

            log.info("发送下单请求到OKX: {}", url);
            log.debug("请求体: {}", body);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class
            );

            log.info("OKX响应状态: {}", response.getStatusCode());
            log.debug("OKX响应体: {}", response.getBody());

            return objectMapper.readTree(response.getBody());

        } catch (Exception e) {

            //TODO 下单失败 需要记录 或者邮箱提醒 以及重试
            log.error("OKX下单失败", e);
            throw new RuntimeException("OKX下单失败: " + e.getMessage(), e);
        }
    }

    /**
     * 查询订单
     *
     * @param apiKey      API Key
     * @param secretKey   Secret Key
     * @param passphrase  Passphrase
     * @param instId      产品ID(如:BTC-USDT)
     * @param ordId       订单ID
     * @param isSimulated 是否模拟盘
     * @return API 响应
     */
    public JsonNode queryOrder(String apiKey, String secretKey, String passphrase,
                               String instId, String ordId, boolean isSimulated) {
        String endpoint = "/api/v5/trade/order?instId=" + instId + "&ordId=" + ordId;
        String method = "GET";

        try {
            String timestamp = getTimestamp();
            String sign = generateSign(timestamp, method, endpoint, "", secretKey);

            HttpHeaders headers = buildHeaders(apiKey, sign, timestamp, passphrase, isSimulated);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            String url = getBaseUrl(isSimulated) + endpoint;
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class
            );

            return objectMapper.readTree(response.getBody());

        } catch (Exception e) {
            log.error("查询OKX订单失败", e);
            throw new RuntimeException("查询订单失败: " + e.getMessage(), e);
        }
    }

    /**
     * 取消订单
     *
     * @param apiKey      API Key
     * @param secretKey   Secret Key
     * @param passphrase  Passphrase
     * @param instId      产品ID
     * @param ordId       订单ID
     * @param isSimulated 是否模拟盘
     * @return API 响应
     */
    public JsonNode cancelOrder(String apiKey, String secretKey, String passphrase,
                                String instId, String ordId, boolean isSimulated) {
        String endpoint = "/api/v5/trade/cancel-order";
        String method = "POST";

        try {
            Map<String, String> cancelParams = Map.of(
                    "instId", instId,
                    "ordId", ordId
            );

            String body = objectMapper.writeValueAsString(cancelParams);
            String timestamp = getTimestamp();
            String sign = generateSign(timestamp, method, endpoint, body, secretKey);

            HttpHeaders headers = buildHeaders(apiKey, sign, timestamp, passphrase, isSimulated);
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            String url = getBaseUrl(isSimulated) + endpoint;
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class
            );

            return objectMapper.readTree(response.getBody());

        } catch (Exception e) {
            log.error("取消OKX订单失败", e);
            throw new RuntimeException("取消订单失败: " + e.getMessage(), e);
        }
    }

    /**
     * 查询账户余额
     *
     * @param apiKey      API Key
     * @param secretKey   Secret Key
     * @param passphrase  Passphrase
     * @param isSimulated 是否模拟盘
     * @return API 响应
     */
    public JsonNode getBalance(String apiKey, String secretKey, String passphrase, boolean isSimulated) {
        String endpoint = "/api/v5/account/balance";
        String method = "GET";

        try {
            String timestamp = getTimestamp();
            String sign = generateSign(timestamp, method, endpoint, "", secretKey);

            HttpHeaders headers = buildHeaders(apiKey, sign, timestamp, passphrase, isSimulated);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            String url = getBaseUrl(isSimulated) + endpoint;
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class
            );

            return objectMapper.readTree(response.getBody());

        } catch (Exception e) {
            log.error("查询OKX账户余额失败", e);
            throw new RuntimeException("查询余额失败: " + e.getMessage(), e);
        }
    }

    /**
     * 查询持仓
     *
     * @param apiKey      API Key
     * @param secretKey   Secret Key
     * @param passphrase  Passphrase
     * @param instType    产品类型(SWAP/FUTURES等)
     * @param isSimulated 是否模拟盘
     * @return API 响应
     */
    public JsonNode getPositions(String apiKey, String secretKey, String passphrase,
                                 String instType, boolean isSimulated) {
        String endpoint = "/api/v5/account/positions";
        if (instType != null && !instType.isEmpty()) {
            endpoint += "?instType=" + instType;
        }
        String method = "GET";

        try {
            String timestamp = getTimestamp();
            String sign = generateSign(timestamp, method, endpoint, "", secretKey);

            HttpHeaders headers = buildHeaders(apiKey, sign, timestamp, passphrase, isSimulated);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            String url = getBaseUrl(isSimulated) + endpoint;
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class
            );

            return objectMapper.readTree(response.getBody());

        } catch (Exception e) {
            log.error("查询OKX持仓失败", e);
            throw new RuntimeException("查询持仓失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成签名
     * OKX API v5 签名算法: Base64(HMAC-SHA256(timestamp + method + requestPath + body, secretKey))
     */
    private String generateSign(String timestamp, String method, String requestPath,
                                String body, String secretKey) {
        try {
            String safeBody = body == null ? "" : body;
            String preHash = timestamp + method.toUpperCase() + requestPath + safeBody;

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"
            );
            mac.init(secretKeySpec);

            byte[] hash = mac.doFinal(preHash.getBytes(StandardCharsets.UTF_8));
            String sign = Base64.getEncoder().encodeToString(hash);
            log.info("timestamp={}", timestamp);
            log.info("preHash={}", timestamp + method + requestPath + body);
            log.info("sign={}", sign);
            log.info("body={}", body);

            return sign;

        } catch (Exception e) {
            throw new RuntimeException("生成签名失败", e);
        }
    }

    /**
     * 获取时间戳(ISO 8601格式)
     * 格式: 2024-11-18T10:30:00.123Z
     */
    private String getTimestamp() {
        Instant now = Instant.now();
        return DateTimeFormatter
                .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .withZone(ZoneOffset.UTC)
                .format(now);
    }


    /**
     * 构建请求头
     */
    private HttpHeaders buildHeaders(String apiKey, String sign, String timestamp,
                                     String passphrase, boolean isSimulated) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("OK-ACCESS-KEY", apiKey);
        headers.set("OK-ACCESS-SIGN", sign);
        headers.set("OK-ACCESS-TIMESTAMP", timestamp);
        headers.set("OK-ACCESS-PASSPHRASE", passphrase);

        // 模拟盘标识
        if (isSimulated) {
            headers.set("x-simulated-trading", "1");
        }

        return headers;
    }

    /**
     * 获取基础URL
     */
    private String getBaseUrl(boolean isSimulated) {
        return isSimulated ? OKX_DEMO_API_URL : OKX_API_URL;
    }

    /**
     * 获取交易对信息（默认SWAP）
     * 调用 OKX 接口：/api/v5/public/instruments
     *
     * @return 原始 JSON 字符串
     */
    public String getInstruments() {
        return getInstruments("SWAP");
    }

    /**
     * 获取交易对信息
     * 调用 OKX 接口：/api/v5/public/instruments
     *
     * @param instType 产品类型：SPOT-现货，SWAP-永续合约，FUTURES-交割合约
     * @return 原始 JSON 字符串
     */
    public String getInstruments(String instType) {
        if (instType == null || instType.isEmpty()) {
            instType = "SWAP";
        }
        String endpoint = "/api/v5/public/instruments?instType=" + instType;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            String url = getBaseUrl(false) + endpoint;
            log.info("获取OKX交易对信息: instType={}", instType);
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);
            log.info("OKX响应状态: {}", response.getStatusCode());
            log.debug("OKX响应体: {}", response.getBody());
            return response.getBody();
        } catch (Exception e) {
            log.error("获取OKX交易对信息失败: instType={}", instType, e);
            throw new RuntimeException("获取交易对信息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取ticker价格（公开接口，无需认证）
     * 调用 OKX 接口：/api/v5/market/ticker
     *
     * @param instId 交易对ID，如：BTC-USDT-SWAP
     * @return API 响应，包含最新价格等信息
     */
    public JsonNode getTicker(String instId) {
        String endpoint = "/api/v5/market/ticker?instId=" + instId;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            String url = getBaseUrl(false) + endpoint;
            
            log.debug("获取ticker价格: instId={}, url={}", instId, url);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);
            
            log.debug("OKX ticker响应状态: {}", response.getStatusCode());
            log.debug("OKX ticker响应体: {}", response.getBody());
            
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            log.error("获取OKX ticker价格失败: instId={}", instId, e);
            throw new RuntimeException("获取ticker价格失败: " + e.getMessage(), e);
        }
    }

    /**
     * 批量获取ticker价格（公开接口，无需认证）
     * 调用 OKX 接口：/api/v5/market/tickers
     * 
     * @param instIds 交易对ID列表，用逗号分隔，如："BTC-USDT-SWAP,ETH-USDT-SWAP"
     *                如果不传或传空，则需要指定instType来获取所有交易对
     * @param instType 产品类型，如：SWAP、SPOT、FUTURES等，如果不传则默认使用SWAP
     * @return API 响应，包含多个交易对的最新价格等信息
     */
    public JsonNode getTickers(String instIds, String instType) {
        String endpoint = "/api/v5/market/tickers";
        
        // 如果instType为空，默认使用SWAP（因为马丁策略主要使用SWAP合约）
        if (instType == null || instType.trim().isEmpty()) {
            instType = "SWAP";
        }
        
        // 构建查询参数
        StringBuilder params = new StringBuilder();
        params.append("instType=").append(instType);
        
        if (instIds != null && !instIds.trim().isEmpty()) {
            params.append("&instId=").append(instIds);
        }
        
        endpoint += "?" + params.toString();
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            String url = getBaseUrl(false) + endpoint;
            
            log.debug("批量获取ticker价格: instIds={}, instType={}, url={}", instIds, instType, url);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);
            
            log.debug("OKX tickers响应状态: {}", response.getStatusCode());
            log.debug("OKX tickers响应体长度: {}", response.getBody() != null ? response.getBody().length() : 0);
            
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            log.error("批量获取OKX ticker价格失败: instIds={}, instType={}", instIds, instType, e);
            throw new RuntimeException("批量获取ticker价格失败: " + e.getMessage(), e);
        }
    }

    /**
     * 批量获取ticker价格（使用默认instType=SWAP）
     * 
     * @param instIds 交易对ID列表，用逗号分隔
     * @return API 响应
     */
    public JsonNode getTickers(String instIds) {
        return getTickers(instIds, null);
    }

    /**
     * 设置合约杠杆倍数
     * 调用 OKX 接口：/api/v5/account/set-leverage
     *
     * @param apiKey      API Key
     * @param secretKey   Secret Key
     * @param passphrase  Passphrase
     * @param instId      交易对ID，如：BTC-USDT-SWAP
     * @param lever       杠杆倍数，如：20
     * @param mgnMode     保证金模式（可选）：cross-全仓，isolated-逐仓
     * @param ccy         币种（可选）
     * @param posSide     持仓方向（可选）：long-多，short-空，net-净持仓
     * @param isSimulated 是否模拟盘
     * @return API 响应
     */
    public JsonNode setLeverage(String apiKey, String secretKey, String passphrase,
                                String instId, String lever, String mgnMode,
                                String ccy, String posSide, boolean isSimulated) {
        String endpoint = "/api/v5/account/set-leverage";
        String method = "POST";

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("instId", instId);
            params.put("lever", lever);
            
            // 可选参数
            if (mgnMode != null && !mgnMode.trim().isEmpty()) {
                params.put("mgnMode", mgnMode);
            }
            if (ccy != null && !ccy.trim().isEmpty()) {
                params.put("ccy", ccy);
            }
            if (posSide != null && !posSide.trim().isEmpty()) {
                params.put("posSide", posSide);
            }

            String body = objectMapper.writeValueAsString(params);
            String timestamp = getTimestamp();
            String sign = generateSign(timestamp, method, endpoint, body, secretKey);

            HttpHeaders headers = buildHeaders(apiKey, sign, timestamp, passphrase, isSimulated);
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            String url = getBaseUrl(isSimulated) + endpoint;
            
            log.info("设置杠杆: instId={}, lever={}, mgnMode={}, url={}", instId, lever, mgnMode, url);
            log.debug("请求体: {}", body);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class
            );

            log.info("OKX设置杠杆响应状态: {}", response.getStatusCode());
            log.debug("OKX设置杠杆响应体: {}", response.getBody());

            return objectMapper.readTree(response.getBody());

        } catch (Exception e) {
            log.error("设置OKX杠杆失败: instId={}, lever={}", instId, lever, e);
            throw new RuntimeException("设置杠杆失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取历史K线数据（公开接口，无需认证）
     * 调用 OKX 接口：/api/v5/market/history-candles
     * 用于获取非今天的历史数据
     * 
     * @param instId 交易对ID，如：BTC-USDT-SWAP
     * @param bar K线周期，如：1m, 5m, 15m, 1h, 4h
     * @param after 请求此时间戳之后的数据（可选，毫秒）
     * @param before 请求此时间戳之前的数据（可选，毫秒）
     * @param limit 返回数量，默认100，最大300
     * @return API 响应，包含K线数据数组
     */
    public JsonNode getHistoryCandles(String instId, String bar, Long after, Long before, Integer limit) {
        StringBuilder endpoint = new StringBuilder("/api/v5/market/history-candles");
        StringBuilder params = new StringBuilder();
        
        params.append("instId=").append(instId);
        params.append("&bar=").append(bar);
        
        if (after != null) {
            params.append("&after=").append(after);
        }
        if (before != null) {
            params.append("&before=").append(before);
        }
        if (limit != null) {
            params.append("&limit=").append(Math.min(limit, 300)); // 最大300
        } else {
            params.append("&limit=300"); // 默认300
        }
        
        endpoint.append("?").append(params.toString());
        
        // 重试配置
        int maxRetries = 3;
        long retryDelayMs = 1000; // 1秒
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> entity = new HttpEntity<>(headers);
                String url = getBaseUrl(false) + endpoint.toString();
                
                // 将时间戳转换为可读格式用于日志
                String afterStr = after != null ? java.time.Instant.ofEpochMilli(after).toString() : "null";
                String beforeStr = before != null ? java.time.Instant.ofEpochMilli(before).toString() : "null";
                
                log.info("获取OKX历史K线数据 (尝试 {}/{}): instId={}, bar={}, after={} ({}), before={} ({}), url={}", 
                         attempt, maxRetries, instId, bar, after, afterStr, before, beforeStr, url);
                
                ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);
                
                log.info("OKX历史K线响应状态: {}", response.getStatusCode());
                String responseBody = response.getBody();
                log.info("OKX历史K线响应体长度: {}", responseBody != null ? responseBody.length() : 0);
                
                // 如果响应体不为空且长度小于1000，记录完整响应
                if (responseBody != null && responseBody.length() < 1000) {
                    log.info("OKX历史K线完整响应: {}", responseBody);
                } else if (responseBody != null) {
                    log.info("OKX历史K线响应前500字符: {}", responseBody.substring(0, Math.min(500, responseBody.length())));
                }
                
                return objectMapper.readTree(responseBody);
                
            } catch (ResourceAccessException e) {
                // 网络连接异常
                Throwable rootCause = e.getRootCause();
                String errorMsg = "网络连接失败";
                String suggestion = "";
                
                if (rootCause instanceof ConnectException) {
                    errorMsg = "连接被拒绝 (Connection refused)";
                    suggestion = "可能的原因：\n" +
                            "1. 网络无法访问 www.okx.com（可能需要代理或VPN）\n" +
                            "2. 防火墙或安全软件阻止了连接\n" +
                            "3. OKX服务器暂时不可用\n" +
                            "4. DNS解析失败\n" +
                            "5. 代理配置错误（如果配置了代理）\n" +
                            "建议：检查网络连接、代理配置，或稍后重试";
                } else if (rootCause instanceof SocketTimeoutException) {
                    errorMsg = "连接超时";
                    suggestion = "可能的原因：网络延迟过高或服务器响应慢。建议：检查网络连接或增加超时时间";
                } else if (rootCause instanceof UnknownHostException) {
                    errorMsg = "无法解析域名";
                    suggestion = "无法解析 www.okx.com 域名。建议：检查DNS配置或网络连接";
                }
                
                if (attempt < maxRetries) {
                    log.warn("获取OKX历史K线数据失败 (尝试 {}/{}): {}, 将在{}ms后重试. 详细信息: {}", 
                            attempt, maxRetries, errorMsg, retryDelayMs, suggestion, e);
                    try {
                        Thread.sleep(retryDelayMs * attempt); // 递增延迟
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("重试被中断", ie);
                    }
                } else {
                    log.error("获取OKX历史K线数据失败 (已重试{}次): instId={}, bar={}, url={}, 错误: {}\n{}", 
                            maxRetries, instId, bar, getBaseUrl(false) + endpoint.toString(), errorMsg, suggestion, e);
                    throw new RuntimeException("获取历史K线数据失败: " + errorMsg + ". " + suggestion, e);
                }
                
            } catch (RestClientException e) {
                // 其他HTTP客户端异常
                if (attempt < maxRetries) {
                    log.warn("获取OKX历史K线数据失败 (尝试 {}/{}): {}, 将在{}ms后重试", 
                            attempt, maxRetries, e.getMessage(), retryDelayMs, e);
                    try {
                        Thread.sleep(retryDelayMs * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("重试被中断", ie);
                    }
                } else {
                    log.error("获取OKX历史K线数据失败 (已重试{}次): instId={}, bar={}", 
                            maxRetries, instId, bar, e);
                    throw new RuntimeException("获取历史K线数据失败: " + e.getMessage(), e);
                }
                
            } catch (Exception e) {
                // 其他异常（如JSON解析错误）不重试
                log.error("获取OKX历史K线数据失败: instId={}, bar={}", instId, bar, e);
                throw new RuntimeException("获取历史K线数据失败: " + e.getMessage(), e);
            }
        }
        
        // 理论上不会到达这里
        throw new RuntimeException("获取历史K线数据失败: 未知错误");
    }

    /**
     * 获取K线数据（公开接口，无需认证）
     * 调用 OKX 接口：/api/v5/market/candles
     * 
     * @param instId 交易对ID，如：BTC-USDT-SWAP
     * @param bar K线周期，如：1m, 5m, 15m, 1h, 4h
     * @param after 请求此时间戳之后的数据（可选，毫秒）- 用于获取更旧的历史数据
     * @param before 请求此时间戳之前的数据（可选，毫秒）- 用于获取更新的数据
     * @param limit 返回数量，默认100，最大300
     * @return API 响应，包含K线数据数组
     * 
     * 注意：OKX API返回的数据是按时间倒序排列的（最新的在前）
     * - 使用 after 参数：获取指定时间戳之后的历史数据（更旧的数据）
     * - 使用 before 参数：获取指定时间戳之前的数据（更新的数据，接近当前时间）
     */
    public JsonNode getKlines(String instId, String bar, Long before, Long after, Integer limit) {
        StringBuilder endpoint = new StringBuilder("/api/v5/market/candles");
        StringBuilder params = new StringBuilder();
        
        params.append("instId=").append(instId);
        params.append("&bar=").append(bar);
        
        // OKX API参数说明：
        // after: Unix时间戳（毫秒），请求此时间戳之后的数据
        // before: Unix时间戳（毫秒），请求此时间戳之前的数据
        if (after != null) {
            params.append("&after=").append(after);
        }
        if (before != null) {
            params.append("&before=").append(before);
        }
        if (limit != null) {
            params.append("&limit=").append(Math.min(limit, 300)); // 最大300
        } else {
            params.append("&limit=300"); // 默认300
        }
        
        endpoint.append("?").append(params.toString());
        
        // 重试配置
        int maxRetries = 3;
        long retryDelayMs = 1000; // 1秒
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> entity = new HttpEntity<>(headers);
                String url = getBaseUrl(false) + endpoint.toString();
                
                // 将时间戳转换为可读格式用于日志
                String afterStr = after != null ? java.time.Instant.ofEpochMilli(after).toString() : "null";
                String beforeStr = before != null ? java.time.Instant.ofEpochMilli(before).toString() : "null";
                
                log.info("获取OKX K线数据 (尝试 {}/{}): instId={}, bar={}, after={} ({}), before={} ({}), url={}", 
                         attempt, maxRetries, instId, bar, after, afterStr, before, beforeStr, url);
                
                ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);
                
                log.info("OKX K线响应状态: {}", response.getStatusCode());
                String responseBody = response.getBody();
                log.info("OKX K线响应体长度: {}", responseBody != null ? responseBody.length() : 0);
                
                // 如果响应体不为空且长度小于1000，记录完整响应
                if (responseBody != null && responseBody.length() < 1000) {
                    log.info("OKX K线完整响应: {}", responseBody);
                } else if (responseBody != null) {
                    log.info("OKX K线响应前500字符: {}", responseBody.substring(0, Math.min(500, responseBody.length())));
                }
                
                return objectMapper.readTree(responseBody);
                
            } catch (ResourceAccessException e) {
                // 网络连接异常
                Throwable rootCause = e.getRootCause();
                String errorMsg = "网络连接失败";
                String suggestion = "";
                
                if (rootCause instanceof ConnectException) {
                    errorMsg = "连接被拒绝 (Connection refused)";
                    suggestion = "可能的原因：\n" +
                            "1. 网络无法访问 www.okx.com（可能需要代理或VPN）\n" +
                            "2. 防火墙或安全软件阻止了连接\n" +
                            "3. OKX服务器暂时不可用\n" +
                            "4. DNS解析失败\n" +
                            "5. 代理配置错误（如果配置了代理）\n" +
                            "建议：检查网络连接、代理配置，或稍后重试";
                } else if (rootCause instanceof SocketTimeoutException) {
                    errorMsg = "连接超时";
                    suggestion = "可能的原因：网络延迟过高或服务器响应慢。建议：检查网络连接或增加超时时间";
                } else if (rootCause instanceof UnknownHostException) {
                    errorMsg = "无法解析域名";
                    suggestion = "无法解析 www.okx.com 域名。建议：检查DNS配置或网络连接";
                }
                
                if (attempt < maxRetries) {
                    log.warn("获取OKX K线数据失败 (尝试 {}/{}): {}, 将在{}ms后重试. 详细信息: {}", 
                            attempt, maxRetries, errorMsg, retryDelayMs, suggestion, e);
                    try {
                        Thread.sleep(retryDelayMs * attempt); // 递增延迟
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("重试被中断", ie);
                    }
                } else {
                    log.error("获取OKX K线数据失败 (已重试{}次): instId={}, bar={}, url={}, 错误: {}\n{}", 
                            maxRetries, instId, bar, getBaseUrl(false) + endpoint.toString(), errorMsg, suggestion, e);
                    throw new RuntimeException("获取K线数据失败: " + errorMsg + ". " + suggestion, e);
                }
                
            } catch (RestClientException e) {
                // 其他HTTP客户端异常
                if (attempt < maxRetries) {
                    log.warn("获取OKX K线数据失败 (尝试 {}/{}): {}, 将在{}ms后重试", 
                            attempt, maxRetries, e.getMessage(), retryDelayMs, e);
                    try {
                        Thread.sleep(retryDelayMs * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("重试被中断", ie);
                    }
                } else {
                    log.error("获取OKX K线数据失败 (已重试{}次): instId={}, bar={}", 
                            maxRetries, instId, bar, e);
                    throw new RuntimeException("获取K线数据失败: " + e.getMessage(), e);
                }
                
            } catch (Exception e) {
                // 其他异常（如JSON解析错误）不重试
                log.error("获取OKX K线数据失败: instId={}, bar={}", instId, bar, e);
                throw new RuntimeException("获取K线数据失败: " + e.getMessage(), e);
            }
        }
        
        // 理论上不会到达这里
        throw new RuntimeException("获取K线数据失败: 未知错误");
    }

    /**
     * 获取K线数据（简化版本，使用时间戳范围）
     * 
     * @param instId 交易对ID
     * @param bar K线周期（如：1m, 5m, 1H）
     * @param fromTimestamp 开始时间戳（毫秒，包含）
     * @param toTimestamp 结束时间戳（毫秒，包含）
     * @return API 响应
     * 
     * OKX API参数说明：
     * - after: Unix时间戳（毫秒），请求此时间戳之后的数据
     * - before: Unix时间戳（毫秒），请求此时间戳之前的数据
     * - limit: 返回数量，最大300
     * 
     * 注意：OKX返回的数据是按时间倒序排列的（最新的在前）
     */
    public JsonNode getKlines(String instId, String bar, long fromTimestamp, long toTimestamp) {
        // OKX API参数：
        // after: 请求此时间戳之后的数据（fromTimestamp）
        // before: 请求此时间戳之前的数据（toTimestamp）
        // 注意：OKX使用Unix时间戳（毫秒）
        return getKlines(instId, bar, fromTimestamp, toTimestamp, 300);
    }
}