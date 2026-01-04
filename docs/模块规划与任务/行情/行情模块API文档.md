# 行情模块 API 文档

> **文档说明**：本文档供前端（Web端）和其他模块（如策略模块）调用行情模块的REST API接口使用。

**文档版本**：v1.0  
**最后更新**：2025-01-15  
**Base URL**：`/api`

---

## 📋 目录

1. [通用说明](#通用说明)
2. [行情查询API](#行情查询api)
3. [行情校准API](#行情校准api)
4. [数据模型](#数据模型)
5. [错误码说明](#错误码说明)
6. [调用示例](#调用示例)

---

## 一、通用说明

### 1.1 请求/响应格式

**请求头**：
```
Content-Type: application/json
Accept: application/json
```

**响应格式**：
```json
{
  "code": 200,
  "message": "成功",
  "data": { ... }
}
```

**响应字段说明**：
- `code`: 响应码（200表示成功，其他表示失败）
- `message`: 响应消息
- `data`: 响应数据（成功时返回，失败时可能为null）

### 1.2 时间格式

**时间戳格式**：
- 所有时间戳使用**毫秒级UTC时间戳**（epoch millis）
- 例如：`1710000000000` 表示 `2024-03-10 00:00:00 UTC`

**日期时间字符串格式**：
- 格式：`yyyy-MM-dd HH:mm:ss`
- 时区：UTC（用户输入的时间字符串直接当作UTC时间处理）
- 例如：`2024-03-10 00:00:00`

### 1.3 分页参数

**通用分页参数**：
- `current`: 当前页码（从1开始，默认1）
- `size`: 每页数量（默认10）

**分页响应格式**：
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "records": [...],
    "total": 100,
    "size": 10,
    "current": 1,
    "pages": 10
  }
}
```

---

## 二、行情查询API

### 2.1 查询K线数据

**接口**：`GET /api/market/kline`

**功能**：查询指定时间范围内的K线数据

**请求参数**：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `symbol` | String | 是 | 交易对符号（如：BTC-USDT） |
| `interval` | String | 是 | K线周期（如：1m, 5m, 15m, 1h） |
| `from` | Long | 否 | 开始时间（毫秒时间戳） |
| `to` | Long | 否 | 结束时间（毫秒时间戳） |
| `limit` | Integer | 否 | 限制返回数量（默认1000，最大10000） |

**请求示例**：
```
GET /api/market/kline?symbol=BTC-USDT&interval=1m&from=1710000000000&to=1710086400000&limit=1000
```

**响应示例**：
```json
{
  "code": 200,
  "message": "成功",
  "data": [
    {
      "symbol": "BTC-USDT",
      "interval": "1m",
      "timestamp": 1710000000000,
      "open": 42000.0,
      "high": 42100.0,
      "low": 41950.0,
      "close": 42080.0,
      "volume": 123.45
    },
    ...
  ]
}
```

**响应字段说明**：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `symbol` | String | 交易对符号 |
| `interval` | String | K线周期 |
| `timestamp` | Long | 时间戳（毫秒，UTC） |
| `open` | Double | 开盘价 |
| `high` | Double | 最高价 |
| `low` | Double | 最低价 |
| `close` | Double | 收盘价 |
| `volume` | Double | 成交量 |

**注意事项**：
- 如果`from`和`to`都为空，返回最新的K线数据（最多`limit`条）
- 如果只提供`from`，返回从`from`开始到最新的数据
- 如果只提供`to`，返回从最早到`to`的数据
- 时间范围过大时建议使用`limit`限制返回数量

---

### 2.2 查询最新K线

**接口**：`GET /api/market/kline/latest`

**功能**：查询指定交易对和周期的最新一根K线

**请求参数**：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `symbol` | String | 是 | 交易对符号（如：BTC-USDT） |
| `interval` | String | 是 | K线周期（如：1m） |

**请求示例**：
```
GET /api/market/kline/latest?symbol=BTC-USDT&interval=1m
```

**响应示例**：
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "symbol": "BTC-USDT",
    "interval": "1m",
    "timestamp": 1710000000000,
    "open": 42000.0,
    "high": 42100.0,
    "low": 41950.0,
    "close": 42080.0,
    "volume": 123.45
  }
}
```

**错误响应**（未找到数据）：
```json
{
  "code": 404,
  "message": "未找到K线数据",
  "data": null
}
```

---

### 2.3 查询时间戳对齐的K线

**接口**：`GET /api/market/kline/timestamp`

**功能**：查询指定时间戳对应的K线数据（用于时间对齐）

**请求参数**：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `symbol` | String | 是 | 交易对符号 |
| `timestamp` | Long | 是 | 时间戳（毫秒，会对齐到周期边界） |

**请求示例**：
```
GET /api/market/kline/timestamp?symbol=BTC-USDT&timestamp=171000001234
```

**响应示例**：
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "symbol": "BTC-USDT",
    "interval": "1m",
    "timestamp": 1710000000000,
    "open": 42000.0,
    "high": 42100.0,
    "low": 41950.0,
    "close": 42080.0,
    "volume": 123.45
  }
}
```

---

### 2.4 查询今日统计

**接口**：`GET /api/market/kline/today-stats`

**功能**：查询指定交易对今日的K线统计信息

**请求参数**：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `symbol` | String | 是 | 交易对符号 |

**请求示例**：
```
GET /api/market/kline/today-stats?symbol=BTC-USDT
```

**响应示例**：
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "symbol": "BTC-USDT",
    "todayKlineCount": 1440,
    "firstKlineTime": 1710000000000,
    "lastKlineTime": 1710086340000,
    "todayHigh": 42500.0,
    "todayLow": 41800.0,
    "todayVolume": 12345.67
  }
}
```

---

## 三、行情校准API

### 3.1 任务配置管理

#### 3.1.1 创建任务配置

**接口**：`POST /api/market-calibration/config`

**功能**：创建新的校准任务配置

**请求体**：
```json
{
  "taskName": "BTC 缺失数据自动补全",
  "taskType": "MISSING_DATA",
  "tradingPairId": 1,
  "executionMode": "AUTO",
  "intervalHours": 1,
  "enabled": 1,
  "remark": "每小时扫描一次，自动补全缺失数据"
}
```

**字段说明**：

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `taskName` | String | 是 | 任务名称 |
| `taskType` | String | 是 | 任务类型：`MISSING_DATA`（缺失检测）或 `DATA_VERIFY`（数据核对） |
| `tradingPairId` | Long | 是 | 交易对ID |
| `executionMode` | String | 是 | 执行模式：`AUTO`（自动）或 `MANUAL`（手动） |
| `intervalHours` | Integer | 否 | 自动模式：检测周期（小时），如1表示检测最近1小时 |
| `startTime` | String | 否 | 手动模式：开始时间（格式：`yyyy-MM-dd HH:mm:ss`） |
| `endTime` | String | 否 | 手动模式：结束时间（格式：`yyyy-MM-dd HH:mm:ss`） |
| `enabled` | Integer | 否 | 是否启用：1-启用 0-禁用（默认1） |
| `remark` | String | 否 | 备注说明 |

**响应示例**：
```json
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "id": 1,
    "taskName": "BTC 缺失数据自动补全",
    "taskType": "MISSING_DATA",
    "tradingPairId": 1,
    "symbol": "BTC-USDT",
    "marketType": "SWAP",
    "executionMode": "AUTO",
    "intervalHours": 1,
    "enabled": 1,
    "remark": "每小时扫描一次，自动补全缺失数据",
    "createdAt": "2024-01-15 10:00:00",
    "updatedAt": "2024-01-15 10:00:00"
  }
}
```

---

#### 3.1.2 更新任务配置

**接口**：`PUT /api/market-calibration/config/{id}`

**功能**：更新指定ID的任务配置

**路径参数**：
- `id`: 任务配置ID

**请求体**：同创建接口（所有字段可选）

**响应示例**：同创建接口

---

#### 3.1.3 删除任务配置

**接口**：`DELETE /api/market-calibration/config/{id}`

**功能**：删除指定ID的任务配置

**路径参数**：
- `id`: 任务配置ID

**响应示例**：
```json
{
  "code": 200,
  "message": "删除成功",
  "data": null
}
```

---

#### 3.1.4 查询任务配置列表

**接口**：`GET /api/market-calibration/config`

**功能**：查询任务配置列表（支持分页和筛选）

**请求参数**：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `taskType` | String | 否 | 任务类型筛选（MISSING_DATA / DATA_VERIFY） |
| `tradingPairId` | Long | 否 | 交易对ID筛选 |
| `executionMode` | String | 否 | 执行模式筛选（AUTO / MANUAL） |
| `enabled` | Integer | 否 | 启用状态筛选（1-启用 0-禁用） |
| `current` | Integer | 否 | 当前页码（默认1） |
| `size` | Integer | 否 | 每页数量（默认10） |

**请求示例**：
```
GET /api/market-calibration/config?taskType=MISSING_DATA&enabled=1&current=1&size=10
```

**响应示例**：
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "records": [
      {
        "id": 1,
        "taskName": "BTC 缺失数据自动补全",
        "taskType": "MISSING_DATA",
        "tradingPairId": 1,
        "symbol": "BTC-USDT",
        "marketType": "SWAP",
        "executionMode": "AUTO",
        "intervalHours": 1,
        "enabled": 1,
        "remark": "每小时扫描一次",
        "createdAt": "2024-01-15 10:00:00",
        "updatedAt": "2024-01-15 10:00:00"
      },
      ...
    ],
    "total": 10,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

---

#### 3.1.5 查询任务配置详情

**接口**：`GET /api/market-calibration/config/{id}`

**功能**：查询指定ID的任务配置详情

**路径参数**：
- `id`: 任务配置ID

**响应示例**：同创建接口的响应

---

#### 3.1.6 启用/禁用任务

**接口**：`POST /api/market-calibration/config/{id}/toggle`

**功能**：启用或禁用指定ID的任务配置

**路径参数**：
- `id`: 任务配置ID

**请求体**：
```json
{
  "enabled": 1
}
```

**字段说明**：
- `enabled`: `1`表示启用，`0`表示禁用

**响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "enabled": 1,
    ...
  }
}
```

---

#### 3.1.7 手动执行任务

**接口**：`POST /api/market-calibration/config/{id}/execute`

**功能**：手动触发执行指定ID的任务（支持手动模式任务）

**路径参数**：
- `id`: 任务配置ID

**请求体**：
```json
{
  "startTime": "2024-01-15 00:00:00",
  "endTime": "2024-01-15 23:59:59"
}
```

**字段说明**：

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `startTime` | String | 是 | 开始时间（格式：`yyyy-MM-dd HH:mm:ss`，UTC时间） |
| `endTime` | String | 是 | 结束时间（格式：`yyyy-MM-dd HH:mm:ss`，UTC时间） |

**响应示例**：
```json
{
  "code": 200,
  "message": "执行成功",
  "data": {
    "taskConfigId": 1,
    "taskName": "BTC 缺失数据检测",
    "status": "SUCCESS",
    "missingCount": 5,
    "filledCount": 5,
    "executeDurationMs": 1234,
    "executeLog": "{\"missingTimestamps\":[...],\"totalMissingCount\":5}"
  }
}
```

**响应字段说明**：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `taskConfigId` | Long | 任务配置ID |
| `taskName` | String | 任务名称 |
| `status` | String | 执行状态：`SUCCESS`（成功）、`FAILED`（失败） |
| `missingCount` | Integer | 缺失K线数量（仅缺失检测任务） |
| `filledCount` | Integer | 补全K线数量（仅缺失检测任务） |
| `duplicateCount` | Integer | 重复数据数量（仅核对任务） |
| `errorCount` | Integer | 异常数据数量（仅核对任务） |
| `executeDurationMs` | Long | 执行耗时（毫秒） |
| `executeLog` | String | 执行日志详情（JSON字符串） |

---

### 3.2 执行日志查询

#### 3.2.1 查询执行日志列表

**接口**：`GET /api/market-calibration/log`

**功能**：查询任务执行日志列表（支持分页和筛选）

**请求参数**：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `taskConfigId` | Long | 否 | 任务配置ID筛选 |
| `tradingPairId` | Long | 否 | 交易对ID筛选 |
| `status` | String | 否 | 执行状态筛选（RUNNING / SUCCESS / FAILED） |
| `current` | Integer | 否 | 当前页码（默认1） |
| `size` | Integer | 否 | 每页数量（默认10） |

**请求示例**：
```
GET /api/market-calibration/log?taskConfigId=1&status=SUCCESS&current=1&size=10
```

**响应示例**：
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "records": [
      {
        "id": 1,
        "taskConfigId": 1,
        "taskName": "BTC 缺失数据检测",
        "taskType": "MISSING_DATA",
        "tradingPairId": 1,
        "symbol": "BTC-USDT",
        "executionMode": "MANUAL",
        "detectStartTime": "2024-01-15 00:00:00",
        "detectEndTime": "2024-01-15 23:59:59",
        "status": "SUCCESS",
        "missingCount": 5,
        "filledCount": 5,
        "executeDurationMs": 1234,
        "errorMessage": null,
        "executeLog": "{\"missingTimestamps\":[...]}",
        "createdAt": "2024-01-15 10:00:00"
      },
      ...
    ],
    "total": 100,
    "size": 10,
    "current": 1,
    "pages": 10
  }
}
```

