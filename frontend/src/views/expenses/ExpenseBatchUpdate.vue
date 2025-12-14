<template>
  <div class="p-4 md:p-6 space-y-4">
    <!-- 页面头部 - 移动端响应式 -->
    <div class="space-y-3">
      <!-- 第一行：标题和基础选择器 -->
      <div class="flex flex-col sm:flex-row sm:items-center gap-3">
        <h1 class="text-xl md:text-2xl font-bold text-gray-900 flex-shrink-0">批量录入支出</h1>
      </div>

      <!-- 第二行：家庭、货币、月份选择器 -->
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
        <div class="flex flex-col sm:flex-row sm:items-center gap-2">
          <label class="text-sm font-medium text-gray-700 whitespace-nowrap">月份：</label>
          <input
            v-model="recordPeriod"
            type="month"
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

    <!-- 支出列表 - 移动端横向滚动 -->
    <div class="bg-white rounded-lg shadow border border-gray-200">
      <div v-if="loading" class="text-center py-8 text-gray-500 text-sm">
        加载中...
      </div>
      <div v-else-if="filteredCategories.length === 0" class="text-center py-8 text-gray-500 text-sm">
        暂无子分类
      </div>
      <div v-else>
        <!-- 横向滚动容器 -->
        <div class="overflow-x-auto -mx-2 sm:mx-0">
          <div class="inline-block min-w-full align-middle px-2 sm:px-0">
            <div class="divide-y divide-gray-200">
              <!-- 表头 -->
              <div class="grid grid-cols-12 gap-3 px-4 py-3 bg-gray-50 text-xs font-medium text-gray-700" style="min-width: 800px;">
                <div class="col-span-3">分类</div>
                <div class="col-span-2 text-right">{{ previousMonth3 }}</div>
                <div class="col-span-2 text-right">{{ previousMonth2 }}</div>
                <div class="col-span-2 text-right">{{ previousMonth1 }}</div>
                <div class="col-span-3">{{ currentMonth }}</div>
              </div>

              <!-- 数据行 -->
              <div
                v-for="category in filteredCategories"
                :key="category.id"
                class="grid grid-cols-12 gap-3 px-4 py-2.5 hover:bg-gray-50 items-center"
                style="min-width: 800px;"
              >
                <!-- 分类信息（含类型标签） -->
                <div class="col-span-3">
                  <div class="flex items-center gap-2">
                    <span class="text-lg">{{ category.majorIcon }}</span>
                    <div>
                      <div class="font-medium text-gray-900 text-sm">
                        {{ category.majorName }} - {{ category.name }}
                      </div>
                      <span :class="[
                        'inline-block px-1.5 py-0.5 text-xs rounded mt-0.5',
                        category.expenseType === 'FIXED_DAILY'
                          ? 'bg-green-100 text-green-700'
                          : 'bg-orange-100 text-orange-700'
                      ]">
                        {{ category.expenseType === 'FIXED_DAILY' ? '固定日常' : '大额不定期' }}
                      </span>
                    </div>
                  </div>
                </div>

                <!-- 前3个月历史数据 -->
                <div class="col-span-2 text-right">
                  <div class="text-sm text-gray-600">
                    {{ formatCurrency(historyData[category.id]?.month3 ?? 0) }}
                  </div>
                </div>
                <div class="col-span-2 text-right">
                  <div class="text-sm text-gray-600">
                    {{ formatCurrency(historyData[category.id]?.month2 ?? 0) }}
                  </div>
                </div>
                <div class="col-span-2 text-right">
                  <div class="text-sm text-gray-700 font-medium">
                    {{ formatCurrency(historyData[category.id]?.month1 ?? 0) }}
                  </div>
                </div>

                <!-- 本月金额输入 -->
                <div class="col-span-3">
                  <div class="relative">
                    <span class="absolute left-2 top-2 text-gray-500 text-sm">{{ selectedCurrency === 'CNY' ? '¥' : '$' }}</span>
                    <input
                      v-model="categoryAmounts[category.id]"
                      type="number"
                      step="0.01"
                      min="0"
                      placeholder="0.00"
                      class="w-full pl-6 pr-2 py-1.5 text-sm border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-primary min-h-[44px]"
                      @input="markAsChanged(category.id)"
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 底部统计 - 移动端响应式 -->
        <div v-if="!loading && filteredCategories.length > 0" class="px-4 py-4 border-t border-gray-200 bg-gray-50">
          <div class="grid grid-cols-1 sm:grid-cols-3 gap-4 sm:gap-6">
            <div>
              <div class="text-xs text-gray-600 mb-1">本月总支出</div>
              <div class="text-xl sm:text-2xl font-bold text-gray-900">{{ formatCurrency(summary.total) }}</div>
            </div>
            <div>
              <div class="text-xs text-gray-600 mb-1">固定日常</div>
              <div class="text-base sm:text-lg font-semibold text-blue-600">
                {{ formatCurrency(summary.fixed) }}
                <span class="text-xs text-gray-500 ml-1">({{ summary.fixedPercent }}%)</span>
              </div>
            </div>
            <div>
              <div class="text-xs text-gray-600 mb-1">不定期支出</div>
              <div class="text-base sm:text-lg font-semibold text-purple-600">
                {{ formatCurrency(summary.large) }}
                <span class="text-xs text-gray-500 ml-1">({{ summary.largePercent }}%)</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { expenseCategoryAPI, expenseRecordAPI } from '@/api/expense'
