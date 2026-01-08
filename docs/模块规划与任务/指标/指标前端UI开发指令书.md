# 【Cursor 开发指令书】V2-Trade 指标模块前端 UI-MVP

> **目的**：
> 为指标模块提供一个**可运行、可验证、可审计**的前端 UI，用于验证
> **BarClosedEvent → 指标计算 → 落库 → 可视化** 的完整闭环。

---

## 一、总体原则（必须遵守）

### 1. 不新增业务能力

* 只能消费**现有后端数据与接口**
* 不新增任何表、不新增任何计算逻辑、不新增任何后台调度

**禁止项（本任务绝对不能做）：**

* ❌ 不新增 `aggregation_config` 表或任何聚合管理 UI
* ❌ 不做指标补算 / 重算 / 覆盖
* ❌ 不做 WebSocket / SSE 推送
* ❌ 不做用户自定义指标 Definition 编辑
* ❌ 不做订阅参数编辑（params 只能创建时填写）
* ❌ 不做 1m 指标订阅（系统不支持）

---

### 2. 时间语义（强制）

* 所有时间展示均为 **UTC**
* 所有指标/日志的时间字段使用：

```
bar_time = bar_close_time
```

* 排序与筛选一律按 `bar_time DESC`

---

### 3. 指标订阅周期限制

* 前端 **不得出现 `1m` 周期**
* 只允许：

```
5m / 15m / 30m / 1h / 4h
```

---

## 二、菜单结构（新增）

### 菜单位置

在所有页面的侧边栏菜单中，新增"指标管理"模块（位于"策略模块"之后）：

```html
<li class="layui-nav-item">
    <a href="javascript:;">指标管理</a>
    <dl class="layui-nav-child">
        <dd><a href="/indicator/dashboard.html">仪表盘</a></dd>
        <dd><a href="/indicator/definitions.html">指标定义</a></dd>
        <dd><a href="/indicator/subscriptions.html">指标订阅</a></dd>
        <dd><a href="/indicator/values.html">指标结果</a></dd>
        <dd><a href="/indicator/logs.html">计算日志</a></dd>
    </dl>
</li>
```

### 菜单图标建议

- 仪表盘：📊
- 指标定义：📋
- 指标订阅：⭐
- 指标结果：📈
- 计算日志：📝

---

## 三、必须实现的页面（5 个）

### 页面 1：指标仪表盘（Dashboard）

**路由**：`/indicator/dashboard.html`  
**文件路径**：`src/main/resources/static/indicator/dashboard.html`

**目的**：快速确认系统是否在"跑"

**内容：**

1. **统计卡片区域**（4个卡片，参考原型）：
   - 今日计算次数（calc_log总数）
   - 成功率（SUCCESS数 / 总数）
   - 失败/冲突数（FAILED / CONFLICT）
   - 平均耗时（ms）

2. **最近一次BarClosedEvent信息**：
   - 显示最近一次事件：symbol / timeframe / barCloseTime

3. **最近20条计算日志表格**：
   - 列：bar_time(UTC close) | user | symbol | tf | indicator | status | cost | error(摘要)
   - 点击行 → 打开抽屉查看详情
   - 筛选：status下拉（全部/SUCCESS/FAILED/CONFLICT）、symbol输入框
   - 手动刷新按钮（可选自动刷新开关，默认关闭）

**API调用**：
- `GET /api/indicator/calc-logs?page=1&size=20` - 获取最近日志
- 统计卡片数据从前端计算（基于日志列表）

---

### 页面 2：指标定义（Definitions｜只读）

**路由**：`/indicator/definitions.html`  
**文件路径**：`src/main/resources/static/indicator/definitions.html`

**目的**：查看系统支持哪些指标

**功能：**

1. **筛选区域**：
   - keyword输入框（code/name搜索）
   - category下拉（全部/MOMENTUM/TREND/VOLATILITY）
   - engine下拉（全部/ta4j/custom）
   - 刷新按钮

2. **列表表格**：
   - 列：code | name | version | category | engine | minBars | supportedTF | schema | enabled
   - 点击"查看"按钮 → 打开抽屉展示完整schema

3. **抽屉内容**：
   - paramSchema（JSON格式化展示）
   - returnSchema（JSON格式化展示）

**API调用**：
- `GET /api/indicator/definitions?keyword=&category=&engine=&page=1&size=50`

**关键字段说明**：
- `supportedTimeframes`：数组格式 `["5m","15m","30m","1h","4h"]`
- `paramSchema`：JSON对象
- `returnSchema`：JSON对象

---

### 页面 3：指标订阅（Subscriptions｜核心）

**路由**：`/indicator/subscriptions.html`  
**文件路径**：`src/main/resources/static/indicator/subscriptions.html`

**目的**：创建与管理指标订阅

