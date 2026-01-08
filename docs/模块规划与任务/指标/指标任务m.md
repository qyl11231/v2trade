非常好，现在这一步**不是再设计系统，而是把“系统工程”变成“可执行工程计划”**。
我会用 **Cursor 能 100%照着干、不走偏、不发散** 的方式来拆。

下面内容你可以**原样发给 Cursor**，不需要再解释。

---

# 指标模块 v1.2.1

## 分阶段开发计划 & 验收标准（给 Cursor）

> **目标**：
> 将《指标模块产品定义 + 工程开发规范 v1.2.1》
> **拆解为可开发、可验收、可冻结的工程阶段**

---

## 总体开发原则（Cursor 必须遵守）

1. **严格阶段推进**

   * 未通过当前阶段验收，不允许进入下一阶段
2. **每阶段必须“可运行 + 可验证”**
3. **不引入任何策略逻辑**
4. **不修改行情系统 / QuestDB 结构**
5. **时间语义必须按 v1.2.1 冻结规范处理**

---

# 阶段 0：工程骨架与契约冻结（Foundation）

> **目的**：
> 让整个指标模块在工程上“站得住”，但不做任何计算

---

## 0.1 任务清单

### 任务 0.1.1：创建指标模块工程结构

* 按规范创建包结构：

```text
indicator/
├── api/
├── domain/
├── definition/
├── engine/
│   ├── ta4j/
│   ├── custom/
├── series/
├── calculator/
├── repository/
├── bootstrap/
├── observability/
```

---

### 任务 0.1.2：定义核心接口（空实现）

必须创建但**暂不实现逻辑**：

* `IndicatorDefinition`
* `IndicatorEngine`
* `IndicatorResult`
* `BarSeriesManager`
* `IndicatorRepository`

---

### 任务 0.1.3：冻结时间语义适配接口

创建：

```java
interface TimeAlignmentAdapter {
    NormalizedBar normalize(MarketBar rawBar);
}
```

> ❗ 不允许在任何其他地方处理 bar_time

---

## 阶段 0 验收标准

✅ 项目可启动
✅ 所有接口可编译
✅ 不包含任何 ta4j / 计算代码
✅ 不依赖 strategy 模块
✅ 时间语义适配职责明确、唯一

---

# 阶段 1：行情时间序列管理（BarSeries Layer）

> **目的**：
> 构建稳定、可持续的 K 线时间序列基础

---

## 1.1 任务清单

### 任务 1.1.1：实现 BarSeriesManager

能力要求：

* 按 `(trading_pair_id, timeframe)` 管理 BarSeries
* 启动时：

  * 从 QuestDB 加载最近 **365 根**
* 只允许 append
* 禁止回写历史

---

### 任务 1.1.2：接入 TimeAlignmentAdapter

* QuestDB bar_time → bar_close_time
* 所有进入 BarSeries 的 bar：

  * **必须已归一化**

---

### 任务 1.1.3：处理 BAR_CLOSED 事件

* 只接受 `BAR_CLOSED`
* 忽略 forming bar

---

## 阶段 1 验收标准

✅ 任意交易对 + 周期 BarSeries 可持续 append
✅ 重启后 BarSeries 可重建
✅ 所有 bar_time 符合 bar_close_time 语义
✅ forming bar 不进入 Series

---

# 阶段 2：指标定义与注册（Definition Layer）

> **目的**：
> 明确“系统支持哪些指标”，但仍不计算

---

## 2.1 任务清单

### 任务 2.1.1：实现 IndicatorDefinitionRegistry

* 支持注册：

  * ta4j 指标
  * 自研指标
* 校验：

  * `code + version` 唯一
  * 参数 & 返回结构合法

---

### 任务 2.1.2：实现 ParameterSpec / ReturnSpec

* 强类型参数
* 禁止 Map<Object>

---

## 阶段 2 验收标准

✅ 系统启动时自动注册指标
✅ 参数定义非法直接失败
✅ 指标元数据可枚举
✅ 未注册指标不可被计算

---

# 阶段 3：指标计算引擎（Engine Layer）

> **目的**：
> 让指标真正“算得出来”

---

## 3.1 任务清单

### 任务 3.1.1：实现 Ta4jIndicatorEngine

* 仅负责：

  * ta4j → IndicatorResult 转换
* 不暴露 ta4j 类型

---

### 任务 3.1.2：实现 CustomIndicatorEngine（示例）

* 至少实现一个：

  * 简单 MA 或自定义指标
* 验证引擎可插拔性

---

## 阶段 3 验收标准

✅ ta4j 与自研指标可并存
✅ 相同 BarSeries 输入 → 相同输出
✅ 引擎之间无相互依赖

---

# 阶段 4：指标调度与触发（Calculator）

> **目的**：
> 形成完整“bar close → 指标计算”的流水线

---

## 4.1 任务清单

### 任务 4.1.1：实现 IndicatorCalculator

* 仅在：

  * BAR_CLOSED
  * Series append 后
* 对该 timeframe 的所有指标计算

---

### 任务 4.1.2：实现数据质量判断

* 数据不足 → PARTIAL
* 异常 → INVALID

---

## 阶段 4 验收标准

✅ 每根 bar 只计算一次
✅ 数据不足状态正确
✅ 不存在重复计算

---

# 阶段 5：指标持久化（Repository）

> **目的**：
> 把指标变成“系统事实”

---

## 5.1 任务清单

### 任务 5.1.1：实现 IndicatorRepository

* 按唯一键写入
* 冲突：

  * ignore
  * 记录告警日志

---

### 任务 5.1.2：实现 latest cache（可选）

* Redis 缓存最新指标
* MySQL 为最终事实源

---

## 阶段 5 验收标准

✅ 指标结果入库
✅ 幂等写入生效
✅ 冲突不覆盖历史

---

# 阶段 6：可观测性（Observability）

> **目的**：
> 让系统在生产“可被理解”

---

## 6.1 任务清单

* 指标计算耗时 metrics
* 错误 / PARTIAL 统计
* 关键日志

---

## 阶段 6 验收标准

✅ 能定位慢指标
✅ 能定位失败指标
✅ 无日志泛滥

---

# 阶段 7：策略对接（只读）

> **目的**：
> 验证指标模块能真正服务交易系统

---

## 7.1 任务清单

* 提供：

  * `getLatestIndicator(...)`
* 策略阶段二只读使用

---

## 阶段 7 验收标准（最终）

✅ 策略不参与指标计算
✅ 重启后指标一致
✅ 时间语义正确
✅ 可回放、可审计

---

# 最终冻结声明（非常重要）

> **只要 Cursor 严格按以上阶段推进，
> 这个指标模块将具备工业级稳定性、可维护性与长期演进能力。**

---

如果你愿意，下一步我可以帮你做 **三件极其省时间的事之一**：

1. **直接把这个计划转换成 Cursor Prompt（逐阶段）**
2. **给你一份“阶段 1 / 阶段 3 的参考实现骨架代码”**
3. **继续拆“策略阶段二”的同等级任务与验收规范**

你已经把“设计难题”全部解决了，现在剩下的是**正确地交付工程**。