---

#### 3.2.2 查询执行日志详情

**接口**：`GET /api/market-calibration/log/{id}`

**功能**：查询指定ID的执行日志详情

**路径参数**：
- `id`: 执行日志ID

**响应示例**：同日志列表中的单条记录格式

---

## 四、数据模型

### 4.1 K线数据模型

**KlineResponse**：

```json
{
  "symbol": "BTC-USDT",
  "interval": "1m",
  "timestamp": 1710000000000,
  "open": 42000.0,
  "high": 42100.0,
  "low": 41950.0,
  "close": 42080.0,
  "volume": 123.45
}
```

### 4.2 任务配置模型

**TaskConfigVO**：

```json
{
  "id": 1,
  "taskName": "BTC 缺失数据自动补全",
  "taskType": "MISSING_DATA",
  "tradingPairId": 1,
  "symbol": "BTC-USDT",
  "marketType": "SWAP",
  "executionMode": "AUTO",
  "intervalHours": 1,
  "startTime": null,
  "endTime": null,
  "enabled": 1,
  "remark": "备注说明",
  "createdAt": "2024-01-15 10:00:00",
  "updatedAt": "2024-01-15 10:00:00"
}
```

**字段枚举值**：

- `taskType`：
  - `MISSING_DATA`: 缺失数据检测
  - `DATA_VERIFY`: 数据核对

