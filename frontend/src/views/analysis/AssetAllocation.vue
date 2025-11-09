<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <h1 class="text-3xl font-bold">资产配置分析</h1>
    </div>

    <!-- 资产总览卡片 -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
      <div class="bg-white rounded-lg shadow p-6">
        <div class="text-sm text-gray-500 mb-1">总资产</div>
        <div class="text-3xl font-bold text-green-600">
          ¥{{ formatNumber(summary.totalAssets) }}
        </div>
      </div>
      <div class="bg-white rounded-lg shadow p-6">
        <div class="text-sm text-gray-500 mb-1">总负债</div>
        <div class="text-3xl font-bold text-red-600">
          ¥{{ formatNumber(summary.totalLiabilities) }}
        </div>
      </div>
      <div class="bg-white rounded-lg shadow p-6">
        <div class="text-sm text-gray-500 mb-1">净资产</div>
        <div class="text-3xl font-bold text-blue-600">
          ¥{{ formatNumber(summary.netWorth) }}
        </div>
      </div>
    </div>

    <!-- 按分类配置 -->
    <div class="bg-white rounded-lg shadow p-6">
      <h2 class="text-xl font-semibold mb-4">按分类配置</h2>
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div class="h-96 flex items-center justify-center">
          <div v-if="loadingCategory" class="text-gray-500">加载中...</div>
          <div v-else-if="categoryAllocation.data.length === 0" class="text-gray-500">暂无数据</div>
          <Pie v-else :data="categoryChartData" :options="pieChartOptions" />
        </div>
        <div class="space-y-2">
          <div
            v-for="item in categoryAllocation.data"
            :key="item.name"
            class="flex items-center justify-between p-3 border border-gray-200 rounded-lg"
          >
            <div class="flex items-center gap-3">
              <div class="w-4 h-4 rounded" :style="{ backgroundColor: getCategoryColor(item.name) }"></div>
              <span class="font-medium">{{ item.name }}</span>
            </div>
            <div class="text-right">
              <div class="font-semibold">¥{{ formatNumber(item.value) }}</div>
              <div class="text-sm text-gray-500">{{ formatNumber(item.percentage) }}%</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 按类型配置 -->
    <div class="bg-white rounded-lg shadow p-6">
      <h2 class="text-xl font-semibold mb-4">按类型配置</h2>
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div class="h-96 flex items-center justify-center">
          <div v-if="loadingType" class="text-gray-500">加载中...</div>
          <div v-else-if="typeAllocation.data.length === 0" class="text-gray-500">暂无数据</div>
          <Pie v-else :data="typeChartData" :options="pieChartOptions" />
        </div>
        <div class="space-y-2">
          <div
            v-for="item in typeAllocation.data"
            :key="item.name"
            class="flex items-center justify-between p-3 border border-gray-200 rounded-lg"
          >
            <div class="flex items-center gap-3">
              <div class="w-4 h-4 rounded" :style="{ backgroundColor: getTypeColor(item.name) }"></div>
              <span class="font-medium">{{ item.name }}</span>
            </div>
            <div class="text-right">
              <div class="font-semibold">¥{{ formatNumber(item.value) }}</div>
              <div class="text-sm text-gray-500">{{ formatNumber(item.percentage) }}%</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Pie } from 'vue-chartjs'
import {
  Chart as ChartJS,
  ArcElement,
  Tooltip,
  Legend
} from 'chart.js'
import { analysisAPI } from '@/api/analysis'

// 注册 Chart.js 组件
ChartJS.register(ArcElement, Tooltip, Legend)

const userId = ref(1) // TODO: 从用户登录状态获取
const loadingCategory = ref(false)
const loadingType = ref(false)
const summary = ref({
  totalAssets: 0,
  totalLiabilities: 0,
  netWorth: 0
})
const categoryAllocation = ref({
  total: 0,
  data: []
})
const typeAllocation = ref({
  total: 0,
  data: []
})

// 颜色配置
const categoryColors = [
  'rgb(34, 197, 94)',   // green
  'rgb(59, 130, 246)',  // blue
  'rgb(251, 146, 60)',  // orange
  'rgb(168, 85, 247)',  // purple
  'rgb(236, 72, 153)',  // pink
  'rgb(234, 179, 8)',   // yellow
  'rgb(20, 184, 166)',  // teal
  'rgb(239, 68, 68)',   // red
  'rgb(156, 163, 175)', // gray
  'rgb(99, 102, 241)',  // indigo
]

const typeColors = [
  'rgb(34, 197, 94)',
  'rgb(59, 130, 246)',
  'rgb(251, 146, 60)',
  'rgb(168, 85, 247)',
  'rgb(236, 72, 153)',
  'rgb(234, 179, 8)',
  'rgb(20, 184, 166)',
  'rgb(239, 68, 68)',
]

const getCategoryColor = (name) => {
  const index = categoryAllocation.value.data.findIndex(item => item.name === name)
  return categoryColors[index % categoryColors.length]
}

const getTypeColor = (name) => {
  const index = typeAllocation.value.data.findIndex(item => item.name === name)
  return typeColors[index % typeColors.length]
}

// 饼图配置
const pieChartOptions = {
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
          const value = context.parsed || 0
          const percentage = ((value / context.dataset.data.reduce((a, b) => a + b, 0)) * 100).toFixed(2)
          return `${label}: ¥${value.toLocaleString('zh-CN', { minimumFractionDigits: 2 })} (${percentage}%)`
        }
      }
    }
  }
}

// 按分类饼图数据
const categoryChartData = computed(() => ({
  labels: categoryAllocation.value.data.map(item => item.name),
  datasets: [
    {
      data: categoryAllocation.value.data.map(item => item.value),
      backgroundColor: categoryAllocation.value.data.map((item, index) =>
        categoryColors[index % categoryColors.length]
      ),
      borderWidth: 2,
      borderColor: '#fff'
    }
  ]
}))

// 按类型饼图数据
const typeChartData = computed(() => ({
  labels: typeAllocation.value.data.map(item => item.name),
  datasets: [
    {
      data: typeAllocation.value.data.map(item => item.value),
      backgroundColor: typeAllocation.value.data.map((item, index) =>
        typeColors[index % typeColors.length]
      ),
      borderWidth: 2,
      borderColor: '#fff'
    }
  ]
}))

// 格式化数字
const formatNumber = (num) => {
  if (!num) return '0.00'
  return parseFloat(num).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// 加载资产总览
const loadSummary = async () => {
  try {
    const response = await analysisAPI.getSummary(userId.value)
    if (response.data.success) {
      summary.value = response.data.data
    }
  } catch (error) {
    console.error('加载资产总览失败:', error)
  }
}

// 加载按分类配置
const loadCategoryAllocation = async () => {
  loadingCategory.value = true
  try {
    const response = await analysisAPI.getAllocationByCategory(userId.value)
    if (response.data.success) {
      categoryAllocation.value = response.data.data
    }
  } catch (error) {
    console.error('加载分类配置失败:', error)
  } finally {
    loadingCategory.value = false
  }
}

// 加载按类型配置
const loadTypeAllocation = async () => {
  loadingType.value = true
  try {
    const response = await analysisAPI.getAllocationByType(userId.value)
    if (response.data.success) {
      typeAllocation.value = response.data.data
    }
  } catch (error) {
    console.error('加载类型配置失败:', error)
  } finally {
    loadingType.value = false
  }
}

onMounted(() => {
  loadSummary()
  loadCategoryAllocation()
  loadTypeAllocation()
})
</script>
