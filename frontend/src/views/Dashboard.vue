<template>
  <div class="p-6 space-y-6">
    <!-- Welcome Section -->
    <div>
      <h1 class="text-3xl font-bold text-gray-900">财务概览</h1>
      <p class="text-gray-600 mt-2">
        欢迎使用个人理财管理系统
      </p>
    </div>

    <!-- Quick Stats -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
      <div class="bg-white rounded-lg shadow border border-gray-200 p-6">
        <div class="text-sm font-medium text-gray-600 mb-2">总资产</div>
        <div class="text-2xl font-bold text-green-600">${{ formatAmount(totalAssets) }}</div>
        <p class="text-xs text-gray-500 mt-1">Total Assets</p>
      </div>

      <div class="bg-white rounded-lg shadow border border-gray-200 p-6">
        <div class="text-sm font-medium text-gray-600 mb-2">总负债</div>
        <div class="text-2xl font-bold text-red-600">${{ formatAmount(totalLiabilities) }}</div>
        <p class="text-xs text-gray-500 mt-1">Total Liabilities</p>
      </div>

      <div class="bg-white rounded-lg shadow border border-gray-200 p-6">
        <div class="text-sm font-medium text-gray-600 mb-2">净资产</div>
        <div class="text-2xl font-bold text-blue-600">${{ formatAmount(netWorth) }}</div>
        <p class="text-xs text-gray-500 mt-1">Net Worth</p>
      </div>

      <div class="bg-white rounded-lg shadow border border-gray-200 p-6">
        <div class="text-sm font-medium text-gray-600 mb-2">资产负债率</div>
        <div class="text-2xl font-bold" :class="debtRatioColor">{{ debtRatio }}%</div>
        <p class="text-xs text-gray-500 mt-1">Debt Ratio</p>
      </div>
    </div>

    <!-- 资产负债分布 -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <!-- 资产分布 -->
      <div class="bg-white rounded-lg shadow border border-gray-200">
        <div class="px-6 py-4 border-b border-gray-200">
          <h2 class="text-lg font-semibold text-gray-900">资产分布</h2>
        </div>
        <div class="p-6">
          <div v-if="assetCategories.length === 0" class="text-sm text-gray-500 text-center py-8">
            暂无资产数据
          </div>
          <div v-else class="h-64">
            <canvas ref="assetChartCanvas"></canvas>
          </div>
          <div v-if="assetCategories.length > 0" class="mt-6 space-y-2">
            <div v-for="category in assetCategories" :key="category.type" class="flex items-center justify-between text-sm">
              <div class="flex items-center gap-2">
                <div class="w-3 h-3 rounded-full" :style="{ backgroundColor: category.color }"></div>
                <span class="text-gray-700">{{ category.name }}</span>
              </div>
              <div class="flex items-center gap-3">
                <span class="text-gray-900 font-medium">${{ formatAmount(category.total) }}</span>
                <span class="text-gray-500">{{ category.percentage }}%</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 负债分布 -->
      <div class="bg-white rounded-lg shadow border border-gray-200">
        <div class="px-6 py-4 border-b border-gray-200">
          <h2 class="text-lg font-semibold text-gray-900">负债分布</h2>
        </div>
        <div class="p-6">
          <div v-if="liabilityCategories.length === 0" class="text-sm text-gray-500 text-center py-8">
            暂无负债数据
          </div>
          <div v-else class="h-64">
            <canvas ref="liabilityChartCanvas"></canvas>
          </div>
          <div v-if="liabilityCategories.length > 0" class="mt-6 space-y-2">
            <div v-for="category in liabilityCategories" :key="category.type" class="flex items-center justify-between text-sm">
              <div class="flex items-center gap-2">
                <div class="w-3 h-3 rounded-full" :style="{ backgroundColor: category.color }"></div>
                <span class="text-gray-700">{{ category.name }}</span>
              </div>
              <div class="flex items-center gap-3">
                <span class="text-gray-900 font-medium">${{ formatAmount(category.total) }}</span>
                <span class="text-gray-500">{{ category.percentage }}%</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 净资产分类 -->
    <div class="bg-white rounded-lg shadow border border-gray-200">
      <div class="px-6 py-4 border-b border-gray-200">
        <h2 class="text-lg font-semibold text-gray-900">净资产分类</h2>
        <p class="text-sm text-gray-500 mt-1">资产减去对应负债后的净值分布</p>
      </div>
      <div class="p-6">
        <div v-if="netAssetAllocation.data.length === 0" class="text-sm text-gray-500 text-center py-8">
          暂无净资产数据
        </div>
        <div v-else class="grid grid-cols-1 lg:grid-cols-5 gap-6">
          <!-- 左侧饼图 - 占3列 -->
          <div class="lg:col-span-3 h-80 flex items-center justify-center">
            <canvas ref="netAssetChartCanvas"></canvas>
          </div>

          <!-- 右侧详细列表 - 占2列 -->
          <div class="lg:col-span-2 space-y-1.5">
            <div v-for="category in netAssetAllocation.data" :key="category.code"
                 class="flex items-center justify-between p-2 border border-gray-200 rounded hover:bg-gray-50 transition-colors text-xs">
              <div class="flex items-center gap-2 flex-shrink-0">
                <div class="w-2.5 h-2.5 rounded-full" :style="{ backgroundColor: category.color }"></div>
                <span class="font-semibold text-gray-900">{{ category.name }}</span>
              </div>
              <div class="flex items-center gap-3 text-right">
                <div class="text-gray-500">
                  <span class="text-green-600 font-medium">${{ formatAmount(category.assets) }}</span>
                  <span class="mx-1">-</span>
                  <span class="text-red-600 font-medium">${{ formatAmount(category.liabilities) }}</span>
                </div>
                <div class="font-bold text-gray-900 min-w-[80px]">
                  ${{ formatAmount(category.netValue) }}
                </div>
                <div class="text-gray-500 min-w-[45px]">{{ category.percentage }}%</div>
              </div>
            </div>
            <div class="border-t border-gray-200 pt-2 mt-3">
              <div class="flex items-center justify-between font-semibold text-sm">
                <span class="text-gray-700">总净资产</span>
                <span class="text-lg text-blue-600">${{ formatAmount(netAssetAllocation.total) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 快捷操作 & 账户概览 -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <!-- 快捷操作 -->
      <div class="bg-white rounded-lg shadow border border-gray-200">
        <div class="px-6 py-4 border-b border-gray-200">
          <h2 class="text-lg font-semibold text-gray-900">快捷操作</h2>
        </div>
        <div class="p-6">
          <div class="space-y-2">
            <button
              @click="$router.push('/assets/history')"
              class="w-full px-4 py-3 text-left border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors flex items-center gap-3"
            >
              <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5 text-green-600" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="12" y1="5" x2="12" y2="19"></line>
                <line x1="5" y1="12" x2="19" y2="12"></line>
              </svg>
              <span class="font-medium text-gray-900">管理资产账户</span>
            </button>
            <button
              @click="$router.push('/liabilities/history')"
              class="w-full px-4 py-3 text-left border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors flex items-center gap-3"
            >
              <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5 text-red-600" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="12" y1="5" x2="12" y2="19"></line>
                <line x1="5" y1="12" x2="19" y2="12"></line>
              </svg>
              <span class="font-medium text-gray-900">管理负债账户</span>
            </button>
            <button
              @click="$router.push('/assets/batch-update')"
              class="w-full px-4 py-3 text-left border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors flex items-center gap-3"
            >
              <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5 text-blue-600" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
              </svg>
              <span class="font-medium text-gray-900">批量更新资产</span>
            </button>
            <button
              @click="$router.push('/analysis/trend')"
              class="w-full px-4 py-3 text-left border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors flex items-center gap-3"
            >
              <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5 text-purple-600" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"></polyline>
              </svg>
              <span class="font-medium text-gray-900">查看趋势分析</span>
            </button>
          </div>
        </div>
      </div>

      <!-- 账户概览 -->
      <div class="bg-white rounded-lg shadow border border-gray-200">
        <div class="px-6 py-4 border-b border-gray-200">
          <h2 class="text-lg font-semibold text-gray-900">账户概览</h2>
        </div>
        <div class="p-6">
          <div class="grid grid-cols-2 gap-4">
            <div class="bg-green-50 rounded-lg p-4 border border-green-200">
              <div class="text-sm text-green-700 font-medium mb-1">资产账户</div>
              <div class="text-2xl font-bold text-green-900">{{ totalAssetAccounts }}</div>
              <div class="text-xs text-green-600 mt-1">个账户</div>
            </div>
            <div class="bg-red-50 rounded-lg p-4 border border-red-200">
              <div class="text-sm text-red-700 font-medium mb-1">负债账户</div>
              <div class="text-2xl font-bold text-red-900">{{ totalLiabilityAccounts }}</div>
              <div class="text-xs text-red-600 mt-1">个账户</div>
            </div>
          </div>

          <div class="mt-6 space-y-3">
            <div class="flex items-center justify-between text-sm">
              <span class="text-gray-600">活跃资产账户</span>
              <span class="font-medium text-gray-900">{{ activeAssetAccounts }}</span>
            </div>
            <div class="flex items-center justify-between text-sm">
              <span class="text-gray-600">活跃负债账户</span>
              <span class="font-medium text-gray-900">{{ activeLiabilityAccounts }}</span>
            </div>
            <div class="flex items-center justify-between text-sm pt-3 border-t border-gray-200">
              <span class="text-gray-600">总账户数</span>
              <span class="font-bold text-gray-900">{{ totalAccounts }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 净资产趋势图 -->
    <div class="bg-white rounded-lg shadow border border-gray-200">
      <div class="px-6 py-4 border-b border-gray-200 flex items-center justify-between">
        <h2 class="text-lg font-semibold text-gray-900">净资产趋势</h2>
        <div class="flex gap-2">
          <button
            v-for="range in timeRanges"
            :key="range.value"
            @click="selectTimeRange(range.value)"
            :class="[
              'px-3 py-1 text-xs rounded-md font-medium transition-colors',
              selectedTimeRange === range.value
                ? 'bg-blue-600 text-white'
                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            ]"
          >
            {{ range.label }}
          </button>
        </div>
      </div>
      <div class="p-6">
        <div v-if="loadingTrend" class="text-sm text-gray-500 text-center py-12">
          加载中...
        </div>
        <div v-else-if="overallTrendData.length === 0" class="text-sm text-gray-500 text-center py-12">
          暂无趋势数据，请先添加资产或负债记录
        </div>
        <div v-else class="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <!-- 左侧图表 (2/3) -->
          <div class="lg:col-span-2 h-80">
            <Line :data="overallChartData" :options="overallChartOptions" />
          </div>

          <!-- 右侧统计表格 (1/3) -->
          <div class="lg:col-span-1">
            <div class="border border-gray-200 rounded-lg overflow-hidden">
              <table class="min-w-full divide-y divide-gray-200">
                <thead class="bg-gray-50">
                  <tr>
                    <th class="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">指标</th>
                    <th class="px-3 py-2 text-right text-xs font-medium text-gray-500 uppercase">最早</th>
                    <th class="px-3 py-2 text-right text-xs font-medium text-gray-500 uppercase">最新</th>
                    <th class="px-3 py-2 text-right text-xs font-medium text-gray-500 uppercase">变化</th>
                  </tr>
                </thead>
                <tbody class="bg-white divide-y divide-gray-200">
                  <tr v-for="stat in overallStats" :key="stat.name">
                    <td class="px-3 py-2 text-sm font-medium text-gray-900">{{ stat.name }}</td>
                    <td class="px-3 py-2 text-sm text-gray-600 text-right">
                      ${{ formatAmount(stat.earliestValue) }}
                    </td>
                    <td class="px-3 py-2 text-sm text-gray-900 text-right font-medium">
                      ${{ formatAmount(stat.latestValue) }}
                    </td>
                    <td class="px-3 py-2 text-sm text-right">
                      <span :class="getChangeColorClass(stat.change)">
                        {{ stat.change > 0 ? '+' : '' }}{{ formatAmount(stat.change) }}%
                      </span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { assetAccountAPI, liabilityAccountAPI } from '@/api'
import { analysisAPI } from '@/api/analysis'
import { Line } from 'vue-chartjs'
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  ArcElement,
  Title,
  Tooltip,
  Legend,
  Filler
} from 'chart.js'

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  ArcElement,
  Title,
  Tooltip,
  Legend,
  Filler
)

