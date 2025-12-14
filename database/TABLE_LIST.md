# Database Tables Overview

## 总览
- **总表数**: 24个表 + 1个视图
- **存储过程**: 2个
- **字符集**: UTF8MB4
- **引擎**: InnoDB
- **Entity总数**: 22个（所有Entity都有对应表）

---

## 📊 数据表分类

### 1️⃣ 用户与家庭管理 (4个表)
| 表名 | 说明 | 关键字段 |
|------|------|---------|
| `users` | 用户表 | username, email, password_hash |
| `user_profiles` | 用户配置 | nickname, avatar, timezone |
| `user_preferences` | 用户偏好设置 | theme, language, base_currency |
| `families` | 家庭/组 | family_name, owner_user_id |

### 2️⃣ 资产管理 (4个表)
| 表名 | 说明 | 关键字段 |
|------|------|---------|
| `asset_type` | 资产类型定义 | type(CASH/STOCKS/RETIREMENT_FUND等), chinese_name, is_investment |
| `asset_accounts` | 资产账户 | account_name, asset_type_id, currency, tax_status |
| `asset_records` | 资产记录 | record_date, amount, quantity, unit_price |
| `investment_transactions` | 投资交易记录 | transaction_period, transaction_type(DEPOSIT/WITHDRAWAL), amount |

**资产类型 (8种)**:
- CASH (现金及现金等价物)
- STOCKS (股票及基金)
- RETIREMENT_FUND (退休基金)
- INSURANCE (保险)
- REAL_ESTATE (房地产)
- CRYPTOCURRENCY (数字货币)
- PRECIOUS_METALS (贵金属)
- OTHER (其他)

### 3️⃣ 负债管理 (3个表)
| 表名 | 说明 | 关键字段 |
|------|------|---------|
| `liability_type` | 负债类型定义 | type(MORTGAGE/AUTO_LOAN等), chinese_name, english_name |
| `liability_accounts` | 负债账户 | account_name, liability_type_id, interest_rate, monthly_payment |
| `liability_records` | 负债记录 | record_date, outstanding_balance, payment_amount |

**负债类型 (7种)**:
- MORTGAGE (房贷)
- AUTO_LOAN (车贷)
- CREDIT_CARD (信用卡)
- PERSONAL_LOAN (个人借款)
- STUDENT_LOAN (学生贷款)
- BUSINESS_LOAN (商业贷款)
- OTHER (其他)

### 4️⃣ 净资产分析 (3个表)
| 表名 | 说明 | 关键字段 |
|------|------|---------|
| `net_asset_categories` | 净资产分类 | code(REAL_ESTATE_NET等), name, display_order |
| `net_asset_category_asset_type_mappings` | 资产类型映射 | net_asset_category_id → asset_type |
| `net_asset_category_liability_type_mappings` | 负债类型映射 | net_asset_category_id → liability_type |

**净资产分类 (5种)**:
- REAL_ESTATE_NET (房地产净值)
- RETIREMENT_FUND_NET (退休基金净值)
- LIQUID_NET (流动资产净值)
- INVESTMENT_NET (投资净值)
- OTHER_NET (其他净值)

### 5️⃣ 支出管理 (5个表) 🆕
| 表名 | 说明 | 关键字段 |
|------|------|---------|
| `expense_categories_major` | 支出大类 | code(CHILDREN/FOOD/HOUSING等), icon, color |
| `expense_categories_minor` | 支出子分类 | major_category_id, name, is_default |
| `expense_records` | 支出记录 | expense_period(YYYY-MM), amount, expense_type |
| `expense_category_adjustment_config` 🆕 | 支出类别调整配置 | major_category_id, adjustment_type(ASSET/LIABILITY) |
| `annual_expense_summary` 🆕 | 年度支出汇总 | summary_year, base_expense_amount, actual_expense_amount |

**初始数据**:
- 10个大类：子女👶/衣👔/食🍜/住🏠/行🚗/保险🛡️/人情🎁/娱乐🎮/经营💼/其他📦
- 10个默认子分类（极简版，用户自行扩展）
- 2条调整配置：住房(房贷调整)、保险(资产调整)

