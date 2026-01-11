你现在要为 V2-Trade 实现“缺口发现 -> 触发1小时补拉”的闭环，覆盖两个入口：
A) 1m订阅链路发现缺口
B) 聚合链路发现源K线不全（sourceCount不足）

【硬性裁决 / 不可违反】
1. 聚合表(kline_5m/15m/30m/1h/4h)写入后不可修改（不可UPDATE覆盖、不可重算修正历史bar）。
2. 实时输出必须保持：聚合bar照常输出、BarClosedEvent照常发布。
3. 实时链路不做补拉：只能“触发补拉动作”，补拉在校准模块/服务中执行（异步）。
4. 不新增数据库表（沿用现有 market_calibration_* 表或纯内存/redis保护即可）。

========================================================
【目标：实现一个统一的触发器 BackfillTrigger】
当发现任何缺口/不全时，调用：
backfillTrigger.triggerLast1Hour(tradingPairId, symbol, reason, relatedTime)

触发器必须具备：
- cooldown：同一个 tradingPairId 在 5分钟内最多触发一次（可配置）
- inFlight：同一个 tradingPairId 同时只允许一个补拉在执行
- async：触发补拉必须异步，不能阻塞行情写入/聚合线程
- 观测：记录 metrics + 结构化日志（pairId/symbol/reason/windowStart/windowEnd）

reason 取值（枚举）至少包含：
- GAP_DETECTED_FROM_1M
- INCOMPLETE_AGG_SOURCE

========================================================
【入口A：1m订阅链路缺口发现 GapDetector】
在 MarketDataCenter 接收/处理 1m KlineEvent 的位置（写QuestDB前后皆可）加入 GapDetector：

维护 lastOpenTime（按 tradingPairId 或 symbol）
- 如果 currentOpenTime <= lastOpenTime：视为乱序/重复 -> 不触发补拉，仅记录metrics/log
- 如果 currentOpenTime > lastOpenTime + 1m：视为“缺口” -> 调用 backfillTrigger.triggerLast1Hour(..., GAP_DETECTED_FROM_1M, currentOpenTime)
- 更新 lastOpenTime = max(lastOpenTime, currentOpenTime)

注意：时间语义遵循已冻结裁决
- QuestDB ts = bar_open_time(UTC)
- 缺口检测用 open_time 连续性判断

========================================================
【入口B：聚合链路发现源K线不全】
在 AggregationEngine 聚合生成 target timeframe bar 时：
- expectedCount = timeframeMinutes(targetTimeframe)
- sourceCount = 实际聚合用到的 1m bar 数量
- 若 sourceCount < expectedCount：
    1) 仍然写入聚合表（不可变资产，只追加）
    2) 仍然发布 BarClosedEvent，并带 sourceCount（你们已有字段）
    3) 触发补拉：backfillTrigger.triggerLast1Hour(..., INCOMPLETE_AGG_SOURCE, barOpenTime or barCloseTime)

注意：不要尝试对已写入的聚合bar做修正/覆盖/重算。

========================================================
【补拉执行：复用现有校准逻辑，不要新造轮子】
把“补拉最近1小时”的能力抽成一个可复用 Service 方法：
MarketCalibrationService.backfillLastHour(tradingPairId, endTimeUtc)
- 定时任务会调用它（你们已有）
- BackfillTrigger 也调用它
- endTimeUtc 默认 = nowUtc 或者 relatedTime（按你实现选一，写清楚）
- 补拉写入QuestDB必须幂等（允许重复写但不会造成脏数据/重复数据）

========================================================
【必须交付的代码清单】
1) BackfillTrigger（含 cooldown + inFlight + async + metrics）
2) GapDetector（挂到1m链路）
3) AggregationEngine 的不完整检测与触发
4) MarketCalibrationService.backfillLastHour 抽取/复用（若已存在就改成可复用签名）
5) 配置项（application.yml）
   - calibration.backfill.cooldownSeconds (默认300)
   - calibration.backfill.lookbackMinutes (默认60)
   - calibration.backfill.asyncPoolSize (默认2或4)