- `executionMode`：
  - `AUTO`: 自动执行
  - `MANUAL`: 手动执行

### 4.3 执行日志模型

**TaskLogVO**：

```json
{
  "id": 1,
  "taskConfigId": 1,
  "taskName": "BTC 缺失数据检测",
  "taskType": "MISSING_DATA",
  "tradingPairId": 1,
  "symbol": "BTC-USDT",
  "executionMode": "MANUAL",
  "detectStartTime": "2024-01-15 00:00:00",
  "detectEndTime": "2024-01-15 23:59:59",
  "status": "SUCCESS",
  "missingCount": 5,
  "filledCount": 5,
  "duplicateCount": 0,
  "errorCount": 0,
  "executeDurationMs": 1234,
  "errorMessage": null,
  "executeLog": "{\"missingTimestamps\":[...]}",
  "createdAt": "2024-01-15 10:00:00"
}
```

**字段枚举值**：

- `status`：
  - `RUNNING`: 执行中
  - `SUCCESS`: 成功
  - `FAILED`: 失败

---

## 五、错误码说明

| 错误码 | 说明 | 示例 |
|--------|------|------|
| 200 | 成功 | - |
| 400 | 请求参数错误 | 缺少必填参数、参数格式错误 |
| 404 | 资源不存在 | 任务配置不存在、K线数据未找到 |
| 500 | 服务器内部错误 | 数据库连接失败、系统异常 |