**功能：**

1. **筛选区域**：
   - userId输入框
   - symbol输入框（包含搜索）
   - timeframe下拉（**不包含1m**：5m/15m/30m/1h/4h）
   - enabled下拉（全部/启用/停用）
   - 刷新按钮

2. **列表表格**：
   - 列：user | symbol | market_type | timeframe | indicator | params | enabled | created_at | 操作
   - 操作列：详情按钮 | 启用/停用按钮

3. **新建订阅弹窗**（点击右上角"新建订阅"按钮）：
   - userId（输入框）
   - tradingPair（下拉，调用`GET /api/trading-pairs`）
   - timeframe（下拉，**不包含1m**：5m/15m/30m/1h/4h）
   - indicator（下拉，选择code@version）
   - params（textarea，JSON格式，提示用户按paramSchema填写）
   - enabled（开关，默认开启）
   - 创建按钮 | 取消按钮

4. **启用/停用功能**：
   - 点击"启用"或"停用"按钮
   - 调用 `PATCH /api/indicator/subscriptions/{id}`，body: `{"enabled": true/false}`

**API调用**：
- `GET /api/indicator/subscriptions?userId=&tradingPairId=&timeframe=&enabled=&page=1&size=50`
- `POST /api/indicator/subscriptions` - 创建订阅
- `PATCH /api/indicator/subscriptions/{id}` - 启停订阅
- `GET /api/trading-pairs` - 获取交易对列表（用于下拉）

**错误处理**：
- 409冲突 → Toast提示："已存在相同指标订阅"
- 5xx错误 → Toast提示："系统错误：{error message}"

---

### 页面 4：指标结果（Values）

**路由**：`/indicator/values.html`  
**文件路径**：`src/main/resources/static/indicator/values.html`

**目的**：验证指标是否真的算出来了

**功能：**

1. **视图切换**：
   - 默认：Latest Only（每个订阅维度最新一条）
   - 切换：历史视图（按时间范围）

2. **筛选区域**：
   - userId输入框
   - symbol输入框
   - timeframe下拉（5m/15m/30m/1h/4h）
   - Latest/History切换开关
   - 刷新按钮

3. **列表表格**：
   - 列：bar_time(UTC close) | user | symbol | tf | indicator | value | extra_values | data_quality | fingerprint(短) | created_at
   - 点击行 → 打开抽屉展示完整信息

4. **抽屉内容**：
   - 完整的指标值信息（JSON格式化展示）
   - 包含：value、extraValues、dataQuality、calcEngine、calcFingerprint等

**API调用**：
- `GET /api/indicator/values/latest?userId=&tradingPairId=&timeframe=&indicatorCode=&indicatorVersion=` - Latest视图
- `GET /api/indicator/values?userId=&tradingPairId=&timeframe=&indicatorCode=&indicatorVersion=&startTime=&endTime=&page=1&size=200` - History视图

**关键字段说明**：
- `extraValues`：JSON对象或null
- `dataQuality`：OK / PARTIAL / INVALID
- `fingerprint`：显示前16个字符 + "…"

---

### 页面 5：计算日志（Calc Logs）

**路由**：`/indicator/logs.html`  
**文件路径**：`src/main/resources/static/indicator/logs.html`

**目的**：系统级审计与问题定位

**功能：**

1. **筛选区域**：
   - userId输入框
   - symbol输入框
   - timeframe下拉（5m/15m/30m/1h/4h）
   - status下拉（全部/SUCCESS/FAILED/CONFLICT）
   - 刷新按钮

2. **列表表格**：
   - 列：created_at | bar_time(UTC close) | user | symbol | tf | indicator | status | cost_ms | fingerprint(短) | error_msg(摘要)
   - 点击行 → 打开抽屉展示完整信息

3. **抽屉内容**：
   - 完整日志信息
   - 完整fingerprint
   - 完整error_msg
   - 关联的indicator_value信息（如果存在）

**API调用**：
- `GET /api/indicator/calc-logs?userId=&tradingPairId=&timeframe=&indicatorCode=&status=&startTime=&endTime=&page=1&size=200`
- `GET /api/indicator/calc-logs/{id}` - 详情（可选）

**状态展示**：
- SUCCESS：绿色圆点 + 文字
- FAILED：红色圆点 + 文字
- CONFLICT：黄色圆点 + 文字

---

## 四、技术实现规范

### 1. 页面基础结构

所有页面都遵循Layui标准结构：

