下面给你一份**“策略大脑 runtimeRules 完整 JSON 配置规范 + 3 个可直接落库的示例（趋势/网格/马丁）”**，满足你要的场景：

* ✅ 三种大脑：**趋势 / 网格 / 马丁**
* ✅ **多因子**
* ✅ **全满足（all）**
* ✅ **可进场（Entry）/可退场（Exit）**
* ✅ **开发人员一看就懂**（我会先给字段口径与执行语义，再给 3 份完整 JSON）

---

## Step 1：先冻结一份“开发可读”的 JSON 总结构

> **runtimeRules（建议存到 strategy_instance.runtimeRules）**

### 1.1 顶层字段

* `rulesType`：大脑类型（1=趋势，2=马丁，3=网格）
* `version`：配置版本（便于灰度/兼容）
* `entryRules`：进场规则（多因子）
* `exitRules`：退场规则（多因子）
* `brainParams`：该大脑专属参数（趋势/网格/马丁各自不同）
* `notes`：给开发/量化的说明（运行时可忽略）

### 1.2 Entry/Exit 规则字段（全满足 all 的关键）

* `entryRules.entryType = "all"`：表示**所有启用因子都满足**才允许进场
* `exitRules.exitType = "all"`：表示**所有启用因子都满足**才允许退场
* `factorList[]`：因子列表（每个因子都是一个“指标实例 + 判定方式”）

### 1.3 因子字段（必须让后端能解释执行）

每个因子最关键的是：

* `indicator`：怎么计算（指标 code + params）
* `condition`：怎么算“满足”（这是 all 模式的核心）
* `scope`：方向性（long/short 是否启用；权重可留着但 all 不依赖）

> 注意：你之前“权重打分”也很强，但你这次明确要“全满足”，所以**阈值 threshold 可以不出现**，改为每个因子必须有 `condition`。

---

## Step 2：统一 Condition DSL（让开发能实现）

为了可读、好实现，我建议 condition 用**结构化 JSON**，避免前端自由字符串导致解析困难。

支持这些操作符（足够覆盖趋势/网格/马丁的入退场）：

* 比较：`GT / GTE / LT / LTE / EQ`
* 交叉：`CROSS_UP / CROSS_DOWN`
* 区间：`BETWEEN`
* 变化率：`CHANGE_PCT_GTE`（例如突破幅度 ≥ x%）
* 时间有效性：`WITHIN_SEC`（信号有效期等）

字段引用：

* `ref: "price.last"`（最新价）
* `ref: "bar.close"` / `bar.high` 等
* `ref: "ind.rsi.value"`（指标输出）
* `ref: "signal.direction"` / `signal.price` 等

---

## Step 3：三份“完整可运行”的 JSON（趋势/网格/马丁）

> 说明：以下示例都采用 **多因子 + all + Entry/Exit**。
> 你可以直接把某一份写进实例的 runtimeRules。

---

### A) 趋势大脑（rulesType=1）—— 多因子全满足进场 + 全满足退场

```json
{
  "version": 1,
  "rulesType": 1,
  "notes": "趋势策略：多因子全满足才进场；多因子全满足才退场。适用于顺势突破/回撤止盈止损的前置决策层。",
  "brainParams": {
    "when": "BAR_CLOSE",
    "timeframe": "5m",
    "directionMode": "BOTH",
    "cooldownSecAfterExit": 120
  },
  "entryRules": {
    "entryType": "all",
    "factorList": [
      {
        "factorId": "entry_f1_ma_cross",
        "enabled": true,
        "name": "均线金叉确认（快线上穿慢线）",
        "scope": { "enableLong": true, "enableShort": false, "longWeight": 0, "shortWeight": 0 },
        "indicator": {
          "code": "ma",
          "dataSource": "BAR",
          "params": { "timeframe": "5m", "fastPeriod": 20, "slowPeriod": 60, "source": "CLOSE" }
        },
        "condition": {
          "op": "CROSS_UP",
          "left": { "ref": "ind.ma.fast" },
          "right": { "ref": "ind.ma.slow" }
        }
      },
      {
        "factorId": "entry_f2_rsi_not_overbought",
        "enabled": true,
        "name": "RSI 非超买（避免追高）",
        "scope": { "enableLong": true, "enableShort": false, "longWeight": 0, "shortWeight": 0 },
        "indicator": {
          "code": "rsi",
          "dataSource": "BAR",
          "params": { "timeframe": "5m", "length": 14 }
        },
        "condition": {
          "op": "LT",
          "left": { "ref": "ind.rsi.value" },
          "right": { "const": 70 }
        }
      },
      {
        "factorId": "entry_f3_breakout_confirm",
        "enabled": true,
        "name": "价格突破确认（突破近 N 根最高）",
        "scope": { "enableLong": true, "enableShort": false, "longWeight": 0, "shortWeight": 0 },
        "indicator": {
          "code": "price_breakout",
          "dataSource": "MIXED",
          "params": { "timeframe": "5m", "lookback": 60, "source": "HIGH_LOW", "epsilonRatio": 0.0005 }
        },
        "condition": {
          "op": "EQ",
          "left": { "ref": "ind.price_breakout.isBreakoutUp" },
          "right": { "const": true }
        }
      }
    ]
  },
  "exitRules": {
    "exitType": "all",
    "factorList": [
      {
        "factorId": "exit_f1_ma_cross_down",
        "enabled": true,
        "name": "均线死叉（趋势结束）",
        "scope": { "enableLong": true, "enableShort": false, "longWeight": 0, "shortWeight": 0 },
        "indicator": {
          "code": "ma",
          "dataSource": "BAR",
          "params": { "timeframe": "5m", "fastPeriod": 20, "slowPeriod": 60, "source": "CLOSE" }
        },
        "condition": {
          "op": "CROSS_DOWN",
          "left": { "ref": "ind.ma.fast" },
          "right": { "ref": "ind.ma.slow" }
        }
      },
      {
        "factorId": "exit_f2_rsi_cooldown",
        "enabled": true,
        "name": "RSI 进入高位（趋势衰竭信号）",
        "scope": { "enableLong": true, "enableShort": false, "longWeight": 0, "shortWeight": 0 },
        "indicator": {
          "code": "rsi",
          "dataSource": "BAR",
          "params": { "timeframe": "5m", "length": 14 }
        },
        "condition": {
          "op": "GTE",
          "left": { "ref": "ind.rsi.value" },
          "right": { "const": 75 }
        }
      }
    ]
  }
}
```

