<template>
  <div class="p-3 md:p-4 space-y-3">
    <!-- 选择器和保存按钮 - 紧凑单行布局 -->
    <div class="flex flex-col sm:flex-row gap-2 items-start sm:items-center sm:justify-between border-b border-gray-200 pb-2">
      <div class="flex flex-col sm:flex-row gap-2">
        <div class="flex items-center gap-2">
          <label class="text-xs font-medium text-gray-700 whitespace-nowrap">家庭:</label>
          <select
            v-model="selectedFamilyId"
            class="px-2 py-1 text-xs border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-primary"
          >
            <option v-for="family in families" :key="family.id" :value="family.id">
              {{ family.familyName }}
            </option>
          </select>
        </div>
        <div class="flex items-center gap-2">
          <label class="text-xs font-medium text-gray-700 whitespace-nowrap">年份:</label>
          <input
            v-model.number="selectedYear"
            type="number"
            min="2020"
            :max="new Date().getFullYear() + 5"
            class="px-2 py-1 text-xs border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-primary w-20"
          />
        </div>
        <div class="flex items-center gap-2">
          <label class="text-xs font-medium text-gray-700 whitespace-nowrap">货币:</label>
          <select
            v-model="selectedCurrency"
            class="px-2 py-1 text-xs border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-primary"
          >
            <option v-for="currency in currencies" :key="currency" :value="currency">
              {{ currency }}
            </option>
          </select>
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
    <div v-if="!loading && budgets.length > 0" class="bg-white rounded-lg shadow border border-gray-200 px-3 py-2 space-y-3">
      <!-- 当前年份统计 -->
      <div>
        <h4 class="text-xs font-semibold text-gray-700 mb-2">{{ selectedYear }}年</h4>
        <div class="grid grid-cols-2 sm:grid-cols-4 gap-3">
          <div>
            <div class="text-xs text-gray-600 mb-0.5">总预算</div>
            <div class="text-base font-bold text-gray-900">{{ formatCurrency(totalBudget) }}</div>
          </div>
          <div>
            <div class="text-xs text-gray-600 mb-0.5">实际支出</div>
            <div class="text-base font-bold text-blue-600">{{ formatCurrency(currentYearActual) }}</div>
          </div>
          <div>
            <div class="text-xs text-gray-600 mb-0.5">固定日常</div>
            <div class="text-sm font-semibold text-blue-600">
              {{ formatCurrency(fixedBudget) }}
              <span class="text-xs text-gray-500 ml-1">({{ fixedPercentage }}%)</span>
            </div>
          </div>
          <div>
            <div class="text-xs text-gray-600 mb-0.5">不定期</div>
            <div class="text-sm font-semibold text-purple-600">
              {{ formatCurrency(irregularBudget) }}
              <span class="text-xs text-gray-500 ml-1">({{ irregularPercentage }}%)</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 前一年统计 -->
      <div class="border-t border-gray-200 pt-2">
        <h4 class="text-xs font-semibold text-gray-700 mb-2">{{ selectedYear - 1 }}年</h4>
        <div class="grid grid-cols-2 sm:grid-cols-4 gap-3">
          <div>
            <div class="text-xs text-gray-600 mb-0.5">总预算</div>
            <div class="text-sm font-semibold text-gray-700">{{ formatCurrency(lastYearBudget) }}</div>
          </div>
          <div>
            <div class="text-xs text-gray-600 mb-0.5">实际支出</div>
            <div class="text-sm font-semibold text-gray-600">{{ formatCurrency(lastYearActual) }}</div>
          </div>
          <div>
            <div class="text-xs text-gray-600 mb-0.5">预算差异</div>
            <div class="text-sm font-semibold" :class="budgetYearOverYearGrowth >= 0 ? 'text-red-600' : 'text-green-600'">
              {{ budgetYearOverYearGrowth >= 0 ? '+' : '' }}{{ budgetYearOverYearGrowth.toFixed(1) }}%
            </div>
          </div>
          <div>
            <div class="text-xs text-gray-600 mb-0.5">支出同比</div>
            <div class="text-sm font-semibold" :class="actualYearOverYearGrowth >= 0 ? 'text-red-600' : 'text-green-600'">
              {{ actualYearOverYearGrowth >= 0 ? '+' : '' }}{{ actualYearOverYearGrowth.toFixed(1) }}%
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 预算列表 - 表格布局（带滚动条） -->
    <div class="bg-white rounded-lg shadow border border-gray-200">
      <div v-if="loading" class="text-center py-6 text-gray-500 text-xs">加载中...</div>
      <div v-else-if="budgets.length === 0" class="text-center py-6 text-gray-500 text-xs">暂无子分类</div>
      <div v-else>
        <div class="overflow-x-auto max-h-[calc(100vh-280px)] overflow-y-auto">
          <table class="w-full">
            <thead class="bg-gray-50 sticky top-0">
              <tr>
                <th class="px-2 py-1.5 text-left text-xs font-medium text-gray-700 border-r border-gray-300">分类</th>
                <th class="px-2 py-1.5 text-center text-xs font-medium text-gray-700" colspan="2">{{ selectedYear }}年</th>
                <th class="px-2 py-1.5 text-center text-xs font-medium text-gray-700 border-l border-gray-300" colspan="2">{{ selectedYear - 1 }}年</th>
                <th class="px-2 py-1.5 text-left text-xs font-medium text-gray-700 border-l border-gray-300">备注</th>
              </tr>
              <tr class="bg-gray-100">
                <th class="px-2 py-1.5 text-xs font-medium text-gray-600 border-r border-gray-300"></th>
                <th class="px-2 py-1.5 text-center text-xs font-medium text-gray-600">预算</th>
                <th class="px-2 py-1.5 text-center text-xs font-medium text-gray-600">实际</th>
                <th class="px-2 py-1.5 text-center text-xs font-medium text-gray-600 border-l border-gray-300">预算</th>
                <th class="px-2 py-1.5 text-center text-xs font-medium text-gray-600">实际</th>
                <th class="px-2 py-1.5 text-xs font-medium text-gray-600 border-l border-gray-300"></th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-200">
              <tr v-for="budget in budgets" :key="budget.minorCategoryId" class="hover:bg-gray-50">
                <!-- 分类信息 -->
                <td class="px-2 py-1.5 border-r border-gray-200">
                  <div class="flex items-center gap-1.5">
                    <span class="text-base">{{ budget.majorCategoryIcon }}</span>
                    <div class="flex items-center gap-1.5 text-xs">
                      <span class="font-medium text-gray-900">{{ budget.majorCategoryName }} - {{ budget.minorCategoryName }}</span>
                      <span :class="[
                        'inline-block px-1 py-0.5 text-xs rounded',
                        budget.expenseType === 'FIXED_DAILY'
                          ? 'bg-green-100 text-green-700'
                          : 'bg-orange-100 text-orange-700'
                      ]">
                        {{ budget.expenseType === 'FIXED_DAILY' ? '固定' : '不定期' }}
                      </span>
                    </div>
                  </div>
                </td>

                <!-- 当前年份预算金额输入 -->
                <td class="px-2 py-1.5">
                  <div class="relative">
                    <span class="absolute left-1.5 top-1/2 -translate-y-1/2 text-gray-500 text-xs">{{ selectedCurrency === 'CNY' ? '¥' : '$' }}</span>
                    <input
                      v-model.number="budgetAmounts[budget.minorCategoryId]"
                      @input="markAsChanged(budget.minorCategoryId)"
                      type="number"
                      step="0.01"
                      min="0"
                      placeholder="0.00"
                      class="w-24 pl-5 pr-1.5 py-1 text-xs border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-primary text-right"
                    />
                  </div>
                </td>

                <!-- 当前年份实际支出 -->
                <td class="px-2 py-1.5 text-right text-xs">
                  <span class="text-gray-700">{{ formatCurrency(getCurrentYearActual(budget.minorCategoryId)) }}</span>
                </td>

                <!-- 前一年预算 -->
                <td class="px-2 py-1.5 text-right text-xs border-l border-gray-200">
                  <span class="text-gray-600">{{ formatCurrency(getLastYearBudget(budget.minorCategoryId)) }}</span>
                </td>

                <!-- 前一年实际支出 -->
                <td class="px-2 py-1.5 text-right text-xs">
                  <span class="text-gray-600">{{ formatCurrency(getLastYearActual(budget.minorCategoryId)) }}</span>
                </td>

                <!-- 备注 -->
                <td class="px-2 py-1.5 border-l border-gray-200">
                  <input
                    v-model="budgetNotes[budget.minorCategoryId]"
                    @input="markAsChanged(budget.minorCategoryId)"
                    type="text"
                    maxlength="500"
                    placeholder="备注（可选）"
                    class="w-full px-2 py-1 text-xs border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-primary"
                  />
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
import { ref, computed, watch, onMounted } from 'vue'
import { familyAPI } from '@/api/family'
import { exchangeRateAPI } from '@/api/exchangeRate'
import { expenseAnalysisAPI } from '@/api/expense'
import axios from 'axios'