const userId = ref(1) // TODO: 从用户登录状态获取
const assetAccounts = ref([])
const liabilityAccounts = ref([])
const netAssetAllocation = ref({ total: 0, data: [] })
const loading = ref(false)
const loadingTrend = ref(false)

const assetChartCanvas = ref(null)
const liabilityChartCanvas = ref(null)
const netAssetChartCanvas = ref(null)
let assetChartInstance = null
let liabilityChartInstance = null
let netAssetChartInstance = null

const selectedTimeRange = ref('1y')
const timeRanges = [
  { value: '3m', label: '3个月' },
  { value: '6m', label: '6个月' },
  { value: '1y', label: '1年' },
  { value: '3y', label: '3年' },
  { value: 'all', label: '全部' }
]

// 趋势数据
const overallTrendData = ref([])

// 资产分类颜色映射
const ASSET_COLORS = {
  'CASH': '#10b981',
  'STOCKS': '#3b82f6',
  'RETIREMENT_FUND': '#8b5cf6',
  'INSURANCE': '#f59e0b',
  'REAL_ESTATE': '#ef4444',
  'CRYPTOCURRENCY': '#6366f1',
  'PRECIOUS_METALS': '#eab308',
  'OTHER': '#6b7280'
}

// 负债分类颜色映射
const LIABILITY_COLORS = {
  'MORTGAGE': '#dc2626',
  'AUTO_LOAN': '#ea580c',
  'CREDIT_CARD': '#f59e0b',
  'PERSONAL_LOAN': '#f97316',
  'STUDENT_LOAN': '#fb923c',
  'BUSINESS_LOAN': '#fdba74',
  'OTHER': '#9ca3af'
}

