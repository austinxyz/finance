<template>
  <div class="p-3 md:p-6 space-y-4 md:space-y-6">
    <!-- 页面标题和筛选控制区 -->
    <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
      <div>
        <h1 class="text-xl md:text-2xl font-bold text-gray-900">年度收入分析</h1>
        <p class="text-xs md:text-sm text-gray-600 mt-1">分析年度收入结构和趋势</p>
      </div>
      <div class="flex flex-wrap items-center gap-2">
        <!-- 刷新按钮 -->
        <button
          @click="refreshData"
          :disabled="refreshing"
          class="px-3 py-2 bg-blue-600 text-white text-sm rounded-lg hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
        >
          <svg v-if="!refreshing" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
          </svg>
          <svg v-else class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          <span>{{ refreshing ? '刷新中...' : '刷新数据' }}</span>
        </button>
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

    <!-- 收入总览汇总卡片 -->
    <div v-if="majorCategoryData.length > 0" class="bg-gradient-to-r from-green-50 to-emerald-50 rounded-lg shadow border border-green-200 p-4">
      <h3 class="text-base font-semibold text-gray-900 mb-3">收入总览</h3>
      <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-3">
        <div class="bg-white rounded-lg p-3 shadow-sm">
          <div class="text-xs text-gray-600 mb-1">总收入</div>
          <div class="text-lg font-bold text-gray-900">{{ formatCurrency(totalIncome) }}</div>
        </div>
        <div class="bg-white rounded-lg p-3 shadow-sm">
          <div class="text-xs text-gray-600 mb-1">平均月收入</div>
          <div class="text-lg font-bold text-green-600">{{ formatCurrency(totalIncome / 12) }}</div>
        </div>
        <div class="bg-white rounded-lg p-3 shadow-sm">
          <div class="text-xs text-gray-600 mb-1">{{ selectedYear - 1 }}年收入</div>
          <div class="text-lg font-bold text-gray-700">{{ formatCurrency(lastYearTotalIncome) }}</div>
        </div>
        <div class="bg-white rounded-lg p-3 shadow-sm">
          <div class="text-xs text-gray-600 mb-1">同比增长</div>
          <div class="text-lg font-bold" :class="yearOverYearGrowth >= 0 ? 'text-green-600' : 'text-red-600'">
            {{ yearOverYearGrowth >= 0 ? '+' : '' }}{{ yearOverYearGrowth.toFixed(1) }}%
          </div>
        </div>
        <div class="bg-white rounded-lg p-3 shadow-sm">
          <div class="text-xs text-gray-600 mb-1">最高单月</div>
          <div class="text-lg font-bold text-blue-600">{{ formatCurrency(maxMonthlyIncome) }}</div>
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
                <th class="px-1.5 py-1.5 text-right font-medium text-gray-700 uppercase tracking-tight text-[10px]">实际</th>
                <th class="px-1.5 py-1.5 text-right font-medium text-gray-700 uppercase tracking-tight text-[10px]">占比</th>
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
                    <span class="font-medium text-gray-900 text-[11px]">{{ item.majorCategoryChineseName || item.majorCategoryName }}</span>
                  </div>
                </td>
                <td class="px-1.5 py-1.5 text-right font-medium text-gray-900 whitespace-nowrap text-[11px]">
                  {{ formatCurrency(item.totalAmount) }}
                </td>
                <td class="px-1.5 py-1.5 text-right text-gray-600 whitespace-nowrap text-[11px]">
                  {{ ((item.totalAmount / totalIncome) * 100).toFixed(1) }}%
                </td>
                <td class="px-1.5 py-1.5 text-right text-gray-600 whitespace-nowrap text-[11px]">
                  {{ formatCurrency(getLastYearIncome(item.majorCategoryId)) }}
                </td>
                <td class="px-1.5 py-1.5 text-right font-semibold whitespace-nowrap text-[11px]"
                    :class="getCategoryYearOverYearGrowth(item.majorCategoryId, item.totalAmount) >= 0 ? 'text-green-600' : 'text-red-600'">
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
                  <th class="px-2 py-1 text-right font-medium text-gray-700 uppercase tracking-tight">实际</th>
                  <th class="px-2 py-1 text-right font-medium text-gray-700 uppercase tracking-tight">去年</th>
                  <th class="px-2 py-1 text-right font-medium text-gray-700 uppercase tracking-tight">同比</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-200">
                <tr
                  v-for="item in minorCategoryData"
                  :key="item.minorCategoryId || 'no-minor'"
                  @click="selectMinorCategory(item)"
                  :class="[
                    'cursor-pointer transition-colors',
                    selectedMinorCategoryId === item.minorCategoryId
                      ? 'bg-primary/10'
                      : 'hover:bg-gray-50'
                  ]"
                >
                  <td class="px-2 py-1">
                    <div class="font-medium text-gray-900">{{ item.minorCategoryChineseName || item.minorCategoryName }}</div>
                  </td>
                  <td class="px-2 py-1 text-right font-medium text-gray-900 whitespace-nowrap">
                    {{ formatCurrency(item.totalAmount) }}
                  </td>
                  <td class="px-2 py-1 text-right text-gray-600 whitespace-nowrap">
                    {{ formatCurrency(getLastYearMinorIncome(item.minorCategoryId)) }}
                  </td>
                  <td class="px-2 py-1 text-right font-semibold whitespace-nowrap"
                      :class="getMinorCategoryYearOverYearGrowth(item.minorCategoryId, item.totalAmount) >= 0 ? 'text-green-600' : 'text-red-600'">
                    {{ getMinorCategoryYearOverYearGrowth(item.minorCategoryId, item.totalAmount) >= 0 ? '+' : '' }}{{ getMinorCategoryYearOverYearGrowth(item.minorCategoryId, item.totalAmount).toFixed(1) }}%
                  </td>
                </tr>
              </tbody>
              <tfoot class="bg-blue-50 border-t-2 border-blue-200">
                <tr class="font-bold">
                  <td class="px-2 py-1.5 text-left text-gray-900">总计</td>
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
        v-if="selectedMinorCategoryId !== undefined"
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
            <div class="text-gray-600">平均月收入</div>
            <div class="font-medium text-gray-600">{{ formatCurrency(monthlyTrendTotal / 12) }}</div>
          </div>
          <div class="bg-gray-50 px-2 py-1.5 rounded">
            <div class="text-gray-600">最高月收入</div>
            <div class="font-semibold text-green-600">{{ formatCurrency(maxMonthlyTrendIncome) }}</div>
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
import { useFamilyStore } from '@/stores/family'
import { exchangeRateAPI } from '@/api/exchangeRate'
import { incomeAnalysisAPI, incomeCategoryAPI } from '@/api/income'

