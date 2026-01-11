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

## Styling with Tailwind CSS

### Overview
This project uses Tailwind CSS utility classes exclusively for all styling. Never use CSS variables, custom scoped styles, or inline styles.

### Best Practices

1. **Use Tailwind Utility Classes**: All styling should be done via Tailwind's utility classes
2. **No CSS Variables**: Don't use CSS variables like `--primary`, `--card-bg`, etc.
3. **No Scoped Styles**: Avoid `<style scoped>` blocks unless absolutely necessary
4. **Composable Patterns**: Reuse common utility patterns across components

### Common Patterns

#### 1. Card/Container Styling

```vue
<!-- White card with shadow and rounded corners -->
<div class="bg-white rounded-lg shadow p-6">
  <h2 class="text-xl font-semibold text-gray-900 mb-4">Card Title</h2>
  <p class="text-sm text-gray-600">Card content</p>
</div>

<!-- Card with border instead of shadow -->
<div class="bg-white rounded-lg border border-gray-200 p-6">
  <!-- Content -->
</div>
```

#### 2. Button Styling

```vue
<!-- Primary button -->
<button class="px-5 py-2.5 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors">
  Primary Action
</button>

<!-- Secondary button -->
<button class="px-4 py-2 bg-white text-gray-700 border border-gray-300 rounded-lg font-medium hover:bg-gray-50 transition-colors">
  Secondary Action
</button>

<!-- Danger button -->
<button class="px-4 py-2 bg-red-600 text-white rounded-lg font-medium hover:bg-red-700 transition-colors">
  Delete
</button>
```

#### 3. Form Controls

```vue
<!-- Input field -->
<input
  type="text"
  class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
  placeholder="Enter value"
/>

<!-- Select dropdown -->
<select class="px-4 py-2 border border-gray-300 rounded-lg text-sm font-medium focus:ring-2 focus:ring-blue-500 bg-white">
  <option value="">Select option</option>
  <option value="1">Option 1</option>
</select>

<!-- Label -->
<label class="block text-sm font-medium text-gray-700 mb-2">
  Field Label
</label>
```

#### 4. Tables

```vue
<div class="overflow-x-auto">
  <table class="w-full">
    <thead class="bg-gray-50">
      <tr>
        <th class="px-4 py-3 text-left text-xs font-semibold text-gray-600 uppercase">Column</th>
      </tr>
    </thead>
    <tbody class="divide-y divide-gray-200">
      <tr class="hover:bg-gray-50">
        <td class="px-4 py-3 text-sm text-gray-900">Cell content</td>
      </tr>
    </tbody>
  </table>
</div>
```

#### 5. Badge/Tag Components

```vue
<!-- Color-coded badges -->
<span class="inline-block px-3 py-1 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
  Daily
</span>

<span class="inline-block px-3 py-1 rounded-full text-xs font-medium bg-orange-100 text-orange-800">
  Weekly
</span>

<span class="inline-block px-3 py-1 rounded-full text-xs font-medium bg-purple-100 text-purple-800">
  Monthly
</span>

<span class="inline-block px-3 py-1 rounded-full text-xs font-medium bg-gray-100 text-gray-800">
  Manual
</span>
```

#### 6. Status Indicators

```vue
<!-- Success/healthy status -->
<span class="text-lg font-semibold text-green-600">✓ Normal</span>

<!-- Error/unhealthy status -->
<span class="text-lg font-semibold text-red-600">✗ Error</span>

<!-- Warning status -->
<span class="text-lg font-semibold text-orange-600">⚠ Warning</span>
```

#### 7. Layout Utilities

```vue
<!-- Flex layouts -->
<div class="flex items-center justify-between gap-4">
  <div class="flex-1">Flexible content</div>
  <div class="flex gap-2">
    <button>Action 1</button>
    <button>Action 2</button>
  </div>
</div>

<!-- Grid layouts -->
<div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
  <div>Item 1</div>
  <div>Item 2</div>
  <div>Item 3</div>
</div>

<!-- Spacing -->
<div class="space-y-6 p-6">
  <div>Section 1</div>
  <div>Section 2</div>
</div>
```

