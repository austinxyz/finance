<template>
  <div class="space-y-6">
    <!-- 页头和日期选择 -->
    <div class="bg-white rounded-lg shadow p-4">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-lg font-semibold text-gray-900">财务指标</h2>
        <div class="flex items-center gap-2">
          <label class="text-sm font-medium text-gray-700">查询日期：</label>
          <input
            v-model="selectedDate"
            type="date"
            @change="loadMetrics"
            class="px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary text-sm"
          />
          <button
            v-if="selectedDate"
            @click="clearDate"
            class="px-3 py-2 text-sm text-gray-600 hover:text-gray-800"
          >
            清除
          </button>
        </div>
      </div>
      <div class="text-sm text-gray-600">
        <span v-if="!selectedDate && metrics.asOfDate" class="flex items-center gap-2">
          <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4 text-blue-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
            <line x1="16" y1="2" x2="16" y2="6"></line>
            <line x1="8" y1="2" x2="8" y2="6"></line>
            <line x1="3" y1="10" x2="21" y2="10"></line>
          </svg>
          <span>
            <span class="font-medium text-gray-700">数据日期:</span>
            <span class="text-blue-600 font-semibold">{{ formatDate(metrics.asOfDate) }}</span>
            <span class="text-gray-500 ml-2">(显示最新可用数据)</span>
          </span>
        </span>
        <span v-else-if="selectedDate">
          <span class="font-medium text-gray-700">查询日期:</span> {{ selectedDate }}
          <span v-if="metrics.asOfDate && metrics.asOfDate !== selectedDate" class="ml-2">
            <span class="text-gray-500">→ 实际数据日期: </span>
            <span class="text-blue-600 font-semibold">{{ formatDate(metrics.asOfDate) }}</span>
          </span>
        </span>
      </div>
    </div>

    <!-- 加载中状态 -->
    <div v-if="loading" class="flex items-center justify-center py-12">
      <div class="text-gray-500">加载中...</div>
    </div>

    <!-- 财务指标内容 -->
    <div v-else class="space-y-6">
      <!-- 基础指标卡片 -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <!-- 总资产 -->
        <div class="bg-white rounded-lg shadow p-6 border-l-4 border-green-500">
          <div class="text-sm text-gray-500 mb-1">总资产</div>
          <div class="text-2xl font-bold text-green-600">
            ${{ formatNumber(metrics.totalAssets) }}
          </div>
        </div>

        <!-- 总负债 -->
        <div class="bg-white rounded-lg shadow p-6 border-l-4 border-red-500">
          <div class="text-sm text-gray-500 mb-1">总负债</div>
          <div class="text-2xl font-bold text-red-600">
            ${{ formatNumber(metrics.totalLiabilities) }}
          </div>
        </div>

        <!-- 净资产 -->
        <div class="bg-white rounded-lg shadow p-6 border-l-4 border-blue-500">
          <div class="text-sm text-gray-500 mb-1">净资产</div>
          <div class="text-2xl font-bold text-blue-600">
            ${{ formatNumber(metrics.netWorth) }}
          </div>
        </div>
      </div>

      <!-- 财务比率 -->
      <div class="bg-white rounded-lg shadow p-6">
        <h3 class="text-lg font-semibold text-gray-900 mb-4">财务比率</h3>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <!-- 资产负债率 -->
          <div class="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
            <div>
              <div class="text-sm text-gray-600">资产负债率</div>
              <div class="text-xs text-gray-500 mt-1">总负债 / 总资产</div>
            </div>
            <div class="text-right">
              <div class="text-2xl font-bold" :class="getDebtRatioColor(metrics.debtToAssetRatio)">
                {{ formatNumber(metrics.debtToAssetRatio) }}%
              </div>
              <div class="text-xs text-gray-500 mt-1">{{ getDebtRatioLevel(metrics.debtToAssetRatio) }}</div>
            </div>
          </div>

          <!-- 流动性比率 -->
          <div class="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
            <div>
              <div class="text-sm text-gray-600">流动性比率</div>
              <div class="text-xs text-gray-500 mt-1">现金类资产 / 总资产</div>
            </div>
            <div class="text-right">
              <div class="text-2xl font-bold" :class="getLiquidityRatioColor(metrics.liquidityRatio)">
                {{ formatNumber(metrics.liquidityRatio) }}%
              </div>
              <div class="text-xs text-gray-500 mt-1">现金: ${{ formatNumber(metrics.cashAmount) }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 变化趋势 -->
      <div class="bg-white rounded-lg shadow p-6">
        <h3 class="text-lg font-semibold text-gray-900 mb-4">资产变化</h3>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <!-- 月度变化 -->
          <div class="border border-gray-200 rounded-lg p-4">
            <div class="flex items-center justify-between mb-3">
              <div class="text-sm font-medium text-gray-700">月度变化</div>
              <div class="text-xs text-gray-500">
                {{ metrics.previousMonthDate ? formatDate(metrics.previousMonthDate) : '-' }} → {{ formatDate(metrics.asOfDate) }}
              </div>
            </div>
            <div class="space-y-2">
              <div class="flex items-center justify-between">
                <span class="text-sm text-gray-600">变化金额</span>
                <span class="font-semibold" :class="getChangeColor(metrics.monthlyChange)">
                  {{ metrics.monthlyChange >= 0 ? '+' : '' }}${{ formatNumber(metrics.monthlyChange) }}
                </span>
              </div>
              <div class="flex items-center justify-between">
                <span class="text-sm text-gray-600">变化率</span>
                <span class="font-semibold" :class="getChangeColor(metrics.monthlyChange)">
                  {{ metrics.monthlyChangeRate >= 0 ? '+' : '' }}{{ formatNumber(metrics.monthlyChangeRate) }}%
                </span>
              </div>
              <div class="flex items-center justify-between pt-2 border-t border-gray-100">
                <span class="text-xs text-gray-500">上月净资产</span>
                <span class="text-xs text-gray-600">${{ formatNumber(metrics.previousMonthNetWorth) }}</span>
              </div>
            </div>
          </div>

          <!-- 年度变化 -->
          <div class="border border-gray-200 rounded-lg p-4">
            <div class="flex items-center justify-between mb-3">
              <div class="text-sm font-medium text-gray-700">年度变化</div>
              <div class="text-xs text-gray-500">
                {{ metrics.previousYearDate ? formatDate(metrics.previousYearDate) : '-' }} → {{ formatDate(metrics.asOfDate) }}
              </div>
            </div>
            <div class="space-y-2">
              <div class="flex items-center justify-between">
                <span class="text-sm text-gray-600">变化金额</span>
                <span class="font-semibold" :class="getChangeColor(metrics.yearlyChange)">
                  {{ metrics.yearlyChange >= 0 ? '+' : '' }}${{ formatNumber(metrics.yearlyChange) }}
                </span>
              </div>
              <div class="flex items-center justify-between">
                <span class="text-sm text-gray-600">变化率</span>
                <span class="font-semibold" :class="getChangeColor(metrics.yearlyChange)">
                  {{ metrics.yearlyChangeRate >= 0 ? '+' : '' }}{{ formatNumber(metrics.yearlyChangeRate) }}%
                </span>
              </div>
              <div class="flex items-center justify-between pt-2 border-t border-gray-100">
                <span class="text-xs text-gray-500">去年同期净资产</span>
                <span class="text-xs text-gray-600">${{ formatNumber(metrics.previousYearNetWorth) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 财务健康度评估 -->
      <div class="bg-white rounded-lg shadow p-6">
        <h3 class="text-lg font-semibold text-gray-900 mb-4">财务健康度评估</h3>
        <div class="space-y-4">
          <!-- 负债压力 -->
          <div class="flex items-start gap-3">
            <div class="flex-shrink-0 w-8 h-8 rounded-full flex items-center justify-center" :class="getDebtPressureIcon(metrics.debtToAssetRatio).bgClass">
              <span class="text-lg">{{ getDebtPressureIcon(metrics.debtToAssetRatio).icon }}</span>
            </div>
            <div class="flex-1">
              <div class="font-medium text-gray-900">负债压力</div>
              <div class="text-sm text-gray-600 mt-1">{{ getDebtPressureMessage(metrics.debtToAssetRatio) }}</div>
            </div>
          </div>

          <!-- 流动性状况 -->
          <div class="flex items-start gap-3">
            <div class="flex-shrink-0 w-8 h-8 rounded-full flex items-center justify-center" :class="getLiquidityIcon(metrics.liquidityRatio).bgClass">
              <span class="text-lg">{{ getLiquidityIcon(metrics.liquidityRatio).icon }}</span>
            </div>
            <div class="flex-1">
              <div class="font-medium text-gray-900">流动性状况</div>
              <div class="text-sm text-gray-600 mt-1">{{ getLiquidityMessage(metrics.liquidityRatio) }}</div>
            </div>
          </div>

          <!-- 资产增长 -->
          <div class="flex items-start gap-3">
            <div class="flex-shrink-0 w-8 h-8 rounded-full flex items-center justify-center" :class="getGrowthIcon(metrics.yearlyChangeRate).bgClass">
              <span class="text-lg">{{ getGrowthIcon(metrics.yearlyChangeRate).icon }}</span>
            </div>
            <div class="flex-1">
              <div class="font-medium text-gray-900">资产增长</div>
              <div class="text-sm text-gray-600 mt-1">{{ getGrowthMessage(metrics.yearlyChangeRate) }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { analysisAPI } from '@/api/analysis'

const loading = ref(false)
const selectedDate = ref('')
const metrics = ref({
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
  previousYearNetWorth: 0
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
    const response = await analysisAPI.getFinancialMetrics(null, selectedDate.value || null)
    if (response.success) {
      metrics.value = response.data
    }
  } catch (error) {
    console.error('加载财务指标失败:', error)
  } finally {
    loading.value = false
  }
}

// 清除日期
const clearDate = () => {
  selectedDate.value = ''
  loadMetrics()
}

onMounted(() => {
  loadMetrics()
})
</script>
