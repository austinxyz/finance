<template>
  <div class="p-4 md:p-6 space-y-4">
    <!-- 页面头部 - 移动端响应式 -->
    <div class="space-y-3">
      <!-- 第一行：标题 -->
      <div class="flex flex-col sm:flex-row sm:items-center gap-3">
        <h1 class="text-xl md:text-2xl font-bold text-gray-900 flex-shrink-0">批量录入投资交易</h1>
      </div>

      <!-- 第二行：家庭、月份选择器 -->
      <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <div class="flex flex-col sm:flex-row sm:items-center gap-2">
          <label class="text-sm font-medium text-gray-700 whitespace-nowrap">家庭：</label>
          <select
            v-model="selectedFamilyId"
            @change="onFamilyChange"
            class="px-3 py-1.5 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary min-h-[44px] flex-1"
          >
            <option v-for="family in families" :key="family.id" :value="family.id">
              {{ family.familyName }}
            </option>
          </select>
        </div>
        <div class="flex flex-col sm:flex-row sm:items-center gap-2">
          <label class="text-sm font-medium text-gray-700 whitespace-nowrap">月份：</label>
          <input
            v-model="transactionPeriod"
            type="month"
            @change="loadData"
            class="px-3 py-1.5 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary min-h-[44px] flex-1"
          />
        </div>
      </div>

      <!-- 第三行：操作按钮 -->
      <div class="flex items-center justify-end">
        <button
          @click="saveAll"
          :disabled="saving || !hasChanges"
          class="px-4 py-2 bg-primary text-white rounded-md hover:bg-primary/90 text-sm font-medium disabled:opacity-50 disabled:cursor-not-allowed min-h-[44px] whitespace-nowrap w-full sm:w-auto"
        >
          {{ saving ? '保存中...' : '保存全部' }}
        </button>
      </div>
    </div>

    <!-- 投资账户列表 - 移动端横向滚动 -->
    <div class="bg-white rounded-lg shadow border border-gray-200">
      <div v-if="loading" class="text-center py-8 text-gray-500 text-sm">
        加载中...
      </div>
      <div v-else-if="accounts.length === 0" class="text-center py-8 text-gray-500 text-sm">
        暂无投资账户
      </div>
      <div v-else>
        <!-- 横向滚动容器 -->
        <div class="overflow-x-auto -mx-2 sm:mx-0">
          <div class="inline-block min-w-full align-middle px-2 sm:px-0">
            <div class="divide-y divide-gray-200">
              <!-- 表头 -->
              <div class="grid grid-cols-12 gap-3 px-4 py-3 bg-gray-50 text-xs font-medium text-gray-700" style="min-width: 900px;">
                <div class="col-span-3">账户</div>
                <div class="col-span-2 text-right">{{ previousMonth3 }}</div>
                <div class="col-span-2 text-right">{{ previousMonth2 }}</div>
                <div class="col-span-2 text-right">{{ previousMonth1 }}</div>
                <div class="col-span-3 text-center">{{ currentMonth }}</div>
              </div>

              <!-- 数据行 -->
              <div
                v-for="account in accounts"
                :key="account.accountId"
                class="grid grid-cols-12 gap-3 px-4 py-2.5 hover:bg-gray-50 items-center"
                style="min-width: 900px;"
              >
                <!-- 账户信息 -->
                <div class="col-span-3">
                  <div class="flex items-center gap-2">
                    <span class="text-lg">{{ account.categoryIcon }}</span>
                    <div>
                      <div class="font-medium text-gray-900 text-sm">
                        {{ account.accountName }}
                      </div>
                      <div class="text-xs text-gray-500">
                        {{ account.categoryName }} | {{ account.userName }}
                      </div>
                    </div>
                  </div>
                </div>

                <!-- 前3个月历史数据 (只显示投入) -->
                <div class="col-span-2 text-right">
                  <div class="text-sm text-gray-600">
                    {{ getCurrencySymbol(account.currency) }}{{ formatCurrency(historyData[account.accountId]?.month3 || 0) }}
                  </div>
                </div>
                <div class="col-span-2 text-right">
                  <div class="text-sm text-gray-600">
                    {{ getCurrencySymbol(account.currency) }}{{ formatCurrency(historyData[account.accountId]?.month2 || 0) }}
                  </div>
                </div>
                <div class="col-span-2 text-right">
                  <div class="text-sm text-gray-700 font-medium">
                    {{ getCurrencySymbol(account.currency) }}{{ formatCurrency(historyData[account.accountId]?.month1 || 0) }}
                  </div>
                </div>

                <!-- 本月投入和取出输入框 -->
                <div class="col-span-3 grid grid-cols-2 gap-2">
                  <!-- 投入 -->
                  <div class="relative">
                    <label class="block text-xs text-gray-600 mb-1">投入</label>
                    <span class="absolute left-2 top-[26px] text-gray-500 text-sm">{{ getCurrencySymbol(account.currency) }}</span>
                    <input
                      v-model="accountAmounts[account.accountId].deposits"
                      type="number"
                      step="0.01"
                      min="0"
                      placeholder="0.00"
                      class="w-full pl-6 pr-2 py-1.5 text-sm border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-primary min-h-[44px]"
                      @input="markAsChanged(account.accountId)"
                    />
                  </div>
                  <!-- 取出 -->
                  <div class="relative">
                    <label class="block text-xs text-gray-600 mb-1">取出</label>
                    <span class="absolute left-2 top-[26px] text-gray-500 text-sm">{{ getCurrencySymbol(account.currency) }}</span>
                    <input
                      v-model="accountAmounts[account.accountId].withdrawals"
                      type="number"
                      step="0.01"
                      min="0"
                      placeholder="0.00"
                      class="w-full pl-6 pr-2 py-1.5 text-sm border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-primary min-h-[44px]"
                      @input="markAsChanged(account.accountId)"
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 底部统计 - 移动端响应式 -->
        <div v-if="!loading && accounts.length > 0" class="px-4 py-4 border-t border-gray-200 bg-gray-50">
          <div class="grid grid-cols-1 sm:grid-cols-3 gap-4 sm:gap-6">
            <div>
              <div class="text-xs text-gray-600 mb-1">本月总投入</div>
              <div class="text-xl sm:text-2xl font-bold text-green-600">${{ formatCurrency(summary.totalDeposits) }}</div>
            </div>
            <div>
              <div class="text-xs text-gray-600 mb-1">本月总取出</div>
              <div class="text-xl sm:text-2xl font-bold text-red-600">${{ formatCurrency(summary.totalWithdrawals) }}</div>
            </div>
            <div>
              <div class="text-xs text-gray-600 mb-1">净投入</div>
              <div class="text-xl sm:text-2xl font-bold text-primary">${{ formatCurrency(summary.netInvestment) }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, watch } from 'vue'
