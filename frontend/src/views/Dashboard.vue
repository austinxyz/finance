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
              @click="$router.push('/expenses/batch-update')"
              class="w-full px-4 py-3 text-left border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors flex items-center gap-3"
            >
              <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5 text-orange-600" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
              </svg>
              <span class="font-medium text-gray-900">批量录入支出</span>
            </button>
            <button
              @click="$router.push('/expenses/categories')"
              class="w-full px-4 py-3 text-left border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors flex items-center gap-3"
            >
              <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5 text-amber-600" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                <polyline points="14 2 14 8 20 8"></polyline>
                <line x1="16" y1="13" x2="8" y2="13"></line>
                <line x1="16" y1="17" x2="8" y2="17"></line>
                <polyline points="10 9 9 9 8 9"></polyline>
              </svg>
              <span class="font-medium text-gray-900">支出分类与记录</span>
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

    <!-- 年度净资产趋势图 -->
    <div class="bg-white rounded-lg shadow border border-gray-200">
      <div class="px-6 py-4 border-b border-gray-200 flex items-center justify-between">
        <h2 class="text-lg font-semibold text-gray-900">年度净资产趋势</h2>
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
          <div class="lg:col-span-2 h-96">
            <canvas ref="annualNetWorthChartCanvas"></canvas>
          </div>

          <!-- 右侧年度汇总表格 (1/3) -->
          <div class="lg:col-span-1">
            <div class="border border-gray-200 rounded-lg overflow-hidden">
              <table class="min-w-full divide-y divide-gray-200">
                <thead class="bg-gray-50">
                  <tr>
                    <th class="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">年份</th>
                    <th class="px-3 py-2 text-right text-xs font-medium text-gray-500 uppercase">净资产</th>
                    <th class="px-3 py-2 text-right text-xs font-medium text-gray-500 uppercase">同比</th>
                  </tr>
                </thead>
                <tbody class="bg-white divide-y divide-gray-200">
                  <tr v-for="(item, index) in annualSummaryData" :key="item.year" class="hover:bg-gray-50">
                    <td class="px-3 py-2 text-sm font-medium text-gray-900">{{ item.year }}</td>
                    <td class="px-3 py-2 text-sm text-gray-900 text-right font-medium">
                      ${{ formatAmount(item.netWorth) }}
                    </td>
                    <td class="px-3 py-2 text-sm text-right">
                      <div v-if="item.yoyChange !== null">
                        <div :class="getChangeColorClass(item.yoyChangePct)" class="font-medium">
                          {{ item.yoyChange > 0 ? '+' : '' }}${{ formatAmount(Math.abs(item.yoyChange)) }}
                        </div>
                        <div :class="getChangeColorClass(item.yoyChangePct)" class="text-xs">
                          ({{ item.yoyChangePct > 0 ? '+' : '' }}{{ item.yoyChangePct.toFixed(1) }}%)
                        </div>
                      </div>
                      <div v-else class="text-xs text-gray-400">-</div>
                    </td>
                  </tr>
                </tbody>
                <tfoot class="bg-gray-50 border-t-2 border-gray-300">
                  <tr>
                    <td class="px-3 py-3 text-sm font-bold text-gray-900">累计</td>
                    <td class="px-3 py-3 text-sm text-right font-bold text-blue-600">
                      ${{ formatAmount(totalNetWorth) }}
                    </td>
                    <td class="px-3 py-3 text-sm text-right">
                      <div v-if="totalChange !== null && annualizedGrowthRate !== null">
                        <div :class="getChangeColorClass(totalChange)" class="font-bold">
                          {{ totalChange > 0 ? '+' : '' }}${{ formatAmount(Math.abs(totalChange)) }}
                        </div>
                        <div :class="getChangeColorClass(annualizedGrowthRate)" class="text-xs mt-0.5">
                          (年均{{ annualizedGrowthRate > 0 ? '+' : '' }}{{ annualizedGrowthRate.toFixed(2) }}%)
                        </div>
                      </div>
                    </td>
                  </tr>
                </tfoot>
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
import { annualSummaryAPI } from '@/api/annualSummary'
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  BarController,
  ArcElement,
  DoughnutController,
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
  BarElement,
  BarController,
  ArcElement,
  DoughnutController,
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
const annualNetWorthChartCanvas = ref(null)
let assetChartInstance = null
let liabilityChartInstance = null
let netAssetChartInstance = null
let annualNetWorthChartInstance = null

