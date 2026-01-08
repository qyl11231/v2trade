# OKX价格订阅问题分析与修复

## 问题背景

根据OKX官方文档（https://www.okx.com/docs-v5/zh/#public-data-websocket-instruments-channel）和项目日志中的错误信息，发现价格订阅WebSocket参数存在问题。

## 发现的问题

### 1. 端点URL错误 ❌

**错误配置**：
- 使用端点：`wss://ws.okx.com:8443/ws/v5/business`

**正确配置**：
- 应使用端点：`wss://ws.okx.com:8443/ws/v5/public`

**原因分析**：
- 根据OKX API文档，`ticker`频道属于**公共数据频道**，应使用 `/ws/v5/public` 端点
- `/ws/v5/business` 端点用于业务频道（如K线频道），不支持ticker订阅
- 日志错误信息：`"Wrong URL or channel:tickers,instId:BTC-USDT-SWAP doesn't exist"` 证实了端点使用错误

### 2. 频道名称错误 ❌

**错误配置**：
- 使用频道名称：`"tickers"`（复数）

**正确配置**：
- 应使用频道名称：`"ticker"`（单数）

**原因分析**：
- 根据OKX API文档，订阅单个交易对ticker数据的频道名称是 `"ticker"`（单数）
- 代码中 `PriceChannel.java` 已经正确使用了 `"ticker"`（单数），但 `PriceWebSocketManager.java` 使用了 `"tickers"`（复数），导致不一致
- REST API中使用 `/api/v5/market/tickers`（复数）获取多个ticker，但WebSocket订阅频道名称是 `"ticker"`（单数）

## 修复内容

### 修复文件
- `src/main/java/com/qyl/v2trade/market/subscription/collector/websocket/PriceWebSocketManager.java`

### 修复内容

1. **端点URL修复**：
   ```java
   // 修复前
   String url = "wss://ws.okx.com:8443/ws/v5/business";
   
   // 修复后
   String url = "wss://ws.okx.com:8443/ws/v5/public";
   ```

2. **频道名称修复**：
   ```java
   // 修复前
   args.append(String.format("{\"channel\":\"tickers\",\"instId\":\"%s\"}", symbol));
   
   // 修复后
   args.append(String.format("{\"channel\":\"ticker\",\"instId\":\"%s\"}", symbol));
   ```

3. **注释更新**：
   - 更新了相关注释，说明正确的端点使用原因
   - 明确了ticker频道属于公共数据频道，应使用 `/ws/v5/public` 端点

## OKX WebSocket端点说明

根据OKX API文档和项目文档分析：

| 端点 | 用途 | 支持的频道 |
|------|------|-----------|
| `/ws/v5/public` | 公共数据频道 | ticker, trades, books, tickers（批量）, etc. |
| `/ws/v5/business` | 业务频道 | candle（K线）, liquidation-orders, etc. |
| `/ws/v5/private` | 私有频道 | 账户、订单、持仓等需要认证的数据 |

## 正确的订阅格式

### 单个ticker订阅

```json
{
  "op": "subscribe",
  "args": [
    {
      "channel": "ticker",
      "instId": "BTC-USDT-SWAP"
    }
  ]
}
```

**关键参数**：
- `op`: 操作类型，`"subscribe"` 或 `"unsubscribe"`
- `args`: 参数数组
  - `channel`: **必须是 `"ticker"`（单数）**
  - `instId`: 交易对ID，如 `"BTC-USDT-SWAP"`

### 批量ticker订阅

```json
{
  "op": "subscribe",
  "args": [
    {
      "channel": "ticker",
      "instId": "BTC-USDT-SWAP"
    },
    {
      "channel": "ticker",
      "instId": "ETH-USDT-SWAP"
    }
  ]
}
```

## 验证方法

修复后，应验证：

1. **连接成功**：日志中不应再出现 `"Wrong URL or channel"` 错误
2. **订阅成功**：收到订阅确认消息，格式类似：
   ```json
   {
     "event": "subscribe",
     "arg": {
       "channel": "ticker",
       "instId": "BTC-USDT-SWAP"
     },
     "connId": "..."
   }
   ```
3. **数据接收**：能够正常接收ticker数据推送

## 相关文档参考

- OKX WebSocket API文档：https://www.okx.com/docs-v5/zh/#websocket
- 公共数据频道文档：https://www.okx.com/docs-v5/zh/#public-data-websocket-instruments-channel
- 项目内部分析文档：`docs/模块规划与任务/行情/实时价格订阅-WebSocket端点分析.md`

## 注意事项

1. **频道名称区分**：
   - REST API: `/api/v5/market/ticker`（单数）和 `/api/v5/market/tickers`（复数）
   - WebSocket订阅: 频道名称统一为 `"ticker"`（单数），无论订阅单个还是多个交易对

2. **端点选择**：
   - 公共数据（ticker、trades、books等）→ `/ws/v5/public`
   - 业务数据（K线等）→ `/ws/v5/business`
   - 私有数据（账户、订单等）→ `/ws/v5/private`

3. **代码一致性**：
   - `PriceChannel.java` 中已经正确使用 `"ticker"`（单数）
   - 修复后，`PriceWebSocketManager.java` 与 `PriceChannel.java` 保持一致

## 修复日期
2026-01-05

