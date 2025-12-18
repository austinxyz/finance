# 年度趋势图新增特殊支出显示功能

## 更新时间
2025-12-17

## 功能概述
在年度支出趋势分析页面的"年度趋势图"Tab中，新增特殊支出的显示：
1. **左侧趋势图**: 新增特殊支出柱状图系列（橙色）
2. **右侧汇总表**: 新增特殊支出列（基础支出和实际支出之间）

## 后端更新

### ExpenseAnalysisService.java

**文件**: `backend/src/main/java/com/finance/app/service/expense/ExpenseAnalysisService.java`

**修改方法**: `getAnnualExpenseTrend()`

**变更说明**:
在年度趋势数据中新增`specialExpense`字段，与`baseExpense`和`actualExpense`一起返回。

**修改代码** (lines 571-588):
```java
// 转换货币
BigDecimal baseExpense = summary.getBaseExpenseAmount();
BigDecimal specialExpense = summary.getSpecialExpenseAmount();  // ← 新增
BigDecimal actualExpense = summary.getActualExpenseAmount();

if (!currency.equals(summary.getCurrency())) {
    Map<String, BigDecimal> rates = exchangeRatesByYear.get(summary.getSummaryYear());
    baseExpense = convertCurrency(baseExpense, summary.getCurrency(), currency, rates);
    specialExpense = convertCurrency(specialExpense, summary.getCurrency(), currency, rates);  // ← 新增
    actualExpense = convertCurrency(actualExpense, summary.getCurrency(), currency, rates);
}

// 基本信息
yearData.put("year", summary.getSummaryYear());
yearData.put("baseExpense", baseExpense);
yearData.put("specialExpense", specialExpense != null ? specialExpense : BigDecimal.ZERO);  // ← 新增
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
    "actualExpense": 115631.46,
    "currency": "USD",
    "yoyBaseChange": null,
    "yoyBaseChangePct": null,
    "yoyActualChange": null,
    "yoyActualChangePct": null
  },
  {
    "year": 2024,
    "baseExpense": 142315.57,
    "specialExpense": 43897.27,
    "actualExpense": 155784.19,
    "currency": "USD",
    "yoyBaseChange": 1545.76,
    "yoyBaseChangePct": 1.09,
    "yoyActualChange": -40152.73,
    "yoyActualChangePct": -25.77
  }
]
```

## 前端更新

### ExpenseAnnualTrend.vue

**文件**: `frontend/src/views/analysis/ExpenseAnnualTrend.vue`

### 1. 右侧汇总表新增特殊支出列

**修改位置**: 年度趋势图Tab的右侧数据表格 (lines 108-147)

**表头更新**:
```vue
<thead class="bg-gray-50 border-b border-gray-200 sticky top-0">
  <tr>
    <th class="px-2 md:px-3 py-2 text-left text-xs md:text-sm font-medium text-gray-500 uppercase">年份</th>
    <th class="px-2 md:px-3 py-2 text-right text-xs md:text-sm font-medium text-gray-500 uppercase">基础支出</th>
    <th class="px-2 md:px-3 py-2 text-right text-xs md:text-sm font-medium text-gray-500 uppercase">特殊支出</th> ← 新增
    <th class="px-2 md:px-3 py-2 text-right text-xs md:text-sm font-medium text-gray-500 uppercase">实际支出</th>
    <th class="px-2 md:px-3 py-2 text-right text-xs md:text-sm font-medium text-gray-500 uppercase">实际同比</th>
  </tr>
</thead>
```

**数据行更新**:
```vue
<tbody class="bg-white">
  <tr v-for="item in convertedTrendData" :key="item.year" class="hover:bg-gray-50 border-b border-gray-200">
    <td class="px-2 md:px-3 py-2 whitespace-nowrap">
      <div class="text-xs md:text-sm font-medium text-gray-900">{{ item.year }}</div>
    </td>
    <td class="px-2 md:px-3 py-2 whitespace-nowrap text-right">
      <div class="text-xs md:text-sm font-medium text-gray-900">{{ getCurrencySymbol(selectedCurrency) }}{{ formatAmount(item.baseExpense) }}</div>
    </td>
    <td class="px-2 md:px-3 py-2 whitespace-nowrap text-right">  ← 新增
      <div class="text-xs md:text-sm font-medium text-orange-600">{{ getCurrencySymbol(selectedCurrency) }}{{ formatAmount(item.specialExpense || 0) }}</div>
    </td>
    <td class="px-2 md:px-3 py-2 whitespace-nowrap text-right">
      <div class="text-xs md:text-sm font-bold text-blue-600">{{ getCurrencySymbol(selectedCurrency) }}{{ formatAmount(item.actualExpense) }}</div>
    </td>
    <!-- ... 同比数据 ... -->
  </tr>
</tbody>
```

**样式说明**:
- 特殊支出列使用橙色文字 (`text-orange-600`)
- 与"年度汇总表"Tab中的特殊支出汇总行颜色保持一致

### 2. 左侧趋势图新增特殊支出柱状图

**修改位置**: `renderChart()` 函数 (lines 575-730)

**数据准备** (lines 579-588):
```javascript
const sortedData = [...trendData.value].reverse() // 从旧到新排序
const years = sortedData.map(d => d.year)
// 将USD金额转换为选中货币（使用各年份的汇率）
const baseExpenses = sortedData.map(d => convertCurrency(d.baseExpense, d.year))
const specialExpenses = sortedData.map(d => convertCurrency(d.specialExpense || 0, d.year))  // ← 新增
const actualExpenses = sortedData.map(d => convertCurrency(d.actualExpense, d.year))
const baseGrowths = sortedData.map(d => d.yoyBaseChangePct ? Number(d.yoyBaseChangePct) : 0)
const actualGrowths = sortedData.map(d => d.yoyActualChangePct ? Number(d.yoyActualChangePct) : 0)
```

