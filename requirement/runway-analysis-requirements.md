# Runway Analysis — 需求说明

> **目的**：回答"如果停止工作，我的钱能撑多久？"—— 将系统中已有的资产和支出数据合并为可操作的财务跑道估算。

---

## 一、核心概念

**Runway（资金跑道）** = 流动资产总额 ÷ 月均支出 = 可维持月数

- **流动资产**：随时可变现的资产（现金、股票、经纪账户、加密货币等），不含房产、退休账户、保险
- **月均支出**：基于系统中实际录入的支出记录，取最近 N 个月的平均值（默认 6 个月）

---

## 二、数据来源

### 流动资产
- 数据来源：`asset_records` 表（每个账户取最新快照）
- 默认纳入类型：CASH、STOCKS、CRYPTOCURRENCY、PRECIOUS_METALS
- 默认排除：REAL_ESTATE（流动性差）、RETIREMENT_FUND（提前取出有罚款）、INSURANCE
- 用户可通过界面勾选/取消**特定账户**（账户级别，非类型级别）
- 所有金额取 USD 换算值（`amount_usd` 字段，已有汇率转换）

### 月均支出
- 数据来源：`expense_records` 表（实际录入支出，非预算）
- 取最近 N 个完整历月的总额，求均值（默认 N=6，用户可调整为 3 或 12）
- 按大类（major category）汇总，用于展示支出结构
- 用户可对各大类手动输入调整金额（正负值），用于情景模拟

---

## 三、Runway 计算逻辑

```
月均支出（基准）= sum(最近6个完整月的支出) / 实际有数据的月数
调整后月均支出  = 月均支出 + sum(各大类调整额)
流动资产总额    = sum(已纳入账户最新快照的 amount_usd)
Runway（月）   = floor(流动资产总额 / 调整后月均支出)
预计耗尽日期   = 当前月份 + Runway 月数
```

### 情景模拟（客户端计算，无需额外 API）

| 情景 | 乘数（默认） | 公式 |
|------|-------------|------|
| 基准 | 1.0× | 调整后月均支出 × 1.0 |
| 乐观（节省开支） | 0.8× | 调整后月均支出 × 0.8 |
| 悲观（额外开销） | 1.2× | 调整后月均支出 × 1.2 |

用户可自定义乘数。

---

## 四、API 设计

### 4.1 Runway 计算接口

```
GET /api/runway/analysis
  ?familyId={id}
  &months={6}                          # 支出回溯月数，默认6
  &includedTypes={CASH,STOCKS,...}     # 流动资产类型，默认4类
```

#### 响应结构

```json
{
  "success": true,
  "data": {
    "liquidTotal": 640250.00,
    "monthlyBurn": 17000.00,
    "runwayMonths": 37,
    "depletionDate": "2029-04",
    "expenseMonthsUsed": 6,
    "latestSnapshotDate": "2026-02-28",
    "assetDataMissing": false,
    "expenseDataWarning": false,
    "accountBreakdown": [
      { "id": 1, "accountName": "Chase Checking", "accountType": "CASH", "usdValue": 170000.00 },
      { "id": 2, "accountName": "Fidelity Brokerage", "accountType": "STOCKS", "usdValue": 360000.00 }
    ],
    "expenseBreakdown": {
      "HOUSING": 9000.00,
      "FOOD": 3500.00,
      "TRANSPORTATION": 1200.00,
      "OTHER": 3300.00
    }
  }
}
```

### 4.2 报告持久化接口

```
POST /api/runway/reports
  Body: { "familyId": 1, "snapshotJson": "..." }
  # 保存当前快照；报告名自动生成为 runway-YYYY-MM-DD-report
  # 同日重复保存追加序号：runway-YYYY-MM-DD-report-2

GET /api/runway/reports?familyId={id}
  # 返回该家庭所有报告摘要（按保存时间倒序）

GET /api/runway/reports/{id}?familyId={id}
  # 返回单个报告详情（含完整 snapshotJson）

DELETE /api/runway/reports/{id}
  # 删除报告（需同家庭权限）
```

