下面是**最新冻结版：策略阶段2（Decision Engine）规划 v3.1**。我用“新人也能看懂”的方式写清楚：**业务逻辑、闭环、边界、状态机制、如何对接信号/价格/K线/指标**，并且保证后续可长期演进。

---

# 策略阶段2（Decision Engine）规划 v3.1（冻结版）

## 0. 一句话定位

阶段2做且只做一件事：

> **把“外部变化”（信号/指标/K线/价格）转成一次“明确的交易动作意图声明”，并把这次声明写入 `strategy_intent_record` 供下游执行。**

* ✅ 它像“大脑”：思考并声明“我想做什么”
* ❌ 它不下单、不查余额、不改真实持仓

---

## 1. 阶段2的输入/输出边界（新手最重要）

### 输入（全部只读）

阶段2读取以下信息，**不修改**它们：

1. **策略配置**：`strategy_definition`、`strategy_param`
2. **策略绑定的交易对**：`strategy_symbol`
3. **策略逻辑状态**：`strategy_logic_state`（逻辑持仓、phase）
4. **信号视图**：LATEST_ONLY（来自 signal/signal_intent）
5. **指标事实**：`indicator_value` latest（来自指标模块）
6. **K线闭合事件**：`BarClosedEvent`（来自聚合链路）
7. **价格快照**：最新价（来自行情内存快照）

### 输出（唯一副作用）

阶段2只做一次落库：

✅ `INSERT INTO strategy_intent_record (...)`

> **裁决（已同意）**：只有当决策是“动作意图”才落库：
> `OPEN/CLOSE/ADD/REDUCE/REVERSE`
> `HOLD` 和“数据未就绪/门禁拦截”**不落库**（只打指标/日志）。

---

## 2. 阶段2为什么必须存在（解决什么问题）

没有阶段2，系统会出现：

* 信号来了直接下单（不可审计、不可回放）
* 价格抖动导致重复开/平
* 策略重启后“失忆”
* 指标与信号耦合，越做越乱

阶段2的价值：

* **可回放**：每次“做决定”的原因都写入决策流水
* **可审计**：谁、何时、因为什么条件决定开/平仓
* **可扩展**：后续加更多策略类型、更多指标都不破坏架构

---

## 3. 闭环链路（从触发到可验证）

阶段2闭环是这样跑起来的：

1. **触发事件到达**（信号/指标/K线/价格）
2. 找到受影响的策略实例（StrategyInstance）
3. 为该实例做一次**原子快照**（DecisionContext，不可变）
4. 执行门禁（是否允许决策、是否重复触发、数据是否过期）
5. 调用策略逻辑（StrategyLogic）计算结果
6. 若结果是动作意图 → 写 `strategy_intent_record`
7. 下游阶段3（风控/执行）消费该记录去真正下单
8. 执行成功后阶段3更新 `strategy_logic_state`（阶段2不改）

> 你只要能在 MySQL 里看到 `strategy_intent_record`，就证明阶段2闭环已跑通。

---

## 4. 四类触发源与设计思想（信号/指标/K线/价格）

### 4.1 信号（Signal）

**定位**：外部事实（TradingView等），不是下单命令
**适合做**：入场/反向/提示类触发
**输入形态**：LATEST_ONLY（最新一条即可）

* 空仓：信号触发 `OPEN`
* 持仓：反向信号触发 `CLOSE/REVERSE`

> 关键：信号必须做**时效校验**（过期信号直接忽略）。

---

### 4.2 指标（Indicator）

**定位**：比原始行情更高级的决策依据（最适合长期演进）
**适合做**：趋势/动量/均值回归/过滤器
**输入形态**：从 `indicator_value` 取 latest（事实表，重启不丢）

* 空仓：RSI < 30 且满足过滤条件 → `OPEN`
* 持仓：RSI > 70 / 均线死叉 → `CLOSE/REDUCE`

> 指标来自事实表，是你们“回放/回测”的基石。

---

### 4.3 K线闭合（BarClosedEvent）

**定位**：稳定的“收盘确认”，避免 forming bar 噪音
**适合做**：形态确认、收盘触发的策略、收盘止损/止盈确认
**输入形态**：只用 bar_closed（你们系统已冻结）

* 空仓：收盘确认突破 → `OPEN`
* 持仓：收盘确认破位 → `CLOSE`

---

### 4.4 价格（Price）

**定位**：实时风控触发器（止盈止损/移动止损/突破价位）
**注意**：绝对不能 tick 直接触发决策，否则系统会爆