### 6️⃣ 交易管理 (1个表)
| 表名 | 说明 | 关键字段 |
|------|------|---------|
| `transaction_categories` | 交易分类（有初始数据，功能未实现） | name, type(INCOME/EXPENSE), parent_id |

### 7️⃣ 预算管理 (1个表)
| 表名 | 说明 | 关键字段 |
|------|------|---------|
| `expense_budgets` | 支出年度预算 | family_id, budget_year, minor_category_id, budget_amount |

### 8️⃣ 系统数据 (5个表)
| 表名 | 说明 | 关键字段 |
|------|------|---------|
| `exchange_rates` | 汇率 | currency, rate_to_usd, effective_date |
| `annual_financial_summary` | 年度财务汇总 | year, summary_date, net_worth, total_assets |
| `annual_expense_summary` | 年度支出汇总 | summary_year, base_expense_amount, actual_expense_amount |
| `user_preferences` | 用户偏好设置 | base_currency, locale, timezone |
| `asset_liability_type_mappings` | 资产负债关联（暂未使用） | asset_type, liability_type |

---

## 📈 视图 (Views)

| 视图名 | 说明 | 关键字段 |
|--------|------|---------|
| `v_annual_financial_trend` | 年度财务趋势视图 | family_id, year, total_assets, total_liabilities, net_worth, yoy_changes |

---

## ⚙️ 存储过程

| 过程名 | 说明 | 参数 |
|--------|------|------|
| `sp_calculate_annual_summary` | 计算年度财务汇总 | p_family_id, p_year |
| `calculate_annual_expense_summary_v2` 🆕 | 计算年度支出汇总（USD版本） | p_family_id, p_summary_year |

**功能说明**:
- `sp_calculate_annual_summary`: 计算指定家庭、指定年份的年末财务快照（12月31日），包括净资产、总资产、总负债、各类资产/负债分项
- `calculate_annual_expense_summary_v2`: 计算年度支出汇总，自动进行USD货币转换，并根据配置调整资产/负债变化对实际支出的影响

---

## 🔗 核心关系

```
families (1) ─────┬─── (N) users ──── (1) user_preferences
                  │                └─── (1) user_profiles
                  ├─── (N) asset_accounts ──── (N) asset_records
                  │                       └─── (N) investment_transactions
                  ├─── (N) liability_accounts ── (N) liability_records
                  ├─── (N) expense_records
                  └─── (N) expense_budgets

asset_type (1) ──── (N) asset_accounts
liability_type (1) ── (N) liability_accounts
expense_categories_major (1) ── (N) expense_categories_minor ── (N) expense_records
expense_categories_minor (1) ── (N) expense_budgets
```

---

## 💡 设计特点

1. **类型化系统**: 使用 `asset_type` 和 `liability_type` 表替代分类表，支持更灵活的类型定义
2. **多货币支持**: USD为基准货币，资产/负债记录包含原始货币和基准货币金额
3. **历史追踪**: 资产/负债/支出均保留历史记录，支持趋势分析
4. **软删除**: 重要数据使用 `is_active` 字段标记删除，不物理删除
5. **唯一约束**: 防止重复数据（如同一期间同一账户多条记录）
6. **投资追踪**: 新增 `investment_transactions` 表，单独记录买入/卖出交易
7. **净资产分类**: 通过映射表将资产类型和负债类型映射到净资产分类，支持灵活的净资产分析

---

## 🔄 最近更新

**2025-12-13 下午**:
- ✅ 删除没有entity且无数据的空表：budgets, financial_goals, goal_progress_records, transactions
- ✅ 删除过时的Entity类：AssetCategory.java, LiabilityCategory.java
- ✅ 所有Entity与数据库表完全对应，无孤立类
- ✅ 数据库从28个表减少到24个表，更加精简

**2025-12-13 上午**:
- ✅ 完成从 category-based 到 type-based 系统的迁移
- ✅ 删除 `asset_categories` 和 `liability_categories` 表
- ✅ 新增 `asset_type` 和 `liability_type` 表
- ✅ 新增 `investment_transactions` 表用于投资交易记录
- ✅ 更新所有存储过程以使用新的 type 表
- ✅ 清理所有中间迁移脚本

---

**最后更新**: 2025-12-13 下午
