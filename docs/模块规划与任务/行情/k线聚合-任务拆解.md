# 📋 K线聚合模块 - 任务拆解

> **版本**：v1.0  
> **状态**：待开始  
> **预计总工期**：11天  
> **基于文档**：`k线聚合-设计开发规范.md`

---

## 📊 任务总览

| Phase | 阶段名称 | 任务数 | 预计工期 | 状态 | 完成时间 | 下一阶段开启 |
|-------|---------|--------|---------|------|---------|------------|
| Phase 1 | 基础架构搭建 | 3 | 2天 | ⏳ 待开始  | -  | 等待Phase 1完成 |
| Phase 2 | 核心聚合逻辑 | 4 | 3.5天 | ⏳ 待开始 | - | 等待Phase 2完成 |
| Phase 3 | 持久化层 | 3 | 2天 | 🔒 已锁定 | - | 等待Phase 2完成 |
| Phase 4 | 集成和优化 | 3 | 2天 | 🔒 已锁定 | - | 等待Phase 3完成 |
| Phase 5 | 测试和文档 | 3 | 2天 | 🔒 已锁定 | - | 等待Phase 4完成 |
| **总计** | | **16** | **11.5天** | | | |

### 阶段开启规则

**阶段状态说明**：
- ⏳ **待开始**：阶段尚未开始，可以开始开发
- 🔄 **进行中**：阶段正在进行中
- ✅ **已完成**：阶段所有任务已完成
- 🔒 **已锁定**：等待前置阶段完成，无法开始

**开启下一阶段**：
- 当前阶段所有任务完成后，状态自动变为"✅ 已完成"
- 需要**明确同意**才能开启下一阶段
- 同意方式：在文档中标记"✅ 同意开启下一阶段"
- 开启后，下一阶段状态变为"⏳ 待开始"

**阶段依赖关系**：
- Phase 2 依赖 Phase 1 完成
- Phase 3 依赖 Phase 2 完成
- Phase 4 依赖 Phase 3 完成
- Phase 5 依赖 Phase 4 完成

---

## Phase 1: 基础架构搭建（2天）

**阶段状态**：✅ 已完成  
**前置条件**：无  
**完成条件**：所有任务验收通过  
**开启下一阶段**：✅ 同意开启下一阶段（Phase 2）- 已同意（2025-01-15）

---

### 任务1.1：创建包结构和基础类

**负责人**：开发工程师  
**预计工期**：0.5天  
**优先级**：P0（阻塞）

#### 任务描述
创建聚合模块的包结构，定义核心数据模型和枚举类。

#### 具体步骤

1. **创建包结构**
   ```
   com.qyl.v2trade.market.aggregation
   ├── core/
   ├── event/
   ├── persistence/
   ├── config/
   └── listener/
   ```

2. **创建AggregatedKLine事件模型**
   - 文件：`event/AggregatedKLine.java`
   - 字段：symbol, period, timestamp, open, high, low, close, volume, sourceKlineCount
   - 要求：不可变（record），可序列化

3. **创建SupportedPeriod枚举**
   - 文件：`config/SupportedPeriod.java`
   - 枚举值：M5("5m"), M15("15m"), M30("30m"), H1("1h"), H4("4h")
   - 字段：period（String）, durationMs（long）

4. **创建PeriodCalculator工具类**
   - 文件：`core/PeriodCalculator.java`
   - 方法：
     - `calculateWindowStart(long timestamp, SupportedPeriod period)`
     - `calculateWindowEnd(long windowStart, SupportedPeriod period)`
     - `alignTimestamp(long timestamp, SupportedPeriod period)`

#### 验收标准

- [ ] 所有类编译通过
- [ ] 包结构符合设计规范
- [ ] `AggregatedKLine` 为不可变record
- [ ] `SupportedPeriod` 包含5个周期
- [ ] `PeriodCalculator` 方法签名正确

#### 依赖关系
- 无前置依赖