**错误响应格式**：
```json
{
  "code": 400,
  "message": "任务名称不能为空",
  "data": null
}
```

---

## 六、调用示例

### 6.1 JavaScript/TypeScript 示例

```typescript
// 查询K线数据
async function queryKlines(symbol: string, interval: string, from: number, to: number) {
  const response = await fetch(
    `/api/market/kline?symbol=${symbol}&interval=${interval}&from=${from}&to=${to}`
  );
  const result = await response.json();
  if (result.code === 200) {
    return result.data;
  } else {
    throw new Error(result.message);
  }
}

// 创建校准任务
async function createCalibrationTask(taskConfig: any) {
  const response = await fetch('/api/market-calibration/config', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(taskConfig)
  });
  const result = await response.json();
  if (result.code === 200) {
    return result.data;
  } else {
    throw new Error(result.message);
  }
}

// 手动执行任务
async function executeTask(taskConfigId: number, startTime: string, endTime: string) {
  const response = await fetch(`/api/market-calibration/config/${taskConfigId}/execute`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ startTime, endTime })
  });
  const result = await response.json();
  if (result.code === 200) {
    return result.data;
  } else {
    throw new Error(result.message);
  }
}
```

### 6.2 Java 示例

```java
// 查询K线数据
RestTemplate restTemplate = new RestTemplate();
String url = String.format(
    "/api/market/kline?symbol=%s&interval=%s&from=%d&to=%d",
    "BTC-USDT", "1m", 1710000000000L, 1710086400000L
);
Result<List<KlineResponse>> result = restTemplate.getForObject(url, Result.class);
if (result.getCode() == 200) {
    List<KlineResponse> klines = result.getData();
    // 处理数据
}

// 创建校准任务
TaskConfigCreateRequest request = new TaskConfigCreateRequest();
request.setTaskName("BTC 缺失数据自动补全");
request.setTaskType("MISSING_DATA");
request.setTradingPairId(1L);
request.setExecutionMode("AUTO");
request.setIntervalHours(1);
request.setEnabled(1);

Result<TaskConfigVO> result = restTemplate.postForObject(
    "/api/market-calibration/config",
    request,
    Result.class
);
```

---

## 七、注意事项

### 7.1 时间处理

- **所有时间戳使用UTC时间**（毫秒级）
- **用户输入的时间字符串直接当作UTC时间处理**，不进行时区转换
- 例如：用户输入 `2024-01-15 00:00:00` 应该查询UTC时间的 `2024-01-15 00:00:00`

### 7.2 性能建议

- K线查询建议使用`limit`参数限制返回数量（默认1000，最大10000）
- 时间范围过大时建议分批查询
- 最新K线查询适合高频调用，历史数据查询建议缓存结果

### 7.3 错误处理

- 所有API调用都应该检查响应码（`code`字段）
- 网络错误、超时等异常应该进行重试（建议指数退避）
- 参数校验失败时，响应中的`message`字段会包含具体错误信息

### 7.4 数据完整性

- K线数据可能由于网络问题存在缺失，建议使用校准任务定期检查和补全
- 查询时如果发现数据缺失，可以通过校准API手动触发补全

---

**文档维护者**：后端开发团队  
**最后审核日期**：2025-01-15

