# 策略模块完整闭环设计方案

> **项目**：v2trade 量化交易系统  
> **版本**：v2.0  
> **创建时间**：2026-01-08  
> **设计目标**：基于已有架构，完成策略阶段2（决策阶段）的生产级闭环实现

---

## 📋 目录

1. [设计概述](#一设计概述)
2. [架构设计](#二架构设计)
3. [核心模块设计](#三核心模块设计)
4. [数据流闭环](#四数据流闭环)
5. [事件驱动机制](#五事件驱动机制)
6. [状态机设计](#六状态机设计)
7. [接口设计](#七接口设计)
8. [实现路线图](#八实现路线图)

---

## 一、设计概述

### 1.1 设计背景

v2trade 项目已完成**策略阶段1**（Strategy Bootstrap Phase），实现了策略实例化、配置加载、状态恢复等基础能力。现需要实现**策略阶段2**（Strategy Decision Phase），构建完整的决策闭环。

### 1.2 核心目标

**策略阶段2的唯一职责**：对某个策略实例，在某一时刻，回答"我现在想做什么？"

这个"想做什么"必须：
- **明确**：OPEN / CLOSE / ADD / REDUCE / REVERSE / HOLD
- **可回放**：所有决策记录可追溯
- **不依赖未来状态**：决策基于当前可见数据
- **不因重启而改变**：决策结果持久化

### 1.3 设计原则

| 原则 | 说明 |
|------|------|
| **纯决策层** | 不碰账户、不下单、不改状态，所有副作用交给阶段3 |
| **事件驱动** | 由信号、行情、指标事件触发，禁止定时扫描 |
| **只写一次** | 决策记录只写入一次，不回滚，不覆盖 |
| **幂等性** | 相同输入产生相同决策，支持重放 |
| **单一职责** | 每个模块职责清晰，禁止跨层调用 |

### 1.4 阶段边界

#### 阶段2允许的操作

| 操作类型 | 说明 |
|---------|------|
| ✅ 读取 StrategyInstance | 获取策略实例 |
| ✅ 读取 logic_state | 读取逻辑状态（只读） |
| ✅ 读取 signal_intent | 读取最新信号意图（只读） |
| ✅ 读取行情数据 | 读取最新行情（只读） |
| ✅ 读取指标数据 | 读取最新指标（只读） |
| ✅ 写入 strategy_intent_record | 记录决策意图（唯一副作用） |
| ✅ 计算下单数量 | 基于策略参数计算 |
| ✅ 判断交易方向 | 基于信号和条件判断 |

#### 阶段2禁止的操作

| 操作类型 | 原因 |
|---------|------|
| ❌ 修改 logic_state | 状态修改属于阶段3 |
| ❌ 消费信号 | 信号消费属于阶段3 |
| ❌ 修改 signal_intent 状态 | 信号状态管理属于阶段3 |
| ❌ 发送交易指令 | 交易执行属于阶段3 |
| ❌ 判断账户余额 | 资金裁决属于更下游 |
| ❌ 处理失败重试 | 决策是声明，不是命令 |
| ❌ 定时扫描 | 必须事件驱动 |

---

## 二、架构设计

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────────┐
│                        事件源层                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │ 信号事件  │  │ 行情事件  │  │ 指标事件  │  │ 定时事件  │   │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘   │
└───────┼─────────────┼─────────────┼─────────────┼──────────┘
        │             │             │             │
        └─────────────┴─────────────┴─────────────┘
                      │
        ┌─────────────▼─────────────┐
        │   DecisionTriggerRouter   │  ← 事件路由器
        │   （路由到对应策略实例）    │
        └─────────────┬─────────────┘
                      │
        ┌─────────────▼─────────────┐
        │      DecisionEngine       │  ← 决策引擎（入口）
        │   （协调整个决策流程）      │
        └─────────────┬─────────────┘
                      │
        ┌─────────────▼─────────────┐
        │ DecisionContextBuilder    │  ← 上下文构建器
        │   （构建决策上下文快照）    │
        └─────────────┬─────────────┘
                      │
        ┌─────────────▼─────────────┐
        │       GuardChain          │  ← 决策门禁链
        │   （判定是否允许决策）      │
        └─────────────┬─────────────┘
                      │
        ┌─────────────▼─────────────┐
        │     IntentResolver        │  ← 意图推导器
        │   （纯函数推导交易意图）    │
        └─────────────┬─────────────┘
                      │
        ┌─────────────▼─────────────┐
        │     IntentRecorder        │  ← 意图记录器
        │   （决策落库，唯一副作用）  │
        └─────────────┬─────────────┘
                      │
        ┌─────────────▼─────────────┐
        │  strategy_intent_record   │  ← 决策记录表
        └───────────────────────────┘
```

### 2.2 模块职责划分

| 模块 | 职责 | 输入 | 输出 | 禁止行为 |
|------|------|------|------|---------|
| **DecisionTriggerRouter** | 事件路由，将事件分发到对应策略实例 | 各类事件 | 触发决策 | 不包含业务逻辑 |
| **DecisionEngine** | 决策入口，协调整个决策流程 | StrategyInstance + Trigger | DecisionResult | 不直接操作数据库 |
| **DecisionContextBuilder** | 上下文构建器，构建决策上下文快照 | StrategyInstance + Trigger | DecisionContext | 不修改数据，只读 |
| **GuardChain** | 决策门禁链，判定是否允许决策 | DecisionContext | boolean | 不修改数据，只做校验 |
| **IntentResolver** | 意图推导器，纯函数推导意图 | DecisionContext | DecisionResult | 不访问数据库，纯计算 |
| **IntentRecorder** | 意图记录器，决策落库 | DecisionResult | void | 不修改数据，只写一次 |

### 2.3 包结构设计

```
com.qyl.v2trade.business.strategy
├── decision                    # 决策模块（阶段2核心）
│   ├── engine                  # 决策引擎
│   │   ├── DecisionEngine.java
│   │   └── DecisionTriggerRouter.java
│   ├── context                 # 上下文构建
│   │   ├── DecisionContextBuilder.java
│   │   └── model
│   │       ├── DecisionContext.java
│   │       ├── DecisionTrigger.java
│   │       └── TriggerType.java
│   ├── guard                   # 决策门禁
│   │   ├── GuardChain.java
│   │   ├── Guard.java
│   │   └── guards
│   │       ├── PhaseGuard.java
│   │       ├── SignalGuard.java
│   │       ├── DuplicateGuard.java
│   │       └── CooldownGuard.java
│   ├── resolver                # 意图推导
│   │   ├── IntentResolver.java
│   │   └── model
│   │       ├── DecisionResult.java
│   │       └── IntentActionEnum.java
│   └── recorder                # 意图记录
│       ├── IntentRecorder.java
│       └── model
│           └── StrategyIntentRecord.java
├── mapper                      # 数据访问层
│   └── StrategyIntentRecordMapper.java
└── service                     # 服务层
    └── StrategyIntentRecordService.java
```

---

## 三、核心模块设计

### 3.1 DecisionEngine（决策引擎）

#### 3.1.1 职责

- 作为决策流程的入口和协调者
- 协调各个子模块完成决策
- 保证决策的原子性和幂等性

#### 3.1.2 接口设计

```java
/**
 * 决策引擎
 * 
 * <p>职责：
 * <ul>
 *   <li>协调整个决策流程</li>
 *   <li>事件驱动触发决策</li>
 *   <li>保证决策的原子性和幂等性</li>
 * </ul>
 * 
 * <p>阶段2约束：
 * <ul>
 *   <li>不修改 logic_state</li>
 *   <li>不发送交易指令</li>
 *   <li>只写 strategy_intent_record</li>
 * </ul>
 */
public interface DecisionEngine {
    
    /**
     * 执行决策
     * 
     * @param instance 策略实例
     * @param trigger 触发事件
     * @return 决策结果，如果决策被拒绝返回null
     */
    DecisionResult execute(StrategyInstance instance, DecisionTrigger trigger);
    
    /**
     * 批量执行决策（用于多个实例同时触发）
     * 
     * @param instances 策略实例列表
     * @param trigger 触发事件
     * @return 决策结果列表
     */
    List<DecisionResult> executeBatch(List<StrategyInstance> instances, DecisionTrigger trigger);
}
```

#### 3.1.3 核心流程

```java
@Override
public DecisionResult execute(StrategyInstance instance, DecisionTrigger trigger) {
    // Step 1: 构建决策上下文（只读快照）
    DecisionContext context = contextBuilder.build(instance, trigger);
    
    // Step 2: 门禁校验（是否允许决策）
    if (!guardChain.isAllowed(context)) {
        log.debug("决策被门禁拒绝: strategyId={}, tradingPairId={}", 
            instance.getStrategyId(), instance.getTradingPairId());
        return null;
    }
    
    // Step 3: 意图推导（纯函数）
    DecisionResult result = intentResolver.resolve(context);
    
    // Step 4: 决策落库（唯一副作用）
    intentRecorder.record(result);
    
    // Step 5: 发布决策完成事件（供阶段3监听）
    eventPublisher.publishEvent(new DecisionCompletedEvent(result));
    
    return result;
}
```

### 3.2 DecisionContextBuilder（上下文构建器）

#### 3.2.1 职责

- 构建决策上下文快照
- 从各个数据源读取当前状态
- 保证上下文的完整性和一致性

#### 3.2.2 DecisionContext 模型

```java
/**
 * 决策上下文（只读快照）
 * 
 * <p>包含决策所需的全部信息，防止隐式依赖
 */
@Getter
@Builder
public class DecisionContext {
    
    // ========== 基础信息 ==========
    
    /**
     * 策略ID
     */
    private final Long strategyId;
    
    /**
     * 交易对ID
     */
    private final Long tradingPairId;
    
    /**
     * 交易对符号（如 BTC-USDT-SWAP）
     */
    private final String symbol;
    
    /**
     * 用户ID
     */
    private final Long userId;
    
    // ========== 触发信息 ==========
    
    /**
     * 触发类型（SIGNAL / MARKET / INDICATOR / TIMER）
     */
    private final TriggerType triggerType;
    
    /**
     * 触发时间
     */
    private final LocalDateTime triggerTime;
    
    // ========== 状态快照 ==========
    
    /**
     * 逻辑状态快照
     */
    private final StrategyLogicState logicState;
    
    /**
     * 策略参数快照
     */
    private final StrategyParam strategyParam;
    
    // ========== 信号快照 ==========
    
    /**
     * 最新信号意图（nullable）
     */
    private final SignalIntent latestSignalIntent;
    
    // ========== 行情快照 ==========
    
    /**
     * 最新市场价格
     */
    private final BigDecimal latestPrice;
    
    /**
     * 最新K线（nullable）
     */
    private final Kline latestKline;
    
    // ========== 指标快照 ==========
    
    /**
     * 指标值映射（indicator_code -> value）
     */
    private final Map<String, BigDecimal> indicators;
    
    // ========== 历史决策 ==========
    
    /**
     * 上一次决策记录（nullable）
     */
    private final StrategyIntentRecord lastIntentRecord;
}
```

#### 3.2.3 构建流程

```java
@Override
public DecisionContext build(StrategyInstance instance, DecisionTrigger trigger) {
    Long strategyId = instance.getStrategyId();
    Long tradingPairId = instance.getTradingPairId();
    
    return DecisionContext.builder()
        // 基础信息
        .strategyId(strategyId)
        .tradingPairId(tradingPairId)
        .symbol(getTradingPairSymbol(tradingPairId))
        .userId(instance.getLogicState().getUserId())
        
        // 触发信息
        .triggerType(trigger.getType())
        .triggerTime(LocalDateTime.now())
        
        // 状态快照
        .logicState(instance.getLogicState())
        .strategyParam(strategyParamService.getByStrategyId(strategyId))
        
        // 信号快照
        .latestSignalIntent(getLatestActiveSignal(strategyId, tradingPairId))
        
        // 行情快照
        .latestPrice(marketService.getLatestPrice(tradingPairId))
        .latestKline(klineService.getLatestKline(tradingPairId, "1m"))
        
        // 指标快照
        .indicators(indicatorService.getLatestIndicators(tradingPairId))
        
        // 历史决策
        .lastIntentRecord(intentRecordService.getLatest(strategyId, tradingPairId))
        
        .build();
}
```

### 3.3 GuardChain（决策门禁链）

#### 3.3.1 职责

- 判定是否允许进入决策
- 多个门禁按顺序执行
- 任一门禁失败则拒绝决策

#### 3.3.2 Guard 接口

```java
/**
 * 决策门禁接口
 */
public interface Guard {
    
    /**
     * 判断是否允许决策
     * 
     * @param context 决策上下文
     * @return true表示允许，false表示拒绝
     */
    boolean isAllowed(DecisionContext context);
    
    /**
     * 获取门禁名称
     * 
     * @return 门禁名称
     */
    String getName();
    
    /**
     * 获取拒绝原因
     * 
     * @return 拒绝原因
     */
    String getReason();
}
```

#### 3.3.3 内置门禁

| 门禁 | 职责 | 拒绝条件 |
|------|------|---------|
| **PhaseGuard** | 阶段校验 | logic_state.phase 不允许决策 |
| **SignalGuard** | 信号校验 | 无有效信号且触发类型为SIGNAL |
| **DuplicateGuard** | 重复校验 | 与上次决策相同且时间间隔过短 |
| **CooldownGuard** | 冷却校验 | 距离上次决策时间过短 |

#### 3.3.4 GuardChain 实现

```java
@Component
public class GuardChain {
    
    private final List<Guard> guards;
    
    public GuardChain(List<Guard> guards) {
        this.guards = guards;
    }
    
    /**
     * 判断是否允许决策
     * 
     * @param context 决策上下文
     * @return true表示允许，false表示拒绝
     */
    public boolean isAllowed(DecisionContext context) {
        for (Guard guard : guards) {
            if (!guard.isAllowed(context)) {
                log.debug("决策被门禁拒绝: guard={}, reason={}, strategyId={}, tradingPairId={}",
                    guard.getName(), guard.getReason(), 
                    context.getStrategyId(), context.getTradingPairId());
                return false;
            }
        }
        return true;
    }
}
```

### 3.4 IntentResolver（意图推导器）

#### 3.4.1 职责

- 基于决策上下文推导交易意图
- 纯函数，无副作用
- 不访问数据库，不修改状态

#### 3.4.2 IntentActionEnum 枚举

```java
/**
 * 意图动作枚举
 */
public enum IntentActionEnum {
    
    /**
     * 开仓
     */
    OPEN("OPEN", "开仓"),
    
    /**
     * 平仓
     */
    CLOSE("CLOSE", "平仓"),
    
    /**
     * 加仓
     */
    ADD("ADD", "加仓"),
    
    /**
     * 减仓
     */
    REDUCE("REDUCE", "减仓"),
    
    /**
     * 反向开仓
     */
    REVERSE("REVERSE", "反向开仓"),
    
    /**
     * 持有（不操作）
     */
    HOLD("HOLD", "持有");
    
    private final String code;
    private final String description;
    
    // ... getter and fromCode method
}
```

#### 3.4.3 DecisionResult 模型

```java
/**
 * 决策结果
 */
@Getter
@Builder
public class DecisionResult {
    
    /**
     * 策略ID
     */
    private final Long strategyId;
    
    /**
     * 交易对ID
     */
    private final Long tradingPairId;
    
    /**
     * 用户ID
     */
    private final Long userId;
    
    /**
     * 触发信号ID（nullable）
     */
    private final Long signalId;
    
    /**
     * 意图动作
     */
    private final IntentActionEnum intentAction;
    
    /**
     * 计算数量
     */
    private final BigDecimal calculatedQty;
    
    /**
     * 决策原因
     */
    private final String decisionReason;
    
    /**
     * 决策时间
     */
    private final LocalDateTime decisionTime;
    
    /**
     * 创建 HOLD 决策
     */
    public static DecisionResult hold(DecisionContext context, String reason) {
        return DecisionResult.builder()
            .strategyId(context.getStrategyId())
            .tradingPairId(context.getTradingPairId())
            .userId(context.getUserId())
            .intentAction(IntentActionEnum.HOLD)
            .calculatedQty(BigDecimal.ZERO)
            .decisionReason(reason)
            .decisionTime(LocalDateTime.now())
            .build();
    }
}
```

#### 3.4.4 推导逻辑

```java
@Override
public DecisionResult resolve(DecisionContext context) {
    // 获取当前状态
    LogicPhaseEnum currentPhase = context.getLogicState().getStatePhaseEnum();
    SignalIntent signal = context.getLatestSignalIntent();
    
    // 场景1：空仓 + 有信号 → 开仓
    if (currentPhase == LogicPhaseEnum.IDLE && signal != null) {
        return resolveOpenIntent(context, signal);
    }
    
    // 场景2：持仓 + 反向信号 → 平仓或反向
    if (currentPhase == LogicPhaseEnum.OPENED && signal != null) {
        if (isReverseSignal(context, signal)) {
            return resolveCloseOrReverseIntent(context, signal);
        }
    }
    
    // 场景3：持仓 + 止盈止损触发 → 平仓
    if (currentPhase == LogicPhaseEnum.OPENED) {
        if (shouldTakeProfit(context) || shouldStopLoss(context)) {
            return resolveCloseIntent(context, "止盈止损触发");
        }
    }
    
    // 场景4：其他情况 → 持有
    return DecisionResult.hold(context, "无需操作");
}

/**
 * 推导开仓意图
 */
private DecisionResult resolveOpenIntent(DecisionContext context, SignalIntent signal) {
    // 计算开仓数量
    BigDecimal qty = calculateOpenQty(context);
    
    return DecisionResult.builder()
        .strategyId(context.getStrategyId())
        .tradingPairId(context.getTradingPairId())
        .userId(context.getUserId())
        .signalId(signal.getId())
        .intentAction(IntentActionEnum.OPEN)
        .calculatedQty(qty)
        .decisionReason("信号触发开仓: " + signal.getIntentDirection())
        .decisionTime(LocalDateTime.now())
        .build();
}

/**
 * 计算开仓数量
 */
private BigDecimal calculateOpenQty(DecisionContext context) {
    StrategyParam param = context.getStrategyParam();
    BigDecimal price = context.getLatestPrice();
    
    // 基于初始资金和下单比例计算
    BigDecimal capital = param.getInitialCapital();
    BigDecimal ratio = param.getBaseOrderRatio();
    
    return capital.multiply(ratio).divide(price, 8, RoundingMode.DOWN);
}
```

### 3.5 IntentRecorder（意图记录器）

#### 3.5.1 职责

- 将决策结果持久化到数据库
- 保证原子性和幂等性
- 只写一次，不回滚，不覆盖

#### 3.5.2 实现

```java
@Service
@Slf4j
public class IntentRecorder {
    
    private final StrategyIntentRecordMapper intentRecordMapper;
    
    /**
     * 记录决策意图
     * 
     * @param result 决策结果
     */
    @Transactional
    public void record(DecisionResult result) {
        // 转换为实体
        StrategyIntentRecord record = convertToRecord(result);
        
        // 写入数据库（只写一次）
        intentRecordMapper.insert(record);
        
        log.info("决策记录已保存: strategyId={}, tradingPairId={}, action={}, qty={}",
            result.getStrategyId(), result.getTradingPairId(), 
            result.getIntentAction(), result.getCalculatedQty());
    }
    
    /**
     * 转换为实体
     */
    private StrategyIntentRecord convertToRecord(DecisionResult result) {
        StrategyIntentRecord record = new StrategyIntentRecord();
        record.setUserId(result.getUserId());
        record.setStrategyId(result.getStrategyId());
        record.setTradingPairId(result.getTradingPairId());
        record.setSignalId(result.getSignalId());
        record.setIntentAction(result.getIntentAction().getCode());
        record.setCalculatedQty(result.getCalculatedQty());
        record.setDecisionReason(result.getDecisionReason());
        record.setCreatedAt(result.getDecisionTime());
        return record;
    }
}
```

---

## 四、数据流闭环

### 4.1 完整数据流图

```
┌─────────────────────────────────────────────────────────────┐
│                        外部事件源                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐                  │
│  │TradingView│  │ OKX行情  │  │ 指标计算  │                  │
│  │  Webhook │  │ WebSocket│  │  完成事件 │                  │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘                  │
└───────┼─────────────┼─────────────┼─────────────────────────┘
        │             │             │
        ▼             ▼             ▼
┌─────────────────────────────────────────────────────────────┐
│                      数据持久化层                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ signal_intent│  │ kline (QuestDB)│  │indicator_value│   │
│  │   (MySQL)    │  │              │  │   (MySQL)     │     │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘     │
└─────────┼──────────────────┼──────────────────┼─────────────┘
          │                  │                  │
          └──────────────────┴──────────────────┘
                             │
          ┌──────────────────▼──────────────────┐
          │       DecisionTriggerRouter         │
          │    （根据订阅关系路由到策略实例）      │
          └──────────────────┬──────────────────┘
                             │
          ┌──────────────────▼──────────────────┐
          │         DecisionEngine              │
          │      （执行决策流程）                 │
          └──────────────────┬──────────────────┘
                             │
          ┌──────────────────▼──────────────────┐
          │  DecisionContextBuilder             │
          │  （读取各数据源，构建上下文快照）       │
          │                                     │
          │  读取：                              │
          │  - strategy_logic_state             │
          │  - strategy_param                   │
          │  - signal_intent (LATEST_ONLY)      │
          │  - kline (最新)                     │
          │  - indicator_value (最新)           │
          └──────────────────┬──────────────────┘
                             │
          ┌──────────────────▼──────────────────┐
          │         GuardChain                  │
          │      （门禁校验）                     │
          └──────────────────┬──────────────────┘
                             │
          ┌──────────────────▼──────────────────┐
          │       IntentResolver                │
          │    （纯函数推导意图）                  │
          └──────────────────┬──────────────────┘
                             │
          ┌──────────────────▼──────────────────┐
          │       IntentRecorder                │
          │    （决策落库）                       │
          └──────────────────┬──────────────────┘
                             │
          ┌──────────────────▼──────────────────┐
          │   strategy_intent_record (MySQL)    │
          │    （决策记录表）                     │
          └──────────────────┬──────────────────┘
                             │
          ┌──────────────────▼──────────────────┐
          │    DecisionCompletedEvent           │
          │    （发布决策完成事件）                │
          └──────────────────┬──────────────────┘
                             │
          ┌──────────────────▼──────────────────┐
          │         阶段3：执行阶段                │
          │    （状态修改、交易执行）               │
          └─────────────────────────────────────┘
```

### 4.2 闭环关键点

| 关键点 | 说明 | 保障措施 |
|--------|------|---------|
| **事件触发** | 外部事件驱动决策 | 事件监听器 + 路由器 |
| **数据快照** | 构建当前时刻的完整上下文 | DecisionContextBuilder 只读操作 |
| **门禁校验** | 防止无效决策 | GuardChain 多重校验 |
| **意图推导** | 纯函数计算 | IntentResolver 无副作用 |
| **决策落库** | 唯一副作用 | IntentRecorder 事务保证 |
| **事件发布** | 通知下游阶段 | Spring Event 异步发布 |

---

## 五、事件驱动机制

### 5.1 事件类型定义

```java
/**
 * 触发类型枚举
 */
public enum TriggerType {
    
    /**
     * 信号触发
     */
    SIGNAL("SIGNAL", "信号触发"),
    
    /**
     * 行情触发
     */
    MARKET("MARKET", "行情触发"),
    
    /**
     * 指标触发
     */
    INDICATOR("INDICATOR", "指标触发"),
    
    /**
     * 定时触发（阶段2禁止）
     */
    TIMER("TIMER", "定时触发");
    
    private final String code;
    private final String description;
    
    // ... getter and fromCode method
}
```

### 5.2 DecisionTrigger 模型

```java
/**
 * 决策触发器
 */
@Getter
@Builder
public class DecisionTrigger {
    
    /**
     * 触发类型
     */
    private final TriggerType type;
    
    /**
     * 触发时间
     */
    private final LocalDateTime triggerTime;
    
    /**
     * 触发源ID（如 signal_id, kline_id, indicator_id）
     */
    private final Long sourceId;
    
    /**
     * 触发数据（可选，用于携带额外信息）
     */
    private final Map<String, Object> data;
    
    /**
     * 创建信号触发器
     */
    public static DecisionTrigger signal(Long signalId) {
        return DecisionTrigger.builder()
            .type(TriggerType.SIGNAL)
            .triggerTime(LocalDateTime.now())
            .sourceId(signalId)
            .build();
    }
    
    /**
     * 创建行情触发器
     */
    public static DecisionTrigger market(Long klineId) {
        return DecisionTrigger.builder()
            .type(TriggerType.MARKET)
            .triggerTime(LocalDateTime.now())
            .sourceId(klineId)
            .build();
    }
    
    /**
     * 创建指标触发器
     */
    public static DecisionTrigger indicator(Long indicatorId) {
        return DecisionTrigger.builder()
            .type(TriggerType.INDICATOR)
            .triggerTime(LocalDateTime.now())
            .sourceId(indicatorId)
            .build();
    }
}
```

### 5.3 事件监听器

```java
/**
 * 信号事件监听器
 */
@Component
@Slf4j
public class SignalEventListener {
    
    private final DecisionTriggerRouter router;
    
    /**
     * 监听信号到达事件
     */
    @EventListener
    @Async
    public void onSignalReceived(SignalReceivedEvent event) {
        log.info("收到信号事件: signalId={}, tradingPairId={}", 
            event.getSignalId(), event.getTradingPairId());
        
        // 创建触发器
        DecisionTrigger trigger = DecisionTrigger.signal(event.getSignalId());
        
        // 路由到对应策略实例
        router.route(event.getTradingPairId(), trigger);
    }
}

/**
 * 行情事件监听器
 */
@Component
@Slf4j
public class MarketEventListener {
    
    private final DecisionTriggerRouter router;
    
    /**
     * 监听K线闭合事件
     */
    @EventListener
    @Async
    public void onBarClosed(BarClosedEvent event) {
        log.debug("收到K线闭合事件: tradingPairId={}, interval={}", 
            event.getTradingPairId(), event.getInterval());
        
        // 创建触发器
        DecisionTrigger trigger = DecisionTrigger.market(event.getKlineId());
        
        // 路由到对应策略实例
        router.route(event.getTradingPairId(), trigger);
    }
}

/**
 * 指标事件监听器
 */
@Component
@Slf4j
public class IndicatorEventListener {
    
    private final DecisionTriggerRouter router;
    
    /**
     * 监听指标计算完成事件
     */
    @EventListener
    @Async
    public void onIndicatorCalculated(IndicatorCalculatedEvent event) {
        log.debug("收到指标计算完成事件: indicatorId={}, tradingPairId={}", 
            event.getIndicatorId(), event.getTradingPairId());
        
        // 创建触发器
        DecisionTrigger trigger = DecisionTrigger.indicator(event.getIndicatorId());
        
        // 路由到对应策略实例
        router.route(event.getTradingPairId(), trigger);
    }
}
```

### 5.4 DecisionTriggerRouter（事件路由器）

```java
/**
 * 决策触发路由器
 * 
 * <p>职责：
 * <ul>
 *   <li>根据交易对ID查找订阅该交易对的所有策略实例</li>
 *   <li>将触发事件分发到对应的策略实例</li>
 *   <li>批量触发决策</li>
 * </ul>
 */
@Component
@Slf4j
public class DecisionTriggerRouter {
    
    private final StrategyRuntimeRegistry registry;
    private final DecisionEngine decisionEngine;
    
    /**
     * 路由触发事件到策略实例
     * 
     * @param tradingPairId 交易对ID
     * @param trigger 触发器
     */
    public void route(Long tradingPairId, DecisionTrigger trigger) {
        // 查找订阅该交易对的所有策略实例
        List<StrategyInstance> instances = registry.getInstancesByTradingPair(tradingPairId);
        
        if (instances.isEmpty()) {
            log.debug("没有策略实例订阅交易对: tradingPairId={}", tradingPairId);
            return;
        }
        
        log.info("路由触发事件到 {} 个策略实例: tradingPairId={}, triggerType={}", 
            instances.size(), tradingPairId, trigger.getType());
        
        // 批量执行决策
        List<DecisionResult> results = decisionEngine.executeBatch(instances, trigger);
        
        log.info("决策完成: tradingPairId={}, 成功={}, 拒绝={}", 
            tradingPairId, 
            results.stream().filter(r -> r != null).count(),
            results.stream().filter(r -> r == null).count());
    }
}
```

---

## 六、状态机设计

### 6.1 LogicPhase 状态机

```
┌─────────┐
│  IDLE   │  ← 初始状态（空仓）
└────┬────┘
     │ 决策：OPEN
     ▼
┌──────────────┐
│ OPEN_PENDING │  ← 已决策待执行（阶段2产出）
└──────┬───────┘
       │ 执行：开仓成功（阶段3）
       ▼
┌─────────┐
│ OPENED  │  ← 已开仓（有持仓）
└────┬────┘
     │ 决策：REDUCE
     ▼
┌───────────────┐
│ PARTIAL_EXIT  │  ← 部分减仓
└───────┬───────┘
        │ 决策：CLOSE
        ▼
┌──────────────┐
│ EXIT_PENDING │  ← 已决策待平仓（阶段2产出）
└──────┬───────┘
       │ 执行：平仓成功（阶段3）
       ▼
┌─────────┐
│ CLOSED  │  ← 已平仓
└────┬────┘
     │ 自动转换
     ▼
┌─────────┐
│  IDLE   │  ← 回到空仓状态
└─────────┘
```

### 6.2 状态转换规则

| 当前状态 | 允许的决策动作 | 目标状态（阶段3修改） |
|---------|---------------|---------------------|
| IDLE | OPEN | OPEN_PENDING |
| IDLE | HOLD | IDLE |
| OPEN_PENDING | HOLD | OPEN_PENDING |
| OPENED | CLOSE | EXIT_PENDING |
| OPENED | REDUCE | PARTIAL_EXIT |
| OPENED | ADD | OPENED |
| OPENED | REVERSE | EXIT_PENDING |
| OPENED | HOLD | OPENED |
| PARTIAL_EXIT | CLOSE | EXIT_PENDING |
| PARTIAL_EXIT | ADD | OPENED |
| PARTIAL_EXIT | HOLD | PARTIAL_EXIT |
| EXIT_PENDING | HOLD | EXIT_PENDING |
| CLOSED | HOLD | CLOSED |

### 6.3 阶段2与阶段3的状态职责划分

| 阶段 | 职责 | 操作 |
|------|------|------|
| **阶段2** | 决策层 | 只读 logic_state，产出 intent_record |
| **阶段3** | 执行层 | 读取 intent_record，修改 logic_state，发送交易指令 |

---

## 七、接口设计

### 7.1 REST API 设计

#### 7.1.1 查询决策记录

```
GET /api/strategy/intent-records

Query Parameters:
- strategyId: Long (必填)
- tradingPairId: Long (可选)
- startTime: LocalDateTime (可选)
- endTime: LocalDateTime (可选)
- page: Integer (默认1)
- size: Integer (默认20)

Response:
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 100,
    "records": [
      {
        "id": 1,
        "strategyId": 1,
        "tradingPairId": 1,
        "signalId": 10,
        "intentAction": "OPEN",
        "calculatedQty": "0.01",
        "decisionReason": "信号触发开仓: BUY",
        "createdAt": "2026-01-08T10:00:00"
      }
    ]
  }
}
```

#### 7.1.2 手动触发决策

```
POST /api/strategy/trigger-decision

Request Body:
{
  "strategyId": 1,
  "tradingPairId": 1,
  "triggerType": "MANUAL"
}

Response:
{
  "code": 200,
  "message": "决策触发成功",
  "data": {
    "intentAction": "OPEN",
    "calculatedQty": "0.01",
    "decisionReason": "手动触发决策"
  }
}
```

### 7.2 事件接口

#### 7.2.1 DecisionCompletedEvent（决策完成事件）

```java
/**
 * 决策完成事件
 * 
 * <p>用于通知阶段3执行层
 */
@Getter
public class DecisionCompletedEvent extends ApplicationEvent {
    
    /**
     * 决策结果
     */
    private final DecisionResult result;
    
    public DecisionCompletedEvent(Object source, DecisionResult result) {
        super(source);
        this.result = result;
    }
}
```

---

## 八、实现路线图

### 8.1 开发阶段划分

#### 阶段 2.1：基础设施（1-2天）

**任务列表**：
1. 创建 `strategy_intent_record` 表的 Mapper 和 Entity
2. 创建枚举类型：`IntentActionEnum`、`TriggerType`
3. 创建核心模型：`DecisionContext`、`DecisionTrigger`、`DecisionResult`
4. 搭建包结构

**验收标准**：
- 数据库表和实体类映射正确
- 枚举类型定义完整
- 核心模型编译通过

#### 阶段 2.2：核心模块实现（3-4天）

**任务列表**：
1. 实现 `DecisionContextBuilder`
2. 实现 `GuardChain` 和内置门禁
3. 实现 `IntentResolver`（基础推导逻辑）
4. 实现 `IntentRecorder`
5. 实现 `DecisionEngine`

**验收标准**：
- 所有模块单元测试通过
- 决策流程可正常执行
- 决策记录可正常落库

#### 阶段 2.3：事件驱动集成（2-3天）

**任务列表**：
1. 实现 `DecisionTriggerRouter`
2. 实现事件监听器（Signal、Market、Indicator）
3. 集成到现有事件系统
4. 实现决策完成事件发布

**验收标准**：
- 信号到达可触发决策
- K线闭合可触发决策
- 指标计算完成可触发决策
- 事件路由正确

#### 阶段 2.4：高级特性（2-3天）

**任务列表**：
1. 实现止盈止损逻辑
2. 实现反向开仓逻辑
3. 实现加仓减仓逻辑
4. 优化推导算法

**验收标准**：
- 止盈止损触发正确
- 反向信号处理正确
- 加仓减仓计算正确

#### 阶段 2.5：测试与优化（2-3天）

**任务列表**：
1. 编写集成测试
2. 编写压力测试
3. 性能优化
4. 文档完善

**验收标准**：
- 集成测试覆盖率 ≥ 80%
- 性能满足要求（1000 TPS）
- 文档完整

### 8.2 总体时间估算

| 阶段 | 工作量 | 依赖 |
|------|--------|------|
| 2.1 基础设施 | 1-2天 | 无 |
| 2.2 核心模块 | 3-4天 | 2.1 |
| 2.3 事件驱动 | 2-3天 | 2.2 |
| 2.4 高级特性 | 2-3天 | 2.3 |
| 2.5 测试优化 | 2-3天 | 2.4 |
| **总计** | **10-15天** | - |

### 8.3 里程碑

| 里程碑 | 标志 | 交付物 |
|--------|------|--------|
| **M1** | 基础设施完成 | 数据表、实体类、枚举、模型 |
| **M2** | 核心流程打通 | 决策引擎可执行完整流程 |
| **M3** | 事件驱动集成 | 外部事件可触发决策 |
| **M4** | 功能完整 | 支持所有决策场景 |
| **M5** | 生产就绪 | 测试通过，文档完整 |

---

## 九、总结

### 9.1 设计亮点

1. **职责清晰**：阶段2只负责决策，不修改状态，不发送指令
2. **事件驱动**：由外部事件触发，避免定时扫描的问题
3. **纯函数设计**：IntentResolver 无副作用，易测试，易回放
4. **完整闭环**：从事件触发到决策落库，形成完整数据流
5. **可扩展性**：支持多种触发源、多种决策策略、多种门禁规则

### 9.2 生产级保障

1. **幂等性**：相同输入产生相同决策
2. **原子性**：决策记录事务保证
3. **可追溯**：所有决策记录永久保存
4. **可回放**：基于历史数据可重现决策过程
5. **容错性**：门禁机制防止异常决策

### 9.3 后续演进方向

1. **阶段3**：执行阶段，状态修改、交易执行
2. **多信号融合**：支持多个信号源的综合决策
3. **机器学习**：引入 ML 模型优化决策算法
4. **回测系统**：基于决策记录进行策略回测
5. **监控告警**：决策异常监控和告警

---

**文档作者**：Manus AI  
**最后更新**：2026-01-08  
**版本**：v2.0
