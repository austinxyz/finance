<template>
  <div class="p-3 md:p-6 space-y-4 md:space-y-4 md:space-y-6">
    <!-- 页面头部 -->
    <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-3 md:gap-0">
      <div>
        <h1 class="text-xl md:text-2xl font-bold text-gray-900">年度预算分析</h1>
        <p class="text-xs md:text-sm text-gray-500 mt-1">对比预算与实际支出</p>
      </div>
      <div class="flex flex-col sm:flex-row items-stretch sm:items-center gap-2 md:gap-4">
        <div class="flex items-center gap-2">
          <label class="text-xs md:text-sm font-medium text-gray-700">年份：</label>
          <input
            v-model.number="selectedYear"
            type="number"
            min="2020"
            :max="new Date().getFullYear() + 5"
            class="px-3 py-1.5 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary w-24"
          />
        </div>
        <div class="flex items-center gap-2">
          <label class="text-xs md:text-sm font-medium text-gray-700">货币：</label>
          <select
            v-model="selectedCurrency"
            class="px-3 py-1.5 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary"
          >
            <option v-for="currency in currencies" :key="currency" :value="currency">
              {{ currency === 'All' ? 'All (折算为USD)' : currency === 'CNY' ? 'CNY (¥)' : 'USD ($)' }}
            </option>
          </select>
        </div>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="text-center py-12 text-gray-500">
      加载中...
    </div>

    <template v-else>
      <!-- 总览卡片 -->
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div class="bg-white p-4 rounded-lg shadow border border-gray-200">
          <div class="text-xs md:text-sm text-gray-600 mb-1">年度预算总额</div>
          <div class="text-xl md:text-2xl font-bold text-blue-600">
            {{ formatCurrency(totalBudget) }}
          </div>
        </div>
        <div class="bg-white p-4 rounded-lg shadow border border-gray-200">
          <div class="text-xs md:text-sm text-gray-600 mb-1">实际支出总额</div>
          <div class="text-xl md:text-2xl font-bold text-orange-600">
            {{ formatCurrency(totalActual) }}
          </div>
        </div>
        <div class="bg-white p-4 rounded-lg shadow border border-gray-200">
          <div class="text-xs md:text-sm text-gray-600 mb-1">剩余预算</div>
          <div class="text-xl md:text-2xl font-bold" :class="totalRemaining >= 0 ? 'text-green-600' : 'text-red-600'">
            {{ formatCurrency(totalRemaining) }}
          </div>
        </div>
        <div class="bg-white p-4 rounded-lg shadow border border-gray-200">
          <div class="text-xs md:text-sm text-gray-600 mb-1">预算执行率</div>
          <div class="text-xl md:text-2xl font-bold text-gray-900">
            {{ executionRate }}%
          </div>
        </div>
      </div>

      <!-- 对比图表 -->
      <div class="bg-white p-6 rounded-lg shadow border border-gray-200">
        <h2 class="text-md md:text-lg font-semibold text-gray-900 mb-4">预算 vs 实际支出对比</h2>
        <div style="height: 500px;">
          <canvas ref="comparisonChartCanvas"></canvas>
        </div>
      </div>

      <!-- 分类详细对比表 -->
      <div class="bg-white rounded-lg shadow border border-gray-200">
        <div class="px-6 py-4 border-b border-gray-200">
          <h2 class="text-md md:text-lg font-semibold text-gray-900">分类详细对比</h2>
        </div>
        <div class="overflow-x-auto">
          <table class="min-w-full divide-y divide-gray-200">
            <thead class="bg-gray-50">
              <tr>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  分类
                </th>
                <th class="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                  年度预算
                </th>
                <th class="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                  实际支出
                </th>
                <th class="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                  剩余预算
                </th>
                <th class="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                  执行率
                </th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  进度
                </th>
              </tr>
            </thead>
            <tbody class="bg-white divide-y divide-gray-200">
              <template v-for="major in majorCategories" :key="major.id">
                <!-- 大类行 -->
                <tr class="bg-gray-50 font-medium">
                  <td class="px-6 py-4 whitespace-nowrap">
                    <span class="text-xl mr-2">{{ major.icon }}</span>
                    {{ major.name }}
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap text-right text-sm text-gray-900">
                    {{ formatCurrency(major.totalBudget) }}
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap text-right text-sm text-gray-900">
                    {{ formatCurrency(major.totalActual) }}
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap text-right text-sm" :class="major.totalRemaining >= 0 ? 'text-green-600' : 'text-red-600'">
                    {{ formatCurrency(major.totalRemaining) }}
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap text-right text-sm text-gray-900">
                    {{ major.executionRate }}%
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap">
                    <div class="w-full bg-gray-200 rounded-full h-2">
                      <div
                        class="h-2 rounded-full transition-all"
                        :class="major.executionRate > 100 ? 'bg-red-500' : major.executionRate > 80 ? 'bg-yellow-500' : 'bg-green-500'"
                        :style="{ width: Math.min(major.executionRate, 100) + '%' }"
                      ></div>
                    </div>
                  </td>
                </tr>

                <!-- 子分类明细行 -->
                <tr
                  v-for="minor in major.minors"
                  :key="minor.id"
                  class="hover:bg-gray-50"
                >
                  <td class="px-6 py-3 whitespace-nowrap pl-12">
                    <div class="flex items-center gap-2">
                      <span class="text-xs text-gray-600">{{ minor.name }}</span>
                      <span :class="[
                        'inline-block px-1.5 py-0.5 text-xs rounded',
                        minor.expenseType === 'FIXED_DAILY'
                          ? 'bg-green-100 text-green-700'
                          : 'bg-orange-100 text-orange-700'
                      ]">
                        {{ minor.expenseType === 'FIXED_DAILY' ? '固定' : '不定期' }}
                      </span>
                    </div>
                  </td>
                  <td class="px-6 py-3 whitespace-nowrap text-right text-xs text-gray-600">
                    {{ formatCurrency(minor.budget) }}
                  </td>
                  <td class="px-6 py-3 whitespace-nowrap text-right text-xs text-gray-600">
                    {{ formatCurrency(minor.actual) }}
                  </td>
                  <td class="px-6 py-3 whitespace-nowrap text-right text-xs" :class="minor.remaining >= 0 ? 'text-green-600' : 'text-red-600'">
                    {{ formatCurrency(minor.remaining) }}
                  </td>
                  <td class="px-6 py-3 whitespace-nowrap text-right text-xs text-gray-600">
                    {{ minor.executionRate }}%
                  </td>
                  <td class="px-6 py-3 whitespace-nowrap">
                    <div class="w-full bg-gray-200 rounded-full h-1.5">
                      <div
                        class="h-1.5 rounded-full transition-all"
                        :class="minor.executionRate > 100 ? 'bg-red-500' : minor.executionRate > 80 ? 'bg-yellow-500' : 'bg-green-500'"
                        :style="{ width: Math.min(minor.executionRate, 100) + '%' }"
                      ></div>
                    </div>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { useFamilyStore } from '@/stores/family'