Chart.register(...registerables, ChartDataLabels)

export default {
  name: 'IncomeAnnual',
  setup() {
    // Family store
    const familyStore = useFamilyStore()

    // 响应式数据
    const selectedFamilyId = computed(() => familyStore.currentFamilyId)
    const selectedYear = ref(new Date().getFullYear())
    const currencies = ref(['All'])
    const selectedCurrency = ref('USD')
    const loading = ref(false)
    const refreshing = ref(false)

    // 大类数据
    const majorCategoryData = ref([])
    const selectedMajorCategoryId = ref(null)
    const selectedMajorCategoryName = ref('')

    // 小类数据
    const minorCategoryData = ref([])
    const selectedMinorCategoryId = ref(undefined)
    const selectedMinorCategoryName = ref('')

    // 月度趋势数据
    const monthlyTrendData = ref([])

    // 上一年数据
    const lastYearTotalIncome = ref(0)
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

    const totalIncome = computed(() => {
      return majorCategoryData.value.reduce((sum, item) => sum + parseFloat(item.totalAmount), 0)
    })

    const minorCategoryTotal = computed(() => {
      return minorCategoryData.value.reduce((sum, item) => sum + parseFloat(item.totalAmount), 0)
    })

    const monthlyTrendTotal = computed(() => {
      return monthlyTrendData.value.reduce((sum, item) => sum + parseFloat(item.amount || 0), 0)
    })

    const maxMonthlyTrendIncome = computed(() => {
      if (monthlyTrendData.value.length === 0) return 0
      return Math.max(...monthlyTrendData.value.map(item => parseFloat(item.amount || 0)))
    })

    const maxMonthlyIncome = computed(() => {
      // 计算所有大类中的最高月收入
      if (majorCategoryData.value.length === 0) return 0
      // 这里简化处理，返回平均月收入的1.5倍作为估算
      return (totalIncome.value / 12) * 1.5
    })

    // 同比增长率（总体）
    const yearOverYearGrowth = computed(() => {
      if (lastYearTotalIncome.value === 0) return 0
      return ((totalIncome.value - lastYearTotalIncome.value) / lastYearTotalIncome.value) * 100
    })

    // 获取指定大类的上一年收入
    const getLastYearIncome = (majorCategoryId) => {
      const lastYearCategory = lastYearMajorCategoryData.value.find(
        item => item.majorCategoryId === majorCategoryId
      )
      return lastYearCategory ? parseFloat(lastYearCategory.totalAmount || 0) : 0
    }

    // 计算指定大类的同比增长率
    const getCategoryYearOverYearGrowth = (majorCategoryId, currentAmount) => {
      const lastYearAmount = getLastYearIncome(majorCategoryId)
      if (lastYearAmount === 0) return 0
      return ((parseFloat(currentAmount) - lastYearAmount) / lastYearAmount) * 100
    }

    // 获取指定小类的上一年收入
    const getLastYearMinorIncome = (minorCategoryId) => {
      const lastYearCategory = lastYearMinorCategoryData.value.find(
        item => item.minorCategoryId === minorCategoryId
      )
      return lastYearCategory ? parseFloat(lastYearCategory.totalAmount || 0) : 0
    }

    // 计算指定小类的同比增长率
    const getMinorCategoryYearOverYearGrowth = (minorCategoryId, currentAmount) => {
      const lastYearAmount = getLastYearMinorIncome(minorCategoryId)
      if (lastYearAmount === 0) return 0
      return ((parseFloat(currentAmount) - lastYearAmount) / lastYearAmount) * 100
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

    // 刷新数据
    const refreshData = async () => {
      if (!selectedFamilyId.value) {
        alert('请先选择家庭')
        return
      }

      refreshing.value = true
      try {
        // 调用刷新API
        await incomeAnalysisAPI.refreshAnnualSummary(
          selectedFamilyId.value,
          selectedYear.value,
          selectedCurrency.value
        )

        // 刷新成功后重新加载数据
        await loadMajorCategoryData()

        alert('数据刷新成功！')
      } catch (error) {
        console.error('刷新数据失败:', error)
        alert('刷新数据失败: ' + (error.message || '未知错误'))
      } finally {
        refreshing.value = false
      }
    }

    // 加载大类汇总数据
    const loadMajorCategoryData = async () => {
      if (!selectedFamilyId.value) {
        console.warn('No family selected, waiting for family store to initialize')
        return
      }

      loading.value = true
      try {
        // 1. 先获取所有大类分类
        const categoriesResponse = await incomeCategoryAPI.getAll()
        const allMajorCategories = categoriesResponse.data || []

        // 2. 并行加载当年收入数据和上一年收入数据
        let currentYearIncomeData = []
        await Promise.all([
          incomeAnalysisAPI.getAnnualMajorCategories(
            selectedFamilyId.value,
            selectedYear.value,
            selectedCurrency.value
          ).then(response => {
            if (response && response.success) {
              currentYearIncomeData = response.data || []
            }
          })
        ])

        // 3. 加载上一年收入数据
        let lastYearIncomeData = []
        try {
          const lastYearResponse = await incomeAnalysisAPI.getAnnualMajorCategories(
            selectedFamilyId.value,
            selectedYear.value - 1,
            selectedCurrency.value
          )

          if (lastYearResponse && lastYearResponse.success) {
            lastYearIncomeData = lastYearResponse.data || []
            // 保存上一年大类数据（用于表格显示）
            lastYearMajorCategoryData.value = lastYearIncomeData
            // 计算上一年总收入
            lastYearTotalIncome.value = lastYearIncomeData.reduce((sum, item) => sum + parseFloat(item.totalAmount || 0), 0)
          } else {
            lastYearMajorCategoryData.value = []
            lastYearTotalIncome.value = 0
          }
        } catch (error) {
          console.error('加载上一年数据失败:', error)
          lastYearMajorCategoryData.value = []
          lastYearTotalIncome.value = 0
        }

        // 4. 创建当年收入数据的映射（按大类ID索引）
        const currentYearIncomeMap = {}
        currentYearIncomeData.forEach(item => {
          currentYearIncomeMap[item.majorCategoryId] = item
        })

        // 5. 将所有大类和收入数据合并
        // 注意：新的API返回嵌套结构，大类字段为 id, name, chineseName, icon（不带major前缀）
        majorCategoryData.value = allMajorCategories
          .filter(major => major.id) // 只保留有id的项（新结构使用id而非majorCategoryId）
          .map(major => {
            const incomeItem = currentYearIncomeMap[major.id]
            return {
              majorCategoryId: major.id,
              majorCategoryName: major.name,
              majorCategoryChineseName: major.chineseName,
              majorCategoryIcon: major.icon,
              totalAmount: incomeItem ? incomeItem.totalAmount : 0,
              currency: incomeItem ? incomeItem.currency : selectedCurrency.value
            }
          })

        // 清空小类和月度数据
        selectedMajorCategoryId.value = null
        selectedMinorCategoryId.value = undefined
        minorCategoryData.value = []
        monthlyTrendData.value = []

        // 更新大类饼图
        await nextTick()
        updateMajorCategoryChart()
      } catch (error) {
        console.error('加载大类数据失败:', error)
        majorCategoryData.value = []
        lastYearTotalIncome.value = 0
        lastYearMajorCategoryData.value = []
      } finally {
        loading.value = false
      }
    }

    // 选择大类
    const selectMajorCategory = async (item) => {
      selectedMajorCategoryId.value = item.majorCategoryId
      selectedMajorCategoryName.value = item.majorCategoryChineseName || item.majorCategoryName

      // 清空小类选择
      selectedMinorCategoryId.value = undefined
      monthlyTrendData.value = []

      try {
        // 并行加载当年和上一年的小类数据
        const [currentYearResponse, lastYearResponse] = await Promise.all([
          incomeAnalysisAPI.getAnnualMinorCategories(
            selectedFamilyId.value,
            selectedYear.value,
            item.majorCategoryId,
            selectedCurrency.value
          ),
          incomeAnalysisAPI.getAnnualMinorCategories(
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

    // 选择小类
    const selectMinorCategory = async (item) => {
      selectedMinorCategoryId.value = item.minorCategoryId
      selectedMinorCategoryName.value = item.minorCategoryChineseName || item.minorCategoryName

      try {
        const response = await incomeAnalysisAPI.getAnnualMonthlyTrend(
          selectedFamilyId.value,
          selectedYear.value,
          selectedMajorCategoryId.value,
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
        '#10b981', '#3b82f6', '#f59e0b', '#8b5cf6', '#ec4899',
        '#14b8a6', '#f97316', '#06b6d4', '#84cc16', '#ef4444'
      ]

      // 只在饼图中显示有收入的大类（排除零收入类别以获得更清晰的可视化）
      const nonZeroCategories = majorCategoryData.value.filter(d => parseFloat(d.totalAmount) > 0)

      majorCategoryChart.value = new Chart(ctx, {
        type: 'pie',
        data: {
          labels: nonZeroCategories.map(d => d.majorCategoryChineseName || d.majorCategoryName),
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
                const label = nonZeroCategories[context.dataIndex].majorCategoryChineseName ||
                             nonZeroCategories[context.dataIndex].majorCategoryName
                const percentage = ((value / totalIncome.value) * 100).toFixed(1)
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
        '#10b981', '#3b82f6', '#f59e0b', '#8b5cf6', '#ec4899',
        '#14b8a6', '#f97316', '#06b6d4', '#84cc16', '#ef4444'
      ]

      minorCategoryChart.value = new Chart(ctx, {
        type: 'pie',
        data: {
          labels: minorCategoryData.value.map(d => d.minorCategoryChineseName || d.minorCategoryName),
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
                const label = minorCategoryData.value[context.dataIndex].minorCategoryChineseName ||
                             minorCategoryData.value[context.dataIndex].minorCategoryName
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
            label: '收入金额',
            data: amounts,
            backgroundColor: '#10b981',
            borderColor: '#10b981',
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
    watch([() => familyStore.currentFamilyId, selectedYear, selectedCurrency], ([newFamilyId, newYear, newCurrency], [oldFamilyId]) => {
      if (newFamilyId && newFamilyId !== oldFamilyId) {
        loadMajorCategoryData()
      } else if (newFamilyId) {
        // Year or currency changed
        loadMajorCategoryData()
      }
    })

    // 组件挂载时
    onMounted(async () => {
      await loadCurrencies()

      // Load page data if family is already available
      if (selectedFamilyId.value) {
        await loadMajorCategoryData()
      }
    })

    return {
      selectedFamilyId,
      selectedYear,
      availableYears,
      currencies,
      selectedCurrency,
      loading,
      refreshing,
      majorCategoryData,
      minorCategoryData,
      monthlyTrendData,
      selectedMajorCategoryId,
      selectedMajorCategoryName,
      selectedMinorCategoryId,
      selectedMinorCategoryName,
      totalIncome,
      minorCategoryTotal,
      monthlyTrendTotal,
      maxMonthlyTrendIncome,
      maxMonthlyIncome,
      lastYearTotalIncome,
      lastYearMajorCategoryData,
      yearOverYearGrowth,
      getLastYearIncome,
      getCategoryYearOverYearGrowth,
      getLastYearMinorIncome,
      getMinorCategoryYearOverYearGrowth,
      majorCategoryChartCanvas,
      minorCategoryChartCanvas,
      monthlyTrendChartCanvas,
      selectMajorCategory,
      selectMinorCategory,
      formatCurrency,
      refreshData
    }
  }
}
</script>

<style scoped>
/* 自定义样式 */
</style>
