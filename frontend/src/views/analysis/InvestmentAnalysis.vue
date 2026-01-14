<template>
  <div class="p-3 md:p-6 space-y-4 md:space-y-6">
    <!-- 页面标题和筛选控制区 -->
    <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
      <div>
        <h1 class="text-xl md:text-2xl font-bold text-gray-900">年度投资分析</h1>
        <p class="text-xs md:text-sm text-gray-600 mt-1">分析年度投资结构和净投入情况</p>
      </div>
      <div class="flex flex-wrap items-center gap-2">
        <label class="text-sm font-medium text-gray-700 whitespace-nowrap">年份:</label>
        <select
          v-model="selectedYear"
          @change="onFilterChange"
          class="px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-primary bg-white text-sm"
        >
          <option v-for="year in availableYears" :key="year" :value="year">
            {{ year }}年
          </option>
        </select>

        <label class="text-sm font-medium text-gray-700 whitespace-nowrap">币种:</label>
        <select
          v-model="selectedCurrency"
          @change="onFilterChange"
          class="px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-primary bg-white text-sm"
        >
          <option value="USD">USD</option>
          <option value="CNY">CNY</option>
          <option value="All">All (折算为USD)</option>
        </select>
      </div>
    </div>

    <!-- 投资总览汇总卡片 -->
    <div v-if="majorCategoryData.length > 0" class="bg-gradient-to-r from-blue-50 to-indigo-50 rounded-lg shadow border border-blue-200 p-4">
      <h3 class="text-base font-semibold text-gray-900 mb-3">投资总览</h3>
      <div class="grid grid-cols-2 md:grid-cols-5 gap-3">
        <div class="bg-white rounded-lg p-3 shadow-sm">
          <div class="text-xs text-gray-600 mb-1">上一年值</div>
          <div class="text-lg font-bold text-gray-900">{{ formatCurrency(totalSummary.lastYearEndAssets) }}</div>
        </div>
        <div class="bg-white rounded-lg p-3 shadow-sm">
          <div class="text-xs text-gray-600 mb-1">当前年值</div>
          <div class="text-lg font-bold text-gray-900">{{ formatCurrency(totalSummary.currentAssets) }}</div>
        </div>
        <div class="bg-white rounded-lg p-3 shadow-sm">
          <div class="text-xs text-gray-600 mb-1">净投入</div>
          <div class="text-lg font-bold text-blue-600">{{ formatCurrency(totalSummary.netDeposits) }}</div>
        </div>
        <div class="bg-white rounded-lg p-3 shadow-sm">
          <div class="text-xs text-gray-600 mb-1">投资回报</div>
          <div class="text-lg font-bold" :class="totalSummary.returns >= 0 ? 'text-green-600' : 'text-red-600'">
            {{ formatCurrency(totalSummary.returns) }}
          </div>
        </div>
        <div class="bg-white rounded-lg p-3 shadow-sm">
          <div class="text-xs text-gray-600 mb-1">回报率</div>
          <div class="text-lg font-bold" :class="totalSummary.returnRate >= 0 ? 'text-green-600' : 'text-red-600'">
            {{ totalSummary.returnRate != null ? totalSummary.returnRate.toFixed(2) + '%' : 'N/A' }}
          </div>
        </div>
      </div>
    </div>

    <!-- 大类分布：图表和表格 -->
    <div class="bg-white rounded-lg shadow border border-gray-200 p-4">
      <h3 class="text-base font-semibold text-gray-900 mb-3">大类分布</h3>

      <!-- 图表区域：资产配置饼图 + 回报率柱状图 -->
      <div v-if="majorCategoryData.length > 0" class="grid grid-cols-1 lg:grid-cols-2 gap-4 mb-4">
        <!-- 左侧：资产配置饼图 -->
        <div class="flex flex-col">
          <h4 class="text-sm font-medium text-gray-700 mb-2 text-center">资产配置占比</h4>
          <div class="h-64">
            <canvas ref="majorCategoryAssetChartCanvas"></canvas>
          </div>
        </div>

        <!-- 右侧：回报率柱状图 -->
        <div class="flex flex-col">
          <h4 class="text-sm font-medium text-gray-700 mb-2 text-center">投资回报率对比</h4>
          <div class="h-64">
            <canvas ref="majorCategoryReturnChartCanvas"></canvas>
          </div>
        </div>
      </div>

      <!-- 表格 -->
      <div class="overflow-auto">
          <table v-if="majorCategoryData.length > 0" class="min-w-full text-xs">
            <thead class="bg-gray-50 sticky top-0">
              <tr>
                <th class="px-2 py-1.5 text-left font-medium text-gray-700 uppercase tracking-tight">分类</th>
                <th class="px-2 py-1.5 text-right font-medium text-gray-700 uppercase tracking-tight">上一年值</th>
                <th class="px-2 py-1.5 text-right font-medium text-gray-700 uppercase tracking-tight">当前年值</th>
                <th class="px-2 py-1.5 text-right font-medium text-gray-700 uppercase tracking-tight">净投入</th>
                <th class="px-2 py-1.5 text-right font-medium text-gray-700 uppercase tracking-tight">投资回报</th>
                <th class="px-2 py-1.5 text-right font-medium text-gray-700 uppercase tracking-tight">回报率</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-200">
              <tr
                v-for="item in majorCategoryData"
                :key="item.categoryId"
                @click="selectMajorCategory(item)"
                :class="[
                  'cursor-pointer transition-colors',
                  selectedMajorCategoryId === item.categoryId
                    ? 'bg-primary/10'
                    : 'hover:bg-gray-50'
                ]"
              >
                <td class="px-2 py-1.5">
                  <div class="flex items-center gap-1.5">
                    <span class="text-base">{{ item.categoryIcon }}</span>
                    <span class="font-medium text-gray-900">{{ item.categoryName }}</span>
                  </div>
                </td>
                <td class="px-2 py-1.5 text-right text-gray-700 whitespace-nowrap">
                  {{ formatCurrency(item.lastYearEndAssets) }}
                </td>
                <td class="px-2 py-1.5 text-right font-medium text-gray-900 whitespace-nowrap">
                  {{ formatCurrency(item.currentAssets) }}
                </td>
                <td class="px-2 py-1.5 text-right text-gray-600 whitespace-nowrap">
                  {{ formatCurrency(item.netDeposits) }}
                </td>
                <td class="px-2 py-1.5 text-right font-medium whitespace-nowrap" :class="item.returns >= 0 ? 'text-green-600' : 'text-red-600'">
                  {{ formatCurrency(item.returns) }}
                </td>
                <td class="px-2 py-1.5 text-right font-semibold whitespace-nowrap" :class="item.returnRate >= 0 ? 'text-green-600' : 'text-red-600'">
                  {{ item.returnRate != null ? item.returnRate.toFixed(2) + '%' : 'N/A' }}
                </td>
              </tr>
            </tbody>
          </table>
          <div v-else class="text-center py-8 text-gray-500 text-sm">
            暂无大类分布数据
          </div>
      </div>
    </div>

    <!-- 钻取区域：账户分布和月度趋势 -->
    <div v-if="selectedMajorCategoryId" class="grid grid-cols-1 lg:grid-cols-2 gap-4">
      <!-- 账户分布 -->
      <div class="bg-white rounded-lg shadow border border-gray-200 p-4">
        <h3 class="text-base font-semibold text-gray-900 mb-3">
          {{ selectedMajorCategoryName }} - 账户分布
        </h3>

        <!-- 饼图和表格 -->
        <div class="flex flex-col gap-3">
          <!-- 账户资产配置饼图 -->
          <div v-if="accountData.length > 0">
            <h4 class="text-sm font-medium text-gray-700 mb-2 text-center">账户资产占比</h4>
            <div class="w-full h-48">
              <canvas ref="accountChartCanvas"></canvas>
            </div>
          </div>
          <div v-else class="h-48 flex items-center justify-center text-gray-500 text-sm">
            暂无账户数据
          </div>

          <!-- 账户表格 -->
          <div class="overflow-auto max-h-64">
            <table v-if="accountData.length > 0" class="min-w-full text-xs">
              <thead class="bg-gray-50 sticky top-0">
                <tr>
                  <th class="px-2 py-1 text-left font-medium text-gray-700 uppercase tracking-tight">账户</th>
                  <th class="px-2 py-1 text-right font-medium text-gray-700 uppercase tracking-tight">上一年值</th>
                  <th class="px-2 py-1 text-right font-medium text-gray-700 uppercase tracking-tight">当前年值</th>
                  <th class="px-2 py-1 text-right font-medium text-gray-700 uppercase tracking-tight">净投入</th>
                  <th class="px-2 py-1 text-right font-medium text-gray-700 uppercase tracking-tight">回报</th>
                  <th class="px-2 py-1 text-right font-medium text-gray-700 uppercase tracking-tight">回报率</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-200">
                <tr
                  v-for="item in accountData"
                  :key="item.accountId"
                  @click="selectAccount(item)"
                  :class="[
                    'cursor-pointer transition-colors',
                    selectedAccountId === item.accountId
                      ? 'bg-primary/10'
                      : 'hover:bg-gray-50'
                  ]"
                >
                  <td class="px-2 py-1">
                    <div class="font-medium text-gray-900 relative group cursor-help">
                      {{ item.accountName }}
                      <!-- Tooltip -->
                      <div class="absolute left-0 bottom-full mb-2 hidden group-hover:block z-10 px-2 py-1 text-xs text-white bg-gray-800 rounded shadow-lg whitespace-nowrap">
                        {{ item.userName }}
                        <div class="absolute left-4 top-full w-0 h-0 border-l-4 border-r-4 border-t-4 border-transparent border-t-gray-800"></div>
                      </div>
                    </div>
                  </td>
                  <td class="px-2 py-1 text-right text-gray-700 whitespace-nowrap">
                    {{ formatCurrency(item.lastYearEndAssets, selectedCurrency === 'All' ? 'USD' : item.currency) }}
                  </td>
                  <td class="px-2 py-1 text-right font-medium text-gray-900 whitespace-nowrap">
                    {{ formatCurrency(item.currentAssets, selectedCurrency === 'All' ? 'USD' : item.currency) }}
                  </td>
                  <td class="px-2 py-1 text-right text-gray-600 whitespace-nowrap">
                    {{ formatCurrency(item.netDeposits, selectedCurrency === 'All' ? 'USD' : item.currency) }}
                  </td>
                  <td class="px-2 py-1 text-right font-medium whitespace-nowrap" :class="item.returns >= 0 ? 'text-green-600' : 'text-red-600'">
                    {{ formatCurrency(item.returns, selectedCurrency === 'All' ? 'USD' : item.currency) }}
                  </td>
                  <td class="px-2 py-1 text-right font-semibold whitespace-nowrap" :class="item.returnRate >= 0 ? 'text-green-600' : 'text-red-600'">
                    {{ item.returnRate != null ? item.returnRate.toFixed(2) + '%' : 'N/A' }}
                  </td>
                </tr>
              </tbody>
              <tfoot class="bg-blue-50 border-t-2 border-blue-200">
                <tr class="font-bold">
                  <td class="px-2 py-1.5 text-left text-gray-900">总计</td>
                  <td class="px-2 py-1.5 text-right text-gray-900 whitespace-nowrap">
                    {{ formatCurrency(accountTotal.lastYearEndAssets) }}
                  </td>
                  <td class="px-2 py-1.5 text-right text-gray-900 whitespace-nowrap">
                    {{ formatCurrency(accountTotal.currentAssets) }}
                  </td>
                  <td class="px-2 py-1.5 text-right text-gray-900 whitespace-nowrap">
                    {{ formatCurrency(accountTotal.netDeposits) }}
                  </td>
                  <td class="px-2 py-1.5 text-right whitespace-nowrap" :class="accountTotal.returns >= 0 ? 'text-green-600' : 'text-red-600'">
                    {{ formatCurrency(accountTotal.returns) }}
                  </td>
                  <td class="px-2 py-1.5 text-right whitespace-nowrap" :class="accountTotal.returnRate >= 0 ? 'text-green-600' : 'text-red-600'">
                    {{ accountTotal.returnRate != null ? accountTotal.returnRate.toFixed(2) + '%' : 'N/A' }}
                  </td>
                </tr>
              </tfoot>
            </table>
          </div>
        </div>
      </div>

      <!-- 月度趋势 -->
      <div
        v-if="selectedAccountId"
        class="bg-white rounded-lg shadow border border-gray-200 p-4"
      >
        <h3 class="text-base font-semibold text-gray-900 mb-2">
          {{ selectedAccountName }} - 月度趋势
        </h3>

        <div class="grid grid-cols-5 gap-2 mb-3 text-xs">
          <div class="bg-gray-50 px-2 py-1.5 rounded">
            <div class="text-gray-600">上一年值</div>
            <div class="font-medium text-gray-900">{{ formatCurrency(accountSummary.lastYearEndAssets || 0, selectedCurrency === 'All' ? 'USD' : accountSummary.currency) }}</div>
          </div>
          <div class="bg-gray-50 px-2 py-1.5 rounded">
            <div class="text-gray-600">当前年值</div>
            <div class="font-medium text-gray-900">{{ formatCurrency(accountSummary.currentAssets || 0, selectedCurrency === 'All' ? 'USD' : accountSummary.currency) }}</div>
          </div>
          <div class="bg-gray-50 px-2 py-1.5 rounded">
            <div class="text-gray-600">净投入</div>
            <div class="font-medium text-gray-600">{{ formatCurrency(accountSummary.netDeposits || 0, selectedCurrency === 'All' ? 'USD' : accountSummary.currency) }}</div>
          </div>
          <div class="bg-gray-50 px-2 py-1.5 rounded">
            <div class="text-gray-600">投资回报</div>
            <div class="font-semibold" :class="(accountSummary.returns || 0) >= 0 ? 'text-green-600' : 'text-red-600'">
              {{ formatCurrency(accountSummary.returns || 0, selectedCurrency === 'All' ? 'USD' : accountSummary.currency) }}
            </div>
          </div>
          <div class="bg-gray-50 px-2 py-1.5 rounded">
            <div class="text-gray-600">回报率</div>
            <div class="font-semibold" :class="(accountSummary.returnRate || 0) >= 0 ? 'text-green-600' : 'text-red-600'">
              {{ accountSummary.returnRate != null ? accountSummary.returnRate.toFixed(2) + '%' : '0.00%' }}
            </div>
          </div>
        </div>

        <div v-if="monthlyTrendData.length > 0" class="h-64">
          <canvas ref="monthlyTrendChartCanvas"></canvas>
        </div>
        <div v-else class="h-64 flex items-center justify-center text-gray-500 text-sm">
          暂无月度趋势数据
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useFamilyStore } from '@/stores/family'
import { investmentAnalysisAPI } from '@/api/investment'
import Chart from 'chart.js/auto'
import ChartDataLabels from 'chartjs-plugin-datalabels'

