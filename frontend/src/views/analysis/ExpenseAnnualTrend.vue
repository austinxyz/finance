<template>
  <div class="p-3 md:p-6 space-y-4 md:space-y-6">
    <!-- 页面标题和控制栏 -->
    <div class="bg-white rounded-lg shadow p-3 md:p-4">
      <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-3 md:gap-0 mb-4">
        <h1 class="text-xl md:text-2xl font-bold text-gray-900">支出年度趋势分析</h1>
        <div class="flex flex-col sm:flex-row items-stretch sm:items-center gap-2 md:gap-4">
          <!-- 家庭选择 -->
          <div class="flex items-center gap-2">
            <label class="text-xs md:text-sm font-medium text-gray-700 whitespace-nowrap">选择家庭：</label>
            <select v-model.number="familyId" @change="onFamilyChange"
                    class="flex-1 sm:flex-none px-2 md:px-3 py-1.5 md:py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary text-xs md:text-sm min-w-0 sm:min-w-[180px]">
              <option v-for="family in families" :key="family.id" :value="family.id">
                {{ family.familyName }}
              </option>
            </select>
          </div>

          <!-- 年份选择 -->
          <div class="flex items-center gap-2">
            <label class="text-xs md:text-sm font-medium text-gray-700 whitespace-nowrap">显示年数：</label>
            <select v-model.number="displayYears" @change="fetchData"
                    class="flex-1 sm:flex-none px-2 md:px-3 py-1.5 md:py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary text-xs md:text-sm">
              <option :value="3">最近3年</option>
              <option :value="5">最近5年</option>
              <option :value="10">最近10年</option>
              <option :value="999">全部</option>
            </select>
          </div>

          <!-- 货币选择 -->
          <div class="flex items-center gap-2">
            <label class="text-xs md:text-sm font-medium text-gray-700 whitespace-nowrap">显示货币：</label>
            <select v-model="selectedCurrency" @change="onCurrencyChange"
                    class="flex-1 sm:flex-none px-2 md:px-3 py-1.5 md:py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary text-xs md:text-sm">
              <option value="USD">USD ($)</option>
              <option value="CNY">CNY (¥)</option>
            </select>
          </div>

          <button @click="fetchData"
                  class="px-3 md:px-4 py-1.5 md:py-2 bg-primary text-white rounded-md hover:bg-primary/90 text-xs md:text-sm font-medium whitespace-nowrap">
            刷新数据
          </button>
        </div>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="bg-white rounded-lg shadow p-12">
      <div class="flex flex-col items-center justify-center">
        <div class="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
        <p class="text-gray-600 mt-4">加载中...</p>
      </div>
    </div>

    <!-- 内容区域 -->
    <div v-else-if="trendData.length > 0" class="space-y-4">
      <!-- 图表和表格并列显示 -->
      <div class="flex flex-col lg:flex-row gap-4">
        <!-- 左侧：趋势图表 -->
        <div class="bg-white rounded-lg shadow p-3 md:p-6 flex-1 lg:w-1/2">
          <div class="mb-3 md:mb-4">
            <h2 class="text-base md:text-lg font-semibold text-gray-900">年度支出趋势</h2>
            <p class="text-xs md:text-sm text-gray-500 mt-1">基础支出和实际支出年度变化及同比增长率</p>
          </div>
          <div class="h-96 md:h-[500px] w-full">
            <canvas ref="trendChartCanvas" class="w-full h-full"></canvas>
          </div>
        </div>

        <!-- 右侧：数据表格 -->
        <div class="bg-white rounded-lg shadow p-3 md:p-6 flex-1 lg:w-1/2">
          <div class="mb-3 md:mb-4">
            <h2 class="text-base md:text-lg font-semibold text-gray-900">年度汇总表</h2>
            <p class="text-xs md:text-sm text-gray-500 mt-1">各年度支出数据对比</p>
          </div>
          <div class="overflow-y-auto max-h-96 md:max-h-[500px]">
            <table class="w-full border-separate border-spacing-0">
              <thead class="bg-gray-50 border-b border-gray-200 sticky top-0">
                <tr>
                  <th class="px-2 md:px-3 py-2 text-left text-xs md:text-sm font-medium text-gray-500 uppercase">年份</th>
                  <th class="px-2 md:px-3 py-2 text-right text-xs md:text-sm font-medium text-gray-500 uppercase">基础支出</th>
                  <th class="px-2 md:px-3 py-2 text-right text-xs md:text-sm font-medium text-gray-500 uppercase">基础同比</th>
                  <th class="px-2 md:px-3 py-2 text-right text-xs md:text-sm font-medium text-gray-500 uppercase">实际支出</th>
                  <th class="px-2 md:px-3 py-2 text-right text-xs md:text-sm font-medium text-gray-500 uppercase">实际同比</th>
                </tr>
              </thead>
              <tbody class="bg-white">
                <tr v-for="item in convertedTrendData" :key="item.year" class="hover:bg-gray-50 border-b border-gray-200">
                  <td class="px-2 md:px-3 py-2 whitespace-nowrap">
                    <div class="text-xs md:text-sm font-medium text-gray-900">{{ item.year }}</div>
                  </td>
                  <td class="px-2 md:px-3 py-2 whitespace-nowrap text-right">
                    <div class="text-xs md:text-sm font-medium text-gray-900">{{ getCurrencySymbol(selectedCurrency) }}{{ formatAmount(item.baseExpense) }}</div>
                  </td>
                  <td class="px-2 md:px-3 py-2 whitespace-nowrap text-right">
                    <div v-if="item.yoyBaseChange !== null" class="text-xs md:text-sm">
                      <div :class="getChangeColor(item.yoyBaseChange)" class="font-medium">
                        {{ formatChange(item.yoyBaseChange) }}
                      </div>
                      <div :class="getChangeColor(item.yoyBaseChangePct)" class="text-[10px] md:text-xs">
                        ({{ formatPercent(item.yoyBaseChangePct) }})
                      </div>
                    </div>
                    <div v-else class="text-xs md:text-sm text-gray-400">基准年</div>
                  </td>
                  <td class="px-2 md:px-3 py-2 whitespace-nowrap text-right">
                    <div class="text-xs md:text-sm font-bold text-blue-600">{{ getCurrencySymbol(selectedCurrency) }}{{ formatAmount(item.actualExpense) }}</div>
                  </td>
                  <td class="px-2 md:px-3 py-2 whitespace-nowrap text-right">
                    <div v-if="item.yoyActualChange !== null" class="text-xs md:text-sm">
                      <div :class="getChangeColor(item.yoyActualChange)" class="font-medium">
                        {{ formatChange(item.yoyActualChange) }}
                      </div>
                      <div :class="getChangeColor(item.yoyActualChangePct)" class="text-[10px] md:text-xs">
                        ({{ formatPercent(item.yoyActualChangePct) }})
                      </div>
                    </div>
                    <div v-else class="text-xs md:text-sm text-gray-400">基准年</div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- 大类趋势图 -->
      <div v-if="categoryData.length > 0" class="bg-white rounded-lg shadow p-3 md:p-6">
        <div class="mb-3 md:mb-4">
          <h2 class="text-base md:text-lg font-semibold text-gray-900">各大类支出趋势</h2>
          <p class="text-xs md:text-sm text-gray-500 mt-1">各支出大类实际支出年度变化对比（已调整资产负债）</p>

          <!-- 大类过滤选择器 -->
          <div class="mt-3 p-3 bg-gray-50 rounded-lg border border-gray-200">
            <div class="flex items-center justify-between mb-2">
              <span class="text-xs md:text-sm font-medium text-gray-700">显示大类：</span>
              <div class="flex gap-2">
                <button @click="selectAllCategories"
                        class="text-xs px-2 py-1 text-primary hover:bg-primary/10 rounded">
                  全选
                </button>
                <button @click="deselectAllCategories"
                        class="text-xs px-2 py-1 text-gray-600 hover:bg-gray-200 rounded">
                  清空
                </button>
              </div>
            </div>
            <div class="flex flex-wrap gap-2">
              <label v-for="category in categoryData" :key="category.majorCategoryId"
                     class="inline-flex items-center gap-1.5 px-2 md:px-3 py-1.5 bg-white border border-gray-300 rounded-md hover:bg-gray-50 cursor-pointer text-xs md:text-sm">
                <input type="checkbox"
                       :value="category.majorCategoryId"
                       v-model="selectedCategories"
                       @change="onCategoryFilterChange"
                       class="w-3 h-3 md:w-4 md:h-4 text-primary focus:ring-2 focus:ring-primary rounded">
                <span class="text-base">{{ category.majorCategoryIcon }}</span>
                <span class="text-gray-700">{{ category.majorCategoryName }}</span>
              </label>
            </div>
          </div>
        </div>
        <div class="h-96 md:h-[500px] w-full">
          <canvas ref="categoryTrendChartCanvas" class="w-full h-full"></canvas>
        </div>
      </div>
    </div>

    <!-- 无数据提示 -->
    <div v-else class="bg-white rounded-lg shadow border border-gray-200 p-12 text-center">
      <div class="text-gray-400 mb-2">
        <svg class="mx-auto h-12 w-12" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
        </svg>
      </div>
      <h3 class="text-lg font-medium text-gray-900 mb-2">暂无年度支出数据</h3>
      <p class="text-gray-600">请先添加支出记录并计算年度汇总</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, computed, watch } from 'vue'