const selectedTimeRange = ref('3y')
const timeRanges = [
  { value: '1y', label: '近1年' },
  { value: '3y', label: '近3年' },
  { value: '5y', label: '近5年' },
  { value: '10y', label: '近10年' },
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

// 计算年份范围
const getYearRange = () => {
  let years = 5 // 默认5年

  switch (selectedTimeRange.value) {
    case '1y':
      years = 1
      break
    case '3y':
      years = 3
      break
    case '5y':
      years = 5
      break
    case '10y':
      years = 10
      break
    case 'all':
      years = 20
      break
  }

  return { years }
}

// 计算日期范围
const getDateRange = () => {
  const end = new Date()
  const start = new Date()

  switch (selectedTimeRange.value) {
    case '1y':
      start.setFullYear(end.getFullYear() - 1)
      break
    case '3y':
      start.setFullYear(end.getFullYear() - 3)
      break
    case '5y':
      start.setFullYear(end.getFullYear() - 5)
      break
    case '10y':
      start.setFullYear(end.getFullYear() - 10)
      break
    case 'all':
      start.setFullYear(end.getFullYear() - 20)
      break
  }

  return {
    start: start.toISOString().split('T')[0],
    end: end.toISOString().split('T')[0]
  }
}

// 计算年度汇总数据（用于右侧表格）
const annualSummaryData = computed(() => {
  if (overallTrendData.value.length === 0) return []

  const data = overallTrendData.value.map((item, index) => {
    const year = new Date(item.date).getFullYear()
    const netWorth = item.netWorth || 0

    let yoyChange = null
    let yoyChangePct = null

    if (index > 0) {
      const prevNetWorth = overallTrendData.value[index - 1].netWorth || 0
      yoyChange = netWorth - prevNetWorth
      yoyChangePct = prevNetWorth !== 0 ? (yoyChange / prevNetWorth) * 100 : 0
    }

    return {
      year,
      netWorth,
      yoyChange,
      yoyChangePct
    }
  })

  return data.reverse() // 最新年份在上
})

// 计算累计净资产（最新年份）
const totalNetWorth = computed(() => {
  if (annualSummaryData.value.length === 0) return 0
  return annualSummaryData.value[0].netWorth
})

// 计算总变化（从最早到最新）
const totalChange = computed(() => {
  if (annualSummaryData.value.length < 2) return null
  const latest = annualSummaryData.value[0].netWorth
  const earliest = annualSummaryData.value[annualSummaryData.value.length - 1].netWorth
  return latest - earliest
})

const totalChangePct = computed(() => {
  if (totalChange.value === null) return null
  const earliest = annualSummaryData.value[annualSummaryData.value.length - 1].netWorth
  return earliest !== 0 ? (totalChange.value / earliest) * 100 : 0
})

// 计算年化复合增长率（CAGR）
const annualizedGrowthRate = computed(() => {
  if (annualSummaryData.value.length < 2) return null
  const latest = annualSummaryData.value[0].netWorth
  const earliest = annualSummaryData.value[annualSummaryData.value.length - 1].netWorth
  const years = annualSummaryData.value[0].year - annualSummaryData.value[annualSummaryData.value.length - 1].year

  if (years === 0 || earliest <= 0) return null

  // CAGR = (最新值/最早值)^(1/年数) - 1
  return (Math.pow(latest / earliest, 1 / years) - 1) * 100
})

// 更新年度净资产图表（双Y轴：柱状图+折线图）
function updateAnnualNetWorthChart() {
  if (!annualNetWorthChartCanvas.value) {
    console.warn('Canvas element not ready')
    return
  }

  if (overallTrendData.value.length === 0) {
    console.warn('No trend data available')
    return
  }

  if (annualNetWorthChartInstance) {
    annualNetWorthChartInstance.destroy()
  }

  console.log('Creating annual net worth chart with', overallTrendData.value.length, 'data points')

  // 准备数据
  const years = overallTrendData.value.map(item => new Date(item.date).getFullYear())
  const netWorths = overallTrendData.value.map(item => item.netWorth || 0)

  // 计算同比增长率
  const growthRates = overallTrendData.value.map((item, index) => {
    if (index === 0) return null
    const current = item.netWorth || 0
    const previous = overallTrendData.value[index - 1].netWorth || 0
    return previous !== 0 ? ((current - previous) / previous) * 100 : 0
  })

  const ctx = annualNetWorthChartCanvas.value.getContext('2d')
  annualNetWorthChartInstance = new ChartJS(ctx, {
    type: 'bar',
    data: {
      labels: years,
      datasets: [
        {
          label: '净资产',
          data: netWorths,
          backgroundColor: 'rgba(59, 130, 246, 0.7)',
          borderColor: 'rgb(59, 130, 246)',
          borderWidth: 1,
          yAxisID: 'y'
        },
        {
          label: '同比增长率',
          data: growthRates,
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
          position: 'top',
        },
        tooltip: {
          callbacks: {
            label: function(context) {
              if (context.dataset.label === '净资产') {
                return context.dataset.label + ': $' + Number(context.parsed.y).toLocaleString('en-US', {
                  minimumFractionDigits: 2,
                  maximumFractionDigits: 2
                })
              } else {
                return context.dataset.label + ': ' + (context.parsed.y !== null ? context.parsed.y.toFixed(2) + '%' : '-')
              }
            }
          }
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
                return '$' + (value / 1000000).toFixed(1) + 'M'
              } else if (value >= 1000) {
                return '$' + (value / 1000).toFixed(1) + 'K'
              }
              return '$' + value.toFixed(0)
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
            }
          }
        }
      }
    }
  })
}

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
    // 使用AnnualSummary API代替OverallTrend API以确保数据一致性
    const { years } = getYearRange()
    const response = await annualSummaryAPI.getRecent(1, years) // familyId=1
    if (response.success) {
      // 转换AnnualSummary数据格式为OverallTrend格式
      overallTrendData.value = response.data
        .map(item => ({
          date: item.summaryDate,
          netWorth: item.netWorth,
          totalAssets: item.totalAssets,
          totalLiabilities: item.totalLiabilities
        }))
        .reverse() // AnnualSummary返回的是倒序，需要reverse
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

// 监听趋势数据变化，自动更新图表
watch(overallTrendData, async () => {
  if (overallTrendData.value.length > 0) {
    // 使用setTimeout确保DOM完全渲染
    setTimeout(() => {
      if (annualNetWorthChartCanvas.value) {
        updateAnnualNetWorthChart()
      }
    }, 100)
  }
}, { deep: true })

onMounted(() => {
  loadAccounts()
  loadOverallTrend()
})
</script>