#### 输出物
- 源代码文件（4个类）
- 代码审查通过

---

### 任务1.2：实现AggregationBucket

**负责人**：开发工程师  
**预计工期**：0.5天  
**优先级**：P0（阻塞）

#### 任务描述
实现聚合桶类，用于维护一个时间窗口内的聚合状态。

#### 具体步骤

1. **创建AggregationBucket类**
   - 文件：`core/AggregationBucket.java`
   - 字段：
     - symbol, period, windowStart, windowEnd
     - open, high, low, close, volume
     - klineCount, isComplete

2. **实现状态更新方法**
   - `update(KlineEvent event)` - 更新OHLCV
   - `merge(AggregationBucket other)` - 合并两个Bucket（可选）

3. **实现窗口判断方法**
   - `isWindowComplete(long klineTimestamp)` - 判断窗口是否结束
   - `isExpired(long currentTime)` - 判断是否过期（用于清理）

4. **实现线程安全**
   - 使用 `synchronized` 或 `AtomicReference` 保护状态

5. **编写单元测试**
   - 文件：`core/AggregationBucketTest.java`
   - 测试用例：
     - 正常更新流程
     - OHLCV计算正确性
     - 窗口结束判断
     - 线程安全测试

#### 验收标准

- [ ] Bucket状态更新正确
- [ ] OHLCV计算逻辑正确
- [ ] 窗口结束判断准确
- [ ] 线程安全测试通过
- [ ] 单元测试覆盖率 > 80%

#### 依赖关系
- 依赖：任务1.1（PeriodCalculator）
- 依赖：`KlineEvent`（已有）

#### 输出物
- `AggregationBucket.java`
- `AggregationBucketTest.java`
- 测试报告

---

### 任务1.3：实现PeriodCalculator

**负责人**：开发工程师  
**预计工期**：1天  
**优先级**：P0（阻塞）

#### 任务描述
实现周期计算工具类，用于计算时间窗口的起始和结束时间。

#### 具体步骤

1. **实现窗口起始时间计算**
   ```java
   public static long calculateWindowStart(long timestamp, SupportedPeriod period)
   ```
   - 规则：将时间戳对齐到周期边界
   - 示例：10:03 -> 10:00（5m周期）

2. **实现窗口结束时间计算**
   ```java
   public static long calculateWindowEnd(long windowStart, SupportedPeriod period)
   ```
   - 规则：windowStart + durationMs

3. **实现时间戳对齐**
   ```java
   public static long alignTimestamp(long timestamp, SupportedPeriod period)
   ```
   - 规则：对齐到周期起始点

4. **编写单元测试**
   - 文件：`core/PeriodCalculatorTest.java`
   - 测试用例：
     - 5m周期窗口计算
     - 15m周期窗口计算
     - 30m周期窗口计算
     - 1h周期窗口计算
     - 4h周期窗口计算
     - 边界情况（整点、跨天等）

#### 验收标准

- [ ] 所有周期窗口计算正确
- [ ] 边界情况处理正确
- [ ] 单元测试覆盖率 100%
- [ ] 性能测试通过（计算延迟 < 0.1ms）

#### 依赖关系
- 依赖：任务1.1（SupportedPeriod）

#### 输出物
- `PeriodCalculator.java`
- `PeriodCalculatorTest.java`
- 测试报告

---

## Phase 2: 核心聚合逻辑（3.5天）

**阶段状态**：⏳ 待开始  
**前置条件**：Phase 1 所有任务完成 + 同意开启 ✅  
**完成条件**：所有任务验收通过  
**开启下一阶段**：✅ 同意开启下一阶段（Phase 3）

---

### 任务2.1：实现KlineAggregator核心逻辑

**负责人**：开发工程师  
**预计工期**：2天  
**优先级**：P0（核心）

#### 任务描述
实现K线聚合器的核心逻辑，包括Bucket管理、事件处理、多周期聚合。

#### 具体步骤

