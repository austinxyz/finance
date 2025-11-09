# 数据库设计文档

## 概述

个人理财管理系统数据库设计，采用MySQL 8.0，字符集utf8mb4，支持多用户、多货币、时间序列数据追踪。

## 文件说明

- `schema.sql` - 完整数据库表结构定义
- `init_data.sql` - 初始化数据（系统预设类别、汇率等）
- `README.md` - 本文档

## 核心设计理念

### 1. 时间序列数据模式

资产和负债采用"账户+记录"的双表设计：
- **账户表**：存储账户基本信息（如账户名称、机构、利率等）
- **记录表**：存储时间序列数据点（每个日期一条记录）

**优势：**
- 支持历史趋势分析
- 支持任意时间点查询
- 数据不会因更新而丢失历史信息

### 2. 多货币支持

- 每条记录保存原始货币和金额
- 同时保存转换为基准货币的金额
- 汇率表记录历史汇率
- 用户可设置个人基准货币

### 3. 分类层级设计

- 资产/负债/交易均支持分类管理
- 系统预设类别 + 用户自定义类别
- 使用`type`字段标记大类，`name`支持细分

## 数据表说明

### 用户相关 (3张表)

#### users - 用户表
核心字段：
- `username` - 用户名（唯一）
- `email` - 邮箱（唯一）
- `password_hash` - 密码哈希（BCrypt）
- `is_active` - 账户激活状态

#### user_preferences - 用户偏好
个性化设置：
- `base_currency` - 基准货币（默认CNY）
- `locale` - 语言区域
- `timezone` - 时区
- `theme` - 界面主题（light/dark）
- `fiscal_year_start_month` - 财年起始月

### 资产管理 (3张表)

#### asset_categories - 资产类别
预设类型：
- `CASH` - 现金（银行存款、货币基金）
- `STOCKS` - 股票（国内股票、海外股票、基金）
- `RETIREMENT_FUND` - 退休基金（401k、IRA、养老保险）
- `INSURANCE` - 保险（人寿保险、年金保险）
- `REAL_ESTATE` - 房产（自住、投资）
- `CRYPTOCURRENCY` - 数字货币（比特币、以太坊）

#### asset_accounts - 资产账户
存储具体账户信息：
- `account_name` - 账户名称
- `account_number` - 账号
- `institution` - 金融机构
- `currency` - 货币类型
- `is_active` - 是否活跃

#### asset_records - 资产记录（时间序列）
关键字段：
- `record_date` - 记录日期（唯一约束：account_id + record_date）
- `amount` - 原币金额
- `quantity` - 数量（用于股票、基金）
- `unit_price` - 单价
- `amount_in_base_currency` - 基准货币金额
- `attachment_url` - 附件链接（截图、凭证）

### 负债管理 (3张表)

#### liability_categories - 负债类别
预设类型：
- `MORTGAGE` - 房贷
- `AUTO_LOAN` - 车贷
- `CREDIT_CARD` - 信用卡
- `PERSONAL_LOAN` - 个人贷款
- `STUDENT_LOAN` - 学生贷款
- `OTHER` - 其他负债

#### liability_accounts - 负债账户
扩展字段：
- `interest_rate` - 利率(%)
- `original_amount` - 原始借款金额
- `start_date` / `end_date` - 借款期限
- `monthly_payment` - 月供

#### liability_records - 负债记录（时间序列）
关键字段：
- `outstanding_balance` - 未偿余额
- `payment_amount` - 本期还款
- `principal_payment` - 本金还款
- `interest_payment` - 利息还款

### 财务目标 (2张表)

#### financial_goals - 财务目标
目标类型：
- `SAVING` - 储蓄目标
- `INVESTMENT` - 投资目标
- `DEBT_PAYOFF` - 还债目标
- `RETIREMENT` - 退休规划
- `PURCHASE` - 购买计划
- `OTHER` - 其他

状态流转：
`NOT_STARTED` → `IN_PROGRESS` → `COMPLETED` / `CANCELLED`

#### goal_progress_records - 目标进度
时间序列记录目标完成情况

### 交易记录 (2张表)

#### transaction_categories - 交易类别
- 收入类别（INCOME）：工资、奖金、投资收益、租金等
- 支出类别（EXPENSE）：餐饮、购物、交通、住房等
- 支持父子分类（parent_id）

