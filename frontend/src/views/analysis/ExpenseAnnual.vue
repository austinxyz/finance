<template>
  <div class="p-6 space-y-6">
    <!-- 页面标题和控制 -->
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">年度支出分析</h1>
        <p class="text-sm text-gray-600 mt-1">分析年度支出结构和趋势</p>
      </div>
      <div class="flex items-center gap-4">
        <!-- 家庭选择 -->
        <select
          v-model="selectedFamilyId"
          class="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent"
        >
          <option v-for="family in families" :key="family.id" :value="family.id">
            {{ family.familyName }}
          </option>
        </select>

        <!-- 年份选择 -->
        <select
          v-model="selectedYear"
          class="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent"
        >
          <option v-for="year in availableYears" :key="year" :value="year">
            {{ year }}年
          </option>
        </select>

        <!-- 货币选择 -->
        <select
          v-model="selectedCurrency"
          class="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent"
        >
          <option v-for="currency in currencies" :key="currency" :value="currency">
            {{ currency }}
          </option>
        </select>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="text-center py-12">
      <div class="text-gray-500">加载中...</div>
    </div>

    <!-- 主内容 -->
    <div v-else class="grid grid-cols-12 gap-6 auto-rows-min">
      <!-- 左侧：大类饼图和表格（跨两行） -->
      <div class="col-span-12 lg:col-span-6 lg:row-span-2 bg-white rounded-lg shadow border border-gray-200 p-6 flex flex-col" style="min-height: 800px;">
        <h3 class="text-lg font-semibold mb-4">大类分布</h3>

        <!-- 大类饼图 -->
        <div class="mb-6 flex-shrink-0" style="height: 400px;">
          <canvas ref="majorCategoryChartCanvas"></canvas>
        </div>

        <!-- 大类表格 -->
        <div class="overflow-auto flex-1">
          <table class="min-w-full divide-y divide-gray-200">
            <thead class="bg-gray-50 sticky top-0">
              <tr>
                <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">分类</th>
                <th class="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">金额</th>
                <th class="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">占比</th>
              </tr>
            </thead>
            <tbody class="bg-white divide-y divide-gray-200">
              <tr
                v-for="item in majorCategoryData"
                :key="item.majorCategoryId"
                @click="selectMajorCategory(item)"
                class="cursor-pointer hover:bg-gray-50 transition-colors"
              >
                <td class="px-4 py-3 whitespace-nowrap">
                  <span class="text-xl mr-2">{{ item.majorCategoryIcon }}</span>
                  {{ item.majorCategoryName }}
                </td>
                <td class="px-4 py-3 text-right font-medium">
                  {{ formatCurrency(item.totalAmount) }}
                </td>
                <td class="px-4 py-3 text-right text-gray-600">
                  {{ ((item.totalAmount / totalExpense) * 100).toFixed(1) }}%
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 右侧上：小类饼图和表格（点击大类后显示） -->
      <div v-if="selectedMajorCategoryId" class="col-span-12 lg:col-span-6 bg-white rounded-lg shadow border border-gray-200 p-6">
        <h3 class="text-lg font-semibold mb-3">{{ selectedMajorCategoryName }} - 小类分布</h3>

        <!-- 饼图和表格横向排列 -->
        <div class="flex gap-4">
          <!-- 左侧：小类饼图 -->
          <div class="flex-shrink-0" style="width: 280px; height: 280px;">
            <canvas ref="minorCategoryChartCanvas"></canvas>
          </div>

          <!-- 右侧：小类表格 -->
          <div class="flex-1 overflow-auto" style="max-height: 280px;">
            <table class="min-w-full divide-y divide-gray-200">
              <thead class="bg-gray-50 sticky top-0">
                <tr>
                  <th class="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">子分类</th>
                  <th class="px-3 py-2 text-right text-xs font-medium text-gray-500 uppercase">金额</th>
                </tr>
              </thead>
              <tbody class="bg-white divide-y divide-gray-200">
                <tr
                  v-for="item in minorCategoryData"
                  :key="item.minorCategoryId"
                  @click="selectMinorCategory(item)"
                  class="cursor-pointer hover:bg-gray-50 transition-colors"
                >
                  <td class="px-3 py-2">
                    <div class="whitespace-nowrap font-medium">{{ item.minorCategoryName }}</div>
                    <div class="text-xs text-gray-500 mt-1">
                      <span
                        :class="{
                          'px-2 py-0.5 rounded-full': true,
                          'bg-green-100 text-green-700': item.expenseType === 'FIXED_DAILY',
                          'bg-orange-100 text-orange-700': item.expenseType === 'LARGE_IRREGULAR'
                        }"
                      >
                        {{ item.expenseType === 'FIXED_DAILY' ? '固定' : '不定期' }}
                      </span>
                      <span class="ml-2">{{ ((item.totalAmount / minorCategoryTotal) * 100).toFixed(1) }}%</span>
                    </div>
                  </td>
                  <td class="px-3 py-2 text-right font-medium whitespace-nowrap">
                    {{ formatCurrency(item.totalAmount) }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- 右侧下：月度趋势柱状图（点击小类后显示） -->
      <div v-if="selectedMinorCategoryId" class="col-span-12 lg:col-span-6 bg-white rounded-lg shadow border border-gray-200 p-6">
        <h3 class="text-lg font-semibold mb-3">{{ selectedMinorCategoryName }} - 月度趋势</h3>
        <div style="height: 300px;">
          <canvas ref="monthlyTrendChartCanvas"></canvas>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, watch } from 'vue'