// 分类名称映射
const ASSET_NAMES = {
  'CASH': '现金类',
  'STOCKS': '股票投资',
  'RETIREMENT_FUND': '退休基金',
  'INSURANCE': '保险',
  'REAL_ESTATE': '房地产',
  'CRYPTOCURRENCY': '数字货币',
  'PRECIOUS_METALS': '贵金属',
  'OTHER': '其他'
}

const LIABILITY_NAMES = {
  'MORTGAGE': '房贷',
  'AUTO_LOAN': '车贷',
  'CREDIT_CARD': '信用卡',
  'PERSONAL_LOAN': '个人借款',
  'STUDENT_LOAN': '学生贷款',
  'BUSINESS_LOAN': '商业贷款',
  'OTHER': '其他'
}

// 计算总资产（基准货币）
const totalAssets = computed(() => {
  return assetAccounts.value.reduce((sum, acc) => {
    const amount = acc.latestAmountInBaseCurrency || 0
    return sum + Number(amount)
  }, 0)
})

// 计算总负债（基准货币）
const totalLiabilities = computed(() => {
  return liabilityAccounts.value.reduce((sum, acc) => {
    const balance = acc.latestBalanceInBaseCurrency || 0
    return sum + Number(balance)
  }, 0)
})

// 计算净资产
const netWorth = computed(() => {
  return totalAssets.value - totalLiabilities.value
})

