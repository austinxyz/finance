<template>
  <div class="space-y-6">
    <!-- 总览卡片作为导航 -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
      <!-- 净资产卡片 -->
      <button
        @click="switchTab('net')"
        :class="[
          'bg-white rounded-lg shadow p-4 text-left transition-all transform hover:scale-105',
          activeTab === 'net' ? 'ring-2 ring-blue-500 shadow-lg' : 'hover:shadow-md'
        ]"
      >
        <div class="text-xs text-gray-500 mb-1">净资产 (USD)</div>
        <div class="text-2xl font-bold text-blue-600">
          ${{ formatNumber(summary.netWorth) }}
        </div>
        <div v-if="activeTab === 'net'" class="text-xs text-blue-600 mt-1">已选中</div>
      </button>

      <!-- 总资产卡片 -->
      <button
        @click="switchTab('asset')"
        :class="[
          'bg-white rounded-lg shadow p-4 text-left transition-all transform hover:scale-105',
          activeTab === 'asset' ? 'ring-2 ring-green-500 shadow-lg' : 'hover:shadow-md'
        ]"
      >
        <div class="text-xs text-gray-500 mb-1">总资产 (USD)</div>
        <div class="text-2xl font-bold text-green-600">
          ${{ formatNumber(summary.totalAssets) }}
        </div>
        <div v-if="activeTab === 'asset'" class="text-xs text-green-600 mt-1">已选中</div>
      </button>

      <!-- 总负债卡片 -->
      <button
        @click="switchTab('liability')"
        :class="[
          'bg-white rounded-lg shadow p-4 text-left transition-all transform hover:scale-105',
          activeTab === 'liability' ? 'ring-2 ring-red-500 shadow-lg' : 'hover:shadow-md'
        ]"
      >
        <div class="text-xs text-gray-500 mb-1">总负债 (USD)</div>
        <div class="text-2xl font-bold text-red-600">
          ${{ formatNumber(summary.totalLiabilities) }}
        </div>
        <div v-if="activeTab === 'liability'" class="text-xs text-red-600 mt-1">已选中</div>
      </button>
    </div>

    <!-- 内容区域 -->
    <div class="bg-white rounded-lg shadow p-6">
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <!-- 主饼图 -->
        <div class="h-80 flex items-center justify-center">
          <div v-if="loading" class="text-gray-500">加载中...</div>
          <div v-else-if="!currentAllocation || currentAllocation.data.length === 0" class="text-gray-500">暂无数据</div>
          <Pie v-else :data="currentChartData" :options="mainPieChartOptions" />
        </div>

        <!-- 详细列表或钻取饼图 -->
        <div>
          <!-- 未选中类别时显示列表 -->
          <div v-if="!selectedCategory" class="space-y-2">
            <div
              v-for="item in currentAllocation?.data || []"
              :key="item.name || item.code"
              class="flex items-center justify-between p-3 border border-gray-200 rounded-lg cursor-pointer hover:bg-gray-50 transition-colors"
              @click="selectCategory(item)"
            >
              <div class="flex items-center gap-3">
                <div class="w-4 h-4 rounded" :style="{ backgroundColor: item.color || getItemColor(item.name) }"></div>
                <span class="font-medium">{{ item.name }}</span>
              </div>
              <div class="text-right">
                <div class="font-semibold">${{ formatNumber(getItemValue(item)) }}</div>
                <div class="text-sm text-gray-500">{{ formatNumber(item.percentage) }}%</div>
              </div>
            </div>
          </div>

          <!-- 选中类别时显示钻取饼图 -->
          <div v-else class="space-y-4">
            <div class="flex items-center justify-between">
              <h4 class="font-semibold">{{ selectedCategory.name }} - 账户分布</h4>
              <button
                @click="selectedCategory = null; drillDownData = null"
                class="text-sm text-gray-500 hover:text-gray-700"
              >
                返回
              </button>
            </div>
            <div class="space-y-3">
              <div class="h-64 flex items-center justify-center">
                <div v-if="loadingDrillDown" class="text-gray-500">加载中...</div>
                <div v-else-if="!drillDownChartData || drillDownData.length === 0" class="text-gray-500">暂无账户数据</div>
                <Pie v-else :data="drillDownChartData" :options="drillDownPieChartOptions" />
              </div>
              <!-- 在饼图下方显示类型总计 -->
              <div v-if="drillDownData && drillDownData.length > 0" class="text-center p-3 bg-gray-50 rounded-lg">
                <div class="text-xs text-gray-500">{{ selectedCategory?.name || '该类型' }}总计</div>
                <div class="text-xl font-bold text-gray-900">
                  ${{ formatNumber(drillDownData.reduce((sum, item) => sum + item.balance, 0)) }}
                </div>
              </div>
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
import { assetAccountAPI } from '@/api/asset'
import { liabilityAccountAPI } from '@/api/liability'

