<template>
  <div class="health-score-card bg-white rounded-lg shadow-md p-6">
    <h3 class="text-lg font-semibold mb-4 text-gray-800">财务健康评分</h3>

    <!-- Overall Score -->
    <div class="overall-score mb-6">
      <div class="flex items-center justify-between mb-2">
        <span class="text-4xl font-bold" :class="getScoreColor(healthScore.totalScore)">
          {{ healthScore.totalScore.toFixed(0) }}
        </span>
        <span class="grade-badge" :class="getGradeBadgeClass(healthScore.grade)">
          {{ healthScore.grade }}
        </span>
      </div>
      <div class="text-sm text-gray-500">总分 (满分100分)</div>
    </div>

    <!-- Score Breakdown -->
    <div class="score-breakdown mb-6">
      <h4 class="text-sm font-medium text-gray-700 mb-3">评分明细</h4>

      <!-- Debt Management -->
      <div class="score-item mb-4">
        <div class="flex justify-between mb-1">
          <span class="text-sm text-gray-700">资产负债管理</span>
          <span class="text-sm font-medium">{{ healthScore.scores.debtManagement.toFixed(0) }} / 25</span>
        </div>
        <div class="progress-bar">
          <div
            class="progress-fill"
            :class="getProgressColor(healthScore.scores.debtManagement, 25)"
            :style="{ width: `${(healthScore.scores.debtManagement / 25) * 100}%` }"
          ></div>
        </div>
      </div>

      <!-- Liquidity -->
      <div class="score-item mb-4">
        <div class="flex justify-between mb-1">
          <span class="text-sm text-gray-700">流动性管理</span>
          <span class="text-sm font-medium">{{ healthScore.scores.liquidity.toFixed(0) }} / 20</span>
        </div>
        <div class="progress-bar">
          <div
            class="progress-fill"
            :class="getProgressColor(healthScore.scores.liquidity, 20)"
            :style="{ width: `${(healthScore.scores.liquidity / 20) * 100}%` }"
          ></div>
        </div>
      </div>

      <!-- Savings -->
      <div class="score-item mb-4">
        <div class="flex justify-between mb-1">
          <span class="text-sm text-gray-700">储蓄能力</span>
          <span class="text-sm font-medium">{{ healthScore.scores.savings.toFixed(0) }} / 25</span>
        </div>
        <div class="progress-bar">
          <div
            class="progress-fill"
            :class="getProgressColor(healthScore.scores.savings, 25)"
            :style="{ width: `${(healthScore.scores.savings / 25) * 100}%` }"
          ></div>
        </div>
      </div>

      <!-- Investment -->
      <div class="score-item mb-4">
        <div class="flex justify-between mb-1">
          <span class="text-sm text-gray-700">投资收益</span>
          <span class="text-sm font-medium">{{ healthScore.scores.investment.toFixed(0) }} / 20</span>
        </div>
        <div class="progress-bar">
          <div
            class="progress-fill"
            :class="getProgressColor(healthScore.scores.investment, 20)"
            :style="{ width: `${(healthScore.scores.investment / 20) * 100}%` }"
          ></div>
        </div>
      </div>

      <!-- Growth -->
      <div class="score-item mb-4">
        <div class="flex justify-between mb-1">
          <span class="text-sm text-gray-700">资产增长</span>
          <span class="text-sm font-medium">{{ healthScore.scores.growth.toFixed(0) }} / 10</span>
        </div>
        <div class="progress-bar">
          <div
            class="progress-fill"
            :class="getProgressColor(healthScore.scores.growth, 10)"
            :style="{ width: `${(healthScore.scores.growth / 10) * 100}%` }"
          ></div>
        </div>
      </div>
    </div>

    <!-- Recommendations -->
    <div v-if="healthScore.recommendations && healthScore.recommendations.length > 0" class="recommendations">
      <h4 class="text-sm font-medium text-gray-700 mb-3">改进建议</h4>
      <ul class="space-y-2">
        <li
          v-for="(recommendation, index) in healthScore.recommendations"
          :key="index"
          class="flex items-start text-sm text-gray-600"
        >
          <span class="recommendation-bullet">•</span>
          <span>{{ recommendation }}</span>
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  healthScore: {
    type: Object,
    required: true,
    validator: (value) => {
      return value.totalScore !== undefined &&
             value.grade !== undefined &&
             value.scores !== undefined
    }
  }
})

// Get score color based on value
const getScoreColor = (score) => {
  if (score >= 90) return 'text-green-600'
  if (score >= 80) return 'text-blue-600'
  if (score >= 70) return 'text-yellow-600'
  if (score >= 60) return 'text-orange-600'
  return 'text-red-600'
}

// Get grade badge color
const getGradeBadgeClass = (grade) => {
  const gradeMap = {
    'A+': 'badge-a-plus',
    'A': 'badge-a',
    'B': 'badge-b',
    'C': 'badge-c',
    'D': 'badge-d'
  }
  return gradeMap[grade] || 'badge-d'
}

// Get progress bar color based on percentage
const getProgressColor = (value, max) => {
  const percentage = (value / max) * 100
  if (percentage >= 90) return 'bg-green-600'
  if (percentage >= 75) return 'bg-blue-600'
  if (percentage >= 60) return 'bg-yellow-600'
  if (percentage >= 40) return 'bg-orange-600'
  return 'bg-red-600'
}
</script>

<style scoped>
.health-score-card {
  border: 1px solid #e5e7eb;
}

.overall-score {
  text-align: center;
  padding: 1.5rem;
  background-color: #f9fafb;
  border-radius: 0.5rem;
  border: 1px solid #e5e7eb;
}

.grade-badge {
  padding: 0.5rem 1rem;
  border-radius: 0.5rem;
  font-size: 1.25rem;
  font-weight: 700;
  color: white;
}

.badge-a-plus {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
}

.badge-a {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
}

.badge-b {
  background: linear-gradient(135deg, #eab308 0%, #ca8a04 100%);
}

.badge-c {
  background: linear-gradient(135deg, #f97316 0%, #ea580c 100%);
}

.badge-d {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
}

.progress-bar {
  width: 100%;
  height: 0.5rem;
  background-color: #e5e7eb;
  border-radius: 0.25rem;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 0.25rem;
  transition: width 0.3s ease;
}

.recommendation-bullet {
  color: #3b82f6;
  font-size: 1.25rem;
  line-height: 1;
  margin-right: 0.5rem;
  flex-shrink: 0;
}

.recommendations ul {
  list-style: none;
  padding: 0;
}
</style>