import { familyAPI } from '@/api/family'
import { exchangeRateAPI } from '@/api/exchangeRate'

const loading = ref(false)
const saving = ref(false)
const families = ref([])
const currencies = ref([])
const majorCategories = ref([])
const allMinorCategories = ref([])
const categoryAmounts = ref({})
const categoryRecordIds = ref({}) // 存储每个分类的记录ID，用于删除
const historyData = ref({}) // 前3个月历史数据
const changedRecords = ref(new Set())

// 批量录入基础选项
const selectedFamilyId = ref(null)
const selectedCurrency = ref('USD')

// 记录期间，默认为当前月份
const today = new Date()
const recordPeriod = ref(`${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}`)

// 过滤后的分类 - 按类型排序（固定日常在前），同类型内有历史值的排在前面
const filteredCategories = computed(() => {
  return allMinorCategories.value.sort((a, b) => {
    // 1. 先按类型排序：FIXED_DAILY = 1, LARGE_IRREGULAR = 2
    const typeOrder = { 'FIXED_DAILY': 1, 'LARGE_IRREGULAR': 2 }
    const typeA = typeOrder[a.expenseType] || 3
    const typeB = typeOrder[b.expenseType] || 3

    if (typeA !== typeB) {
      return typeA - typeB
    }

    // 2. 同类型内，检查是否有历史数据（前3个月任一有值即可）
    const hasHistoryA = historyData.value[a.id] && Object.values(historyData.value[a.id]).some(amount => amount > 0)
    const hasHistoryB = historyData.value[b.id] && Object.values(historyData.value[b.id]).some(amount => amount > 0)

    // 有历史数据的排在前面
    if (hasHistoryA && !hasHistoryB) return -1
    if (!hasHistoryA && hasHistoryB) return 1

    // 都有或都没有历史数据，保持原顺序（按ID）
    return a.id - b.id
  })
})

// 月份显示
const currentMonth = computed(() => {
  if (!recordPeriod.value) return ''
  const [year, month] = recordPeriod.value.split('-')
  return `${year}年${month}月`
})

const previousMonth1 = computed(() => {
  const period = getPreviousPeriod(recordPeriod.value, 1)
  const [year, month] = period.split('-')
  return `${month}月`
})

const previousMonth2 = computed(() => {
  const period = getPreviousPeriod(recordPeriod.value, 2)
  const [year, month] = period.split('-')
  return `${month}月`
})

const previousMonth3 = computed(() => {
  const period = getPreviousPeriod(recordPeriod.value, 3)
  const [year, month] = period.split('-')
  return `${month}月`
})

// 是否有修改
const hasChanges = computed(() => changedRecords.value.size > 0)

// 统计数据
const summary = computed(() => {
  const total = filteredCategories.value.reduce((sum, c) => {
    const amount = parseFloat(categoryAmounts.value[c.id] || 0)
    return sum + amount
  }, 0)

  const fixed = filteredCategories.value.reduce((sum, c) => {
    if (c.expenseType === 'FIXED_DAILY') {
      const amount = parseFloat(categoryAmounts.value[c.id] || 0)
      return sum + amount
    }
    return sum
  }, 0)

  const large = total - fixed
  const fixedPercent = total > 0 ? ((fixed / total) * 100).toFixed(1) : 0
  const largePercent = total > 0 ? ((large / total) * 100).toFixed(1) : 0

  return { total, fixed, large, fixedPercent, largePercent }
})

// 格式化数字
function formatNumber(num) {
  return new Intl.NumberFormat('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(num || 0)
}

// 格式化货币显示
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
  const symbol = currencySymbols[selectedCurrency.value] || '$'
  return `${symbol}${formatNumber(amount)}`
}