正确做法：**价格稀疏化**

* tick 只更新最新价快照
* 只有“穿越阈值”才生成 `PriceTriggeredDecisionEvent`

这让你既能：

* ✅ 实时止盈止损
* ✅ 不写爆决策表
* ✅ 长期运行稳定

---

## 5. 状态机制：空仓/持仓怎么分流（新人一看就懂）

阶段2决策必须先看：我现在是空仓还是持仓？

读取 `strategy_logic_state`：

* `logic_position_side = FLAT` → 空仓态
* `logic_position_side = LONG/SHORT && qty > 0` → 持仓态

### 5.1 空仓态（FLAT）

允许产生的动作意图：

* `OPEN`（开多/开空）
* `HOLD`（不落库）

输入怎么用：

* 信号/指标/K线/价格阈值（突破）都可触发入场
* 但必须过门禁（时效、重复、phase允许）

### 5.2 持仓态（LONG/SHORT）

允许产生的动作意图：

* `CLOSE`（平仓：止盈止损、反向、破位）
* `REDUCE`（减仓：部分止盈、风控）
* `ADD`（加仓：趋势增强、回撤加仓）
* `REVERSE`（反手：强反向）
* `HOLD`（不落库）

输入怎么用：

* 价格阈值：TP/SL/移动止损（最关键）
* 指标：趋势变坏/超买超卖/均线交叉
* K线：形态确认止损/止盈
* 信号：反向/平仓提示

---

## 6. “阶段2不改状态”的裁决（保证长期不出错）

为什么阶段2不更新 `strategy_logic_state`？
因为阶段2只是“我想这么做”，但：

* 下游风控可能拒绝
* 执行可能失败/部分成交/延迟

如果阶段2先改状态，会产生**状态漂移**：逻辑世界和真实世界不一致。

✅ 正确做法：

* 阶段2：写 `strategy_intent_record`（声明）
* 阶段3：执行成功/失败后再写回 `strategy_logic_state`（确认）

---

## 7. 核心组件拆分（工程实现不乱）

### 7.1 DecisionEventRouter（事件路由器）

订阅四类触发事件：

* SignalIntentActivatedEvent
* IndicatorComputedEvent
* BarClosedEvent
* PriceTriggeredDecisionEvent

职责：

* 找到受影响的 StrategyInstance
* 投递到 `StripedExecutor(instanceKey)`，保证同实例串行

### 7.2 AtomicSampler（原子采样器）

将当时能看到的世界“冻结”成不可变 `DecisionContext`：

* logic_state_snapshot
* param_snapshot
* signal_snapshot（可空）
* indicator_snapshot（来自 indicator_value latest）
* bar_snapshot（若为 bar 触发）
* price_snapshot（最新价）

### 7.3 GuardChain（门禁链）

必须包含：

* phase gate（OPEN_PENDING/EXIT_PENDING 等禁止重复）
* staleness gate（信号过期/指标未就绪）
* dedup gate（同触发不重复）
* sanity gate（例如 qty/side 合法性）

### 7.4 StrategyLogic（策略逻辑插件）

阶段2的“灵魂”：

* `DecisionResult decide(DecisionContext ctx)`（纯函数）
* 由 `StrategyLogicRegistry` 按 `strategy_type` 路由

MVP 先支持：

* SIGNAL_DRIVEN
* INDICATOR_DRIVEN
* HYBRID（ALL/ANY）

### 7.5 IntentRecorder（落库器）

* 仅当 action != HOLD 才 INSERT
* `decision_reason` 必须是结构化 JSON（用于回放）

---

## 8. 长期演进能力（你后面要做的都能接上）

这套设计天然支持后续扩展：

* 加新指标：只影响 StrategyLogic 的条件，不影响框架
* 加新策略类型：新增一个 StrategyLogic 实现即可
* 加风控：在阶段3做（或阶段2门禁中加入“静态风控”也可）
* 回测：把历史事件喂给 DecisionEngine，写出的 intent_record 可对比回放

---

## 9. 阶段2完成标志（可验收）

满足以下即阶段2完成：

1. ✅ 信号/指标/K线/价格阈值任一变化能触发策略评估
2. ✅ 只有真正动作意图才写入 `strategy_intent_record`
3. ✅ 每条 intent_record 的 reason JSON 可复盘触发源与关键快照（信号/指标/价格/Bar）
4. ✅ 同一触发重复到达不产生重复 intent_record（幂等成立）
5. ✅ 止盈止损可在 bar 未闭合时触发（价格阈值穿越）

