# 价格订阅 WebSocket 端点分析

## 问题背景

根据 OKX API 文档和公告（2023年4月24日）：

- **K线频道（candle）**：已迁移到 `/ws/v5/business` 端点
- **公共频道（ticker/trades/books等）**：使用 `/ws/v5/public` 端点

当前系统：
- `ExchangeWebSocketManager` 使用单一 WebSocket 连接
- 配置的 URL 为：`wss://ws.okx.com:8443/ws/v5/business`（K线专用）
- 价格订阅（ticker）需要：`wss://ws.okx.com:8443/ws/v5/public`

## 问题影响

如果价格订阅和K线订阅使用不同的端点，当前架构存在以下问题：

1. **单一连接限制**：`ExchangeWebSocketManager` 只能连接一个端点
2. **订阅冲突**：在 `/ws/v5/business` 上无法订阅 ticker 频道
3. **架构耦合**：K线和价格订阅共享同一个连接管理器

## 解决方案选项

### 方案A：双连接架构（推荐，但复杂度高）

**设计思路**：
- 创建两个独立的 WebSocket 连接
- `KlineWebSocketManager` → `/ws/v5/business`（K线专用）
- `PriceWebSocketManager` → `/ws/v5/public`（价格/公共频道专用）
- 两个连接独立管理，互不干扰

**优点**：
- ✅ 符合 OKX API 规范
- ✅ 连接隔离，故障互不影响PriceSubscriptionService
- ✅ 扩展性好（未来可支持 trades、orderbook 等公共频道）

**缺点**： 
- ❌ 需要重构现有 `ExchangeWebSocketManager`
- ❌ 增加连接管理复杂度
- ❌ 需要维护两套连接状态（心跳、重连等）

**实施难度**：⭐⭐⭐⭐（高）

---

### 方案B：统一使用 /ws/v5/public（需验证）

**设计思路**：
- 验证 `/ws/v5/public` 是否支持订阅 K线频道
- 如果支持，统一使用 `/ws/v5/public`
- 如果不支持，此方案不可行

**优点**：
- ✅ 代码改动最小
- ✅ 单一连接，简单清晰

**缺点**：
- ❌ 根据OKX公告，K线已迁移到 `/business`，可能不支持
- ❌ 需要实际测试验证

**实施难度**：⭐⭐（低，但需验证）

---

### 方案C：保持现状，价格订阅使用 HTTP 轮询（临时方案）

**设计思路**：
- 价格订阅暂不使用 WebSocket
- 使用 OKX REST API `/api/v5/market/ticker` 定期轮询
- 或使用 `/api/v5/market/tickers` 批量获取

**优点**：
- ✅ 无需改动现有 WebSocket 架构
- ✅ 实现简单，快速上线

**缺点**：
- ❌ 非实时（有轮询延迟）
- ❌ 增加 HTTP 请求压力
- ❌ 不符合"实时价格订阅"的产品定位

**实施难度**：⭐⭐（低）

---

### 方案D：抽象连接管理器，支持多端点（最佳长期方案）

**设计思路**：
- 抽象 `WebSocketConnectionManager` 接口
- 实现 `BusinessWebSocketManager`（K线）和 `PublicWebSocketManager`（价格）
- `ExchangeWebSocketManager` 作为门面，管理多个连接管理器
- 每个连接管理器独立管理自己的连接、订阅、重连

**架构示例**：
```java
interface WebSocketConnectionManager {
    void connect();
    void subscribe(Set<String> symbols, ChannelType channelType);
    void disconnect();
}

class BusinessWebSocketManager implements WebSocketConnectionManager {
    // 管理 /ws/v5/business 连接（K线）
}

class PublicWebSocketManager implements WebSocketConnectionManager {
    // 管理 /ws/v5/public 连接（价格/ticker）
}

class ExchangeWebSocketManager {
    private BusinessWebSocketManager klineManager;
    private PublicWebSocketManager priceManager;
    // 统一门面
}
```

**优点**：
- ✅ 架构清晰，职责分明
- ✅ 易于扩展（未来支持更多频道类型）
- ✅ 符合单一职责原则
- ✅ 连接故障隔离

**缺点**：
- ❌ 需要较大重构
- ❌ 开发周期较长

**实施难度**：⭐⭐⭐⭐⭐（最高，但最合理）

---

## 推荐方案

### 短期方案（快速上线）：方案B → 方案C

1. **先验证方案B**：测试 `/ws/v5/public` 是否支持 K线订阅
   - 如果支持：统一使用 `/ws/v5/public`，最小改动
   - 如果不支持：采用方案C（HTTP轮询）作为临时方案

2. **中期优化（方案A）**：实现双连接架构
   - 在 Phase 4 或后续版本中实施
   - 为价格订阅创建独立的 WebSocket 连接

### 长期方案（架构优化）：方案D

- 在系统演进过程中逐步实施
- 抽象连接管理层，支持多端点、多频道类型

---

## 验证步骤

1. **验证 OKX API 文档**
   - 查阅最新 API 文档，确认端点限制
   - 确认 `/ws/v5/public` 是否支持 candle 频道

2. **实际测试**
   - 连接到 `/ws/v5/public`
   - 尝试订阅 candle 频道
   - 观察是否成功接收数据

3. **验证 ticker 频道**
   - 连接到 `/ws/v5/business`
   - 尝试订阅 ticker 频道
   - 观察是否成功接收数据

---

## 决策建议

**如果验证结果**：

- ✅ `/ws/v5/public` 支持 K线 → **采用方案B**（统一端点）
- ❌ `/ws/v5/public` 不支持 K线 → **短期方案C + 长期方案A/D**

**建议优先级**：
1. 先做验证（方案B）
2. 验证失败则临时使用 HTTP（方案C）
3. 后续版本重构为双连接（方案A）或抽象架构（方案D）