import { Chart, registerables } from 'chart.js'
import { expenseAnalysisAPI } from '@/api/expense'
import { exchangeRateAPI } from '@/api/exchangeRate'
import familyAPI from '@/api/family'

Chart.register(...registerables)

// 数据
const trendData = ref([])
const categoryData = ref([])
const families = ref([])
const loading = ref(false)
const displayYears = ref(5)
const familyId = ref(null)
const selectedCurrency = ref('USD')
const selectedCategories = ref([])
const exchangeRates = ref([]) // 汇率数据

// 图表引用
const trendChartCanvas = ref(null)
const categoryTrendChartCanvas = ref(null)

// 图表实例
let trendChart = null
let categoryTrendChart = null

// 计算属性：转换后的趋势数据（用于表格显示）
const convertedTrendData = computed(() => {
  return trendData.value.map(item => ({
    ...item,
    baseExpense: convertCurrency(item.baseExpense),
    actualExpense: convertCurrency(item.actualExpense),
    yoyBaseChange: item.yoyBaseChange ? convertCurrency(item.yoyBaseChange) : null,
    yoyActualChange: item.yoyActualChange ? convertCurrency(item.yoyActualChange) : null
  }))
})

// 获取家庭列表
const fetchFamilies = async () => {
  try {
    const response = await familyAPI.getAll()
    families.value = response.data

    // 如果familyId还未设置，获取默认家庭
    if (!familyId.value) {
      try {
        const defaultResponse = await familyAPI.getDefault()
        if (defaultResponse.success && defaultResponse.data) {
          familyId.value = defaultResponse.data.id
        } else if (families.value.length > 0) {
          familyId.value = families.value[0].id
        }
      } catch (err) {
        console.error('获取默认家庭失败:', err)
        if (families.value.length > 0) {
          familyId.value = families.value[0].id
        }
      }
    }
  } catch (error) {
    console.error('获取家庭列表失败:', error)
  }
}

