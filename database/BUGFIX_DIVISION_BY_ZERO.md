# 修复：年度汇总表除以零错误

## 问题时间
2025-12-17

## 问题描述

**用户报告**: 无法查看"年度汇总表"，前端没有显示数据。

## 问题定位

### 1. API测试

测试年度汇总表API端点：

```bash
curl "http://localhost:8080/api/expenses/analysis/annual/summary-table?familyId=1&limit=5"
```

**返回错误**:
```json
{
  "success": false,
  "message": "查询年度汇总表失败: / by zero"
}
```

### 2. 数据验证

查询数据库发现"其他"大类（major_category_id=10）在2021-2023年**只有特殊支出，没有基础支出**：

```sql
SELECT summary_year, major_category_id, base_expense_amount, special_expense_amount
FROM annual_expense_summary
WHERE family_id = 1
  AND minor_category_id IS NULL
  AND base_expense_amount = 0;
```

结果：
- 2021年：base_expense_amount = 0, special_expense_amount = 15,700 USD
- 2022年：base_expense_amount = 0, special_expense_amount = 24,650 USD
- 2023年：base_expense_amount = 0, special_expense_amount = 37,418 USD

### 3. 根本原因

在 `ExpenseAnalysisService.java` 的 `getAnnualSummaryTable` 方法中，计算同比增长率时：

**问题代码** (lines 859-863):
```java
BigDecimal baseChange = currentCategorySummary.getBaseExpenseAmount()
        .subtract(previousCategorySummary.getBaseExpenseAmount());
BigDecimal baseChangePct = baseChange
        .divide(previousCategorySummary.getBaseExpenseAmount(), 4, BigDecimal.ROUND_HALF_UP)
        .multiply(new BigDecimal("100"));
```

当 `previousCategorySummary.getBaseExpenseAmount()` 为 **0** 时，触发 **ArithmeticException: / by zero**。

同样的问题也存在于总计行的计算中（lines 907-911）。

## 修复方案

在计算基础支出同比增长率之前，增加**除数为零检查**：

### 修复1：大类同比计算（lines 848-876）

```java
// 计算同比
if (previousCategorySummary != null &&
    previousCategorySummary.getActualExpenseAmount() != null &&
    previousCategorySummary.getActualExpenseAmount().compareTo(BigDecimal.ZERO) != 0) {

    BigDecimal actualChange = currentCategorySummary.getActualExpenseAmount()
            .subtract(previousCategorySummary.getActualExpenseAmount());
    BigDecimal actualChangePct = actualChange
            .divide(previousCategorySummary.getActualExpenseAmount(), 4, BigDecimal.ROUND_HALF_UP)
            .multiply(new BigDecimal("100"));

    categoryData.put("actualChangePct", actualChangePct);

    // 基础支出同比：检查除数是否为零 ← 新增
    if (previousCategorySummary.getBaseExpenseAmount() != null &&
        previousCategorySummary.getBaseExpenseAmount().compareTo(BigDecimal.ZERO) != 0) {
        BigDecimal baseChange = currentCategorySummary.getBaseExpenseAmount()
                .subtract(previousCategorySummary.getBaseExpenseAmount());
        BigDecimal baseChangePct = baseChange
                .divide(previousCategorySummary.getBaseExpenseAmount(), 4, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal("100"));
        categoryData.put("baseChangePct", baseChangePct);
    } else {
        categoryData.put("baseChangePct", null); // 除数为零时返回null
    }
} else {
    categoryData.put("actualChangePct", null);
    categoryData.put("baseChangePct", null);
}
```

### 修复2：总计同比计算（lines 903-930）

```java
if (previousTotalSummary != null &&
    previousTotalSummary.getActualExpenseAmount() != null &&
    previousTotalSummary.getActualExpenseAmount().compareTo(BigDecimal.ZERO) != 0) {

    BigDecimal actualChange = totalSummary.getActualExpenseAmount()
            .subtract(previousTotalSummary.getActualExpenseAmount());
    BigDecimal actualChangePct = actualChange
            .divide(previousTotalSummary.getActualExpenseAmount(), 4, BigDecimal.ROUND_HALF_UP)
            .multiply(new BigDecimal("100"));

    totalData.put("actualChangePct", actualChangePct);

    // 基础支出同比：检查除数是否为零 ← 新增
    if (previousTotalSummary.getBaseExpenseAmount() != null &&
        previousTotalSummary.getBaseExpenseAmount().compareTo(BigDecimal.ZERO) != 0) {
        BigDecimal baseChange = totalSummary.getBaseExpenseAmount()
                .subtract(previousTotalSummary.getBaseExpenseAmount());
        BigDecimal baseChangePct = baseChange
                .divide(previousTotalSummary.getBaseExpenseAmount(), 4, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal("100"));
        totalData.put("baseChangePct", baseChangePct);
    } else {
        totalData.put("baseChangePct", null); // 除数为零时返回null
    }
} else {
    totalData.put("actualChangePct", null);
    totalData.put("baseChangePct", null);
}
```

