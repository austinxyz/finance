# 年度趋势图汇总表新增调整值列

## 更新时间
2025-12-17

## 功能概述
在"年度趋势图"Tab的右侧年度汇总表中新增"调整值"列，完整展示支出构成公式：

**公式**: 基础支出 + 特殊支出 - 调整值 = 实际支出

## 表格结构

### 更新前
```
年份 | 基础支出 | 特殊支出 | 实际支出 | 实际同比
```

### 更新后
```
年份 | 基础支出 | 特殊支出 | 调整值 | 实际支出 | 实际同比
```

## 后端更新

### ExpenseAnalysisService.java

**文件**: `backend/src/main/java/com/finance/app/service/expense/ExpenseAnalysisService.java`

**修改方法**: `getAnnualExpenseTrend()`

**新增返回字段**:
- `assetAdjustment`: 资产调整值
- `liabilityAdjustment`: 负债调整值
- `totalAdjustment`: 总调整值（资产 + 负债）

**修改代码** (lines 571-599):
```java
// 转换货币
BigDecimal baseExpense = summary.getBaseExpenseAmount();
BigDecimal specialExpense = summary.getSpecialExpenseAmount();
BigDecimal assetAdjustment = summary.getAssetAdjustment();       // ← 新增
BigDecimal liabilityAdjustment = summary.getLiabilityAdjustment(); // ← 新增
BigDecimal actualExpense = summary.getActualExpenseAmount();

if (!currency.equals(summary.getCurrency())) {
    Map<String, BigDecimal> rates = exchangeRatesByYear.get(summary.getSummaryYear());
    baseExpense = convertCurrency(baseExpense, summary.getCurrency(), currency, rates);
    specialExpense = convertCurrency(specialExpense, summary.getCurrency(), currency, rates);
    assetAdjustment = convertCurrency(assetAdjustment, summary.getCurrency(), currency, rates);       // ← 新增
    liabilityAdjustment = convertCurrency(liabilityAdjustment, summary.getCurrency(), currency, rates); // ← 新增
    actualExpense = convertCurrency(actualExpense, summary.getCurrency(), currency, rates);
}

// 计算总调整值（资产调整 + 负债调整）
BigDecimal totalAdjustment = (assetAdjustment != null ? assetAdjustment : BigDecimal.ZERO)
        .add(liabilityAdjustment != null ? liabilityAdjustment : BigDecimal.ZERO);

// 基本信息
yearData.put("year", summary.getSummaryYear());
yearData.put("baseExpense", baseExpense);
yearData.put("specialExpense", specialExpense != null ? specialExpense : BigDecimal.ZERO);
yearData.put("assetAdjustment", assetAdjustment != null ? assetAdjustment : BigDecimal.ZERO);       // ← 新增
yearData.put("liabilityAdjustment", liabilityAdjustment != null ? liabilityAdjustment : BigDecimal.ZERO); // ← 新增
yearData.put("totalAdjustment", totalAdjustment);  // ← 新增
yearData.put("actualExpense", actualExpense);
yearData.put("currency", currency);
```

**API返回示例**:
```json
[
  {
    "year": 2025,
    "baseExpense": 143861.33,
    "specialExpense": 54372.56,
    "assetAdjustment": 54242.71,
    "liabilityAdjustment": 28359.72,
    "totalAdjustment": 82602.43,
    "actualExpense": 115631.46,
    "currency": "USD"
  },
  {
    "year": 2024,
    "baseExpense": 142315.57,
    "specialExpense": 43897.27,
    "assetAdjustment": 23925.85,
    "liabilityAdjustment": 6502.80,
    "totalAdjustment": 30428.65,
    "actualExpense": 155784.19,
    "currency": "USD"
  }
]
```

**公式验证** (2025年):
```
基础支出 + 特殊支出 - 调整值 = 实际支出
143,861.33 + 54,372.56 - 82,602.43 = 115,631.46 ✓
```

## 前端更新

### ExpenseAnnualTrend.vue

**文件**: `frontend/src/views/analysis/ExpenseAnnualTrend.vue`

### 1. 计算属性更新

**修改**: `convertedTrendData` (lines 405-416)

新增调整值字段的货币转换：

