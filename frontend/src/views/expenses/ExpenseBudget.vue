<template>
  <div class="p-4 md:p-6 space-y-4">
    <!-- 页面头部 - 移动端响应式 -->
    <div class="space-y-3">
      <!-- 第一行：标题 -->
      <div class="flex flex-col sm:flex-row sm:items-center gap-3">
        <h1 class="text-xl md:text-2xl font-bold text-gray-900 flex-shrink-0">年度预算管理</h1>
      </div>

      <!-- 第二行：家庭、年份、货币选择器 -->
      <div class="grid grid-cols-1 sm:grid-cols-3 gap-3">
        <div class="flex flex-col sm:flex-row sm:items-center gap-2">
          <label class="text-sm font-medium text-gray-700 whitespace-nowrap">家庭：</label>
          <select
            v-model="selectedFamilyId"
            class="px-3 py-1.5 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary min-h-[44px] flex-1"
          >
            <option v-for="family in families" :key="family.id" :value="family.id">
              {{ family.familyName }}
            </option>
          </select>
        </div>
        <div class="flex flex-col sm:flex-row sm:items-center gap-2">
          <label class="text-sm font-medium text-gray-700 whitespace-nowrap">年份：</label>
          <input
            v-model.number="selectedYear"
            type="number"
            min="2020"
            :max="new Date().getFullYear() + 5"
            class="px-3 py-1.5 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary min-h-[44px] flex-1"
          />
        </div>
        <div class="flex flex-col sm:flex-row sm:items-center gap-2">
          <label class="text-sm font-medium text-gray-700 whitespace-nowrap">货币：</label>
          <select
            v-model="selectedCurrency"
            class="px-3 py-1.5 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary min-h-[44px] flex-1"
          >
            <option v-for="currency in currencies" :key="currency" :value="currency">
              {{ currency }}
            </option>
          </select>
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

    <!-- 预算列表 - 移动端横向滚动 -->
    <div class="bg-white rounded-lg shadow border border-gray-200">
      <div v-if="loading" class="text-center py-8 text-gray-500 text-sm">
        加载中...
      </div>
      <div v-else-if="budgets.length === 0" class="text-center py-8 text-gray-500 text-sm">
        暂无子分类
      </div>
      <div v-else>
        <!-- 横向滚动容器 -->
        <div class="overflow-x-auto -mx-2 sm:mx-0">
          <div class="inline-block min-w-full align-middle px-2 sm:px-0">
            <div class="divide-y divide-gray-200">
              <!-- 表头 -->
              <div class="grid grid-cols-12 gap-3 px-4 py-3 bg-gray-50 text-xs font-medium text-gray-700" style="min-width: 800px;">
                <div class="col-span-4">分类</div>
                <div class="col-span-3">预算金额</div>
                <div class="col-span-5">备注</div>
              </div>

              <!-- 数据行 -->
              <div
                v-for="budget in budgets"
                :key="budget.minorCategoryId"
                class="grid grid-cols-12 gap-3 px-4 py-2.5 hover:bg-gray-50 items-center"
                style="min-width: 800px;"
              >
                <!-- 分类信息 -->
                <div class="col-span-4">
                  <div class="flex items-center gap-2">
                    <span class="text-lg">{{ budget.majorCategoryIcon }}</span>
                    <div>
                      <div class="font-medium text-gray-900 text-sm">
                        {{ budget.majorCategoryName }} - {{ budget.minorCategoryName }}
                      </div>
                      <span :class="[
                        'inline-block px-1.5 py-0.5 text-xs rounded mt-0.5',
                        budget.expenseType === 'FIXED_DAILY'
                          ? 'bg-green-100 text-green-700'
                          : 'bg-orange-100 text-orange-700'
                      ]">
                        {{ budget.expenseType === 'FIXED_DAILY' ? '固定' : '不定期' }}
                      </span>
                    </div>
                  </div>
                </div>

                <!-- 预算金额输入 -->
                <div class="col-span-3">
                  <input
                    v-model.number="budgetAmounts[budget.minorCategoryId]"
                    @input="markAsChanged(budget.minorCategoryId)"
                    type="number"
                    step="0.01"
                    min="0"
                    placeholder="0.00"
                    class="w-full px-3 py-1.5 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary min-h-[44px]"
                  />
                </div>

                <!-- 备注 -->
                <div class="col-span-5">
                  <input
                    v-model="budgetNotes[budget.minorCategoryId]"
                    @input="markAsChanged(budget.minorCategoryId)"
                    type="text"
                    maxlength="500"
                    placeholder="备注（可选）"
                    class="w-full px-3 py-1.5 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary min-h-[44px]"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 统计信息 - 移动端响应式 -->
    <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
      <div class="bg-white p-4 rounded-lg shadow border border-gray-200">
        <div class="text-sm text-gray-600 mb-1">总预算</div>
        <div class="text-xl sm:text-2xl font-bold text-gray-900">
          {{ formatCurrency(totalBudget) }}
        </div>
      </div>
      <div class="bg-white p-4 rounded-lg shadow border border-gray-200">
        <div class="text-sm text-gray-600 mb-1">固定日常预算</div>
        <div class="text-xl sm:text-2xl font-bold text-green-600">
          {{ formatCurrency(fixedBudget) }}
          <span class="text-xs sm:text-sm text-gray-500 ml-2">({{ fixedPercentage }}%)</span>
        </div>
      </div>
      <div class="bg-white p-4 rounded-lg shadow border border-gray-200">
        <div class="text-sm text-gray-600 mb-1">大额不定期预算</div>
        <div class="text-xl sm:text-2xl font-bold text-orange-600">
          {{ formatCurrency(irregularBudget) }}
          <span class="text-xs sm:text-sm text-gray-500 ml-2">({{ irregularPercentage }}%)</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { familyAPI } from '@/api/family'
import { exchangeRateAPI } from '@/api/exchangeRate'
import axios from 'axios'

// 响应式数据
const families = ref([])
const currencies = ref([])
const budgets = ref([])
const budgetAmounts = ref({})  // categoryId -> amount
const budgetNotes = ref({})    // categoryId -> notes
const budgetIds = ref({})      // categoryId -> budgetId (for tracking existing records)

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

    // 默认选中第一个家庭
    if (families.value.length > 0 && !selectedFamilyId.value) {
      selectedFamilyId.value = families.value[0].id
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
    const response = await axios.get('/api/expense-budgets', {
      params: {
        familyId: selectedFamilyId.value,
        budgetYear: selectedYear.value,
        currency: selectedCurrency.value
      }
    })

    // 三种响应格式处理
    let data = []
    if (Array.isArray(response.data)) {
      data = response.data
    } else if (response.data && response.data.data) {
      data = Array.isArray(response.data.data) ? response.data.data : []
    } else if (response.data && 'success' in response.data) {
      data = Array.isArray(response.data.data) ? response.data.data : []
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
