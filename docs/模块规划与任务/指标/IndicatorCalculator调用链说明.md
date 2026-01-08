# IndicatorCalculator.onBarClosed 方法调用链说明

## 📋 概述

`IndicatorCalculator.onBarClosed` 方法通过 **Spring 事件监听机制** 被触发，当 K 线聚合完成时会自动调用该方法进行指标计算。

---

## 🔄 完整调用链

```
1. K线聚合完成（KlineAggregator）
   ↓
2. AggregationConfig.aggregationCallback (回调函数)
   ↓
3. AggregatedKLineToBarClosedEventConverter.convert() (转换)
   ↓
4. SimpleBarClosedEventPublisher.publish() (发布事件)
   ↓
5. Spring ApplicationEventPublisher.publishEvent() (Spring事件机制)
   ↓
6. IndicatorCalculator.onBarClosed() (@EventListener 自动触发)
   ↓
7. 异步执行 (@Async("indicatorCalculatorExecutor"))
   ↓
8. 查询订阅 → 计算指标 → 落库
```

---

## 📝 详细流程

### 步骤1：K线聚合完成

**位置**：`KlineAggregator` 聚合器

当接收到 K 线数据并完成聚合时（例如5分钟周期完成），会触发聚合完成回调。

---

### 步骤2：AggregationConfig 回调

**文件**：`src/main/java/com/qyl/v2trade/market/aggregation/config/AggregationConfig.java`

```java
aggregator.setAggregationCallback(aggregatedKLine -> {
    // 1. 发布原有的AggregationEvent
    aggregationEventPublisher.publish(aggregatedKLine);
    
    // 2. 发布BarClosedEvent（指标模块使用）
    if (barClosedEventPublisher != null && aggregatedKLineToBarClosedEventConverter != null) {
        var barClosedEvent = aggregatedKLineToBarClosedEventConverter.convert(aggregatedKLine);
        barClosedEventPublisher.publish(barClosedEvent);
    }
});
```

**说明**：
- `AggregationConfig` 配置了 `KlineAggregator` 的回调函数
- 当聚合完成时，会调用这个回调
- 回调中会发布 `BarClosedEvent`

---

### 步骤3：转换 AggregatedKLine → BarClosedEvent

**文件**：`src/main/java/com/qyl/v2trade/indicator/infrastructure/converter/AggregatedKLineToBarClosedEventConverter.java`

```java
public BarClosedEvent convert(AggregatedKLine aggregatedKLine) {
    // 1. 解析 tradingPairId
    // 2. 计算 bar_close_time (openTime + timeframe_duration)
    // 3. 构建 BarClosedEvent 对象
    return BarClosedEvent.of(...);
}
```

**关键转换**：
- `AggregatedKLine.timestamp()` 是开盘时间（openTime）
- `BarClosedEvent.barCloseTime()` 是收盘时间（closeTime）
- **转换规则**：`barCloseTime = timestamp + timeframe_duration`

---

### 步骤4：发布 BarClosedEvent

**文件**：`src/main/java/com/qyl/v2trade/indicator/domain/event/impl/SimpleBarClosedEventPublisher.java`

```java
public void publish(BarClosedEvent event) {
    // 同步发布给所有订阅者
    for (Consumer<BarClosedEvent> consumer : subscribers) {
        consumer.accept(event);
    }
}
```

**注意**：当前实现使用的是自定义订阅者模式（`Consumer`），但 `IndicatorCalculator` 使用的是 Spring 的 `@EventListener`。

**问题**：这两个机制不匹配！

---

### 步骤5：Spring 事件监听（实际触发）

**文件**：`src/main/java/com/qyl/v2trade/indicator/calculator/IndicatorCalculator.java`

```java
@EventListener
@Async("indicatorCalculatorExecutor")
public void onBarClosed(BarClosedEvent event) {
    // 处理逻辑
}
```

**注解说明**：
- `@EventListener`：Spring 事件监听器，自动监听 `BarClosedEvent` 类型的 Spring 事件
- `@Async("indicatorCalculatorExecutor")`：异步执行，使用指定的线程池

**关键问题**：`SimpleBarClosedEventPublisher` 使用的是自定义发布机制，而不是 Spring 的 `ApplicationEventPublisher`，所以 `@EventListener` 可能无法被触发！

---

## ✅ 当前实现（已修复）

### 事件发布机制

`AggregationConfig` 现在同时使用两种机制：

1. **Spring ApplicationEventPublisher**（主要）：
   - 通过 `applicationEventPublisher.publishEvent(barClosedEvent)` 发布
   - `@EventListener` 可以自动接收并触发

2. **自定义 BarClosedEventPublisher**（向后兼容）：
   - 通过 `SimpleBarClosedEventPublisher` 发布
   - 支持 `Consumer` 订阅者模式（如 `BarSeriesManager`）

### 工作原理

Spring 的 `@EventListener` 可以监听**任意类型**的事件对象（从 Spring 4.2 开始），只要：
- 通过 `ApplicationEventPublisher.publishEvent()` 发布
- 监听方法的参数类型匹配事件类型

所以 `BarClosedEvent` **不需要**继承 `ApplicationEvent`，只要通过 Spring 的 `ApplicationEventPublisher` 发布即可。

---

## 🔍 验证方法

### 检查日志

启动应用后，查看日志中是否有：

```
收到BarClosedEvent，准备计算指标: pairId=..., timeframe=..., barTime=...
```

如果有，说明事件被正确触发。

如果没有，说明事件机制不匹配。

---

## 📊 当前状态

根据代码分析，**当前 `onBarClosed` 方法可能无法被自动触发**，因为：

1. `SimpleBarClosedEventPublisher` 使用自定义机制
2. `@EventListener` 需要 Spring 事件机制

**建议**：修改为使用 Spring 的 `ApplicationEventPublisher` 以确保事件能够正确触发。

