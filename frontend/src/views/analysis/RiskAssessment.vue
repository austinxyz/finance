<template>
  <div class="space-y-4 md:space-y-6">
    <!-- 页头和日期选择 -->
    <div class="bg-white rounded-lg shadow p-3 md:p-4">
      <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-3 md:gap-0 mb-4">
        <h2 class="text-md md:text-lg font-semibold text-gray-900">风险评估</h2>
        <div class="flex items-center gap-2">
          <label class="text-xs md:text-sm font-medium text-gray-700">查询日期：</label>
          <input
            v-model="selectedDate"
            type="date"
            @change="loadRiskAssessment"
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
      <div class="text-xs md:text-sm text-gray-600">
        <span v-if="!selectedDate && assessment.asOfDate" class="flex items-center gap-2">
          <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4 text-blue-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
            <line x1="16" y1="2" x2="16" y2="6"></line>
            <line x1="8" y1="2" x2="8" y2="6"></line>
            <line x1="3" y1="10" x2="21" y2="10"></line>
          </svg>
          <span>
            <span class="font-medium text-gray-700">数据日期:</span>
            <span class="text-blue-600 font-semibold">{{ formatDate(assessment.asOfDate) }}</span>
            <span class="text-gray-500 ml-2">(显示最新可用数据)</span>
          </span>
        </span>
        <span v-else-if="selectedDate">
          <span class="font-medium text-gray-700">查询日期:</span> {{ selectedDate }}
        </span>
      </div>
    </div>

    <!-- 加载中状态 -->
    <div v-if="loading" class="flex items-center justify-center py-12">
      <div class="text-gray-500">加载中...</div>
    </div>

    <!-- 风险评估内容 -->
    <div v-else class="space-y-4 md:space-y-6">
      <!-- 综合风险评分卡片 -->
      <div class="bg-white rounded-lg shadow p-3 md:p-6">
        <h3 class="text-md md:text-lg font-semibold text-gray-900 mb-4">综合风险评估</h3>
        <div class="flex items-center gap-4 md:gap-6">
          <!-- 风险评分圆形进度 -->
          <div class="relative w-32 h-32 flex-shrink-0">
            <svg class="w-32 h-32 transform -rotate-90">
              <!-- 背景圆 -->
              <circle
                cx="64"
                cy="64"
                r="56"
                stroke="#e5e7eb"
                stroke-width="12"
                fill="none"
              />
              <!-- 进度圆 -->
              <circle
                cx="64"
                cy="64"
                r="56"
                :stroke="getRiskLevelColor(assessment.overallRiskLevel)"
                stroke-width="12"
                fill="none"
                :stroke-dasharray="`${(assessment.overallRiskScore / 100) * 351.858} 351.858`"
                stroke-linecap="round"
              />
            </svg>
            <div class="absolute inset-0 flex flex-col items-center justify-center">
              <div class="text-3xl font-bold" :class="getRiskLevelTextColor(assessment.overallRiskLevel)">
                {{ formatNumber(assessment.overallRiskScore) }}
              </div>
              <div class="text-xs text-gray-500">风险分</div>
            </div>
          </div>

          <!-- 风险等级和说明 -->
          <div class="flex-1">
            <div class="flex items-center gap-3 mb-3">
              <span class="text-2xl">{{ getRiskLevelIcon(assessment.overallRiskLevel) }}</span>
              <div>
                <div class="text-lg md:text-xl font-bold" :class="getRiskLevelTextColor(assessment.overallRiskLevel)">
                  {{ getRiskLevelName(assessment.overallRiskLevel) }}
                </div>
                <div class="text-xs md:text-sm text-gray-600">{{ getRiskLevelDescription(assessment.overallRiskLevel) }}</div>
              </div>
            </div>

            <!-- 各维度风险概览 -->
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-3 mt-4">
              <div class="flex items-center gap-2 text-sm">
                <span :class="getRiskLevelBadgeClass(assessment.concentrationRisk?.level)">
                  集中度风险
                </span>
                <span class="text-gray-600">{{ formatNumber(assessment.concentrationRisk?.score || 0) }}</span>
              </div>
              <div class="flex items-center gap-2 text-sm">
                <span :class="getRiskLevelBadgeClass(assessment.debtPressure?.level)">
                  负债压力
                </span>
                <span class="text-gray-600">{{ formatNumber(assessment.debtPressure?.score || 0) }}</span>
              </div>
              <div class="flex items-center gap-2 text-sm">
                <span :class="getRiskLevelBadgeClass(assessment.liquidityRisk?.level)">
                  流动性风险
                </span>
                <span class="text-gray-600">{{ formatNumber(assessment.liquidityRisk?.score || 0) }}</span>
              </div>
              <div class="flex items-center gap-2 text-sm">
                <span :class="getRiskLevelBadgeClass(assessment.marketRisk?.level)">
                  市场风险
                </span>
                <span class="text-gray-600">{{ formatNumber(assessment.marketRisk?.score || 0) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 资产集中度风险 -->
      <div v-if="assessment.concentrationRisk" class="bg-white rounded-lg shadow p-3 md:p-6">
        <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-3 md:gap-0 mb-4">
          <h3 class="text-md md:text-lg font-semibold text-gray-900">资产集中度风险</h3>
          <span :class="getRiskLevelBadgeClass(assessment.concentrationRisk.level)">
            {{ getRiskLevelName(assessment.concentrationRisk.level) }}
          </span>
        </div>

        <div class="space-y-4">
          <!-- 风险描述 -->
          <div class="bg-gray-50 rounded-lg p-4">
            <p class="text-xs md:text-sm text-gray-700">{{ assessment.concentrationRisk.description }}</p>
          </div>

          <!-- 关键指标 -->
          <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div class="border border-gray-200 rounded-lg p-4">
              <div class="text-xs text-gray-500 mb-1">最高集中类别</div>
              <div class="text-md md:text-lg font-semibold text-gray-900">
                {{ assessment.concentrationRisk.topConcentratedCategory }}
              </div>
            </div>
            <div class="border border-gray-200 rounded-lg p-4">
              <div class="text-xs text-gray-500 mb-1">集中度占比</div>
              <div class="text-md md:text-lg font-semibold" :class="getConcentrationColor(assessment.concentrationRisk.topConcentrationPercentage)">
                {{ formatNumber(assessment.concentrationRisk.topConcentrationPercentage) }}%
              </div>
            </div>
            <div class="border border-gray-200 rounded-lg p-4">
              <div class="text-xs text-gray-500 mb-1">赫芬达尔指数</div>
              <div class="text-md md:text-lg font-semibold" :class="getHerfindahlColor(assessment.concentrationRisk.herfindahlIndex)">
                {{ formatNumber(assessment.concentrationRisk.herfindahlIndex, 3) }}
              </div>
            </div>
          </div>

          <!-- 建议 -->
          <div v-if="assessment.concentrationRisk.suggestions?.length">
            <div class="text-xs md:text-sm font-medium text-gray-700 mb-2">改善建议</div>
            <ul class="space-y-2">
              <li v-for="(suggestion, index) in assessment.concentrationRisk.suggestions" :key="index"
                  class="flex items-start gap-2 text-sm text-gray-600">
                <span class="text-primary mt-0.5">•</span>
                <span>{{ suggestion }}</span>
              </li>
            </ul>
          </div>
        </div>
      </div>

      <!-- 负债压力评估 -->
      <div v-if="assessment.debtPressure" class="bg-white rounded-lg shadow p-3 md:p-6">
        <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-3 md:gap-0 mb-4">
          <h3 class="text-md md:text-lg font-semibold text-gray-900">负债压力评估</h3>
          <span :class="getRiskLevelBadgeClass(assessment.debtPressure.level)">
            {{ getRiskLevelName(assessment.debtPressure.level) }}
          </span>
        </div>

        <div class="space-y-4">
          <!-- 风险描述 -->
          <div class="bg-gray-50 rounded-lg p-4">
            <p class="text-xs md:text-sm text-gray-700">{{ assessment.debtPressure.description }}</p>
          </div>

          <!-- 关键指标 -->
          <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div class="border border-gray-200 rounded-lg p-4">
              <div class="text-xs text-gray-500 mb-1">资产负债率</div>
              <div class="text-md md:text-lg font-semibold" :class="getDebtRatioColor(assessment.debtPressure.debtToAssetRatio)">
                {{ formatNumber(assessment.debtPressure.debtToAssetRatio) }}%
              </div>
            </div>
            <div class="border border-gray-200 rounded-lg p-4">
              <div class="text-xs text-gray-500 mb-1">总资产</div>
              <div class="text-md md:text-lg font-semibold text-green-600">
                ${{ formatNumber(assessment.debtPressure.totalAssets) }}
              </div>
            </div>
            <div class="border border-gray-200 rounded-lg p-4">
              <div class="text-xs text-gray-500 mb-1">总负债</div>
              <div class="text-md md:text-lg font-semibold text-red-600">
                ${{ formatNumber(assessment.debtPressure.totalLiabilities) }}
              </div>
            </div>
          </div>

          <!-- 建议 -->
          <div v-if="assessment.debtPressure.suggestions?.length">
            <div class="text-xs md:text-sm font-medium text-gray-700 mb-2">改善建议</div>
            <ul class="space-y-2">
              <li v-for="(suggestion, index) in assessment.debtPressure.suggestions" :key="index"
                  class="flex items-start gap-2 text-sm text-gray-600">
                <span class="text-primary mt-0.5">•</span>
                <span>{{ suggestion }}</span>
              </li>
            </ul>
          </div>
        </div>
      </div>

      <!-- 流动性风险 -->
      <div v-if="assessment.liquidityRisk" class="bg-white rounded-lg shadow p-3 md:p-6">
        <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-3 md:gap-0 mb-4">
          <h3 class="text-md md:text-lg font-semibold text-gray-900">流动性风险</h3>
          <span :class="getRiskLevelBadgeClass(assessment.liquidityRisk.level)">
            {{ getRiskLevelName(assessment.liquidityRisk.level) }}
          </span>
        </div>

        <div class="space-y-4">
          <!-- 风险描述 -->
          <div class="bg-gray-50 rounded-lg p-4">
            <p class="text-xs md:text-sm text-gray-700">{{ assessment.liquidityRisk.description }}</p>
          </div>

          <!-- 关键指标 -->
          <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div class="border border-gray-200 rounded-lg p-4">
              <div class="text-xs text-gray-500 mb-1">流动性比率</div>
              <div class="text-md md:text-lg font-semibold" :class="getLiquidityRatioColor(assessment.liquidityRisk.liquidityRatio)">
                {{ formatNumber(assessment.liquidityRisk.liquidityRatio) }}%
              </div>
            </div>
            <div class="border border-gray-200 rounded-lg p-4">
              <div class="text-xs text-gray-500 mb-1">现金储备</div>
              <div class="text-md md:text-lg font-semibold text-blue-600">
                ${{ formatNumber(assessment.liquidityRisk.cashAmount) }}
              </div>
            </div>
            <div class="border border-gray-200 rounded-lg p-4">
              <div class="text-xs text-gray-500 mb-1">建议储备金</div>
              <div class="text-md md:text-lg font-semibold text-gray-600">
                ${{ formatNumber(assessment.liquidityRisk.recommendedEmergencyFund) }}
              </div>
            </div>
          </div>

          <!-- 建议 -->
          <div v-if="assessment.liquidityRisk.suggestions?.length">
            <div class="text-xs md:text-sm font-medium text-gray-700 mb-2">改善建议</div>
            <ul class="space-y-2">
              <li v-for="(suggestion, index) in assessment.liquidityRisk.suggestions" :key="index"
                  class="flex items-start gap-2 text-sm text-gray-600">
                <span class="text-primary mt-0.5">•</span>
                <span>{{ suggestion }}</span>
              </li>
            </ul>
          </div>
        </div>
      </div>

      <!-- 市场风险 -->
      <div v-if="assessment.marketRisk" class="bg-white rounded-lg shadow p-3 md:p-6">
        <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-3 md:gap-0 mb-4">
          <h3 class="text-md md:text-lg font-semibold text-gray-900">市场风险</h3>
          <span :class="getRiskLevelBadgeClass(assessment.marketRisk.level)">
            {{ getRiskLevelName(assessment.marketRisk.level) }}
          </span>
        </div>

        <div class="space-y-4">
          <!-- 风险描述 -->
          <div class="bg-gray-50 rounded-lg p-4">
            <p class="text-xs md:text-sm text-gray-700">{{ assessment.marketRisk.description }}</p>
          </div>

          <!-- 关键指标 -->
          <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div class="border border-gray-200 rounded-lg p-4">
              <div class="text-xs text-gray-500 mb-1">高风险资产占比</div>
              <div class="text-md md:text-lg font-semibold" :class="getMarketRiskColor(assessment.marketRisk.highRiskAssetsPercentage)">
                {{ formatNumber(assessment.marketRisk.highRiskAssetsPercentage) }}%
              </div>
            </div>
            <div class="border border-gray-200 rounded-lg p-4">
              <div class="text-xs text-gray-500 mb-1">股票投资占比</div>
              <div class="text-md md:text-lg font-semibold text-purple-600">
                {{ formatNumber(assessment.marketRisk.stockAllocationPercentage) }}%
              </div>
            </div>
            <div class="border border-gray-200 rounded-lg p-4">
              <div class="text-xs text-gray-500 mb-1">数字货币占比</div>
              <div class="text-md md:text-lg font-semibold text-orange-600">
                {{ formatNumber(assessment.marketRisk.cryptoAllocationPercentage) }}%
              </div>
            </div>
          </div>

          <!-- 建议 -->
          <div v-if="assessment.marketRisk.suggestions?.length">
            <div class="text-xs md:text-sm font-medium text-gray-700 mb-2">改善建议</div>
            <ul class="space-y-2">
              <li v-for="(suggestion, index) in assessment.marketRisk.suggestions" :key="index"
                  class="flex items-start gap-2 text-sm text-gray-600">
                <span class="text-primary mt-0.5">•</span>
                <span>{{ suggestion }}</span>
              </li>
            </ul>
          </div>
        </div>
      </div>

      <!-- 综合建议 -->
      <div v-if="assessment.recommendations?.length" class="bg-white rounded-lg shadow p-3 md:p-6">
        <h3 class="text-md md:text-lg font-semibold text-gray-900 mb-4">综合建议</h3>
        <div class="space-y-3">
          <div v-for="(recommendation, index) in assessment.recommendations" :key="index"
               class="flex items-start gap-3 p-4 rounded-lg"
               :class="getRecommendationBgClass(recommendation)">
            <span class="text-xl flex-shrink-0">{{ getRecommendationIcon(recommendation) }}</span>
            <p class="text-xs md:text-sm text-gray-700 flex-1">{{ recommendation }}</p>
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
const assessment = ref({
  asOfDate: '',
  overallRiskScore: 0,
  overallRiskLevel: 'LOW',
  concentrationRisk: null,
  debtPressure: null,
  liquidityRisk: null,
  marketRisk: null,
  recommendations: []
})

// 格式化数字
const formatNumber = (num, decimals = 2) => {
  if (!num && num !== 0) return '0.00'
  return parseFloat(num).toLocaleString('en-US', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals
  })
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '-'
  if (typeof dateString === 'string' && dateString.match(/^\d{4}-\d{2}-\d{2}$/)) {
    const [year, month, day] = dateString.split('-')
    return `${year}年${month}月${day}日`
  }
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
}

// 获取风险等级颜色
const getRiskLevelColor = (level) => {
  const colors = {
    'LOW': '#10b981',      // green
    'MEDIUM': '#f59e0b',   // yellow
    'HIGH': '#ef4444',     // red
    'CRITICAL': '#dc2626'  // dark red
  }
  return colors[level] || colors.LOW
}

// 获取风险等级文本颜色
const getRiskLevelTextColor = (level) => {
  const colors = {
    'LOW': 'text-green-600',
    'MEDIUM': 'text-yellow-600',
    'HIGH': 'text-red-600',
    'CRITICAL': 'text-red-700'
  }
  return colors[level] || colors.LOW
}

// 获取风险等级名称
const getRiskLevelName = (level) => {
  const names = {
    'LOW': '低风险',
    'MEDIUM': '中等风险',
    'HIGH': '高风险',
    'CRITICAL': '严重风险'
  }
  return names[level] || '低风险'
}

// 获取风险等级描述
const getRiskLevelDescription = (level) => {
  const descriptions = {
    'LOW': '财务状况良好，风险可控',
    'MEDIUM': '存在一定风险，需要关注',
    'HIGH': '风险较大，建议尽快调整',
    'CRITICAL': '风险严重，需要立即处理'
  }
  return descriptions[level] || '财务状况良好'
}

// 获取风险等级图标
const getRiskLevelIcon = (level) => {
  const icons = {
    'LOW': '✅',
    'MEDIUM': '⚠️',
    'HIGH': '❗',
    'CRITICAL': '🚨'
  }
  return icons[level] || icons.LOW
}

// 获取风险等级徽章样式
const getRiskLevelBadgeClass = (level) => {
  const classes = {
    'LOW': 'px-2 py-1 rounded-full text-xs font-medium bg-green-100 text-green-800',
    'MEDIUM': 'px-2 py-1 rounded-full text-xs font-medium bg-yellow-100 text-yellow-800',
    'HIGH': 'px-2 py-1 rounded-full text-xs font-medium bg-red-100 text-red-800',
    'CRITICAL': 'px-2 py-1 rounded-full text-xs font-medium bg-red-200 text-red-900'
  }
  return classes[level] || classes.LOW
}

// 获取集中度颜色
const getConcentrationColor = (percentage) => {
  if (percentage > 70) return 'text-red-600'
  if (percentage > 50) return 'text-yellow-600'
  return 'text-green-600'
}

// 获取赫芬达尔指数颜色
const getHerfindahlColor = (index) => {
  if (index > 0.5) return 'text-red-600'
  if (index > 0.25) return 'text-yellow-600'
  return 'text-green-600'
}

// 获取资产负债率颜色
const getDebtRatioColor = (ratio) => {
  if (ratio > 70) return 'text-red-600'
  if (ratio > 50) return 'text-yellow-600'
  if (ratio > 30) return 'text-blue-600'
  return 'text-green-600'
}

// 获取流动性比率颜色
const getLiquidityRatioColor = (ratio) => {
  if (ratio < 5) return 'text-red-600'
  if (ratio < 10) return 'text-yellow-600'
  if (ratio > 40) return 'text-yellow-600'
  return 'text-green-600'
}

// 获取市场风险颜色
const getMarketRiskColor = (percentage) => {
  if (percentage > 60) return 'text-red-600'
  if (percentage > 40) return 'text-yellow-600'
  return 'text-green-600'
}

// 获取建议图标
const getRecommendationIcon = (recommendation) => {
  if (recommendation.includes('【优先】') || recommendation.includes('【紧急】')) {
    return '🚨'
  }
  if (recommendation.includes('【重要】')) {
    return '⚠️'
  }
  return '💡'
}

// 获取建议背景样式
const getRecommendationBgClass = (recommendation) => {
  if (recommendation.includes('【优先】') || recommendation.includes('【紧急】')) {
    return 'bg-red-50 border-l-4 border-red-500'
  }
  if (recommendation.includes('【重要】')) {
    return 'bg-yellow-50 border-l-4 border-yellow-500'
  }
  return 'bg-blue-50 border-l-4 border-blue-500'
}

// 加载风险评估
const loadRiskAssessment = async () => {
  loading.value = true
  try {
    const response = await analysisAPI.getRiskAssessment(null, selectedDate.value || null)
    if (response.success) {
      assessment.value = response.data
    }
  } catch (error) {
    console.error('加载风险评估失败:', error)
  } finally {
    loading.value = false
  }
}

// 清除日期
const clearDate = () => {
  selectedDate.value = ''
  loadRiskAssessment()
}

onMounted(() => {
  loadRiskAssessment()
})
</script>
