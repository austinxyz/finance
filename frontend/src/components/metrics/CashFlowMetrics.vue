<template>
  <div class="cash-flow-metrics bg-white rounded-lg shadow-md p-6">
    <h3 class="text-lg font-semibold mb-4 text-gray-800">现金流分析</h3>

    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
      <!-- 年度总收入 -->
      <div class="metric-card">
        <div class="metric-label">年度总收入</div>
        <div class="metric-value text-green-600">
          {{ formatCurrency(metrics.annualTotalIncome, 'USD') }}
        </div>
        <div v-if="metrics.incomeGrowthRate" class="metric-change" :class="getChangeClass(metrics.incomeGrowthRate)">
          <span class="change-icon">{{ metrics.incomeGrowthRate >= 0 ? '↑' : '↓' }}</span>
          <span>{{ formatPercentage(Math.abs(metrics.incomeGrowthRate)) }}</span>
        </div>
      </div>

      <!-- 年度总支出 -->
      <div class="metric-card">
        <div class="metric-label">年度总支出</div>
        <div class="metric-value text-orange-600">
          {{ formatCurrency(metrics.annualTotalExpense, 'USD') }}
        </div>
        <div v-if="metrics.expenseGrowthRate" class="metric-change" :class="getChangeClass(-metrics.expenseGrowthRate)">
          <span class="change-icon">{{ metrics.expenseGrowthRate >= 0 ? '↑' : '↓' }}</span>
          <span>{{ formatPercentage(Math.abs(metrics.expenseGrowthRate)) }}</span>
        </div>
      </div>

      <!-- 净现金流 -->
      <div class="metric-card">
        <div class="metric-label">净现金流</div>
        <div class="metric-value" :class="metrics.netCashFlow >= 0 ? 'text-green-600' : 'text-red-600'">
          {{ formatCurrency(metrics.netCashFlow, 'USD') }}
        </div>
        <div class="metric-sublabel text-gray-500 text-sm mt-1">
          收入 - 支出
        </div>
      </div>

      <!-- 储蓄率 -->
      <div class="metric-card">
        <div class="metric-label">储蓄率</div>
        <div class="metric-value" :class="getSavingsRateClass(metrics.savingsRate)">
          {{ formatPercentage(metrics.savingsRate) }}
        </div>
        <div class="metric-sublabel text-gray-500 text-sm mt-1">
          {{ getSavingsRateLabel(metrics.savingsRate) }}
        </div>
      </div>
    </div>

    <!-- 收入构成 -->
    <div class="mt-6 grid grid-cols-1 md:grid-cols-3 gap-4">
      <div class="income-breakdown">
        <div class="breakdown-label">工资收入</div>
        <div class="breakdown-value">{{ formatCurrency(metrics.annualWorkIncome, 'USD') }}</div>
        <div class="breakdown-percentage text-gray-500 text-sm">
          {{ formatPercentage(getPercentage(metrics.annualWorkIncome, metrics.annualTotalIncome)) }}
        </div>
      </div>

      <div class="income-breakdown">
        <div class="breakdown-label">投资收入</div>
        <div class="breakdown-value">{{ formatCurrency(metrics.annualInvestmentIncome, 'USD') }}</div>
        <div class="breakdown-percentage text-gray-500 text-sm">
          {{ formatPercentage(getPercentage(metrics.annualInvestmentIncome, metrics.annualTotalIncome)) }}
        </div>
      </div>

      <div class="income-breakdown">
        <div class="breakdown-label">其他收入</div>
        <div class="breakdown-value">{{ formatCurrency(metrics.annualOtherIncome, 'USD') }}</div>
        <div class="breakdown-percentage text-gray-500 text-sm">
          {{ formatPercentage(getPercentage(metrics.annualOtherIncome, metrics.annualTotalIncome)) }}
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

const formatCurrency = (value, currency = 'USD') => {
  if (value === null || value === undefined) return '-'
  const symbol = currency === 'USD' ? '$' : '¥'
  return `${symbol}${Math.abs(value).toLocaleString('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 0 })}`
}

const formatPercentage = (value) => {
  if (value === null || value === undefined) return '-'
  return `${value.toFixed(1)}%`
}

const getPercentage = (part, total) => {
  if (!total || total === 0) return 0
  return (part / total) * 100
}

const getChangeClass = (value) => {
  if (value > 0) return 'text-green-600'
  if (value < 0) return 'text-red-600'
  return 'text-gray-500'
}

const getSavingsRateClass = (rate) => {
  if (rate >= 30) return 'text-green-600'
  if (rate >= 20) return 'text-blue-600'
  if (rate >= 10) return 'text-orange-600'
  return 'text-red-600'
}

const getSavingsRateLabel = (rate) => {
  if (rate >= 30) return '⭐ 优秀'
  if (rate >= 20) return '✓ 良好'
  if (rate >= 10) return '! 一般'
  return '⚠ 偏低'
}
</script>

<style scoped>
.cash-flow-metrics {
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

.metric-change {
  font-size: 0.875rem;
  margin-top: 0.25rem;
}

.change-icon {
  margin-right: 0.25rem;
}

.income-breakdown {
  padding: 1rem;
  background-color: #f9fafb;
  border-radius: 0.5rem;
  border: 1px solid #e5e7eb;
}

.breakdown-label {
  font-size: 0.875rem;
  color: #6b7280;
  margin-bottom: 0.5rem;
}

.breakdown-value {
  font-size: 1.25rem;
  font-weight: 600;
  color: #111827;
  margin-bottom: 0.25rem;
}

.breakdown-percentage {
  font-size: 0.875rem;
}
</style>
