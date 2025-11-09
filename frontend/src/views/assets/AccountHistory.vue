<template>
  <div class="p-6 space-y-6">
    <!-- 页面标题和账户选择 -->
    <div>
      <h1 class="text-2xl font-bold text-gray-900">资产历史记录</h1>
      <div class="mt-4 flex items-center gap-4 flex-wrap">
        <div class="flex items-center gap-2">
          <label class="text-sm font-medium text-gray-700">选择账户:</label>
          <select
            v-model="selectedAccountId"
            @change="loadRecords"
            class="px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
          >
            <option :value="null">请选择账户</option>
            <option v-for="account in accounts" :key="account.id" :value="account.id">
              {{ account.accountName }} - {{ account.categoryName }}
            </option>
          </select>
        </div>

        <!-- 账户信息 -->
        <div v-if="selectedAccount" class="flex items-center gap-4 px-4 py-2 bg-gray-50 rounded-lg border border-gray-200">
          <div class="text-sm">
            <span class="text-gray-600">所属用户:</span>
            <span class="font-medium text-gray-900 ml-1">{{ selectedAccount.userName }}</span>
          </div>
          <div class="text-sm">
            <span class="text-gray-600">币种:</span>
            <span class="font-medium text-gray-900 ml-1">{{ selectedAccount.currency }}</span>
          </div>
        </div>

        <button
          v-if="selectedAccountId"
          @click="openCreateDialog"
          class="ml-auto px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary/90 text-sm font-medium flex items-center gap-2"
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="12" y1="5" x2="12" y2="19"></line>
            <line x1="5" y1="12" x2="19" y2="12"></line>
          </svg>
          添加/插入记录
        </button>
      </div>
    </div>

    <!-- 两列布局：图表 + 历史记录 -->
    <div v-if="selectedAccountId" class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <!-- 左侧：趋势图表 -->
      <div class="bg-white rounded-lg shadow border border-gray-200 p-6">
        <div class="flex items-center justify-between mb-4">
          <h2 class="text-lg font-semibold text-gray-900">资产趋势</h2>
          <div class="flex gap-2">
            <button
              v-for="range in timeRanges"
              :key="range.value"
              @click="selectedTimeRange = range.value"
              :class="[
                'px-2 py-1 text-xs rounded-md font-medium transition-colors',
                selectedTimeRange === range.value
                  ? 'bg-primary text-white'
                  : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
              ]"
            >
              {{ range.label }}
            </button>
          </div>
        </div>
        <div v-if="records.length > 0" class="h-96">
          <canvas ref="chartCanvas"></canvas>
        </div>
        <div v-else class="h-96 flex items-center justify-center text-gray-500 text-sm">
          暂无数据，请添加记录
        </div>
      </div>

      <!-- 右侧：历史记录列表 -->
      <div class="bg-white rounded-lg shadow border border-gray-200">
      <div class="px-4 py-3 border-b border-gray-200">
        <h2 class="text-lg font-semibold text-gray-900">历史记录</h2>
      </div>

      <div v-if="loading" class="text-center py-8 text-gray-500">
        加载中...
      </div>

      <div v-else-if="filteredRecords.length === 0" class="text-center py-8 text-gray-500">
        暂无记录
      </div>

      <div v-else class="overflow-x-auto">
        <table class="min-w-full divide-y divide-gray-200">
          <thead class="bg-gray-50">
            <tr>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">日期</th>
              <th class="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">金额</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">备注</th>
              <th class="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">操作</th>
            </tr>
          </thead>
          <tbody class="bg-white divide-y divide-gray-200">
            <tr v-for="record in filteredRecords" :key="record.id" class="hover:bg-gray-50">
              <td class="px-4 py-3 text-sm text-gray-900">{{ formatDate(record.recordDate) }}</td>
              <td class="px-4 py-3 text-sm text-gray-900 text-right font-medium">
                {{ getCurrencySymbol(record.currency) }}{{ formatNumber(record.amount) }}
                <span v-if="record.quantity" class="text-xs text-gray-500 ml-2">
                  ({{ formatNumber(record.quantity) }} × {{ getCurrencySymbol(record.currency) }}{{ formatNumber(record.unitPrice) }})
                </span>
              </td>
              <td class="px-4 py-3 text-sm text-gray-600">{{ record.notes || '-' }}</td>
              <td class="px-4 py-3 text-sm text-right">
                <div class="flex justify-end gap-2">
                  <button
                    @click="editRecord(record)"
                    class="text-blue-600 hover:text-blue-800 text-sm"
                    title="编辑"
                  >
                    编辑
                  </button>
                  <button
                    @click="deleteRecord(record)"
                    class="text-red-600 hover:text-red-800 text-sm"
                    title="删除"
                  >
                    删除
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
    </div>

    <!-- 创建/编辑记录对话框 -->
    <div
      v-if="showDialog"
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50"
      @click.self="closeDialog"
    >
      <div class="bg-white rounded-lg p-6 w-full max-w-md">
        <h2 class="text-xl font-bold mb-4">
          {{ editingRecord ? '编辑记录' : '添加历史记录' }}
        </h2>
        <p v-if="!editingRecord" class="text-sm text-gray-600 mb-4">
          可以添加任意日期的资产记录，包括过去的历史数据
        </p>
        <form @submit.prevent="submitForm" class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              记录日期 *
              <span class="text-xs text-gray-500 font-normal">(只能选择今天或过去的日期)</span>
            </label>
            <input
              v-model="formData.recordDate"
              type="date"
              required
              :max="new Date().toISOString().split('T')[0]"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">金额 *</label>
            <input
              v-model="formData.amount"
              type="number"
              step="0.01"
              required
              placeholder="例如：10000.00"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">币种 *</label>
            <select
              v-model="formData.currency"
              required
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
            >
              <option value="CNY">人民币 (CNY)</option>
              <option value="USD">美元 (USD)</option>
              <option value="EUR">欧元 (EUR)</option>
              <option value="HKD">港币 (HKD)</option>
            </select>
          </div>

          <!-- 可选字段：数量和单价 -->
          <details class="border border-gray-200 rounded-lg">
            <summary class="px-3 py-2 cursor-pointer text-sm text-gray-600 hover:bg-gray-50">
              高级选项（数量/单价 - 可选）
            </summary>
            <div class="p-3 space-y-3 bg-gray-50">
              <div>
                <label class="block text-xs font-medium text-gray-600 mb-1">数量（可选）</label>
                <input
                  v-model="formData.quantity"
                  type="number"
                  step="0.01"
                  placeholder="例如：100 (股票/基金份额)"
                  class="w-full px-3 py-2 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
                />
              </div>

              <div>
                <label class="block text-xs font-medium text-gray-600 mb-1">单价（可选）</label>
                <input
                  v-model="formData.unitPrice"
                  type="number"
                  step="0.01"
                  placeholder="例如：100.00 (每股/每份价格)"
                  class="w-full px-3 py-2 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
                />
              </div>
            </div>
          </details>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">备注</label>
            <textarea
              v-model="formData.notes"
              rows="3"
              placeholder="添加备注信息"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
            ></textarea>
          </div>

          <div class="flex gap-3 pt-4">
            <button
              type="submit"
              class="flex-1 px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary/90 font-medium"
            >
              {{ editingRecord ? '保存' : '创建' }}
            </button>
            <button
              type="button"
              @click="closeDialog"
              class="flex-1 px-4 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 font-medium"
            >
              取消
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { assetAccountAPI, assetRecordAPI } from '@/api/asset'
import { Chart, registerables } from 'chart.js'