1. **创建KlineAggregator接口和实现**
   - 接口：`core/KlineAggregator.java`
   - 实现：`core/impl/KlineAggregatorImpl.java`
   - 方法：
     - `onKlineEvent(KlineEvent event)`
     - `getStats()` - 获取统计信息
     - `cleanupExpiredBuckets()` - 清理过期Bucket

2. **实现Bucket管理**
   - 使用 `ConcurrentHashMap<String, AggregationBucket>` 存储
   - Key格式：`{symbol}_{period}_{windowStart}`
   - 实现Bucket创建、查找、清理逻辑

3. **实现事件处理流程**
   ```
   1. 接收1m KlineEvent
   2. 遍历所有支持的周期
   3. 计算该K线所属的窗口
   4. 找到或创建对应的Bucket
   5. 更新Bucket状态
   6. 判断窗口是否结束
   7. 如果结束，生成聚合结果并发布
   ```

4. **实现多周期并发聚合**
   - 支持同时聚合5个周期（5m, 15m, 30m, 1h, 4h）
   - 每个周期独立维护Bucket

5. **实现去重机制**
   - 检查Bucket中是否已包含该时间戳的K线
   - 如果已包含，跳过更新

6. **编写单元测试**
   - 文件：`core/impl/KlineAggregatorImplTest.java`
   - 测试用例：
     - 单周期正常聚合
     - 多周期并发聚合
     - 窗口结束触发
     - 重复数据去重
     - 边界情况（跨窗口）

#### 验收标准

- [ ] 单周期聚合正确
- [ ] 多周期并发聚合正确
- [ ] 窗口结束判断准确
- [ ] 去重机制有效
- [ ] 单元测试覆盖率 > 85%
- [ ] 性能测试通过（单symbol > 1000 events/s）

#### 依赖关系
- 依赖：Phase 1 所有任务
- 依赖：`KlineEvent`（已有）
- 依赖：`MarketEventBus`（已有）

#### 输出物
- `KlineAggregator.java`
- `KlineAggregatorImpl.java`
- `KlineAggregatorImplTest.java`
- 测试报告

---

### 任务2.2：实现事件订阅和发布

**负责人**：开发工程师  
**预计工期**：0.5天  
**优先级**：P0（核心）

#### 任务描述
实现事件订阅（消费1m K线事件）和事件发布（发布聚合完成事件）。

#### 具体步骤

1. **实现KlineEventConsumer**
   - 文件：`listener/KlineEventConsumer.java`
   - 职责：订阅MarketEventBus的KlineEvent
   - 过滤：只处理interval="1m"的事件
   - 调用：`aggregator.onKlineEvent(event)`

2. **实现AggregationEventPublisher**
   - 接口：`event/AggregationEventPublisher.java`
   - 实现：`event/impl/AggregationEventPublisherImpl.java`
   - 方法：`publish(AggregatedKLine aggregatedKLine)`
   - 实现：通过MarketEventBus发布事件

3. **集成到Spring**
   - 使用 `@Component` 注解
   - 在 `@PostConstruct` 中订阅事件
   - 配置Bean依赖注入

4. **编写集成测试**
   - 文件：`listener/KlineEventConsumerTest.java`
   - 测试用例：
     - 事件订阅正常
     - 事件发布正常
     - 事件流完整性

#### 验收标准

- [ ] 事件订阅成功
- [ ] 事件发布成功
- [ ] 事件流无丢失
- [ ] 集成测试通过

#### 依赖关系
- 依赖：任务2.1（KlineAggregator）
- 依赖：`MarketEventBus`（已有）

#### 输出物
- `KlineEventConsumer.java`
- `AggregationEventPublisher.java`
- `AggregationEventPublisherImpl.java`
- 集成测试代码

---

### 任务2.3：实现内存管理和清理

**负责人**：开发工程师  
**预计工期**：0.5天  
**优先级**：P1（重要）