#### transactions - 交易记录
交易类型：
- `INCOME` - 收入
- `EXPENSE` - 支出
- `TRANSFER` - 转账（账户间转移）

### 辅助表

#### budgets - 预算表
支持月度/季度/年度预算设置

#### exchange_rates - 汇率表
存储历史汇率数据，支持多货币转换

## 视图定义

### v_latest_asset_values
最新资产值视图，关联账户和最新记录

### v_latest_liability_values
最新负债值视图，关联账户和最新记录

### v_user_net_worth
用户净资产汇总视图：
- `total_assets` - 总资产
- `total_liabilities` - 总负债
- `net_worth` - 净资产（总资产 - 总负债）

## 索引策略

### 主要索引

1. **时间序列查询优化**
   - `idx_user_date` - 用户+日期组合索引
   - `idx_account_date` - 账户+日期组合索引

2. **分类查询优化**
   - `idx_user_type` - 用户+类型组合索引
   - `idx_user_category` - 用户+类别组合索引

3. **唯一性约束**
   - `uk_account_date` - 账户+日期唯一（防止重复记录）
   - `uk_user_name` - 用户+名称唯一（防止重复类别）

## 数据完整性

### 外键约束
- 所有子表通过外键关联用户表
- 级联删除策略：删除用户时删除所有相关数据
- RESTRICT策略：删除类别时如果有关联账户则禁止删除

### 数据验证
- 金额字段使用DECIMAL(18,2)，精确存储
- 汇率使用DECIMAL(12,6)，高精度
- 百分比使用DECIMAL(5,2)（支持0-999.99%）
- 日期字段使用DATE类型，时间戳使用TIMESTAMP

## 使用指南

### 1. 初始化数据库

```sql
-- 创建数据库和表结构
mysql -u root -p < schema.sql

-- 导入初始数据
mysql -u root -p finance < init_data.sql
```

### 2. 常见查询示例

#### 查询用户当前净资产
```sql
SELECT * FROM v_user_net_worth WHERE user_id = 1;
```

#### 查询某用户所有资产最新值
```sql
SELECT * FROM v_latest_asset_values WHERE user_id = 1;
```

#### 查询某资产的历史趋势
```sql
SELECT
    record_date,
    amount,
    amount_in_base_currency
FROM asset_records
WHERE account_id = 1
ORDER BY record_date DESC
LIMIT 30;
```

#### 计算月度资产增长
```sql
SELECT
    DATE_FORMAT(record_date, '%Y-%m') AS month,
    SUM(amount_in_base_currency) AS total_assets
FROM asset_records
WHERE user_id = 1
GROUP BY DATE_FORMAT(record_date, '%Y-%m')
ORDER BY month DESC;
```

#### 查询资产配置占比
```sql
SELECT
    c.name AS category_name,
    c.type AS category_type,
    SUM(r.amount_in_base_currency) AS total_amount,
    ROUND(SUM(r.amount_in_base_currency) /
          (SELECT SUM(amount_in_base_currency)
           FROM v_latest_asset_values
           WHERE user_id = 1) * 100, 2) AS percentage
FROM v_latest_asset_values r
JOIN asset_categories c ON r.category_id = c.id
WHERE r.user_id = 1
GROUP BY c.id, c.name, c.type
ORDER BY total_amount DESC;
```

## 性能优化建议

1. **分区表**：对于数据量大的时间序列表，考虑按月/年分区
2. **归档策略**：定期归档超过N年的历史数据
3. **缓存策略**：最新值查询可以使用Redis缓存
4. **读写分离**：查询分析可以使用只读副本

## 安全考虑

1. **密码存储**：使用BCrypt或Argon2加密
2. **敏感数据加密**：账号、金额等敏感字段考虑数据库级加密
3. **SQL注入防护**：使用参数化查询
4. **访问控制**：确保用户只能访问自己的数据（WHERE user_id = ?）

## 扩展性

### 未来可能的扩展

1. **多用户家庭账本**：添加家庭组表，支持多用户共享数据
2. **自动同步**：添加第三方账户授权表
3. **提醒通知**：添加提醒规则和通知历史表
4. **审计日志**：添加操作日志表，追踪所有数据变更
5. **附件管理**：添加专门的附件表，支持多文件上传