// 响应式数据
const families = ref([])
const currencies = ref([])
const budgets = ref([])
const budgetAmounts = ref({})  // categoryId -> amount
const budgetNotes = ref({})    // categoryId -> notes
const budgetIds = ref({})      // categoryId -> budgetId (for tracking existing records)

// 实际支出数据
const currentYearActualData = ref([])  // 当前年份实际支出
const lastYearBudgetData = ref([])     // 前一年预算
const lastYearActualData = ref([])     // 前一年实际支出

const selectedFamilyId = ref(null)
const selectedYear = ref(new Date().getFullYear())
const selectedCurrency = ref('USD')

const loading = ref(false)
const saving = ref(false)
const changedCategories = ref(new Set())

// 计算属性
const hasChanges = computed(() => changedCategories.value.size > 0)

const totalBudget = computed(() => {
  return budgets.value.reduce((sum, budget) => {
    const amount = budgetAmounts.value[budget.minorCategoryId] || 0
    return sum + Number(amount)
  }, 0)
})

const fixedBudget = computed(() => {
  return budgets.value
    .filter(b => b.expenseType === 'FIXED_DAILY')
    .reduce((sum, budget) => {
      const amount = budgetAmounts.value[budget.minorCategoryId] || 0
      return sum + Number(amount)
    }, 0)
})