```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
    <title>指标管理 - [页面名称]</title>
    <link rel="stylesheet" href="/layui/css/layui.css">
    <style>
        /* 页面特定样式 */
    </style>
</head>
<body>
    <!-- 侧边栏菜单（复制现有结构并添加指标管理菜单） -->
    <div class="layui-side layui-bg-black">
        <div class="layui-side-scroll">
            <ul class="layui-nav layui-nav-tree" lay-filter="sideNav">
                <!-- 现有菜单项... -->
                <!-- 新增指标管理菜单 -->
                <li class="layui-nav-item">
                    <a href="javascript:;">指标管理</a>
                    <dl class="layui-nav-child">
                        <dd><a href="/indicator/dashboard.html">仪表盘</a></dd>
                        <dd><a href="/indicator/definitions.html">指标定义</a></dd>
                        <dd><a href="/indicator/subscriptions.html">指标订阅</a></dd>
                        <dd><a href="/indicator/values.html">指标结果</a></dd>
                        <dd><a href="/indicator/logs.html">计算日志</a></dd>
                    </dl>
                </li>
            </ul>
        </div>
    </div>
    
    <!-- 主内容区 -->
    <div class="layui-body">
        <!-- 页面内容 -->
    </div>
    
    <script src="/layui/layui.js"></script>
    <script>
        // 页面脚本
    </script>
</body>
</html>
```

### 2. API调用规范

使用Layui的`layui.use(['layer', 'jquery'])`或原生`fetch`：

```javascript
// 示例：获取订阅列表
layui.use(['layer', 'jquery'], function(){
    var layer = layui.layer;
    var $ = layui.$;
    
    function loadSubscriptions(params) {
        $.ajax({
            url: '/api/indicator/subscriptions',
            type: 'GET',
            data: params,
            dataType: 'json',
            success: function(res) {
                if (res.code === 200) {
                    // 渲染表格
                    renderTable(res.data);
                } else {
                    layer.msg(res.message || '查询失败', {icon: 2});
                }
            },
            error: function() {
                layer.msg('网络错误', {icon: 2});
            }
        });
    }
});
```

### 3. JSON字段处理

所有JSON字段统一处理：

```javascript
// 格式化JSON展示
function formatJson(obj) {
    if (!obj) return '-';
    if (typeof obj === 'string') {
        try {
            obj = JSON.parse(obj);
        } catch(e) {
            return obj;
        }
    }
    return JSON.stringify(obj, null, 2);
}

// 表格中展示JSON（截断）
function formatJsonShort(obj, maxLen) {
    var str = formatJson(obj);
    if (str.length > maxLen) {
        return str.substring(0, maxLen) + '…';
    }
    return str;
}
```

### 4. 时间格式化

所有时间统一显示为UTC格式：

```javascript
function formatUTC(timeStr) {
    if (!timeStr) return '-';
    // 假设后端返回格式：2024-01-01T12:00:00 或 2024-01-01 12:00:00
    return timeStr.replace('T', ' ').replace(/\.\d+Z?$/, '') + ' UTC';
}
```

### 5. 抽屉/弹窗实现

使用Layui的`layer.open`实现抽屉效果：

```javascript
function openDrawer(title, content) {
    layer.open({
        type: 1,
        title: title,
        content: content,
        area: ['520px', '100%'],
        offset: 'rt',
        anim: 0,
        closeBtn: 1,
        shadeClose: true
    });
}
```

---

## 五、后端API接口清单（需要实现）

### A1. 指标定义列表

```
GET /api/indicator/definitions
参数：
  - keyword (可选): 搜索关键字
  - category (可选): MOMENTUM/TREND/VOLATILITY
  - engine (可选): ta4j/custom
  - enabled (可选): 1/0
  - page (可选, 默认1)
  - size (可选, 默认50)

返回：
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [...],
    "total": 100,
    "current": 1,
    "size": 50
  }
}
```

### A2. 订阅管理

**列表查询**：
```
GET /api/indicator/subscriptions
参数：
  - userId (可选)
  - tradingPairId (可选)
  - symbolKeyword (可选): symbol模糊搜索
  - timeframe (可选)
  - indicatorCode (可选)
  - enabled (可选): 1/0
  - page, size
```

**创建订阅**：
```
POST /api/indicator/subscriptions
Body:
{
  "userId": 1,
  "tradingPairId": 1,
  "timeframe": "1h",
  "indicatorCode": "RSI",
  "indicatorVersion": "v1",
  "params": {"period": 14},
  "enabled": true
}

返回：409时表示冲突
```

**启停订阅**：
```
PATCH /api/indicator/subscriptions/{id}
Body:
{
  "enabled": true/false
}
```

### A3. 指标结果查询

**Latest**：
```
GET /api/indicator/values/latest
参数：
  - userId (可选)
  - tradingPairId (可选)
  - timeframe (可选)
  - indicatorCode (可选)
  - indicatorVersion (可选)
```