#### 快照 JSON 结构

```json
{
  "version": "1",
  "settings": {
    "lookbackMonths": 6,
    "optimisticMultiplier": 0.8,
    "pessimisticMultiplier": 1.2
  },
  "excludedAccountIds": [3, 7],
  "expenseAdjustments": { "HOUSING": -500, "FOOD": 200 },
  "snapshot": {
    "liquidTotal": 640250.00,
    "monthlyBurn": 17000.00,
    "runwayMonths": 37,
    "depletionDate": "2029-04",
    "accountBreakdown": [...],
    "expenseBreakdown": { "HOUSING": 9000.00, "FOOD": 3500.00 }
  }
}
```

---

## 五、界面功能

### 5.1 Runway 分析页（/analysis/runway）

#### 汇总区
- 流动资产总额（大字展示）
- 月均支出
- Runway 月数 + 预计耗尽日期

#### 情景对比
| 情景 | 月支出 | Runway | 耗尽日期 |
|------|--------|--------|---------|
| 乐观（×0.8） | $13,600 | 47 个月 | 2030-02 |
| 基准（×1.0） | $17,000 | 37 个月 | 2029-04 |
| 悲观（×1.2） | $20,400 | 31 个月 | 2028-10 |

#### 资产明细
- 各账户列表：账户名、类型、金额、排除勾选框
- 排除后实时重算 Runway

#### 支出结构
- 各大类平均月支出（降序），支持手动输入调整额（正负），实时重算

#### 控制参数
- 支出回溯月数选择（3 / 6 / 12 个月）
- 情景乘数调整（乐观 / 悲观）

#### 操作按钮
- **保存报告**：将当前快照（含排除账户、支出调整、计算结果）持久化到后端数据库
- **导出 PDF**：使用 html2canvas 截图渲染区域，生成多页 A4 PDF 下载（支持中文字符）
- **历史报告**：跳转到报告列表页

#### 数据质量提示
- ⚠️ 若最新资产快照超过 30 天：提示数据可能不准确
- ⚠️ 若支出数据不足 3 个月：提示估算基础有限

### 5.2 历史跑道报告页（/analysis/runway-reports）

- 列表展示所有已保存报告：报告名称、保存时间
- 支持点击进入详情查看
- 支持删除报告

### 5.3 报告详情页（/analysis/runway-reports/:id）

- 只读展示保存时的历史快照（不重新拉取当前数据）
- 布局与分析页一致：汇总卡片、情景对比、账户明细、支出结构
- 已排除账户以灰色标注
- 支持导出 PDF（包含家庭名称）

---

## 六、数据库

| 表 | 说明 |
|----|------|
| `asset_records` | 现有，提供流动资产数据 |
| `expense_records` | 现有，提供支出数据 |
| `runway_reports` | 新建，存储历史快照报告 |

### runway_reports 表结构

```sql
CREATE TABLE runway_reports (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  family_id    BIGINT NOT NULL,
  report_name  VARCHAR(255) NOT NULL,
  saved_at     DATETIME NOT NULL,
  snapshot_json TEXT NOT NULL,
  INDEX idx_runway_reports_family_id (family_id)
);
```

---

## 七、约束与边界条件

| 情况 | 处理方式 |
|------|---------|
| 无流动资产记录 | `liquidTotal = 0`，显示警告 |
| 无支出记录 | `monthlyBurn = null`，`runwayMonths = null`，显示错误提示 |
| 支出数据不足请求月数 | 使用实际有数据的月数，通过 `expenseMonthsUsed` 字段告知前端 |
| 月均支出为 0 | 返回错误，不显示 Runway |
| 多币种资产 | 使用已存储的 `amount_usd` 字段，不做实时汇率查询 |
| 跨家庭访问报告 | 后端抛出 `UnauthorizedException`，前端显示错误 |
| 同日多次保存 | 报告名追加序号（-2, -3…） |

---

## 八、不在范围内

- 收入模拟 / 再就业收入预测
- 资产变现税费计算
- 退休账户提前取出罚款建议
- 从此页面编辑支出记录

---

*状态：已实现*