// 注册 Chart.js 组件
ChartJS.register(ArcElement, Tooltip, Legend)

// Tab 定义
const tabs = [
  { key: 'net', label: '净资产' },
  { key: 'asset', label: '总资产' },
  { key: 'liability', label: '总负债' }
]

const activeTab = ref('net')
const loading = ref(false)
const loadingDrillDown = ref(false)
const selectedCategory = ref(null)
const drillDownData = ref(null)

const summary = ref({
  totalAssets: 0,
  totalLiabilities: 0,
  netWorth: 0
})

const netAllocation = ref({
  total: 0,
  data: []
})

const assetAllocation = ref({
  total: 0,
  data: []
})

const liabilityAllocation = ref({
  total: 0,
  data: []
})

// 颜色配置
const defaultColors = [
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

const getItemColor = (name) => {
  const data = currentAllocation.value?.data || []
  const index = data.findIndex(item => item.name === name)
  return defaultColors[index % defaultColors.length]
}

const getItemValue = (item) => {
  if (activeTab.value === 'net') {
    return item.netValue || 0
  }
  return item.value || 0
}

// 当前激活的配置数据
const currentAllocation = computed(() => {
  switch (activeTab.value) {
    case 'net':
      return netAllocation.value
    case 'asset':
      return assetAllocation.value
    case 'liability':
      return liabilityAllocation.value
    default:
      return { total: 0, data: [] }
  }
})

// 主饼图数据
const currentChartData = computed(() => {
  const data = currentAllocation.value?.data || []
  return {
    labels: data.map(item => item.name),
    datasets: [
      {
        data: data.map(item => getItemValue(item)),
        backgroundColor: data.map(item => item.color || getItemColor(item.name)),
        borderWidth: 2,
        borderColor: '#fff'
      }
    ]
  }
})

// 钻取饼图数据
const drillDownChartData = computed(() => {
  if (!drillDownData.value || drillDownData.value.length === 0) {
    return null
  }

  return {
    labels: drillDownData.value.map(item => item.accountName),
    datasets: [
      {
        data: drillDownData.value.map(item => item.balance),
        backgroundColor: drillDownData.value.map((_, index) =>
          defaultColors[index % defaultColors.length]
        ),
        borderWidth: 2,
        borderColor: '#fff'
      }
    ]
  }
})

// 主饼图配置 - 带点击事件
const mainPieChartOptions = {
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
          const total = context.dataset.data.reduce((a, b) => a + b, 0)
          const percentage = ((value / total) * 100).toFixed(2)
          return `${label}: $${value.toLocaleString('en-US', { minimumFractionDigits: 2 })} (${percentage}%)`
        }
      }
    }
  },
  onClick: (event, elements) => {
    if (elements.length > 0) {
      const index = elements[0].index
      const item = currentAllocation.value.data[index]
      selectCategory(item)
    }
  }
}

// 钻取饼图配置
const drillDownPieChartOptions = computed(() => {
  return {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'bottom',
        labels: {
          boxWidth: 12,
          padding: 10,
          font: {
            size: 11
          }
        }
      },
      tooltip: {
        callbacks: {
          label: function(context) {
            const label = context.label || ''
            const value = context.parsed || 0
            const total = context.dataset.data.reduce((a, b) => a + b, 0)
            const percentage = ((value / total) * 100).toFixed(2)
            return `${label}: $${value.toLocaleString('en-US', { minimumFractionDigits: 2 })} (${percentage}%)`
          },
          footer: function(tooltipItems) {
            const total = tooltipItems[0].dataset.data.reduce((a, b) => a + b, 0)
            return `${selectedCategory.value?.name || '该类型'}总计: $${total.toLocaleString('en-US', { minimumFractionDigits: 2 })}`
          }
        }
      }
    }
  }
})