// 计算资产负债率
const debtRatio = computed(() => {
  if (totalAssets.value === 0) return '0.0'
  return ((totalLiabilities.value / totalAssets.value) * 100).toFixed(1)
})

// 资产负债率颜色
const debtRatioColor = computed(() => {
  const ratio = parseFloat(debtRatio.value)
  if (ratio < 30) return 'text-green-600'
  if (ratio < 50) return 'text-yellow-600'
  return 'text-red-600'
})

// 账户统计
const totalAssetAccounts = computed(() => assetAccounts.value.length)
const totalLiabilityAccounts = computed(() => liabilityAccounts.value.length)
const activeAssetAccounts = computed(() => assetAccounts.value.filter(acc => acc.isActive).length)
const activeLiabilityAccounts = computed(() => liabilityAccounts.value.filter(acc => acc.isActive).length)
const totalAccounts = computed(() => totalAssetAccounts.value + totalLiabilityAccounts.value)

// 资产分类统计
const assetCategories = computed(() => {
  const categoryMap = {}

  assetAccounts.value.forEach(acc => {
    const type = acc.categoryType
    const amount = Number(acc.latestAmountInBaseCurrency || 0)

    if (!categoryMap[type]) {
      categoryMap[type] = {
        type,
        name: ASSET_NAMES[type] || type,
        color: ASSET_COLORS[type] || '#6b7280',
        total: 0
      }
    }
    categoryMap[type].total += amount
  })

  const categories = Object.values(categoryMap).filter(cat => cat.total > 0)
  const total = categories.reduce((sum, cat) => sum + cat.total, 0)

  return categories.map(cat => ({
    ...cat,
    percentage: total > 0 ? ((cat.total / total) * 100).toFixed(1) : '0.0'
  })).sort((a, b) => b.total - a.total)
})