## 修复结果

重启后端服务后，API正常返回数据：

```bash
curl "http://localhost:8080/api/expenses/analysis/annual/summary-table?familyId=1&limit=3"
```

**成功响应**:
```json
{
  "success": true,
  "data": {
    "years": [2025, 2024, 2023],
    "categories": [...],
    "rows": [
      {
        "year": 2025,
        "total": {
          "baseExpense": 143861.33,
          "specialExpense": 54372.56,
          "actualExpense": 115631.46,
          "actualChangePct": -25.77,
          "baseChangePct": 1.09  ← 正常计算
        },
        "categoryData": {
          "OTHER": {
            "baseExpense": 0.00,
            "specialExpense": 37418.00,
            "actualChangePct": ...,
            "baseChangePct": null  ← 除数为零时返回null
          }
        }
      }
    ]
  }
}
```

### 验证"其他"大类数据

**2023年**（基础支出=0）:
- baseExpense: 0 USD
- specialExpense: 37,418 USD
- actualExpense: 37,418 USD
- baseChangePct: **null** （上一年基础支出为0，无法计算同比）

**2022年**（基础支出=0）:
- baseExpense: 0 USD
- specialExpense: 24,650 USD
- actualExpense: 24,650 USD
- baseChangePct: **null**

**2021年**（基础支出=0）:
- baseExpense: 0 USD
- specialExpense: 15,700 USD
- actualExpense: 15,700 USD
- baseChangePct: **null**

## 影响范围

这个bug影响了：
1. **年度汇总表API** (`/api/expenses/analysis/annual/summary-table`)
2. 所有**基础支出为0的大类**（例如"其他"大类只有特殊支出的年份）

修复后：
- 实际支出同比（actualChangePct）正常计算
- 基础支出同比（baseChangePct）在除数为零时返回 **null**
- 前端可以正确处理 null 值，不再抛出异常

## 前端显示

前端 `ExpenseAnnualTrend.vue` 已经有处理 null 值的逻辑：

```vue
<div v-if="row.total.actualChangePct !== null" class="text-[10px] md:text-xs">
  <span :class="getChangeColor(row.total.actualChangePct)" class="font-semibold">
    {{ formatPercent(row.total.actualChangePct) }}
  </span>
  <span class="text-blue-700">
    ({{ formatPercent(row.total.baseChangePct) }})
  </span>
</div>
<div v-else class="text-[10px] text-gray-400">基准年</div>
```

当 `baseChangePct` 为 null 时，`formatPercent` 函数返回 `-`，不会导致显示错误。

## 预防措施

1. **所有除法运算前检查除数**: 在进行 `BigDecimal.divide()` 之前，始终检查除数是否为零
2. **单元测试**: 添加测试用例覆盖"只有特殊支出的大类"场景
3. **边界条件**: 在设计时考虑特殊支出可能独立存在的情况

## 相关文件

- 修改文件: `backend/src/main/java/com/finance/app/service/expense/ExpenseAnalysisService.java` (lines 848-876, 903-930)
- API端点: `/api/expenses/analysis/annual/summary-table`
- 前端页面: `frontend/src/views/analysis/ExpenseAnnualTrend.vue`

## 技术要点

**BigDecimal除法安全模式**:
```java
// ❌ 不安全：可能抛出 ArithmeticException
BigDecimal result = a.divide(b, 4, BigDecimal.ROUND_HALF_UP);

// ✅ 安全：先检查除数
if (b != null && b.compareTo(BigDecimal.ZERO) != 0) {
    BigDecimal result = a.divide(b, 4, BigDecimal.ROUND_HALF_UP);
} else {
    // 返回null或其他默认值
}
```

**同比增长率计算逻辑**:
- 实际支出同比：基于 `actualExpenseAmount`（必然有值，因为=基础+特殊+调整）
- 基础支出同比：基于 `baseExpenseAmount`（可能为0，需要检查）

当基础支出为0但有特殊支出时：
- 实际支出同比 = 正常计算（基于总支出）
- 基础支出同比 = **null**（无法从0计算增长率）
