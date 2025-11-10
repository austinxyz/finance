<template>
  <div class="p-6 space-y-4">
    <!-- 页面头部 -->
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-3">
        <h1 class="text-2xl font-bold text-gray-900">批量更新负债</h1>
        <select
          v-model="selectedCategoryType"
          class="px-3 py-1.5 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-red-500"
        >
          <option :value="null">全部分类</option>
          <option v-for="type in categoryTypes" :key="type.value" :value="type.value">
            {{ type.label }}
          </option>
        </select>
      </div>
      <div class="flex items-center gap-3">
        <input
          v-model="recordDate"
          type="date"
          class="px-3 py-1.5 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-red-500"
        />
        <button
          @click="saveAll"
          :disabled="saving || !hasChanges"
          class="px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 text-sm font-medium disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {{ saving ? '保存中...' : '保存全部' }}
        </button>
      </div>
    </div>

    <!-- 账户列表 -->
    <div class="bg-white rounded-lg shadow border border-gray-200">
      <div v-if="loading" class="text-center py-8 text-gray-500 text-sm">
        加载中...
      </div>
      <div v-else-if="filteredAccounts.length === 0" class="text-center py-8 text-gray-500 text-sm">
        暂无账户
      </div>
      <div v-else class="divide-y divide-gray-200">
        <div
          v-for="account in filteredAccounts"
          :key="account.id"
          class="grid grid-cols-12 gap-3 px-4 py-2.5 hover:bg-gray-50 items-center"
        >
          <!-- 账户信息 -->
          <div class="col-span-3">
            <div class="font-medium text-gray-900 text-sm">{{ account.accountName }}</div>
            <div class="text-xs text-gray-500 mt-0.5">
              {{ getTypeLabel(account.categoryType) }} › {{ account.categoryName }}
            </div>
          </div>

          <!-- 用户 -->
          <div class="col-span-1">
            <div class="text-xs text-gray-600">{{ account.userName || '-' }}</div>
          </div>

          <!-- 最近余额和日期 -->
          <div class="col-span-2 text-right">
            <div class="text-sm font-semibold text-red-600">
              {{ getCurrencySymbol(account.currency) }}{{ formatNumber(account.latestBalance) }}
            </div>
            <div class="text-xs text-gray-400 mt-0.5">
              {{ formatDate(account.latestRecordDate) }}
            </div>
          </div>

          <!-- 新余额输入 -->
          <div class="col-span-3">
            <input
              v-model="accountBalances[account.id]"
              type="number"
              step="0.01"
              placeholder="新余额"
              class="w-full px-2 py-1.5 text-sm border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-red-500"
              @input="markAsChanged(account.id)"
            />
          </div>

          <!-- 差额显示（负债减少显示为绿色） -->
          <div class="col-span-3 text-right">
            <div v-if="accountBalances[account.id]" class="text-xs">
              <span class="text-gray-500">差额: </span>
              <span :class="getDifferenceClass(account.id, account.latestBalance)">
                {{ formatDifference(account.id, account.latestBalance, account.currency) }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { liabilityAccountAPI, liabilityRecordAPI } from '@/api/liability'
import { getExchangeRate } from '@/utils/exchangeRate'

const userId = ref(1) // TODO: 从用户登录状态获取
const loading = ref(false)
const saving = ref(false)
const accounts = ref([])
const accountBalances = ref({})
const changedAccounts = ref(new Set())
const selectedCategoryType = ref(null)

// 记录日期，默认为今天
const today = new Date().toISOString().split('T')[0]
const recordDate = ref(today)

// 分类类型
const categoryTypes = [
  { value: 'MORTGAGE', label: '房贷' },
  { value: 'AUTO_LOAN', label: '车贷' },
  { value: 'CREDIT_CARD', label: '信用卡' },
  { value: 'PERSONAL_LOAN', label: '个人借款' },
  { value: 'STUDENT_LOAN', label: '学生贷款' },
  { value: 'BUSINESS_LOAN', label: '商业贷款' },
  { value: 'OTHER', label: '其他' }
]

// 过滤后的账户
const filteredAccounts = computed(() => {
  if (selectedCategoryType.value === null) {
    return accounts.value
  }
  return accounts.value.filter(a => a.categoryType === selectedCategoryType.value)
})

// 是否有修改
const hasChanges = computed(() => changedAccounts.value.size > 0)

// 格式化数字
const formatNumber = (num) => {
  if (!num) return '0.00'
  return parseFloat(num).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const recordDate = new Date(date.getFullYear(), date.getMonth(), date.getDate())

  const diffDays = Math.floor((today - recordDate) / (1000 * 60 * 60 * 24))

  if (diffDays === 0) return '今天'
  if (diffDays === 1) return '昨天'
  if (diffDays < 7) return `${diffDays}天前`

  return date.toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' })
}

// 获取大类别标签
const getTypeLabel = (type) => {
  const typeMap = {
    'MORTGAGE': '房贷',
    'AUTO_LOAN': '车贷',
    'CREDIT_CARD': '信用卡',
    'PERSONAL_LOAN': '个人借款',
    'STUDENT_LOAN': '学生贷款',
    'BUSINESS_LOAN': '商业贷款',
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
  const balance = accountBalances.value[accountId]
  if (balance && parseFloat(balance) > 0) {
    changedAccounts.value.add(accountId)
  } else {
    changedAccounts.value.delete(accountId)
  }
}

// 计算差额（负债减少是好的）
const formatDifference = (accountId, currentBalance, currency) => {
  const newBalance = parseFloat(accountBalances.value[accountId])
  if (!newBalance || isNaN(newBalance)) return ''

  const diff = newBalance - (currentBalance || 0)
  const sign = diff >= 0 ? '+' : ''
  return `${sign}${getCurrencySymbol(currency)}${formatNumber(Math.abs(diff))}`
}

// 获取差额样式类（负债减少显示为绿色，增加显示为红色）
const getDifferenceClass = (accountId, currentBalance) => {
  const newBalance = parseFloat(accountBalances.value[accountId])
  if (!newBalance || isNaN(newBalance)) return ''

  const diff = newBalance - (currentBalance || 0)
  return diff < 0 ? 'text-green-600 font-medium' : 'text-red-600 font-medium'
}

// 加载账户列表
const loadAccounts = async () => {
  loading.value = true
  try {
    const response = await liabilityAccountAPI.getAll()
    console.log('Response:', response)

    // Axios 拦截器已经返回了 response.data，所以 response 就是后端的完整响应
    if (response && response.success) {
      // 按大类别排序账户
      const sortedAccounts = (response.data || []).sort((a, b) => {
        // 定义大类别的优先级顺序
        const categoryOrder = {
          'MORTGAGE': 1,
          'AUTO_LOAN': 2,
          'CREDIT_CARD': 3,
          'PERSONAL_LOAN': 4,
          'STUDENT_LOAN': 5,
          'BUSINESS_LOAN': 6,
          'OTHER': 7
        }

        const orderA = categoryOrder[a.categoryType] || 999
        const orderB = categoryOrder[b.categoryType] || 999

        // 首先按大类别排序
        if (orderA !== orderB) {
          return orderA - orderB
        }

        // 同一大类别内，按账户名称排序
        return (a.accountName || '').localeCompare(b.accountName || '', 'zh-CN')
      })

      accounts.value = sortedAccounts
      console.log('Loaded accounts:', accounts.value)

      // 初始化余额输入框，为空字符串以便用户输入
      accounts.value.forEach(account => {
        accountBalances.value[account.id] = ''
      })
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

// 保存全部
const saveAll = async (overwriteExisting = false) => {
  saving.value = true
  try {
    // 构建所有账户列表（包括有修改和没有修改的）
    const allAccountIds = accounts.value.map(a => a.id)

    // 检查哪些账户在指定日期已有记录
    const checkResponse = await liabilityRecordAPI.checkExisting({
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

      // 获取用户输入的余额
      const inputBalance = accountBalances.value[account.id]
      let balance = null

      if (inputBalance && parseFloat(inputBalance) > 0) {
        // 用户输入了新余额
        balance = parseFloat(inputBalance)
      } else if (!existingAccountIds.includes(account.id) && account.latestBalance) {
        // 用户没有输入新余额，且该日期没有记录，使用最近记录的余额
        balance = account.latestBalance
      }

      // 如果有余额（用户输入的或最近记录的），添加到批量更新列表
      if (balance !== null && balance > 0) {
        const currency = account.currency || 'USD'
        const exchangeRate = getExchangeRate(currency)

        batchData.accounts.push({
          accountId: account.id,
          amount: balance,
          exchangeRate: exchangeRate,
          currency: currency
        })
      }
    })

    if (batchData.accounts.length === 0) {
      alert('没有需要保存的记录')
      return
    }

    console.log('批量更新请求数据:', JSON.stringify(batchData, null, 2))
    const response = await liabilityRecordAPI.batchUpdate(batchData)
    if (response.success) {
      const updatedCount = response.data.length
      const userChangedCount = changedAccountsArray.length
      const autoFilledCount = batchData.accounts.length - changedAccountsArray.filter(id =>
        batchData.accounts.some(a => a.accountId === id)
      ).length

      let message = `成功更新 ${updatedCount} 个账户的负债记录`
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
    console.error('错误响应:', error.response)
    console.error('错误数据:', error.response?.data)
    alert('批量更新失败: ' + (error.response?.data?.message || error.message))
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadAccounts()
})
</script>