const irregularBudget = computed(() => {
  return budgets.value
    .filter(b => b.expenseType === 'LARGE_IRREGULAR')
    .reduce((sum, budget) => {
      const amount = budgetAmounts.value[budget.minorCategoryId] || 0
      return sum + Number(amount)
    }, 0)
})

const fixedPercentage = computed(() => {
  if (totalBudget.value === 0) return 0
  return ((fixedBudget.value / totalBudget.value) * 100).toFixed(1)
})

const irregularPercentage = computed(() => {
  if (totalBudget.value === 0) return 0
  return ((irregularBudget.value / totalBudget.value) * 100).toFixed(1)
})

// 当前年份实际支出总计
const currentYearActual = computed(() => {
  return currentYearActualData.value.reduce((sum, item) => {
    return sum + parseFloat(item.actualAmount || 0)
  }, 0)
})

// 前一年预算总计
const lastYearBudget = computed(() => {
  return lastYearBudgetData.value.reduce((sum, item) => {
    return sum + parseFloat(item.budgetAmount || 0)
  }, 0)
})

// 前一年实际支出总计
const lastYearActual = computed(() => {
  return lastYearActualData.value.reduce((sum, item) => {
    return sum + parseFloat(item.actualAmount || 0)
  }, 0)
})

// 预算同比增长
const budgetYearOverYearGrowth = computed(() => {
  if (lastYearBudget.value === 0) return 0
  return ((totalBudget.value - lastYearBudget.value) / lastYearBudget.value) * 100
})

// 实际支出同比增长
const actualYearOverYearGrowth = computed(() => {
  if (lastYearActual.value === 0) return 0
  return ((currentYearActual.value - lastYearActual.value) / lastYearActual.value) * 100
})

// 获取当前年份指定分类的实际支出
function getCurrentYearActual(minorCategoryId) {
  const item = currentYearActualData.value.find(d => d.minorCategoryId === minorCategoryId)
  return item ? parseFloat(item.actualAmount || 0) : 0
}

// 获取前一年指定分类的预算
function getLastYearBudget(minorCategoryId) {
  const item = lastYearBudgetData.value.find(d => d.minorCategoryId === minorCategoryId)
  return item ? parseFloat(item.budgetAmount || 0) : 0
}

