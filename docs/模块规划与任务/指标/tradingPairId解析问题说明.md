# tradingPairId 解析问题说明

## 🔴 问题描述

`BarSeriesManager.onBarClosed()` 收到的 `BarClosedEvent` 中 `tradingPairId` 为 `null`。

---

## 🔍 问题根源

### 1. Symbol 格式不匹配

**AggregatedKLine.symbol** 的可能格式：
- `"BTC-USDT-SWAP"`（OKX 格式，包含市场类型）
- `"BTC-USDT"`（标准格式）

**trading_pair 表的 symbol 字段**：
- 标准格式：`"BTC-USDT"`
- 同时有 `market_type` 字段：`"SPOT"`, `"SWAP"`, `"FUTURES"`

### 2. 解析逻辑不足

原来的 `DefaultTradingPairResolver.symbolToTradingPairId()` 只做了简单的字符串匹配：
```java
// 只尝试直接匹配 symbol
.eq(TradingPair::getSymbol, symbol)
```

如果 `symbol` 是 `"BTC-USDT-SWAP"`，但数据库中是 `"BTC-USDT"` + `market_type="SWAP"`，就会匹配失败。

---

## ✅ 解决方案

### 增强 DefaultTradingPairResolver

实现多策略解析：

1. **策略1：提取市场类型后精确匹配**
   - 如果 symbol 是 `"BTC-USDT-SWAP"`，提取 `baseSymbol="BTC-USDT"`, `marketType="SWAP"`
   - 查询：`WHERE symbol='BTC-USDT' AND market_type='SWAP'`

2. **策略2：直接匹配 symbol**
   - 如果 symbol 是标准格式 `"BTC-USDT"`，直接匹配

3. **策略3：通过 baseSymbol 匹配**
   - 如果提取了 baseSymbol，尝试匹配 baseSymbol（取第一个结果）

### 代码变更

**文件**：`src/main/java/com/qyl/v2trade/indicator/infrastructure/resolver/impl/DefaultTradingPairResolver.java`

- 增强 `symbolToTradingPairId()` 方法
- 支持解析 `-SWAP`、`-FUTURES` 后缀
- 添加详细的调试日志

**文件**：`src/main/java/com/qyl/v2trade/indicator/infrastructure/converter/AggregatedKLineToBarClosedEventConverter.java`

- 增强错误日志
- 明确记录解析成功/失败的情况

---

## 📋 调用链

```
AggregatedKLine.symbol = "BTC-USDT-SWAP"
    ↓
AggregatedKLineToBarClosedEventConverter.convert()
    ↓
TradingPairResolver.symbolToTradingPairId("BTC-USDT-SWAP")
    ↓
DefaultTradingPairResolver.symbolToTradingPairId()
    ↓
1. 提取：baseSymbol="BTC-USDT", marketType="SWAP"
2. 查询数据库：WHERE symbol='BTC-USDT' AND market_type='SWAP'
3. 返回 trading_pair.id
    ↓
BarClosedEvent.of(tradingPairId=712, ...)
    ↓
BarSeriesManager.onBarClosed(event) ✅ tradingPairId 不为空
```

---

## 🧪 验证方法

### 1. 检查日志

启动应用后，查看日志：

**成功情况**：
```
成功解析tradingPairId: symbol=BTC-USDT-SWAP -> id=712 (marketType=SWAP)
```

**失败情况**：
```
未找到tradingPairId: symbol=BTC-USDT-SWAP, baseSymbol=BTC-USDT, marketType=SWAP
```

### 2. 验证数据库

确保 `trading_pair` 表中有对应记录：

```sql
SELECT id, symbol, market_type 
FROM trading_pair 
WHERE symbol = 'BTC-USDT' AND market_type = 'SWAP';
```

### 3. 检查缓存

`DefaultTradingPairResolver` 使用内存缓存，解析成功后会被缓存，后续查询会更快。

---

## 🔧 故障排查

### 如果仍然返回 null

1. **检查 symbol 格式**
   - 查看 `AggregatedKLine.symbol()` 的实际值
   - 确认数据库中的 `symbol` 和 `market_type` 格式

2. **检查数据库数据**
   ```sql
   SELECT * FROM trading_pair WHERE symbol LIKE '%BTC%';
   ```

3. **检查 TradingPairMapper 是否注入**
   - 查看启动日志，是否有警告：`TradingPairMapper未注入`

4. **手动测试解析**
   ```java
   TradingPairResolver resolver = ...;
   Long id = resolver.symbolToTradingPairId("BTC-USDT-SWAP");
   System.out.println("解析结果: " + id);
   ```

---

## 📝 注意事项

1. **缓存机制**：解析结果会被缓存，如果数据库数据更新，需要重启应用或清空缓存

2. **性能考虑**：首次查询会访问数据库，后续查询使用缓存，性能更好

3. **兼容性**：支持多种 symbol 格式，向后兼容

