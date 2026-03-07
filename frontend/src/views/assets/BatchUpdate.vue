<template>
  <div class="p-3 md:p-4 space-y-3">
    <!-- Calculator Component -->
    <Calculator :show="showCalculator" @close="closeCalculator" @apply="applyCalculatorResult" />
    <!-- 选择器和保存按钮 - 紧凑单行布局 -->
    <div class="flex flex-col sm:flex-row gap-2 items-start sm:items-center sm:justify-between border-b border-gray-200 pb-2">
      <div class="flex flex-col sm:flex-row gap-2">
        <div class="flex items-center gap-2">
          <label class="text-xs font-medium text-gray-700 whitespace-nowrap">分类:</label>
          <select
            v-model="selectedCategoryType"
            class="px-2 py-1 text-xs border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-primary"
          >
            <option :value="null">全部</option>
            <option v-for="type in categoryTypes" :key="type.value" :value="type.value">
              {{ type.label }}
            </option>
          </select>
        </div>
        <div class="flex items-center gap-2">
          <label class="text-xs font-medium text-gray-700 whitespace-nowrap">日期:</label>
          <input
            v-model="recordDate"
            type="date"
            class="px-2 py-1 text-xs border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-primary"
          />
        </div>
      </div>
      <button
        @click="saveAll"
        :disabled="saving || !hasChanges"
        class="px-3 py-1.5 bg-primary text-white rounded text-xs font-medium disabled:opacity-50 disabled:cursor-not-allowed whitespace-nowrap w-full sm:w-auto"
      >
        {{ saving ? '保存中...' : '保存全部' }}
      </button>
    </div>

    <!-- 顶部统计 - 固定不滚动 -->
    <div v-if="!loading && filteredAccounts.length > 0" class="bg-white rounded-lg shadow border border-gray-200 px-3 py-2">
      <div class="grid grid-cols-2 sm:grid-cols-4 gap-3">
        <div>
          <div class="text-xs text-gray-600 mb-0.5">账户数量</div>
          <div class="text-base font-bold text-gray-900">{{ summary.accountCount }}</div>
        </div>
        <div>
          <div class="text-xs text-gray-600 mb-0.5">{{ summary.formattedPreviousDate }}总额</div>
          <div class="text-sm font-medium text-gray-700">${{ formatNumber(summary.totalPrevious) }}</div>
        </div>
        <div>
          <div class="text-xs text-gray-600 mb-0.5">{{ formattedRecordDate }}总金额</div>
          <div class="text-base font-bold text-gray-900">${{ formatNumber(summary.totalNew) }}</div>
        </div>
        <div>
          <div class="text-xs text-gray-600 mb-0.5">变化金额</div>
          <div :class="[
            'text-base font-bold',
            summary.difference > 0 ? 'text-green-600' : summary.difference < 0 ? 'text-red-600' : 'text-gray-600'
          ]">
            {{ summary.difference >= 0 ? '+' : '' }}${{ formatNumber(Math.abs(summary.difference)) }}
          </div>
        </div>
      </div>
    </div>

    <!-- 账户列表 - 表格布局（带滚动条） -->
    <div class="bg-white rounded-lg shadow border border-gray-200">
      <div v-if="loading" class="text-center py-6 text-gray-500 text-xs">加载中...</div>
      <div v-else-if="filteredAccounts.length === 0" class="text-center py-6 text-gray-500 text-xs">暂无账户</div>
      <div v-else>
        <div class="overflow-x-auto max-h-[calc(100vh-280px)] overflow-y-auto">
          <table class="w-full">
            <thead class="bg-gray-50 sticky top-0">
              <tr>
                <th class="px-2 py-1.5 text-left text-xs font-medium text-gray-700">账户</th>
                <th class="px-2 py-1.5 text-left text-xs font-medium text-gray-700">分类</th>
                <th class="px-2 py-1.5 text-left text-xs font-medium text-gray-700">用户</th>
                <th class="px-2 py-1.5 text-right text-xs font-medium text-gray-700">之前金额</th>
                <th class="px-2 py-1.5 text-right text-xs font-medium text-gray-700">日期</th>
                <th class="px-2 py-1.5 text-center text-xs font-medium text-gray-700">新金额</th>
                <th class="px-2 py-1.5 text-right text-xs font-medium text-gray-700">差额</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-200">
              <tr v-for="account in filteredAccounts" :key="account.id" class="hover:bg-gray-50">
                <!-- 账户名称 -->
                <td class="px-2 py-1.5 text-xs font-medium text-gray-900">
                  {{ account.accountName }}
                </td>

                <!-- 分类 -->
                <td class="px-2 py-1.5">
                  <div class="flex items-center gap-1.5">
                    <span class="text-base">{{ account.assetTypeIcon }}</span>
                    <span class="text-xs text-gray-600">{{ account.assetTypeName }}</span>
                  </div>
                </td>

                <!-- 用户 -->
                <td class="px-2 py-1.5 text-xs text-gray-600">
                  {{ account.userName || '-' }}
                </td>

                <!-- 之前金额 -->
                <td class="px-2 py-1.5 text-right text-xs font-medium text-gray-900 whitespace-nowrap">
                  {{ getCurrencySymbol(account.currency) }}{{ formatNumber(accountPreviousValues[account.id]?.amount ?? 0) }}
                </td>

                <!-- 日期 -->
                <td class="px-2 py-1.5 text-right text-xs text-gray-500 whitespace-nowrap">
                  {{ formatFullDate(accountPreviousValues[account.id]?.recordDate) }}
                  <span v-if="accountPreviousValues[account.id]?.hasExactRecord" class="ml-1 text-amber-600" title="该日期已有记录">📝</span>
                </td>

                <!-- 新金额输入 -->
                <td class="px-2 py-1.5">
                  <div class="flex items-center gap-1">
                    <input
                      v-model="accountAmounts[account.id]"
                      type="number"
                      step="0.01"
                      placeholder="新金额"
                      class="w-24 px-2 py-1 text-xs border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-primary text-right"
                      @input="markAsChanged(account.id)"
                    />
                    <button
                      @click="openCalculator(account.id)"
                      class="p-1 text-gray-500 hover:text-primary transition"
                      title="打开计算器"
                    >
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 7h6m0 10v-3m-3 3h.01M9 17h.01M9 14h.01M12 14h.01M15 11h.01M12 11h.01M9 11h.01M7 21h10a2 2 0 002-2V5a2 2 0 00-2-2H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
                      </svg>
                    </button>
                  </div>
                </td>

                <!-- 差额 -->
                <td class="px-2 py-1.5 text-right text-xs whitespace-nowrap">
                  <span v-if="accountAmounts[account.id] !== ''" :class="getDifferenceClass(account.id, accountPreviousValues[account.id]?.amount ?? 0)">
                    {{ formatDifference(account.id, accountPreviousValues[account.id]?.amount ?? 0, account.currency) }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { assetAccountAPI, assetRecordAPI } from '@/api/asset'
import { useFamilyStore } from '@/stores/family'
import { getExchangeRate } from '@/utils/exchangeRate'
import { getTodayDate } from '@/lib/utils'
import Calculator from '@/components/Calculator.vue'

const userId = ref(1) // TODO: 从用户登录状态获取
const loading = ref(false)
const saving = ref(false)
const accounts = ref([])
const accountAmounts = ref({})
const accountPreviousValues = ref({}) // 存储每个账户在选择日期的之前值
const changedAccounts = ref(new Set())
const selectedCategoryType = ref(null)

// Calculator state
const showCalculator = ref(false)
const currentCalculatorAccountId = ref(null)

// Family store
const familyStore = useFamilyStore()
const selectedFamilyId = computed(() => familyStore.currentFamilyId)

// 记录日期，默认为今天（使用洛杉矶时区）
const today = getTodayDate()
const recordDate = ref(today)

// 分类类型
const categoryTypes = [
  { value: 'CASH', label: '现金类' },
  { value: 'STOCKS', label: '股票投资' },
  { value: 'RETIREMENT_FUND', label: '退休基金' },
  { value: 'INSURANCE', label: '保险' },
  { value: 'REAL_ESTATE', label: '房地产' },
  { value: 'CRYPTOCURRENCY', label: '数字货币' },
  { value: 'PRECIOUS_METALS', label: '贵金属' },
  { value: 'OTHER', label: '其他' }
]

// 过滤后的账户
const filteredAccounts = computed(() => {
  if (selectedCategoryType.value === null) {
    return accounts.value
  }
  return accounts.value.filter(a => a.assetTypeCode === selectedCategoryType.value)
})

// 是否有修改
const hasChanges = computed(() => changedAccounts.value.size > 0)

// 格式化日期显示 (月/日)
const formattedRecordDate = computed(() => {
  if (!recordDate.value) return ''
  const [year, month, day] = recordDate.value.split('-')
  return `${parseInt(month)}/${parseInt(day)}`
})

// 统计数据
const summary = computed(() => {
  let totalPrevious = 0
  let totalNew = 0
  let accountCount = 0
  let latestPreviousDate = null

  filteredAccounts.value.forEach(account => {
    const previousData = accountPreviousValues.value[account.id]
    const previousAmount = previousData?.amount || 0
    const previousCurrency = previousData?.currency || account.currency || 'USD'
    const previousDate = previousData?.recordDate

    // Track the latest previous date
    if (previousDate) {
      if (!latestPreviousDate || previousDate > latestPreviousDate) {
        latestPreviousDate = previousDate
      }
    }

    // Use current exchange rate to convert previous amount to USD
    const previousExchangeRate = getExchangeRate(previousCurrency)
    const previousInUSD = previousAmount * previousExchangeRate
    totalPrevious += previousInUSD

    // For new total: use user input if available, otherwise use previous value
    const userInput = accountAmounts.value[account.id]
    let finalAmount = previousAmount
    let finalCurrency = previousCurrency

    if (userInput !== '' && userInput !== null && userInput !== undefined) {
      const newAmount = parseFloat(userInput)
      if (!isNaN(newAmount)) {
        finalAmount = newAmount
        finalCurrency = account.currency || 'USD'
      }
    }

    const finalExchangeRate = getExchangeRate(finalCurrency)
    const finalInUSD = finalAmount * finalExchangeRate
    totalNew += finalInUSD

    accountCount++
  })

  const difference = totalNew - totalPrevious

  // Format the latest previous date (月/日)
  let formattedPreviousDate = ''
  if (latestPreviousDate) {
    const [year, month, day] = latestPreviousDate.split('-')
    formattedPreviousDate = `${parseInt(month)}/${parseInt(day)}`
  }

  return {
    totalPrevious,
    totalNew,
    difference,
    accountCount,
    formattedPreviousDate
  }
})

// 格式化数字
const formatNumber = (num) => {
  if (!num) return '0.00'
  return parseFloat(num).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '-'
  // 直接解析字符串格式 YYYY-MM-DD，避免时区转换
  let year, month, day
  if (typeof dateString === 'string' && dateString.match(/^\d{4}-\d{2}-\d{2}$/)) {
    [year, month, day] = dateString.split('-').map(Number)
  } else {
    const date = new Date(dateString)
    year = date.getFullYear()
    month = date.getMonth() + 1
    day = date.getDate()
  }
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth() + 1, now.getDate())
  const recordDate = new Date(year, month, day)
  const diffDays = Math.floor((today - recordDate) / (1000 * 60 * 60 * 24))
  if (diffDays === 0) return '今天'
  if (diffDays === 1) return '昨天'
  if (diffDays < 7) return `${diffDays}天前`
  return `${month}/${day}`
}

