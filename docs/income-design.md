# 收入管理功能设计

## 概述

收入管理模块用于追踪家庭成员的各类收入来源，包括工资、奖金、投资收益、租金等。支持月度批量录入、多币种转换、关联资产账户，并与投资管理模块联动自动汇总投资收益。

## 核心设计理念

### 1. 分类体系

**10个预设大类**（不可修改）：
- 工资 💼 (Salary)
- 奖金 🎁 (Bonus)
- 投资收益 📈 (Investment)
- 租金 🏠 (Rental)
- 副业 💡 (SideHustle)
- 股票RSU 📊 (RSU)
- 退休基金贡献 🏦 (Retirement)
- 退税 💰 (TaxRefund)
- 礼金 🎀 (Gift)
- 其他 📦 (Other)

**30+预设小类**（不可修改）：
- 工资：基本工资、加班费、提成
- 奖金：年终奖、绩效奖金、签约奖金
- 投资收益：股票收益、分红、利息、数字货币收益
- 租金：住宅租金、商业租金
- 副业：自由职业、咨询、线上业务
- RSU：Vested股票、员工购股计划
- 退休基金：雇主匹配、雇主贡献、个人贡献
- 退税：联邦退税、州退税
- 礼金：婚礼礼金、生日礼金、节日礼金

### 2. 数据模型

#### 收入记录（income_records）

**关键字段**：
- `family_id` - 家庭ID
- `user_id` - 用户ID（家庭成员）
- `asset_account_id` - 关联的资产账户ID（可选，记录收入最终到账的账户）
- `major_category_id` - 收入大类ID
- `minor_category_id` - 收入小类ID（可选）
- `period` - 收入周期（YYYY-MM格式，如"2024-12"）
- `amount` - 金额（税后实际到账）
- `currency` - 币种（USD/CNY/EUR/GBP/JPY/AUD/CAD）
- `amount_usd` - 换算成USD的金额（自动计算）
- `description` - 备注

**唯一性约束**：
```sql
UNIQUE KEY uk_income (family_id, user_id, period, major_category_id, minor_category_id, currency)
```

同一家庭、同一用户、同一期间、同一分类、同一币种只能有一条记录。批量保存时如记录已存在则更新，否则创建。

#### 年度收入预算（income_budgets）

**关键字段**：
- `family_id` - 家庭ID
- `user_id` - 用户ID（NULL表示全家庭预算）
- `major_category_id` - 收入大类ID
- `minor_category_id` - 收入小类ID（可选）
- `year` - 年份
- `budgeted_amount` - 预算金额
- `currency` - 币种

**唯一性约束**：
```sql
UNIQUE KEY uk_budget (family_id, user_id, major_category_id, minor_category_id, year, currency)
```

### 3. 核心业务逻辑

#### 投资收益自动汇总

**特殊规则**：
- "投资收益"大类（Investment）的记录由系统自动生成，**禁止手动创建/更新/删除**
- 汇总来源：`investment_transactions` 表中的所有交易记录
- 汇总维度：按家庭、用户、月份汇总
- 汇总内容：
  - 股票收益（买卖差价）
  - 分红（Dividend交易类型）
  - 利息（Interest交易类型）
  - 数字货币收益（Crypto资产类型）

**实现逻辑**：
```java
// IncomeService.java
if ("Investment".equals(major.getName())) {
    throw new IllegalArgumentException("投资收益由系统自动计算，不能手动录入");
}
```

#### 资产账户关联

**关联意义**：
- 记录收入最终到账的资产账户（如"Chase Checking"）
- 可选字段，不强制关联
- 便于追踪资金流向，验证账户余额变化

**示例**：
- 工资 → Chase Checking账户
- RSU Vested → Fidelity Brokerage账户
- 租金收入 → Chase Savings账户

#### 多币种支持

**换算逻辑**：
- 所有金额都保存原币种金额（amount）和USD等值金额（amount_usd）
- USD金额自动计算：`amount_usd = amount * exchange_rate`
- 汇率来源：`ExchangeRateService.getExchangeRate(currency, date)`
- 汇率日期：使用收入期间的第一天（如"2024-12" → "2024-12-01"）

**支持币种**：USD, CNY, EUR, GBP, JPY, AUD, CAD

### 4. API设计

#### 月度批量录入

**场景**：用户每月录入当月所有收入

**接口**：`POST /api/incomes/records/batch`

**请求示例**：
```json
{
  "familyId": 1,
  "userId": 1,
  "period": "2024-12",
  "records": [
    {
      "majorCategoryId": 1,
      "minorCategoryId": 1,
      "assetAccountId": 5,
      "amount": 10000.00,
      "currency": "USD",
      "description": "基本工资"
    },
    {
      "majorCategoryId": 2,
      "minorCategoryId": 4,
      "amount": 5000.00,
      "currency": "USD",
      "description": "年终奖"
    }
  ]
}
```

**逻辑**：
1. 验证大类是否存在，排除"投资收益"类别
2. 检查是否已存在记录（唯一性约束）
3. 如存在则更新，否则创建
4. 自动计算USD金额（查询汇率）
5. 批量保存到数据库

#### 收入记录查询

**按期间查询**：`GET /api/incomes/records?familyId=1&period=2024-12`
- 返回指定家庭、指定月份的所有收入记录

**按期间范围查询**：`GET /api/incomes/records/range?familyId=1&startPeriod=2024-01&endPeriod=2024-12`
- 返回指定时间段内的所有收入记录
- 用于年度汇总、趋势分析

