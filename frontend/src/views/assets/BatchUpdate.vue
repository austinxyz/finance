<template>
  <div class="p-6 space-y-4">
    <!-- 页面头部 -->
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-3">
        <h1 class="text-2xl font-bold text-gray-900">批量更新资产</h1>
        <select
          v-model="selectedCategoryType"
          class="px-3 py-1.5 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary"
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
          class="px-3 py-1.5 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary"
        />
        <button
          @click="saveAll"
          :disabled="saving || !hasChanges"
          class="px-4 py-2 bg-primary text-white rounded-md hover:bg-primary/90 text-sm font-medium disabled:opacity-50 disabled:cursor-not-allowed"
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

          <!-- 最近金额和日期 -->
          <div class="col-span-2 text-right">
            <div class="text-sm font-semibold text-gray-900">
              {{ getCurrencySymbol(account.currency) }}{{ formatNumber(account.latestAmount) }}
            </div>
            <div class="text-xs text-gray-400 mt-0.5">
              {{ formatDate(account.latestRecordDate) }}
            </div>
          </div>

          <!-- 新金额输入 -->
          <div class="col-span-3">
            <input
              v-model="accountAmounts[account.id]"
              type="number"
              step="0.01"
              placeholder="新金额"
              class="w-full px-2 py-1.5 text-sm border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-primary"
              @input="markAsChanged(account.id)"
            />
          </div>

          <!-- 差额显示 -->
          <div class="col-span-3 text-right">
            <div v-if="accountAmounts[account.id]" class="text-xs">
              <span class="text-gray-500">差额: </span>
              <span :class="getDifferenceClass(account.id, account.latestAmount)">
                {{ formatDifference(account.id, account.latestAmount, account.currency) }}
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
import { assetAccountAPI, assetRecordAPI } from '@/api/asset'

const userId = ref(1) // TODO: 从用户登录状态获取
const loading = ref(false)
const saving = ref(false)
const accounts = ref([])
const accountAmounts = ref({})
const changedAccounts = ref(new Set())
const selectedCategoryType = ref(null)

// 记录日期，默认为今天
const today = new Date().toISOString().split('T')[0]
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
  return accounts.value.filter(a => a.categoryType === selectedCategoryType.value)
})

// 是否有修改
const hasChanges = computed(() => changedAccounts.value.size > 0)

// 格式化数字
const formatNumber = (num) => {
  if (!num) return '0.00'
  return parseFloat(num).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
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
  loading.value = true
  try {
    const response = await assetAccountAPI.getAll()
    console.log('Response:', response)

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

      // 初始化金额输入框，为空字符串以便用户输入
      accounts.value.forEach(account => {
        accountAmounts.value[account.id] = ''
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
      overwriteExisting: overwriteExisting,
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

      if (inputAmount && parseFloat(inputAmount) > 0) {
        // 用户输入了新金额
        amount = parseFloat(inputAmount)
      } else if (!existingAccountIds.includes(account.id) && account.latestAmount) {
        // 用户没有输入新金额，且该日期没有记录，使用最近记录的金额
        amount = account.latestAmount
      }

      // 如果有金额（用户输入的或最近记录的），添加到批量更新列表
      if (amount !== null && amount > 0) {
        batchData.accounts.push({
          accountId: account.id,
          amount: amount,
          exchangeRate: 1.0,
          currency: account.currency || 'CNY'
        })
      }
    })

    if (batchData.accounts.length === 0) {
      alert('没有需要保存的记录')
      return
    }

    const response = await assetRecordAPI.batchUpdate(batchData)
    if (response.success) {
      const updatedCount = response.data.length
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

onMounted(() => {
  loadAccounts()
})
</script>