#### 任务描述
实现过期Bucket清理机制，防止内存泄漏。

#### 具体步骤

1. **实现清理逻辑**
   - 在 `KlineAggregator` 中实现 `cleanupExpiredBuckets()`
   - 规则：清理窗口结束超过1小时的Bucket
   - 使用定时任务（`@Scheduled`）定期清理

2. **实现内存监控**
   - 统计活跃Bucket数量
   - 统计内存使用量
   - 提供监控指标接口

3. **编写单元测试**
   - 测试清理逻辑
   - 测试内存监控

#### 验收标准

- [ ] 过期Bucket正确清理
- [ ] 内存使用稳定
- [ ] 监控指标正常
- [ ] 无内存泄漏

#### 依赖关系
- 依赖：任务2.1（KlineAggregator）

#### 输出物
- 清理逻辑代码
- 监控代码
- 测试代码

---

### 任务2.4：实现启动时历史数据补齐

**负责人**：开发工程师  
**预计工期**：1天  
**优先级**：P0（核心）

#### 任务描述
实现系统启动时的历史数据补齐机制，确保启动前未完成的聚合窗口能够被正确补齐。

#### 问题场景
- 系统在10:30启动，订阅1小时K线
- 当前窗口是 [10:00, 11:00)，已经进行了30分钟
- 如果不补齐，只会聚合10:30之后的数据，导致10:00-10:30的数据丢失

#### 具体步骤

1. **实现窗口扫描逻辑**
   - 方法：`scanIncompleteWindows(String symbol, SupportedPeriod period, long currentTime)`
   - 功能：扫描当前时间点之前所有未完成的窗口
   - 规则：
     - 扫描范围：最多最近24小时（可配置）
     - 检查每个窗口是否已有聚合数据
     - 如果没有，加入补齐列表

2. **实现历史数据读取**
   - 从QuestDB读取指定窗口内的所有1m K线数据
   - 使用 `QuestDbMarketQueryService` 查询
   - 时间范围：`[windowStart, windowEnd)`

3. **实现历史数据聚合**
   - 方法：`backfillWindow(String symbol, SupportedPeriod period, long windowStart)`
   - 流程：
     - 读取窗口内的1m数据
     - 创建AggregationBucket
     - 遍历1m数据，更新Bucket
     - 生成聚合结果
     - 写入QuestDB（幂等）
     - 不发布事件（避免重复通知下游）

4. **实现初始化逻辑**
   - 创建 `KlineAggregatorInitializer` 类
   - 在 `@PostConstruct` 中调用初始化
   - 支持异步执行（不阻塞启动）
   - 遍历所有交易对和周期

5. **实现配置项**
   - `enableHistoryBackfill`：是否启用历史数据补齐（默认true）
   - `historyScanHours`：历史数据扫描范围（小时，默认24）
   - `asyncInitialization`：是否异步初始化（默认true）

6. **编写单元测试**
   - 文件：`core/impl/KlineAggregatorInitializerTest.java`
   - 测试用例：
     - 窗口扫描正确性
     - 历史数据读取正确性
     - 历史数据聚合正确性
     - 幂等性测试（重复补齐不报错）
     - 边界情况（无历史数据、部分历史数据）

#### 验收标准

- [ ] 启动时能正确扫描未完成的窗口
- [ ] 能正确读取历史1m数据
- [ ] 能正确聚合历史数据
- [ ] 补齐的数据正确写入QuestDB
- [ ] 不产生重复聚合（幂等性）
- [ ] 不发布事件（避免重复通知）
- [ ] 异步执行不阻塞启动
- [ ] 单元测试覆盖率 > 80%

#### 依赖关系
- 依赖：任务2.1（KlineAggregator）
- 依赖：任务3.2（StorageService）
- 依赖：`QuestDbMarketQueryService`（已有）

#### 输出物
- `KlineAggregatorInitializer.java`
- 窗口扫描逻辑
- 历史数据聚合逻辑
- 配置类
- 测试代码

