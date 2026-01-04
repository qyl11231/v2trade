# K线聚合模块 - SWAP合约处理说明

## 一、问题背景

在K线聚合模块中，需要处理以下情况：
1. **同一个symbol可能有多个marketType**：例如 BTC-USDT 既有 SPOT（现货）也有 SWAP（合约）
2. **聚合应该基于行情订阅配置表**：只聚合已订阅的交易对
3. **需要区分SPOT和SWAP**：在存储和查询时需要区分不同的市场类型

## 二、数据模型

### 2.1 交易对表（trading_pair）
- `symbol`: 标准化交易对，如 `BTC-USDT`（不包含marketType）
- `market_type`: 市场类型，如 `SPOT`、`SWAP`、`FUTURES`
- 唯一约束：`(symbol, market_type)`

### 2.2 交易所交易规则表（exchange_market_pair）
- `symbol_on_exchange`: 交易所内部标识，如 `BTC-USDT`（现货）或 `BTC-USDT-SWAP`（合约）
- `trading_pair_id`: 关联到 `trading_pair` 表

### 2.3 行情订阅配置表（market_subscription_config）
- `trading_pair_id`: 关联到 `trading_pair` 表
- `enabled`: 是否启用行情采集

## 三、解决方案

### 3.1 历史数据补齐

在 `KlineAggregatorInitializer` 中：
1. **获取交易对列表**：从 `MarketSubscriptionConfig` 获取启用的订阅配置
2. **生成唯一标识**：使用 `symbol-marketType` 格式（如 `BTC-USDT-SPOT`、`BTC-USDT-SWAP`）
3. **查询QuestDB**：在查询1m数据时，尝试使用标准化symbol（去掉marketType后缀）

```java
// 示例：symbol = "BTC-USDT-SWAP"
// 1. 提取标准化symbol: "BTC-USDT"
// 2. 查询QuestDB: 使用 "BTC-USDT" 或 "BTC-USDT-SWAP"（取决于QuestDB中存储的格式）
```

### 3.2 实时聚合

在 `KlineAggregatorImpl` 中：
1. **接收KlineEvent**：`KlineEvent` 的 `symbol` 字段是 `instId`（如 `BTC-USDT-SWAP`），可以直接区分SPOT和SWAP
2. **聚合处理**：使用 `symbol` 作为唯一标识进行聚合
3. **存储**：使用 `symbol` 存储到QuestDB（保持与1m数据一致）

### 3.3 存储格式

在QuestDB中：
- **1m K线表**：`symbol` 字段存储 `instId`（如 `BTC-USDT-SWAP`）
- **聚合K线表**：`symbol` 字段存储 `instId`（与1m数据保持一致）

## 四、代码修改

### 4.1 KlineAggregatorInitializer

**修改点1：getTargetSymbols方法**
- 从 `MarketSubscriptionConfig` 获取启用的订阅配置
- 通过 `tradingPairId` 查询 `TradingPair` 获取 `symbol` 和 `marketType`
- 生成 `symbol-marketType` 格式的唯一标识

**修改点2：backfillWindow方法**
- 解析 `symbol-marketType` 格式，提取标准化symbol
- 查询QuestDB时，先尝试使用标准化symbol，如果查询不到，再尝试使用原始symbol

### 4.2 实时聚合

**当前实现**：
- `KlineEvent` 的 `symbol` 字段是 `instId`（如 `BTC-USDT-SWAP`）
- 可以直接区分SPOT和SWAP
- 无需修改

## 五、注意事项

1. **QuestDB中的symbol格式**：
   - 如果QuestDB中存储的是标准化symbol（如 `BTC-USDT`），需要根据marketType区分
   - 如果QuestDB中存储的是交易所格式（如 `BTC-USDT-SWAP`），可以直接使用

2. **历史数据补齐**：
   - 历史数据补齐时，需要根据 `symbol-marketType` 格式查询对应的1m数据
   - 如果QuestDB中存储的是标准化symbol，可能需要额外的逻辑来区分SPOT和SWAP

3. **实时聚合**：
   - 实时聚合时，`KlineEvent` 的 `symbol` 已经是 `instId`，可以直接使用
   - 存储时保持与1m数据一致

## 六、后续优化建议

1. **统一symbol格式**：
   - 建议在QuestDB中统一使用 `instId` 格式（如 `BTC-USDT-SWAP`）
   - 这样可以避免在查询时需要区分SPOT和SWAP

2. **添加marketType字段**：
   - 在 `AggregatedKLine` 中添加 `marketType` 字段
   - 在存储时同时存储 `symbol` 和 `marketType`

3. **优化查询逻辑**：
   - 在查询QuestDB时，如果知道 `tradingPairId`，可以直接查询对应的 `symbolOnExchange`
   - 避免需要解析 `symbol-marketType` 格式