## 数据库表结构

### income_categories_major（收入大类表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| name | VARCHAR(100) | 英文名称（唯一） |
| chinese_name | VARCHAR(100) | 中文名称 |
| icon | VARCHAR(50) | 图标emoji |
| color | VARCHAR(20) | 显示颜色（Hex） |
| display_order | INT | 显示顺序 |
| is_active | BOOLEAN | 是否启用 |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |

### income_categories_minor（收入小类表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| major_category_id | BIGINT | 所属大类ID（外键） |
| name | VARCHAR(100) | 英文名称 |
| chinese_name | VARCHAR(100) | 中文名称 |
| is_active | BOOLEAN | 是否启用 |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |

**唯一约束**：`UNIQUE KEY uk_major_name (major_category_id, name)`

### income_records（收入记录表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| family_id | BIGINT | 家庭ID（外键） |
| user_id | BIGINT | 用户ID（外键） |
| asset_account_id | BIGINT | 关联的资产账户ID（外键，可选） |
| major_category_id | BIGINT | 收入大类ID（外键） |
| minor_category_id | BIGINT | 收入小类ID（外键，可选） |
| period | VARCHAR(7) | 周期（YYYY-MM） |
| amount | DECIMAL(18,2) | 金额（税后实际到账） |
| currency | VARCHAR(10) | 币种 |
| amount_usd | DECIMAL(18,2) | 换算成USD的金额 |
| description | TEXT | 备注 |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |

**唯一约束**：`UNIQUE KEY uk_income (family_id, user_id, period, major_category_id, minor_category_id, currency)`

**索引**：
- `INDEX idx_family_period (family_id, period)`
- `INDEX idx_user_period (user_id, period)`

### income_budgets（年度收入预算表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| family_id | BIGINT | 家庭ID（外键） |
| user_id | BIGINT | 用户ID（外键，NULL表示全家庭） |
| major_category_id | BIGINT | 收入大类ID（外键） |
| minor_category_id | BIGINT | 收入小类ID（外键，可选） |
| year | INT | 年份 |
| budgeted_amount | DECIMAL(18,2) | 预算金额 |
| currency | VARCHAR(10) | 币种 |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |

**唯一约束**：`UNIQUE KEY uk_budget (family_id, user_id, major_category_id, minor_category_id, year, currency)`

**索引**：`INDEX idx_family_year (family_id, year)`

## 前端实现要点

### 1. 月度录入界面

**布局**：
- 左侧：分类树（大类→小类）
- 右侧：录入表格（金额、币种、资产账户、备注）
- 顶部：期间选择器（YYYY-MM格式）、家庭成员选择器

**交互**：
- 点击分类树节点，右侧表格自动添加对应行
- 支持批量编辑（同时编辑多条记录）
- 自动保存（失焦/切换焦点时保存）
- 实时计算USD金额（显示汇率）

### 2. 收入汇总分析

**年度汇总**：
- 按大类汇总年度收入（柱状图）
- 按小类钻取查看明细（饼图）
- 月度趋势分析（折线图）

**预算执行**：
- 预算vs实际对比（进度条）
- 超预算/低于预算警示

### 3. 投资收益展示

**特殊处理**：
- 投资收益记录显示"（系统自动计算）"标签
- 禁用编辑/删除按钮
- 点击后跳转到投资管理模块查看详情

## 测试要点

### 1. 唯一性约束测试

**场景**：同一家庭、同一用户、同一期间、同一分类、同一币种
- 批量保存时应更新现有记录，而非报错
- 手动创建时应提示"记录已存在"

### 2. 投资收益保护测试

**场景**：尝试手动操作"投资收益"类别
- 创建：应拒绝并提示"投资收益由系统自动计算"
- 更新：应拒绝并提示"不能手动更新"
- 删除：应拒绝并提示"不能手动删除"

### 3. 汇率计算测试

**场景**：录入非USD币种的收入
- 应自动查询对应日期的汇率
- 如汇率不存在，应提示用户先添加汇率
- amount_usd应为 amount * exchange_rate

### 4. 资产账户关联测试

**场景**：关联的资产账户被删除
- 应仍然保留收入记录
- asset_account_id变为NULL
- 不影响收入统计

## 未来扩展

### 1. 收入预测

**基于历史数据**：
- 分析近12个月的收入趋势
- 预测下个月的收入范围
- 识别季节性波动（如年终奖）

### 2. 收入目标管理

**年度目标**：
- 设置年度收入目标（如"2025年总收入达到$200,000"）
- 按月追踪进度
- 预警偏离轨道的情况

### 3. 税务优化建议

**税收分析**：
- 识别应税收入（工资、奖金）
- 识别免税收入（部分退休基金贡献）
- 提供税务优化建议（如增加退休金贡献以降低应税收入）

### 4. 收入vs支出对比

**现金流分析**：
- 月度收入vs支出对比
- 净现金流趋势
- 识别入不敷出的月份

## 总结

收入管理模块的核心设计原则：
1. **预设分类**：10大类+30+小类，不可修改，确保数据一致性
2. **投资收益隔离**：自动汇总，禁止手动操作，避免数据冲突
3. **批量录入**：月度批量保存，提升录入效率
4. **多币种支持**：自动换算USD金额，便于汇总分析
5. **资产关联**：追踪资金流向，验证账户余额

这些设计确保收入数据的准确性、一致性和可追溯性，为后续的财务分析和决策提供可靠的数据基础。
