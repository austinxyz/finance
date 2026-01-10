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
        <div class="flex items-center gap-2">
          <label class="text-xs font-medium text-gray-700 whitespace-nowrap">月份:</label>
          <input
            v-model="recordPeriod"
            type="month"
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
    <div v-if="!loading && filteredCategories.length > 0" class="bg-white rounded-lg shadow border border-gray-200 px-3 py-2">
      <div class="grid grid-cols-2 sm:grid-cols-4 gap-3">
        <!-- 前三个月总收入 -->
        <div>
          <div class="text-xs text-gray-600 mb-0.5">{{ previousMonth3 }}总收入</div>
          <div class="text-sm font-medium text-gray-700">
            {{ formatCurrency(historyMonthTotal3) }}
          </div>
        </div>
        <div>
          <div class="text-xs text-gray-600 mb-0.5">{{ previousMonth2 }}总收入</div>
          <div class="text-sm font-medium text-gray-700">
            {{ formatCurrency(historyMonthTotal2) }}
          </div>
        </div>
        <div>
          <div class="text-xs text-gray-600 mb-0.5">{{ previousMonth1 }}总收入</div>
          <div class="text-sm font-semibold text-gray-900">
            {{ formatCurrency(historyMonthTotal1) }}
          </div>
        </div>

        <!-- 本月总收入 -->
        <div>
          <div class="text-xs text-gray-600 mb-0.5">本月总收入</div>
          <div class="text-base font-bold text-green-600">{{ formatCurrency(summary.total) }}</div>
        </div>
      </div>
    </div>

    <!-- 收入列表 - 表格布局（带滚动条） -->
    <div class="bg-white rounded-lg shadow border border-gray-200">
      <div v-if="loading" class="text-center py-6 text-gray-500 text-xs">加载中...</div>
      <div v-else-if="filteredCategories.length === 0" class="text-center py-6 text-gray-500 text-xs">暂无子分类</div>
      <div v-else>
        <div class="overflow-x-auto max-h-[calc(100vh-280px)] overflow-y-auto">
          <table class="w-full">
            <thead class="bg-gray-50 sticky top-0">
              <tr>
                <th class="px-2 py-1.5 text-left text-xs font-medium text-gray-700">分类</th>
                <th class="px-2 py-1.5 text-right text-xs font-medium text-gray-700">{{ previousMonth3 }}</th>
                <th class="px-2 py-1.5 text-right text-xs font-medium text-gray-700">{{ previousMonth2 }}</th>
                <th class="px-2 py-1.5 text-right text-xs font-medium text-gray-700">{{ previousMonth1 }}</th>
                <th class="px-2 py-1.5 text-center text-xs font-medium text-gray-700">{{ currentMonth }}</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-200">
              <tr v-for="category in filteredCategories" :key="category.id" class="hover:bg-gray-50">
                <!-- 分类信息 -->
                <td class="px-2 py-1.5">
                  <div class="flex items-center gap-1.5">
                    <span class="text-base">{{ category.majorIcon }}</span>
                    <div class="flex items-center gap-1.5 text-xs">
                      <span class="font-medium text-gray-900">{{ category.majorName }} - {{ category.name }}</span>
                      <span v-if="category.userId" class="text-xs px-2 py-0.5 bg-blue-100 text-blue-700 rounded flex-shrink-0">
                        {{ getUserName(category.userId) }}
                      </span>
                    </div>
                  </div>
                </td>

                <!-- 历史数据 -->
                <td class="px-2 py-1.5 text-right text-xs text-gray-600 whitespace-nowrap">
                  {{ formatCurrency(historyData[category.id]?.month3 ?? 0) }}
                </td>
                <td class="px-2 py-1.5 text-right text-xs text-gray-600 whitespace-nowrap">
                  {{ formatCurrency(historyData[category.id]?.month2 ?? 0) }}
                </td>
                <td class="px-2 py-1.5 text-right text-xs text-gray-700 font-medium whitespace-nowrap">
                  {{ formatCurrency(historyData[category.id]?.month1 ?? 0) }}
                </td>

                <!-- 本月输入 -->
                <td class="px-2 py-1.5">
                  <div class="relative">
                    <span class="absolute left-1.5 top-1/2 -translate-y-1/2 text-gray-500 text-xs">{{ selectedCurrency === 'CNY' ? '¥' : '$' }}</span>
                    <input
                      v-model="categoryAmounts[category.id]"
                      type="number"
                      step="0.01"
                      min="0"
                      placeholder="0.00"
                      class="w-24 pl-5 pr-1.5 py-1 text-xs border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-primary text-right"
                      @input="markAsChanged(category.id)"
                    />
                  </div>
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
import { incomeCategoryAPI, incomeRecordAPI } from '@/api/income'
import { familyAPI } from '@/api/family'
import { userAPI } from '@/api/user'
import { exchangeRateAPI } from '@/api/exchangeRate'

const loading = ref(false)
const saving = ref(false)
const families = ref([])
const users = ref([])
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