// 获取前一年指定分类的实际支出
function getLastYearActual(minorCategoryId) {
  const item = lastYearActualData.value.find(d => d.minorCategoryId === minorCategoryId)
  return item ? parseFloat(item.actualAmount || 0) : 0
}

// 格式化货币
function formatCurrency(amount) {
  const currencySymbols = {
    'CNY': '¥',
    'USD': '$',
    'EUR': '€',
    'GBP': '£',
    'JPY': '¥',
    'AUD': 'A$',
    'CAD': 'C$'
  }
  const symbol = currencySymbols[selectedCurrency.value] || selectedCurrency.value + ' '
  return symbol + (amount || 0).toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

// 标记为已修改
function markAsChanged(categoryId) {
  const amount = budgetAmounts.value[categoryId]
  const hasValue = amount !== null && amount !== undefined && amount !== ''
  const hasOriginalBudget = budgetIds.value[categoryId]

  // 三种情况都标记为changed：
  // 1. 输入了大于0的值（create或update）
  // 2. 输入了0且原来有预算（delete）
  // 3. 备注有修改
  if (hasValue && Number(amount) > 0) {
    changedCategories.value.add(categoryId)
  } else if (hasOriginalBudget && (!hasValue || Number(amount) === 0)) {
    changedCategories.value.add(categoryId)
  } else {
    changedCategories.value.delete(categoryId)
  }
}

// 加载家庭列表
async function loadFamilies() {
  try {
    const response = await familyAPI.getAll()

    // 三种响应格式处理
    if (Array.isArray(response.data)) {
      families.value = response.data
    } else if (response.data && response.data.data) {
      families.value = Array.isArray(response.data.data) ? response.data.data : []
    } else if (response.data && 'success' in response.data) {
      families.value = Array.isArray(response.data.data) ? response.data.data : []
    }

    // 如果selectedFamilyId还未设置，获取默认家庭
    if (!selectedFamilyId.value) {
      try {
        const defaultResponse = await familyAPI.getDefault()
        if (defaultResponse.success && defaultResponse.data) {
          selectedFamilyId.value = defaultResponse.data.id
        } else if (families.value.length > 0) {
          selectedFamilyId.value = families.value[0].id
        }
      } catch (err) {
        console.error('获取默认家庭失败:', err)
        if (families.value.length > 0) {
          selectedFamilyId.value = families.value[0].id
        }
      }
    }
  } catch (error) {
    console.error('加载家庭列表失败:', error)
  }
}

// 加载货币列表
async function loadCurrencies() {
  try {
    const response = await exchangeRateAPI.getAllActive()

    // 三种响应格式处理
    let rates = []
    if (Array.isArray(response.data)) {
      rates = response.data
    } else if (response.data && response.data.data) {
      rates = Array.isArray(response.data.data) ? response.data.data : []
    } else if (response.data && 'success' in response.data) {
      rates = Array.isArray(response.data.data) ? response.data.data : []
    }

    // 提取唯一货币
    const currencySet = new Set(rates.map(rate => rate.currency))
    currencySet.add('USD')  // 手动添加美元（基准货币）

    currencies.value = Array.from(currencySet).sort()
  } catch (error) {
    console.error('加载货币列表失败:', error)
    currencies.value = ['USD', 'CNY', 'EUR', 'GBP', 'JPY', 'AUD', 'CAD']
  }
}

// 加载预算数据
async function loadBudgets() {
  if (!selectedFamilyId.value || !selectedYear.value || !selectedCurrency.value) {
    return
  }

  loading.value = true
  try {
    // 并行加载当前年份预算、实际支出，以及前一年的预算和实际支出
    const [budgetResponse, currentActualResponse, lastYearBudgetResponse, lastYearActualResponse] = await Promise.all([
      // 当前年份预算
      axios.get('/api/expense-budgets', {
        params: {
          familyId: selectedFamilyId.value,
          budgetYear: selectedYear.value,
          currency: selectedCurrency.value
        }
      }),
      // 当前年份实际支出
      expenseAnalysisAPI.getBudgetExecution(
        selectedFamilyId.value,
        selectedYear.value,
        selectedCurrency.value
      ).catch(() => ({ success: false, data: [] })),
      // 前一年预算
      axios.get('/api/expense-budgets', {
        params: {
          familyId: selectedFamilyId.value,
          budgetYear: selectedYear.value - 1,
          currency: selectedCurrency.value
        }
      }).catch(() => ({ data: [] })),
      // 前一年实际支出
      expenseAnalysisAPI.getBudgetExecution(
        selectedFamilyId.value,
        selectedYear.value - 1,
        selectedCurrency.value
      ).catch(() => ({ success: false, data: [] }))
    ])

    // 处理当前年份预算数据
    let data = []
    if (Array.isArray(budgetResponse.data)) {
      data = budgetResponse.data
    } else if (budgetResponse.data && budgetResponse.data.data) {
      data = Array.isArray(budgetResponse.data.data) ? budgetResponse.data.data : []
    } else if (budgetResponse.data && 'success' in budgetResponse.data) {
      data = Array.isArray(budgetResponse.data.data) ? budgetResponse.data.data : []
    }

    budgets.value = data

    // 按类型排序：固定日常在前
    budgets.value.sort((a, b) => {
      const typeOrder = { 'FIXED_DAILY': 0, 'LARGE_IRREGULAR': 1 }
      const orderA = typeOrder[a.expenseType] ?? 2
      const orderB = typeOrder[b.expenseType] ?? 2
      return orderA - orderB
    })

    // 填充预算金额和备注
    budgetAmounts.value = {}
    budgetNotes.value = {}
    budgetIds.value = {}

    budgets.value.forEach(budget => {
      budgetAmounts.value[budget.minorCategoryId] = budget.budgetAmount || 0
      budgetNotes.value[budget.minorCategoryId] = budget.notes || ''
      budgetIds.value[budget.minorCategoryId] = budget.id
    })

    // 处理当前年份实际支出数据
    if (currentActualResponse && currentActualResponse.success) {
      currentYearActualData.value = currentActualResponse.data || []
    } else {
      currentYearActualData.value = []
    }

    // 处理前一年预算数据
    let lastYearBudgets = []
    if (Array.isArray(lastYearBudgetResponse.data)) {
      lastYearBudgets = lastYearBudgetResponse.data
    } else if (lastYearBudgetResponse.data && lastYearBudgetResponse.data.data) {
      lastYearBudgets = Array.isArray(lastYearBudgetResponse.data.data) ? lastYearBudgetResponse.data.data : []
    }
    lastYearBudgetData.value = lastYearBudgets

    // 处理前一年实际支出数据
    if (lastYearActualResponse && lastYearActualResponse.success) {
      lastYearActualData.value = lastYearActualResponse.data || []
    } else {
      lastYearActualData.value = []
    }

    // 清空变更标记
    changedCategories.value.clear()

  } catch (error) {
    console.error('加载预算数据失败:', error)
  } finally {
    loading.value = false
  }
}

// 保存全部
async function saveAll() {
  if (!hasChanges.value || saving.value) return

  saving.value = true
  try {
    // 构建要保存的预算项
    const budgetsToSave = Array.from(changedCategories.value).map(categoryId => ({
      minorCategoryId: categoryId,
      budgetAmount: budgetAmounts.value[categoryId] || 0,
      notes: budgetNotes.value[categoryId] || null
    }))

    const response = await axios.post('/api/expense-budgets/batch', {
      familyId: selectedFamilyId.value,
      budgetYear: selectedYear.value,
      currency: selectedCurrency.value,
      budgets: budgetsToSave
    })

    if (response.data.success) {
      alert('保存成功')
      // 重新加载数据
      await loadBudgets()
    } else {
      alert('保存失败: ' + response.data.message)
    }

  } catch (error) {
    console.error('保存失败:', error)
    alert('保存失败: ' + (error.response?.data?.message || error.message))
  } finally {
    saving.value = false
  }
}

// 监听选择器变化
watch([selectedFamilyId, selectedYear, selectedCurrency], () => {
  loadBudgets()
})

// 组件挂载
onMounted(async () => {
  await loadFamilies()
  await loadCurrencies()
  await loadBudgets()
})
</script>
