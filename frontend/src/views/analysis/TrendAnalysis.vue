<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <h1 class="text-3xl font-bold">趋势分析</h1>
      <div class="flex gap-2">
        <input
          v-model="startDate"
          type="date"
          class="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
        />
        <span class="flex items-center">至</span>
        <input
          v-model="endDate"
          type="date"
          class="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
        />
        <button
          @click="loadTrend"
          class="px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-green-500"
        >
          查询
        </button>
      </div>
    </div>

    <!-- 总资产趋势图 -->
    <div class="bg-white rounded-lg shadow p-6">
      <h2 class="text-xl font-semibold mb-4">总资产趋势</h2>
      <div v-if="loading" class="flex justify-center items-center h-64">
        <div class="text-gray-500">加载中...</div>
      </div>
      <div v-else-if="trendData.length === 0" class="flex justify-center items-center h-64">
        <div class="text-gray-500">暂无数据</div>
      </div>
      <div v-else class="h-96">
        <Line :data="chartData" :options="chartOptions" />
      </div>
    </div>

    <!-- 账户列表 -->
    <div class="bg-white rounded-lg shadow p-6">
      <h2 class="text-xl font-semibold mb-4">单个账户趋势</h2>
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <div
          v-for="account in accounts"
          :key="account.id"
          class="border border-gray-200 rounded-lg p-4 cursor-pointer hover:border-green-500 transition-colors"
          @click="viewAccountTrend(account.id)"
        >
          <div class="flex justify-between items-start mb-2">
            <h3 class="font-semibold">{{ account.accountName }}</h3>
            <span class="text-xs text-gray-500">{{ account.categoryName }}</span>
          </div>
          <div class="text-2xl font-bold text-green-600">
            {{ getCurrencySymbol(account.currency) }}{{ formatNumber(account.currentAmount) }}
          </div>
        </div>
      </div>
    </div>

    <!-- 单个账户趋势弹窗 -->
    <div
      v-if="showAccountTrend"
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50"
      @click.self="showAccountTrend = false"
    >
      <div class="bg-white rounded-lg shadow-lg p-6 w-full max-w-4xl">
        <div class="flex justify-between items-center mb-4">
          <h2 class="text-xl font-semibold">{{ selectedAccountName }} - 趋势分析</h2>
          <button
            @click="showAccountTrend = false"
            class="text-gray-500 hover:text-gray-700"
          >
            <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
        <div v-if="loadingAccount" class="flex justify-center items-center h-64">
          <div class="text-gray-500">加载中...</div>
        </div>
        <div v-else-if="accountTrendData.length === 0" class="flex justify-center items-center h-64">
          <div class="text-gray-500">暂无数据</div>
        </div>
        <div v-else class="h-96">
          <Line :data="accountChartData" :options="chartOptions" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Line } from 'vue-chartjs'
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend
} from 'chart.js'
import { analysisAPI } from '@/api/analysis'
import { assetAccountAPI } from '@/api/asset'

// 注册 Chart.js 组件
ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend
)

const userId = ref(1) // TODO: 从用户登录状态获取
const loading = ref(false)
const loadingAccount = ref(false)
const trendData = ref([])
const accounts = ref([])
const accountTrendData = ref([])
const showAccountTrend = ref(false)
const selectedAccountName = ref('')

// 日期范围
const today = new Date()
const startDate = ref(new Date(today.getFullYear(), today.getMonth() - 11, 1).toISOString().split('T')[0])
const endDate = ref(today.toISOString().split('T')[0])

// 图表配置
const chartOptions = {
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
            label += '¥' + context.parsed.y.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
          }
          return label
        }
      }
    }
  },
  scales: {
    y: {
      beginAtZero: false,
      ticks: {
        callback: function(value) {
          return '¥' + value.toLocaleString('zh-CN')
        }
      }
    }
  },
  interaction: {
    mode: 'nearest',
    axis: 'x',
    intersect: false
  }
}

// 总资产趋势图表数据
const chartData = computed(() => ({
  labels: trendData.value.map(item => item.date),
  datasets: [
    {
      label: '总资产',
      data: trendData.value.map(item => item.amount),
      borderColor: 'rgb(34, 197, 94)',
      backgroundColor: 'rgba(34, 197, 94, 0.1)',
      tension: 0.3,
      fill: true
    }
  ]
}))

// 单个账户趋势图表数据
const accountChartData = computed(() => ({
  labels: accountTrendData.value.map(item => item.date),
  datasets: [
    {
      label: selectedAccountName.value,
      data: accountTrendData.value.map(item => item.amount),
      borderColor: 'rgb(59, 130, 246)',
      backgroundColor: 'rgba(59, 130, 246, 0.1)',
      tension: 0.3,
      fill: true
    }
  ]
}))

// 格式化数字
const formatNumber = (num) => {
  if (!num) return '0.00'
  return parseFloat(num).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// 获取货币符号
const getCurrencySymbol = (currency) => {
  const currencyMap = {
    'CNY': '¥',
    'USD': '$',
    'EUR': '€',
    'GBP': '£',
    'JPY': '¥',
    'HKD': 'HK$',
    'AUD': 'A$',
    'CAD': 'C$',
    'SGD': 'S$',
    'KRW': '₩'
  }
  return currencyMap[currency] || currency + ' '
}

// 加载总资产趋势
const loadTrend = async () => {
  loading.value = true
  try {
    const response = await analysisAPI.getTotalTrend(userId.value, startDate.value, endDate.value)
    if (response.data.success) {
      trendData.value = response.data.data
    }
  } catch (error) {
    console.error('加载趋势数据失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载账户列表
const loadAccounts = async () => {
  try {
    const response = await assetAccountAPI.getAll(userId.value)
    if (response.data.success) {
      accounts.value = response.data.data
    }
  } catch (error) {
    console.error('加载账户列表失败:', error)
  }
}

// 查看单个账户趋势
const viewAccountTrend = async (accountId) => {
  const account = accounts.value.find(a => a.id === accountId)
  if (account) {
    selectedAccountName.value = account.accountName
  }

  showAccountTrend.value = true
  loadingAccount.value = true

  try {
    const response = await analysisAPI.getAccountTrend(accountId)
    if (response.data.success) {
      accountTrendData.value = response.data.data
    }
  } catch (error) {
    console.error('加载账户趋势失败:', error)
  } finally {
    loadingAccount.value = false
  }
}

onMounted(() => {
  loadTrend()
  loadAccounts()
})
</script>