export default {
  name: 'InvestmentAnalysis',
  setup() {
    const familyStore = useFamilyStore()
    // ========== 图表配置常量 ==========
    // 使用色彩学上更和谐的颜色方案（基于Material Design）
    const CHART_COLORS = [
      '#2196F3', // Blue 500
      '#4CAF50', // Green 500
      '#FF9800', // Orange 500
      '#F44336', // Red 500
      '#9C27B0', // Purple 500
      '#E91E63', // Pink 500
      '#00BCD4', // Cyan 500
      '#FF5722', // Deep Orange 500
      '#009688', // Teal 500
      '#8BC34A', // Light Green 500
      '#FFC107', // Amber 500
      '#673AB7'  // Deep Purple 500
    ]

    // 图表默认配置
    const CHART_DEFAULTS = {
      responsive: true,
      maintainAspectRatio: false,
      animation: {
        duration: 750,
        easing: 'easeInOutQuart'
      },
      interaction: {
        mode: 'index',
        intersect: false
      }
    }

    // 饼图专用配置生成器
    const createPieChartOptions = (formatCurrencyFn, showDataLabels = true) => ({
      ...CHART_DEFAULTS,
      plugins: {
        legend: {
          display: true,
          position: 'bottom',
          labels: {
            padding: 15,
            font: {
              size: 12
            },
            usePointStyle: true,
            pointStyle: 'circle',
            generateLabels: (chart) => {
              const data = chart.data
              if (data.labels.length && data.datasets.length) {
                const dataset = data.datasets[0]
                const total = dataset.data.reduce((a, b) => Math.abs(a) + Math.abs(b), 0)

                return data.labels.map((label, i) => {
                  const value = dataset.data[i]
                  const percentage = total > 0 ? ((Math.abs(value) / total) * 100).toFixed(1) : '0.0'
                  return {
                    text: `${label} (${percentage}%)`,
                    fillStyle: dataset.backgroundColor[i],
                    hidden: false,
                    index: i
                  }
                })
              }
              return []
            }
          }
        },
        tooltip: {
          backgroundColor: 'rgba(0, 0, 0, 0.8)',
          padding: 12,
          titleFont: {
            size: 14,
            weight: 'bold'
          },
          bodyFont: {
            size: 13
          },
          cornerRadius: 6,
          displayColors: true,
          callbacks: {
            label: function(context) {
              const label = context.label || ''
              const value = context.parsed || 0
              const dataset = context.dataset
              const total = dataset.data.reduce((a, b) => Math.abs(a) + Math.abs(b), 0)
              const percentage = total > 0 ? ((Math.abs(value) / total) * 100).toFixed(1) : '0.0'
              return `${label}: ${formatCurrencyFn(value)} (${percentage}%)`
            }
          }
        },
        datalabels: showDataLabels ? {
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

            // 只显示大于5%的标签
            if (percentage <= 5) return ''

            // 根据百分比大小决定显示内容
            if (percentage > 10) {
              // 大扇区：显示名称和百分比（两行）
              return `${label}\n${percentage}%`
            } else {
              // 中等扇区：只显示百分比
              return `${percentage}%`
            }
          },
          anchor: 'center',
          align: 'center',
          textAlign: 'center'
        } : false
      },
      // 添加点击事件处理
      onClick: (event, elements) => {
        if (elements.length > 0) {
          const index = elements[0].index
          // 可以在这里添加点击反馈效果
        }
      }
    })

    // 数据
    const selectedFamilyId = computed(() => familyStore.currentFamilyId)
    // 默认选择当前年
    const selectedYear = ref(new Date().getFullYear())
    const selectedCurrency = ref('USD')
    const availableYears = ref([])

    // 大类数据
    const majorCategoryData = ref([])
    const selectedMajorCategoryId = ref(null)
    const selectedMajorCategoryName = ref(null)

    // 账户数据
    const accountData = ref([])
    const selectedAccountId = ref(null)
    const selectedAccountName = ref(null)

    // 月度趋势数据
    const monthlyTrendData = ref([])
    const accountSummary = ref({})

    // 图表实例
    const majorCategoryAssetChartCanvas = ref(null)
    const majorCategoryReturnChartCanvas = ref(null)
    const accountChartCanvas = ref(null)
    const monthlyTrendChartCanvas = ref(null)
    const majorCategoryAssetChartInstance = ref(null)
    const majorCategoryReturnChartInstance = ref(null)
    const accountChartInstance = ref(null)
    const monthlyTrendChartInstance = ref(null)

    // 计算属性
    const totalSummary = computed(() => {
      const summary = {
        lastYearEndAssets: 0,
        currentAssets: 0,
        netDeposits: 0,
        returns: 0,
        returnRate: 0
      }

      if (majorCategoryData.value.length === 0) {
        return summary
      }

      // 汇总所有大类的数据
      majorCategoryData.value.forEach(item => {
        summary.lastYearEndAssets += (item.lastYearEndAssets || 0)
        summary.currentAssets += (item.currentAssets || 0)
        summary.netDeposits += (item.netDeposits || 0)
        summary.returns += (item.returns || 0)
      })

      // 计算总回报率
      const denominator = summary.lastYearEndAssets + summary.netDeposits
      if (denominator !== 0) {
        summary.returnRate = (summary.returns / denominator) * 100
      }

      return summary
    })

    const totalNetDeposits = computed(() => {
      return majorCategoryData.value.reduce((sum, item) => sum + (item.netDeposits || 0), 0)
    })

    const accountTotal = computed(() => {
      const total = {
        lastYearEndAssets: 0,
        currentAssets: 0,
        netDeposits: 0,
        returns: 0,
        returnRate: 0
      }

      if (accountData.value.length === 0) {
        return total
      }

      // 汇总所有账户的USD转换后数据（用于总计）
      accountData.value.forEach(item => {
        total.lastYearEndAssets += (item.lastYearEndAssetsUsd || 0)
        total.currentAssets += (item.currentAssetsUsd || 0)
        total.netDeposits += (item.netDepositsUsd || 0)
        total.returns += (item.returnsUsd || 0)
      })

      // 计算总回报率
      const denominator = total.lastYearEndAssets + total.netDeposits
      if (denominator !== 0) {
        total.returnRate = (total.returns / denominator) * 100
      }

      return total
    })

    // 工具函数
    const formatCurrency = (value, currency = null) => {
      // 如果没有指定货币，使用选中的货币
      const curr = currency || selectedCurrency.value

      if (!value) {
        // 如果选择All，则显示美元符号（因为All模式下所有金额都折算为USD）
        const displayCurr = curr === 'All' ? 'USD' : curr
        return displayCurr === 'CNY' ? '¥0.00' : '$0.00'
      }

      const formatted = parseFloat(value).toLocaleString('en-US', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
      })

      // 根据货币返回不同符号
      // 如果选择All，则显示美元符号（因为All模式下所有金额都折算为USD）
      const displayCurr = curr === 'All' ? 'USD' : curr
      const symbol = displayCurr === 'CNY' ? '¥' : '$'
      return symbol + formatted
    }

    // 生成可用年份列表（当前年份往前推10年）
    const generateAvailableYears = () => {
      const currentYear = new Date().getFullYear()
      const years = []
      for (let i = 0; i < 10; i++) {
        years.push(currentYear - i)
      }
      availableYears.value = years
    }

    // 加载大类分布数据
    const loadMajorCategoryData = async () => {
      if (!selectedFamilyId.value) {
        return
      }

      try {
        const response = await investmentAnalysisAPI.getAnnualByCategory(
          selectedFamilyId.value,
          selectedYear.value,
          selectedCurrency.value
        )
        if (response.success) {
          majorCategoryData.value = response.data
          await nextTick()
          renderMajorCategoryChart()
        }
      } catch (error) {
        console.error('加载大类分布数据失败:', error)
      }
    }

    // 渲染大类图表（资产配置饼图 + 回报率柱状图）
    const renderMajorCategoryChart = () => {
      renderMajorCategoryAssetChart()
      renderMajorCategoryReturnChart()
    }

    // 渲染大类资产配置饼图
    const renderMajorCategoryAssetChart = () => {
      if (!majorCategoryAssetChartCanvas.value || majorCategoryData.value.length === 0) return

      // 销毁旧图表
      if (majorCategoryAssetChartInstance.value) {
        majorCategoryAssetChartInstance.value.destroy()
      }

      const ctx = majorCategoryAssetChartCanvas.value.getContext('2d')

      // 准备数据 - 使用当前资产值
      const chartData = majorCategoryData.value.map(item => ({
        label: item.categoryName,
        value: item.currentAssets || 0
      }))

      // 为每个分类分配颜色
      const backgroundColor = chartData.map((_, index) => CHART_COLORS[index % CHART_COLORS.length])

      // 注册 datalabels 插件
      Chart.register(ChartDataLabels)

      majorCategoryAssetChartInstance.value = new Chart(ctx, {
        type: 'pie',
        data: {
          labels: chartData.map(item => item.label),
          datasets: [{
            data: chartData.map(item => item.value),
            backgroundColor,
            borderWidth: 2,
            borderColor: '#fff',
            hoverBorderWidth: 3,
            hoverBorderColor: '#fff',
            hoverOffset: 10
          }]
        },
        options: createPieChartOptions(formatCurrency, true)
      })
    }

    // 渲染大类回报率柱状图
    const renderMajorCategoryReturnChart = () => {
      if (!majorCategoryReturnChartCanvas.value || majorCategoryData.value.length === 0) return

      // 销毁旧图表
      if (majorCategoryReturnChartInstance.value) {
        majorCategoryReturnChartInstance.value.destroy()
      }

      const ctx = majorCategoryReturnChartCanvas.value.getContext('2d')

      // 准备数据
      const labels = majorCategoryData.value.map(item => item.categoryName)
      const returnRates = majorCategoryData.value.map(item => item.returnRate || 0)

      // 根据回报率正负设置颜色
      const backgroundColors = returnRates.map(rate => rate >= 0 ? '#10b981' : '#ef4444')

      // 计算平均回报率
      const avgReturnRate = returnRates.length > 0
        ? returnRates.reduce((sum, rate) => sum + rate, 0) / returnRates.length
        : 0

      majorCategoryReturnChartInstance.value = new Chart(ctx, {
        type: 'bar',
        data: {
          labels,
          datasets: [{
            label: '回报率 (%)',
            data: returnRates,
            backgroundColor: backgroundColors,
            borderColor: backgroundColors,
            borderWidth: 1
          }]
        },
        options: {
          ...CHART_DEFAULTS,
          indexAxis: 'y', // 横向柱状图
          plugins: {
            legend: {
              display: false
            },
            tooltip: {
              backgroundColor: 'rgba(0, 0, 0, 0.8)',
              padding: 12,
              titleFont: {
                size: 14,
                weight: 'bold'
              },
              bodyFont: {
                size: 13
              },
              cornerRadius: 6,
              callbacks: {
                label: function(context) {
                  return `回报率: ${context.parsed.x.toFixed(2)}%`
                },
                afterBody: function() {
                  return `平均回报率: ${avgReturnRate.toFixed(2)}%`
                }
              }
            }
          },
          scales: {
            x: {
              beginAtZero: true,
              title: {
                display: true,
                text: '回报率 (%)',
                font: {
                  size: 12
                }
              },
              ticks: {
                callback: function(value) {
                  return value.toFixed(1) + '%'
                }
              },
              // 添加零线
              grid: {
                color: function(context) {
                  if (context.tick.value === 0) {
                    return '#000'
                  }
                  return '#e5e7eb'
                },
                lineWidth: function(context) {
                  if (context.tick.value === 0) {
                    return 2
                  }
                  return 1
                }
              }
            },
            y: {
              title: {
                display: true,
                text: '投资类别',
                font: {
                  size: 12
                }
              }
            }
          }
        }
      })
    }

    // 选择大类
    const selectMajorCategory = async (item) => {
      selectedMajorCategoryId.value = item.categoryId
      selectedMajorCategoryName.value = item.categoryName

      // 清空账户选择
      selectedAccountId.value = null
      selectedAccountName.value = null
      monthlyTrendData.value = []

      // 加载该大类下的账户数据
      try {
        const response = await investmentAnalysisAPI.getAnnualByAccount(
          selectedFamilyId.value,
          selectedYear.value,
          item.categoryId,
          selectedCurrency.value
        )
        if (response.success) {
          accountData.value = response.data
          await nextTick()
          renderAccountChart()
        }
      } catch (error) {
        console.error('加载账户分布数据失败:', error)
      }
    }

    // 渲染账户资产配置饼图
    const renderAccountChart = () => {
      if (!accountChartCanvas.value || accountData.value.length === 0) return

      // 销毁旧图表
      if (accountChartInstance.value) {
        accountChartInstance.value.destroy()
      }

      const ctx = accountChartCanvas.value.getContext('2d')

      // 准备数据 - 使用当前资产值
      const chartData = accountData.value.map(item => ({
        label: item.accountName,
        value: item.currentAssets || 0
      }))

      // 为每个账户分配颜色
      const backgroundColor = chartData.map((_, index) => CHART_COLORS[index % CHART_COLORS.length])

      accountChartInstance.value = new Chart(ctx, {
        type: 'pie',
        data: {
          labels: chartData.map(item => item.label),
          datasets: [{
            data: chartData.map(item => item.value),
            backgroundColor,
            borderWidth: 2,
            borderColor: '#fff',
            hoverBorderWidth: 3,
            hoverBorderColor: '#fff',
            hoverOffset: 10
          }]
        },
        options: createPieChartOptions(formatCurrency, true)
      })
    }

    // 选择账户
    const selectAccount = async (item) => {
      selectedAccountId.value = item.accountId
      selectedAccountName.value = item.accountName

      // 初始化默认值
      accountSummary.value = {
        accountName: item.accountName,
        currency: item.currency || 'USD',
        currentAssets: item.currentAssets || 0,
        lastYearEndAssets: item.lastYearEndAssets || 0,
        netDeposits: item.netDeposits || 0,
        returns: item.returns || 0,
        returnRate: item.returnRate || 0
      }

      // 加载该账户的月度趋势
      try {
        const response = await investmentAnalysisAPI.getAccountMonthlyTrend(
          item.accountId,
          selectedYear.value
        )
        if (response.success) {
          // 新的响应格式包含 monthlyData 和账户汇总信息
          accountSummary.value = {
            accountName: response.data.accountName || item.accountName,
            currency: response.data.currency || item.currency || 'USD',
            currentAssets: response.data.currentAssets || 0,
            lastYearEndAssets: response.data.lastYearEndAssets || 0,
            netDeposits: response.data.netDeposits || 0,
            returns: response.data.returns || 0,
            returnRate: response.data.returnRate || 0
          }
          monthlyTrendData.value = response.data.monthlyData || []
          await nextTick()
          renderMonthlyTrendChart()
        }
      } catch (error) {
        console.error('加载月度趋势数据失败:', error)
        // 即使出错也保留默认值，确保UI显示
      }
    }

    // 渲染月度趋势柱状图
    const renderMonthlyTrendChart = () => {
      if (!monthlyTrendChartCanvas.value || monthlyTrendData.value.length === 0) return

      // 销毁旧图表
      if (monthlyTrendChartInstance.value) {
        monthlyTrendChartInstance.value.destroy()
      }

      // 准备12个月的数据（即使某些月没有数据也要显示）
      const months = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月']
      const deposits = new Array(12).fill(0)
      const withdrawals = new Array(12).fill(0)

      monthlyTrendData.value.forEach(item => {
        const monthIndex = item.month - 1
        if (monthIndex >= 0 && monthIndex < 12) {
          deposits[monthIndex] = item.deposits || 0
          withdrawals[monthIndex] = item.withdrawals || 0
        }
      })

      const ctx = monthlyTrendChartCanvas.value.getContext('2d')
      monthlyTrendChartInstance.value = new Chart(ctx, {
        type: 'bar',
        data: {
          labels: months,
          datasets: [
            {
              label: '投入',
              data: deposits,
              backgroundColor: '#10b981',
              borderColor: '#10b981',
              borderWidth: 1
            },
            {
              label: '取出',
              data: withdrawals,
              backgroundColor: '#ef4444',
              borderColor: '#ef4444',
              borderWidth: 1
            }
          ]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: {
              display: true,
              position: 'top'
            },
            tooltip: {
              callbacks: {
                label: function(context) {
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
                text: '金额'
              },
              ticks: {
                callback: function(value) {
                  return formatCurrency(value)
                }
              }
            },
            x: {
              title: {
                display: true,
                text: '月份'
              }
            }
          }
        }
      })
    }

    // 筛选条件变化处理
    const onFilterChange = async () => {
      // 清空选择
      selectedMajorCategoryId.value = null
      selectedMajorCategoryName.value = null
      selectedAccountId.value = null
      selectedAccountName.value = null
      accountData.value = []
      monthlyTrendData.value = []

      // 重新加载大类数据
      await loadMajorCategoryData()
    }

    // 清理所有图表实例
    const destroyAllCharts = () => {
      if (majorCategoryAssetChartInstance.value) {
        majorCategoryAssetChartInstance.value.destroy()
        majorCategoryAssetChartInstance.value = null
      }
      if (majorCategoryReturnChartInstance.value) {
        majorCategoryReturnChartInstance.value.destroy()
        majorCategoryReturnChartInstance.value = null
      }
      if (accountChartInstance.value) {
        accountChartInstance.value.destroy()
        accountChartInstance.value = null
      }
      if (monthlyTrendChartInstance.value) {
        monthlyTrendChartInstance.value.destroy()
        monthlyTrendChartInstance.value = null
      }
    }

    // 响应式窗口大小调整
    let resizeTimer = null
    const handleResize = () => {
      if (resizeTimer) clearTimeout(resizeTimer)

      resizeTimer = setTimeout(() => {
        // 重新渲染所有活动的图表
        if (majorCategoryData.value.length > 0) {
          renderMajorCategoryChart()
        }
        if (accountData.value.length > 0) {
          renderAccountChart()
        }
        if (monthlyTrendData.value.length > 0) {
          renderMonthlyTrendChart()
        }
      }, 250) // 250ms 防抖
    }

    // Watch for family changes and reload data
    watch(() => familyStore.currentFamilyId, (newFamilyId, oldFamilyId) => {
      if (newFamilyId && newFamilyId !== oldFamilyId) {
        loadMajorCategoryData()
      }
    })

    // 初始化
    onMounted(async () => {
      generateAvailableYears()

      // Load page data if family is already available
      if (selectedFamilyId.value) {
        await loadMajorCategoryData()
      }

      // 添加窗口大小调整监听
      window.addEventListener('resize', handleResize)
    })

    // 组件卸载时清理
    onBeforeUnmount(() => {
      destroyAllCharts()
      window.removeEventListener('resize', handleResize)
      if (resizeTimer) clearTimeout(resizeTimer)
    })

    return {
      // 数据
      selectedYear,
      selectedCurrency,
      availableYears,
      majorCategoryData,
      selectedMajorCategoryId,
      selectedMajorCategoryName,
      accountData,
      selectedAccountId,
      selectedAccountName,
      monthlyTrendData,
      accountSummary,

      // 图表引用
      majorCategoryAssetChartCanvas,
      majorCategoryReturnChartCanvas,
      accountChartCanvas,
      monthlyTrendChartCanvas,

      // 计算属性
      totalSummary,
      totalNetDeposits,
      accountTotal,

      // 方法
      formatCurrency,
      selectMajorCategory,
      selectAccount,
      onFilterChange
    }
  }
}
</script>