#### 8. Alert/Warning Boxes

```vue
<!-- Warning alert -->
<div class="bg-orange-50 border-l-4 border-orange-500 p-4 mb-4">
  <p class="font-semibold text-gray-900 mb-2">Warning Title</p>
  <p class="text-sm text-gray-700">Warning message content</p>
</div>

<!-- Info alert -->
<div class="bg-blue-50 border-l-4 border-blue-500 p-4">
  <p class="text-sm text-gray-700">Info message</p>
</div>

<!-- Error alert -->
<div class="bg-red-50 border-l-4 border-red-500 p-4">
  <p class="text-sm text-red-700">Error message</p>
</div>
```

#### 9. Modal/Dialog

```vue
<!-- Overlay -->
<div class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
  <!-- Dialog -->
  <div class="bg-white rounded-lg shadow-xl w-full max-w-md">
    <!-- Header -->
    <div class="p-6 border-b border-gray-200">
      <h3 class="text-lg font-semibold text-gray-900">Dialog Title</h3>
    </div>

    <!-- Body -->
    <div class="p-6">
      <p class="text-sm text-gray-700">Dialog content</p>
    </div>

    <!-- Footer -->
    <div class="p-4 bg-gray-50 border-t border-gray-200 flex justify-end gap-3">
      <button class="px-4 py-2 bg-white border border-gray-300 rounded-lg">Cancel</button>
      <button class="px-4 py-2 bg-blue-600 text-white rounded-lg">Confirm</button>
    </div>
  </div>
</div>
```

### Color System

#### Primary Colors
- **Blue**: `bg-blue-600`, `text-blue-600`, `border-blue-600`
- **Red**: `bg-red-600`, `text-red-600`, `border-red-600`
- **Green**: `bg-green-600`, `text-green-600`, `border-green-600`
- **Orange**: `bg-orange-600`, `text-orange-600`, `border-orange-600`

#### Gray Scale
- **Background**: `bg-gray-50` (light), `bg-gray-100`, `bg-gray-200`
- **Borders**: `border-gray-200`, `border-gray-300`
- **Text**: `text-gray-600` (secondary), `text-gray-700`, `text-gray-900` (primary)

#### Semantic Colors
- **Success**: Green (`text-green-600`)
- **Error/Danger**: Red (`text-red-600`, `bg-red-600`)
- **Warning**: Orange (`text-orange-600`, `bg-orange-50`)
- **Info**: Blue (`text-blue-600`, `bg-blue-50`)

### Spacing Scale

- **xs**: `gap-1`, `p-1` (0.25rem / 4px)
- **sm**: `gap-2`, `p-2` (0.5rem / 8px)
- **md**: `gap-3`, `p-3` (0.75rem / 12px)
- **base**: `gap-4`, `p-4` (1rem / 16px)
- **lg**: `gap-6`, `p-6` (1.5rem / 24px)
- **xl**: `gap-8`, `p-8` (2rem / 32px)

### Typography

```vue
<!-- Headings -->
<h1 class="text-2xl font-bold text-gray-900">Main Title</h1>
<h2 class="text-xl font-semibold text-gray-900">Section Title</h2>
<h3 class="text-lg font-semibold text-gray-900">Subsection</h3>

<!-- Body text -->
<p class="text-sm text-gray-600">Description text</p>
<span class="text-xs text-gray-500">Small text / label</span>

<!-- Monospace (for code/filenames) -->
<code class="bg-black bg-opacity-10 px-2 py-0.5 rounded text-xs font-mono">filename.sql</code>
```

### Anti-Patterns to Avoid

