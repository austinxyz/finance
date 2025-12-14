# Frontend Best Practices

This document contains detailed implementation guides for frontend development patterns used in this project.

## Chart.js Pie Chart Labels

### Overview
When creating pie charts, use a consistent label format that balances information density with visual clarity.

### Best Practices

1. **Label Format**: Display as "名称-百分比%" (e.g., "股票-35.2%")
2. **Visibility Threshold**: Only show labels for items with ≥5% share to avoid clutter
3. **Required Plugin**: Use `chartjs-plugin-datalabels` for custom label rendering

### Implementation

```javascript
import { Chart, registerables } from 'chart.js'
import ChartDataLabels from 'chartjs-plugin-datalabels'

Chart.register(...registerables, ChartDataLabels)

// Pie chart configuration
const chart = new Chart(ctx, {
  type: 'pie',
  data: {
    labels: categoryData.map(d => d.categoryName),
    datasets: [{
      data: categoryData.map(d => d.amount),
      backgroundColor: colors
    }]
  },
  options: {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: true,
        position: 'bottom'
      },
      tooltip: {
        callbacks: {
          label: function(context) {
            const label = context.label || ''
            const value = context.parsed || 0
            const total = context.dataset.data.reduce((a, b) => a + b, 0)
            const percentage = ((value / total) * 100).toFixed(1)
            return `${label}: ${formatCurrency(value)} (${percentage}%)`
          }
        }
      },
      datalabels: {
        color: '#fff',
        font: {
          weight: 'bold',
          size: 11
        },
        formatter: (value, context) => {
          const label = categoryData[context.dataIndex].categoryName
          const total = context.chart.data.datasets[0].data.reduce((a, b) => a + b, 0)
          const percentage = ((value / total) * 100).toFixed(1)
          // Only show labels for items with ≥5% share
          if (percentage >= 5) {
            return `${label}-${percentage}%`
          }
          return ''
        },
        textAlign: 'center'
      }
    }
  }
})
```

### Key Points

- Labels with <5% share are hidden to keep chart clean and readable
- Tooltip always shows full information regardless of percentage
- Label format: "类别名称-XX.X%" (e.g., "股票-35.2%")
- White color labels on colored segments for contrast
- Font size 11px, bold weight for readability

### Examples

See implementation in:
- `frontend/src/views/analysis/ExpenseAnnual.vue` (lines 563-624)
- `frontend/src/views/analysis/InvestmentAnalysis.vue` (lines 450-496)

## Currency Display

### Overview
Maintain consistent currency formatting throughout the application.

### Best Practices

1. **Dynamic Symbol**: Use currency symbol based on selected currency ($ for USD, ¥ for CNY)
2. **Decimal Places**: Always show 2 decimal places for amounts
3. **Thousand Separators**: Use locale-specific separators (e.g., 1,234.56)

### Implementation

```javascript
const formatCurrency = (value, currency = null) => {
  // If no currency specified, use selected currency
  const curr = currency || selectedCurrency.value

  if (!value) {
    return curr === 'CNY' ? '¥0.00' : '$0.00'
  }

  const formatted = parseFloat(value).toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })

  const symbol = curr === 'CNY' ? '¥' : '$'
  return symbol + formatted
}
```

## Responsive Chart-Table Layouts

### Overview
For data analysis pages, use a consistent 50/50 split between charts and tables.

### Best Practices

1. **Desktop Layout**: Charts and tables side-by-side, each taking 50% width
2. **Mobile Layout**: Stack vertically using `flex-col lg:flex-row`
3. **Height Consistency**: Match chart height with table max-height to avoid scrolling issues
4. **Table Scrolling**: Use `overflow-y-auto` for vertical scrolling only

### Implementation

```vue
<div class="flex flex-col lg:flex-row gap-4">
  <!-- Chart: 50% width on desktop -->
  <div class="flex-1 w-full lg:w-1/2 h-[500px]">
    <canvas ref="chartCanvas"></canvas>
  </div>

  <!-- Table: 50% width on desktop, matches chart height -->
  <div class="flex-1 w-full lg:w-1/2 overflow-y-auto max-h-[500px]">
    <table class="w-full text-xs">
      <!-- table content -->
    </table>
  </div>
</div>
```

### Key Points

- Use `flex-1` for equal width distribution
- Set explicit heights (e.g., `h-[500px]`) for consistent layout
- Match `max-h-[500px]` on table container to chart height
- No horizontal scrolling - adjust font sizes and padding if needed
- Font sizes: headers `text-[10px]`, cells `text-[11px]` for compact display

### Examples

See implementation in:
- `frontend/src/views/analysis/ExpenseAnnual.vue` (lines 71-130)
- `frontend/src/views/analysis/InvestmentAnalysis.vue` (lines 80-142)