// 负债分类统计
const liabilityCategories = computed(() => {
  const categoryMap = {}

  liabilityAccounts.value.forEach(acc => {
    const type = acc.categoryType
    const balance = Number(acc.latestBalanceInBaseCurrency || 0)

    if (!categoryMap[type]) {
      categoryMap[type] = {
        type,
        name: LIABILITY_NAMES[type] || type,
        color: LIABILITY_COLORS[type] || '#9ca3af',
        total: 0
      }
    }
    categoryMap[type].total += balance
  })

  const categories = Object.values(categoryMap).filter(cat => cat.total > 0)
  const total = categories.reduce((sum, cat) => sum + cat.total, 0)

  return categories.map(cat => ({
    ...cat,
    percentage: total > 0 ? ((cat.total / total) * 100).toFixed(1) : '0.0'
  })).sort((a, b) => b.total - a.total)
})

// 获取变化百分比的颜色类
const getChangeColorClass = (change) => {
  if (change > 0) return 'text-green-600 font-medium'
  if (change < 0) return 'text-red-600 font-medium'
  return 'text-gray-600'
}

// 计算日期范围
const getDateRange = () => {
  const end = new Date()
  const start = new Date()

  switch (selectedTimeRange.value) {
    case '3m':
      start.setMonth(end.getMonth() - 3)
      break
    case '6m':
      start.setMonth(end.getMonth() - 6)
      break
    case '1y':
      start.setFullYear(end.getFullYear() - 1)
      break
    case '3y':
      start.setFullYear(end.getFullYear() - 3)
      break
    case 'all':
      start.setFullYear(end.getFullYear() - 10)
      break
  }

  return {
    start: start.toISOString().split('T')[0],
    end: end.toISOString().split('T')[0]
  }
}