import { investmentAccountAPI, investmentTransactionAPI } from '@/api/investment'
import { familyAPI } from '@/api/family'

export default {
  name: 'InvestmentBatchEntry',
  setup() {
    // 数据
    const families = ref([])
    const selectedFamilyId = ref(null)
    const transactionPeriod = ref('')
    const accounts = ref([])
    const accountAmounts = ref({})
    const historyData = ref({})
    const changedAccounts = ref(new Set())

    // 状态
    const loading = ref(false)
    const saving = ref(false)

    // 计算属性
    const hasChanges = computed(() => changedAccounts.value.size > 0)

    const summary = computed(() => {
      let totalDeposits = 0
      let totalWithdrawals = 0

      accounts.value.forEach(account => {
        const amounts = accountAmounts.value[account.accountId]
        if (amounts) {
          totalDeposits += parseFloat(amounts.deposits) || 0
          totalWithdrawals += parseFloat(amounts.withdrawals) || 0
        }
      })

      return {
        totalDeposits,
        totalWithdrawals,
        netInvestment: totalDeposits - totalWithdrawals
      }
    })

    // 月份标签
    const currentMonth = computed(() => {
      if (!transactionPeriod.value) return ''
      const [year, month] = transactionPeriod.value.split('-')
      return `${year}年${month}月`
    })

    const previousMonth1 = computed(() => getPreviousMonth(transactionPeriod.value, 1))
    const previousMonth2 = computed(() => getPreviousMonth(transactionPeriod.value, 2))
    const previousMonth3 = computed(() => getPreviousMonth(transactionPeriod.value, 3))

    // 工具函数
    const formatCurrency = (value) => {
      if (!value) return '0'
      const num = parseFloat(value)
      if (num >= 1000) {
        return (num / 1000).toFixed(1) + 'K'
      }
      return num.toFixed(0)
    }

    const getCurrencySymbol = (currency) => {
      const symbols = { CNY: '¥', USD: '$', EUR: '€', GBP: '£' }
      return symbols[currency] || currency
    }

    const getPreviousMonth = (period, offset) => {
      if (!period) return ''
      const [year, month] = period.split('-').map(Number)
      const date = new Date(year, month - 1 - offset, 1)
      const m = String(date.getMonth() + 1).padStart(2, '0')
      return `${m}月`
    }

    const markAsChanged = (accountId) => {
      changedAccounts.value.add(accountId)
    }

    // 家庭变更
    const onFamilyChange = async () => {
      await loadData()
    }

    // 加载家庭列表
    const loadFamilies = async () => {
      try {
        const response = await familyAPI.getAll()
        if (response.success) {
          families.value = response.data
          if (families.value.length > 0) {
            selectedFamilyId.value = families.value[0].id
          }
        }
      } catch (error) {
        console.error('加载家庭列表失败:', error)
      }
    }

    // 加载数据
    const loadData = async () => {
      if (!selectedFamilyId.value || !transactionPeriod.value) return

      loading.value = true
      try {
        // 加载所有投资账户
        const accountsResponse = await investmentAccountAPI.getAll(selectedFamilyId.value)
        if (accountsResponse.success) {
          accounts.value = accountsResponse.data

          // 初始化金额对象
          accountAmounts.value = {}
          accounts.value.forEach(account => {
            accountAmounts.value[account.accountId] = {
              deposits: 0,
              withdrawals: 0
            }
          })

          // 加载当前月份的交易记录
          await loadCurrentMonthTransactions()

          // 加载历史数据
          await loadHistoryData()
        }
      } catch (error) {
        console.error('加载数据失败:', error)
      } finally {
        loading.value = false
      }
    }

    // 加载当前月份交易记录
    const loadCurrentMonthTransactions = async () => {
      for (const account of accounts.value) {
        try {
          const response = await investmentTransactionAPI.getByAccount(
            account.accountId,
            transactionPeriod.value,
            transactionPeriod.value
          )
          if (response.success && response.data.length > 0) {
            response.data.forEach(tx => {
              if (tx.transactionType === 'DEPOSIT') {
                accountAmounts.value[account.accountId].deposits = tx.amount
              } else if (tx.transactionType === 'WITHDRAWAL') {
                accountAmounts.value[account.accountId].withdrawals = tx.amount
              }
            })
          }
        } catch (error) {
          console.error(`加载账户${account.accountId}交易记录失败:`, error)
        }
      }
    }

    // 加载历史数据
    const loadHistoryData = async () => {
      const [year, month] = transactionPeriod.value.split('-').map(Number)

      for (const account of accounts.value) {
        const history = { month1: 0, month2: 0, month3: 0 }

        for (let i = 1; i <= 3; i++) {
          const date = new Date(year, month - 1 - i, 1)
          const period = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`

          try {
            const response = await investmentTransactionAPI.getByAccount(account.accountId, period, period)
            if (response.success && response.data.length > 0) {
              const deposits = response.data
                .filter(tx => tx.transactionType === 'DEPOSIT')
                .reduce((sum, tx) => sum + parseFloat(tx.amount), 0)
              history[`month${i}`] = deposits
            }
          } catch (error) {
            console.error(`加载账户${account.accountId}历史数据失败:`, error)
          }
        }

        historyData.value[account.accountId] = history
      }
    }

    // 保存全部
    const saveAll = async () => {
      if (!hasChanges.value) return

      saving.value = true
      try {
        const transactions = accounts.value.map(account => ({
          accountId: account.accountId,
          deposits: parseFloat(accountAmounts.value[account.accountId].deposits) || null,
          withdrawals: parseFloat(accountAmounts.value[account.accountId].withdrawals) || null,
          description: `${transactionPeriod.value} 批量录入`
        }))

        const response = await investmentTransactionAPI.batchSave({
          familyId: selectedFamilyId.value,
          transactionPeriod: transactionPeriod.value,
          transactions
        })

        if (response.success) {
          changedAccounts.value.clear()
          alert(`保存成功！创建: ${response.data.created}, 更新: ${response.data.updated}, 删除: ${response.data.deleted}`)
        } else {
          alert(response.message || '保存失败')
        }
      } catch (error) {
        console.error('保存失败:', error)
        alert('保存失败：' + error.message)
      } finally {
        saving.value = false
      }
    }

    // 初始化
    onMounted(() => {
      loadFamilies()
      // 默认设置为当前月份
      const now = new Date()
      transactionPeriod.value = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
    })

    // 监听家庭和月份变化
    watch([selectedFamilyId, transactionPeriod], () => {
      changedAccounts.value.clear()
      loadData()
    })

    return {
      // 数据
      families,
      selectedFamilyId,
      transactionPeriod,
      accounts,
      accountAmounts,
      historyData,

      // 状态
      loading,
      saving,
      hasChanges,

      // 计算属性
      summary,
      currentMonth,
      previousMonth1,
      previousMonth2,
      previousMonth3,

      // 方法
      formatCurrency,
      getCurrencySymbol,
      markAsChanged,
      onFamilyChange,
      saveAll
    }
  }
}
</script>