import { exchangeRateAPI } from '@/api/exchangeRate'
import { expenseAnalysisAPI } from '@/api/expense'
import { Chart, registerables } from 'chart.js'

Chart.register(...registerables)

// 响应式数据
// Family store
const familyStore = useFamilyStore()
const selectedFamilyId = computed(() => familyStore.currentFamilyId)

const currencies = ref(['All', 'CNY', 'USD'])
const budgetExecutionData = ref([])

const selectedYear = ref(new Date().getFullYear())
const selectedCurrency = ref('All')

const loading = ref(false)
const comparisonChartCanvas = ref(null)
let comparisonChart = null

// 计算属性 - 总览数据
const totalBudget = computed(() => {
  return budgetExecutionData.value.reduce((sum, item) => sum + parseFloat(item.budgetAmount), 0)
})

const totalActual = computed(() => {
  return budgetExecutionData.value.reduce((sum, item) => sum + parseFloat(item.actualAmount), 0)
})

const totalRemaining = computed(() => {
  return totalBudget.value - totalActual.value
})

const executionRate = computed(() => {
  if (totalBudget.value === 0) return 0
  return ((totalActual.value / totalBudget.value) * 100).toFixed(1)
})

// 计算属性 - 按大类汇总
const majorCategories = computed(() => {
  const majorMap = new Map()

  budgetExecutionData.value.forEach(item => {
    if (!majorMap.has(item.majorCategoryId)) {
      majorMap.set(item.majorCategoryId, {
        id: item.majorCategoryId,
        name: item.majorCategoryName,
        icon: item.majorCategoryIcon,
        totalBudget: 0,
        totalActual: 0,
        minors: []
      })
    }

    const major = majorMap.get(item.majorCategoryId)
    major.totalBudget += parseFloat(item.budgetAmount)
    major.totalActual += parseFloat(item.actualAmount)

    const minorBudget = parseFloat(item.budgetAmount)
    const minorActual = parseFloat(item.actualAmount)
    const minorRemaining = minorBudget - minorActual

    major.minors.push({
      id: item.minorCategoryId,
      name: item.minorCategoryName,
      expenseType: item.expenseType,
      budget: minorBudget,
      actual: minorActual,
      remaining: minorRemaining,
      executionRate: minorBudget > 0 ? ((minorActual / minorBudget) * 100).toFixed(1) : 0
    })
  })

  const result = Array.from(majorMap.values()).map(major => ({
    ...major,
    totalRemaining: major.totalBudget - major.totalActual,
    executionRate: major.totalBudget > 0 ? ((major.totalActual / major.totalBudget) * 100).toFixed(1) : 0
  }))

  return result.sort((a, b) => a.id - b.id)
})