// 计算综合趋势统计数据
const overallStats = computed(() => {
  if (overallTrendData.value.length === 0) return []

  const earliest = overallTrendData.value[0]
  const latest = overallTrendData.value[overallTrendData.value.length - 1]

  const calculateChange = (earliestVal, latestVal) => {
    if (!earliestVal || earliestVal === 0) return 0
    return ((latestVal - earliestVal) / earliestVal) * 100
  }

  return [
    {
      name: '净资产',
      earliestValue: earliest.netWorth || 0,
      latestValue: latest.netWorth || 0,
      change: calculateChange(earliest.netWorth, latest.netWorth)
    },
    {
      name: '总资产',
      earliestValue: earliest.totalAssets || 0,
      latestValue: latest.totalAssets || 0,
      change: calculateChange(earliest.totalAssets, latest.totalAssets)
    },
    {
      name: '总负债',
      earliestValue: earliest.totalLiabilities || 0,
      latestValue: latest.totalLiabilities || 0,
      change: calculateChange(earliest.totalLiabilities, latest.totalLiabilities)
    }
  ]
})

// 综合趋势图表数据
const overallChartData = computed(() => {
  const labels = overallTrendData.value.map(item => item.date)

  return {
    labels,
    datasets: [
      {
        label: '净资产',
        data: overallTrendData.value.map(item => item.netWorth || 0),
        borderColor: 'rgb(59, 130, 246)',
        backgroundColor: 'rgba(59, 130, 246, 0.1)',
        tension: 0.3,
        fill: true
      },
      {
        label: '总资产',
        data: overallTrendData.value.map(item => item.totalAssets || 0),
        borderColor: 'rgb(34, 197, 94)',
        backgroundColor: 'rgba(34, 197, 94, 0.1)',
        tension: 0.3,
        fill: false
      },
      {
        label: '总负债',
        data: overallTrendData.value.map(item => item.totalLiabilities || 0),
        borderColor: 'rgb(239, 68, 68)',
        backgroundColor: 'rgba(239, 68, 68, 0.1)',
        tension: 0.3,
        fill: false
      }
    ]
  }
})

// 综合趋势图表配置
const overallChartOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      display: true,
      position: 'top'
    },
    tooltip: {
      mode: 'index',
      intersect: false,
      callbacks: {
        label: function(context) {
          let label = context.dataset.label || ''
          if (label) {
            label += ': '
          }
          if (context.parsed.y !== null) {
            label += '$' + context.parsed.y.toLocaleString('zh-CN', {
              minimumFractionDigits: 2,
              maximumFractionDigits: 2
            })
          }
          return label
        }
      }
    }
  },
  scales: {
    y: {
      beginAtZero: true,
      ticks: {
        callback: function(value) {
          return '$' + value.toLocaleString('zh-CN')
        }
      }
    }
  },
  interaction: {
    mode: 'nearest',
    axis: 'x',
    intersect: false
  }
}))

