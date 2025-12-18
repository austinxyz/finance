# 年度汇总表新增汇总行功能

## 更新时间
2025-12-17

## 功能概述
在年度汇总表中新增两个汇总行，以更清晰地展示特殊支出和调整值的全局视图：
1. **特殊支出汇总行** - 显示各年度所有大类的特殊支出总额和详细明细
2. **调整值汇总行** - 显示各年度资产和负债调整的汇总

## 表格结构

### 原有结构
```
大类行（住、食、行、衣、医、子女、娱乐、杂项）
总计行
```

### 更新后结构
```
大类行（住、食、行、衣、医、子女、娱乐、杂项）
总计行（💰）
特殊支出汇总行（⚠️）← 新增
调整值汇总行（⚖️）← 新增
```

## 后端更新

### ExpenseAnalysisService.java

在 `getAnnualSummaryTable()` 方法的总计数据（totalData）中新增字段：

```java
// 总计数据
Map<String, Object> totalData = new HashMap<>();
if (totalSummary != null) {
    totalData.put("actualExpense", totalSummary.getActualExpenseAmount());
    totalData.put("baseExpense", totalSummary.getBaseExpenseAmount());
    totalData.put("specialExpense", totalSummary.getSpecialExpenseAmount());
    totalData.put("assetAdjustment", totalSummary.getAssetAdjustment());  // ← 新增
    totalData.put("liabilityAdjustment", totalSummary.getLiabilityAdjustment());  // ← 新增
    totalData.put("adjustmentDetails", totalSummary.getAdjustmentDetails());  // ← 新增
    // ...
}
```

### API 返回示例

```json
{
  "data": {
    "rows": [
      {
        "year": 2025,
        "total": {
          "actualExpense": 115631.46,
          "baseExpense": 143861.33,
          "specialExpense": 54372.56,
          "assetAdjustment": 54242.71,
          "liabilityAdjustment": 28359.72,
          "adjustmentDetails": "[...]"
        },
        "categoryData": {
          "CHILDREN": {
            "actualExpense": 40624.95,
            "baseExpense": 20279.95,
            "specialExpense": 20345.00,
            "specialExpenseDetails": "[{\"amount\": 20345.00, \"minorCategoryId\": 82, \"minorCategoryName\": \"学费\"}]"
          }
        }
      }
    ]
  }
}
```

## 前端更新

### ExpenseAnnualTrend.vue

#### 1. 特殊支出汇总行（橙色背景）

```vue
<tr class="bg-orange-50 border-t border-orange-200">
  <td class="px-2 md:px-4 py-2 md:py-3 text-gray-900 sticky left-0 bg-orange-50 z-10">
    <div class="flex items-center gap-2">
      <span class="text-lg">⚠️</span>
      <span class="font-semibold">特殊支出</span>
    </div>
  </td>
  <td v-for="row in summaryTableData.rows" :key="`special-${row.year}`">
    <!-- 特殊支出金额 -->
    <div class="font-semibold text-orange-700">
      $54,372.56
    </div>
    <!-- 特殊支出详情（子分类汇总） -->
    <div class="text-[9px] text-gray-600">
      <div>学费: $20,345.00</div>
      <div>房产税: $23,552.27</div>
      <div>装修: $10,475.29</div>
    </div>
  </td>
</tr>
```

#### 2. 调整值汇总行（紫色背景）

```vue
<tr class="bg-purple-50 border-t border-purple-200">
  <td class="px-2 md:px-4 py-2 md:py-3 text-gray-900 sticky left-0 bg-purple-50 z-10">
    <div class="flex items-center gap-2">
      <span class="text-lg">⚖️</span>
      <span class="font-semibold">调整值</span>
    </div>
  </td>
  <td v-for="row in summaryTableData.rows" :key="`adjustment-${row.year}`">
    <!-- 总调整值（资产 + 负债） -->
    <div class="font-semibold text-red-600">
      $82,602.43
    </div>
    <!-- 调整值详情（按资产/负债类型显示） -->
    <div class="text-[9px] text-gray-600">
      <div>📉 房贷: $28,359.72</div>
      <div>📈 保险: $54,242.71</div>
    </div>
  </td>
</tr>
```

**重要更新**: 调整值详情现在显示具体的资产/负债类型，而不是泛泛的"资产调整"和"负债调整"：
- 资产类型（8种）：现金类、数字货币、保险、其他、贵金属、房地产、退休基金、股票投资
- 负债类型（6种）：车贷、信用卡、房贷、其他负债、个人贷款、学生贷款

**技术实现**:
1. 存储过程Part 4.5汇总各大类的adjustment_details到总计记录
2. 前端parseAdjustmentDetails()函数解析JSON并映射类型码到中文名称
3. 每个调整项显示对应的emoji图标（📈资产/📉负债/🏠房产购买）

### 新增辅助函数

#### getSpecialExpenseDetailsSummary(row)
汇总某年度所有大类的特殊支出详情

```javascript
const getSpecialExpenseDetailsSummary = (row) => {
  const allDetails = []

  // 遍历所有大类
  if (row.categoryData && summaryTableData.value.categories) {
    summaryTableData.value.categories.forEach(category => {
      const categoryCode = category.code
      const categoryDataItem = row.categoryData[categoryCode]

      if (categoryDataItem && categoryDataItem.specialExpenseDetails) {
        const details = parseSpecialExpenseDetails(categoryDataItem.specialExpenseDetails)
        details.forEach(detail => {
          allDetails.push({
            categoryName: detail.minorCategoryName,
            amount: detail.amount
          })
        })
      }
    })
  }

  return allDetails
}
```