```javascript
const convertedTrendData = computed(() => {
  return trendData.value.map(item => ({
    ...item,
    baseExpense: convertCurrency(item.baseExpense, item.year),
    specialExpense: convertCurrency(item.specialExpense || 0, item.year),
    assetAdjustment: convertCurrency(item.assetAdjustment || 0, item.year),       // ← 新增
    liabilityAdjustment: convertCurrency(item.liabilityAdjustment || 0, item.year), // ← 新增
    actualExpense: convertCurrency(item.actualExpense, item.year),
    yoyBaseChange: item.yoyBaseChange ? convertCurrency(item.yoyBaseChange, item.year) : null,
    yoyActualChange: item.yoyActualChange ? convertCurrency(item.yoyActualChange, item.year) : null
  }))
})
```

### 2. 表格更新

**表头** (lines 110-118):
```vue
<thead class="bg-gray-50 border-b border-gray-200 sticky top-0">
  <tr>
    <th class="px-2 md:px-3 py-2 text-left text-xs md:text-sm font-medium text-gray-500 uppercase">年份</th>
    <th class="px-2 md:px-3 py-2 text-right text-xs md:text-sm font-medium text-gray-500 uppercase">基础支出</th>
    <th class="px-2 md:px-3 py-2 text-right text-xs md:text-sm font-medium text-gray-500 uppercase">特殊支出</th>
    <th class="px-2 md:px-3 py-2 text-right text-xs md:text-sm font-medium text-gray-500 uppercase">调整值</th> ← 新增
    <th class="px-2 md:px-3 py-2 text-right text-xs md:text-sm font-medium text-gray-500 uppercase">实际支出</th>
    <th class="px-2 md:px-3 py-2 text-right text-xs md:text-sm font-medium text-gray-500 uppercase">实际同比</th>
  </tr>
</thead>
```

**数据行** (lines 120-152):
```vue
<tbody class="bg-white">
  <tr v-for="item in convertedTrendData" :key="item.year" class="hover:bg-gray-50 border-b border-gray-200">
    <td class="px-2 md:px-3 py-2 whitespace-nowrap">
      <div class="text-xs md:text-sm font-medium text-gray-900">{{ item.year }}</div>
    </td>
    <td class="px-2 md:px-3 py-2 whitespace-nowrap text-right">
      <div class="text-xs md:text-sm font-medium text-gray-900">
        {{ getCurrencySymbol(selectedCurrency) }}{{ formatAmount(item.baseExpense) }}
      </div>
    </td>
    <td class="px-2 md:px-3 py-2 whitespace-nowrap text-right">
      <div class="text-xs md:text-sm font-medium text-orange-600">
        {{ getCurrencySymbol(selectedCurrency) }}{{ formatAmount(item.specialExpense || 0) }}
      </div>
    </td>
    <td class="px-2 md:px-3 py-2 whitespace-nowrap text-right">  ← 新增
      <div class="text-xs md:text-sm font-medium" :class="getTotalAdjustmentColor(item.assetAdjustment, item.liabilityAdjustment)">
        {{ getCurrencySymbol(selectedCurrency) }}{{ formatAmount(getTotalAdjustment(item.assetAdjustment || 0, item.liabilityAdjustment || 0)) }}
      </div>
    </td>
    <td class="px-2 md:px-3 py-2 whitespace-nowrap text-right">
      <div class="text-xs md:text-sm font-bold text-blue-600">
        {{ getCurrencySymbol(selectedCurrency) }}{{ formatAmount(item.actualExpense) }}
      </div>
    </td>
    <!-- ... 同比数据 ... -->
  </tr>
</tbody>
```

**复用现有函数**:
- `getTotalAdjustment(assetAdjustment, liabilityAdjustment)`: 计算总调整值
- `getTotalAdjustmentColor(assetAdjustment, liabilityAdjustment)`: 获取颜色类名（红/绿/灰）

## 视觉设计

### 配色方案
- **基础支出**: 黑色 `text-gray-900`
- **特殊支出**: 橙色 `text-orange-600`
- **调整值**:
  - 正值（支出增加）: 红色 `text-red-600`
  - 负值（支出减少）: 绿色 `text-green-600`
  - 零值: 灰色 `text-gray-600`