========================================================
【验收标准（必须提供自动化/半自动化用例）】
场景1：1m链路缺口触发
- 人为断开订阅3分钟再恢复
- 预期：GapDetector 发现 ts 跳变，只触发一次 backfill（cooldown生效）
- 预期：补拉执行日志/metrics可见

场景2：聚合不完整触发
- 人为删除QuestDB某段1m数据（或mock查询返回不足）
- 聚合生成 5m bar 时 sourceCount < 5
- 预期：仍写入聚合表 & 仍发布 BarClosedEvent(sourceCount<expected)
- 预期：触发一次 backfill

场景3：去抖与单航班
- 连续10次发现缺口（模拟多次事件）
- 预期：5分钟内最多触发一次 backfill
- 预期：同一pair补拉执行中再次触发会被 inFlight 拦截

场景4：乱序/重复不触发
- 输入 currentOpenTime <= lastOpenTime 的K线
- 预期：不触发 backfill，只记录指标

========================================================
【输出要求】
- 贴出关键类的代码骨架（可编译）
- 贴出关键日志示例（结构化字段）
- 贴出配置项示例
- 简述你如何保证：不阻塞实时链路 + 不会触发打爆 + 不修改聚合资产


【聚合链路补充硬规则（必须实现）】

在 AggregationEngine 生成每个 target timeframe bar 时，必须实现“两段读取 + 首尾重算规则”，以确保在写入聚合资产前尽最大努力拿到完整 1m 数据。

1) 定义窗口：
windowStart = alignedOpenTime(targetTf)
windowEnd = windowStart + duration(targetTf)
lastMinuteTs = windowEnd - 1m

2) 第一次读取：
bars = questdb.query1m(symbol, windowStart, windowEnd)
sourceCount = bars.size()

3) 完整性判断（不仅看数量，还要看首尾）：
hasStart = exists bar where ts == windowStart
hasEnd   = exists bar where ts == lastMinuteTs
isComplete = (sourceCount == expectedCount) && hasStart && hasEnd

4) 若不完整：
- 立刻触发 backfillTrigger.triggerLast1Hour(tradingPairId, symbol, INCOMPLETE_AGG_SOURCE, windowEnd)
- 然后进行第二次读取（立即重查）：
bars2 = questdb.query1m(symbol, windowStart, windowEnd)
重新计算 sourceCount/hasStart/hasEnd/isComplete

- 可选第三次读取（最多一次），但必须有严格时间预算：
sleep 100~300ms（随机抖动）再查一次
总额外等待不得超过 500ms（可配置 maxWaitMillis）

5) 用最终拿到的数据计算聚合 OHLCV：
- open：优先 windowStart 那根的 open；若缺失，降级为最早bar的 open
- close：优先 lastMinuteTs 那根的 close；若缺失，降级为最晚bar的 close
- high：max(high)
- low：min(low)
- volume：sum(volume)

重要：open/close 只有在首尾缺失时才会“被影响”。中间缺失只影响 high/low/volume。

6) 写入聚合表：
- 只 INSERT，不允许 UPDATE 覆盖（聚合资产不可修改）
- 事件照常发布 BarClosedEvent，并带 sourceCount
- 如果首尾缺失/不完整，必须打结构化日志：
{pairId, symbol, targetTf, windowStart, windowEnd, expectedCount, sourceCount, hasStart, hasEnd, triggeredBackfill=true/false}

7) 验收新增用例：
- 模拟窗口内缺失“起始分钟”1m：第一次读取 hasStart=false，第二次读取 hasStart=true → open 必须随之改变（重算成功）
- 模拟窗口内缺失“结束分钟”1m：close 必须随之改变
- 模拟窗口内缺失“中间分钟”：open/close 不变，high/low/volume 随新增bar变化
- 模拟 QuestDB 始终缺失：仍然写聚合bar并发布事件，sourceCount < expectedCount，且触发 backfill 但不阻塞