// 家庭切换事件
const onFamilyChange = () => {
  fetchData()
}

// 货币切换事件
const onCurrencyChange = () => {
  // 货币切换时只需要重新渲染图表，不需要重新获取数据
  renderChart()
  renderCategoryTrendChart()
}

// 获取汇率数据
const fetchExchangeRates = async () => {
  try {
    const response = await exchangeRateAPI.getAllActive()
    if (response.success && response.data) {
      exchangeRates.value = response.data
    } else if (response.data && Array.isArray(response.data)) {
      exchangeRates.value = response.data
    }
  } catch (error) {
    console.error('获取汇率数据失败:', error)
    exchangeRates.value = []
  }
}

// 获取指定货币的汇率（USD为基准）
const getExchangeRate = (currency) => {
  if (currency === 'USD') return 1
  const rate = exchangeRates.value.find(r => r.currency === currency)
  return rate ? rate.rateToUsd : 1
}

// 将USD金额转换为选中货币
const convertCurrency = (usdAmount) => {
  if (!usdAmount) return 0
  const rate = getExchangeRate(selectedCurrency.value)
  if (selectedCurrency.value === 'USD') {
    return Number(usdAmount)
  }
  // USD转其他货币：USD金额 / 汇率
  return Number(usdAmount) / rate
}