---

### B) 网格大脑（rulesType=3）—— 多因子全满足允许“开网格” + 多因子全满足“关网格”

> 网格核心是 **在某个区间内挂网格**，因此 Entry 不是“开仓”，而是“允许启动网格逻辑”；Exit 是“停止网格并清仓/收敛”。

```json
{
  "version": 1,
  "rulesType": 3,
  "notes": "网格策略：Entry 满足则允许启动网格；Exit 满足则停止网格并触发收敛。网格的下单/挂单执行由后续阶段实现。",
  "brainParams": {
    "when": "PRICE",
    "directionMode": "NEUTRAL",
    "grid": {
      "mode": "ARITHMETIC",
      "upperPriceRef": "ind.grid_range.upper",
      "lowerPriceRef": "ind.grid_range.lower",
      "gridCount": 20,
      "baseOrderQty": 0.001,
      "maxActiveOrders": 10,
      "rebalanceOnBreak": true
    }
  },
  "entryRules": {
    "entryType": "all",
    "factorList": [
      {
        "factorId": "entry_f1_range_detect",
        "enabled": true,
        "name": "识别震荡区间（高低波动在阈值内）",
        "scope": { "enableLong": true, "enableShort": true, "longWeight": 0, "shortWeight": 0 },
        "indicator": {
          "code": "grid_range",
          "dataSource": "BAR",
          "params": { "timeframe": "15m", "lookback": 96, "maxRangePct": 0.06 }
        },
        "condition": {
          "op": "EQ",
          "left": { "ref": "ind.grid_range.isRange" },
          "right": { "const": true }
        }
      },
      {
        "factorId": "entry_f2_atr_ok",
        "enabled": true,
        "name": "ATR 波动率适中（避免极端波动开网格）",
        "scope": { "enableLong": true, "enableShort": true, "longWeight": 0, "shortWeight": 0 },
        "indicator": {
          "code": "atr",
          "dataSource": "BAR",
          "params": { "timeframe": "15m", "length": 14 }
        },
        "condition": {
          "op": "BETWEEN",
          "value": { "ref": "ind.atr.valuePct" },
          "lower": { "const": 0.002 },
          "upper": { "const": 0.02 }
        }
      }
    ]
  },
  "exitRules": {
    "exitType": "all",
    "factorList": [
      {
        "factorId": "exit_f1_break_range",
        "enabled": true,
        "name": "价格突破区间（突破上轨或下轨）",
        "scope": { "enableLong": true, "enableShort": true, "longWeight": 0, "shortWeight": 0 },
        "indicator": {
          "code": "grid_range",
          "dataSource": "BAR",
          "params": { "timeframe": "15m", "lookback": 96, "maxRangePct": 0.06 }
        },
        "condition": {
          "op": "EQ",
          "left": { "ref": "ind.grid_range.isBreak" },
          "right": { "const": true }
        }
      },
      {
        "factorId": "exit_f2_vol_spike",
        "enabled": true,
        "name": "波动率爆发（网格风险上升，退出）",
        "scope": { "enableLong": true, "enableShort": true, "longWeight": 0, "shortWeight": 0 },
        "indicator": {
          "code": "atr",
          "dataSource": "BAR",
          "params": { "timeframe": "15m", "length": 14 }
        },
        "condition": {
          "op": "GTE",
          "left": { "ref": "ind.atr.valuePct" },
          "right": { "const": 0.03 }
        }
      }
    ]
  }
}
```

> 备注：这里出现了 `grid_range / atr` 两个指标 code。
> 如果你们当前指标库没这两个，就按同一结构把它们加入指标定义表 + 代码实现即可。

---

