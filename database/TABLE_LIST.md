# Database Tables Overview

## 总览
- **总表数**: 26个表 + 4个视图
- **存储过程**: 2个
- **字符集**: UTF8MB4
- **引擎**: InnoDB

---

## 📊 数据表分类

### 1️⃣ 用户与家庭管理 (4个表)
| 表名 | 说明 | 关键字段 |
|------|------|---------|
| `users` | 用户表 | username, email, password_hash |
| `user_profiles` | 用户配置 | nickname, avatar, timezone |
| `user_preferences` | 用户偏好设置 | theme, language, base_currency |
| `families` | 家庭/组 | family_name, owner_user_id |

### 2️⃣ 资产管理 (3个表)
| 表名 | 说明 | 关键字段 |
|------|------|---------|
| `asset_categories` | 资产分类 | code(CHECKING/SAVINGS/BROKERAGE等) |
| `asset_accounts` | 资产账户 | account_name, initial_balance, currency |
| `asset_records` | 资产记录 | record_date, amount, amount_in_base_currency |

### 3️⃣ 负债管理 (3个表)
| 表名 | 说明 | 关键字段 |
|------|------|---------|
| `liability_categories` | 负债分类 | code(MORTGAGE/CREDIT_CARD等) |
| `liability_accounts` | 负债账户 | account_name, initial_balance, interest_rate |
| `liability_records` | 负债记录 | record_date, outstanding_balance |

### 4️⃣ 净资产分析 (4个表)
| 表名 | 说明 | 关键字段 |
|------|------|---------|
| `net_asset_categories` | 净资产分类 | category_name, display_order |
| `net_asset_category_asset_type_mappings` | 资产类型映射 | net_asset_category_id → asset_category_id |
| `net_asset_category_liability_type_mappings` | 负债类型映射 | net_asset_category_id → liability_category_id |
| `asset_liability_type_mappings` | 资产负债关联 | asset_type_code ↔ liability_type_code |

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

### 6️⃣ 交易管理 (2个表)
| 表名 | 说明 | 关键字段 |
|------|------|---------|
| `transaction_categories` | 交易分类 | category_name, transaction_type(INCOME/EXPENSE) |
| `transactions` | 交易记录 | transaction_date, amount, type, category_id |

### 7️⃣ 预算与目标 (3个表)
| 表名 | 说明 | 关键字段 |
|------|------|---------|
| `budgets` | 预算 | budget_month, category_id, planned_amount |
| `financial_goals` | 财务目标 | goal_name, target_amount, target_date |
| `goal_progress_records` | 目标进度 | goal_id, current_amount, record_date |

### 8️⃣ 系统数据 (2个表)
| 表名 | 说明 | 关键字段 |
|------|------|---------|
| `exchange_rates` | 汇率 | currency, rate_to_usd, effective_date |
| `annual_financial_summary` | 年度汇总 | year, summary_date, net_worth, total_assets |

---

## 📈 视图 (Views)

| 视图名 | 说明 |
|--------|------|
| `v_annual_financial_trend` | 年度财务趋势视图 |
| `v_latest_asset_values` | 最新资产价值视图 |
| `v_latest_liability_values` | 最新负债价值视图 |
| `v_user_net_worth` | 用户净资产视图 |

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
families (1) ─────┬─── (N) users
                  ├─── (N) asset_accounts ──── (N) asset_records
                  ├─── (N) liability_accounts ── (N) liability_records
                  ├─── (N) expense_records
                  ├─── (N) transactions
                  ├─── (N) budgets
                  └─── (N) financial_goals

asset_categories (1) ──── (N) asset_accounts
liability_categories (1) ── (N) liability_accounts
expense_categories_major (1) ── (N) expense_categories_minor ── (N) expense_records
transaction_categories (1) ──── (N) transactions
```

---

## 💡 设计特点

1. **多货币支持**: USD为基准货币，所有金额自动转换存储 `amount_in_base_currency`
2. **历史追踪**: 资产/负债/支出均保留历史记录，支持趋势分析
3. **软删除**: 重要数据使用 `is_active` 字段标记删除，不物理删除
4. **唯一约束**: 防止重复数据（如同一期间同一账户多条记录）
5. **极简默认**: 分类数据极简初始化，用户按需自定义扩展

---

**最后更新**: 2025-12-12