**返回示例**:
```javascript
[
  { categoryName: "学费", amount: 20345.00 },
  { categoryName: "房产税", amount: 23552.27 },
  { categoryName: "装修", amount: 10475.29 }
]
```

#### getTotalAdjustment(assetAdjustment, liabilityAdjustment)
计算总调整值（资产调整 + 负债调整）

```javascript
const getTotalAdjustment = (assetAdjustment, liabilityAdjustment) => {
  const asset = assetAdjustment || 0
  const liability = liabilityAdjustment || 0
  return Number(asset) + Number(liability)
}
```

#### parseAdjustmentDetails(adjustmentDetailsJson)
解析调整值详情JSON并转换为可读格式

```javascript
const parseAdjustmentDetails = (adjustmentDetailsJson) => {
  if (!adjustmentDetailsJson) return []
  try {
    const details = typeof adjustmentDetailsJson === 'string' ? JSON.parse(adjustmentDetailsJson) : adjustmentDetailsJson
    if (!Array.isArray(details)) return []

    return details.map(detail => {
      const type = detail.type
      const code = detail.code
      const amount = detail.amount

      let typeName = ''
      let icon = ''

      if (type === 'ASSET') {
        typeName = assetTypeNames[code] || code
        icon = '📈'
      } else if (type === 'LIABILITY') {
        typeName = liabilityTypeNames[code] || code
        icon = '📉'
      } else if (type === 'PROPERTY_PURCHASE') {
        typeName = '房产购买'
        icon = '🏠'
      }

      return { type, code, typeName, icon, amount }
    })
  } catch (error) {
    console.error('解析调整值详情失败:', error)
    return []
  }
}
```

**类型映射**:
```javascript
const assetTypeNames = {
  'CASH': '现金类',
  'CRYPTOCURRENCY': '数字货币',
  'INSURANCE': '保险',
  'OTHER': '其他',
  'PRECIOUS_METALS': '贵金属',
  'REAL_ESTATE': '房地产',
  'RETIREMENT_FUND': '退休基金',
  'STOCKS': '股票投资'
}

const liabilityTypeNames = {
  'AUTO_LOAN': '车贷',
  'CREDIT_CARD': '信用卡',
  'MORTGAGE': '房贷',
  'OTHER': '其他负债',
  'PERSONAL_LOAN': '个人贷款',
  'STUDENT_LOAN': '学生贷款'
}
```

#### getTotalAdjustmentColor(assetAdjustment, liabilityAdjustment)
根据调整值总额返回颜色类

```javascript
const getTotalAdjustmentColor = (assetAdjustment, liabilityAdjustment) => {
  const total = getTotalAdjustment(assetAdjustment, liabilityAdjustment)
  if (total === 0) return 'text-gray-600'
  // 调整值为正表示支出增加（红色），为负表示支出减少（绿色）
  return total > 0 ? 'text-red-600' : 'text-green-600'
}
```

## 视觉设计

### 配色方案
- **总计行**: 蓝色背景 `bg-blue-50`，蓝色文字
- **特殊支出行**: 橙色背景 `bg-orange-50`，橙色标题文字 `text-orange-700`
- **调整值行**: 紫色背景 `bg-purple-50`，根据正负值显示红/绿色

### 图标选择
- **总计**: 💰 (金钱袋)
- **特殊支出**: ⚠️ (警告标志)
- **调整值**: ⚖️ (天平)

### 字体大小
- 主金额: `font-semibold`（常规大小）
- 详情行: `text-[9px]`（极小字体）

## 实际案例：2025年

| 行类型 | 金额 | 详细明细 |
|--------|------|----------|
| **总计** | $115,631.46 | 基础支出: $143,861.33 |
| **特殊支出** | $54,372.56 | 学费: $20,345.00<br>房产税: $23,552.27<br>装修: $10,475.29 |
| **调整值** | $82,602.43 | 资产: $54,242.71<br>负债: $28,359.72 |

### 公式验证
```
实际支出 = 基础支出 + 特殊支出 + 调整值
$115,631.46 = $143,861.33 + $54,372.56 + $82,602.43  ✗ (待验证)

实际计算：
基础支出: $143,861.33
特殊支出: $54,372.56
资产调整: $54,242.71
负债调整: $28,359.72
总调整: $82,602.43

实际支出应为: 143,861.33 + 54,372.56 - 82,602.43 = 115,631.46 ✓
```

**注意**: 调整值在实际支出计算中是减法（负债调整为正值时减少支出）

## 优势

1. **信息层次清晰**: 三层汇总（总计、特殊支出、调整值）各司其职
2. **视觉区分明显**: 不同背景色和图标便于识别
3. **详情可追溯**: 每行都显示明细，便于审计
4. **响应式设计**: 小字体详情在移动端也能正常显示
5. **数据完整性**: 所有组成部分一目了然

## 注意事项

1. 特殊支出详情从各大类数据中汇总，不重复计算
2. 调整值包括资产和负债两部分，需要分别显示
3. 颜色语义：红色=支出增加（不好），绿色=支出减少（好）
4. 极小字体（9px）仅用于详情行，保持界面整洁