// 格式化金额
function formatAmount(amount) {
  if (!amount && amount !== 0) return '0.00'
  return Number(amount).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

// 加载账户数据
async function loadAccounts() {
  loading.value = true
  try {
    const [assetResponse, liabilityResponse, netAssetResponse] = await Promise.all([
      assetAccountAPI.getAll(),
      liabilityAccountAPI.getAll(),
      analysisAPI.getNetAssetAllocation()
    ])

    if (assetResponse.success) {
      assetAccounts.value = assetResponse.data
    }

    if (liabilityResponse.success) {
      liabilityAccounts.value = liabilityResponse.data
    }

    if (netAssetResponse.success) {
      netAssetAllocation.value = netAssetResponse.data
    }

    await nextTick()
    updateCharts()
  } catch (error) {
    console.error('加载账户失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载综合趋势
async function loadOverallTrend() {
  loadingTrend.value = true
  try {
    const { start, end } = getDateRange()
    const response = await analysisAPI.getOverallTrend(start, end)
    if (response.success) {
      overallTrendData.value = response.data
    }
  } catch (error) {
    console.error('加载综合趋势失败:', error)
    overallTrendData.value = []
  } finally {
    loadingTrend.value = false
  }
}

// 选择时间范围
function selectTimeRange(range) {
  selectedTimeRange.value = range
  loadOverallTrend()
}

// 更新图表
function updateCharts() {
  updateAssetChart()
  updateLiabilityChart()
  updateNetAssetChart()
}

// 更新资产分布图表
function updateAssetChart() {
  if (!assetChartCanvas.value || assetCategories.value.length === 0) {
    if (assetChartInstance) {
      assetChartInstance.destroy()
      assetChartInstance = null
    }
    return
  }

  if (assetChartInstance) {
    assetChartInstance.destroy()
  }

  const ctx = assetChartCanvas.value.getContext('2d')
  assetChartInstance = new ChartJS(ctx, {
    type: 'doughnut',
    data: {
      labels: assetCategories.value.map(c => c.name),
      datasets: [{
        data: assetCategories.value.map(c => c.total),
        backgroundColor: assetCategories.value.map(c => c.color),
        borderWidth: 2,
        borderColor: '#fff'
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          display: false
        },
        tooltip: {
          callbacks: {
            label: function(context) {
              const label = context.label || ''
              const value = formatAmount(context.parsed)
              const percentage = assetCategories.value[context.dataIndex].percentage
              return `${label}: $${value} (${percentage}%)`
            }
          }
        }
      }
    }
  })
}

// 更新负债分布图表
function updateLiabilityChart() {
  if (!liabilityChartCanvas.value || liabilityCategories.value.length === 0) {
    if (liabilityChartInstance) {
      liabilityChartInstance.destroy()
      liabilityChartInstance = null
    }
    return
  }

  if (liabilityChartInstance) {
    liabilityChartInstance.destroy()
  }

  const ctx = liabilityChartCanvas.value.getContext('2d')
  liabilityChartInstance = new ChartJS(ctx, {
    type: 'doughnut',
    data: {
      labels: liabilityCategories.value.map(c => c.name),
      datasets: [{
        data: liabilityCategories.value.map(c => c.total),
        backgroundColor: liabilityCategories.value.map(c => c.color),
        borderWidth: 2,
        borderColor: '#fff'
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          display: false
        },
        tooltip: {
          callbacks: {
            label: function(context) {
              const label = context.label || ''
              const value = formatAmount(context.parsed)
              const percentage = liabilityCategories.value[context.dataIndex].percentage
              return `${label}: $${value} (${percentage}%)`
            }
          }
        }
      }
    }
  })
}

// 更新净资产分布图表
function updateNetAssetChart() {
  if (!netAssetChartCanvas.value || netAssetAllocation.value.data.length === 0) {
    if (netAssetChartInstance) {
      netAssetChartInstance.destroy()
      netAssetChartInstance = null
    }
    return
  }

  if (netAssetChartInstance) {
    netAssetChartInstance.destroy()
  }

  const ctx = netAssetChartCanvas.value.getContext('2d')
  netAssetChartInstance = new ChartJS(ctx, {
    type: 'doughnut',
    data: {
      labels: netAssetAllocation.value.data.map(c => c.name),
      datasets: [{
        data: netAssetAllocation.value.data.map(c => c.netValue),
        backgroundColor: netAssetAllocation.value.data.map(c => c.color),
        borderWidth: 2,
        borderColor: '#fff'
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          display: false
        },
        tooltip: {
          callbacks: {
            label: function(context) {
              const label = context.label || ''
              const value = formatAmount(context.parsed)
              const category = netAssetAllocation.value.data[context.dataIndex]
              const percentage = category.percentage
              return `${label}: $${value} (${percentage}%)`
            }
          }
        }
      }
    }
  })
}

onMounted(() => {
  loadAccounts()
  loadOverallTrend()
})
</script>
