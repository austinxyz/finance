# Database Files

本目录包含Finance应用的数据库相关文件。

## 文件说明

### 1. `01_schema.sql` - 数据库表结构
完整的数据库schema定义，包含所有表结构、索引、视图等。

**包含的表**：
- 用户相关：`users`, `user_profiles`, `user_preferences`, `families`
- 资产管理：`asset_categories`, `asset_accounts`, `asset_records`
- 负债管理：`liability_categories`, `liability_accounts`, `liability_records`
- 净资产：`net_asset_categories`, `net_asset_category_*_mappings`
- 支出管理：`expense_categories_major`, `expense_categories_minor`, `expense_records`
- 交易：`transactions`, `transaction_categories`
- 预算：`budgets`, `financial_goals`, `goal_progress_records`
- 汇率：`exchange_rates`
- 年度汇总：`annual_financial_summary`
- 视图：`v_annual_financial_trend`, `v_latest_asset_values`, `v_latest_liability_values`, `v_user_net_worth`

### 2. `02_initial_data.sql` - 初始化数据
系统必需的初始化参考数据。

**包含的数据**：
- 资产分类（Asset Categories）
- 负债分类（Liability Categories）
- 净资产分类及映射（Net Asset Categories & Mappings）
- 交易分类（Transaction Categories）
- 支出分类（Expense Categories - 10个大类 + 10个默认子分类）

### 3. `03_stored_procedures.sql` - 存储过程
数据库存储过程定义。

**包含的存储过程**：
- `sp_calculate_annual_summary` - 计算年度财务汇总

## 其他SQL文件位置

### Flyway Migration Files
位置：`backend/src/main/resources/db/migration/`

这些文件由Flyway框架管理，用于数据库版本控制：
- `V001__create_expense_tables.sql` - 创建支出管理表
- `V002__insert_expense_initial_data.sql` - 支出分类初始化数据（极简版）

**注意**：Flyway migration文件一旦执行不应修改，新的变更应创建新的migration文件。

## 使用说明

### 初始化数据库
```bash
# 1. 创建表结构
mysql -h <host> -P <port> -u <user> -p<password> finance < 01_schema.sql

# 2. 导入初始数据
mysql -h <host> -P <port> -u <user> -p<password> finance < 02_initial_data.sql

# 3. 创建存储过程
mysql -h <host> -P <port> -u <user> -p<password> finance < 03_stored_procedures.sql
```

### 更新数据库
应用启动时会自动执行Flyway migration文件。

## 数据库架构演进

### 主要模块
1. **用户与家庭** - 多用户、多家庭支持
2. **资产管理** - 资产账户、记录、分类
3. **负债管理** - 负债账户、记录、分类
4. **支出管理** - 两级分类（大类/子分类）+ 月度记录
5. **净资产分析** - 净资产分类及类型映射
6. **交易管理** - 收支交易记录
7. **预算与目标** - 预算管理、财务目标追踪
8. **年度汇总** - 年度财务数据快照

### 设计原则
- **多货币支持** - USD为基准货币，CNY等其他货币自动转换
- **历史追踪** - 所有记录保留历史快照
- **数据完整性** - 外键约束确保引用完整性
- **极简默认** - 分类默认数据极简，用户按需扩展

## 维护

### 导出最新Schema
```bash
mysqldump -h <host> -P <port> -u <user> -p<password> \
  --no-data --skip-triggers finance > 01_schema.sql
```

### 导出存储过程
```bash
mysqldump -h <host> -P <port> -u <user> -p<password> \
  --no-create-info --no-data --routines --no-create-db finance > 03_stored_procedures.sql
```

---

**最后更新**: 2025-12-10
**数据库版本**: MySQL 8.0+
**字符集**: UTF8MB4
