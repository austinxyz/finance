# 投资管理模块数据库迁移说明

## 概述

本次数据库迁移为个人理财系统添加了投资管理功能，包括：

1. 为资产分类表添加投资标识字段
2. 创建投资交易记录表
3. 更新现有数据标记投资类资产

## 迁移文件

- **文件名**: `05_investment_management.sql`
- **版本**: V005
- **创建日期**: 2024-12-13

## 数据库变更详情

### 1. asset_categories 表修改

#### 新增字段

| 字段名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `is_investment` | TINYINT(1) | 0 | 是否为投资类账户 |

#### 新增索引

- `idx_is_investment`: 用于优化按投资类型查询的性能

#### 数据更新

以下资产类型被标记为投资类（`is_investment = 1`）：

- **STOCKS** (股票投资)
- **RETIREMENT_FUND** (退休基金)
- **CRYPTOCURRENCY** (数字货币)

其他资产类型保持默认值 0（非投资类）：

- CASH (现金)
- INSURANCE (保险)
- REAL_ESTATE (房产)
- PRECIOUS_METALS (贵金属)
- OTHER (其他)

### 2. investment_transactions 表创建

#### 表结构

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 主键ID |
| `account_id` | BIGINT | NOT NULL, FOREIGN KEY | 资产账户ID，关联asset_accounts表 |
| `transaction_period` | VARCHAR(7) | NOT NULL | 交易期间，格式：YYYY-MM（如：2024-01） |
| `transaction_type` | ENUM | NOT NULL | 交易类型：DEPOSIT(投入), WITHDRAWAL(取出) |
| `amount` | DECIMAL(15,2) | NOT NULL | 交易金额（正数） |
| `description` | VARCHAR(500) | NULL | 交易说明 |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

#### 索引说明

1. **PRIMARY KEY** (`id`): 主键索引
2. **UNIQUE KEY** `uk_account_period_type` (`account_id`, `transaction_period`, `transaction_type`):
   - 确保同一账户、同一期间、同一交易类型只能有一条记录
3. **INDEX** `idx_account_period` (`account_id`, `transaction_period`):
   - 优化按账户和期间查询（最常用查询模式）
4. **INDEX** `idx_period` (`transaction_period`):
   - 优化批量录入页面按期间查询
5. **INDEX** `idx_transaction_type` (`transaction_type`):
   - 优化按交易类型查询

#### 外键约束

- `investment_transactions_ibfk_1`:
  - 关联: `account_id` → `asset_accounts.id`
  - 删除策略: ON DELETE CASCADE
  - 说明: 账户删除时，相关交易记录自动删除

#### 字段详细说明

**transaction_type 枚举值**:

- `DEPOSIT`: 投入（买入、追加投资）
- `WITHDRAWAL`: 取出（卖出、提取资金）

**amount 字段**:

- 存储正数
- 交易方向由 `transaction_type` 字段决定
- 精度: DECIMAL(15,2)，支持最大999,999,999,999,999.99

**transaction_period 格式**:

- 格式: `YYYY-MM`
- 示例: `2024-01`, `2024-12`
- 用途: 按月汇总投资交易数据

## 数据完整性

### 业务规则验证（应用层）

数据完整性验证在应用层面进行，确保：

1. 只有投资类账户（`is_investment = 1`）才能创建投资交易记录
2. 验证逻辑：
   - 在创建/更新投资交易前，查询账户对应的资产分类
   - 检查 `asset_categories.is_investment` 字段是否为 1
   - 如果不是投资类账户，拒绝操作并返回错误信息

### 外键约束

- 确保每笔交易记录都关联到有效的资产账户
- 账户删除时，相关交易记录自动级联删除

## 验证查询

执行以下查询验证迁移结果：

```sql
-- 1. 查看所有投资类资产分类
SELECT id, name, type, is_investment
FROM asset_categories
WHERE is_investment = 1
ORDER BY type, name;

-- 2. 验证 investment_transactions 表结构
SHOW CREATE TABLE investment_transactions;

-- 3. 验证索引
SHOW INDEX FROM investment_transactions;

-- 4. 查看投资类账户数量
SELECT
    ac.type,
    ac.name as category_name,
    COUNT(aa.id) as account_count
FROM asset_categories ac
LEFT JOIN asset_accounts aa ON ac.id = aa.category_id
WHERE ac.is_investment = 1
GROUP BY ac.type, ac.name
ORDER BY ac.type;
```

## 迁移结果

### 投资类资产分类（is_investment = 1）

根据验证查询，以下资产分类已成功标记为投资类：

**股票类 (STOCKS)**: 6个分类
- 股票
- 国内股票
- 海外股票
- 基金
- ETF基金
- 债券

**退休基金 (RETIREMENT_FUND)**: 4个分类
- 退休基金
- 401k
- IRA
- 养老保险

**数字货币 (CRYPTOCURRENCY)**: 4个分类
- 数字货币
- 比特币
- 以太坊
- 稳定币

**总计**: 14个投资类资产分类

### 非投资类资产（is_investment = 0）

- **CASH** (现金): 7个分类
- **INSURANCE** (保险): 3个分类
- **REAL_ESTATE** (房产): 5个分类
- **PRECIOUS_METALS** (贵金属): 2个分类
- **OTHER** (其他): 9个分类

## 回滚方案

如需回滚此次迁移，执行以下SQL：

```sql
-- 1. 删除 investment_transactions 表
DROP TABLE IF EXISTS `investment_transactions`;

-- 2. 删除 is_investment 字段的索引
ALTER TABLE `asset_categories` DROP INDEX `idx_is_investment`;

-- 3. 删除 is_investment 字段
ALTER TABLE `asset_categories` DROP COLUMN `is_investment`;
```

⚠️ **警告**: 回滚将永久删除所有投资交易记录数据，请谨慎操作！

## 后续步骤

数据库迁移完成后，需要进行以下后端开发工作：

1. **Model层**:
   - 创建 `InvestmentTransaction.java` 实体类
   - 更新 `AssetCategory.java`，添加 `isInvestment` 字段

2. **Repository层**:
   - 创建 `InvestmentTransactionRepository.java`

3. **Service层**:
   - 创建 `InvestmentTransactionService.java`
   - 创建 `InvestmentAnalysisService.java`

4. **Controller层**:
   - 创建 `InvestmentTransactionController.java`
   - 创建 `InvestmentAnalysisController.java`

5. **DTO层**:
   - 创建各种数据传输对象

详见需求文档第7章"开发任务拆分"。

## 注意事项

1. **幂等性**: 迁移脚本支持重复执行，不会出错
2. **性能优化**: 已添加必要的索引，支持高效查询
3. **数据安全**: 外键约束确保数据完整性
4. **扩展性**: 表结构预留了description字段，支持未来功能扩展

## 相关文档

- 投资管理需求文档: `requirement/投资管理需求.md`
- 数据库Schema: `database/01_schema.sql`
- 数据库初始数据: `database/02_initial_data.sql`
