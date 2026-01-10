<template>
  <div class="space-y-4 md:space-y-6">
    <!-- 页头、家庭选择和日期选择 -->
    <div class="bg-white rounded-lg shadow p-3 md:p-4">
      <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-3 md:gap-0 mb-4">
        <h2 class="text-md md:text-lg font-semibold text-gray-900">财务指标</h2>
        <div class="flex flex-col sm:flex-row items-stretch sm:items-center gap-2 md:gap-4">
          <div class="flex items-center gap-2">
            <label class="text-xs md:text-sm font-medium text-gray-700">选择家庭：</label>
            <select
              v-model="selectedFamilyId"
              @change="onFamilyChange"
              class="px-2 md:px-3 py-1.5 md:py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary text-sm"
            >
              <option v-for="family in families" :key="family.id" :value="family.id">
                {{ family.familyName }}
              </option>
            </select>
          </div>
          <div class="flex items-center gap-2">
            <label class="text-xs md:text-sm font-medium text-gray-700">查询日期：</label>
          <input
            v-model="selectedDate"
            type="date"
            @change="loadMetrics"
            class="px-2 md:px-3 py-1.5 md:py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary text-sm"
          />
          <button
            v-if="selectedDate"
            @click="clearDate"
            class="px-2 md:px-3 py-1.5 md:py-2 text-sm text-gray-600 hover:text-gray-800"
          >
            清除
          </button>
          </div>
        </div>
      </div>
      <div class="text-xs md:text-sm text-gray-600">
        <span v-if="!selectedDate && enhancedMetrics.asOfDate" class="flex items-center gap-2">
          <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4 text-blue-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
            <line x1="16" y1="2" x2="16" y2="6"></line>
            <line x1="8" y1="2" x2="8" y2="6"></line>
            <line x1="3" y1="10" x2="21" y2="10"></line>
          </svg>
          <span>
            <span class="font-medium text-gray-700">数据日期:</span>
            <span class="text-blue-600 font-semibold">{{ formatDate(enhancedMetrics.asOfDate) }}</span>
            <span class="text-gray-500 ml-2">(显示最新可用数据)</span>
          </span>
        </span>
        <span v-else-if="selectedDate">
          <span class="font-medium text-gray-700">查询日期:</span> {{ selectedDate }}
          <span v-if="enhancedMetrics.asOfDate && enhancedMetrics.asOfDate !== selectedDate" class="ml-2">
            <span class="text-gray-500">→ 实际数据日期: </span>
            <span class="text-blue-600 font-semibold">{{ formatDate(enhancedMetrics.asOfDate) }}</span>
          </span>
        </span>
      </div>
    </div>

    <!-- 加载中状态 -->
    <div v-if="loading" class="flex items-center justify-center py-12">
      <div class="text-gray-500">加载中...</div>
    </div>

    <!-- 财务指标内容 -->
    <div v-else class="space-y-4 md:space-y-6">
      <!-- 基础指标卡片 -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-3 md:gap-4">
        <!-- 总资产 -->
        <div class="bg-white rounded-lg shadow p-4 md:p-6 border-l-4 border-green-500">
          <div class="text-xs md:text-sm text-gray-500 mb-1">总资产</div>
          <div class="text-lg md:text-2xl font-bold text-green-600">
            ${{ formatNumber(enhancedMetrics.totalAssets) }}
          </div>
        </div>

        <!-- 总负债 -->
        <div class="bg-white rounded-lg shadow p-4 md:p-6 border-l-4 border-red-500">
          <div class="text-xs md:text-sm text-gray-500 mb-1">总负债</div>
          <div class="text-lg md:text-2xl font-bold text-red-600">
            ${{ formatNumber(enhancedMetrics.totalLiabilities) }}
          </div>
        </div>

        <!-- 净资产 -->
        <div class="bg-white rounded-lg shadow p-4 md:p-6 border-l-4 border-blue-500">
          <div class="text-xs md:text-sm text-gray-500 mb-1">净资产</div>
          <div class="text-lg md:text-2xl font-bold text-blue-600">
            ${{ formatNumber(enhancedMetrics.netWorth) }}
          </div>
        </div>
      </div>

      <!-- 财务比率 -->
      <div class="bg-white rounded-lg shadow p-3 md:p-6">
        <h3 class="text-md md:text-lg font-semibold text-gray-900 mb-4">财务比率</h3>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4 md:gap-6">
          <!-- 资产负债率 -->
          <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-3 md:gap-0 p-4 bg-gray-50 rounded-lg">
            <div>
              <div class="text-xs md:text-sm text-gray-600">资产负债率</div>
              <div class="text-xs text-gray-500 mt-1">总负债 / 总资产</div>
            </div>
            <div class="text-right">
              <div class="text-lg md:text-2xl font-bold" :class="getDebtRatioColor(enhancedMetrics.debtToAssetRatio)">
                {{ formatNumber(enhancedMetrics.debtToAssetRatio) }}%
              </div>
              <div class="text-xs text-gray-500 mt-1">{{ getDebtRatioLevel(enhancedMetrics.debtToAssetRatio) }}</div>
            </div>
          </div>

          <!-- 流动性比率 -->
          <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-3 md:gap-0 p-4 bg-gray-50 rounded-lg">
            <div>
              <div class="text-xs md:text-sm text-gray-600">流动性比率</div>
              <div class="text-xs text-gray-500 mt-1">现金类资产 / 总资产</div>
            </div>
            <div class="text-right">
              <div class="text-lg md:text-2xl font-bold" :class="getLiquidityRatioColor(enhancedMetrics.liquidityRatio)">
                {{ formatNumber(enhancedMetrics.liquidityRatio) }}%
              </div>
              <div class="text-xs text-gray-500 mt-1">现金: ${{ formatNumber(enhancedMetrics.cashAmount) }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 变化趋势 -->
      <div class="bg-white rounded-lg shadow p-3 md:p-6">
        <h3 class="text-md md:text-lg font-semibold text-gray-900 mb-4">资产变化</h3>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4 md:gap-6">
          <!-- 月度变化 -->
          <div class="border border-gray-200 rounded-lg p-4">
            <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-3 md:gap-0 mb-3">
              <div class="text-xs md:text-sm font-medium text-gray-700">月度变化</div>
              <div class="text-xs text-gray-500">
                {{ enhancedMetrics.previousMonthDate ? formatDate(enhancedMetrics.previousMonthDate) : '-' }} → {{ formatDate(enhancedMetrics.asOfDate) }}
              </div>
            </div>
            <div class="space-y-2">
              <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-3 md:gap-0">
                <span class="text-xs md:text-sm text-gray-600">变化金额</span>
                <span class="font-semibold" :class="getChangeColor(enhancedMetrics.monthlyChange)">
                  {{ enhancedMetrics.monthlyChange >= 0 ? '+' : '' }}${{ formatNumber(enhancedMetrics.monthlyChange) }}
                </span>
              </div>
              <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-3 md:gap-0">
                <span class="text-xs md:text-sm text-gray-600">变化率</span>
                <span class="font-semibold" :class="getChangeColor(enhancedMetrics.monthlyChange)">
                  {{ enhancedMetrics.monthlyChangeRate >= 0 ? '+' : '' }}{{ formatNumber(enhancedMetrics.monthlyChangeRate) }}%
                </span>
              </div>
              <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-3 md:gap-0 pt-2 border-t border-gray-100">
                <span class="text-xs text-gray-500">上月净资产</span>
                <span class="text-xs text-gray-600">${{ formatNumber(enhancedMetrics.previousMonthNetWorth) }}</span>
              </div>
            </div>
          </div>

          <!-- 年度变化 -->
          <div class="border border-gray-200 rounded-lg p-4">
            <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-3 md:gap-0 mb-3">
              <div class="text-xs md:text-sm font-medium text-gray-700">年度变化</div>
              <div class="text-xs text-gray-500">
                {{ enhancedMetrics.previousYearDate ? formatDate(enhancedMetrics.previousYearDate) : '-' }} → {{ formatDate(enhancedMetrics.asOfDate) }}
              </div>
            </div>
            <div class="space-y-2">
              <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-3 md:gap-0">
                <span class="text-xs md:text-sm text-gray-600">变化金额</span>
                <span class="font-semibold" :class="getChangeColor(enhancedMetrics.yearlyChange)">
                  {{ enhancedMetrics.yearlyChange >= 0 ? '+' : '' }}${{ formatNumber(enhancedMetrics.yearlyChange) }}
                </span>
              </div>
              <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-3 md:gap-0">
                <span class="text-xs md:text-sm text-gray-600">变化率</span>
                <span class="font-semibold" :class="getChangeColor(enhancedMetrics.yearlyChange)">
                  {{ enhancedMetrics.yearlyChangeRate >= 0 ? '+' : '' }}{{ formatNumber(enhancedMetrics.yearlyChangeRate) }}%
                </span>
              </div>
              <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-3 md:gap-0 pt-2 border-t border-gray-100">
                <span class="text-xs text-gray-500">去年同期净资产</span>
                <span class="text-xs text-gray-600">${{ formatNumber(enhancedMetrics.previousYearNetWorth) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 财务健康度评估（简化版） -->
      <div class="bg-white rounded-lg shadow p-3 md:p-6">
        <h3 class="text-md md:text-lg font-semibold text-gray-900 mb-4">财务健康度评估（简化版）</h3>
        <div class="space-y-4">
          <!-- 负债压力 -->
          <div class="flex items-start gap-2 md:gap-3">
            <div class="flex-shrink-0 w-8 h-8 md:w-10 md:h-10 rounded-full flex items-center justify-center" :class="getDebtPressureIcon(enhancedMetrics.debtToAssetRatio).bgClass">
              <span class="text-base md:text-lg">{{ getDebtPressureIcon(enhancedMetrics.debtToAssetRatio).icon }}</span>
            </div>
            <div class="flex-1">
              <div class="text-sm md:text-base font-medium text-gray-900">负债压力</div>
              <div class="text-xs md:text-sm text-gray-600 mt-1">{{ getDebtPressureMessage(enhancedMetrics.debtToAssetRatio) }}</div>
            </div>
          </div>

          <!-- 流动性状况 -->
          <div class="flex items-start gap-2 md:gap-3">
            <div class="flex-shrink-0 w-8 h-8 md:w-10 md:h-10 rounded-full flex items-center justify-center" :class="getLiquidityIcon(enhancedMetrics.liquidityRatio).bgClass">
              <span class="text-base md:text-lg">{{ getLiquidityIcon(enhancedMetrics.liquidityRatio).icon }}</span>
            </div>
            <div class="flex-1">
              <div class="text-sm md:text-base font-medium text-gray-900">流动性状况</div>
              <div class="text-xs md:text-sm text-gray-600 mt-1">{{ getLiquidityMessage(enhancedMetrics.liquidityRatio) }}</div>
            </div>
          </div>

          <!-- 资产增长 -->
          <div class="flex items-start gap-2 md:gap-3">
            <div class="flex-shrink-0 w-8 h-8 md:w-10 md:h-10 rounded-full flex items-center justify-center" :class="getGrowthIcon(enhancedMetrics.yearlyChangeRate).bgClass">
              <span class="text-base md:text-lg">{{ getGrowthIcon(enhancedMetrics.yearlyChangeRate).icon }}</span>
            </div>
            <div class="flex-1">
              <div class="text-sm md:text-base font-medium text-gray-900">资产增长</div>
              <div class="text-xs md:text-sm text-gray-600 mt-1">{{ getGrowthMessage(enhancedMetrics.yearlyChangeRate) }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 现金流分析 -->
      <CashFlowMetrics :metrics="enhancedMetrics" />

      <!-- 投资收益概览 -->
      <InvestmentMetrics :metrics="enhancedMetrics" />

      <!-- 财务健康评分 -->
      <HealthScoreCard :health-score="enhancedMetrics.healthScore" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { analysisAPI } from '@/api/analysis'
import { familyAPI } from '@/api/family'
import CashFlowMetrics from '@/components/metrics/CashFlowMetrics.vue'
import InvestmentMetrics from '@/components/metrics/InvestmentMetrics.vue'
import HealthScoreCard from '@/components/metrics/HealthScoreCard.vue'

const loading = ref(false)
const selectedDate = ref('')
const families = ref([])
const selectedFamilyId = ref(null) // 将从默认家庭API获取
const enhancedMetrics = ref({
  // 基础指标
  totalAssets: 0,
  totalLiabilities: 0,
  netWorth: 0,
  debtToAssetRatio: 0,
  liquidityRatio: 0,
  cashAmount: 0,
  monthlyChange: 0,
  monthlyChangeRate: 0,
  yearlyChange: 0,
  yearlyChangeRate: 0,
  asOfDate: '',
  previousMonthDate: '',
  previousYearDate: '',
  previousMonthNetWorth: 0,
  previousYearNetWorth: 0,
  // 现金流指标
  annualTotalIncome: 0,
  annualWorkIncome: 0,
  annualInvestmentIncome: 0,
  annualOtherIncome: 0,
  annualTotalExpense: 0,
  netCashFlow: 0,
  savingsRate: 0,
  expenseRatio: 0,
  incomeGrowthRate: 0,
  expenseGrowthRate: 0,
  // 投资指标
  totalInvested: 0,
  currentInvestmentValue: 0,
  totalInvestmentReturn: 0,
  investmentReturnRate: 0,
  topCategories: [],
  // 健康评分
  healthScore: {
    totalScore: 0,
    grade: 'D',
    scores: {
      debtManagement: 0,
      liquidity: 0,
      savings: 0,
      investment: 0,
      growth: 0
    },
    recommendations: []
  }
})

// 格式化数字
const formatNumber = (num) => {
  if (!num && num !== 0) return '0.00'
  return parseFloat(num).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '-'
  // 如果是 YYYY-MM-DD 格式，转换为 YYYY年MM月DD日
  if (typeof dateString === 'string' && dateString.match(/^\d{4}-\d{2}-\d{2}$/)) {
    const [year, month, day] = dateString.split('-')
    return `${year}年${month}月${day}日`
  }
  // 兼容其他格式
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
}

// 获取资产负债率颜色
const getDebtRatioColor = (ratio) => {
  if (ratio < 30) return 'text-green-600'
  if (ratio < 60) return 'text-yellow-600'
  return 'text-red-600'
}

// 获取资产负债率等级
const getDebtRatioLevel = (ratio) => {
  if (ratio < 30) return '优秀'
  if (ratio < 60) return '良好'
  if (ratio < 80) return '偏高'
  return '高风险'
}

// 获取流动性比率颜色
const getLiquidityRatioColor = (ratio) => {
  if (ratio >= 20) return 'text-green-600'
  if (ratio >= 10) return 'text-yellow-600'
  return 'text-red-600'
}

// 获取变化颜色
const getChangeColor = (change) => {
  if (change > 0) return 'text-green-600'
  if (change < 0) return 'text-red-600'
  return 'text-gray-600'
}

// 获取负债压力图标
const getDebtPressureIcon = (ratio) => {
  if (ratio < 30) return { icon: '✅', bgClass: 'bg-green-100' }
  if (ratio < 60) return { icon: '⚠️', bgClass: 'bg-yellow-100' }
  return { icon: '❗', bgClass: 'bg-red-100' }
}

// 获取负债压力信息
const getDebtPressureMessage = (ratio) => {
  if (ratio < 30) return '负债压力较小,财务状况良好'
  if (ratio < 60) return '负债比例适中,建议关注负债结构'
  if (ratio < 80) return '负债压力偏大,建议加快债务偿还'
  return '负债压力过大,需要优先处理债务问题'
}

// 获取流动性图标
const getLiquidityIcon = (ratio) => {
  if (ratio >= 20) return { icon: '💰', bgClass: 'bg-green-100' }
  if (ratio >= 10) return { icon: '💵', bgClass: 'bg-yellow-100' }
  return { icon: '⚠️', bgClass: 'bg-red-100' }
}

// 获取流动性信息
const getLiquidityMessage = (ratio) => {
  if (ratio >= 20) return '流动性充足,应急资金储备良好'
  if (ratio >= 10) return '流动性尚可,建议适当增加现金储备'
  return '流动性不足,建议增加应急资金储备'
}

// 获取增长图标
const getGrowthIcon = (rate) => {
  if (rate > 10) return { icon: '📈', bgClass: 'bg-green-100' }
  if (rate > 0) return { icon: '📊', bgClass: 'bg-blue-100' }
  return { icon: '📉', bgClass: 'bg-red-100' }
}

// 获取增长信息
const getGrowthMessage = (rate) => {
  if (rate > 10) return `年度增长率 ${formatNumber(rate)}%,资产增长良好`
  if (rate > 0) return `年度增长率 ${formatNumber(rate)}%,保持稳定增长`
  return `年度增长率 ${formatNumber(rate)}%,需要关注资产配置`
}

// 加载财务指标
const loadMetrics = async () => {
  loading.value = true
  try {
    const response = await analysisAPI.getEnhancedFinancialMetrics(null, selectedFamilyId.value, selectedDate.value || null)
    if (response.success) {
      enhancedMetrics.value = response.data
    }
  } catch (error) {
    console.error('加载财务指标失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载家庭列表
const loadFamilies = async () => {
  try {
    const response = await familyAPI.getDefault()

    // getDefault() 返回单个家庭对象，需要包装成数组
    // 响应拦截器已经解包一层，所以response就是 { success: true, data: {...} }
    if (response && response.success && response.data && response.data.id) {
      families.value = [response.data]

      // 设置默认选中
      if (!selectedFamilyId.value) {
        selectedFamilyId.value = response.data.id
      }
    } else {
      families.value = []
      console.error('获取默认家庭失败: 返回数据格式错误', response)
    }
  } catch (error) {
    console.error('加载家庭列表失败:', error)
    families.value = []
  }
}

// 家庭切换事件处理
const onFamilyChange = () => {
  loadMetrics()
}

// 清除日期
const clearDate = () => {
  selectedDate.value = ''
  loadMetrics()
}

// 监听selectedFamilyId变化，自动加载数据
watch(selectedFamilyId, (newId) => {
  if (newId) {
    loadMetrics()
  }
})

onMounted(async () => {
  await loadFamilies()
  // loadFamilies会设置selectedFamilyId，然后watcher会自动加载数据
})
</script>