❌ **Don't use CSS variables**
```vue
<!-- Bad -->
<div :style="{ color: 'var(--primary)', background: 'var(--card-bg)' }">

<!-- Good -->
<div class="text-blue-600 bg-white">
```

❌ **Don't use scoped styles**
```vue
<!-- Bad -->
<style scoped>
.my-button {
  background-color: #3b82f6;
  padding: 0.5rem 1rem;
}
</style>

<!-- Good -->
<button class="bg-blue-600 px-4 py-2">
```

❌ **Don't use inline styles**
```vue
<!-- Bad -->
<div style="background-color: white; padding: 1.5rem;">

<!-- Good -->
<div class="bg-white p-6">
```

### Examples

See implementation in:
- `frontend/src/views/admin/BackupManagement.vue` - Complete UI with cards, tables, buttons, badges, and modals
- `frontend/src/components/Sidebar.vue` - Navigation components with active states
- `frontend/src/views/Dashboard.vue` - Responsive grid layouts and stat cards

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

## Mobile-Friendly Design

### Overview
Ensure all pages are fully responsive and optimized for mobile devices (phones and tablets). The application uses Tailwind CSS's responsive utilities to adapt layouts across different screen sizes.

### Responsive Breakpoints

Tailwind CSS breakpoints used in this project:
- **sm**: 640px (landscape phones)
- **md**: 768px (tablets)
- **lg**: 1024px (small desktops)
- **xl**: 1280px (large desktops)

### Core Principles

1. **Mobile-First Approach**: Base styles target mobile, use breakpoint prefixes for larger screens
2. **Touch-Friendly**: Minimum tap target size of 44x44px for buttons and interactive elements
3. **Readable Text**: Responsive font sizes that scale appropriately
4. **Scrollable Tables**: Horizontal scroll for wide data tables on small screens
5. **Adaptive Layouts**: Grid and flex layouts that stack on mobile, spread on desktop

### Layout Patterns

#### 1. Responsive Grid Layouts

```vue
<!-- Stats cards: 1 column mobile, 2 tablet, 5 desktop -->
<div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
  <div class="bg-white rounded-lg shadow p-4">
    <!-- Card content -->
  </div>
</div>

<!-- Chart sections: stack mobile, side-by-side desktop -->
<div class="grid grid-cols-1 lg:grid-cols-2 gap-4 md:gap-6">
  <div class="bg-white rounded-lg shadow p-4">
    <!-- Chart 1 -->
  </div>
  <div class="bg-white rounded-lg shadow p-4">
    <!-- Chart 2 -->
  </div>
</div>

<!-- Three-column layout: 1 mobile, 2 tablet, 3 desktop -->
<div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
  <!-- Items -->
</div>
```

#### 2. Responsive Flex Layouts

```vue
<!-- Header with title and actions -->
<div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
  <div>
    <h1 class="text-xl md:text-2xl font-bold">Page Title</h1>
    <p class="text-xs md:text-sm text-gray-600 mt-1">Description</p>
  </div>
  <div class="flex flex-wrap items-center gap-2">
    <!-- Filters and actions -->
  </div>
</div>

<!-- Form buttons: stack mobile, inline desktop -->
<div class="flex flex-col sm:flex-row gap-2 sm:gap-3">
  <button class="px-4 py-2">Primary</button>
  <button class="px-4 py-2">Secondary</button>
</div>

<!-- Reverse order on mobile (Cancel button below) -->
<div class="flex flex-col-reverse sm:flex-row gap-2">
  <button>Cancel</button>
  <button>Submit</button>
</div>
```

#### 3. Chart-Table Layouts

```vue
<!-- Horizontal layout desktop, stacked mobile -->
<div class="flex flex-col lg:flex-row gap-4">
  <!-- Chart: fixed width desktop, full width mobile -->
  <div class="flex-shrink-0 w-full lg:w-80 h-64">
    <canvas ref="chartCanvas"></canvas>
  </div>

  <!-- Table: flex-1 to fill remaining space -->
  <div class="flex-1 overflow-auto">
    <table class="min-w-full text-xs">
      <!-- table content -->
    </table>
  </div>
</div>
```

