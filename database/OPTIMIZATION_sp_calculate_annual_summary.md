# sp_calculate_annual_summary 存储过程优化说明

**优化时间**: 2024-12-17
**版本**: v3（优化版）

## 优化目标

1. ✅ 每一年度只保留一笔汇总记录
2. ✅ 取当年最晚日期的资产、负债进行汇总
3. ✅ 多币种资产/负债统一转换到基准货币
4. ✅ 使用当年最晚日期的汇率进行货币转换
5. ✅ 提高性能，避免重复查询汇率

## 核心逻辑

### 1. 确定汇总日期

```sql
-- 找到当年所有资产和负债记录中的最晚日期
SELECT MAX(record_date) INTO v_summary_date
FROM (
    -- 资产记录
    SELECT ar.record_date FROM asset_records ar ...
    WHERE YEAR(ar.record_date) = p_year
    UNION
    -- 负债记录
    SELECT lr.record_date FROM liability_records lr ...
    WHERE YEAR(lr.record_date) = p_year
) AS all_dates
WHERE record_date <= CONCAT(p_year, '-12-31');
```

**说明**：
- 汇总日期 = MIN(当年最晚记录日期, 12月31日)
- 如果当年没有任何记录，则使用 12月31日

### 2. 汇率快照机制

使用临时表存储截止到汇总日期的最新汇率：

```sql
CREATE TEMPORARY TABLE temp_exchange_rates AS
SELECT currency, rate_to_usd
FROM (
    SELECT
        currency, rate_to_usd,
        ROW_NUMBER() OVER (PARTITION BY currency ORDER BY effective_date DESC) as rn
    FROM exchange_rates
    WHERE effective_date <= v_summary_date
        AND is_active = TRUE
) ranked
WHERE rn = 1;
```

**优势**：
- 避免每笔资产/负债记录都要查询汇率
- 确保所有转换使用同一时间点的汇率快照
- 提高查询性能（一次性获取所有汇率）

### 3. 货币转换公式

**统一转换公式**：
```
转换后金额 = 原币金额 × 原币对USD汇率 ÷ 基准货币对USD汇率
```

**示例**：
- 基准货币：USD
- 某资产：10,000 CNY
- CNY汇率（2024-12-31）：0.137
- 转换结果：10,000 × 0.137 ÷ 1 = 1,370 USD

**基准货币为非USD的情况**：
- 基准货币：CNY
- 某资产：1,000 USD
- CNY汇率（2024-12-31）：0.137（CNY对USD）
- 转换结果：1,000 × 1 ÷ 0.137 ≈ 7,299.27 CNY

### 4. 账户最新值获取

对每个资产/负债账户，取其在汇总日期之前的最新记录：

```sql
WHERE ar.record_date = (
    SELECT MAX(ar2.record_date)
    FROM asset_records ar2
    WHERE ar2.account_id = ar.account_id
        AND ar2.record_date <= v_summary_date
)
```

**说明**：
- 确保每个账户只取一笔记录（最接近汇总日期的记录）
- 即使某账户在当年有多笔记录，也只采用最新的那笔

## 数据库表约束

`annual_financial_summary` 表有唯一约束：

```sql
UNIQUE KEY `uk_family_year` (family_id, year)
```

**作用**：
- 确保每个家庭每年只有一笔汇总记录
- 重复调用存储过程会更新现有记录，而不是插入新记录

## 性能优化点

### 优化前
- 每笔资产/负债记录都要子查询汇率表
- 汇率查询可能重复数百次
- 效率：O(n × m)，其中 n=记录数，m=汇率表行数

### 优化后
- 使用临时表一次性获取所有汇率
- LEFT JOIN 关联汇率数据
- 效率：O(n + m)

**性能提升**：
- 假设 100 个账户，10 种货币
- 优化前：100 × 10 = 1000 次汇率查询
- 优化后：10 次汇率查询（一次性）
- **提升约 100 倍**

## 测试验证

### 测试用例 1：2024年汇总

```sql
CALL sp_calculate_annual_summary(1, 2024);

SELECT
    year, summary_date,
    total_assets, total_liabilities, net_worth
FROM annual_financial_summary
WHERE family_id = 1 AND year = 2024;
```

**预期结果**：
- summary_date = 2024-12-31（当年最晚日期）
- 所有资产和负债都已转换为基准货币
- 房产净值、非房产净值正确计算

### 测试用例 2：同比数据

```sql
CALL sp_calculate_annual_summary(1, 2023);
CALL sp_calculate_annual_summary(1, 2024);

SELECT
    year, net_worth,
    yoy_net_worth_change,
    yoy_net_worth_change_pct
FROM annual_financial_summary
WHERE family_id = 1 AND year IN (2023, 2024)
ORDER BY year;
```

**预期结果**：
- 2023年 yoy_* 字段可能为 NULL（如果没有2022年数据）
- 2024年 yoy_net_worth_change = 2024净资产 - 2023净资产
- 2024年 yoy_net_worth_change_pct = 变化额 / 2023净资产 × 100

## 使用示例

### Java Service 调用

```java
// AnnualFinancialSummaryService.java
entityManager.createNativeQuery("CALL sp_calculate_annual_summary(:familyId, :year)")
    .setParameter("familyId", familyId)
    .setParameter("year", year)
    .executeUpdate();
```

### 重新计算所有年份

```sql
-- 重新计算 2020-2024 年的汇总数据
CALL sp_calculate_annual_summary(1, 2020);
CALL sp_calculate_annual_summary(1, 2021);
CALL sp_calculate_annual_summary(1, 2022);
CALL sp_calculate_annual_summary(1, 2023);
CALL sp_calculate_annual_summary(1, 2024);
```

## 注意事项

1. **汇率数据依赖**：
   - 确保 `exchange_rates` 表有完整的历史汇率数据
   - 每年12月31日应有各币种的汇率快照
   - 如果某币种缺失汇率，默认使用 1.0（等同于USD）

2. **基准货币设置**：
   - 从 `user_preferences.base_currency` 读取
   - 如果未设置，默认使用 USD
   - 建议在创建家庭时设置基准货币

3. **历史数据重算**：
   - 修改汇率数据后，需要重新调用存储过程
   - 建议批量重算多年数据以更新同比字段

4. **临时表清理**：
   - 存储过程会自动清理临时表 `temp_exchange_rates`
   - 即使存储过程异常退出，临时表也会在会话结束时自动删除

## 相关文件

- **存储过程定义**: `database/03_stored_procedures.sql`
- **表结构**: `database/01_schema.sql` (annual_financial_summary 表)
- **汇率数据**: `exchange_rates` 表
- **Java Service**: `AnnualFinancialSummaryService.java:97`