---

## Phase 3: 持久化层（2天）

**阶段状态**：🔒 已锁定（等待Phase 2完成）  
**前置条件**：Phase 2 所有任务完成 + 同意开启  
**完成条件**：所有任务验收通过  
**开启下一阶段**：✅ 同意开启下一阶段（Phase 4）

---

### 任务3.1：创建QuestDB表结构

**负责人**：开发工程师  
**预计工期**：0.5天  
**优先级**：P0（阻塞）

#### 任务描述
创建5个周期的K线聚合表。

#### 具体步骤

1. **编写初始化SQL**
   - 文件：`src/main/resources/sql/kline_aggregation_init.sql`
   - 创建5个表：kline_5m, kline_15m, kline_30m, kline_1h, kline_4h
   - 表结构：
     - symbol SYMBOL
     - ts TIMESTAMP
     - open, high, low, close, volume DOUBLE
     - source_kline_count INT
     - 分区：PARTITION BY DAY

2. **执行SQL创建表**
   - 在QuestDB中执行SQL
   - 验证表结构

3. **验证表结构**
   - 检查字段类型
   - 检查分区设置
   - 检查索引

#### 验收标准

- [ ] 所有表创建成功
- [ ] 表结构符合设计规范
- [ ] 分区设置正确
- [ ] 可以正常写入数据

#### 依赖关系
- 无前置依赖

#### 输出物
- `kline_aggregation_init.sql`
- 表创建验证报告

---

### 任务3.2：实现AggregatedKLineStorageService

**负责人**：开发工程师  
**预计工期**：1天  
**优先级**：P0（核心）

#### 任务描述
实现聚合K线存储服务，支持幂等写入和批量写入。

#### 具体步骤

1. **创建接口和实现**
   - 接口：`persistence/AggregatedKLineStorageService.java`
   - 实现：`persistence/impl/QuestDbAggregatedKLineStorageServiceImpl.java`
   - 方法：
     - `save(AggregatedKLine aggregatedKLine)` - 单条写入（幂等）
     - `batchSave(List<AggregatedKLine> aggregatedKLines)` - 批量写入
     - `exists(String symbol, String period, long timestamp)` - 存在性检查

2. **实现QuestDB写入逻辑**
   - 使用QuestDB的HTTP API或JDBC
   - 根据period选择对应的表
   - 实现INSERT语句

3. **实现幂等性**
   - 写入前检查是否存在
   - 如果存在，跳过写入（返回false）
   - 如果不存在，执行写入（返回true）

4. **实现异步写入**
   - 使用线程池异步执行
   - 不阻塞聚合流程
   - 实现写入失败重试（最多3次）

5. **编写单元测试**
   - 文件：`persistence/impl/QuestDbAggregatedKLineStorageServiceImplTest.java`
   - 测试用例：
     - 单条写入成功
     - 幂等性测试（重复写入）
     - 批量写入成功
     - 写入失败重试
     - 异步写入测试

#### 验收标准

- [ ] 单条写入成功
- [ ] 幂等性保证（重复写入不报错）
- [ ] 批量写入成功
- [ ] 异步写入不阻塞
- [ ] 写入失败重试正常
- [ ] 单元测试覆盖率 > 80%

#### 依赖关系
- 依赖：任务3.1（表结构）
- 依赖：QuestDB连接配置

#### 输出物
- `AggregatedKLineStorageService.java`
- `QuestDbAggregatedKLineStorageServiceImpl.java`
- 测试代码

---

### 任务3.3：集成存储服务

**负责人**：开发工程师  
**预计工期**：0.5天  
**优先级**：P0（核心）

#### 任务描述
将存储服务集成到聚合流程中。

#### 具体步骤

1. **在聚合完成后调用存储服务**
   - 在 `KlineAggregator` 中注入 `AggregatedKLineStorageService`
   - 窗口结束时，生成聚合结果后调用存储服务
   - 异步执行，不阻塞聚合流程

