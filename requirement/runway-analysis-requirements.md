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
- 默认纳入类型：CASH、STOCK、BROKERAGE、CRYPTO、DIGITAL_CURRENCY
- 默认排除：REAL_ESTATE（流动性差）、RETIREMENT / 401K（提前取出有罚款）、INSURANCE
- 用户可通过界面勾选/取消特定资产类型
- 所有金额取 USD 换算值（`amount_usd` 字段，已有汇率转换）

### 月均支出
- 数据来源：`expense_records` 表（实际录入支出，非预算）
- 取最近 N 个完整历历月的总额，求均值（默认 N=6，用户可调整为 3 或 12）
- 按大类（major category）汇总，用于展示支出结构

---

## 三、Runway 计算逻辑

```
月均支出 = sum(最近6个完整月的支出) / 实际有数据的月数
流动资产总额 = sum(各流动账户最新快照的 amount_usd)
Runway（月）= floor(流动资产总额 / 月均支出)
预计耗尽日期 = 当前月份 + Runway 月数
```

### 情景模拟（客户端计算，无需额外 API）

| 情景 | 乘数（默认） | 公式 |
|------|-------------|------|
| 基准 | 1.0× | 月均支出 × 1.0 |
| 乐观（节省开支） | 0.8× | 月均支出 × 0.8 |
| 悲观（额外开销） | 1.2× | 月均支出 × 1.2 |

用户可自定义乘数。

---

## 四、API 设计

```
GET /api/runway/analysis
  ?familyId={id}
  &months={6}                          # 支出回溯月数，默认6
  &includedTypes={CASH,STOCK,...}      # 流动资产类型，默认5类
```

### 响应结构

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
      { "accountName": "Chase Checking", "accountType": "CASH", "usdValue": 170000.00 },
      { "accountName": "Fidelity Brokerage", "accountType": "STOCK", "usdValue": 360000.00 }
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

---

## 五、界面功能

### 汇总区
- 流动资产总额（大字展示）
- 月均支出
- Runway 月数 + 预计耗尽日期

### 情景对比
| 情景 | 月支出 | Runway | 耗尽日期 |
|------|--------|--------|---------|
| 乐观（×0.8） | $13,600 | 47 个月 | 2030-02 |
| 基准（×1.0） | $17,000 | 37 个月 | 2029-04 |
| 悲观（×1.2） | $20,400 | 31 个月 | 2028-10 |

### 资产明细
- 各账户列表：账户名、类型、金额
- 资产类型勾选框（控制纳入哪些类型）

### 支出结构
- 各大类平均月支出表格（降序）

### 控制参数
- 支出回溯月数选择（3 / 6 / 12 个月）
- 情景乘数调整

### 数据质量提示
- ⚠️ 若最新资产快照超过 30 天：提示数据可能不准确
- ⚠️ 若支出数据不足 3 个月：提示估算基础有限

---

## 六、约束与边界条件

| 情况 | 处理方式 |
|------|---------|
| 无流动资产记录 | `liquidTotal = 0`，显示警告 |
| 无支出记录 | `monthlyBurn = null`，`runwayMonths = null`，显示错误提示 |
| 支出数据不足请求月数 | 使用实际有数据的月数，通过 `expenseMonthsUsed` 字段告知前端 |
| 月均支出为 0 | 返回错误，不显示 Runway |
| 多币种资产 | 使用已存储的 `amount_usd` 字段，不做实时汇率查询 |

---

## 七、不在范围内

- 收入模拟 / 再就业收入预测
- 资产变现税费计算
- 退休账户提前取出罚款建议
- 从此页面编辑支出记录

---

*数据来源：系统已有的 asset_records 和 expense_records 表，无需新建数据库表*
*状态：待实现*
