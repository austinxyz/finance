<template>
  <div class="p-3 md:p-4 space-y-3">
    <!-- Tab 切换和家庭选择器 -->
    <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 border-b border-gray-200 pb-2">
      <nav class="-mb-2 flex space-x-2" aria-label="Tabs">
        <button
          @click="activeTab = 'by-month'"
          :class="[
            'whitespace-nowrap py-2 px-3 border-b-2 font-medium text-xs transition-colors',
            activeTab === 'by-month'
              ? 'border-primary text-primary'
              : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
          ]"
        >
          按月份录入
        </button>
        <button
          @click="activeTab = 'by-account'"
          :class="[
            'whitespace-nowrap py-2 px-3 border-b-2 font-medium text-xs transition-colors',
            activeTab === 'by-account'
              ? 'border-primary text-primary'
              : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
          ]"
        >
          按账户录入
        </button>
      </nav>

      <div class="flex items-center gap-2">
        <label class="text-xs font-medium text-gray-700 whitespace-nowrap">家庭:</label>
        <select
          v-model="activeTab === 'by-month' ? selectedFamilyId : selectedFamilyIdYear"
          @change="activeTab === 'by-month' ? loadMonthData() : loadYearAccounts()"
          class="px-2 py-1 text-xs border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-primary"
        >
          <option v-for="family in families" :key="family.id" :value="family.id">
            {{ family.familyName }}
          </option>
        </select>
      </div>
    </div>

    <!-- 按月份录入模式 -->
    <div v-if="activeTab === 'by-month'" class="space-y-2">
      <!-- 选择器和保存按钮 -->
      <div class="flex flex-col sm:flex-row gap-2 items-start sm:items-center sm:justify-between">
        <div class="flex items-center gap-2">
          <label class="text-xs font-medium text-gray-700 whitespace-nowrap">月份:</label>
          <input
            v-model="transactionPeriod"
            type="month"
            @change="loadMonthData"
            class="px-2 py-1 text-xs border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-primary"
          />
        </div>

        <button
          @click="saveMonthData"
          :disabled="savingMonth || !hasMonthChanges"
          class="px-3 py-1.5 bg-primary text-white rounded text-xs font-medium disabled:opacity-50 disabled:cursor-not-allowed whitespace-nowrap w-full sm:w-auto"
        >
          {{ savingMonth ? '保存中...' : '保存全部' }}
        </button>
      </div>

      <!-- 按月份的账户列表 -->
      <div class="bg-white rounded-lg shadow border border-gray-200">
        <div v-if="loadingMonth" class="text-center py-6 text-gray-500 text-xs">加载中...</div>
        <div v-else-if="monthAccounts.length === 0" class="text-center py-6 text-gray-500 text-xs">暂无投资账户</div>
        <div v-else>
          <div class="overflow-x-auto">
            <div class="inline-block min-w-full align-middle">
              <div class="divide-y divide-gray-200">
                <!-- 表头 -->
                <div class="grid grid-cols-12 gap-2 px-2 py-1.5 bg-gray-50 text-xs font-medium text-gray-700" style="min-width: 800px;">
                  <div class="col-span-3">账户</div>
                  <div class="col-span-2 text-right">{{ previousMonth3 }}</div>
                  <div class="col-span-2 text-right">{{ previousMonth2 }}</div>
                  <div class="col-span-2 text-right">{{ previousMonth1 }}</div>
                  <div class="col-span-3 text-center">{{ currentMonth }}</div>
                </div>

                <!-- 数据行 -->
                <div
                  v-for="account in monthAccounts"
                  :key="account.accountId"
                  class="grid grid-cols-12 gap-2 px-2 py-1.5 hover:bg-gray-50 items-center"
                  style="min-width: 800px;"
                >
                  <!-- 账户信息 -->
                  <div class="col-span-3">
                    <div class="flex items-center gap-1.5">
                      <span class="text-base">{{ account.categoryIcon }}</span>
                      <div>
                        <div class="font-medium text-gray-900 text-xs">{{ account.accountName }}</div>
                        <div class="text-xs text-gray-500">{{ account.categoryName }} | {{ account.userName }}</div>
                      </div>
                    </div>
                  </div>

                  <!-- 历史数据 -->
                  <div class="col-span-2 text-right text-xs text-gray-600">
                    {{ getCurrencySymbol(account.currency) }}{{ formatCurrency(monthHistoryData[account.accountId]?.month3 || 0) }}
                  </div>
                  <div class="col-span-2 text-right text-xs text-gray-600">
                    {{ getCurrencySymbol(account.currency) }}{{ formatCurrency(monthHistoryData[account.accountId]?.month2 || 0) }}
                  </div>
                  <div class="col-span-2 text-right text-xs text-gray-700 font-medium">
                    {{ getCurrencySymbol(account.currency) }}{{ formatCurrency(monthHistoryData[account.accountId]?.month1 || 0) }}
                  </div>

                  <!-- 本月输入 -->
                  <div class="col-span-3 grid grid-cols-2 gap-1.5">
                    <div class="relative">
                      <label class="block text-xs text-gray-600 mb-0.5">投入</label>
                      <span class="absolute left-1.5 top-[20px] text-gray-500 text-xs">{{ getCurrencySymbol(account.currency) }}</span>
                      <input
                        v-model="monthAmounts[account.accountId].deposits"
                        type="number"
                        step="0.01"
                        min="0"
                        placeholder="0.00"
                        class="w-full pl-5 pr-1.5 py-1 text-xs border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-primary"
                        @input="markMonthChanged(account.accountId)"
                      />
                    </div>
                    <div class="relative">
                      <label class="block text-xs text-gray-600 mb-0.5">取出</label>
                      <span class="absolute left-1.5 top-[20px] text-gray-500 text-xs">{{ getCurrencySymbol(account.currency) }}</span>
                      <input
                        v-model="monthAmounts[account.accountId].withdrawals"
                        type="number"
                        step="0.01"
                        min="0"
                        placeholder="0.00"
                        class="w-full pl-5 pr-1.5 py-1 text-xs border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-primary"
                        @input="markMonthChanged(account.accountId)"
                      />
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 统计 -->
          <div v-if="!loadingMonth && monthAccounts.length > 0" class="px-3 py-2 border-t border-gray-200 bg-gray-50">
            <div class="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <div>
                <div class="text-xs text-gray-600 mb-0.5">本月总投入</div>
                <div class="text-base font-bold text-green-600">${{ formatCurrency(monthSummary.totalDeposits) }}</div>
              </div>
              <div>
                <div class="text-xs text-gray-600 mb-0.5">本月总取出</div>
                <div class="text-base font-bold text-red-600">${{ formatCurrency(monthSummary.totalWithdrawals) }}</div>
              </div>
              <div>
                <div class="text-xs text-gray-600 mb-0.5">净投入</div>
                <div class="text-base font-bold text-primary">${{ formatCurrency(monthSummary.netInvestment) }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 按账户录入模式 -->
    <div v-if="activeTab === 'by-account'" class="space-y-2">
      <!-- 选择器和保存按钮 -->
      <div class="flex flex-col sm:flex-row gap-2 items-start sm:items-center sm:justify-between">
        <div class="flex flex-col sm:flex-row gap-2">
          <div class="flex items-center gap-2">
            <label class="text-xs font-medium text-gray-700 whitespace-nowrap">年份:</label>
            <select
              v-model.number="selectedYear"
              @change="loadYearData"
              class="px-2 py-1 text-xs border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-primary"
            >
              <option v-for="year in yearOptions" :key="year" :value="year">
                {{ year }}
              </option>
            </select>
          </div>
          <div class="flex items-center gap-2">
            <label class="text-xs font-medium text-gray-700 whitespace-nowrap">账户:</label>
            <select
              v-model="selectedAccountId"
              @change="loadYearData"
              class="px-2 py-1 text-xs border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-primary min-w-[200px]"
            >
              <option v-if="yearAccounts.length === 0" value="">请先选择家庭</option>
              <option v-for="account in yearAccounts" :key="account.accountId" :value="account.accountId">
                {{ account.categoryIcon }} {{ account.accountName }} ({{ account.userName }})
              </option>
            </select>
          </div>
        </div>

        <button
          @click="saveYearData"
          :disabled="savingYear || !hasYearChanges || !selectedAccountId"
          class="px-3 py-1.5 bg-primary text-white rounded text-xs font-medium disabled:opacity-50 disabled:cursor-not-allowed whitespace-nowrap w-full sm:w-auto"
        >
          {{ savingYear ? '保存中...' : '保存全部' }}
        </button>
      </div>

      <!-- 12个月的录入表格 -->
      <div class="bg-white rounded-lg shadow border border-gray-200">
        <div v-if="loadingYear" class="text-center py-6 text-gray-500 text-xs">加载中...</div>
        <div v-else-if="!selectedAccountId" class="text-center py-6 text-gray-500 text-xs">请选择账户</div>
        <div v-else>
          <div class="overflow-x-auto">
            <table class="w-full">
              <thead class="bg-gray-50">
                <tr>
                  <th class="px-2 py-1.5 text-left text-xs font-medium text-gray-700">月份</th>
                  <th class="px-2 py-1.5 text-right text-xs font-medium text-gray-700">投入 ({{ selectedAccountCurrency }})</th>
                  <th class="px-2 py-1.5 text-right text-xs font-medium text-gray-700">取出 ({{ selectedAccountCurrency }})</th>
                  <th class="px-2 py-1.5 text-right text-xs font-medium text-gray-700">净额</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-200">
                <tr v-for="month in 12" :key="month" class="hover:bg-gray-50">
                  <td class="px-2 py-1.5 text-xs font-medium text-gray-900">
                    {{ selectedYear }}年{{ month }}月
                  </td>
                  <td class="px-2 py-1.5 text-right">
                    <div class="relative inline-block w-24">
                      <span class="absolute left-1.5 top-1 text-gray-500 text-xs">{{ getCurrencySymbol(selectedAccountCurrency) }}</span>
                      <input
                        v-model="yearMonthAmounts[month].deposits"
                        type="number"
                        step="0.01"
                        min="0"
                        placeholder="0.00"
                        class="w-full pl-5 pr-1.5 py-1 text-xs border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-primary text-right"
                        @input="markYearChanged(month)"
                      />
                    </div>
                  </td>
                  <td class="px-2 py-1.5 text-right">
                    <div class="relative inline-block w-24">
                      <span class="absolute left-1.5 top-1 text-gray-500 text-xs">{{ getCurrencySymbol(selectedAccountCurrency) }}</span>
                      <input
                        v-model="yearMonthAmounts[month].withdrawals"
                        type="number"
                        step="0.01"
                        min="0"
                        placeholder="0.00"
                        class="w-full pl-5 pr-1.5 py-1 text-xs border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-primary text-right"
                        @input="markYearChanged(month)"
                      />
                    </div>
                  </td>
                  <td class="px-2 py-1.5 text-right text-xs">
                    <span :class="getNetAmountClass(month)">
                      {{ getCurrencySymbol(selectedAccountCurrency) }}{{ formatAmount(getNetAmount(month)) }}
                    </span>
                  </td>
                </tr>
              </tbody>
              <tfoot class="bg-gray-50 border-t-2 border-gray-300">
                <tr>
                  <td class="px-2 py-1.5 text-xs font-bold text-gray-900">合计</td>
                  <td class="px-2 py-1.5 text-right text-xs font-bold text-green-600">
                    {{ getCurrencySymbol(selectedAccountCurrency) }}{{ formatAmount(yearSummary.totalDeposits) }}
                  </td>
                  <td class="px-2 py-1.5 text-right text-xs font-bold text-red-600">
                    {{ getCurrencySymbol(selectedAccountCurrency) }}{{ formatAmount(yearSummary.totalWithdrawals) }}
                  </td>
                  <td class="px-2 py-1.5 text-right text-xs font-bold text-primary">
                    {{ getCurrencySymbol(selectedAccountCurrency) }}{{ formatAmount(yearSummary.netInvestment) }}
                  </td>
                </tr>
              </tfoot>
            </table>
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
    // Tab状态
    const activeTab = ref('by-month')

    // 通用数据
    const families = ref([])

    // 按月份模式的数据
    const selectedFamilyId = ref(null)
    const transactionPeriod = ref('')
    const monthAccounts = ref([])
    const monthAmounts = ref({})
    const monthHistoryData = ref({})
    const changedMonthAccounts = ref(new Set())
    const loadingMonth = ref(false)
    const savingMonth = ref(false)

    // 按账户模式的数据
    const selectedFamilyIdYear = ref(null)
    const selectedYear = ref(new Date().getFullYear())
    const selectedAccountId = ref('')
    const yearAccounts = ref([])
    const yearMonthAmounts = ref({})
    const changedYearMonths = ref(new Set())
    const loadingYear = ref(false)
    const savingYear = ref(false)

    // 初始化12个月的数据结构
    for (let i = 1; i <= 12; i++) {
      yearMonthAmounts.value[i] = { deposits: 0, withdrawals: 0 }
    }

    // 计算属性 - 按月份模式
    const hasMonthChanges = computed(() => changedMonthAccounts.value.size > 0)

    const monthSummary = computed(() => {
      let totalDeposits = 0
      let totalWithdrawals = 0

      monthAccounts.value.forEach(account => {
        const amounts = monthAmounts.value[account.accountId]
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

    const currentMonth = computed(() => {
      if (!transactionPeriod.value) return ''
      const [year, month] = transactionPeriod.value.split('-')
      return `${year}年${month}月`
    })

    const previousMonth1 = computed(() => getPreviousMonth(transactionPeriod.value, 1))
    const previousMonth2 = computed(() => getPreviousMonth(transactionPeriod.value, 2))
    const previousMonth3 = computed(() => getPreviousMonth(transactionPeriod.value, 3))

    // 计算属性 - 按账户模式
    const hasYearChanges = computed(() => changedYearMonths.value.size > 0)

    const yearOptions = computed(() => {
      const currentYear = new Date().getFullYear()
      const years = []
      // 从2020年到明年，提供足够的年份选择
      for (let year = 2020; year <= currentYear + 1; year++) {
        years.push(year)
      }
      return years.reverse() // 倒序，最新年份在前
    })

    const selectedAccountCurrency = computed(() => {
      if (!selectedAccountId.value) return 'USD'
      const account = yearAccounts.value.find(a => a.accountId === selectedAccountId.value)
      return account?.currency || 'USD'
    })

    const yearSummary = computed(() => {
      let totalDeposits = 0
      let totalWithdrawals = 0

      for (let i = 1; i <= 12; i++) {
        totalDeposits += parseFloat(yearMonthAmounts.value[i].deposits) || 0
        totalWithdrawals += parseFloat(yearMonthAmounts.value[i].withdrawals) || 0
      }

      return {
        totalDeposits,
        totalWithdrawals,
        netInvestment: totalDeposits - totalWithdrawals
      }
    })

    // 工具函数
    const formatCurrency = (value) => {
      if (!value) return '0'
      const num = parseFloat(value)
      if (num >= 1000) {
        return (num / 1000).toFixed(1) + 'K'
      }
      return num.toFixed(0)
    }

    const formatAmount = (value) => {
      if (!value) return '0.00'
      return parseFloat(value).toFixed(2)
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

    const getNetAmount = (month) => {
      const deposits = parseFloat(yearMonthAmounts.value[month].deposits) || 0
      const withdrawals = parseFloat(yearMonthAmounts.value[month].withdrawals) || 0
      return deposits - withdrawals
    }

    const getNetAmountClass = (month) => {
      const net = getNetAmount(month)
      if (net > 0) return 'text-green-600 font-medium'
      if (net < 0) return 'text-red-600 font-medium'
      return 'text-gray-600'
    }

    // 标记变更
    const markMonthChanged = (accountId) => {
      changedMonthAccounts.value.add(accountId)
    }

    const markYearChanged = (month) => {
      changedYearMonths.value.add(month)
    }

    // 加载家庭列表
    const loadFamilies = async () => {
      try {
        const response = await familyAPI.getAll()
        if (response.success) {
          families.value = response.data
          if (families.value.length > 0) {
            selectedFamilyId.value = families.value[0].id
            selectedFamilyIdYear.value = families.value[0].id
          }
        }
      } catch (error) {
        console.error('加载家庭列表失败:', error)
      }
    }

    // 按月份模式 - 加载数据
    const loadMonthData = async () => {
      if (!selectedFamilyId.value || !transactionPeriod.value) return

      loadingMonth.value = true
      try {
        const accountsResponse = await investmentAccountAPI.getAll(selectedFamilyId.value)
        if (accountsResponse.success) {
          monthAccounts.value = accountsResponse.data

          monthAmounts.value = {}
          monthAccounts.value.forEach(account => {
            monthAmounts.value[account.accountId] = { deposits: 0, withdrawals: 0 }
          })

          await loadCurrentMonthTransactions()
          await loadMonthHistoryData()
        }
      } catch (error) {
        console.error('加载数据失败:', error)
      } finally {
        loadingMonth.value = false
      }
    }

    const loadCurrentMonthTransactions = async () => {
      for (const account of monthAccounts.value) {
        try {
          const response = await investmentTransactionAPI.getByAccount(
            account.accountId,
            transactionPeriod.value,
            transactionPeriod.value
          )
          if (response.success && response.data.length > 0) {
            response.data.forEach(tx => {
              if (tx.transactionType === 'DEPOSIT') {
                monthAmounts.value[account.accountId].deposits = tx.amount
              } else if (tx.transactionType === 'WITHDRAWAL') {
                monthAmounts.value[account.accountId].withdrawals = tx.amount
              }
            })
          }
        } catch (error) {
          console.error(`加载账户${account.accountId}交易记录失败:`, error)
        }
      }
    }

    const loadMonthHistoryData = async () => {
      const [year, month] = transactionPeriod.value.split('-').map(Number)

      for (const account of monthAccounts.value) {
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

        monthHistoryData.value[account.accountId] = history
      }
    }

    const saveMonthData = async () => {
      if (!hasMonthChanges.value) return

      savingMonth.value = true
      try {
        const transactions = monthAccounts.value.map(account => ({
          accountId: account.accountId,
          deposits: parseFloat(monthAmounts.value[account.accountId].deposits) || null,
          withdrawals: parseFloat(monthAmounts.value[account.accountId].withdrawals) || null,
          description: `${transactionPeriod.value} 批量录入`
        }))

        const response = await investmentTransactionAPI.batchSave({
          familyId: selectedFamilyId.value,
          transactionPeriod: transactionPeriod.value,
          transactions
        })

        if (response.success) {
          changedMonthAccounts.value.clear()
          alert(`保存成功！创建: ${response.data.created}, 更新: ${response.data.updated}, 删除: ${response.data.deleted}`)
        } else {
          alert(response.message || '保存失败')
        }
      } catch (error) {
        console.error('保存失败:', error)
        alert('保存失败：' + error.message)
      } finally {
        savingMonth.value = false
      }
    }

    // 按账户模式 - 加载账户列表
    const loadYearAccounts = async () => {
      if (!selectedFamilyIdYear.value) return

      try {
        const response = await investmentAccountAPI.getAll(selectedFamilyIdYear.value)
        if (response.success) {
          yearAccounts.value = response.data
          if (yearAccounts.value.length > 0 && !selectedAccountId.value) {
            selectedAccountId.value = yearAccounts.value[0].accountId
          }
        }
      } catch (error) {
        console.error('加载账户列表失败:', error)
      }
    }

    // 按账户模式 - 加载年度数据
    const loadYearData = async () => {
      if (!selectedAccountId.value || !selectedYear.value) return

      loadingYear.value = true
      try {
        // 重置数据
        for (let i = 1; i <= 12; i++) {
          yearMonthAmounts.value[i] = { deposits: 0, withdrawals: 0 }
        }

        // 加载12个月的交易记录
        for (let month = 1; month <= 12; month++) {
          const period = `${selectedYear.value}-${String(month).padStart(2, '0')}`

          try {
            const response = await investmentTransactionAPI.getByAccount(
              selectedAccountId.value,
              period,
              period
            )
            if (response.success && response.data.length > 0) {
              response.data.forEach(tx => {
                if (tx.transactionType === 'DEPOSIT') {
                  yearMonthAmounts.value[month].deposits = tx.amount
                } else if (tx.transactionType === 'WITHDRAWAL') {
                  yearMonthAmounts.value[month].withdrawals = tx.amount
                }
              })
            }
          } catch (error) {
            console.error(`加载${month}月交易记录失败:`, error)
          }
        }
      } catch (error) {
        console.error('加载年度数据失败:', error)
      } finally {
        loadingYear.value = false
      }
    }

    const saveYearData = async () => {
      if (!hasYearChanges.value || !selectedAccountId.value) return

      savingYear.value = true
      try {
        let totalCreated = 0
        let totalUpdated = 0
        let totalDeleted = 0

        // 逐月保存
        for (let month = 1; month <= 12; month++) {
          const period = `${selectedYear.value}-${String(month).padStart(2, '0')}`

          const transactions = [{
            accountId: selectedAccountId.value,
            deposits: parseFloat(yearMonthAmounts.value[month].deposits) || null,
            withdrawals: parseFloat(yearMonthAmounts.value[month].withdrawals) || null,
            description: `${period} 批量录入`
          }]

          const response = await investmentTransactionAPI.batchSave({
            familyId: selectedFamilyIdYear.value,
            transactionPeriod: period,
            transactions
          })

          if (response.success) {
            totalCreated += response.data.created
            totalUpdated += response.data.updated
            totalDeleted += response.data.deleted
          }
        }

        changedYearMonths.value.clear()
        alert(`保存成功！创建: ${totalCreated}, 更新: ${totalUpdated}, 删除: ${totalDeleted}`)
      } catch (error) {
        console.error('保存失败:', error)
        alert('保存失败：' + error.message)
      } finally {
        savingYear.value = false
      }
    }

    // 初始化
    onMounted(() => {
      loadFamilies()
      const now = new Date()
      transactionPeriod.value = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
    })

    // 监听变化
    watch([selectedFamilyId, transactionPeriod], () => {
      if (activeTab.value === 'by-month') {
        changedMonthAccounts.value.clear()
        loadMonthData()
      }
    })

    watch(selectedFamilyIdYear, () => {
      loadYearAccounts()
    })

    watch([selectedAccountId, selectedYear], () => {
      if (activeTab.value === 'by-account') {
        changedYearMonths.value.clear()
        loadYearData()
      }
    })

    watch(activeTab, (newTab) => {
      if (newTab === 'by-month' && selectedFamilyId.value && transactionPeriod.value) {
        loadMonthData()
      } else if (newTab === 'by-account' && selectedFamilyIdYear.value) {
        loadYearAccounts().then(() => {
          if (selectedAccountId.value) {
            loadYearData()
          }
        })
      }
    })

    return {
      // Tab
      activeTab,

      // 通用
      families,
      formatCurrency,
      formatAmount,
      getCurrencySymbol,

      // 按月份
      selectedFamilyId,
      transactionPeriod,
      monthAccounts,
      monthAmounts,
      monthHistoryData,
      loadingMonth,
      savingMonth,
      hasMonthChanges,
      monthSummary,
      currentMonth,
      previousMonth1,
      previousMonth2,
      previousMonth3,
      markMonthChanged,
      loadMonthData,
      saveMonthData,

      // 按账户
      selectedFamilyIdYear,
      selectedYear,
      yearOptions,
      selectedAccountId,
      yearAccounts,
      yearMonthAmounts,
      loadingYear,
      savingYear,
      hasYearChanges,
      selectedAccountCurrency,
      yearSummary,
      getNetAmount,
      getNetAmountClass,
      markYearChanged,
      loadYearAccounts,
      loadYearData,
      saveYearData
    }
  }
}
</script>