// 过滤后的分类 - 按大类排序，同大类内有历史值的排在前面
const filteredCategories = computed(() => {
  return allMinorCategories.value.sort((a, b) => {
    // 1. 先按大类排序
    if (a.majorCategoryId !== b.majorCategoryId) {
      return a.majorCategoryId - b.majorCategoryId
    }

    // 2. 同大类内，检查是否有历史数据（前3个月任一有值即可）
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

  return { total }
})

// 前三个月的总收入统计
const historyMonthTotal3 = computed(() => {
  return filteredCategories.value.reduce((sum, c) => {
    return sum + (historyData.value[c.id]?.month3 || 0)
  }, 0)
})

const historyMonthTotal2 = computed(() => {
  return filteredCategories.value.reduce((sum, c) => {
    return sum + (historyData.value[c.id]?.month2 || 0)
  }, 0)
})

const historyMonthTotal1 = computed(() => {
  return filteredCategories.value.reduce((sum, c) => {
    return sum + (historyData.value[c.id]?.month1 || 0)
  }, 0)
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

// 获取用户名
function getUserName(userId) {
  const user = users.value.find(u => u.id === userId)
  return user ? user.username : '未知'
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
    const response = await familyAPI.getDefault()

    // getDefault() returns a single family object, wrap it in an array
    if (response.success && response.data) {
      families.value = [response.data]

      // Set the default family
      if (!selectedFamilyId.value) {
        selectedFamilyId.value = response.data.id
        await loadUsers(response.data.id)
      }
    }
  } catch (error) {
    console.error('加载家庭列表失败:', error)
  }
}

// 加载用户列表
async function loadUsers(familyId) {
  try {
    const response = await userAPI.getByFamily(familyId)
    users.value = response.data || []
  } catch (error) {
    console.error('加载用户列表失败:', error)
    users.value = []
  }
}

// 加载货币列表
async function loadCurrencies() {
  try {
    const response = await exchangeRateAPI.getAllActive()

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

// 加载大类
async function loadMajorCategories() {
  loading.value = true
  try {
    const response = await incomeCategoryAPI.getAllMajor()

    let categoryData = []
    if (Array.isArray(response.data)) {
      categoryData = response.data
    } else if (response.data && response.data.data) {
      categoryData = response.data.data
    } else if (response.data && 'success' in response.data) {
      categoryData = response.data.data || []
    }

    majorCategories.value = categoryData.filter(c => c.isActive)

    // 加载所有子分类
    await loadAllMinorCategories()
  } catch (error) {
    console.error('加载大类失败:', error)
    alert('加载分类失败，请刷新重试')
  } finally {
    loading.value = false
  }
}

// 加载所有子分类
async function loadAllMinorCategories() {
  if (!selectedFamilyId.value) return

  try {
    const promises = majorCategories.value.map(async (major) => {
      const response = await incomeCategoryAPI.getMinorByFamilyAndMajor(
        selectedFamilyId.value,
        major.id
      )

      let minorData = []
      if (Array.isArray(response.data)) {
        minorData = response.data
      } else if (response.data && response.data.data) {
        minorData = response.data.data
      } else if (response.data && 'success' in response.data) {
        minorData = response.data.data || []
      }

      return minorData.filter(m => m.isActive).map(minor => ({
        ...minor,
        majorCategoryId: major.id,
        majorName: major.chineseName,
        majorIcon: major.icon
      }))
    })

    const results = await Promise.all(promises)
    allMinorCategories.value = results.flat()

    // 加载前3个月历史数据
    await loadHistoryData()

    // 加载本月数据
    await loadCurrentMonthData()
  } catch (error) {
    console.error('加载子分类失败:', error)
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
        incomeRecordAPI.getByPeriod(selectedFamilyId.value, period)
          .catch(() => ({ data: [] }))
      )
    )

    // 清空历史数据
    historyData.value = {}

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
    const response = await incomeRecordAPI.getByPeriod(selectedFamilyId.value, recordPeriod.value)

    let records = []
    if (Array.isArray(response.data)) {
      records = response.data
    } else if (response.data && response.data.data) {
      records = response.data.data
    } else if (response.data && 'success' in response.data) {
      records = response.data.data || []
    }

    // 清空当前数据
    categoryAmounts.value = {}
    categoryRecordIds.value = {}

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
        majorCategoryId: category?.majorCategoryId,
        amount: amount,
        currency: selectedCurrency.value,
        assetAccountId: null,
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
      const saveResponse = await incomeRecordAPI.batchSave({
        familyId: selectedFamilyId.value,
        period: recordPeriod.value,
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
          await incomeRecordAPI.delete(recordId)
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

// 监听家庭变化
watch(selectedFamilyId, async (newId) => {
  if (newId) {
    await loadUsers(newId)
    categoryAmounts.value = {}
    categoryRecordIds.value = {}
    historyData.value = {}
    changedRecords.value.clear()
    // 重新加载小分类（因为不同家庭看到的小分类不同）
    await loadAllMinorCategories()
    loadHistoryData()
    loadCurrentMonthData()
  }
})

// 监听货币变化
watch(selectedCurrency, () => {
  categoryAmounts.value = {}
  categoryRecordIds.value = {}
  historyData.value = {}
  changedRecords.value.clear()
  loadHistoryData()
  loadCurrentMonthData()
})

// 初始化
onMounted(async () => {
  await loadFamilies()  // 等待家庭加载完成，确保 selectedFamilyId 有值
  loadCurrencies()
  loadMajorCategories()  // 这会调用 loadAllMinorCategories，此时 selectedFamilyId 已有值
})
</script>