// 获取前N个月的期间
function getPreviousPeriod(currentPeriod, monthsAgo) {
  const [year, month] = currentPeriod.split('-').map(Number)
  const date = new Date(year, month - 1 - monthsAgo, 1)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
}

// 标记为已修改
function markAsChanged(categoryId) {
  const currentAmount = categoryAmounts.value[categoryId]
  const originalRecordId = categoryRecordIds.value[categoryId]

  // 如果当前值与原始状态不同，标记为已修改
  // 三种情况都算修改：
  // 1. 有原始记录，改为0（删除）
  // 2. 有原始记录，改为其他值（更新）
  // 3. 无原始记录，输入新值（新增）

  const hasValue = currentAmount && parseFloat(currentAmount) > 0
  const hasOriginalRecord = !!originalRecordId

  if (hasValue || (hasOriginalRecord && (!currentAmount || parseFloat(currentAmount) === 0))) {
    // 有新值，或者有原始记录被改为0（要删除）
    changedRecords.value.add(categoryId)
  } else {
    // 无值且无原始记录（从未填写）
    changedRecords.value.delete(categoryId)
  }
}

// 加载家庭列表
async function loadFamilies() {
  try {
    const response = await familyAPI.getAll()

    // 处理响应数据 - 兼容多种返回格式
    let familyList = []
    if (Array.isArray(response.data)) {
      familyList = response.data
    } else if (response.data && response.data.data) {
      familyList = response.data.data
    } else if (response.data && 'success' in response.data) {
      familyList = response.data.data || []
    }

    families.value = familyList

    // 如果selectedFamilyId还未设置，获取默认家庭
    if (!selectedFamilyId.value) {
      try {
        const defaultResponse = await familyAPI.getDefault()
        if (defaultResponse.success && defaultResponse.data) {
          selectedFamilyId.value = defaultResponse.data.id
        } else if (familyList.length > 0) {
          selectedFamilyId.value = familyList[0].id
        }
      } catch (err) {
        console.error('获取默认家庭失败:', err)
        if (familyList.length > 0) {
          selectedFamilyId.value = familyList[0].id
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

    // 处理响应数据 - 兼容多种返回格式
    let ratesList = []
    if (Array.isArray(response.data)) {
      ratesList = response.data
    } else if (response.data && response.data.data) {
      ratesList = response.data.data
    } else if (response.data && 'success' in response.data) {
      ratesList = response.data.data || []
    }

    // 提取唯一的货币代码
    const currencySet = new Set(ratesList.map(rate => rate.currency))

    // 添加USD（基准货币，数据库中不存在汇率记录）
    currencySet.add('USD')

    // 排序
    currencies.value = Array.from(currencySet).sort()
  } catch (error) {
    console.error('加载货币列表失败:', error)
  }
}

// 加载分类
async function loadCategories() {
  loading.value = true
  try {
    const response = await expenseCategoryAPI.getAll()

    // 处理响应数据 - 兼容多种返回格式
    let categories = []
    if (Array.isArray(response.data)) {
      categories = response.data
    } else if (response.data && response.data.data) {
      categories = response.data.data
    } else if (response.data && 'success' in response.data) {
      categories = response.data.data || []
    }

    majorCategories.value = categories

    // 展平所有子分类
    allMinorCategories.value = categories.flatMap(major =>
      (major.minorCategories || []).map(minor => ({
        ...minor,
        majorCategoryId: major.id,
        majorName: major.name,
        majorIcon: major.icon
      }))
    )

    // 加载前3个月历史数据
    await loadHistoryData()

    // 加载本月数据
    await loadCurrentMonthData()
  } catch (error) {
    console.error('加载分类失败:', error)
    alert('加载分类失败，请刷新重试')
  } finally {
    loading.value = false
  }
}

// 加载前3个月历史数据
async function loadHistoryData() {
  if (!selectedFamilyId.value || !selectedCurrency.value) return

  try {
    // 加载前3个月的数据
    const periods = [
      getPreviousPeriod(recordPeriod.value, 3),
      getPreviousPeriod(recordPeriod.value, 2),
      getPreviousPeriod(recordPeriod.value, 1)
    ]

    const results = await Promise.all(
      periods.map(period =>
        expenseRecordAPI.getByPeriod(selectedFamilyId.value, period)
          .catch(() => ({ data: [] }))
      )
    )

    // 处理每个月的数据
    results.forEach((response, index) => {
      let records = []
      if (Array.isArray(response.data)) {
        records = response.data
      } else if (response.data && response.data.data) {
        records = response.data.data
      } else if (response.data && 'success' in response.data) {
        records = response.data.data || []
      }

      // 只加载与当前选择货币相同的记录
      records.filter(r => r.currency === selectedCurrency.value).forEach(record => {
        if (!historyData.value[record.minorCategoryId]) {
          historyData.value[record.minorCategoryId] = {}
        }
        const monthKey = `month${3 - index}` // month3, month2, month1
        historyData.value[record.minorCategoryId][monthKey] = record.amount
      })
    })
  } catch (error) {
    console.error('加载历史数据失败:', error)
  }
}

// 加载本月数据
async function loadCurrentMonthData() {
  if (!selectedFamilyId.value || !selectedCurrency.value) return

  try {
    const response = await expenseRecordAPI.getByPeriod(selectedFamilyId.value, recordPeriod.value)

    // 处理响应数据 - 兼容多种返回格式
    let records = []
    if (Array.isArray(response.data)) {
      records = response.data
    } else if (response.data && response.data.data) {
      records = response.data.data
    } else if (response.data && 'success' in response.data) {
      records = response.data.data || []
    }

    // 只加载与当前选择货币相同的记录
    records.filter(r => r.currency === selectedCurrency.value).forEach(record => {
      categoryAmounts.value[record.minorCategoryId] = record.amount
      categoryRecordIds.value[record.minorCategoryId] = record.id // 保存记录ID用于删除
    })
  } catch (error) {
    console.error('加载本月数据失败:', error)
  }
}

// 保存全部
async function saveAll() {
  // 分离需要保存的记录和需要删除的记录
  const recordsToSave = []
  const recordsToDelete = []

  Array.from(changedRecords.value).forEach(categoryId => {
    const amount = parseFloat(categoryAmounts.value[categoryId])
    const category = allMinorCategories.value.find(c => c.id === categoryId)

    if (!amount || amount <= 0) {
      // 金额为0，标记为删除
      recordsToDelete.push(categoryId)
    } else {
      // 金额大于0，正常保存
      recordsToSave.push({
        minorCategoryId: categoryId,
        amount: amount,
        currency: selectedCurrency.value,
        expenseType: category?.expenseType || 'FIXED_DAILY',
        description: ''
      })
    }
  })

  if (recordsToSave.length === 0 && recordsToDelete.length === 0) {
    alert('没有需要保存的记录')
    return
  }

  saving.value = true
  try {
    let saveCount = 0
    let deleteCount = 0

    // 1. 保存有金额的记录
    if (recordsToSave.length > 0) {
      const saveResponse = await expenseRecordAPI.batchSave({
        familyId: selectedFamilyId.value,
        expensePeriod: recordPeriod.value,
        records: recordsToSave
      })

      if (saveResponse.data?.success !== false) {
        saveCount = recordsToSave.length
      } else {
        alert(`保存失败: ${saveResponse.data?.message || '未知错误'}`)
        saving.value = false
        return
      }
    }

    // 2. 删除金额为0的记录
    if (recordsToDelete.length > 0) {
      // 查找需要删除的记录ID
      const recordIdsToDelete = recordsToDelete
        .map(categoryId => categoryRecordIds.value[categoryId])
        .filter(id => id) // 过滤掉没有现有记录的

      // 逐个删除
      for (const recordId of recordIdsToDelete) {
        try {
          await expenseRecordAPI.delete(recordId)
          deleteCount++
        } catch (err) {
          console.error('删除记录失败:', err)
        }
      }
    }

    // 显示结果
    const messages = []
    if (saveCount > 0) messages.push(`保存${saveCount}条`)
    if (deleteCount > 0) messages.push(`删除${deleteCount}条`)

    alert(`操作成功：${messages.join('，')}`)
    changedRecords.value.clear()
    await loadHistoryData()
    await loadCurrentMonthData()

  } catch (error) {
    console.error('保存失败:', error)
    alert('保存失败，请重试')
  } finally {
    saving.value = false
  }
}


// 监听期间变化
watch(recordPeriod, () => {
  categoryAmounts.value = {}
  categoryRecordIds.value = {}
  historyData.value = {}
  changedRecords.value.clear()
  loadHistoryData()
  loadCurrentMonthData()
})

// 监听家庭和货币变化
watch([selectedFamilyId, selectedCurrency], () => {
  categoryAmounts.value = {}
  categoryRecordIds.value = {}
  historyData.value = {}
  changedRecords.value.clear()
  loadHistoryData()
  loadCurrentMonthData()
})

// 初始化
onMounted(() => {
  loadFamilies()
  loadCurrencies()
  loadCategories()
})
</script>