2. **实现写入失败处理**
   - 记录失败日志
   - 实现重试机制
   - 不影响后续聚合

3. **实现写入监控**
   - 统计写入成功率
   - 统计写入延迟
   - 提供监控指标

4. **编写集成测试**
   - 端到端测试：1m事件 -> 聚合 -> 写入QuestDB
   - 验证数据正确性

#### 验收标准

- [ ] 聚合完成后自动写入QuestDB
- [ ] 写入失败不影响聚合
- [ ] 数据正确写入
- [ ] 监控指标正常
- [ ] 集成测试通过

#### 依赖关系
- 依赖：任务2.1（KlineAggregator）
- 依赖：任务3.2（StorageService）

#### 输出物
- 集成代码
- 集成测试代码
- 监控代码

---

## Phase 4: 集成和优化（2天）

**阶段状态**：🔒 已锁定（等待Phase 3完成）  
**前置条件**：Phase 3 所有任务完成 + 同意开启  
**完成条件**：所有任务验收通过  
**开启下一阶段**：✅ 同意开启下一阶段（Phase 5）

---

### 任务4.1：集成到MarketDataCenter

**负责人**：开发工程师  
**预计工期**：0.5天  
**优先级**：P0（核心）

#### 任务描述
将聚合模块集成到行情数据中心，实现自动启动和运行。

#### 具体步骤

1. **配置Spring Bean**
   - 在配置类中注册所有Bean
   - 配置依赖注入
   - 配置启动顺序

2. **启动时自动订阅事件**
   - 在 `@PostConstruct` 中订阅
   - 验证订阅成功

3. **启动时自动执行历史数据补齐**
   - 在初始化器中调用历史数据补齐
   - 验证补齐流程正常
   - 验证不阻塞启动

4. **验证端到端流程**
   - 发送1m K线事件
   - 验证聚合完成
   - 验证事件发布
   - 验证数据写入

5. **验证启动时历史数据补齐**
   - 模拟启动场景（在窗口中间启动）
   - 验证历史数据正确补齐
   - 验证不产生重复聚合

6. **编写集成测试**
   - 端到端测试
   - 验证数据流完整性
   - 验证历史数据补齐

#### 验收标准

- [ ] 模块自动启动
- [ ] 事件订阅成功
- [ ] 启动时历史数据补齐正常
- [ ] 端到端流程正常
- [ ] 数据正确聚合和存储

#### 依赖关系
- 依赖：Phase 2、Phase 3 所有任务

#### 输出物
- 配置代码
- 集成测试代码

---

### 任务4.2：性能优化

**负责人**：开发工程师  
**预计工期**：1天  
**优先级**：P1（重要）

#### 任务描述
优化聚合性能，满足性能指标要求。

#### 具体步骤

1. **优化并发控制**
   - 使用无锁数据结构（如 `ConcurrentHashMap`）
   - 使用CAS操作替代锁
   - 减少锁竞争

2. **优化内存使用**
   - 优化Bucket数据结构
   - 及时清理过期Bucket
   - 减少对象创建

3. **优化数据库写入**
   - 实现批量写入
   - 优化写入SQL
   - 使用连接池

4. **性能测试**
   - 单symbol吞吐量测试
   - 聚合延迟测试
   - 内存使用测试
   - 数据库写入延迟测试

#### 验收标准

- [ ] 单symbol吞吐量 > 1000 events/s
- [ ] 聚合延迟 P99 < 2ms
- [ ] 数据库写入延迟 < 50ms
- [ ] 内存占用 < 10MB（100个交易对）

#### 依赖关系
- 依赖：Phase 4.1（集成完成）

#### 输出物
- 优化代码
- 性能测试报告

---

### 任务4.3：异常处理和监控

**负责人**：开发工程师  
**预计工期**：0.5天  
**优先级**：P1（重要）

#### 任务描述
实现异常处理逻辑和监控指标收集。