### Text and Spacing

#### 1. Responsive Typography

```vue
<!-- Page titles -->
<h1 class="text-xl md:text-2xl lg:text-3xl font-bold">Title</h1>

<!-- Section headings -->
<h2 class="text-base md:text-lg font-semibold">Section</h2>

<!-- Body text -->
<p class="text-sm md:text-base text-gray-600">Content</p>

<!-- Small text / labels -->
<span class="text-xs md:text-sm text-gray-500">Label</span>
```

#### 2. Responsive Spacing

```vue
<!-- Container padding -->
<div class="p-3 md:p-4 lg:p-6">

<!-- Section spacing -->
<div class="space-y-4 md:space-y-6">

<!-- Grid gaps -->
<div class="grid grid-cols-2 gap-3 md:gap-4 lg:gap-6">

<!-- Button padding -->
<button class="px-3 py-2 md:px-4 md:py-2.5">
```

### Table Handling

#### 1. Horizontal Scroll Tables

```vue
<!-- Wrapper with horizontal scroll -->
<div class="overflow-x-auto">
  <table class="min-w-full text-xs">
    <thead class="bg-gray-50 sticky top-0">
      <tr>
        <th class="px-2 py-1.5 text-left font-medium text-gray-700 uppercase whitespace-nowrap">
          Column Name
        </th>
      </tr>
    </thead>
    <tbody class="divide-y divide-gray-200">
      <tr class="hover:bg-gray-50">
        <td class="px-2 py-1.5 whitespace-nowrap">
          {{ value }}
        </td>
      </tr>
    </tbody>
  </table>
</div>
```

**Key points:**
- Use `overflow-x-auto` for horizontal scrolling
- `min-w-full` ensures table takes full width
- `sticky top-0` keeps headers visible while scrolling
- `whitespace-nowrap` prevents text wrapping
- Compact padding: `px-2 py-1.5`
- Small font: `text-xs` (12px)

#### 2. Responsive Column Hiding

```vue
<!-- Hide columns on mobile -->
<table>
  <thead>
    <tr>
      <th class="px-2 py-1">Name</th>
      <th class="hidden sm:table-cell px-2 py-1">Description</th>
      <th class="hidden md:table-cell px-2 py-1">Created</th>
      <th class="px-2 py-1">Actions</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td class="px-2 py-1">{{ item.name }}</td>
      <td class="hidden sm:table-cell px-2 py-1">{{ item.description }}</td>
      <td class="hidden md:table-cell px-2 py-1">{{ item.created }}</td>
      <td class="px-2 py-1"><!-- actions --></td>
    </tr>
  </tbody>
</table>
```

### Mobile Navigation

#### 1. Sidebar Layout

```vue
<!-- MainLayout.vue pattern -->
<div class="flex h-screen bg-background overflow-hidden">
  <!-- Mobile Header (hamburger menu) -->
  <header class="lg:hidden fixed top-0 left-0 right-0 z-50 h-14 bg-card border-b">
    <button @click="toggleMobileSidebar" class="p-2">
      <!-- Menu icon -->
    </button>
  </header>

  <!-- Overlay for mobile sidebar -->
  <div
    v-if="showMobileSidebar"
    @click="closeMobileSidebar"
    class="lg:hidden fixed inset-0 bg-black/50 z-40"
  ></div>

  <!-- Sidebar (drawer on mobile, static on desktop) -->
  <aside
    :class="[
      'fixed top-0 bottom-0 z-40 transition-transform duration-300',
      'lg:translate-x-0 lg:static',
      showMobileSidebar ? 'translate-x-0' : '-translate-x-full'
    ]"
  >
    <Sidebar @navigate="closeMobileSidebar" />
  </aside>

  <!-- Main content with top padding on mobile -->
  <main class="flex-1 overflow-y-auto pt-14 lg:pt-0">
    <div class="container mx-auto px-4 md:px-6 py-4 md:py-6">
      <RouterView />
    </div>
  </main>
</div>
```