Chart.register(...registerables)

const selectedAccountId = ref(null)
const accounts = ref([])
const records = ref([])
const loading = ref(false)
const showDialog = ref(false)
const editingRecord = ref(null)
const chartCanvas = ref(null)
let chartInstance = null

const selectedTimeRange = ref('all')
const timeRanges = [
  { value: 'week', label: '本周' },
  { value: 'month', label: '本月' },
  { value: 'year', label: '本年' },
  { value: 'all', label: '全部' }
]

const formData = ref({
  recordDate: new Date().toISOString().split('T')[0],
  amount: '',
  quantity: '',
  unitPrice: '',
  currency: 'CNY',
  notes: ''
})

// 当前选中的账户
const selectedAccount = computed(() => {
  if (!selectedAccountId.value) return null
  return accounts.value.find(a => a.id === selectedAccountId.value)
})

// 根据时间范围过滤记录
const filteredRecords = computed(() => {
  if (selectedTimeRange.value === 'all') {
    return records.value
  }

  const now = new Date()
  const filterDate = new Date()

  switch (selectedTimeRange.value) {
    case 'week':
      filterDate.setDate(now.getDate() - 7)
      break
    case 'month':
      filterDate.setMonth(now.getMonth() - 1)
      break
    case 'year':
      filterDate.setFullYear(now.getFullYear() - 1)
      break
  }

  return records.value.filter(record => {
    const recordDate = new Date(record.recordDate)
    return recordDate >= filterDate
  })
})

// 格式化数字
const formatNumber = (num) => {
  if (!num) return '0.00'
  return parseFloat(num).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
}

