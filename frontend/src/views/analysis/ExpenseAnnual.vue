<template>
  <div class="p-3 md:p-6 space-y-4 md:space-y-6">
    <!-- 页面标题和筛选控制区 -->
    <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
      <div>
        <h1 class="text-xl md:text-2xl font-bold text-gray-900">年度支出分析</h1>
        <p class="text-xs md:text-sm text-gray-600 mt-1">分析年度支出结构和趋势</p>
      </div>
      <div class="flex flex-wrap items-center gap-2">
        <label class="text-sm font-medium text-gray-700 whitespace-nowrap">家庭:</label>
        <select
          v-model="selectedFamilyId"
          class="px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-primary bg-white text-sm"
        >
          <option v-for="family in families" :key="family.id" :value="family.id">
            {{ family.familyName }}
          </option>
        </select>

        <label class="text-sm font-medium text-gray-700 whitespace-nowrap">年份:</label>
        <select
          v-model="selectedYear"
          class="px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-primary bg-white text-sm"
        >
          <option v-for="year in availableYears" :key="year" :value="year">
            {{ year }}年
          </option>
        </select>

        <label class="text-sm font-medium text-gray-700 whitespace-nowrap">币种:</label>
        <select
          v-model="selectedCurrency"
          class="px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-primary bg-white text-sm"
        >
          <option v-for="currency in currencies" :key="currency" :value="currency">
            {{ currency }}
          </option>
        </select>
      </div>
    </div>

    <!-- 支出总览汇总卡片 -->
    <div v-if="majorCategoryData.length > 0" class="bg-gradient-to-r from-red-50 to-orange-50 rounded-lg shadow border border-red-200 p-4">
      <h3 class="text-base font-semibold text-gray-900 mb-3">支出总览</h3>
      <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3">
        <div class="bg-white rounded-lg p-3 shadow-sm">
          <div class="text-xs text-gray-600 mb-1">总预算</div>
          <div class="text-lg font-bold text-gray-900">{{ formatCurrency(totalBudget) }}</div>
        </div>
        <div class="bg-white rounded-lg p-3 shadow-sm">
          <div class="text-xs text-gray-600 mb-1">总支出</div>
          <div class="text-lg font-bold text-gray-900">{{ formatCurrency(totalExpense) }}</div>
        </div>
        <div class="bg-white rounded-lg p-3 shadow-sm">
          <div class="text-xs text-gray-600 mb-1">预算执行率</div>
          <div class="text-lg font-bold" :class="totalBudgetExecutionRate <= 100 ? 'text-green-600' : 'text-red-600'">
            {{ totalBudgetExecutionRate.toFixed(1) }}%
          </div>
        </div>
        <div class="bg-white rounded-lg p-3 shadow-sm">
          <div class="text-xs text-gray-600 mb-1">平均月支出</div>
          <div class="text-lg font-bold text-blue-600">{{ formatCurrency(totalExpense / 12) }}</div>
        </div>
        <div class="bg-white rounded-lg p-3 shadow-sm">
          <div class="text-xs text-gray-600 mb-1">{{ selectedYear - 1 }}年支出</div>
          <div class="text-lg font-bold text-gray-700">{{ formatCurrency(lastYearTotalExpense) }}</div>
        </div>
        <div class="bg-white rounded-lg p-3 shadow-sm">
          <div class="text-xs text-gray-600 mb-1">同比增长</div>
          <div class="text-lg font-bold" :class="yearOverYearGrowth >= 0 ? 'text-red-600' : 'text-green-600'">
            {{ yearOverYearGrowth >= 0 ? '+' : '' }}{{ yearOverYearGrowth.toFixed(1) }}%
          </div>
        </div>
      </div>
    </div>

    <!-- 大类分布：饼图和表格横向排列 -->
    <div class="bg-white rounded-lg shadow border border-gray-200 p-4">
      <h3 class="text-base font-semibold text-gray-900 mb-3">大类分布</h3>

      <div class="flex flex-col lg:flex-row gap-4">
        <!-- 左侧：饼图 -->
        <div class="flex-1 w-full lg:w-1/2 h-[500px]">
          <canvas ref="majorCategoryChartCanvas" v-if="majorCategoryData.length > 0"></canvas>
          <div v-else class="h-full flex items-center justify-center text-gray-500 text-sm">
            暂无数据
          </div>
        </div>

        <!-- 右侧：表格 -->
        <div class="flex-1 w-full lg:w-1/2 overflow-y-auto max-h-[500px]">
          <table v-if="majorCategoryData.length > 0" class="w-full text-xs">
            <thead class="bg-gray-50 sticky top-0">
              <tr>
                <th class="px-1.5 py-1.5 text-left font-medium text-gray-700 uppercase tracking-tight text-[10px]">分类</th>
                <th class="px-1.5 py-1.5 text-right font-medium text-gray-700 uppercase tracking-tight text-[10px]">预算</th>
                <th class="px-1.5 py-1.5 text-right font-medium text-gray-700 uppercase tracking-tight text-[10px]">实际</th>
                <th class="px-1.5 py-1.5 text-right font-medium text-gray-700 uppercase tracking-tight text-[10px]">剩余</th>
                <th class="px-1.5 py-1.5 text-right font-medium text-gray-700 uppercase tracking-tight text-[10px]">去年</th>
                <th class="px-1.5 py-1.5 text-right font-medium text-gray-700 uppercase tracking-tight text-[10px]">同比</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-200">
              <tr
                v-for="item in majorCategoryData"
                :key="item.majorCategoryId"
                @click="selectMajorCategory(item)"
                :class="[
                  'cursor-pointer transition-colors',
                  selectedMajorCategoryId === item.majorCategoryId
                    ? 'bg-primary/10'
                    : 'hover:bg-gray-50'
                ]"
              >
                <td class="px-1.5 py-1.5">
                  <div class="flex items-center gap-1">
                    <span class="text-sm">{{ item.majorCategoryIcon }}</span>
                    <span class="font-medium text-gray-900 text-[11px]">{{ item.majorCategoryName }}</span>
                  </div>
                </td>
                <td class="px-1.5 py-1.5 text-right text-gray-600 whitespace-nowrap text-[11px]">
                  {{ formatCurrency(item.budgetAmount || 0) }}
                </td>
                <td class="px-1.5 py-1.5 text-right font-medium text-gray-900 whitespace-nowrap text-[11px]">
                  {{ formatCurrency(item.totalAmount) }}
                </td>
                <td class="px-1.5 py-1.5 text-right font-semibold whitespace-nowrap text-[11px]"
                    :class="(item.budgetAmount - item.totalAmount) >= 0 ? 'text-green-600' : 'text-red-600'">
                  {{ formatCurrency((item.budgetAmount || 0) - item.totalAmount) }}
                </td>
                <td class="px-1.5 py-1.5 text-right text-gray-600 whitespace-nowrap text-[11px]">
                  {{ formatCurrency(getLastYearExpense(item.majorCategoryId)) }}
                </td>
                <td class="px-1.5 py-1.5 text-right font-semibold whitespace-nowrap text-[11px]"
                    :class="getCategoryYearOverYearGrowth(item.majorCategoryId, item.totalAmount) >= 0 ? 'text-red-600' : 'text-green-600'">
                  {{ getCategoryYearOverYearGrowth(item.majorCategoryId, item.totalAmount) >= 0 ? '+' : '' }}{{ getCategoryYearOverYearGrowth(item.majorCategoryId, item.totalAmount).toFixed(1) }}%
                </td>
              </tr>
            </tbody>
          </table>
          <div v-else class="text-center py-8 text-gray-500 text-sm">
            暂无大类分布数据
          </div>
        </div>
      </div>
    </div>

    <!-- 钻取区域：小类分布和月度趋势 -->
    <div v-if="selectedMajorCategoryId" class="grid grid-cols-1 lg:grid-cols-2 gap-4">
      <!-- 小类分布 -->
      <div class="bg-white rounded-lg shadow border border-gray-200 p-4">
        <h3 class="text-base font-semibold text-gray-900 mb-3">
          {{ selectedMajorCategoryName }} - 小类分布
        </h3>

        <!-- 饼图和表格横向排列 -->
        <div class="flex flex-col gap-3">
          <!-- 小类饼图 -->
          <div class="w-full h-72">
            <canvas ref="minorCategoryChartCanvas" v-if="minorCategoryData.length > 0"></canvas>
            <div v-else class="h-full flex items-center justify-center text-gray-500 text-sm">
              暂无小类数据
            </div>
          </div>

          <!-- 小类表格 -->
          <div class="overflow-auto max-h-64">
            <table v-if="minorCategoryData.length > 0" class="min-w-full text-xs">
              <thead class="bg-gray-50 sticky top-0">
                <tr>
                  <th class="px-2 py-1 text-left font-medium text-gray-700 uppercase tracking-tight">子分类</th>
                  <th class="px-2 py-1 text-center font-medium text-gray-700 uppercase tracking-tight">类型</th>
                  <th class="px-2 py-1 text-right font-medium text-gray-700 uppercase tracking-tight">预算</th>
                  <th class="px-2 py-1 text-right font-medium text-gray-700 uppercase tracking-tight">实际</th>
                  <th class="px-2 py-1 text-right font-medium text-gray-700 uppercase tracking-tight">去年</th>
                  <th class="px-2 py-1 text-right font-medium text-gray-700 uppercase tracking-tight">同比</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-200">
                <tr
                  v-for="item in minorCategoryData"
                  :key="item.minorCategoryId"
                  @click="selectMinorCategory(item)"
                  :class="[
                    'cursor-pointer transition-colors',
                    selectedMinorCategoryId === item.minorCategoryId
                      ? 'bg-primary/10'
                      : 'hover:bg-gray-50'
                  ]"
                >
                  <td class="px-2 py-1">
                    <div class="font-medium text-gray-900">{{ item.minorCategoryName }}</div>
                  </td>
                  <td class="px-2 py-1 text-center">
                    <span
                      :class="{
                        'px-2 py-0.5 rounded-full text-xs': true,
                        'bg-green-100 text-green-700': item.expenseType === 'FIXED_DAILY',
                        'bg-orange-100 text-orange-700': item.expenseType === 'LARGE_IRREGULAR'
                      }"
                    >
                      {{ item.expenseType === 'FIXED_DAILY' ? '固定' : '不定期' }}
                    </span>
                  </td>
                  <td class="px-2 py-1 text-right text-gray-600 whitespace-nowrap">
                    {{ formatCurrency(item.budgetAmount || 0) }}
                  </td>
                  <td class="px-2 py-1 text-right font-medium text-gray-900 whitespace-nowrap">
                    {{ formatCurrency(item.totalAmount) }}
                  </td>
                  <td class="px-2 py-1 text-right text-gray-600 whitespace-nowrap">
                    {{ formatCurrency(getLastYearMinorExpense(item.minorCategoryId)) }}
                  </td>
                  <td class="px-2 py-1 text-right font-semibold whitespace-nowrap"
                      :class="getMinorCategoryYearOverYearGrowth(item.minorCategoryId, item.totalAmount) >= 0 ? 'text-red-600' : 'text-green-600'">
                    {{ getMinorCategoryYearOverYearGrowth(item.minorCategoryId, item.totalAmount) >= 0 ? '+' : '' }}{{ getMinorCategoryYearOverYearGrowth(item.minorCategoryId, item.totalAmount).toFixed(1) }}%
                  </td>
                </tr>
              </tbody>
              <tfoot class="bg-blue-50 border-t-2 border-blue-200">
                <tr class="font-bold">
                  <td class="px-2 py-1.5 text-left text-gray-900" colspan="2">总计</td>
                  <td class="px-2 py-1.5 text-right text-gray-900 whitespace-nowrap">
                    {{ formatCurrency(minorCategoryBudgetTotal) }}
                  </td>
                  <td class="px-2 py-1.5 text-right text-gray-900 whitespace-nowrap">
                    {{ formatCurrency(minorCategoryTotal) }}
                  </td>
                  <td class="px-2 py-1.5 text-right text-gray-900 whitespace-nowrap" colspan="2">
                    -
                  </td>
                </tr>
              </tfoot>
            </table>
          </div>
        </div>
      </div>

      <!-- 月度趋势 -->
      <div
        v-if="selectedMinorCategoryId"
        class="bg-white rounded-lg shadow border border-gray-200 p-4"
      >
        <h3 class="text-base font-semibold text-gray-900 mb-2">
          {{ selectedMinorCategoryName }} - 月度趋势
        </h3>

        <div class="grid grid-cols-3 gap-2 mb-3 text-xs">
          <div class="bg-gray-50 px-2 py-1.5 rounded">
            <div class="text-gray-600">年度总计</div>
            <div class="font-medium text-gray-900">{{ formatCurrency(monthlyTrendTotal) }}</div>
          </div>
          <div class="bg-gray-50 px-2 py-1.5 rounded">
            <div class="text-gray-600">平均月支出</div>
            <div class="font-medium text-gray-600">{{ formatCurrency(monthlyTrendTotal / 12) }}</div>
          </div>
          <div class="bg-gray-50 px-2 py-1.5 rounded">
            <div class="text-gray-600">最高月支出</div>
            <div class="font-semibold text-red-600">{{ formatCurrency(maxMonthlyExpense) }}</div>
          </div>
        </div>

        <div v-if="monthlyTrendData.length > 0" class="h-64">
          <canvas ref="monthlyTrendChartCanvas"></canvas>
        </div>
        <div v-else class="h-64 flex items-center justify-center text-gray-500 text-sm">
          暂无月度趋势数据
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { Chart, registerables } from 'chart.js'
import ChartDataLabels from 'chartjs-plugin-datalabels'
import { familyAPI } from '@/api/family'
import { exchangeRateAPI } from '@/api/exchangeRate'
import { expenseAnalysisAPI, expenseCategoryAPI } from '@/api/expense'