#### 具体步骤

1. **实现异常处理逻辑**
   - 时间乱序：拒绝处理，记录日志
   - 重复数据：忽略，记录日志
   - 写入失败：重试，记录日志
   - 内存溢出：清理，告警

2. **实现监控指标收集**
   - 聚合延迟（P50, P95, P99）
   - 聚合成功率
   - 写入失败率
   - 内存使用量
   - 活跃Bucket数量

3. **实现告警规则**
   - 聚合延迟 > 10ms：告警
   - 写入失败率 > 1%：告警
   - 内存使用 > 10MB：告警

4. **编写测试**
   - 异常场景测试
   - 监控指标测试

#### 验收标准

- [ ] 所有异常场景处理正确
- [ ] 监控指标正常收集
- [ ] 告警规则生效
- [ ] 测试通过

#### 依赖关系
- 依赖：Phase 4.1（集成完成）

#### 输出物
- 异常处理代码
- 监控代码
- 测试代码

---

## Phase 5: 测试和文档（2天）

**阶段状态**：🔒 已锁定（等待Phase 4完成）  
**前置条件**：Phase 4 所有任务完成 + 同意开启  
**完成条件**：所有任务验收通过  
**项目完成**：✅ 所有阶段完成

---

### 任务5.1：完整测试

**负责人**：测试工程师 + 开发工程师  
**预计工期**：1天  
**优先级**：P0（必须）

#### 任务描述
完成所有测试，确保功能正确性和稳定性。

#### 具体步骤

1. **单元测试补充**
   - 检查所有类的测试覆盖率
   - 补充缺失的测试用例
   - 目标覆盖率 > 85%

2. **集成测试**
   - 端到端流程测试
   - 多周期并发测试
   - 异常场景测试

3. **性能测试**
   - 吞吐量测试
   - 延迟测试
   - 内存测试
   - 稳定性测试（长时间运行）

4. **异常场景测试**
   - 1m数据延迟
   - 时间乱序
   - 重复数据
   - 写入失败
   - 内存溢出

#### 验收标准

- [ ] 单元测试覆盖率 > 85%
- [ ] 集成测试全部通过
- [ ] 性能测试满足指标
- [ ] 异常场景测试通过
- [ ] 稳定性测试通过（24小时运行）

#### 依赖关系
- 依赖：Phase 4 所有任务

#### 输出物
- 测试报告
- 测试代码
- 性能测试报告

---

### 任务5.2：历史数据回放支持

**负责人**：开发工程师  
**预计工期**：0.5天  
**优先级**：P2（可选）

#### 任务描述
实现历史数据回放功能，支持批量聚合历史1m数据。

#### 具体步骤

1. **实现批量聚合接口**
   - 方法：`batchAggregate(List<KlineEvent> events)`
   - 支持按时间顺序批量处理
   - 支持跳过已存在的聚合结果

2. **实现回放工具**
   - 从QuestDB读取历史1m数据
   - 批量调用聚合接口
   - 支持指定时间范围

3. **测试回放功能**
   - 测试批量聚合
   - 测试回放工具
   - 验证数据正确性

#### 验收标准

- [ ] 批量聚合接口正常
- [ ] 回放工具正常
- [ ] 历史数据正确聚合
- [ ] 不产生重复数据

#### 依赖关系
- 依赖：Phase 4.1（集成完成）

#### 输出物
- 批量聚合接口
- 回放工具
- 测试代码

---

### 任务5.3：文档完善

**负责人**：开发工程师  
**预计工期**：0.5天  
**优先级**：P1（重要）

#### 任务描述
完善项目文档，包括架构文档、使用文档、运维文档。

#### 具体步骤

1. **更新架构文档**
   - 更新 `行情模块架构档案.md`
   - 添加聚合模块说明
   - 更新模块关系图

2. **编写使用文档**
   - API使用说明
   - 配置说明
   - 常见问题

3. **编写运维文档**
   - 部署说明
   - 监控指标说明
   - 故障排查指南

