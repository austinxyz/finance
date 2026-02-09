<template>
  <div class="p-3 md:p-4 space-y-3">
    <!-- Calculator Component -->
    <Calculator :show="showCalculator" @close="closeCalculator" @apply="applyCalculatorResult" />
    <!-- Tab 切换 -->
    <div class="border-b border-gray-200 pb-2">
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
    </div>

    <!-- 按月份录入模式 -->
    <div v-if="activeTab === 'by-month'" class="space-y-2">
      <!-- 选择器和保存按钮 -->
      <div class="flex flex-col sm:flex-row gap-2 items-start sm:items-center sm:justify-between">
        <div class="flex flex-col sm:flex-row gap-2">
          <div class="flex items-center gap-2">
            <label class="text-xs font-medium text-gray-700 whitespace-nowrap">月份:</label>
            <input
              v-model="transactionPeriod"
              type="month"
              @change="loadMonthData"
              class="px-2 py-1 text-xs border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-primary"
            />
          </div>
          <div class="flex items-center gap-2">
            <label class="text-xs font-medium text-gray-700 whitespace-nowrap">分类:</label>
            <select
              v-model="selectedMonthCategory"
              @change="filterMonthAccounts"
              class="px-2 py-1 text-xs border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-primary"
            >
              <option value="">全部</option>
              <option v-for="category in investmentCategories" :key="category.categoryId" :value="category.categoryId">
                {{ category.categoryIcon }} {{ category.categoryName }}
              </option>
            </select>
          </div>
        </div>

        <button
          @click="saveMonthData"
          :disabled="savingMonth || !hasMonthChanges"
          class="px-3 py-1.5 bg-primary text-white rounded text-xs font-medium disabled:opacity-50 disabled:cursor-not-allowed whitespace-nowrap w-full sm:w-auto"
        >
          {{ savingMonth ? '保存中...' : '保存全部' }}
        </button>
      </div>

      <!-- 顶部统计 - 固定不滚动 -->
      <div v-if="!loadingMonth && monthAccounts.length > 0" class="bg-white rounded-lg shadow border border-gray-200 px-3 py-2">
        <div class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
          <!-- 前三个月净投入 -->
          <div>
            <div class="text-xs text-gray-600 mb-0.5">{{ previousMonth3 }}净投入</div>
            <div class="text-sm font-medium text-gray-700">${{ formatCurrency(historyMonthNet3) }}</div>
          </div>
          <div>
            <div class="text-xs text-gray-600 mb-0.5">{{ previousMonth2 }}净投入</div>
            <div class="text-sm font-medium text-gray-700">${{ formatCurrency(historyMonthNet2) }}</div>
          </div>
          <div>
            <div class="text-xs text-gray-600 mb-0.5">{{ previousMonth1 }}净投入</div>
            <div class="text-sm font-semibold text-gray-900">${{ formatCurrency(historyMonthNet1) }}</div>
          </div>

          <!-- 本月数据 -->
          <div>
            <div class="text-xs text-gray-600 mb-0.5">本月总投入</div>
            <div class="text-base font-bold text-green-600">${{ formatCurrency(monthSummary.totalDeposits) }}</div>
          </div>
          <div>
            <div class="text-xs text-gray-600 mb-0.5">本月总取出</div>
            <div class="text-base font-bold text-red-600">${{ formatCurrency(monthSummary.totalWithdrawals) }}</div>
          </div>
          <div>
            <div class="text-xs text-gray-600 mb-0.5">本月净投入</div>
            <div class="text-base font-bold text-primary">${{ formatCurrency(monthSummary.netInvestment) }}</div>
          </div>
        </div>
      </div>

      <!-- 按月份的账户列表 - 表格布局（带滚动条） -->
      <div class="bg-white rounded-lg shadow border border-gray-200">
        <div v-if="loadingMonth" class="text-center py-6 text-gray-500 text-xs">加载中...</div>
        <div v-else-if="filteredMonthAccounts.length === 0" class="text-center py-6 text-gray-500 text-xs">暂无投资账户</div>
        <div v-else>
          <div class="overflow-x-auto max-h-[calc(100vh-280px)] overflow-y-auto">
            <table class="w-full">
              <thead class="bg-gray-50 sticky top-0">
                <tr>
                  <th class="px-2 py-1.5 text-left text-xs font-medium text-gray-700">账户</th>
                  <th class="px-2 py-1.5 text-right text-xs font-medium text-gray-700">{{ previousMonth3 }}</th>
                  <th class="px-2 py-1.5 text-right text-xs font-medium text-gray-700">{{ previousMonth2 }}</th>
                  <th class="px-2 py-1.5 text-right text-xs font-medium text-gray-700">{{ previousMonth1 }}</th>
                  <th class="px-2 py-1.5 text-center text-xs font-medium text-gray-700">投入</th>
                  <th class="px-2 py-1.5 text-center text-xs font-medium text-gray-700">取出</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-200">
                <tr v-for="account in filteredMonthAccounts" :key="account.accountId" class="hover:bg-gray-50">
                  <!-- 账户信息 -->
                  <td class="px-2 py-1.5">
                    <div class="flex items-center gap-1.5">
                      <span class="text-base">{{ account.categoryIcon }}</span>
                      <div class="text-xs truncate">
                        <span class="font-medium text-gray-900">{{ account.accountName }}</span>
                        <span class="text-gray-500 ml-1.5">{{ account.userName }}</span>
                      </div>
                    </div>
                  </td>

                  <!-- 历史数据 -->
                  <td class="px-2 py-1.5 text-right text-xs text-gray-600 whitespace-nowrap">
                    {{ getCurrencySymbol(account.currency) }}{{ formatCurrency(monthHistoryData[account.accountId]?.month3 || 0) }}
                  </td>
                  <td class="px-2 py-1.5 text-right text-xs text-gray-600 whitespace-nowrap">
                    {{ getCurrencySymbol(account.currency) }}{{ formatCurrency(monthHistoryData[account.accountId]?.month2 || 0) }}
                  </td>
                  <td class="px-2 py-1.5 text-right text-xs text-gray-700 font-medium whitespace-nowrap">
                    {{ getCurrencySymbol(account.currency) }}{{ formatCurrency(monthHistoryData[account.accountId]?.month1 || 0) }}
                  </td>

                  <!-- 本月输入 - 投入 -->
                  <td class="px-2 py-1.5">
                    <div class="flex items-center gap-1">
                      <div class="relative flex-1">
                        <span class="absolute left-1.5 top-1/2 -translate-y-1/2 text-gray-500 text-xs">{{ getCurrencySymbol(account.currency) }}</span>
                        <input
                          v-model="monthAmounts[account.accountId].deposits"
                          type="number"
                          step="0.01"
                          min="0"
                          placeholder="0.00"
                          class="w-24 pl-5 pr-1.5 py-1 text-xs border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-primary text-right"
                          @input="markMonthChanged(account.accountId)"
                        />
                      </div>
                      <button
                        @click="openCalculator(account.accountId, 'deposits')"
                        class="p-1 text-gray-500 hover:text-primary transition"
                        title="打开计算器"
                      >
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 7h6m0 10v-3m-3 3h.01M9 17h.01M9 14h.01M12 14h.01M15 11h.01M12 11h.01M9 11h.01M7 21h10a2 2 0 002-2V5a2 2 0 00-2-2H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
                        </svg>
                      </button>
                    </div>
                  </td>

                  <!-- 本月输入 - 取出 -->
                  <td class="px-2 py-1.5">
                    <div class="flex items-center gap-1">
                      <div class="relative flex-1">
                        <span class="absolute left-1.5 top-1/2 -translate-y-1/2 text-gray-500 text-xs">{{ getCurrencySymbol(account.currency) }}</span>
                        <input
                          v-model="monthAmounts[account.accountId].withdrawals"
                          type="number"
                          step="0.01"
                          min="0"
                          placeholder="0.00"
                          class="w-24 pl-5 pr-1.5 py-1 text-xs border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-primary text-right"
                          @input="markMonthChanged(account.accountId)"
                        />
                      </div>
                      <button
                        @click="openCalculator(account.accountId, 'withdrawals')"
                        class="p-1 text-gray-500 hover:text-primary transition"
                        title="打开计算器"
                      >
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 7h6m0 10v-3m-3 3h.01M9 17h.01M9 14h.01M12 14h.01M15 11h.01M12 11h.01M9 11h.01M7 21h10a2 2 0 002-2V5a2 2 0 00-2-2H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
                        </svg>
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
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

      <!-- 快速填充 -->
      <div v-if="selectedAccountId" class="bg-blue-50 border border-blue-200 rounded p-2">
        <div class="flex flex-col sm:flex-row gap-2 items-start sm:items-center">
          <span class="text-xs font-medium text-blue-900">快速填充（定投）:</span>
          <div class="flex items-center gap-2">
            <div class="relative">
              <span class="absolute left-1.5 top-1/2 -translate-y-1/2 text-gray-500 text-xs">{{ getCurrencySymbol(selectedAccountCurrency) }}</span>
              <input
                v-model.number="quickFillDeposit"
                type="number"
                step="0.01"
                min="0"
                placeholder="投入金额"
                class="w-24 pl-5 pr-1.5 py-1 text-xs border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-primary text-right"
              />
            </div>
            <button
              @click="fillAllDeposits"
              :disabled="!quickFillDeposit"
              class="px-2 py-1 bg-blue-600 text-white rounded text-xs font-medium disabled:opacity-50 disabled:cursor-not-allowed whitespace-nowrap"
            >
              填充投入
            </button>
          </div>
          <div class="flex items-center gap-2">
            <div class="relative">
              <span class="absolute left-1.5 top-1/2 -translate-y-1/2 text-gray-500 text-xs">{{ getCurrencySymbol(selectedAccountCurrency) }}</span>
              <input
                v-model.number="quickFillWithdrawal"
                type="number"
                step="0.01"
                min="0"
                placeholder="取出金额"
                class="w-24 pl-5 pr-1.5 py-1 text-xs border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-primary text-right"
              />
            </div>
            <button
              @click="fillAllWithdrawals"
              :disabled="!quickFillWithdrawal"
              class="px-2 py-1 bg-blue-600 text-white rounded text-xs font-medium disabled:opacity-50 disabled:cursor-not-allowed whitespace-nowrap"
            >
              填充取出
            </button>
          </div>
        </div>
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
                    <div class="flex items-center gap-1 justify-end">
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
                      <button
                        @click="openCalculator(month, 'year-deposits')"
                        class="p-1 text-gray-500 hover:text-primary transition"
                        title="打开计算器"
                      >
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 7h6m0 10v-3m-3 3h.01M9 17h.01M9 14h.01M12 14h.01M15 11h.01M12 11h.01M9 11h.01M7 21h10a2 2 0 002-2V5a2 2 0 00-2-2H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
                        </svg>
                      </button>
                    </div>
                  </td>
                  <td class="px-2 py-1.5 text-right">
                    <div class="flex items-center gap-1 justify-end">
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
                      <button
                        @click="openCalculator(month, 'year-withdrawals')"
                        class="p-1 text-gray-500 hover:text-primary transition"
                        title="打开计算器"
                      >
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 7h6m0 10v-3m-3 3h.01M9 17h.01M9 14h.01M12 14h.01M15 11h.01M12 11h.01M9 11h.01M7 21h10a2 2 0 002-2V5a2 2 0 00-2-2H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
                        </svg>
                      </button>
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
import { useFamilyStore } from '@/stores/family'
import Calculator from '@/components/Calculator.vue'