**图表配置** (lines 601-646):
```javascript
datasets: [
  {
    label: '基础支出',
    data: baseExpenses,
    backgroundColor: 'rgba(99, 102, 241, 0.7)',      // 紫蓝色
    borderColor: 'rgb(99, 102, 241)',
    borderWidth: 1,
    yAxisID: 'y'
  },
  {
    label: '特殊支出',                              // ← 新增
    data: specialExpenses,
    backgroundColor: 'rgba(251, 146, 60, 0.7)',      // 橙色
    borderColor: 'rgb(251, 146, 60)',
    borderWidth: 1,
    yAxisID: 'y'
  },
  {
    label: '实际支出',
    data: actualExpenses,
    backgroundColor: 'rgba(239, 68, 68, 0.7)',       // 红色
    borderColor: 'rgb(239, 68, 68)',
    borderWidth: 1,
    yAxisID: 'y'
  },
  {
    label: '基础支出同比',
    data: baseGrowths,
    type: 'line',
    borderColor: 'rgb(59, 130, 246)',                // 蓝色线
    backgroundColor: 'rgba(59, 130, 246, 0.1)',
    borderWidth: 2,
    tension: 0.4,
    yAxisID: 'y1'
  },
  {
    label: '实际支出同比',
    data: actualGrowths,
    type: 'line',
    borderColor: 'rgb(234, 88, 12)',                 // 橙色线
    backgroundColor: 'rgba(234, 88, 12, 0.1)',
    borderWidth: 2,
    tension: 0.4,
    yAxisID: 'y1'
  }
]
```

**图表显示顺序**:
1. 基础支出 (紫蓝色柱)
2. 特殊支出 (橙色柱) ← 新增
3. 实际支出 (红色柱)
4. 基础支出同比 (蓝色线)
5. 实际支出同比 (橙色线)

## 视觉设计

### 配色方案
- **基础支出**: `rgba(99, 102, 241, 0.7)` - 紫蓝色
- **特殊支出**: `rgba(251, 146, 60, 0.7)` - 橙色 (与表格保持一致)
- **实际支出**: `rgba(239, 68, 68, 0.7)` - 红色
- **基础同比线**: `rgb(59, 130, 246)` - 蓝色
- **实际同比线**: `rgb(234, 88, 12)` - 橙色

### 图例顺序
图例自动按数据集顺序显示，从左到右：
1. 基础支出
2. 特殊支出 ← 新增
3. 实际支出
4. 基础支出同比
5. 实际支出同比

## 使用示例

### 测试API端点
```bash
curl "http://localhost:8080/api/expenses/analysis/annual/trend?familyId=1&limit=5&currency=USD"
```

### 实际数据示例

| 年份 | 基础支出 | 特殊支出 | 实际支出 | 实际同比 |
|------|---------|---------|---------|---------|
| 2018 | $84,092.55 | $628,840.92 | $608,055.54 | - |
| 2019 | $90,031.12 | $47,031.68 | $98,234.17 | -83.85% |
| 2020 | $106,896.64 | $50,043.14 | $102,027.18 | +3.86% |
| 2021 | $114,850.31 | $0.00 | $87,569.16 | -14.17% |
| 2022 | $122,169.71 | $56,356.18 | $149,166.17 | +70.34% |
| 2023 | $131,584.65 | $0.00 | $99,128.01 | -33.55% |
| 2024 | $142,315.57 | $43,897.27 | $155,784.19 | +57.16% |
| 2025 | $143,861.33 | $54,372.56 | $115,631.46 | -25.77% |

### 图表特征
1. **2018年**: 特殊支出极高 ($628K) - 主要为房产首付
2. **2019-2020年**: 特殊支出较多 ($47K-$50K) - 多项大额支出
3. **2021年**: 无特殊支出
4. **2022年**: 特殊支出 $56K
5. **2023年**: 无特殊支出
6. **2024-2025年**: 特殊支出恢复 ($44K-$54K)

## 优势

1. **可视化清晰**: 三色柱状图区分基础、特殊、实际支出
2. **趋势明显**: 橙色柱清晰显示各年度特殊支出波动
3. **数据完整**: 图表和表格数据一致，便于对照
4. **交互友好**: Chart.js工具提示显示详细金额
5. **响应式设计**: 适配不同屏幕尺寸（移动端图例位置调整）

## 注意事项

1. 特殊支出为null时自动转换为0，避免显示错误
2. 橙色配色与年度汇总表的特殊支出汇总行保持一致
3. 图表工具提示自动显示货币符号和格式化金额
4. 后端API默认返回USD基准货币，前端根据选中货币转换显示
5. 图表使用双Y轴：左侧为金额，右侧为百分比

## 技术要点

### 汇率转换
- 后端返回USD基准货币数据
- 前端使用各年份的年末汇率进行换算
- `convertCurrency(amount, year)` 函数自动查找对应年份汇率

### 数据处理
- `specialExpense || 0` 处理null值
- `Number()` 转换同比百分比为数值类型
- `reverse()` 排序使图表按时间从旧到新显示

### Chart.js配置
- `type: 'bar'` - 柱状图
- `yAxisID: 'y'` - 金额轴（左侧）
- `yAxisID: 'y1'` - 百分比轴（右侧）
- `tension: 0.4` - 线条平滑度