import { Chart, registerables } from 'chart.js'
import { familyAPI } from '@/api/family'
import { exchangeRateAPI } from '@/api/exchangeRate'
import { expenseAnalysisAPI } from '@/api/expense'

Chart.register(...registerables)

export default {
  name: 'ExpenseAnnual',
  setup() {
    // 响应式数据
    const families = ref([])
    const selectedFamilyId = ref(null)
    const selectedYear = ref(new Date().getFullYear())
    const currencies = ref(['All'])
    const selectedCurrency = ref('USD')
    const loading = ref(false)

    // 大类数据
    const majorCategoryData = ref([])
    const selectedMajorCategoryId = ref(null)
    const selectedMajorCategoryName = ref('')

    // 小类数据
    const minorCategoryData = ref([])
    const selectedMinorCategoryId = ref(null)
    const selectedMinorCategoryName = ref('')

    // 月度趋势数据
    const monthlyTrendData = ref([])

    // Chart实例
    const majorCategoryChart = ref(null)
    const minorCategoryChart = ref(null)
    const monthlyTrendChart = ref(null)

    // Canvas refs
    const majorCategoryChartCanvas = ref(null)
    const minorCategoryChartCanvas = ref(null)
    const monthlyTrendChartCanvas = ref(null)

    // 计算属性
    const availableYears = computed(() => {
      const currentYear = new Date().getFullYear()
      const years = []
      for (let year = currentYear; year >= currentYear - 10; year--) {
        years.push(year)
      }
      return years
    })

    const totalExpense = computed(() => {
      return majorCategoryData.value.reduce((sum, item) => sum + parseFloat(item.totalAmount), 0)
    })

    const minorCategoryTotal = computed(() => {
      return minorCategoryData.value.reduce((sum, item) => sum + parseFloat(item.totalAmount), 0)
    })

    // 加载家庭列表
    const loadFamilies = async () => {
      try {
        const response = await familyAPI.getAll()
        let data = response.data

        if (Array.isArray(data)) {
          families.value = data
        } else if (data && data.data && Array.isArray(data.data)) {
          families.value = data.data
        } else if (data && data.success && Array.isArray(data.data)) {
          families.value = data.data
        }

        if (families.value.length > 0) {
          selectedFamilyId.value = families.value[0].id
        }
      } catch (error) {
        console.error('加载家庭列表失败:', error)
      }
    }

    // 加载货币列表
    const loadCurrencies = async () => {
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
      }
    }

    // 加载大类汇总数据
    const loadMajorCategoryData = async () => {
      if (!selectedFamilyId.value) return

      loading.value = true
      try {
        const response = await expenseAnalysisAPI.getAnnualMajorCategories(
          selectedFamilyId.value,
          selectedYear.value,
          selectedCurrency.value
        )

        if (response && response.success) {
          majorCategoryData.value = response.data || []
        } else {
          majorCategoryData.value = []
        }

        // 清空小类和月度数据
        selectedMajorCategoryId.value = null
        selectedMinorCategoryId.value = null
        minorCategoryData.value = []
        monthlyTrendData.value = []

        // 更新大类饼图
        setTimeout(() => updateMajorCategoryChart(), 100)
      } catch (error) {
        console.error('加载大类数据失败:', error)
        majorCategoryData.value = []
      } finally {
        loading.value = false
      }
    }

    // 选择大类
    const selectMajorCategory = async (item) => {
      selectedMajorCategoryId.value = item.majorCategoryId
      selectedMajorCategoryName.value = item.majorCategoryName

      // 清空小类选择
      selectedMinorCategoryId.value = null
      monthlyTrendData.value = []

      try {
        const response = await expenseAnalysisAPI.getAnnualMinorCategories(
          selectedFamilyId.value,
          selectedYear.value,
          item.majorCategoryId,
          selectedCurrency.value
        )

        if (response && response.success) {
          minorCategoryData.value = response.data || []
        } else {
          minorCategoryData.value = []
        }

        // 更新小类饼图
        setTimeout(() => updateMinorCategoryChart(), 100)
      } catch (error) {
        console.error('加载小类数据失败:', error)
        minorCategoryData.value = []
      }
    }

    // 选择小类
    const selectMinorCategory = async (item) => {
      selectedMinorCategoryId.value = item.minorCategoryId
      selectedMinorCategoryName.value = item.minorCategoryName

      try {
        const response = await expenseAnalysisAPI.getAnnualMonthlyTrend(
          selectedFamilyId.value,
          selectedYear.value,
          item.minorCategoryId,
          selectedCurrency.value
        )

        if (response && response.success) {
          monthlyTrendData.value = response.data || []
        } else {
          monthlyTrendData.value = []
        }

        // 更新月度趋势图
        setTimeout(() => updateMonthlyTrendChart(), 100)
      } catch (error) {
        console.error('加载月度趋势失败:', error)
        monthlyTrendData.value = []
      }
    }

    // 更新大类饼图
    const updateMajorCategoryChart = () => {
      if (!majorCategoryChartCanvas.value) return

      if (majorCategoryChart.value) {
        majorCategoryChart.value.destroy()
      }

      const ctx = majorCategoryChartCanvas.value.getContext('2d')
      majorCategoryChart.value = new Chart(ctx, {
        type: 'pie',
        data: {
          labels: majorCategoryData.value.map(d => `${d.majorCategoryIcon} ${d.majorCategoryName}`),
          datasets: [{
            data: majorCategoryData.value.map(d => d.totalAmount),
            backgroundColor: [
              '#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4', '#FFEAA7',
              '#DFE6E9', '#74B9FF', '#A29BFE', '#FD79A8', '#FDCB6E'
            ]
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: true,
          plugins: {
            legend: { position: 'bottom' },
            tooltip: {
              callbacks: {
                label: (context) => {
                  const value = context.parsed
                  return ` ${formatChartAmount(value)}`
                }
              }
            }
          }
        }
      })
    }

    // 更新小类饼图
    const updateMinorCategoryChart = () => {
      if (!minorCategoryChartCanvas.value) return

      if (minorCategoryChart.value) {
        minorCategoryChart.value.destroy()
      }

      const ctx = minorCategoryChartCanvas.value.getContext('2d')
      minorCategoryChart.value = new Chart(ctx, {
        type: 'pie',
        data: {
          labels: minorCategoryData.value.map(d => d.minorCategoryName),
          datasets: [{
            data: minorCategoryData.value.map(d => d.totalAmount),
            backgroundColor: [
              '#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4', '#FFEAA7',
              '#DFE6E9', '#74B9FF', '#A29BFE', '#FD79A8', '#FDCB6E'
            ]
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: true,
          plugins: {
            legend: { position: 'bottom' },
            tooltip: {
              callbacks: {
                label: (context) => {
                  const value = context.parsed
                  return ` ${formatChartAmount(value)}`
                }
              }
            }
          }
        }
      })
    }

    // 更新月度趋势图
    const updateMonthlyTrendChart = () => {
      if (!monthlyTrendChartCanvas.value) return

      if (monthlyTrendChart.value) {
        monthlyTrendChart.value.destroy()
      }

      const ctx = monthlyTrendChartCanvas.value.getContext('2d')
      monthlyTrendChart.value = new Chart(ctx, {
        type: 'bar',
        data: {
          labels: monthlyTrendData.value.map(d => `${d.month}月`),
          datasets: [{
            label: '支出金额',
            data: monthlyTrendData.value.map(d => d.amount),
            backgroundColor: '#4ECDC4'
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: true,
          plugins: {
            legend: { display: false },
            tooltip: {
              callbacks: {
                label: (context) => {
                  return `金额: ${formatCurrency(context.parsed.y)}`
                }
              }
            }
          },
          scales: {
            y: {
              beginAtZero: true,
              title: {
                display: true,
                text: `金额 (${getCurrencySymbol()})`
              },
              ticks: {
                callback: (value) => getCurrencySymbol() + value.toLocaleString('en-US')
              }
            }
          }
        }
      })
    }

    // 格式化金额
    const formatCurrency = (amount) => {
      const symbol = getCurrencySymbol()
      return symbol + parseFloat(amount).toLocaleString('en-US', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
      })
    }

    // 格式化图表金额（K/M格式）
    const formatChartAmount = (value) => {
      const symbol = getCurrencySymbol()
      if (value >= 1000000) {
        return symbol + (value / 1000000).toFixed(2) + 'M'
      } else if (value >= 1000) {
        return symbol + (value / 1000).toFixed(2) + 'K'
      } else {
        return symbol + value.toFixed(2)
      }
    }

    // 获取货币符号
    const getCurrencySymbol = () => {
      const symbols = {
        'All': '$',
        'USD': '$',
        'CNY': '¥',
        'EUR': '€',
        'GBP': '£',
        'JPY': '¥',
        'AUD': 'A$',
        'CAD': 'C$'
      }
      return symbols[selectedCurrency.value] || '$'
    }

    // 监听选项变化
    watch([selectedFamilyId, selectedYear, selectedCurrency], () => {
      loadMajorCategoryData()
    })

    // 组件挂载时
    onMounted(async () => {
      await loadFamilies()
      await loadCurrencies()
      await loadMajorCategoryData()
    })

    return {
      families,
      selectedFamilyId,
      selectedYear,
      availableYears,
      currencies,
      selectedCurrency,
      loading,
      majorCategoryData,
      minorCategoryData,
      monthlyTrendData,
      selectedMajorCategoryId,
      selectedMajorCategoryName,
      selectedMinorCategoryId,
      selectedMinorCategoryName,
      totalExpense,
      minorCategoryTotal,
      majorCategoryChartCanvas,
      minorCategoryChartCanvas,
      monthlyTrendChartCanvas,
      selectMajorCategory,
      selectMinorCategory,
      formatCurrency
    }
  }
}
</script>

<style scoped>
/* 自定义样式 */
</style>