**Key points:**
- Mobile header only visible on `lg:hidden`
- Sidebar is `fixed` on mobile, `static` on desktop
- Translate animations for drawer effect
- Auto-close sidebar on route change
- Overlay prevents interaction with content

### Form Controls

#### 1. Input Fields

```vue
<!-- Full width on mobile, auto on desktop -->
<div class="flex flex-col sm:flex-row items-stretch sm:items-center gap-2">
  <label class="text-sm font-medium whitespace-nowrap">Label:</label>
  <input
    type="text"
    class="flex-1 sm:flex-none px-3 py-2 border rounded-lg text-sm focus:ring-2 focus:ring-primary"
  />
</div>
```

#### 2. Select Dropdowns

```vue
<!-- Responsive select with proper sizing -->
<select
  v-model="selected"
  class="w-full sm:w-auto px-3 py-2 border rounded-lg text-sm focus:ring-2 focus:ring-primary bg-white"
>
  <option v-for="item in items" :key="item.id" :value="item.id">
    {{ item.name }}
  </option>
</select>
```

### Charts

#### 1. Responsive Chart Containers

```vue
<!-- Fixed height with responsive breakpoints -->
<div class="h-48 sm:h-64 lg:h-80">
  <canvas ref="chartCanvas"></canvas>
</div>

<!-- Chart.js configuration -->
<script setup>
const chartOptions = {
  responsive: true,
  maintainAspectRatio: false, // Required for fixed heights
  // ... other options
}

// Handle window resize
let resizeTimer = null
const handleResize = () => {
  if (resizeTimer) clearTimeout(resizeTimer)
  resizeTimer = setTimeout(() => {
    if (chartInstance) {
      chartInstance.resize()
    }
  }, 250) // 250ms debounce
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (resizeTimer) clearTimeout(resizeTimer)
})
</script>
```

#### 2. Smart Pie Chart Labels (Mobile-Optimized)

```javascript
// InvestmentAnalysis.vue pattern
datalabels: {
  color: '#fff',
  font: {
    weight: 'bold',
    size: 11,
    lineHeight: 1.2
  },
  formatter: (value, context) => {
    const dataset = context.dataset
    const total = dataset.data.reduce((a, b) => Math.abs(a) + Math.abs(b), 0)
    const percentage = total > 0 ? ((Math.abs(value) / total) * 100).toFixed(1) : '0.0'
    const label = context.chart.data.labels[context.dataIndex]

    // Hide small segments to avoid overlap
    if (percentage <= 5) return ''

    // Large segments: show label + percentage
    if (percentage > 10) {
      return `${label}\n${percentage}%`
    }
    // Medium segments: show percentage only
    return `${percentage}%`
  },
  anchor: 'center',
  align: 'center'
}
```

### Interactive Elements

#### 1. Buttons

```vue
<!-- Touch-friendly button sizes -->
<button class="w-full sm:w-auto px-4 py-3 sm:py-2 rounded-lg font-medium">
  Button Text
</button>

<!-- Icon buttons with minimum size -->
<button class="p-3 rounded-lg hover:bg-gray-100">
  <Icon class="w-5 h-5" />
</button>
```

#### 2. Clickable Cards/Rows

```vue
<!-- Larger tap targets on mobile -->
<div
  @click="handleClick"
  class="p-4 cursor-pointer hover:bg-gray-50 transition-colors rounded-lg"
>
  <div class="flex items-center gap-3">
    <Icon class="w-5 h-5" />
    <span class="text-sm font-medium">Item Name</span>
  </div>
</div>
```

### Testing Guidelines

#### Recommended Test Devices