#### 验收标准

- [ ] 架构文档更新完成
- [ ] 使用文档完整
- [ ] 运维文档完整
- [ ] 文档审核通过

#### 依赖关系
- 依赖：Phase 5.1（测试完成）

#### 输出物
- 更新的架构文档
- 使用文档
- 运维文档

---

## 📈 任务依赖关系图

```
Phase 1: 基础架构搭建
├── 1.1 包结构和基础类 ──┐
├── 1.2 AggregationBucket ──┤
└── 1.3 PeriodCalculator ───┘
         │
         ▼
Phase 2: 核心聚合逻辑
├── 2.1 KlineAggregator ────┐
├── 2.2 事件订阅和发布 ──────┤
└── 2.3 内存管理和清理 ──────┘
         │
         ▼
Phase 3: 持久化层
├── 3.1 QuestDB表结构 ──────┐
├── 3.2 StorageService ─────┤
└── 3.3 集成存储服务 ─────────┘
         │
         ▼
Phase 4: 集成和优化
├── 4.1 集成到MarketDataCenter ──┐
├── 4.2 性能优化 ─────────────────┤
└── 4.3 异常处理和监控 ───────────┘
         │
         ▼
Phase 5: 测试和文档
├── 5.1 完整测试
├── 5.2 历史数据回放（可选）
└── 5.3 文档完善
```

---

## 🎯 里程碑

| 里程碑 | 完成条件 | 预计时间 |
|--------|---------|---------|
| M1: 基础架构完成 | Phase 1 所有任务完成 | 第2天 |
| M2: 核心功能完成 | Phase 2 所有任务完成 | 第5.5天 |
| M3: 持久化完成 | Phase 3 所有任务完成 | 第7天 |
| M4: 集成完成 | Phase 4 所有任务完成 | 第9天 |
| M5: 测试完成 | Phase 5 所有任务完成 | 第11天 |

---

## ⚠️ 风险提示

### 高风险项

1. **性能不达标**
   - 风险：聚合延迟 > 2ms
   - 应对：提前进行性能测试，及时优化

2. **数据一致性**
   - 风险：重复聚合或丢失聚合
   - 应对：加强测试，实现幂等性保证

3. **内存泄漏**
   - 风险：Bucket未及时清理
   - 应对：实现定时清理，监控内存使用

### 中风险项

1. **QuestDB写入性能**
   - 风险：写入延迟过高
   - 应对：使用异步写入，批量写入

2. **事件丢失**
   - 风险：事件总线异常导致事件丢失
   - 应对：实现事件持久化（可选）

---

## 📝 任务跟踪

### 任务状态说明

- ⏳ **待开始**：任务尚未开始
- 🔄 **进行中**：任务正在进行
- ✅ **已完成**：任务已完成
- ⚠️ **阻塞**：任务被阻塞
- ❌ **已取消**：任务已取消

### 阶段完成记录

| 阶段 | 完成日期 | 完成人 | 同意开启下一阶段 | 同意日期 | 同意人 |
|------|---------|--------|----------------|---------|--------|
| Phase 1 | 2025-01-15 | 开发团队 | ✅ 已同意 | 2025-01-15 | 项目经理 |
| Phase 2 | - | - | ⏳ 待同意 | - | - |
| Phase 3 | - | - | ⏳ 待同意 | - | - |
| Phase 4 | - | - | ⏳ 待同意 | - | - |
| Phase 5 | - | - | ✅ 项目完成 | - | - |

### 更新记录

| 日期 | 更新内容 | 更新人 |
|------|---------|--------|
| 2025-01-15 | 初始版本创建 | 开发团队 |
| 2025-01-15 | 添加阶段完成确认机制 | 开发团队 |
| 2025-01-15 | Phase 1 完成，同意开启 Phase 2 | 项目经理 |

---

**文档维护者**：开发团队  
**最后更新**：2025-01-15

