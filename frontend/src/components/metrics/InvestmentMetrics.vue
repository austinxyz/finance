<template>
  <div class="investment-metrics bg-white rounded-lg shadow-md p-6">
    <h3 class="text-lg font-semibold mb-4 text-gray-800">投资收益概览</h3>

    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
      <!-- 累计投入 -->
      <div class="metric-card">
        <div class="metric-label">累计投入</div>
        <div class="metric-value text-blue-600">
          {{ formatCurrency(metrics.totalInvested, 'USD') }}
        </div>
      </div>

      <!-- 当前市值 -->
      <div class="metric-card">
        <div class="metric-label">当前市值</div>
        <div class="metric-value text-purple-600">
          {{ formatCurrency(metrics.currentInvestmentValue, 'USD') }}
        </div>
      </div>

      <!-- 总回报 -->
      <div class="metric-card">
        <div class="metric-label">总回报</div>
        <div class="metric-value" :class="metrics.totalInvestmentReturn >= 0 ? 'text-green-600' : 'text-red-600'">
          {{ formatCurrency(metrics.totalInvestmentReturn, 'USD', true) }}
        </div>
        <div class="metric-sublabel text-gray-500 text-sm mt-1">
          市值 - 投入
        </div>
      </div>

      <!-- 收益率 -->
      <div class="metric-card">
        <div class="metric-label">收益率</div>
        <div class="metric-value" :class="getReturnRateClass(metrics.investmentReturnRate)">
          {{ formatPercentage(metrics.investmentReturnRate) }}
        </div>
        <div class="metric-sublabel text-gray-500 text-sm mt-1">
          {{ getReturnRateLabel(metrics.investmentReturnRate) }}
        </div>
      </div>
    </div>

    <!-- 表现最佳的投资大类 -->
    <div v-if="metrics.topCategories && metrics.topCategories.length > 0" class="mt-6">
      <h4 class="text-sm font-medium text-gray-700 mb-3">表现最佳投资类别</h4>
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div
          v-for="(category, index) in metrics.topCategories"
          :key="index"
          class="top-category"
        >
          <div class="category-rank">
            <span class="rank-badge" :class="getRankBadgeClass(index)">
              {{ index + 1 }}
            </span>
            <span class="category-name">{{ category.categoryName }}</span>
          </div>
          <div class="category-value">{{ formatCurrency(category.value, 'USD') }}</div>
          <div class="category-return" :class="getReturnRateClass(category.returnRate)">
            <span class="return-label">收益率:</span>
            <span class="return-value">{{ formatPercentage(category.returnRate) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  metrics: {
    type: Object,
    required: true
  }
})

const formatCurrency = (value, currency = 'USD', showSign = false) => {
  if (value === null || value === undefined) return '-'
  const symbol = currency === 'USD' ? '$' : '¥'
  const sign = showSign && value > 0 ? '+' : ''
  return `${sign}${symbol}${Math.abs(value).toLocaleString('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 0 })}`
}

const formatPercentage = (value) => {
  if (value === null || value === undefined) return '-'
  return `${value >= 0 ? '+' : ''}${value.toFixed(1)}%`
}

const getReturnRateClass = (rate) => {
  if (rate >= 15) return 'text-green-600'
  if (rate >= 10) return 'text-blue-600'
  if (rate >= 5) return 'text-orange-600'
  return 'text-red-600'
}

const getReturnRateLabel = (rate) => {
  if (rate >= 15) return '⭐ 优秀'
  if (rate >= 10) return '✓ 良好'
  if (rate >= 5) return '! 一般'
  return '⚠ 偏低'
}

const getRankBadgeClass = (index) => {
  if (index === 0) return 'rank-first'
  if (index === 1) return 'rank-second'
  return 'rank-third'
}
</script>

<style scoped>
.investment-metrics {
  border: 1px solid #e5e7eb;
}

.metric-card {
  padding: 1rem;
  background-color: #f9fafb;
  border-radius: 0.5rem;
  border: 1px solid #e5e7eb;
}

.metric-label {
  font-size: 0.875rem;
  color: #6b7280;
  margin-bottom: 0.5rem;
}

.metric-value {
  font-size: 1.5rem;
  font-weight: 600;
}

.top-category {
  padding: 1rem;
  background-color: #f9fafb;
  border-radius: 0.5rem;
  border: 1px solid #e5e7eb;
}

.category-rank {
  display: flex;
  align-items: center;
  margin-bottom: 0.5rem;
}

.rank-badge {
  display: inline-block;
  width: 1.5rem;
  height: 1.5rem;
  line-height: 1.5rem;
  text-align: center;
  border-radius: 50%;
  font-size: 0.75rem;
  font-weight: 600;
  color: white;
  margin-right: 0.5rem;
}

.rank-first {
  background-color: #fbbf24;
}

.rank-second {
  background-color: #9ca3af;
}

.rank-third {
  background-color: #cd7f32;
}

.category-name {
  font-size: 0.875rem;
  font-weight: 500;
  color: #374151;
}

.category-value {
  font-size: 1.125rem;
  font-weight: 600;
  color: #111827;
  margin-bottom: 0.25rem;
}

.category-return {
  font-size: 0.875rem;
  font-weight: 500;
}

.return-label {
  margin-right: 0.25rem;
}
</style>