### C) 马丁大脑（rulesType=2）—— 多因子全满足允许马丁启动 + 多因子全满足停止加仓并退出

> 马丁核心在于：价格逆向触发加仓阶梯、风险上限、退出条件。
> N4 这里只负责“决策意图”，执行在后续阶段实现。

```json
{
  "version": 1,
  "rulesType": 2,
  "notes": "马丁策略：Entry 满足则允许启动马丁；Exit 满足则停止加仓并退出/收敛。加仓阶梯与风控上限由 brainParams.martingale 定义。",
  "brainParams": {
    "when": "PRICE",
    "directionMode": "LONG_ONLY",
    "martingale": {
      "baseOrderQty": 0.001,
      "maxSteps": 6,
      "stepMultiplier": 1.6,
      "stepTriggerDrawdownPct": 0.012,
      "maxTotalPositionQty": 0.05,
      "maxLossPctHardStop": 0.08,
      "takeProfitPctAfterAvg": 0.006,
      "cooldownSecAfterExit": 300
    }
  },
  "entryRules": {
    "entryType": "all",
    "factorList": [
      {
        "factorId": "entry_f1_signal_long",
        "enabled": true,
        "name": "外部信号为做多（启动马丁的方向锚）",
        "scope": { "enableLong": true, "enableShort": false, "longWeight": 0, "shortWeight": 0 },
        "indicator": {
          "code": "signal_confirm",
          "dataSource": "MIXED",
          "params": { "validityPeriodSec": 120, "triggerMode": "BOTH", "retraceRatio": 0.02, "breakoutRatio": 0.02, "priceRef": "SIGNAL_PRICE" }
        },
        "condition": {
          "op": "EQ",
          "left": { "ref": "signal.direction" },
          "right": { "const": "LONG" }
        }
      },
      {
        "factorId": "entry_f2_trend_filter",
        "enabled": true,
        "name": "趋势过滤（避免在大空头里做多马丁）",
        "scope": { "enableLong": true, "enableShort": false, "longWeight": 0, "shortWeight": 0 },
        "indicator": {
          "code": "ma",
          "dataSource": "BAR",
          "params": { "timeframe": "1h", "fastPeriod": 20, "slowPeriod": 60, "source": "CLOSE" }
        },
        "condition": {
          "op": "GT",
          "left": { "ref": "ind.ma.fast" },
          "right": { "ref": "ind.ma.slow" }
        }
      }
    ]
  },
  "exitRules": {
    "exitType": "all",
    "factorList": [
      {
        "factorId": "exit_f1_tp_after_avg",
        "enabled": true,
        "name": "均价止盈触发（达到均价上方一定比例）",
        "scope": { "enableLong": true, "enableShort": false, "longWeight": 0, "shortWeight": 0 },
        "indicator": {
          "code": "position",
          "dataSource": "STATE",
          "params": { "field": "avgEntryPrice" }
        },
        "condition": {
          "op": "CHANGE_PCT_GTE",
          "base": { "ref": "state.avgEntryPrice" },
          "value": { "ref": "price.last" },
          "pct": { "ref": "brain.martingale.takeProfitPctAfterAvg" }
        }
      },
      {
        "factorId": "exit_f2_hard_stop",
        "enabled": true,
        "name": "硬止损（触发最大亏损百分比）",
        "scope": { "enableLong": true, "enableShort": false, "longWeight": 0, "shortWeight": 0 },
        "indicator": {
          "code": "risk",
          "dataSource": "STATE",
          "params": { "field": "unrealizedLossPct" }
        },
        "condition": {
          "op": "GTE",
          "left": { "ref": "state.unrealizedLossPct" },
          "right": { "ref": "brain.martingale.maxLossPctHardStop" }
        }
      }
    ]
  }
}
```

> 这里的 `position / risk` 指标是“运行态指标”（从 state/仓位计算）。
> 如果你们 N3 里已有 `strategy_logic_state`，这类指标就是把 state 映射成指标输出即可。

---

## Step 4：给开发的“最低实现指南”（保证能落地）

开发最小要做 4 件事就能跑起来：

1. **IndicatorEngine**

    * 根据 `indicator.code` 找到实现（注册表/工厂）
    * 用 `params` 计算输出，写入 `ind.xxx.yyy`

2. **ConditionEvaluator**

    * 读 `condition.op`
    * 解析 `ref/const`
    * 计算 true/false

3. **RuleSetEvaluator（Entry/Exit）**

    * `all`：所有 enabled factor 的 condition 都 true → pass
    * （未来 weight 模式再引入 scope.weight/threshold）

4. **输出决策意图**

    * entry pass：趋势=OPEN / 网格=START_GRID / 马丁=START_MARTINGALE
    * exit pass：趋势=CLOSE / 网格=STOP_GRID / 马丁=STOP_AND_EXIT

---

## 总结

* 我给了你一份**统一、可解释、可扩展**的 runtimeRules JSON 规范
* 并给了 **趋势 / 网格 / 马丁** 三份**完整可落库的示例**
* 每份都满足：**多因子 + 全满足 + 可进场 + 可退场**，并且字段对开发非常友好