- **实际支出**: 蓝色粗体 `text-blue-600 font-bold`

### 列宽优化
使用 `whitespace-nowrap` 和 `text-right` 确保数字对齐且不换行

## 使用示例

### 实际数据示例

| 年份 | 基础支出 | 特殊支出 | 调整值 | 实际支出 | 实际同比 |
|------|---------|---------|--------|---------|---------|
| 2018 | $84,092.55 | $628,840.92 | -$104,877.93 | $608,055.54 | - |
| 2019 | $90,031.12 | $47,031.68 | -$38,828.63 | $98,234.17 | -83.85% |
| 2020 | $106,896.64 | $50,043.14 | -$54,912.60 | $102,027.18 | +3.86% |
| 2021 | $114,850.31 | $0.00 | -$27,281.15 | $87,569.16 | -14.17% |
| 2022 | $122,169.71 | $56,356.18 | -$29,359.72 | $149,166.17 | +70.34% |
| 2023 | $131,584.65 | $0.00 | -$32,456.64 | $99,128.01 | -33.55% |
| 2024 | $142,315.57 | $43,897.27 | -$30,428.65 | $155,784.19 | +57.16% |
| 2025 | $143,861.33 | $54,372.56 | -$82,602.43 | $115,631.46 | -25.77% |

### 公式验证

**2025年**:
```
基础支出: $143,861.33
特殊支出: +$54,372.56
调整值:   -$82,602.43 (资产$54,242.71 + 负债$28,359.72)
──────────────────
实际支出: $115,631.46 ✓
```

**2024年**:
```
基础支出: $142,315.57
特殊支出: +$43,897.27
调整值:   -$30,428.65 (资产$23,925.85 + 负债$6,502.80)
──────────────────
实际支出: $155,784.19 ✓
```

### 调整值含义

**负值（绿色）**: 表示支出减少
- 资产增加（如房产升值）→ 支出减少
- 负债减少（如房贷还款）→ 支出减少

**正值（红色）**: 表示支出增加
- 资产减少（如保险账户缩水）→ 支出增加
- 负债增加（理论上，但当前数据中不常见）→ 支出增加

## 优势

1. **公式清晰**: 直观展示实际支出的构成
2. **数据完整**: 一眼看出基础、特殊、调整、实际四项数据
3. **颜色提示**: 调整值颜色提示增减情况
4. **便于审计**: 可快速验证计算公式
5. **响应式设计**: 适配不同屏幕尺寸

## 注意事项

1. **调整值为负**: 大部分年份调整值为负（支出减少），这是正常的
2. **货币转换**: 使用各年份年末汇率换算显示
3. **公式方向**: 基础 + 特殊 **-** 调整 = 实际（注意是减法）
4. **null值处理**: `item.assetAdjustment || 0` 避免显示错误
5. **复用函数**: 与"年度汇总表"Tab使用相同的颜色函数，保持一致性

## 技术要点

### 调整值计算
```javascript
const getTotalAdjustment = (assetAdjustment, liabilityAdjustment) => {
  const asset = assetAdjustment || 0
  const liability = liabilityAdjustment || 0
  return Number(asset) + Number(liability)
}
```

### 颜色逻辑
```javascript
const getTotalAdjustmentColor = (assetAdjustment, liabilityAdjustment) => {
  const total = getTotalAdjustment(assetAdjustment, liabilityAdjustment)
  if (total === 0) return 'text-gray-600'
  // 调整值为正表示支出增加（红色），为负表示支出减少（绿色）
  return total > 0 ? 'text-red-600' : 'text-green-600'
}
```

### 后端公式关系
```
实际支出 = 基础支出 + 特殊支出 + 资产调整 + 负债调整
         = 基础支出 + 特殊支出 + 总调整值

其中：
- 资产调整为负 → 资产增加 → 支出减少
- 负债调整为正 → 负债减少 → 支出减少
- 总调整值 = 资产调整 + 负债调整（通常为负）
```

## 相关文档

- `TREND_CHART_SPECIAL_EXPENSE.md` - 特殊支出显示功能
- `SUMMARY_TABLE_ROWS_UPDATE.md` - 汇总表新增汇总行功能
- `SPECIAL_EXPENSE_UI_UPDATES.md` - 特殊支出UI更新