**History**：
```
GET /api/indicator/values
参数：
  - userId (可选)
  - tradingPairId (可选)
  - timeframe (可选)
  - indicatorCode (可选)
  - indicatorVersion (可选)
  - startTime (可选, ISO格式)
  - endTime (可选, ISO格式)
  - page, size
```

### A4. 计算日志查询

```
GET /api/indicator/calc-logs
参数：
  - userId (可选)
  - tradingPairId (可选)
  - timeframe (可选)
  - indicatorCode (可选)
  - status (可选): SUCCESS/FAILED/CONFLICT
  - startTime (可选)
  - endTime (可选)
  - page, size
```

### A5. 交易对列表（复用现有）

```
GET /api/trading-pairs
返回所有启用的交易对
```

---

## 六、验收标准（QA 可直接照跑）

### 核心闭环用例（必须通过）

**步骤**：

1. **创建订阅**：
   - 进入"指标订阅"页面
   - 点击"新建订阅"
   - 选择：BTC-USDT-SWAP + 1h + RSI(period=14)
   - 提交，看到成功提示

2. **等待触发**：
   - 等待下一根1h bar close（或后端触发测试事件）

3. **查看日志**：
   - 进入"计算日志"页面
   - 筛选：symbol包含BTC-USDT，timeframe=1h
   - 看到一条日志（SUCCESS/FAILED/CONFLICT）

4. **查看结果**（如果SUCCESS）：
   - 进入"指标结果"页面
   - 切换到Latest视图
   - 筛选：symbol包含BTC-USDT，timeframe=1h
   - 看到RSI最新值
   - 验证bar_time与日志一致

**通过标准**：步骤1~4能在UI完整完成，且值/日志能互相定位

---

### 停用验证（建议）

**步骤**：
1. 在"指标订阅"页面停用某个订阅
2. 等待下一根bar close
3. 验证"计算日志"中不再出现该订阅的日志

---

## 七、自测清单（开发完成前必须全绿）

- [ ] 所有页面菜单中都有"指标管理"菜单项
- [ ] UI中不出现1m周期（只在5m/15m/30m/1h/4h中选择）
- [ ] 所有时间为UTC且为bar_close_time格式
- [ ] Logs与Values能通过bar_time + indicator定位
- [ ] CONFLICT/FAILED只记录日志，不覆盖值（前端仅展示）
- [ ] JSON字段（params、extraValues、schema）展示无报错
- [ ] 409冲突有明确Toast提示
- [ ] 抽屉/弹窗正常打开关闭
- [ ] 分页功能正常
- [ ] 筛选功能正常

---

## 八、页面路由注册

在`PageController.java`中添加路由：

```java
@GetMapping("/indicator/dashboard")
public String indicatorDashboard() {
    return "redirect:/indicator/dashboard.html";
}

@GetMapping("/indicator/definitions")
public String indicatorDefinitions() {
    return "redirect:/indicator/definitions.html";
}

@GetMapping("/indicator/subscriptions")
public String indicatorSubscriptions() {
    return "redirect:/indicator/subscriptions.html";
}

@GetMapping("/indicator/values")
public String indicatorValues() {
    return "redirect:/indicator/values.html";
}

@GetMapping("/indicator/logs")
public String indicatorLogs() {
    return "redirect:/indicator/logs.html";
}
```

---

## 九、文件清单

**需要创建的文件**：

1. `src/main/resources/static/indicator/dashboard.html`
2. `src/main/resources/static/indicator/definitions.html`
3. `src/main/resources/static/indicator/subscriptions.html`
4. `src/main/resources/static/indicator/values.html`
5. `src/main/resources/static/indicator/logs.html`

**需要修改的文件**：

1. 所有现有HTML页面的菜单部分（添加"指标管理"菜单）
2. `src/main/java/com/qyl/v2trade/business/system/controller/PageController.java`（添加路由）

---

## 十、最终交付定义（Definition of Done）

一个任务算"Done"，必须满足：

1. ✅ 5个页面全部可访问
2. ✅ 菜单正确显示"指标管理"及子菜单
3. ✅ 所有页面不使用mock数据，真实调用后端API
4. ✅ 不引入任何新表或新后台逻辑
5. ✅ QA按闭环用例跑通并截图留档
6. ✅ 所有JSON字段正确展示
7. ✅ 错误处理完善（409、5xx等）

---

## 十一、参考原型

详细交互和样式参考：`docs/模块规划与任务/指标/indicator-prototype.html`

**关键差异**：
- 原型使用自定义CSS，实际使用Layui框架
- 保持功能一致，样式可适配Layui
- 交互逻辑（抽屉、弹窗、筛选）保持一致

---

> **注意**：
> 本任务完成后，系统应具备：
> **"指标模块已可验证、可审计、可演示"** 的工业级状态。
> 其余兼容性 / 安全性 / 补算 / 策略联动 **不在本轮范围内**。