// 获取数据
const fetchData = async () => {
  if (!familyId.value) return

  loading.value = true
  try {
    // 并行获取总支出趋势和大类趋势数据（始终获取USD数据）
    const [trendResponse, categoryResponse] = await Promise.all([
      expenseAnalysisAPI.getAnnualTrend(
        familyId.value,
        displayYears.value,
        'USD'  // 始终获取USD基准货币数据
      ),
      expenseAnalysisAPI.getAnnualCategoryTrend(
        familyId.value,
        displayYears.value,
        'USD'  // 始终获取USD基准货币数据
      )
    ])

    if (trendResponse.success && trendResponse.data) {
      trendData.value = trendResponse.data.sort((a, b) => b.year - a.year)
    }

    if (categoryResponse.success && categoryResponse.data) {
      categoryData.value = categoryResponse.data
      // 默认全选所有大类
      selectedCategories.value = categoryResponse.data.map(cat => cat.majorCategoryId)
    }
  } catch (error) {
    console.error('获取年度支出趋势数据失败:', error)
  } finally {
    loading.value = false
  }
}

// 渲染图表
const renderChart = () => {
  if (trendData.value.length === 0) return

  const sortedData = [...trendData.value].reverse() // 从旧到新排序
  const years = sortedData.map(d => d.year)
  // 将USD金额转换为选中货币
  const baseExpenses = sortedData.map(d => convertCurrency(d.baseExpense))
  const actualExpenses = sortedData.map(d => convertCurrency(d.actualExpense))
  const baseGrowths = sortedData.map(d => d.yoyBaseChangePct ? Number(d.yoyBaseChangePct) : 0)
  const actualGrowths = sortedData.map(d => d.yoyActualChangePct ? Number(d.yoyActualChangePct) : 0)

  const currencySymbol = getCurrencySymbol(selectedCurrency.value)

  // 趋势图（双Y轴）
  if (trendChart) trendChart.destroy()
  if (!trendChartCanvas.value) {
    console.error('Canvas element not found')
    return
  }

  trendChart = new Chart(trendChartCanvas.value, {
      type: 'bar',
      data: {
        labels: years,
        datasets: [
          {
            label: '基础支出',
            data: baseExpenses,
            backgroundColor: 'rgba(99, 102, 241, 0.7)',
            borderColor: 'rgb(99, 102, 241)',
            borderWidth: 1,
            yAxisID: 'y'
          },
          {
            label: '实际支出',
            data: actualExpenses,
            backgroundColor: 'rgba(239, 68, 68, 0.7)',
            borderColor: 'rgb(239, 68, 68)',
            borderWidth: 1,
            yAxisID: 'y'
          },
          {
            label: '基础支出同比',
            data: baseGrowths,
            type: 'line',
            borderColor: 'rgb(59, 130, 246)',
            backgroundColor: 'rgba(59, 130, 246, 0.1)',
            borderWidth: 2,
            tension: 0.4,
            yAxisID: 'y1'
          },
          {
            label: '实际支出同比',
            data: actualGrowths,
            type: 'line',
            borderColor: 'rgb(234, 88, 12)',
            backgroundColor: 'rgba(234, 88, 12, 0.1)',
            borderWidth: 2,
            tension: 0.4,
            yAxisID: 'y1'
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        interaction: {
          mode: 'index',
          intersect: false,
        },
        plugins: {
          legend: {
            position: window.innerWidth < 768 ? 'bottom' : 'top',
            labels: {
              font: {
                size: window.innerWidth < 768 ? 10 : 12,
                weight: 'bold'
              },
              padding: window.innerWidth < 768 ? 6 : 10,
              boxWidth: window.innerWidth < 768 ? 20 : 40
            }
          },
          tooltip: {
            callbacks: {
              label: function(context) {
                if (context.dataset.label.includes('同比')) {
                  return context.dataset.label + ': ' + context.parsed.y.toFixed(2) + '%'
                } else {
                  return context.dataset.label + ': ' + currencySymbol + Number(context.parsed.y).toLocaleString('en-US', {
                    minimumFractionDigits: 0,
                    maximumFractionDigits: 2
                  })
                }
              }
            },
            titleFont: {
              size: window.innerWidth < 768 ? 11 : 12,
              weight: 'bold'
            },
            bodyFont: {
              size: window.innerWidth < 768 ? 10 : 12,
              weight: 'bold'
            },
            padding: window.innerWidth < 768 ? 6 : 10
          }
        },
        scales: {
          y: {
            type: 'linear',
            display: true,
            position: 'left',
            beginAtZero: true,
            ticks: {
              callback: function(value) {
                if (value >= 1000000) {
                  return currencySymbol + (value / 1000000).toFixed(1) + 'M'
                } else if (value >= 1000) {
                  return currencySymbol + (value / 1000).toFixed(1) + 'K'
                }
                return currencySymbol + value.toFixed(0)
              },
              font: {
                size: window.innerWidth < 768 ? 9 : 11,
                weight: 'bold'
              }
            }
          },
          y1: {
            type: 'linear',
            display: true,
            position: 'right',
            grid: {
              drawOnChartArea: false,
            },
            ticks: {
              callback: function(value) {
                return value.toFixed(1) + '%'
              },
              font: {
                size: window.innerWidth < 768 ? 9 : 11,
                weight: 'bold'
              }
            }
          },
          x: {
            ticks: {
              font: {
                size: window.innerWidth < 768 ? 9 : 11,
                weight: 'bold'
              }
            }
          }
        }
      }
    })
}

// 渲染大类趋势图
const renderCategoryTrendChart = () => {
  if (categoryData.value.length === 0) return

  if (!categoryTrendChartCanvas.value) {
    console.error('Category trend canvas element not found')
    return
  }

  // 过滤出选中的大类
  const filteredCategories = categoryData.value.filter(category =>
    selectedCategories.value.includes(category.majorCategoryId)
  )

  // 如果没有选中任何大类，则不渲染
  if (filteredCategories.length === 0) {
    if (categoryTrendChart) categoryTrendChart.destroy()
    return
  }

  // 准备所有年份（从选中的大类中提取）
  const allYearsSet = new Set()
  filteredCategories.forEach(category => {
    category.yearlyData.forEach(item => {
      allYearsSet.add(item.year)
    })
  })
  const years = Array.from(allYearsSet).sort((a, b) => a - b)

  const currencySymbol = getCurrencySymbol(selectedCurrency.value)

  // 为每个大类生成颜色
  const colors = [
    'rgb(99, 102, 241)',   // 紫色
    'rgb(239, 68, 68)',    // 红色
    'rgb(34, 197, 94)',    // 绿色
    'rgb(234, 88, 12)',    // 橙色
    'rgb(59, 130, 246)',   // 蓝色
    'rgb(236, 72, 153)',   // 粉色
    'rgb(168, 85, 247)',   // 紫罗兰
    'rgb(20, 184, 166)',   // 青色
    'rgb(251, 191, 36)',   // 黄色
    'rgb(161, 161, 170)'   // 灰色
  ]

  // 为每个选中的大类创建数据集
  const datasets = filteredCategories.map((category, index) => {
    // 创建年份到金额的映射（并转换为选中货币）
    const yearToExpense = {}
    category.yearlyData.forEach(item => {
      yearToExpense[item.year] = convertCurrency(item.actualExpense)
    })

    // 按所有年份创建数据数组
    const data = years.map(year => yearToExpense[year] || null)

    const color = colors[index % colors.length]

    return {
      label: `${category.majorCategoryIcon} ${category.majorCategoryName}`,
      data: data,
      borderColor: color,
      backgroundColor: color.replace('rgb', 'rgba').replace(')', ', 0.1)'),
      borderWidth: 2,
      tension: 0.4,
      spanGaps: true  // 连接断点
    }
  })

  // 销毁旧图表
  if (categoryTrendChart) categoryTrendChart.destroy()

  // 创建新图表
  categoryTrendChart = new Chart(categoryTrendChartCanvas.value, {
    type: 'line',
    data: {
      labels: years,
      datasets: datasets
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      interaction: {
        mode: 'index',
        intersect: false,
      },
      plugins: {
        legend: {
          position: window.innerWidth < 768 ? 'bottom' : 'top',
          labels: {
            font: {
              size: window.innerWidth < 768 ? 10 : 12,
              weight: 'bold'
            },
            padding: window.innerWidth < 768 ? 6 : 10,
            boxWidth: window.innerWidth < 768 ? 20 : 40,
            usePointStyle: true
          }
        },
        tooltip: {
          callbacks: {
            label: function(context) {
              return context.dataset.label + ': ' + currencySymbol + Number(context.parsed.y).toLocaleString('en-US', {
                minimumFractionDigits: 0,
                maximumFractionDigits: 2
              })
            }
          },
          titleFont: {
            size: window.innerWidth < 768 ? 11 : 12,
            weight: 'bold'
          },
          bodyFont: {
            size: window.innerWidth < 768 ? 10 : 12,
            weight: 'bold'
          },
          padding: window.innerWidth < 768 ? 6 : 10
        }
      },
      scales: {
        y: {
          beginAtZero: true,
          ticks: {
            callback: function(value) {
              if (value >= 1000000) {
                return currencySymbol + (value / 1000000).toFixed(1) + 'M'
              } else if (value >= 1000) {
                return currencySymbol + (value / 1000).toFixed(1) + 'K'
              }
              return currencySymbol + value.toFixed(0)
            },
            font: {
              size: window.innerWidth < 768 ? 9 : 11,
              weight: 'bold'
            }
          }
        },
        x: {
          ticks: {
            font: {
              size: window.innerWidth < 768 ? 9 : 11,
              weight: 'bold'
            }
          }
        }
      }
    }
  })
}

// 货币符号映射
const getCurrencySymbol = (currency) => {
  const symbols = {
    'USD': '$',
    'CNY': '¥',
    'EUR': '€',
    'GBP': '£',
    'JPY': '¥',
    'AUD': 'A$',
    'CAD': 'C$'
  }
  return symbols[currency] || currency
}

// 格式化金额
const formatAmount = (amount) => {
  if (!amount && amount !== 0) return '0.00'
  return Number(amount).toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

// 格式化变化金额
const formatChange = (amount) => {
  if (!amount && amount !== 0) return '-'
  const prefix = amount > 0 ? '+' : ''
  const symbol = getCurrencySymbol(selectedCurrency.value)
  return prefix + symbol + formatAmount(Math.abs(amount))
}

// 格式化百分比
const formatPercent = (percent) => {
  if (!percent && percent !== 0) return '-'
  const prefix = percent > 0 ? '+' : ''
  return prefix + Number(percent).toFixed(2) + '%'
}

// 获取变化颜色
const getChangeColor = (value) => {
  if (!value && value !== 0) return 'text-gray-400'
  // 对于支出，增加是不好的（红色），减少是好的（绿色）
  return value > 0 ? 'text-red-600' : 'text-green-600'
}

// 全选所有大类
const selectAllCategories = () => {
  selectedCategories.value = categoryData.value.map(cat => cat.majorCategoryId)
}

// 清空所有大类选择
const deselectAllCategories = () => {
  selectedCategories.value = []
}

// 大类过滤器变化事件
const onCategoryFilterChange = () => {
  // 重新渲染图表
  if (categoryTrendChartCanvas.value) {
    renderCategoryTrendChart()
  }
}

// 监听数据变化，当数据加载且 canvas 可用时渲染图表
watch([trendData, trendChartCanvas], async () => {
  if (trendData.value.length > 0 && trendChartCanvas.value) {
    // 等待 DOM 更新完成
    await nextTick()
    // 再次确认 canvas 存在（处理条件渲染的情况）
    if (trendChartCanvas.value) {
      renderChart()
    }
  }
}, { flush: 'post' }) // 使用 post flush 确保 DOM 更新后执行

// 监听大类趋势数据变化，当数据加载且 canvas 可用时渲染大类趋势图表
watch([categoryData, categoryTrendChartCanvas], async () => {
  if (categoryData.value.length > 0 && categoryTrendChartCanvas.value) {
    // 等待 DOM 更新完成
    await nextTick()
    // 再次确认 canvas 存在（处理条件渲染的情况）
    if (categoryTrendChartCanvas.value) {
      renderCategoryTrendChart()
    }
  }
}, { flush: 'post' }) // 使用 post flush 确保 DOM 更新后执行

// 组件挂载时获取数据
onMounted(async () => {
  await fetchFamilies()
  await fetchExchangeRates()  // 获取汇率数据
  await fetchData()
})
</script>