export default {
  name: 'InvestmentBatchEntry',
  setup() {
    // Family store
    const familyStore = useFamilyStore()
    const selectedFamilyId = computed(() => familyStore.currentFamilyId)

    // Tab状态
    const activeTab = ref('by-account')

    // 按月份模式的数据
    const transactionPeriod = ref('')
    const selectedMonthCategory = ref('')
    const investmentCategories = ref([])
    const monthAccounts = ref([])
    const monthAmounts = ref({})
    const monthHistoryData = ref({})
    const changedMonthAccounts = ref(new Set())
    const loadingMonth = ref(false)
    const savingMonth = ref(false)

    // 按账户模式的数据
    const selectedYear = ref(new Date().getFullYear())
    const selectedAccountId = ref('')
    const yearAccounts = ref([])
    const yearMonthAmounts = ref({})
    const changedYearMonths = ref(new Set())
    const loadingYear = ref(false)
    const savingYear = ref(false)
    const quickFillDeposit = ref(null)
    const quickFillWithdrawal = ref(null)

    // Calculator state
    const showCalculator = ref(false)
    const currentCalculatorTarget = ref(null) // { id, type } where type is 'deposits', 'withdrawals', 'year-deposits', 'year-withdrawals'

    // 初始化12个月的数据结构
    for (let i = 1; i <= 12; i++) {
      yearMonthAmounts.value[i] = { deposits: 0, withdrawals: 0 }
    }

    // 计算属性 - 按月份模式
    const hasMonthChanges = computed(() => changedMonthAccounts.value.size > 0)

    const filteredMonthAccounts = computed(() => {
      let accounts = monthAccounts.value

      // 按分类过滤
      if (selectedMonthCategory.value) {
        accounts = accounts.filter(account =>
          account.categoryId === selectedMonthCategory.value
        )
      }

      // 按分类ID排序
      return accounts.sort((a, b) => {
        if (a.categoryId === b.categoryId) {
          // 同一分类内按账户名排序
          return a.accountName.localeCompare(b.accountName)
        }
        return (a.categoryId || 0) - (b.categoryId || 0)
      })
    })

    const monthSummary = computed(() => {
      let totalDeposits = 0
      let totalWithdrawals = 0

      filteredMonthAccounts.value.forEach(account => {
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

    // 前三个月的净投入统计
    const historyMonthNet3 = computed(() => {
      return filteredMonthAccounts.value.reduce((sum, account) => {
        const deposits = monthHistoryData.value[account.accountId]?.month3 || 0
        const withdrawals = monthHistoryData.value[account.accountId]?.month3Withdrawals || 0
        return sum + (deposits - withdrawals)
      }, 0)
    })

    const historyMonthNet2 = computed(() => {
      return filteredMonthAccounts.value.reduce((sum, account) => {
        const deposits = monthHistoryData.value[account.accountId]?.month2 || 0
        const withdrawals = monthHistoryData.value[account.accountId]?.month2Withdrawals || 0
        return sum + (deposits - withdrawals)
      }, 0)
    })

    const historyMonthNet1 = computed(() => {
      return filteredMonthAccounts.value.reduce((sum, account) => {
        const deposits = monthHistoryData.value[account.accountId]?.month1 || 0
        const withdrawals = monthHistoryData.value[account.accountId]?.month1Withdrawals || 0
        return sum + (deposits - withdrawals)
      }, 0)
    })

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

    // Calculator functions
    const openCalculator = (id, type) => {
      currentCalculatorTarget.value = { id, type }
      showCalculator.value = true
    }

    const closeCalculator = () => {
      showCalculator.value = false
      currentCalculatorTarget.value = null
    }

    const applyCalculatorResult = (result) => {
      if (!currentCalculatorTarget.value) return

      const { id, type } = currentCalculatorTarget.value

      if (type === 'deposits') {
        // 按月份模式 - 投入
        monthAmounts.value[id].deposits = result.toString()
        markMonthChanged(id)
      } else if (type === 'withdrawals') {
        // 按月份模式 - 取出
        monthAmounts.value[id].withdrawals = result.toString()
        markMonthChanged(id)
      } else if (type === 'year-deposits') {
        // 按账户模式 - 投入
        yearMonthAmounts.value[id].deposits = result.toString()
        markYearChanged(id)
      } else if (type === 'year-withdrawals') {
        // 按账户模式 - 取出
        yearMonthAmounts.value[id].withdrawals = result.toString()
        markYearChanged(id)
      }
    }

    // 加载投资分类
    const loadInvestmentCategories = async () => {
      try {
        const response = await investmentAccountAPI.getCategories()
        if (response.success) {
          investmentCategories.value = response.data
        }
      } catch (error) {
        console.error('加载投资分类失败:', error)
      }
    }

    // 过滤账户（分类选择变化时）
    const filterMonthAccounts = () => {
      // 过滤通过computed属性filteredMonthAccounts实现，这里不需要额外操作
    }


    // 按月份模式 - 加载数据
    const loadMonthData = async () => {
      if (!selectedFamilyId.value || !transactionPeriod.value) {
        console.warn('[InvestmentBatchEntry] loadMonthData - No family or period', {
          familyId: selectedFamilyId.value,
          period: transactionPeriod.value
        })
        return
      }

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
        const history = {
          month1: 0, month2: 0, month3: 0,
          month1Withdrawals: 0, month2Withdrawals: 0, month3Withdrawals: 0
        }

        for (let i = 1; i <= 3; i++) {
          const date = new Date(year, month - 1 - i, 1)
          const period = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`

          try {
            const response = await investmentTransactionAPI.getByAccount(account.accountId, period, period)
            if (response.success && response.data.length > 0) {
              const deposits = response.data
                .filter(tx => tx.transactionType === 'DEPOSIT')
                .reduce((sum, tx) => sum + parseFloat(tx.amount), 0)
              const withdrawals = response.data
                .filter(tx => tx.transactionType === 'WITHDRAWAL')
                .reduce((sum, tx) => sum + parseFloat(tx.amount), 0)
              history[`month${i}`] = deposits
              history[`month${i}Withdrawals`] = withdrawals
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
      if (!selectedFamilyId.value) {
        console.warn('[InvestmentBatchEntry] loadYearAccounts - No family selected')
        return
      }

      try {
        const response = await investmentAccountAPI.getAll(selectedFamilyId.value)
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
            familyId: selectedFamilyId.value,
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

    // 快速填充方法
    const fillAllDeposits = () => {
      if (!quickFillDeposit.value) return

      for (let month = 1; month <= 12; month++) {
        yearMonthAmounts.value[month].deposits = quickFillDeposit.value
        markYearChanged(month)
      }
    }

    const fillAllWithdrawals = () => {
      if (!quickFillWithdrawal.value) return

      for (let month = 1; month <= 12; month++) {
        yearMonthAmounts.value[month].withdrawals = quickFillWithdrawal.value
        markYearChanged(month)
      }
    }

    // 初始化
    onMounted(async () => {
      await loadInvestmentCategories()
      const now = new Date()
      transactionPeriod.value = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`

      // Load data if family is already available
      if (selectedFamilyId.value) {
        if (activeTab.value === 'by-month') {
          await loadMonthData()
        } else {
          await loadYearAccounts()
          if (selectedAccountId.value) {
            await loadYearData()
          }
        }
      } else {
        console.warn('[InvestmentBatchEntry] No family ID available on mount, waiting for family store')
      }
    })

    // 监听家庭切换（管理员切换家庭时自动刷新）
    watch(selectedFamilyId, (newFamilyId) => {
      if (newFamilyId) {
        if (activeTab.value === 'by-month') {
          changedMonthAccounts.value.clear()
          loadMonthData()
        } else {
          loadYearAccounts()
        }
      }
    })

    // 监听按月份模式的时间段变化
    watch(transactionPeriod, () => {
      if (activeTab.value === 'by-month' && selectedFamilyId.value) {
        changedMonthAccounts.value.clear()
        loadMonthData()
      }
    })

    // 监听按账户模式的账户和年份变化
    watch([selectedAccountId, selectedYear], () => {
      if (activeTab.value === 'by-account' && selectedAccountId.value) {
        changedYearMonths.value.clear()
        loadYearData()
      }
    })

    // 监听Tab切换
    watch(activeTab, (newTab) => {
      if (!selectedFamilyId.value) return

      if (newTab === 'by-month' && transactionPeriod.value) {
        loadMonthData()
      } else if (newTab === 'by-account') {
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
      formatCurrency,
      formatAmount,
      getCurrencySymbol,

      // 按月份
      selectedFamilyId,
      transactionPeriod,
      selectedMonthCategory,
      investmentCategories,
      monthAccounts,
      filteredMonthAccounts,
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
      historyMonthNet1,
      historyMonthNet2,
      historyMonthNet3,
      markMonthChanged,
      loadMonthData,
      saveMonthData,
      filterMonthAccounts,

      // 按账户
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
      saveYearData,
      quickFillDeposit,
      quickFillWithdrawal,
      fillAllDeposits,
      fillAllWithdrawals,

      // Calculator
      showCalculator,
      openCalculator,
      closeCalculator,
      applyCalculatorResult
    }
  },
  components: {
    Calculator
  }
}
</script>
