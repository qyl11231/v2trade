# v2trade 时间管理专项代码重构实施计划

> **文档目标**：提供一个可执行、分步骤的清单式指南，指导开发人员完成对 `v2trade` 系统时间处理逻辑的全面重构。本计划旨在确保重构过程的系统性、安全性和可验证性，将《统一时间管理架构方案》中的设计原则精确落地。

**文档版本**：1.0  
**制定团队**：Manus AI 资深量化架构组
**日期**：2026-01-11

---

## 1. 重构准备 (Preparation)

在开始任何代码修改之前，请务必完成以下准备工作：

1.  **创建独立分支**：从 `main` 或 `develop` 分支创建一个新的特性分支，例如 `feature/time-refactoring`。所有修改都必须在此分支上进行，严禁直接在主干分支上操作。
    ```bash
    git checkout develop
    git pull
    git checkout -b feature/time-refactoring
    ```
2.  **确认单元测试覆盖**：检查现有与时间相关的单元测试。如果覆盖率不足，请先补充必要的测试用例，以验证重构前后的行为一致性。
3.  **本地环境验证**：确保您的本地开发环境可以完整运行项目，包括连接到OKX模拟盘、本地QuestDB和MySQL实例。

## 2. 重构执行步骤 (Execution Steps)

请严格按照以下步骤顺序执行。每一步都是后续步骤的基础。

### 第 1 步：核心模型升级 - 全面拥抱 `java.time.Instant`

**目标**：将系统中所有表示“时刻”的 `long` 和 `LocalDateTime` 字段，统一替换为 `java.time.Instant`。

| 序号 | 文件路径 | 修改说明 |
| :--- | :--- | :--- |
| 1.1 | `market/model/event/KlineEvent.java` | 将 `long openTime`, `long closeTime`, `long eventTime` 修改为 `Instant openTime`, `Instant closeTime`, `Instant eventTime`。 |
| 1.2 | `market/model/NormalizedKline.java` | 将 `long timestamp`, `Long exchangeTimestamp` 修改为 `Instant timestamp`, `Instant exchangeTimestamp`。同时调整 `builder` 和 `setter` 方法。 |
| 1.3 | `market/model/dto/KlineResponse.java` | 将 `long timestamp` 修改为 `Instant timestamp`。`time` 字段作为面向前端的展示字段，暂时保留 `String`，其转换逻辑将在Controller层处理。 |
| 1.4 | `business/strategy/decision/context/snapshot/BarSnapshot.java` | 将 `LocalDateTime barCloseTime` 修改为 `Instant barCloseTime`。 |
| 1.5 | `common/util/UtcTimeConverter.java` | 重命名为 `TimeUtil.java`，并彻底重写，移除所有旧方法，仅提供基于 `Instant` 和 `ZonedDateTime` 的静态工具方法。 |

**`TimeUtil.java` 核心实现参考**：

```java
package com.qyl.v2trade.common.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public final class TimeUtil {
    private TimeUtil() {}

    public static final ZoneId ZONE_UTC = ZoneId.of("UTC");
    public static final ZoneId ZONE_SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter LOCAL_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZONE_SHANGHAI);

    // 将 UTC Instant 格式化为上海时间字符串 (用于日志或DTO)
    public static String formatAsShanghaiString(Instant instant) {
        if (instant == null) return null;
        return LOCAL_FORMATTER.format(instant);
    }

    // 将毫秒时间戳安全地转换为 Instant
    public static Instant fromEpochMilli(long epochMilli) {
        return Instant.ofEpochMilli(epochMilli);
    }
}
```

### 第 2 步：根除“魔法数字” - 清理硬编码时区偏移

**目标**：全局搜索并彻底清除所有 `* 8` 或 `+8`/`-8` 小时之类的硬编码时区计算。

| 序号 | 文件路径 | 问题定位与修改方案 |
| :--- | :--- | :--- |
| 2.1 | `market/calibration/service/impl/HistoricalKlineFetcherImpl.java` | **定位**：`fetchHistoricalKlines` 方法中的 `+60000*60*8`。<br>**方案**：彻底删除这些加减操作。API的 `before` 和 `after` 参数必须直接使用上游传入的、未经修改的UTC时间戳。如果上游时间戳有问题，应修复上游，而不是在此处“打补丁”。 |
| 2.2 | `market/calibration/util/KlineTimeCalculator.java` | **定位**：`calculateExpectedTimestamps` 方法中的 `-60000 * 60 * 8`。<br>**方案**：彻底删除这些减法操作。该方法接收的参数 `ostartTimestamp` 和 `oendTimestamp` 必须被假定为已经是正确的UTC时间戳。 |