// 获取货币符号
const getCurrencySymbol = (currency) => {
  const currencyMap = {
    'CNY': '¥',
    'USD': '$',
    'EUR': '€',
    'GBP': '£',
    'JPY': '¥',
    'HKD': 'HK$'
  }
  return currencyMap[currency] || currency + ' '
}

// 加载账户列表
const loadAccounts = async () => {
  try {
    const response = await assetAccountAPI.getAll()
    if (response.success) {
      accounts.value = response.data
    }
  } catch (error) {
    console.error('加载账户失败:', error)
  }
}

// 加载记录
const loadRecords = async () => {
  if (!selectedAccountId.value) {
    records.value = []
    return
  }

  loading.value = true
  try {
    const response = await assetRecordAPI.getByAccountId(selectedAccountId.value)
    if (response.success) {
      records.value = response.data
      await nextTick()
      updateChart()
    }
  } catch (error) {
    console.error('加载记录失败:', error)
    alert('加载记录失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

// 更新图表
const updateChart = () => {
  if (!chartCanvas.value || filteredRecords.value.length === 0) {
    if (chartInstance) {
      chartInstance.destroy()
      chartInstance = null
    }
    return
  }

  const sortedRecords = [...filteredRecords.value].sort((a, b) =>
    new Date(a.recordDate) - new Date(b.recordDate)
  )

  const labels = sortedRecords.map(r => formatDate(r.recordDate))
  const data = sortedRecords.map(r => r.amountInBaseCurrency || r.amount)

  if (chartInstance) {
    chartInstance.destroy()
  }

  const ctx = chartCanvas.value.getContext('2d')
  chartInstance = new Chart(ctx, {
    type: 'line',
    data: {
      labels: labels,
      datasets: [{
        label: '资产金额',
        data: data,
        borderColor: 'rgb(34, 197, 94)',
        backgroundColor: 'rgba(34, 197, 94, 0.1)',
        tension: 0.1,
        fill: true
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          display: true,
          position: 'top'
        },
        tooltip: {
          callbacks: {
            label: function(context) {
              return '金额: ¥' + formatNumber(context.parsed.y)
            }
          }
        }
      },
      scales: {
        y: {
          beginAtZero: false,
          ticks: {
            callback: function(value) {
              return '¥' + formatNumber(value)
            }
          }
        }
      }
    }
  })
}

// 监听时间范围变化，更新图表
watch(selectedTimeRange, () => {
  updateChart()
})

// 打开创建对话框
const openCreateDialog = () => {
  editingRecord.value = null
  // 获取当前选中账户的币种
  const selectedAccount = accounts.value.find(a => a.id === selectedAccountId.value)
  const defaultCurrency = selectedAccount ? selectedAccount.currency : 'CNY'

  formData.value = {
    recordDate: new Date().toISOString().split('T')[0],
    amount: '',
    quantity: '',
    unitPrice: '',
    currency: defaultCurrency,
    notes: ''
  }
  showDialog.value = true
}

// 编辑记录
const editRecord = (record) => {
  editingRecord.value = record
  formData.value = {
    recordDate: record.recordDate,
    amount: record.amount,
    quantity: record.quantity || '',
    unitPrice: record.unitPrice || '',
    currency: record.currency,
    notes: record.notes || ''
  }
  showDialog.value = true
}

// 删除记录
const deleteRecord = async (record) => {
  if (!confirm(`确定要删除 ${formatDate(record.recordDate)} 的记录吗？`)) return

  try {
    const response = await assetRecordAPI.delete(record.id)
    if (response.success) {
      await loadRecords()
    }
  } catch (error) {
    console.error('删除记录失败:', error)
    alert('删除失败，请重试')
  }
}

// 提交表单
const submitForm = async () => {
  try {
    const data = {
      accountId: selectedAccountId.value,
      recordDate: formData.value.recordDate,
      amount: parseFloat(formData.value.amount),
      quantity: formData.value.quantity ? parseFloat(formData.value.quantity) : null,
      unitPrice: formData.value.unitPrice ? parseFloat(formData.value.unitPrice) : null,
      currency: formData.value.currency,
      notes: formData.value.notes,
      exchangeRate: 1.0
    }

    let response
    if (editingRecord.value) {
      response = await assetRecordAPI.update(editingRecord.value.id, data)
    } else {
      response = await assetRecordAPI.create(data)
    }

    if (response.success) {
      closeDialog()
      await loadRecords()
    }
  } catch (error) {
    console.error('提交失败:', error)
    if (error.response?.data?.message) {
      alert('操作失败: ' + error.response.data.message)
    } else {
      alert('操作失败，请重试')
    }
  }
}

// 关闭对话框
const closeDialog = () => {
  showDialog.value = false
  editingRecord.value = null
}

onMounted(() => {
  loadAccounts()
})
</script>