Chart.register(...registerables, ChartDataLabels)

export default {
  name: 'ExpenseAnnual',
  setup() {
    // 响应式数据
    const families = ref([])
    const selectedFamilyId = ref(null)
    const selectedYear = ref(new Date().getFullYear())
    const currencies = ref(['All'])
    const selectedCurrency = ref('USD')
    const loading = ref(false)

    // 大类数据
    const majorCategoryData = ref([])
    const selectedMajorCategoryId = ref(null)
    const selectedMajorCategoryName = ref('')

    // 小类数据
    const minorCategoryData = ref([])
    const selectedMinorCategoryId = ref(null)
    const selectedMinorCategoryName = ref('')

    // 月度趋势数据
    const monthlyTrendData = ref([])

    // 预算数据
    const budgetData = ref([])

    // 上一年数据
    const lastYearTotalExpense = ref(0)
    const lastYearMajorCategoryData = ref([])
    const lastYearMinorCategoryData = ref([])

    // Chart实例
    const majorCategoryChart = ref(null)
    const minorCategoryChart = ref(null)
    const monthlyTrendChart = ref(null)

    // Canvas refs
    const majorCategoryChartCanvas = ref(null)
    const minorCategoryChartCanvas = ref(null)
    const monthlyTrendChartCanvas = ref(null)

    // 计算属性
    const availableYears = computed(() => {
      const currentYear = new Date().getFullYear()
      const years = []
      for (let year = currentYear; year >= currentYear - 10; year--) {
        years.push(year)
      }
      return years
    })

    const totalExpense = computed(() => {
      return majorCategoryData.value.reduce((sum, item) => sum + parseFloat(item.totalAmount), 0)
    })

    const minorCategoryTotal = computed(() => {
      return minorCategoryData.value.reduce((sum, item) => sum + parseFloat(item.totalAmount), 0)
    })

    const monthlyTrendTotal = computed(() => {
      return monthlyTrendData.value.reduce((sum, item) => sum + parseFloat(item.amount || 0), 0)
    })

    const maxMonthlyExpense = computed(() => {
      if (monthlyTrendData.value.length === 0) return 0
      return Math.max(...monthlyTrendData.value.map(item => parseFloat(item.amount || 0)))
    })

    const totalBudget = computed(() => {
      return budgetData.value.reduce((sum, item) => sum + parseFloat(item.budgetAmount || 0), 0)
    })

    const totalBudgetExecutionRate = computed(() => {
      if (totalBudget.value === 0) return 0
      return (totalExpense.value / totalBudget.value) * 100
    })

    // 同比增长率（总体）
    const yearOverYearGrowth = computed(() => {
      if (lastYearTotalExpense.value === 0) return 0
      return ((totalExpense.value - lastYearTotalExpense.value) / lastYearTotalExpense.value) * 100
    })

    // 获取指定大类的上一年支出
    const getLastYearExpense = (majorCategoryId) => {
      const lastYearCategory = lastYearMajorCategoryData.value.find(
        item => item.majorCategoryId === majorCategoryId
      )
      return lastYearCategory ? parseFloat(lastYearCategory.totalAmount || 0) : 0
    }

    // 计算指定大类的同比增长率
    const getCategoryYearOverYearGrowth = (majorCategoryId, currentAmount) => {
      const lastYearAmount = getLastYearExpense(majorCategoryId)
      if (lastYearAmount === 0) return 0
      return ((parseFloat(currentAmount) - lastYearAmount) / lastYearAmount) * 100
    }

    // 获取指定小类的上一年支出
    const getLastYearMinorExpense = (minorCategoryId) => {
      const lastYearCategory = lastYearMinorCategoryData.value.find(
        item => item.minorCategoryId === minorCategoryId
      )
      return lastYearCategory ? parseFloat(lastYearCategory.totalAmount || 0) : 0
    }

    // 计算指定小类的同比增长率
    const getMinorCategoryYearOverYearGrowth = (minorCategoryId, currentAmount) => {
      const lastYearAmount = getLastYearMinorExpense(minorCategoryId)
      if (lastYearAmount === 0) return 0
      return ((parseFloat(currentAmount) - lastYearAmount) / lastYearAmount) * 100
    }

    const minorCategoryBudgetTotal = computed(() => {
      return minorCategoryData.value.reduce((sum, item) => sum + parseFloat(item.budgetAmount || 0), 0)
    })

    // 加载家庭列表
    const loadFamilies = async () => {
      try {
        const response = await familyAPI.getDefault()

        // getDefault() 返回单个家庭对象，需要包装成数组
        if (response && response.success && response.data && response.data.id) {
          families.value = [response.data]

          // 设置默认选中
          if (!selectedFamilyId.value) {
            selectedFamilyId.value = response.data.id
          }
        } else {
          families.value = []
          console.error('获取默认家庭失败: 返回数据格式错误', response)
        }
      } catch (error) {
        console.error('加载家庭列表失败:', error)
      }
    }

    // 加载货币列表
    const loadCurrencies = async () => {
      try {
        const response = await exchangeRateAPI.getAllActive()
        let data = response.data

        let rates = []
        if (Array.isArray(data)) {
          rates = data
        } else if (data && data.data && Array.isArray(data.data)) {
          rates = data.data
        } else if (data && data.success && Array.isArray(data.data)) {
          rates = data.data
        }

        const currencySet = new Set(['All'])
        rates.forEach(rate => currencySet.add(rate.currency))
        currencySet.add('USD')

        currencies.value = Array.from(currencySet).sort()
      } catch (error) {
        console.error('加载货币列表失败:', error)
      }
    }

    // 加载预算数据
    const loadBudgetData = async () => {
      if (!selectedFamilyId.value) return

      try {
        const response = await expenseAnalysisAPI.getBudgetExecution(
          selectedFamilyId.value,
          selectedYear.value,
          selectedCurrency.value
        )

        if (response && response.success) {
          budgetData.value = response.data || []
        } else {
          budgetData.value = []
        }
      } catch (error) {
        console.error('加载预算数据失败:', error)
        budgetData.value = []
      }
    }

    // 加载大类汇总数据
    const loadMajorCategoryData = async () => {
      if (!selectedFamilyId.value) return

      loading.value = true
      try {
        // 1. 先获取所有大类分类
        const categoriesResponse = await expenseCategoryAPI.getAll()
        const allMajorCategories = categoriesResponse.data || []

        // 2. 并行加载预算数据、当年支出数据和上一年支出数据
        let currentYearExpenseData = []
        await Promise.all([
          loadBudgetData(),
          expenseAnalysisAPI.getAnnualMajorCategories(
            selectedFamilyId.value,
            selectedYear.value,
            selectedCurrency.value
          ).then(response => {
            if (response && response.success) {
              currentYearExpenseData = response.data || []
            }
          })
        ])

        // 3. 加载上一年支出数据
        let lastYearExpenseData = []
        try {
          const lastYearResponse = await expenseAnalysisAPI.getAnnualMajorCategories(
            selectedFamilyId.value,
            selectedYear.value - 1,
            selectedCurrency.value
          )

          if (lastYearResponse && lastYearResponse.success) {
            lastYearExpenseData = lastYearResponse.data || []
            // 保存上一年大类数据（用于表格显示）
            lastYearMajorCategoryData.value = lastYearExpenseData
            // 计算上一年总支出
            lastYearTotalExpense.value = lastYearExpenseData.reduce((sum, item) => sum + parseFloat(item.totalAmount || 0), 0)
          } else {
            lastYearMajorCategoryData.value = []
            lastYearTotalExpense.value = 0
          }
        } catch (error) {
          console.error('加载上一年数据失败:', error)
          lastYearMajorCategoryData.value = []
          lastYearTotalExpense.value = 0
        }

        // 4. 创建当年支出数据的映射（按大类ID索引）
        const currentYearExpenseMap = {}
        currentYearExpenseData.forEach(item => {
          currentYearExpenseMap[item.majorCategoryId] = item
        })

        // 5. 将所有大类和支出数据合并
        majorCategoryData.value = allMajorCategories.map(major => {
          const expenseItem = currentYearExpenseMap[major.id]
          return {
            majorCategoryId: major.id,
            majorCategoryName: major.name,
            majorCategoryIcon: major.icon,
            majorCategoryCode: major.code,
            totalAmount: expenseItem ? expenseItem.totalAmount : 0,
            currency: expenseItem ? expenseItem.currency : selectedCurrency.value
          }
        })

        // 6. 将预算数据合并到大类数据中
        mergeBudgetToMajorCategories()

        // 清空小类和月度数据
        selectedMajorCategoryId.value = null
        selectedMinorCategoryId.value = null
        minorCategoryData.value = []
        monthlyTrendData.value = []

        // 更新大类饼图
        await nextTick()
        updateMajorCategoryChart()
      } catch (error) {
        console.error('加载大类数据失败:', error)
        majorCategoryData.value = []
        lastYearTotalExpense.value = 0
        lastYearMajorCategoryData.value = []
      } finally {
        loading.value = false
      }
    }

    // 将预算数据合并到大类数据
    const mergeBudgetToMajorCategories = () => {
      // 按大类汇总预算
      const budgetByMajorCategory = {}
      budgetData.value.forEach(item => {
        const majorId = item.majorCategoryId
        if (!budgetByMajorCategory[majorId]) {
          budgetByMajorCategory[majorId] = 0
        }
        budgetByMajorCategory[majorId] += parseFloat(item.budgetAmount || 0)
      })

      // 合并到大类数据
      majorCategoryData.value = majorCategoryData.value.map(item => ({
        ...item,
        budgetAmount: budgetByMajorCategory[item.majorCategoryId] || 0
      }))
    }

    // 选择大类
    const selectMajorCategory = async (item) => {
      selectedMajorCategoryId.value = item.majorCategoryId
      selectedMajorCategoryName.value = item.majorCategoryName

      // 清空小类选择
      selectedMinorCategoryId.value = null
      monthlyTrendData.value = []

      try {
        // 并行加载当年和上一年的小类数据
        const [currentYearResponse, lastYearResponse] = await Promise.all([
          expenseAnalysisAPI.getAnnualMinorCategories(
            selectedFamilyId.value,
            selectedYear.value,
            item.majorCategoryId,
            selectedCurrency.value
          ),
          expenseAnalysisAPI.getAnnualMinorCategories(
            selectedFamilyId.value,
            selectedYear.value - 1,
            item.majorCategoryId,
            selectedCurrency.value
          ).catch(error => {
            console.error('加载上一年小类数据失败:', error)
            return { success: false, data: [] }
          })
        ])

        if (currentYearResponse && currentYearResponse.success) {
          minorCategoryData.value = currentYearResponse.data || []
          // 将预算数据合并到小类数据
          mergeBudgetToMinorCategories(item.majorCategoryId)
        } else {
          minorCategoryData.value = []
        }

        // 保存上一年小类数据
        if (lastYearResponse && lastYearResponse.success) {
          lastYearMinorCategoryData.value = lastYearResponse.data || []
        } else {
          lastYearMinorCategoryData.value = []
        }

        // 更新小类饼图
        await nextTick()
        updateMinorCategoryChart()
      } catch (error) {
        console.error('加载小类数据失败:', error)
        minorCategoryData.value = []
        lastYearMinorCategoryData.value = []
      }
    }

    // 将预算数据合并到小类数据
    const mergeBudgetToMinorCategories = (majorCategoryId) => {
      // 创建预算映射（按小类ID）
      const budgetByMinorCategory = {}
      budgetData.value
        .filter(item => item.majorCategoryId === majorCategoryId)
        .forEach(item => {
          budgetByMinorCategory[item.minorCategoryId] = parseFloat(item.budgetAmount || 0)
        })

      // 合并到小类数据
      minorCategoryData.value = minorCategoryData.value.map(item => ({
        ...item,
        budgetAmount: budgetByMinorCategory[item.minorCategoryId] || 0
      }))
    }

    // 选择小类
    const selectMinorCategory = async (item) => {
      selectedMinorCategoryId.value = item.minorCategoryId
      selectedMinorCategoryName.value = item.minorCategoryName

      try {
        const response = await expenseAnalysisAPI.getAnnualMonthlyTrend(
          selectedFamilyId.value,
          selectedYear.value,
          item.minorCategoryId,
          selectedCurrency.value
        )

        if (response && response.success) {
          monthlyTrendData.value = response.data || []
        } else {
          monthlyTrendData.value = []
        }

        // 更新月度趋势图
        await nextTick()
        updateMonthlyTrendChart()
      } catch (error) {
        console.error('加载月度趋势失败:', error)
        monthlyTrendData.value = []
      }
    }

    // 更新大类饼图
    const updateMajorCategoryChart = () => {
      if (!majorCategoryChartCanvas.value || majorCategoryData.value.length === 0) return

      if (majorCategoryChart.value) {
        majorCategoryChart.value.destroy()
      }

      const ctx = majorCategoryChartCanvas.value.getContext('2d')
      const colors = [
        '#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6',
        '#ec4899', '#14b8a6', '#f97316', '#06b6d4', '#84cc16'
      ]

      // 只在饼图中显示有支出的大类（排除零支出类别以获得更清晰的可视化）
      const nonZeroCategories = majorCategoryData.value.filter(d => parseFloat(d.totalAmount) > 0)

      majorCategoryChart.value = new Chart(ctx, {
        type: 'pie',
        data: {
          labels: nonZeroCategories.map(d => d.majorCategoryName),
          datasets: [{
            data: nonZeroCategories.map(d => d.totalAmount),
            backgroundColor: colors
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: {
              display: true,
              position: 'bottom'
            },
            tooltip: {
              callbacks: {
                label: function(context) {
                  const label = context.label || ''
                  const value = context.parsed || 0
                  const total = context.dataset.data.reduce((a, b) => a + b, 0)
                  const percentage = ((value / total) * 100).toFixed(1)
                  return `${label}: ${formatCurrency(value)} (${percentage}%)`
                }
              }
            },
            datalabels: {
              color: '#fff',
              font: {
                weight: 'bold',
                size: 11
              },
              formatter: (value, context) => {
                const label = nonZeroCategories[context.dataIndex].majorCategoryName
                const percentage = ((value / totalExpense.value) * 100).toFixed(1)
                // 只显示占比大于5%的标签
                if (percentage >= 5) {
                  return `${label}-${percentage}%`
                }
                return ''
              },
              textAlign: 'center'
            }
          }
        }
      })
    }

    // 更新小类饼图
    const updateMinorCategoryChart = () => {
      if (!minorCategoryChartCanvas.value || minorCategoryData.value.length === 0) return

      if (minorCategoryChart.value) {
        minorCategoryChart.value.destroy()
      }

      const ctx = minorCategoryChartCanvas.value.getContext('2d')
      const colors = [
        '#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6',
        '#ec4899', '#14b8a6', '#f97316', '#06b6d4', '#84cc16'
      ]

      minorCategoryChart.value = new Chart(ctx, {
        type: 'pie',
        data: {
          labels: minorCategoryData.value.map(d => d.minorCategoryName),
          datasets: [{
            data: minorCategoryData.value.map(d => d.totalAmount),
            backgroundColor: colors
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: {
              display: true,
              position: 'bottom'
            },
            tooltip: {
              callbacks: {
                label: function(context) {
                  const label = context.label || ''
                  const value = context.parsed || 0
                  const total = context.dataset.data.reduce((a, b) => a + b, 0)
                  const percentage = ((value / total) * 100).toFixed(1)
                  return `${label}: ${formatCurrency(value)} (${percentage}%)`
                }
              }
            },
            datalabels: {
              color: '#fff',
              font: {
                weight: 'bold',
                size: 10
              },
              formatter: (value, context) => {
                const label = minorCategoryData.value[context.dataIndex].minorCategoryName
                const percentage = ((value / minorCategoryTotal.value) * 100).toFixed(1)
                // 只显示占比大于5%的标签
                if (percentage >= 5) {
                  return `${label}-${percentage}%`
                }
                return ''
              },
              textAlign: 'center'
            }
          }
        }
      })
    }

    // 更新月度趋势图
    const updateMonthlyTrendChart = () => {
      if (!monthlyTrendChartCanvas.value || monthlyTrendData.value.length === 0) return

      if (monthlyTrendChart.value) {
        monthlyTrendChart.value.destroy()
      }

      // 准备12个月的数据
      const months = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月']
      const amounts = new Array(12).fill(0)

      monthlyTrendData.value.forEach(item => {
        const monthIndex = item.month - 1
        if (monthIndex >= 0 && monthIndex < 12) {
          amounts[monthIndex] = item.amount || 0
        }
      })

      const ctx = monthlyTrendChartCanvas.value.getContext('2d')
      monthlyTrendChart.value = new Chart(ctx, {
        type: 'bar',
        data: {
          labels: months,
          datasets: [{
            label: '支出金额',
            data: amounts,
            backgroundColor: '#ef4444',
            borderColor: '#ef4444',
            borderWidth: 1
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: {
              display: false
            },
            tooltip: {
              callbacks: {
                label: function(context) {
                  return `金额: ${formatCurrency(context.parsed.y)}`
                }
              }
            }
          },
          scales: {
            y: {
              beginAtZero: true,
              title: {
                display: true,
                text: '金额'
              },
              ticks: {
                callback: function(value) {
                  return formatCurrency(value)
                }
              }
            },
            x: {
              title: {
                display: true,
                text: '月份'
              }
            }
          }
        }
      })
    }

    // 格式化金额
    const formatCurrency = (amount) => {
      const symbol = getCurrencySymbol()
      return symbol + parseFloat(amount).toLocaleString('en-US', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
      })
    }

    // 获取货币符号
    const getCurrencySymbol = () => {
      const symbols = {
        'All': '$',
        'USD': '$',
        'CNY': '¥',
        'EUR': '€',
        'GBP': '£',
        'JPY': '¥',
        'AUD': 'A$',
        'CAD': 'C$'
      }
      return symbols[selectedCurrency.value] || '$'
    }

    // 监听选项变化
    watch([selectedFamilyId, selectedYear, selectedCurrency], () => {
      loadMajorCategoryData()
    })

    // 组件挂载时
    onMounted(async () => {
      await loadFamilies()
      await loadCurrencies()
      await loadMajorCategoryData()
    })

    return {
      families,
      selectedFamilyId,
      selectedYear,
      availableYears,
      currencies,
      selectedCurrency,
      loading,
      majorCategoryData,
      minorCategoryData,
      monthlyTrendData,
      selectedMajorCategoryId,
      selectedMajorCategoryName,
      selectedMinorCategoryId,
      selectedMinorCategoryName,
      totalExpense,
      minorCategoryTotal,
      monthlyTrendTotal,
      maxMonthlyExpense,
      totalBudget,
      totalBudgetExecutionRate,
      minorCategoryBudgetTotal,
      lastYearTotalExpense,
      lastYearMajorCategoryData,
      yearOverYearGrowth,
      getLastYearExpense,
      getCategoryYearOverYearGrowth,
      getLastYearMinorExpense,
      getMinorCategoryYearOverYearGrowth,
      majorCategoryChartCanvas,
      minorCategoryChartCanvas,
      monthlyTrendChartCanvas,
      selectMajorCategory,
      selectMinorCategory,
      formatCurrency
    }
  }
}
</script>

<style scoped>
/* 自定义样式 */
</style>
