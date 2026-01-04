# 行情订阅包无用代码排查报告

## 排查结果

### ❌ 完全无用的代码（建议删除）

#### 1. **normalizer 包（整个包）**
- `MarketNormalizer.java` - 接口，未被使用
- `OkxMarketNormalizer.java` - 实现类，虽然有 @Component 注解，但没有任何地方注入或调用

**原因分析：**
- 数据转换流程已改为：`KlineChannel` → `KlineEvent` → `EventBus` → `MarketDataCenter.convertToNormalizedKline()`
- `OkxMarketNormalizer` 的两个方法 `normalizeOkxKline()` 和 `normalizeOkxRestKline()` 都没有被调用
- 当前实现中，K线数据标准化直接在 `KlineChannel` 中完成，转换为 `KlineEvent` 后通过 EventBus 传递

**建议：删除整个 `normalizer` 包**

---

#### 2. **MarketWebSocketController.java**
- 位置：`subscription/controller/MarketWebSocketController.java`
- 虽然有 `@Controller` 注解，但实现不完整且未被使用

**问题：**
- 使用了硬编码的 `"client"` 作为 sessionId（第54行、65行）
- 没有实际的业务逻辑，只是简单的消息转发
- WebSocket 消息推送实际由 `WebSocketMarketDistributor` 处理

**建议：删除此文件**

---

### ✅ 正在使用的代码（保留）

#### 1. **MarketDataCenter.java** - 核心服务，被 Spring 自动加载
#### 2. **ingestor 包** - 行情采集，被 MarketDataCenter 使用
#### 3. **websocket 包** - WebSocket 管理，被 ingestor 使用
#### 4. **config 包** - Spring 配置，自动加载
#### 5. **channel 包** - 消息通道，被 config 和 router 使用
#### 6. **eventbus 包** - 事件总线，被多个组件使用
#### 7. **distributor 包** - 消息分发，被 MarketDataCenter 使用
#### 8. **storage 包** - 数据存储，被 MarketDataCenter 使用
#### 9. **cache 包** - 缓存服务，被 MarketDataCenter 和 web 包使用
#### 10. **monitor 包** - 监控服务，被 MarketDataCenter 和 web 包使用
#### 11. **router 包** - 消息路由，被 websocket 和 config 使用

---

## 清理建议

### 可以安全删除的文件：
1. `src/main/java/com/qyl/v2trade/market/subscription/normalizer/` (整个目录)
2. `src/main/java/com/qyl/v2trade/market/subscription/controller/MarketWebSocketController.java`

### 删除后的影响：
- ✅ 不影响现有功能
- ✅ 不会导致编译错误
- ✅ 代码更简洁，减少维护成本

---

## 验证方法

删除前可以运行以下命令验证：
```bash
# 检查编译
mvn clean compile

# 检查是否有引用
grep -r "MarketNormalizer\|OkxMarketNormalizer\|MarketWebSocketController" src/
```