// 格式化完整日期 (月/日/年)
const formatFullDate = (dateString) => {
  if (!dateString) return '-'

  // 直接解析字符串格式 YYYY-MM-DD，避免时区转换
  if (typeof dateString === 'string' && dateString.match(/^\d{4}-\d{2}-\d{2}$/)) {
    const [year, month, day] = dateString.split('-')
    return `${parseInt(month)}/${parseInt(day)}/${year}`
  }

  // 兼容其他格式
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN', {
    month: 'numeric',
    day: 'numeric',
    year: 'numeric'
  })
}
// 获取大类别标签
const getTypeLabel = (type) => {
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
  return typeMap[type] || type
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
// 标记为已修改
const markAsChanged = (accountId) => {
  const amount = accountAmounts.value[accountId]
  if (amount && parseFloat(amount) > 0) {
    changedAccounts.value.add(accountId)
  } else {
    changedAccounts.value.delete(accountId)
  }
}

// Calculator functions
const openCalculator = (accountId) => {
  currentCalculatorAccountId.value = accountId
  showCalculator.value = true
}

const closeCalculator = () => {
  showCalculator.value = false
  currentCalculatorAccountId.value = null
}

const applyCalculatorResult = (result) => {
  if (currentCalculatorAccountId.value !== null) {
    accountAmounts.value[currentCalculatorAccountId.value] = result.toString()
    markAsChanged(currentCalculatorAccountId.value)
  }
}

// 计算差额
const formatDifference = (accountId, currentAmount, currency) => {
  const newAmount = parseFloat(accountAmounts.value[accountId])
  if (!newAmount || isNaN(newAmount)) return ''

  const diff = newAmount - (currentAmount || 0)
  const sign = diff >= 0 ? '+' : ''
  return `${sign}${getCurrencySymbol(currency)}${formatNumber(Math.abs(diff))}`
}
// 获取差额样式类
const getDifferenceClass = (accountId, currentAmount) => {
  const newAmount = parseFloat(accountAmounts.value[accountId])
  if (!newAmount || isNaN(newAmount)) return ''
  const diff = newAmount - (currentAmount || 0)
  return diff >= 0 ? 'text-green-600 font-medium' : 'text-red-600 font-medium'
}

// 加载账户列表
const loadAccounts = async () => {
  if (!selectedFamilyId.value) return

  loading.value = true
  try {
    const response = await assetAccountAPI.getAllByFamily(selectedFamilyId.value)

    // Axios 拦截器已经返回了 response.data，所以 response 就是后端的完整响应
    if (response && response.success) {
      // 按大类别排序账户
      const sortedAccounts = (response.data || []).sort((a, b) => {
        // 定义大类别的优先级顺序
        const categoryOrder = {
          'CASH': 1,
          'STOCKS': 2,
          'RETIREMENT_FUND': 3,
          'INSURANCE': 4,
          'REAL_ESTATE': 5,
          'CRYPTOCURRENCY': 6,
          'PRECIOUS_METALS': 7,
          'OTHER': 8
        }

        const orderA = categoryOrder[a.assetTypeCode] || 999
        const orderB = categoryOrder[b.assetTypeCode] || 999

        // 首先按大类别排序
        if (orderA !== orderB) {
          return orderA - orderB
        }

        // 同一大类别内，按账户名称排序
        return (a.accountName || '').localeCompare(b.accountName || '', 'zh-CN')
      })

      accounts.value = sortedAccounts

      // 初始化金额输入框，为空字符串以便用户输入
      accounts.value.forEach(account => {
        accountAmounts.value[account.id] = ''
      })

      // 加载选择日期的之前值
      await loadPreviousValues()
    } else {
      console.error('API returned error:', response)
      alert('加载失败: ' + (response?.message || '未知错误'))
    }
  } catch (error) {
    console.error('加载账户失败:', error)
    alert('加载账户失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}
// 加载所有账户在选择日期的之前值（批量一次请求）
const loadPreviousValues = async () => {
  if (!accounts.value || accounts.value.length === 0) return
  try {
    const accountIds = accounts.value.map(a => a.id)
    const response = await assetRecordAPI.batchGetValuesAtDate(accountIds, recordDate.value)
    if (response && response.success && response.data) {
      accounts.value.forEach(account => {
        const data = response.data[account.id]
        if (data && data.amount !== undefined) {
          accountPreviousValues.value[account.id] = {
            amount: data.amount || 0,
            recordDate: data.recordDate,
            currency: data.currency,
            hasExactRecord: data.hasExactRecord || false
          }
        } else {
          accountPreviousValues.value[account.id] = {
            amount: account.latestAmount || 0,
            recordDate: account.latestRecordDate,
            currency: account.currency,
            hasExactRecord: false
          }
        }
      })
    }
  } catch (error) {
    console.error('加载之前值失败:', error)
  }
}

// 监听日期变化，重新加载之前值
watch(recordDate, async (newDate, oldDate) => {
  if (newDate !== oldDate && accounts.value.length > 0) {
    await loadPreviousValues()
  }
})

// 保存全部
const saveAll = async (overwriteExisting = false) => {
  saving.value = true
  try {
    // 构建所有账户列表（包括有修改和没有修改的）
    const allAccountIds = accounts.value.map(a => a.id)

    // 检查哪些账户在指定日期已有记录
    const checkResponse = await assetRecordAPI.checkExisting({
      recordDate: recordDate.value,
      accountIds: allAccountIds
    })

    const existingAccountIds = checkResponse.success ? checkResponse.data : []

    // 如果有用户修改的账户在指定日期已有记录，且不是覆盖模式，询问用户
    const changedAccountsArray = Array.from(changedAccounts.value)
    const conflictingAccounts = changedAccountsArray.filter(id => existingAccountIds.includes(id))

    if (!overwriteExisting && conflictingAccounts.length > 0) {
      const existingAccountNames = conflictingAccounts.map(accountId => {
        const account = accounts.value.find(a => a.id === accountId)
        return account ? account.accountName : `账户 ${accountId}`
      }).join(', ')

      const confirmed = confirm(
        `以下账户在 ${recordDate.value} 已有记录：\n${existingAccountNames}\n\n` +
        `是否覆盖这些记录？\n` +
        `点击"确定"覆盖，点击"取消"跳过这些账户。`
      )

      if (confirmed) {
        // 用户选择覆盖，递归调用并设置覆盖标志
        return await saveAll(true)
      }
      // 用户选择跳过，继续执行（overwriteExisting=false）
    }

    // 构建批量更新请求
    const batchData = {
      recordDate: recordDate.value,
      overwriteExisting: Boolean(overwriteExisting),
      accounts: []
    }

    // 遍历所有账户
    accounts.value.forEach(account => {
      // 如果这个账户在指定日期已有记录，且用户选择不覆盖，跳过
      if (existingAccountIds.includes(account.id) && !overwriteExisting) {
        return
      }

      // 获取用户输入的金额
      const inputAmount = accountAmounts.value[account.id]
      let amount = null

      // 处理金额逻辑:
      // 1. 如果用户明确填了金额(包括0)，使用用户填的值
      // 2. 如果用户没填(空字符串或null)，使用该日期的之前值
      if (inputAmount !== '' && inputAmount !== null && inputAmount !== undefined) {
        // 用户明确填了金额，使用用户的输入(可以是0)
        amount = parseFloat(inputAmount)
      } else {
        // 用户没填，使用该日期的之前值
        const previousValue = accountPreviousValues.value[account.id]
        if (previousValue && previousValue.amount !== null && previousValue.amount !== undefined) {
          amount = previousValue.amount
        } else {
          // 如果没有之前值，跳过这个账户
          return
        }
      }

      // 添加到批量更新列表 (允许金额为0)
      if (amount !== null && amount !== undefined && !isNaN(amount)) {
        const currency = account.currency || 'USD'
        const exchangeRate = getExchangeRate(currency)

        batchData.accounts.push({
          accountId: account.id,
          amount: amount,
          exchangeRate: exchangeRate,
          currency: currency
        })
      }
    })

    if (batchData.accounts.length === 0) {
      alert('没有需要保存的记录')
      return
    }

    const response = await assetRecordAPI.batchUpdate(batchData)
    if (response.success) {
      const updatedCount = response.data.count
      const userChangedCount = changedAccountsArray.length
      const autoFilledCount = batchData.accounts.length - changedAccountsArray.filter(id =>
        batchData.accounts.some(a => a.accountId === id)
      ).length

      let message = `成功更新 ${updatedCount} 个账户的资产记录`
      if (userChangedCount > 0) {
        message += `\n其中用户修改: ${userChangedCount} 个`
      }
      if (autoFilledCount > 0) {
        message += `\n自动填充(使用最近记录): ${autoFilledCount} 个`
      }
      alert(message)

      changedAccounts.value.clear()
      // 重新加载账户数据
      await loadAccounts()
    }
  } catch (error) {
    console.error('批量更新失败:', error)
    alert('批量更新失败: ' + (error.response?.data?.message || error.message))
  } finally {
    saving.value = false
  }
}
// 监听selectedFamilyId变化，自动加载账户数据（管理员切换家庭时）
watch(selectedFamilyId, (newId) => {
  if (newId) {
    // 清空当前数据
    accountAmounts.value = {}
    accountPreviousValues.value = {}
    changedAccounts.value.clear()
    // 重新加载账户列表
    loadAccounts()
  }
})

onMounted(async () => {
  // 如果家庭已加载，立即加载账户
  if (selectedFamilyId.value) {
    await loadAccounts()
  }
})
</script>
