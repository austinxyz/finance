<template>
  <div class="space-y-6">
    <!-- 筛选条件 -->
    <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
      <!-- 货币选择 -->
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">选择货币</label>
        <select
          v-model="selectedCurrency"
          class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
          @change="loadRateHistory"
        >
          <option value="CNY">人民币 (CNY)</option>
          <option value="EUR">欧元 (EUR)</option>
          <option value="GBP">英镑 (GBP)</option>
          <option value="JPY">日元 (JPY)</option>
          <option value="AUD">澳元 (AUD)</option>
          <option value="CAD">加元 (CAD)</option>
        </select>
      </div>

      <!-- 开始日期 -->
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">开始日期</label>
        <input
          v-model="startDate"
          type="date"
          class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
          @change="loadRateHistory"
        />
      </div>

      <!-- 结束日期 -->
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">结束日期</label>
        <input
          v-model="endDate"
          type="date"
          class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
          @change="loadRateHistory"
        />
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="bg-white border border-gray-200 rounded-lg p-4">
      <div v-if="loading" class="text-center py-12">
        <div class="animate-spin rounded-full h-10 w-10 border-b-2 border-green-600 mx-auto mb-4"></div>
        <p class="text-gray-500 text-sm">加载中...</p>
      </div>

      <div v-else-if="rateHistory.length === 0" class="text-center py-12">
        <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"/>
        </svg>
        <p class="text-gray-500 mt-4">该时间段内暂无汇率数据</p>
        <p class="text-sm text-gray-400 mt-2">请先在"日期汇率管理"Tab中添加多个日期的汇率数据</p>
      </div>

      <div v-else-if="rateHistory.length === 1" class="text-center py-12">
        <svg class="mx-auto h-12 w-12 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
        </svg>
        <p class="text-gray-700 mt-4 font-medium">数据点不足</p>
        <p class="text-sm text-gray-500 mt-2">需要至少2个日期的汇率数据才能显示趋势图</p>
        <p class="text-sm text-gray-400 mt-1">当前只有 {{ rateHistory[0].effectiveDate }} 的数据</p>
      </div>

      <div v-else>
        <canvas ref="chartCanvas"></canvas>
      </div>
    </div>

    <!-- 数据表格 -->
    <div v-if="!loading && rateHistory.length > 0" class="bg-white border border-gray-200 rounded-lg overflow-hidden">
      <div class="px-4 py-3 border-b border-gray-200 bg-gray-50">
        <h3 class="text-sm font-medium text-gray-700">历史数据明细</h3>
      </div>
      <div class="overflow-x-auto">
        <table class="min-w-full divide-y divide-gray-200">
          <thead class="bg-gray-50">
            <tr>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                日期
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                汇率 (对美元)
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                来源
              </th>
            </tr>
          </thead>
          <tbody class="bg-white divide-y divide-gray-200">
            <tr v-for="rate in rateHistory" :key="rate.id" class="hover:bg-gray-50">
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                {{ rate.effectiveDate }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                {{ formatRate(rate.rateToUsd) }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                {{ rate.source || '-' }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 统计信息 -->
    <div v-if="!loading && rateHistory.length > 0" class="grid grid-cols-2 sm:grid-cols-4 gap-4">
      <div class="bg-blue-50 border border-blue-200 rounded-lg p-4">
        <div class="text-xs text-blue-600 font-medium mb-1">最新汇率</div>
        <div class="text-lg font-bold text-blue-900">{{ formatRate(stats.latest) }}</div>
      </div>
      <div class="bg-green-50 border border-green-200 rounded-lg p-4">
        <div class="text-xs text-green-600 font-medium mb-1">最高汇率</div>
        <div class="text-lg font-bold text-green-900">{{ formatRate(stats.max) }}</div>
      </div>
      <div class="bg-red-50 border border-red-200 rounded-lg p-4">
        <div class="text-xs text-red-600 font-medium mb-1">最低汇率</div>
        <div class="text-lg font-bold text-red-900">{{ formatRate(stats.min) }}</div>
      </div>
      <div class="bg-purple-50 border border-purple-200 rounded-lg p-4">
        <div class="text-xs text-purple-600 font-medium mb-1">平均汇率</div>
        <div class="text-lg font-bold text-purple-900">{{ formatRate(stats.avg) }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { exchangeRateAPI } from '@/api/exchangeRate'
import { Chart, registerables } from 'chart.js'
import 'chartjs-adapter-date-fns'

// 注册 Chart.js 组件
Chart.register(...registerables)

const selectedCurrency = ref('CNY')
const startDate = ref('')
const endDate = ref('')
const rateHistory = ref([])
const loading = ref(false)
const chartCanvas = ref(null)
let chartInstance = null

// 计算统计数据
const stats = computed(() => {
  if (rateHistory.value.length === 0) {
    return { latest: 0, max: 0, min: 0, avg: 0 }
  }

  const rates = rateHistory.value.map(r => parseFloat(r.rateToUsd))
  return {
    latest: rates[rates.length - 1],
    max: Math.max(...rates),
    min: Math.min(...rates),
    avg: rates.reduce((a, b) => a + b, 0) / rates.length
  }
})

// 格式化汇率显示
const formatRate = (rate) => {
  if (!rate) return '0.00000000'
  return parseFloat(rate).toFixed(8)
}

// 初始化日期（默认最近7天）
const initializeDates = () => {
  const today = new Date()
  const sevenDaysAgo = new Date(today)
  sevenDaysAgo.setDate(today.getDate() - 7)

  endDate.value = today.toISOString().split('T')[0]
  startDate.value = sevenDaysAgo.toISOString().split('T')[0]
}

// 加载汇率历史
const loadRateHistory = async () => {
  if (!startDate.value || !endDate.value) {
    return
  }

  loading.value = true
  try {
    const response = await exchangeRateAPI.getRatesByRange(
      selectedCurrency.value,
      startDate.value,
      endDate.value
    )

    if (response.success) {
      rateHistory.value = response.data

      // 等待DOM更新后渲染图表（需要等待 v-else 条件渲染完成）
      if (rateHistory.value.length >= 2) {
        // 使用 setTimeout 确保 v-else 的 canvas 已经渲染
        setTimeout(() => {
          renderChart()
        }, 100)
      }
    }
  } catch (error) {
    console.error('加载汇率历史失败:', error)
    alert('加载汇率历史失败，请重试')
  } finally {
    loading.value = false
  }
}

// 渲染图表
const renderChart = () => {
  if (!chartCanvas.value || rateHistory.value.length < 2) {
    return
  }

  // 销毁旧图表
  if (chartInstance) {
    chartInstance.destroy()
  }

  const ctx = chartCanvas.value.getContext('2d')

  // 使用时间轴，数据点需要包含x和y
  const data = rateHistory.value.map(r => ({
    x: r.effectiveDate,  // 日期字符串，Chart.js会自动解析
    y: parseFloat(r.rateToUsd)
  }))

  chartInstance = new Chart(ctx, {
    type: 'line',
    data: {
      datasets: [{
        label: `${selectedCurrency.value} 对美元汇率`,
        data,
        borderColor: 'rgb(34, 197, 94)',
        backgroundColor: 'rgba(34, 197, 94, 0.1)',
        borderWidth: 2,
        fill: true,
        tension: 0.4,
        pointRadius: 4,
        pointHoverRadius: 6,
        pointBackgroundColor: 'rgb(34, 197, 94)',
        pointBorderColor: '#fff',
        pointBorderWidth: 2
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: true,
      aspectRatio: 2,
      plugins: {
        legend: {
          display: true,
          position: 'top'
        },
        tooltip: {
          mode: 'index',
          intersect: false,
          callbacks: {
            label: (context) => {
              return `汇率: ${formatRate(context.parsed.y)} USD`
            }
          }
        }
      },
      scales: {
        y: {
          beginAtZero: false,
          ticks: {
            callback: (value) => formatRate(value)
          },
          title: {
            display: true,
            text: '汇率 (USD)'
          }
        },
        x: {
          type: 'time',
          time: {
            unit: 'day',
            displayFormats: {
              day: 'MM-dd'
            },
            tooltipFormat: 'yyyy-MM-dd'
          },
          title: {
            display: true,
            text: '日期'
          }
        }
      },
      interaction: {
        mode: 'nearest',
        axis: 'x',
        intersect: false
      }
    }
  })
}

// 监听数据变化
watch(rateHistory, (newVal) => {
  if (newVal.length >= 2) {
    nextTick(() => renderChart())
  }
}, { deep: true })

onMounted(() => {
  initializeDates()
  loadRateHistory()
})
</script>