// 格式化数字
const formatNumber = (num) => {
  if (!num) return '0.00'
  return parseFloat(num).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// 加载资产总览
const loadSummary = async () => {
  try {
    const response = await analysisAPI.getSummary()
    if (response.success) {
      summary.value = response.data
    }
  } catch (error) {
    console.error('加载资产总览失败:', error)
  }
}

// 加载净资产配置
const loadNetAllocation = async () => {
  loading.value = true
  try {
    const response = await analysisAPI.getNetAssetAllocation()
    if (response.success) {
      netAllocation.value = response.data
    }
  } catch (error) {
    console.error('加载净资产配置失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载总资产配置
const loadAssetAllocation = async () => {
  loading.value = true
  try {
    const response = await analysisAPI.getAllocationByType()
    if (response.success) {
      assetAllocation.value = response.data
    }
  } catch (error) {
    console.error('加载资产配置失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载总负债配置
const loadLiabilityAllocation = async () => {
  loading.value = true
  try {
    const response = await analysisAPI.getLiabilityAllocation()
    if (response.success) {
      liabilityAllocation.value = response.data
    }
  } catch (error) {
    console.error('加载负债配置失败:', error)
  } finally {
    loading.value = false
  }
}

// 切换 Tab
const switchTab = (tabKey) => {
  activeTab.value = tabKey
  selectedCategory.value = null
  drillDownData.value = null

  // 根据 tab 加载对应数据
  if (tabKey === 'net' && netAllocation.value.data.length === 0) {
    loadNetAllocation()
  } else if (tabKey === 'asset' && assetAllocation.value.data.length === 0) {
    loadAssetAllocation()
  } else if (tabKey === 'liability' && liabilityAllocation.value.data.length === 0) {
    loadLiabilityAllocation()
  }
}

// 选择类别，加载账户分布
const selectCategory = async (item) => {
  selectedCategory.value = item
  loadingDrillDown.value = true

  try {
    if (activeTab.value === 'net') {
      // 对于净资产，需要获取该净资产类别下的所有资产账户和负债账户
      // TODO: 需要创建后端API来获取净资产类别下的账户列表
      drillDownData.value = []
    } else if (activeTab.value === 'asset') {
      // 获取该资产类型下的所有账户
      const response = await assetAccountAPI.getActiveAccounts()
      if (response.success) {
        // 根据类型过滤账户
        const typeMap = {
          'CASH': '现金类',
          'STOCKS': '股票投资',
          'RETIREMENT_FUND': '退休基金',
          'INSURANCE': '保险',
          'REAL_ESTATE': '房地产',
          'CRYPTOCURRENCY': '数字货币',
          'PRECIOUS_METALS': '贵金属',
          'OTHER': '其他'
        }

        const accounts = response.data.filter(account => {
          return typeMap[account.categoryType] === item.name
        })

        drillDownData.value = accounts.map(account => ({
          accountName: account.accountName,
          balance: account.latestAmountInBaseCurrency || 0
        }))
      }
    } else if (activeTab.value === 'liability') {
      // 获取该负债类型下的所有账户
      const response = await liabilityAccountAPI.getActiveAccounts()
      if (response.success) {
        // 根据类型过滤账户
        const typeMap = {
          'MORTGAGE': '房贷',
          'AUTO_LOAN': '车贷',
          'CREDIT_CARD': '信用卡',
          'PERSONAL_LOAN': '个人借债',
          'STUDENT_LOAN': '学生贷款',
          'BUSINESS_LOAN': '商业贷款',
          'OTHER': '其他'
        }

        const accounts = response.data.filter(account => {
          return typeMap[account.categoryType] === item.name
        })

        drillDownData.value = accounts.map(account => ({
          accountName: account.accountName,
          balance: account.latestBalanceInBaseCurrency || 0
        }))
      }
    }
  } catch (error) {
    console.error('加载账户分布失败:', error)
    drillDownData.value = []
  } finally {
    loadingDrillDown.value = false
  }
}

onMounted(() => {
  loadSummary()
  loadNetAllocation()  // 默认加载净资产配置
})
</script>
