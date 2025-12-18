# 修复：只有特殊支出的大类未被计入汇总

## 问题时间
2025-12-17

## 问题描述

**用户报告**: Austin family在2021、2022、2023年的"其他"大类有大额支出（例如2021年的100,000 CNY），这些支出超过了10,000 USD的特殊支出阈值，但在年度汇总表中没有显示。

## 问题定位

### 1. 数据验证

查询2021年的"其他"大类支出：

```sql
SELECT id, expense_period, amount, currency, description
FROM expense_records
WHERE family_id = 1
  AND expense_year = 2021
  AND major_category_id = 10;  -- 其他大类
```

结果：
- **id=915**: 100,000 CNY (2021-12)
- 按照2021年末汇率 (0.157)，等于 **15,700 USD**
- **超过10,000 USD阈值**，应该被识别为特殊支出

### 2. 汇总表检查

查询2021年的annual_expense_summary：

```sql
SELECT summary_year, major_category_id, ecmaj.name AS major_category_name,
       base_expense_amount, special_expense_amount, special_expense_details
FROM annual_expense_summary aes
LEFT JOIN expense_categories_major ecmaj ON aes.major_category_id = ecmaj.id
WHERE family_id = 1
  AND summary_year = 2021
  AND major_category_id != 0
  AND minor_category_id IS NULL
ORDER BY major_category_id;
```

结果：
- **没有** major_category_id = 10 (其他) 的记录
- 总计显示特殊支出为 **46,993.80 USD** (应该是62,693.80 USD)

### 3. 根本原因

存储过程 `calculate_annual_expense_summary_v3` 的逻辑缺陷：

**Part 1.2** (lines 167-225):
- 计算基础支出时**排除**特殊支出（>=10,000 USD）
- 为每个有基础支出的大类创建annual_expense_summary记录

**Part 1.5.2** (lines 281-309, 修复前):
- 使用`UPDATE`语句将特殊支出添加到**已存在的**记录中
- **问题**: 如果大类只有特殊支出，Part 1.2不会创建记录，Part 1.5.2的UPDATE失败

**对于"其他"大类**:
- 2021年唯一的支出是15,700 USD（特殊支出）
- Part 1.2: 被排除，**没有创建记录**
- Part 1.5.2: UPDATE不存在的记录，**失败**，数据丢失

## 修复方案

在Part 1.5.2中，在UPDATE之前先INSERT缺失的记录：

```sql
-- 1.5.2 大类级别的特殊支出汇总并更新到annual_expense_summary
-- 首先为那些只有特殊支出没有基础支出的大类创建记录
INSERT INTO annual_expense_summary (
    family_id, user_id, summary_year,
    major_category_id, minor_category_id,
    base_expense_amount, actual_expense_amount, currency
)
SELECT
    p_family_id,
    (SELECT id FROM users WHERE family_id = p_family_id LIMIT 1),
    p_summary_year,
    special_summary.major_category_id,
    NULL,
    0,  -- 基础支出为0
    0,  -- 实际支出先设为0，稍后UPDATE时会更新
    'USD'
FROM (
    SELECT major_category_id
    FROM temp_special_expenses_minor
    GROUP BY major_category_id
) AS special_summary
WHERE NOT EXISTS (
    SELECT 1 FROM annual_expense_summary
    WHERE family_id = p_family_id
      AND summary_year = p_summary_year
      AND major_category_id = special_summary.major_category_id
      AND minor_category_id IS NULL
);

-- 然后更新所有大类的特殊支出数据（原有逻辑）
UPDATE annual_expense_summary aes
JOIN (
    SELECT
        major_category_id,
        SUM(special_amount) AS total_special_amount,
        JSON_ARRAYAGG(
            JSON_OBJECT(
                'minorCategoryId', minor_category_id,
                'minorCategoryName', minor_category_name,
                'amount', special_amount
            )
        ) AS special_details
    FROM temp_special_expenses_minor
    GROUP BY major_category_id
) AS special_summary ON aes.major_category_id = special_summary.major_category_id
SET
    aes.special_expense_amount = special_summary.total_special_amount,
    aes.special_expense_details = special_summary.special_details,
    aes.actual_expense_amount = aes.base_expense_amount
                               + special_summary.total_special_amount
                               + COALESCE(aes.asset_adjustment, 0)
                               + COALESCE(aes.liability_adjustment, 0)
WHERE aes.family_id = p_family_id
  AND aes.summary_year = p_summary_year
  AND aes.minor_category_id IS NULL;
```

## 修复结果

重新运行存储过程后，"其他"大类的特殊支出已正确计入：

### 2021年数据

| 大类 | 基础支出 | 特殊支出 | 调整值 | 实际支出 |
|------|---------|---------|--------|---------|
| 住 (Housing) | 64,878.08 | 36,993.80 | -38,460.00 | 63,411.88 |
| 医疗 (Medical) | 1,541.40 | 10,000.00 | 0.00 | 11,541.40 |
| **其他 (Other)** | **0.00** | **15,700.00** | **0.00** | **15,700.00** | ← 新增
| **总计** | 130,625.59 | **62,693.80** | -38,200.00 | 154,599.39 |

**特殊支出详情**:
```json
{
  "amount": 15700.00,
  "minorCategoryId": 80,
  "minorCategoryName": "未分类"
}
```

**修复前**: 特殊支出总计 = 46,993.80 USD
**修复后**: 特殊支出总计 = **62,693.80 USD** ✓

### 其他年份

**2022年**:
- 其他大类特殊支出: **24,650 USD**
- 总特殊支出: 81,006.18 USD

**2023年**:
- 其他大类特殊支出: **37,418 USD**
- 总特殊支出: 73,092.05 USD

## 验证公式

**2021年公式验证**:
```
基础支出: 130,625.59 USD
特殊支出: +62,693.80 USD
  - 住: 36,993.80 USD
  - 医疗: 10,000.00 USD
  - 其他: 15,700.00 USD (100,000 CNY * 0.157)
调整值: -38,200.00 USD
  - 资产调整: +260.00 USD
  - 负债调整: -38,460.00 USD
──────────────────
实际支出: 154,599.39 USD ✓
```

## 影响范围

这个bug影响了所有**只有特殊支出没有基础支出的大类**：

1. **"其他"(Other)大类**: 2021、2022、2023年都只有特殊支出
2. 可能还有其他大类在某些年份也受影响

## 预防措施

1. 存储过程设计时应考虑极端情况（只有特殊支出的大类）
2. 添加单元测试覆盖这种边界情况
3. 在INSERT/UPDATE逻辑中优先使用"先INSERT后UPDATE"或"INSERT ... ON DUPLICATE KEY UPDATE"模式

## 相关文件

- 修改文件: `database/03_stored_procedures.sql` (Part 1.5.2)
- 测试数据: expense_records id=915 (2021年100,000 CNY)

## 技术要点

**修复策略**: 先INSERT缺失记录，再UPDATE所有记录

**NOT EXISTS子查询**:
```sql
WHERE NOT EXISTS (
    SELECT 1 FROM annual_expense_summary
    WHERE family_id = p_family_id
      AND summary_year = p_summary_year
      AND major_category_id = special_summary.major_category_id
      AND minor_category_id IS NULL
);
```

确保只为真正缺失的大类创建记录，避免重复。
