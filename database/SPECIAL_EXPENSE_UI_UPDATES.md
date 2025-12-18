# 前端UI特殊支出显示功能

## 更新时间
2025-12-17

## 功能概述
在年度汇总表中添加特殊支出（单笔 ≥ $10,000）的显示，包括：
- 特殊支出金额
- 特殊支出详情（显示子分类名称和金额）
- 橙色警告标记

## 后端更新

### 1. AnnualExpenseSummary实体类
**文件**: `backend/src/main/java/com/finance/app/model/AnnualExpenseSummary.java`

**新增字段**:
```java
/**
 * 特殊支出金额（单笔>=10000 USD）
 */
@Column(name = "special_expense_amount", precision = 18, scale = 2)
private BigDecimal specialExpenseAmount = BigDecimal.ZERO;

/**
 * 特殊支出详情 (JSON格式)
 */
@JdbcTypeCode(SqlTypes.JSON)
@Column(name = "special_expense_details", columnDefinition = "JSON")
private String specialExpenseDetails;
```

### 2. ExpenseAnalysisService服务
**文件**: `backend/src/main/java/com/finance/app/service/expense/ExpenseAnalysisService.java`

**修改方法**: `getAnnualSummaryTable()`

**新增返回字段**:
- `categoryData[code].specialExpense`: 各大类的特殊支出金额
- `categoryData[code].specialExpenseDetails`: 各大类的特殊支出详情JSON
- `total.specialExpense`: 总特殊支出金额

**示例返回数据**:
```json
{
  "categoryData": {
    "HOUSING": {
      "actualExpense": 48827.52,
      "baseExpense": 53282.68,
      "specialExpense": 23904.56,
      "specialExpenseDetails": "[{\"amount\": 23904.56, \"minorCategoryId\": 85, \"minorCategoryName\": \"房产税\"}]"
    }
  },
  "total": {
    "actualExpense": 115631.46,
    "baseExpense": 143861.33,
    "specialExpense": 54372.56
  }
}
```

## 前端更新

### 1. ExpenseAnnualTrend.vue组件
**文件**: `frontend/src/views/analysis/ExpenseAnnualTrend.vue`

### 显示效果

#### 各大类单元格
```html
<!-- 实际支出（基础支出） -->
$155,784.19 ($142,315.57)

<!-- 特殊支出提示（橙色） -->
⚠️ 特殊: $43,897.27
  学费: $20,345.00
  房产税: $23,552.27

<!-- 同比数据 -->
+16.40% (+9.23%)
```

#### 总计行
```html
$155,784.19
  ($142,315.57)

⚠️ 特殊: $43,897.27

+16.40% (+9.23%)
```

### 2. 新增功能函数

**parseSpecialExpenseDetails函数**:
```javascript
// 解析特殊支出详情JSON
const parseSpecialExpenseDetails = (detailsJson) => {
  if (!detailsJson) return []
  try {
    // detailsJson可能是字符串或已解析的对象
    const details = typeof detailsJson === 'string' ? JSON.parse(detailsJson) : detailsJson
    return Array.isArray(details) ? details : []
  } catch (error) {
    console.error('解析特殊支出详情失败:', error)
    return []
  }
}
```

### 3. 样式设计

**颜色方案**:
- 特殊支出标记: `text-orange-600` （橙色警告）
- 特殊支出详情: `text-gray-600` （灰色次要信息）
- 字体大小: `text-[10px]` 主标记, `text-[9px]` 详情

**图标**:
- ⚠️ 警告emoji表示特殊支出

## 使用示例

### 测试API端点
```bash
curl "http://localhost:8080/api/expenses/analysis/annual/summary-table?familyId=1&limit=3"
```

### 实际数据示例（2025年）

| 大类 | 实际支出 | 基础支出 | 特殊支出 | 特殊支出明细 |
|------|---------|---------|---------|------------|
| 子女 | $40,624.95 | $20,279.95 | $20,345.00 | 学费: $20,345.00 |
| 住 | $50,626.04 | $56,762.42 | $23,552.27 | 房产税: $23,552.27 |
| **总计** | **$155,784.19** | **$142,315.57** | **$43,897.27** | - |

### 历史特殊支出汇总

| 年份 | 总特殊支出 | 主要项目 |
|------|-----------|---------|
| 2018 | $628,840.92 | 首付 (住) |
| 2022 | $56,356.18 | 多项 |
| 2025 | $54,372.56 | 多项 |
| 2020 | $50,043.14 | 多项 |
| 2019 | $47,031.68 | 多项 |
| 2024 | $43,897.27 | 学费、房产税 |

## 说明文本更新

**原文本**:
```
- 实际支出 = 基础支出 + 资产/负债调整
```

**更新后**:
```
- 实际支出 = 基础支出 + 特殊支出 + 资产/负债调整
- ⚠️ 特殊支出：单笔 ≥ $10,000 的支出，显示橙色标记及明细
```

## 优势

1. **可视化清晰**: 橙色标记让大额支出一目了然
2. **信息完整**: 显示子分类名称，便于理解支出性质
3. **多笔支持**: 同一大类的多笔特殊支出都会列出
4. **货币转换**: 自动使用年份年末汇率换算显示
5. **响应式设计**: 适配不同屏幕尺寸

## 注意事项

1. 特殊支出详情采用极小字体（9px），仅在有值时显示
2. JSON解析容错处理，避免前端报错
3. 使用flexbox右对齐，保持表格整洁
4. 橙色标记不影响原有的红绿同比颜色