1. **iPhone SE** (375px width)
   - Smallest common screen
   - Test: Grid stacking, text wrapping, filter overflow

2. **iPhone 12/13** (390px width)
   - Most common mobile size
   - Test: Table scrolling, chart labels, form layouts

3. **iPad** (768px width)
   - Tablet breakpoint (md)
   - Test: Grid transitions, sidebar visibility

4. **Desktop** (1280px+ width)
   - Full desktop experience
   - Test: Multi-column layouts, side-by-side charts

#### Key Test Cases

- [ ] Navigation menu works on mobile (hamburger → drawer)
- [ ] All forms are usable on mobile
- [ ] Tables scroll horizontally without breaking layout
- [ ] Charts render correctly and labels don't overlap
- [ ] Buttons are large enough to tap (44px minimum)
- [ ] Text is readable without zooming (14px+ for body text)
- [ ] Grid layouts stack properly on mobile
- [ ] No horizontal page scrolling (except intentional table scroll)
- [ ] Landscape mode works correctly

### Common Patterns Reference

#### Dashboard Page Pattern
```vue
<!-- See: frontend/src/views/Dashboard.vue -->
<div class="p-3 md:p-6 space-y-4 md:space-y-6">
  <!-- Stats: 1-col mobile, 2-col tablet, 5-col desktop -->
  <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
    <!-- Stat cards -->
  </div>

  <!-- Charts: stack mobile, side-by-side desktop -->
  <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
    <!-- Chart cards -->
  </div>
</div>
```

#### Analysis Page Pattern
```vue
<!-- See: frontend/src/views/analysis/InvestmentAnalysis.vue -->
<div class="p-3 md:p-6 space-y-4 md:space-y-6">
  <!-- Filters -->
  <div class="flex flex-col sm:flex-row sm:items-center gap-4">
    <h1 class="text-xl md:text-2xl font-bold">Title</h1>
    <div class="flex flex-wrap items-center gap-2">
      <!-- Select dropdowns -->
    </div>
  </div>

  <!-- Summary cards -->
  <div class="grid grid-cols-2 md:grid-cols-5 gap-3">
    <!-- Metric cards -->
  </div>

  <!-- Chart + Table -->
  <div class="flex flex-col lg:flex-row gap-4">
    <div class="w-full lg:w-80 h-64">
      <canvas ref="chart"></canvas>
    </div>
    <div class="flex-1 overflow-auto">
      <table class="min-w-full text-xs">
        <!-- Table -->
      </table>
    </div>
  </div>
</div>
```

### Anti-Patterns to Avoid

❌ **Don't use fixed pixel widths for containers**
```vue
<!-- Bad -->
<div class="w-[800px]">

<!-- Good -->
<div class="w-full max-w-4xl mx-auto">
```

❌ **Don't forget overflow handling for tables**
```vue
<!-- Bad: table breaks layout on mobile -->
<table class="w-full">

<!-- Good: horizontal scroll enabled -->
<div class="overflow-x-auto">
  <table class="min-w-full">
```

❌ **Don't use small touch targets**
```vue
<!-- Bad: too small for touch -->
<button class="p-1 text-xs">

<!-- Good: minimum 44x44px -->
<button class="p-3 text-sm">
```

❌ **Don't forget mobile-specific styles**
```vue
<!-- Bad: same padding everywhere -->
<div class="p-6">

<!-- Good: responsive padding -->
<div class="p-3 md:p-6">
```

### Examples

Mobile-friendly implementations:
- `frontend/src/components/MainLayout.vue` - Mobile navigation
- `frontend/src/views/Dashboard.vue` - Responsive dashboard
- `frontend/src/views/analysis/InvestmentAnalysis.vue` - Analysis page with drill-down
- `frontend/src/views/analysis/AssetAllocation.vue` - Filter controls and charts
- `frontend/src/views/settings/FamilyManagement.vue` - Form layouts
