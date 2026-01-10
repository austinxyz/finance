<template>
  <div class="p-3 md:p-6 space-y-4 md:space-y-6">
    <!-- 家庭选择器和投资大类 Tab 在同一行 -->
    <div class="flex items-center justify-between gap-4 border-b border-gray-200">
      <nav class="-mb-px flex space-x-2 md:space-x-4 overflow-x-auto flex-1" aria-label="Tabs">
        <button
          v-for="category in investmentCategories"
          :key="category.categoryId"
          @click="selectedCategory = category"
          :class="[
            'whitespace-nowrap py-3 px-4 border-b-2 font-medium text-sm transition-colors flex items-center gap-2',
            selectedCategory?.categoryId === category.categoryId
              ? 'border-primary text-primary'
              : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
          ]"
        >
          <span class="text-base">{{ category.categoryIcon }}</span>
          <span>{{ category.categoryName }}</span>
        </button>
      </nav>

      <div class="flex items-center gap-2 flex-shrink-0 pb-3">
        <label class="text-sm font-medium text-gray-700 whitespace-nowrap">家庭:</label>
        <select
          v-model="selectedFamilyId"
          @change="onFamilyChange"
          class="px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-primary bg-white text-sm"
        >
          <option v-for="family in families" :key="family.id" :value="family.id">
            {{ family.familyName }}
          </option>
        </select>
      </div>
    </div>

    <!-- 三列布局：账户列表 + 账户详情 -->
    <div class="grid grid-cols-12 gap-6">
      <!-- 左侧：投资账户列表 -->
      <div class="col-span-12 lg:col-span-3">
        <div class="bg-white rounded-lg shadow border border-gray-200">
          <div class="px-4 py-3 border-b border-gray-200 flex items-center justify-between">
            <h2 class="text-sm font-semibold text-gray-900">投资账户列表</h2>
          </div>

          <div v-if="loadingAccounts" class="p-4 text-center text-gray-500 text-sm">
            加载中...
          </div>

          <div v-else-if="accounts.length === 0" class="p-4 text-center text-gray-500 text-sm">
            该大类下暂无投资账户
          </div>

          <div v-else>
            <div class="divide-y divide-gray-200 max-h-[calc(100vh-400px)] overflow-y-auto">
              <div
                v-for="account in accounts"
                :key="account.accountId"
                :class="[
                  'group relative',
                  selectedAccount?.accountId === account.accountId
                    ? 'bg-primary/10 border-l-4 border-primary'
                    : 'hover:bg-gray-50 border-l-4 border-transparent'
                ]"
              >
                <button
                  @click="selectAccount(account)"
                  class="w-full px-3 py-2 text-left transition-colors"
                >
                  <div class="flex items-center justify-between gap-2">
                    <div class="flex-1 min-w-0">
                      <div class="font-medium text-gray-900 text-sm truncate">
                        {{ account.accountName }}
                      </div>
                      <div class="text-xs text-gray-500 mt-0.5">
                        {{ account.recordCount || 0 }}条记录
                        <span v-if="account.latestValue" class="ml-2">
                          最近{{ getCurrencySymbol(account.currency) }}{{ formatNumber(account.latestValue) }}
                        </span>
                      </div>
                    </div>
                  </div>
                </button>
              </div>
            </div>

            <!-- 分类总和 -->
            <div class="px-4 py-3 border-t border-gray-200 bg-gray-50">
              <div class="flex items-center justify-between">
                <span class="text-sm font-medium text-gray-700">{{ selectedCategory?.categoryName }}总计</span>
                <span class="text-base font-bold text-primary">
                  ${{ formatNumber(categoryTotal) }}
                </span>
              </div>
              <div class="text-xs text-gray-500 mt-1">
                共 {{ accounts.length }} 个账户
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：账户详情 -->
      <div v-if="!selectedAccount" class="col-span-12 lg:col-span-9">
        <div class="bg-white rounded-lg shadow border border-gray-200 p-12 text-center">
          <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
          <h3 class="mt-2 text-sm font-medium text-gray-900">请选择投资账户</h3>
          <p class="mt-1 text-sm text-gray-500">从左侧列表选择一个账户查看详情和交易记录</p>
        </div>
      </div>

      <div v-else class="col-span-12 lg:col-span-9">
        <!-- 账户信息栏 -->
        <div class="bg-white rounded-lg shadow border border-gray-200 p-4 mb-6">
          <div class="flex items-center justify-between">
            <div>
              <h2 class="text-lg font-semibold text-gray-900 flex items-center gap-2">
                <span class="text-2xl">{{ selectedCategory?.categoryIcon }}</span>
                {{ selectedCategory?.categoryName }} - {{ selectedAccount.accountName }}
              </h2>
              <p class="text-sm text-gray-600 mt-1">
                持有人: {{ selectedAccount.userName }} | 币种: {{ selectedAccount.currency }}
                <span v-if="selectedAccount.institution" class="ml-2">| 机构: {{ selectedAccount.institution }}</span>
              </p>
            </div>
            <button
              @click="openTransactionDialog()"
              class="px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary/90 text-sm font-medium flex items-center gap-2"
            >
              <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="12" y1="5" x2="12" y2="19"></line>
                <line x1="5" y1="12" x2="19" y2="12"></line>
              </svg>
              添加交易
            </button>
          </div>
        </div>

        <!-- 两列：投资趋势 + 交易历史 -->
        <div class="grid grid-cols-1 xl:grid-cols-2 gap-6">
          <!-- 投资趋势图 -->
          <div class="bg-white rounded-lg shadow border border-gray-200 p-6">
            <div class="flex items-center justify-between mb-4">
              <h3 class="text-base font-semibold text-gray-900">投资趋势</h3>
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
            <div v-show="chartData" class="h-80">
              <canvas ref="chartCanvas"></canvas>
            </div>
            <div v-show="!chartData" class="h-80 flex items-center justify-center text-gray-500 text-sm">
              暂无数据，请添加交易记录
            </div>
          </div>

          <!-- 交易历史记录 -->
          <div class="bg-white rounded-lg shadow border border-gray-200">
            <div class="px-4 py-3 border-b border-gray-200">
              <h3 class="text-base font-semibold text-gray-900">交易历史</h3>
            </div>

            <div v-if="loadingTransactions" class="p-4 text-center text-gray-500 text-sm">
              加载中...
            </div>

            <div v-else-if="transactions.length === 0" class="p-4 text-center text-gray-500 text-sm">
              暂无交易记录
            </div>

            <div v-else class="max-h-[480px] overflow-y-auto">
              <table class="w-full">
                <thead class="bg-gray-50 sticky top-0">
                  <tr>
                    <th class="px-4 py-2 text-left text-xs font-medium text-gray-700">期间</th>
                    <th class="px-4 py-2 text-left text-xs font-medium text-gray-700">类型</th>
                    <th class="px-4 py-2 text-right text-xs font-medium text-gray-700">金额</th>
                    <th class="px-4 py-2 text-right text-xs font-medium text-gray-700">操作</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-gray-200">
                  <tr v-for="tx in transactions" :key="tx.id" class="hover:bg-gray-50">
                    <td class="px-4 py-2 text-sm text-gray-900">{{ tx.transactionPeriod }}</td>
                    <td class="px-4 py-2 text-sm">
                      <span :class="[
                        'inline-block px-2 py-0.5 rounded text-xs font-medium',
                        tx.transactionType === 'DEPOSIT'
                          ? 'bg-green-100 text-green-700'
                          : 'bg-red-100 text-red-700'
                      ]">
                        {{ tx.transactionType === 'DEPOSIT' ? '投入' : '取出' }}
                      </span>
                    </td>
                    <td class="px-4 py-2 text-sm text-right font-medium text-gray-900">
                      {{ getCurrencySymbol(tx.currency) }}{{ formatNumber(tx.amount) }}
                    </td>
                    <td class="px-4 py-2 text-right">
                      <button
                        @click="editTransaction(tx)"
                        class="text-blue-600 hover:text-blue-800 text-xs font-medium mr-2"
                      >
                        编辑
                      </button>
                      <button
                        @click="deleteTransaction(tx)"
                        class="text-red-600 hover:text-red-800 text-xs font-medium"
                      >
                        删除
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 添加/编辑交易对话框 -->
    <div v-if="showTransactionDialog" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div class="bg-white rounded-lg shadow-xl w-full max-w-md mx-4">
        <div class="px-6 py-4 border-b border-gray-200">
          <h3 class="text-lg font-semibold text-gray-900">
            {{ editingTransaction ? '编辑交易记录' : '添加交易记录' }}
          </h3>
        </div>
        <div class="px-6 py-4 space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">交易期间</label>
            <input
              v-model="transactionForm.transactionPeriod"
              type="month"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-primary"
              required
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">交易类型</label>
            <select
              v-model="transactionForm.transactionType"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-primary"
              required
            >
              <option value="DEPOSIT">投入</option>
              <option value="WITHDRAWAL">取出</option>
            </select>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">金额</label>
            <div class="flex gap-2">
              <input
                v-model.number="transactionForm.amount"
                type="number"
                step="0.01"
                min="0"
                class="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-primary"
                placeholder="0.00"
                required
              />
              <div class="px-3 py-2 bg-gray-100 border border-gray-300 rounded-lg text-gray-700 font-medium min-w-[80px] flex items-center justify-center">
                {{ selectedAccount?.currency || 'USD' }}
              </div>
            </div>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">说明（可选）</label>
            <textarea
              v-model="transactionForm.description"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-primary"
              rows="3"
              placeholder="交易说明"
            ></textarea>
          </div>
        </div>
        <div class="px-6 py-4 border-t border-gray-200 flex justify-end gap-2">
          <button
            @click="closeTransactionDialog"
            class="px-4 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 text-sm font-medium"
          >
            取消
          </button>
          <button
            @click="saveTransaction"
            :disabled="savingTransaction"
            class="px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary/90 text-sm font-medium disabled:opacity-50"
          >
            {{ savingTransaction ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { investmentAccountAPI, investmentTransactionAPI } from '@/api/investment'
import { familyAPI } from '@/api/family'
import Chart from 'chart.js/auto'

export default {
  name: 'InvestmentRecords',
  setup() {
    // 数据
    const families = ref([])
    const selectedFamilyId = ref(null)
    const investmentCategories = ref([])
    const selectedCategory = ref(null)
    const accounts = ref([])
    const selectedAccount = ref(null)
    const transactions = ref([])
    const chartData = ref(null)

    // 加载状态
    const loadingAccounts = ref(false)
    const loadingTransactions = ref(false)

    // 对话框
    const showTransactionDialog = ref(false)
    const editingTransaction = ref(null)
    const transactionForm = ref({
      accountId: null,
      transactionPeriod: '',
      transactionType: 'DEPOSIT',
      amount: null,
      description: ''
    })
    const savingTransaction = ref(false)

    // 图表相关
    const chartCanvas = ref(null)
    const chartInstance = ref(null)
    const timeRanges = ref([
      { label: '6月', value: '6m' },
      { label: '1年', value: '12m' },
      { label: '3年', value: '36m' },
      { label: '全部', value: 'all' }
    ])
    const selectedTimeRange = ref('12m')

    // 计算属性
    const categoryTotal = computed(() => {
      return accounts.value.reduce((sum, acc) => sum + (acc.latestValueInUSD || 0), 0)
    })

    // 工具函数
    const formatNumber = (value) => {
      if (!value) return '0.00'
      return parseFloat(value).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    }

    const getCurrencySymbol = (currency) => {
      const symbols = { CNY: '¥', USD: '$', EUR: '€', GBP: '£' }
      return symbols[currency] || currency
    }

    // 家庭选择
    const onFamilyChange = async () => {
      await loadInvestmentCategories()
      if (investmentCategories.value.length > 0) {
        selectedCategory.value = investmentCategories.value[0]
      }
    }

    // 加载家庭列表
    const loadFamilies = async () => {
      try {
        const response = await familyAPI.getDefault()
        if (response.success) {
          families.value = response.data ? [response.data] : []

          // 尝试获取默认家庭
          if (families.value.length > 0) {
            try {
              const defaultResponse = await familyAPI.getDefault()
              if (defaultResponse.success && defaultResponse.data) {
                selectedFamilyId.value = defaultResponse.data.id
              } else {
                // 如果没有默认家庭，使用第一个
                selectedFamilyId.value = families.value[0].id
              }
            } catch (err) {
              console.error('获取默认家庭失败:', err)
              // 如果获取默认家庭失败，使用第一个
              selectedFamilyId.value = families.value[0].id
            }
            await onFamilyChange()
          }
        }
      } catch (error) {
        console.error('加载家庭列表失败:', error)
      }
    }

    // 加载投资大类（从资产分类表中获取is_investment = TRUE的分类）
    const loadInvestmentCategories = async () => {
      try {
        const response = await investmentAccountAPI.getCategories()
        if (response.success) {
          investmentCategories.value = response.data
        }
      } catch (error) {
        console.error('加载投资大类失败:', error)
      }
    }

    // 选择账户
    const selectAccount = (account) => {
      selectedAccount.value = account
      loadTransactions()
    }

    // 加载交易记录
    const loadTransactions = async () => {
      if (!selectedAccount.value) return

      loadingTransactions.value = true
      try {
        const response = await investmentTransactionAPI.getByAccount(selectedAccount.value.accountId)
        if (response.success) {
          transactions.value = response.data
          await nextTick()
          renderChart()
        }
      } catch (error) {
        console.error('加载交易记录失败:', error)
      } finally {
        loadingTransactions.value = false
      }
    }

    // 渲染图表
    const renderChart = () => {
      if (!chartCanvas.value || transactions.value.length === 0) {
        chartData.value = false
        return
      }

      // 销毁旧图表
      if (chartInstance.value) {
        chartInstance.value.destroy()
      }

      // 按期间排序交易记录
      const sortedTransactions = [...transactions.value].sort((a, b) =>
        a.transactionPeriod.localeCompare(b.transactionPeriod)
      )

      // 计算累计账户价值
      let cumulativeValue = 0
      const chartDataPoints = sortedTransactions.map(tx => {
        if (tx.transactionType === 'DEPOSIT' || tx.transactionType === 'TRANSFER_IN') {
          cumulativeValue += tx.amount
        } else if (tx.transactionType === 'WITHDRAWAL' || tx.transactionType === 'TRANSFER_OUT') {
          cumulativeValue -= tx.amount
        } else if (tx.transactionType === 'GAIN') {
          cumulativeValue += tx.amount
        } else if (tx.transactionType === 'LOSS') {
          cumulativeValue -= tx.amount
        }

        return {
          period: tx.transactionPeriod,
          value: cumulativeValue
        }
      })

      // 准备图表数据
      const ctx = chartCanvas.value.getContext('2d')
      chartInstance.value = new Chart(ctx, {
        type: 'line',
        data: {
          labels: chartDataPoints.map(d => d.period),
          datasets: [{
            label: '账户总值',
            data: chartDataPoints.map(d => d.value),
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
                  return '总值: ' + getCurrencySymbol(selectedAccount.value.currency) + formatNumber(context.parsed.y)
                }
              }
            }
          },
          scales: {
            y: {
              beginAtZero: false,
              title: {
                display: true,
                text: '账户价值'
              },
              ticks: {
                callback: function(value) {
                  return getCurrencySymbol(selectedAccount.value.currency) + formatNumber(value)
                }
              }
            },
            x: {
              title: {
                display: true,
                text: '期间'
              }
            }
          }
        }
      })

      chartData.value = true
    }

    // 打开交易对话框
    const openTransactionDialog = (transaction = null) => {
      editingTransaction.value = transaction
      if (transaction) {
        transactionForm.value = {
          accountId: transaction.accountId,
          transactionPeriod: transaction.transactionPeriod,
          transactionType: transaction.transactionType,
          amount: transaction.amount,
          description: transaction.description || ''
        }
      } else {
        transactionForm.value = {
          accountId: selectedAccount.value.accountId,
          transactionPeriod: new Date().toISOString().slice(0, 7),
          transactionType: 'DEPOSIT',
          amount: null,
          description: ''
        }
      }
      showTransactionDialog.value = true
    }

    const closeTransactionDialog = () => {
      showTransactionDialog.value = false
      editingTransaction.value = null
    }

    // 保存交易
    const saveTransaction = async () => {
      savingTransaction.value = true
      try {
        let response
        if (editingTransaction.value) {
          response = await investmentTransactionAPI.update(editingTransaction.value.id, transactionForm.value)
        } else {
          response = await investmentTransactionAPI.create(transactionForm.value)
        }

        if (response.success) {
          closeTransactionDialog()
          await loadTransactions()
        } else {
          alert(response.message || '保存失败')
        }
      } catch (error) {
        console.error('保存交易记录失败:', error)
        alert('保存失败：' + error.message)
      } finally {
        savingTransaction.value = false
      }
    }

    // 编辑交易
    const editTransaction = (transaction) => {
      openTransactionDialog(transaction)
    }

    // 删除交易
    const deleteTransaction = async (transaction) => {
      if (!confirm('确定要删除这条交易记录吗？')) return

      try {
        const response = await investmentTransactionAPI.delete(transaction.id)
        if (response.success) {
          await loadTransactions()
        } else {
          alert(response.message || '删除失败')
        }
      } catch (error) {
        console.error('删除交易记录失败:', error)
        alert('删除失败：' + error.message)
      }
    }

    // 监听大类选择变化
    watch(selectedCategory, async (newCategory) => {
      if (!newCategory) return

      selectedAccount.value = null
      transactions.value = []

      loadingAccounts.value = true
      try {
        const response = await investmentAccountAPI.getByCategory(selectedFamilyId.value, newCategory.categoryId)
        if (response.success) {
          accounts.value = response.data
        }
      } catch (error) {
        console.error('加载投资账户失败:', error)
      } finally {
        loadingAccounts.value = false
      }
    })

    // 初始化
    onMounted(() => {
      loadFamilies()
    })

    return {
      // 数据
      families,
      selectedFamilyId,
      investmentCategories,
      selectedCategory,
      accounts,
      selectedAccount,
      transactions,
      chartData,

      // 状态
      loadingAccounts,
      loadingTransactions,

      // 对话框
      showTransactionDialog,
      editingTransaction,
      transactionForm,
      savingTransaction,

      // 图表
      chartCanvas,
      timeRanges,
      selectedTimeRange,

      // 计算属性
      categoryTotal,

      // 方法
      formatNumber,
      getCurrencySymbol,
      onFamilyChange,
      selectAccount,
      openTransactionDialog,
      closeTransactionDialog,
      saveTransaction,
      editTransaction,
      deleteTransaction
    }
  }
}
</script>