// 格式化货币
function formatCurrency(amount) {
  const currencySymbols = {
    'CNY': '¥',
    'USD': '$',
    'EUR': '€',
    'GBP': '£',
    'JPY': '¥',
    'AUD': 'A$',
    'CAD': 'C$',
    'All': '$'
  }
  const symbol = currencySymbols[selectedCurrency.value] || selectedCurrency.value + ' '
  return symbol + (amount || 0).toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}


// 加载货币列表
async function loadCurrencies() {
  try {
    const response = await exchangeRateAPI.getAllActive()
    let data = response.data

    let rates = []
    if (Array.isArray(data)) {
      rates = data
    } else if (data && data.data && Array.isArray(data.data)) {
      rates = data.data
    } else if (data && data.success && Array.isArray(data.data)) {
      rates = data.data
    }

    const currencySet = new Set(['All'])
    rates.forEach(rate => currencySet.add(rate.currency))
    currencySet.add('USD')

    currencies.value = Array.from(currencySet).sort()
  } catch (error) {
    console.error('加载货币列表失败:', error)
    currencies.value = ['All', 'USD', 'CNY', 'EUR', 'GBP', 'JPY', 'AUD', 'CAD']
  }
}

// 加载预算执行数据
async function loadBudgetExecutionData() {
  if (!selectedFamilyId.value || !selectedYear.value || !selectedCurrency.value) {
    return
  }

  loading.value = true
  try {
    const response = await expenseAnalysisAPI.getBudgetExecution(
      selectedFamilyId.value,
      selectedYear.value,
      selectedCurrency.value
    )

    if (response && response.success) {
      budgetExecutionData.value = response.data || []
    } else {
      budgetExecutionData.value = []
    }
  } catch (error) {
    console.error('加载预算执行数据失败:', error)
    budgetExecutionData.value = []
  } finally {
    loading.value = false
    // 等待loading状态更新，Canvas元素渲染
    await nextTick()
    // 再等待一次，确保计算属性完全更新
    await nextTick()
    // 使用 requestAnimationFrame 确保浏览器完成渲染
    requestAnimationFrame(() => {
      updateComparisonChart()
    })
  }
}

// 更新对比图表
function updateComparisonChart() {
  if (!comparisonChartCanvas.value) {
    console.warn('图表Canvas未准备好')
    return
  }

  const majors = majorCategories.value
  if (majors.length === 0) {
    console.warn('没有分类数据，跳过图表渲染')
    return
  }

  if (comparisonChart) {
    comparisonChart.destroy()
  }

  const ctx = comparisonChartCanvas.value.getContext('2d')

  comparisonChart = new Chart(ctx, {
    type: 'bar',
    data: {
      labels: majors.map(m => `${m.icon} ${m.name}`),
      datasets: [
        {
          label: '年度预算',
          data: majors.map(m => m.totalBudget),
          backgroundColor: '#60A5FA'
        },
        {
          label: '实际支出',
          data: majors.map(m => m.totalActual),
          backgroundColor: '#FB923C'
        }
      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { position: 'top' },
        tooltip: {
          callbacks: {
            label: (context) => {
              return `${context.dataset.label}: ${formatCurrency(context.parsed.y)}`
            }
          }
        }
      },
      scales: {
        y: {
          beginAtZero: true,
          title: {
            display: true,
            text: `金额 (${selectedCurrency.value === 'All' ? 'USD' : selectedCurrency.value})`
          },
          ticks: {
            callback: (value) => formatCurrency(value)
          }
        }
      }
    }
  })
}

// 监听选项变化
// Watch for family/year/currency changes
watch([selectedFamilyId, selectedYear, selectedCurrency], ([newFamilyId]) => {
  if (newFamilyId) {
    loadBudgetExecutionData()
  }
})

// 组件挂载时
onMounted(async () => {
  await loadCurrencies()

  // Load data if family is already available
  if (selectedFamilyId.value) {
    await loadBudgetExecutionData()
  }
})
</script>

<style scoped>
/* 自定义样式 */
</style>
