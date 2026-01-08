# 指标模块 Repository 层实现说明

## 已完成内容

### 1. SQL 迁移脚本（4个）

位置：`src/main/resources/sql/indicator/`

- `001_create_indicator_definition.sql` - 指标定义表
- `002_create_indicator_subscription.sql` - 指标订阅表
- `003_create_indicator_value.sql` - 指标值表
- `004_create_indicator_calc_log.sql` - 指标计算日志表

### 2. Entity 类（4个）

位置：`src/main/java/com/qyl/v2trade/indicator/repository/entity/`

- `IndicatorDefinition` - 指标定义实体
- `IndicatorSubscription` - 指标订阅实体
- `IndicatorValue` - 指标值实体
- `IndicatorCalcLog` - 指标计算日志实体

### 3. Mapper 接口（4个）

位置：`src/main/java/com/qyl/v2trade/indicator/repository/mapper/`

- `IndicatorDefinitionMapper`
- `IndicatorSubscriptionMapper`
- `IndicatorValueMapper`
- `IndicatorCalcLogMapper`

### 4. Repository 接口和实现（4个）

位置：`src/main/java/com/qyl/v2trade/indicator/repository/`

#### 接口：
- `IndicatorDefinitionRepository` - 支持 upsert（系统内置 user_id=0，重复忽略）
- `IndicatorSubscriptionRepository` - 支持 listEnabledByPairTf
- `IndicatorValueRepository` - 幂等 insert（唯一键冲突 ignore，冲突值不同写告警日志）
- `IndicatorCalcLogRepository` - 追加写入（不更新）

#### 实现：
- `IndicatorDefinitionRepositoryImpl`
- `IndicatorSubscriptionRepositoryImpl`
- `IndicatorValueRepositoryImpl`
- `IndicatorCalcLogRepositoryImpl`

### 5. 集成测试

位置：`src/test/java/com/qyl/v2trade/indicator/repository/IndicatorRepositoryIntegrationTest.java`

测试内容：
- 模拟一根 bar_close_time 的 RSI_14 计算结果写入 value + log
- 重复写入同 key 不产生重复记录（幂等验证）
- 冲突场景验证（相同key不同值不覆盖）

## 使用说明

### 1. 执行 SQL 迁移脚本

手动执行或通过应用启动时执行：

```bash
# 连接到MySQL数据库
mysql -u root -p v2_trade < src/main/resources/sql/indicator/001_create_indicator_definition.sql
mysql -u root -p v2_trade < src/main/resources/sql/indicator/002_create_indicator_subscription.sql
mysql -u root -p v2_trade < src/main/resources/sql/indicator/003_create_indicator_value.sql
mysql -u root -p v2_trade < src/main/resources/sql/indicator/004_create_indicator_calc_log.sql
```

### 2. 运行集成测试

```bash
mvn test -Dtest=IndicatorRepositoryIntegrationTest
```

## Repository 使用示例

### 写入指标值（幂等）

```java
@Autowired
private IndicatorValueRepository valueRepository;

IndicatorValue value = new IndicatorValue();
value.setUserId(1L);
value.setTradingPairId(100L);
value.setSymbol("BTC-USDT-SWAP");
value.setTimeframe("5m");
value.setBarTime(LocalDateTime.now());
value.setIndicatorCode("RSI_14");
value.setIndicatorVersion("v1");
value.setValue(new BigDecimal("72.5"));
value.setDataQuality("OK");
value.setCalcEngine("ta4j");
value.setCalcFingerprint("hash...");
value.setCalcCostMs(15);

IndicatorValueRepository.InsertResult result = valueRepository.insertIdempotent(value);
// INSERTED - 新插入
// IGNORED - 已存在且值相同（幂等）
// CONFLICT - 已存在但值不同（告警日志已记录，不覆盖）
```

### 写入计算日志

```java
@Autowired
private IndicatorCalcLogRepository calcLogRepository;

IndicatorCalcLog log = new IndicatorCalcLog();
log.setUserId(1L);
log.setTradingPairId(100L);
log.setSymbol("BTC-USDT-SWAP");
log.setTimeframe("5m");
log.setBarTime(LocalDateTime.now());
log.setIndicatorCode("RSI_14");
log.setIndicatorVersion("v1");
log.setCalcEngine("ta4j");
log.setStatus("SUCCESS");
log.setCostMs(15);

boolean success = calcLogRepository.append(log);
```

### 查询指标订阅

```java
@Autowired
private IndicatorSubscriptionRepository subscriptionRepository;

List<IndicatorSubscription> subscriptions = 
    subscriptionRepository.listEnabledByPairTf(userId, tradingPairId, "5m");
```

### Upsert 指标定义

```java
@Autowired
private IndicatorDefinitionRepository definitionRepository;

IndicatorDefinition definition = new IndicatorDefinition();
definition.setUserId(0L); // 系统内置
definition.setIndicatorCode("RSI");
definition.setIndicatorName("相对强弱指标");
definition.setIndicatorVersion("v1");
// ... 设置其他字段

boolean inserted = definitionRepository.upsert(definition);
// true - 新插入
// false - 已存在（忽略）
```

## 验收标准

✅ 数据库中能看到四张表：
- `indicator_definition`
- `indicator_subscription`
- `indicator_value`
- `indicator_calc_log`

✅ 至少一条 value 与 log（通过集成测试验证）

✅ 幂等生效（重复写入同key不产生重复记录）

✅ 冲突处理（相同key不同值不覆盖，记录告警日志）