### 第 3 步：关键服务重构

**目标**：将新的时间模型和规范应用到核心业务服务中。

| 序号 | 文件路径 | 重构指南 |
| :--- | :--- | :--- |
| 3.1 | `market/subscription/collector/channel/impl/KlineChannel.java` | 在 `onMessage` 方法中，解析来自OKX的 `long` 类型时间戳后，**立即**使用 `TimeUtil.fromEpochMilli()` 将其转换为 `Instant`。后续所有操作，包括创建 `KlineEvent`，都使用 `Instant` 对象。 |
| 3.2 | `market/subscription/persistence/storage/impl/QuestDbMarketStorageService.java` | 在 `saveKline` 方法中，确保从 `NormalizedKline` 获取的是 `Instant` 对象。使用 `preparedStatement.setTimestamp(index, Timestamp.from(instant))` 将 `Instant` 写入QuestDB。日志记录中，使用 `TimeUtil.formatAsShanghaiString()` 来展示本地时间。 |
| 3.3 | `market/calibration/service/impl/HistoricalKlineFetcherImpl.java` | 确保调用 `okxApiClient.getKlines` 时，传入的 `after` 和 `before` 参数是通过 `instant.toEpochMilli()` 从 `Instant` 对象转换而来的 `long` 值。 |
| 3.4 | `market/web/controller/MarketDataController.java` | 在返回给前端的 `KlineResponse` DTO中，进行最后的时区转换。将 `Instant` 类型的 `timestamp` 字段，使用 `TimeUtil.formatAsShanghaiString()` 转换为 `String` 类型的 `time` 字段。 |

**示例：`MarketDataController` DTO转换**

```java
// 假设 kline 是一个包含 Instant timestamp 的内部模型对象
KlineResponse dto = new KlineResponse();
dto.setTimestamp(kline.getTimestamp()); // 保留Instant，供内部使用
dto.setTime(TimeUtil.formatAsShanghaiString(kline.getTimestamp())); // 转换为CST字符串，供前端展示
// ... set其他字段
return dto;
```

### 第 4 步：编译与测试

1.  **全量编译**：在完成上述所有修改后，执行 `mvn clean install`，解决所有编译错误。这个过程可能会因为类型不匹配而揭示出遗漏的修改点。
2.  **运行单元测试**：执行所有单元测试，确保现有逻辑没有被破坏。
3.  **集成测试**：
    *   启动完整应用。
    *   **验证实时行情**：订阅一个交易对，观察 `kline_1m` 表中写入的数据，其 `ts` 字段是否为正确的UTC开盘时间。
    *   **验证历史数据拉取**：手动触发一次历史数据校准，检查 `okxApiClient` 发出的请求，其 `before` 和 `after` 参数是否为正确的UTC毫秒时间戳。
    *   **验证API查询**：调用K线查询接口，对比返回的JSON中 `time` 字段是否为正确的北京时间字符串。

### 第 5 步：代码审查与合并

1.  **发起Pull Request**：将 `feature/time-refactoring` 分支推送到远程，并创建指向 `develop` 分支的Pull Request。
2.  **团队审查**：邀请团队核心成员进行代码审查，重点关注：
    *   是否还有遗漏的 `long`/`LocalDateTime` 时间表示？
    *   是否还有任何硬编码的时区计算？
    *   时区转换是否都集中在 `TimeUtil` 和 Controller/DTO 层？
3.  **合并分支**：在审查通过后，将分支合并到 `develop`，并删除该特性分支。

## 3. 重构验收标准

- [ ] **代码层面**：项目中不再存在任何 `+8` 或 `-8` 小时的硬编码时区换算。
- [ ] **模型层面**：所有核心领域模型（Event, Entity, DTO）均使用 `java.time.Instant` 作为唯一的内部时间表示。
- [ ] **数据层面**：QuestDB 和 MySQL 中存储的所有时间戳字段，其物理值均为UTC时间。
- [ ] **功能层面**：实时行情入库、历史数据拉取、API查询功能均正常，且时间戳准确无误。
- [ ] **日志层面**：日志中打印的时间戳清晰地标明了UTC和CST（上海时间），便于问题排查。
