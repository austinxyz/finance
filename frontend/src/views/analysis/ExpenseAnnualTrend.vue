<template>
  <div class="p-3 md:p-6 space-y-4 md:space-y-6">
    <!-- 页面标题和控制栏 -->
    <div class="bg-white rounded-lg shadow p-3 md:p-4">
      <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-3 md:gap-0 mb-4">
        <h1 class="text-xl md:text-2xl font-bold text-gray-900">支出年度趋势分析</h1>
        <div class="flex flex-col sm:flex-row items-stretch sm:items-center gap-2 md:gap-4">
          <!-- 年份选择 -->
          <div class="flex items-center gap-2">
            <label class="text-xs md:text-sm font-medium text-gray-700 whitespace-nowrap">显示年数：</label>
            <select v-model.number="displayYears" @change="fetchData"
                    class="flex-1 sm:flex-none px-2 md:px-3 py-1.5 md:py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary text-xs md:text-sm">
              <option :value="3">最近3年</option>
              <option :value="5">最近5年</option>
              <option :value="10">最近10年</option>
              <option :value="999">全部</option>
            </select>
          </div>

          <!-- 货币选择 -->
          <div class="flex items-center gap-2">
            <label class="text-xs md:text-sm font-medium text-gray-700 whitespace-nowrap">显示货币：</label>
            <select v-model="selectedCurrency" @change="onCurrencyChange"
                    class="flex-1 sm:flex-none px-2 md:px-3 py-1.5 md:py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary text-xs md:text-sm">
              <option v-for="currency in currencies" :key="currency" :value="currency">
                {{ currency === 'All' ? 'All (折算为USD)' : currency === 'CNY' ? 'CNY (¥)' : 'USD ($)' }}
              </option>
            </select>
          </div>

          <button @click="fetchData"
                  class="px-3 md:px-4 py-1.5 md:py-2 bg-primary text-white rounded-md hover:bg-primary/90 text-xs md:text-sm font-medium whitespace-nowrap">
            刷新数据
          </button>
        </div>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="bg-white rounded-lg shadow p-12">
      <div class="flex flex-col items-center justify-center">
        <div class="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
        <p class="text-gray-600 mt-4">加载中...</p>
      </div>
    </div>

    <!-- Tab选项卡 -->
    <div v-else-if="trendData.length > 0 || categoryData.length > 0" class="bg-white rounded-lg shadow">
      <!-- Tab头部 -->
      <div class="border-b border-gray-200">
        <nav class="flex -mb-px">
          <button
            @click="activeTab = 'trend'"
            :class="[
              'px-4 md:px-6 py-3 md:py-4 text-sm md:text-base font-medium border-b-2 transition-colors',
              activeTab === 'trend'
                ? 'border-primary text-primary bg-primary/5'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            ]">
            年度趋势图
          </button>
          <button
            @click="activeTab = 'table'"
            :class="[
              'px-4 md:px-6 py-3 md:py-4 text-sm md:text-base font-medium border-b-2 transition-colors',
              activeTab === 'table'
                ? 'border-primary text-primary bg-primary/5'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            ]">
            年度汇总表
          </button>
        </nav>
      </div>

      <!-- Tab内容 -->
      <div class="p-3 md:p-6">
        <!-- 年度趋势图 Tab -->
        <div v-show="activeTab === 'trend'" class="space-y-4">
          <!-- 图表和表格并列显示 -->
          <div class="flex flex-col lg:flex-row gap-4">
            <!-- 左侧：趋势图表 -->
            <div class="bg-white rounded-lg border border-gray-200 p-3 md:p-6 flex-1 lg:w-1/2">
              <div class="mb-3 md:mb-4">
                <h2 class="text-base md:text-lg font-semibold text-gray-900">年度支出趋势</h2>
                <p class="text-xs md:text-sm text-gray-500 mt-1">基础支出和实际支出年度变化及同比增长率</p>
              </div>
              <div class="h-96 md:h-[500px] w-full">
                <canvas v-if="trendData.length > 0" ref="trendChartCanvas" class="w-full h-full"></canvas>
                <div v-else class="h-full flex items-center justify-center text-gray-500 text-sm">
                  暂无年度支出趋势数据
                </div>
              </div>
            </div>

            <!-- 右侧：数据表格 -->
            <div class="bg-white rounded-lg border border-gray-200 p-3 md:p-6 flex-1 lg:w-1/2">
              <div class="mb-3 md:mb-4">
                <h2 class="text-base md:text-lg font-semibold text-gray-900">年度汇总表</h2>
                <p class="text-xs md:text-sm text-gray-500 mt-1">各年度支出数据对比</p>
              </div>
              <div class="overflow-y-auto max-h-96 md:max-h-[500px]">
                <div v-if="trendData.length === 0" class="h-96 flex items-center justify-center text-gray-500 text-sm">
                  暂无年度汇总数据
                </div>
                <table v-else class="w-full border-separate border-spacing-0">
                  <thead class="bg-gray-50 border-b border-gray-200 sticky top-0">
                    <tr>
                      <th class="px-2 md:px-3 py-2 text-left text-xs md:text-sm font-medium text-gray-500 uppercase">年份</th>
                      <th class="px-2 md:px-3 py-2 text-right text-xs md:text-sm font-medium text-gray-500 uppercase">基础支出</th>
                      <th class="px-2 md:px-3 py-2 text-right text-xs md:text-sm font-medium text-gray-500 uppercase">特殊支出</th>
                      <th class="px-2 md:px-3 py-2 text-right text-xs md:text-sm font-medium text-gray-500 uppercase">调整值</th>
                      <th class="px-2 md:px-3 py-2 text-right text-xs md:text-sm font-medium text-gray-500 uppercase">实际支出</th>
                      <th class="px-2 md:px-3 py-2 text-right text-xs md:text-sm font-medium text-gray-500 uppercase">实际同比</th>
                    </tr>
                  </thead>
                  <tbody class="bg-white">
                    <tr v-for="item in convertedTrendData" :key="item.year" class="hover:bg-gray-50 border-b border-gray-200">
                      <td class="px-2 md:px-3 py-2 whitespace-nowrap">
                        <div class="text-xs md:text-sm font-medium text-gray-900">{{ item.year }}</div>
                      </td>
                      <td class="px-2 md:px-3 py-2 whitespace-nowrap text-right">
                        <div class="text-xs md:text-sm font-medium text-gray-900">{{ getCurrencySymbol(selectedCurrency) }}{{ formatAmount(item.baseExpense) }}</div>
                      </td>
                      <td class="px-2 md:px-3 py-2 whitespace-nowrap text-right">
                        <div class="text-xs md:text-sm font-medium text-orange-600">{{ getCurrencySymbol(selectedCurrency) }}{{ formatAmount(item.specialExpense || 0) }}</div>
                      </td>
                      <td class="px-2 md:px-3 py-2 whitespace-nowrap text-right">
                        <div class="text-xs md:text-sm font-medium" :class="getTotalAdjustmentColor(item.assetAdjustment, item.liabilityAdjustment)">
                          {{ getCurrencySymbol(selectedCurrency) }}{{ formatAmount(getTotalAdjustment(item.assetAdjustment || 0, item.liabilityAdjustment || 0)) }}
                        </div>
                      </td>
                      <td class="px-2 md:px-3 py-2 whitespace-nowrap text-right">
                        <div class="text-xs md:text-sm font-bold text-blue-600">{{ getCurrencySymbol(selectedCurrency) }}{{ formatAmount(item.actualExpense) }}</div>
                      </td>
                      <td class="px-2 md:px-3 py-2 whitespace-nowrap text-right">
                        <div v-if="item.yoyActualChange !== null" class="text-xs md:text-sm">
                          <div :class="getChangeColor(item.yoyActualChange)" class="font-medium">
                            {{ formatChange(item.yoyActualChange) }}
                          </div>
                          <div :class="getChangeColor(item.yoyActualChangePct)" class="text-[10px] md:text-xs">
                            ({{ formatPercent(item.yoyActualChangePct) }})
                          </div>
                        </div>
                        <div v-else class="text-xs md:text-sm text-gray-400">基准年</div>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          <!-- 大类趋势图 -->
          <div v-if="categoryData.length > 0" class="bg-white rounded-lg border border-gray-200 p-3 md:p-6">
            <div class="mb-3 md:mb-4">
              <h2 class="text-base md:text-lg font-semibold text-gray-900">各大类支出趋势</h2>
              <p class="text-xs md:text-sm text-gray-500 mt-1">各支出大类实际支出年度变化对比（已调整资产负债）</p>

              <!-- 大类过滤选择器 -->
              <div class="mt-3 p-3 bg-gray-50 rounded-lg border border-gray-200">
                <div class="flex items-center justify-between mb-2">
                  <span class="text-xs md:text-sm font-medium text-gray-700">显示大类：</span>
                  <div class="flex gap-2">
                    <button @click="selectAllCategories"
                            class="text-xs px-2 py-1 text-primary hover:bg-primary/10 rounded">
                      全选
                    </button>
                    <button @click="deselectAllCategories"
                            class="text-xs px-2 py-1 text-gray-600 hover:bg-gray-200 rounded">
                      清空
                    </button>
                  </div>
                </div>
                <div class="flex flex-wrap gap-2">
                  <label v-for="category in categoryData" :key="category.majorCategoryId"
                         class="inline-flex items-center gap-1.5 px-2 md:px-3 py-1.5 bg-white border border-gray-300 rounded-md hover:bg-gray-50 cursor-pointer text-xs md:text-sm">
                    <input type="checkbox"
                           :value="category.majorCategoryId"
                           v-model="selectedCategories"
                           @change="onCategoryFilterChange"
                           class="w-3 h-3 md:w-4 md:h-4 text-primary focus:ring-2 focus:ring-primary rounded">
                    <span class="text-base">{{ category.majorCategoryIcon }}</span>
                    <span class="text-gray-700">{{ category.majorCategoryName }}</span>
                  </label>
                </div>
              </div>
            </div>
            <div class="h-96 md:h-[500px] w-full">
              <canvas ref="categoryTrendChartCanvas" class="w-full h-full"></canvas>
            </div>
          </div>
        </div>

        <!-- 年度汇总表 Tab（转置表格：年份为列，大类为行） -->
        <div v-show="activeTab === 'table'">
          <div v-if="summaryTableLoading" class="flex flex-col items-center justify-center py-12">
            <div class="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
            <p class="text-gray-600 mt-4">加载汇总表...</p>
          </div>
          <div v-else-if="summaryTableData.rows && summaryTableData.rows.length > 0">
            <div class="overflow-x-auto">
              <table class="w-full border-collapse text-xs md:text-sm">
                <thead>
                  <tr class="bg-gray-100 border-b-2 border-gray-300">
                    <th class="px-2 md:px-4 py-2 md:py-3 text-left font-semibold text-gray-700 sticky left-0 bg-gray-100 z-10 border-r border-gray-300">大类</th>
                    <!-- 年份列（横坐标） -->
                    <th v-for="year in summaryTableData.years" :key="year"
                        class="px-2 md:px-3 py-2 md:py-3 text-center font-semibold text-gray-700 border-r border-gray-200 min-w-[140px]">
                      {{ year }}
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <!-- 各大类行（纵坐标） -->
                  <tr v-for="(category, catIndex) in summaryTableData.categories" :key="category.code"
                      :class="catIndex % 2 === 0 ? 'bg-white' : 'bg-gray-50'"
                      class="border-b border-gray-200 hover:bg-blue-50">
                    <!-- 大类名称 -->
                    <td class="px-2 md:px-4 py-2 md:py-3 font-semibold text-gray-900 sticky left-0 bg-inherit z-10 border-r border-gray-300">
                      <div class="flex items-center gap-2">
                        <span class="text-lg">{{ category.icon }}</span>
                        <span>{{ category.name }}</span>
                      </div>
                    </td>
                    <!-- 各年份数据 -->
                    <td v-for="row in summaryTableData.rows" :key="row.year"
                        class="px-2 md:px-3 py-2 md:py-3 text-right border-r border-gray-200">
                      <div v-if="row.categoryData[category.code]" class="space-y-1">
                        <!-- 实际支出（基础支出） -->
                        <div class="font-semibold text-gray-900">
                          {{ getCurrencySymbol(selectedCurrency) }}{{ formatAmount(convertCurrency(row.categoryData[category.code].actualExpense, row.year)) }}
                          <span class="text-[10px] text-gray-500">({{ getCurrencySymbol(selectedCurrency) }}{{ formatAmount(convertCurrency(row.categoryData[category.code].baseExpense, row.year)) }})</span>
                        </div>

                        <!-- 实际同比（基础同比） -->
                        <div v-if="row.categoryData[category.code].actualChangePct !== null" class="text-[10px] md:text-xs">
                          <span :class="getChangeColor(row.categoryData[category.code].actualChangePct)">
                            {{ formatPercent(row.categoryData[category.code].actualChangePct) }}
                          </span>
                          <span class="text-gray-500">
                            ({{ formatPercent(row.categoryData[category.code].baseChangePct) }})
                          </span>
                        </div>
                        <div v-else class="text-[10px] text-gray-400">基准年</div>
                      </div>
                      <div v-else class="text-gray-400">-</div>
                    </td>
                  </tr>
                  <!-- 特殊支出汇总行 -->
                  <tr class="bg-orange-50 border-t-2 border-orange-200">
                    <td class="px-2 md:px-4 py-2 md:py-3 text-gray-900 sticky left-0 bg-orange-50 z-10 border-r border-gray-300">
                      <div class="flex items-center gap-2">
                        <span class="text-lg">⚠️</span>
                        <span class="font-semibold">特殊支出</span>
                      </div>
                    </td>
                    <td v-for="row in summaryTableData.rows" :key="`special-${row.year}`"
                        class="px-2 md:px-3 py-2 md:py-3 text-right border-r border-gray-200">
                      <div v-if="row.total && row.total.specialExpense && row.total.specialExpense > 0" class="space-y-1">
                        <!-- 特殊支出金额 -->
                        <div class="font-semibold text-orange-700">
                          {{ getCurrencySymbol(selectedCurrency) }}{{ formatAmount(convertCurrency(row.total.specialExpense, row.year)) }}
                        </div>
                        <!-- 特殊支出详情 -->
                        <div class="text-[9px] text-gray-600">
                          <div v-for="(detail, idx) in getSpecialExpenseDetailsSummary(row)" :key="idx">
                            {{ detail.categoryName }}: {{ getCurrencySymbol(selectedCurrency) }}{{ formatAmount(convertCurrency(detail.amount, row.year)) }}
                          </div>
                        </div>
                      </div>
                      <div v-else class="text-gray-400">-</div>
                    </td>
                  </tr>

                  <!-- 调整值汇总行 -->
                  <tr class="bg-purple-50 border-t border-purple-200">
                    <td class="px-2 md:px-4 py-2 md:py-3 text-gray-900 sticky left-0 bg-purple-50 z-10 border-r border-gray-300">
                      <div class="flex items-center gap-2">
                        <span class="text-lg">⚖️</span>
                        <span class="font-semibold">调整值</span>
                      </div>
                    </td>
                    <td v-for="row in summaryTableData.rows" :key="`adjustment-${row.year}`"
                        class="px-2 md:px-3 py-2 md:py-3 text-right border-r border-gray-200">
                      <div v-if="row.total" class="space-y-1">
                        <!-- 总调整值（资产+负债） -->
                        <div class="font-semibold" :class="getTotalAdjustmentColor(row.total.assetAdjustment, row.total.liabilityAdjustment)">
                          {{ getCurrencySymbol(selectedCurrency) }}{{ formatAmount(convertCurrency(getTotalAdjustment(row.total.assetAdjustment, row.total.liabilityAdjustment), row.year)) }}
                        </div>
                        <!-- 调整值详情（按资产/负债类型显示） -->
                        <div v-if="row.total.adjustmentDetails" class="text-[9px] text-gray-600">
                          <div v-for="(detail, idx) in parseAdjustmentDetails(row.total.adjustmentDetails)" :key="idx">
                            {{ detail.icon }} {{ detail.typeName }}: {{ getCurrencySymbol(selectedCurrency) }}{{ formatAmount(convertCurrency(detail.amount, row.year)) }}
                          </div>
                        </div>
                      </div>
                      <div v-else class="text-gray-400">-</div>
                    </td>
                  </tr>

                  <!-- 总计行 -->
                  <tr class="bg-blue-50 border-t-2 border-blue-300 font-bold">
                    <td class="px-2 md:px-4 py-2 md:py-3 text-gray-900 sticky left-0 bg-blue-50 z-10 border-r border-gray-300">
                      <div class="flex items-center gap-2">
                        <span class="text-lg">💰</span>
                        <span>总计</span>
                      </div>
                    </td>
                    <td v-for="row in summaryTableData.rows" :key="row.year"
                        class="px-2 md:px-3 py-2 md:py-3 text-right border-r border-gray-200">
                      <div v-if="row.total" class="space-y-1">
                        <!-- 实际支出（基础支出 + 特殊支出） -->
                        <div class="text-blue-900">
                          {{ getCurrencySymbol(selectedCurrency) }}{{ formatAmount(convertCurrency(row.total.actualExpense, row.year)) }}
                          <div class="text-[10px] text-blue-700">
                            ({{ getCurrencySymbol(selectedCurrency) }}{{ formatAmount(convertCurrency((row.total.baseExpense || 0) + (row.total.specialExpense || 0), row.year)) }})
                          </div>
                        </div>

                        <!-- 实际同比（基础同比） -->
                        <div v-if="row.total.actualChangePct !== null" class="text-[10px] md:text-xs">
                          <span :class="getChangeColor(row.total.actualChangePct)" class="font-semibold">
                            {{ formatPercent(row.total.actualChangePct) }}
                          </span>
                          <span class="text-blue-700">
                            ({{ formatPercent(row.total.baseChangePct) }})
                          </span>
                        </div>
                        <div v-else class="text-[10px] text-gray-400">基准年</div>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div class="mt-4 p-3 bg-gray-50 rounded-lg text-xs text-gray-600">
              <p><strong>说明：</strong></p>
              <ul class="list-disc list-inside mt-1 space-y-1">
                <li>数据格式：<strong>实际支出</strong>（基础支出）</li>
                <li>同比格式：<strong>实际同比%</strong>（基础同比%）</li>
                <li>实际支出 = 基础支出 + 特殊支出 + 资产/负债调整</li>
                <li>⚠️ 特殊支出：单笔 ≥ $10,000 的支出，显示橙色标记及明细</li>
                <li>红色表示支出增加，绿色表示支出减少</li>
                <li>后端数据为USD基准货币，前端根据选中货币和各年份年末汇率换算显示</li>
              </ul>
            </div>
          </div>
          <div v-else class="text-center py-12 text-gray-500">
            暂无汇总表数据
          </div>
        </div>
      </div>
    </div>

    <!-- 无数据提示 -->
    <div v-else class="bg-white rounded-lg shadow border border-gray-200 p-12 text-center">
      <div class="text-gray-400 mb-2">
        <svg class="mx-auto h-12 w-12" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
        </svg>
      </div>
      <h3 class="text-lg font-medium text-gray-900 mb-2">暂无年度支出数据</h3>
      <p class="text-gray-600">请先添加支出记录并计算年度汇总</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, computed, watch } from 'vue'
import { Chart, registerables } from 'chart.js'
import { expenseAnalysisAPI } from '@/api/expense'
import { exchangeRateAPI } from '@/api/exchangeRate'
import { useFamilyStore } from '@/stores/family'

Chart.register(...registerables)

// 数据
const trendData = ref([])
// Family store
const familyStore = useFamilyStore()
const familyId = computed(() => familyStore.currentFamilyId)

const categoryData = ref([])
const summaryTableData = ref({ years: [], categories: [], rows: [] })
const loading = ref(false)
const summaryTableLoading = ref(false)
const displayYears = ref(5)
const currencies = ['All', 'CNY', 'USD']
const selectedCurrency = ref('All')
const selectedCategories = ref([])
const exchangeRates = ref([]) // 汇率数据
const activeTab = ref('trend') // 当前激活的tab

// 图表引用
const trendChartCanvas = ref(null)
const categoryTrendChartCanvas = ref(null)

// 图表实例
let trendChart = null
let categoryTrendChart = null

// 计算属性：转换后的趋势数据（用于表格显示，使用各年份的汇率）
const convertedTrendData = computed(() => {
  return trendData.value.map(item => ({
    ...item,
    baseExpense: convertCurrency(item.baseExpense, item.year),
    specialExpense: convertCurrency(item.specialExpense || 0, item.year),
    assetAdjustment: convertCurrency(item.assetAdjustment || 0, item.year),
    liabilityAdjustment: convertCurrency(item.liabilityAdjustment || 0, item.year),
    actualExpense: convertCurrency(item.actualExpense, item.year),
    yoyBaseChange: item.yoyBaseChange ? convertCurrency(item.yoyBaseChange, item.year) : null,
    yoyActualChange: item.yoyActualChange ? convertCurrency(item.yoyActualChange, item.year) : null
  }))
})

// Watch for family changes
watch(familyId, (newFamilyId) => {
  if (newFamilyId) {
    fetchData()
  }
})

// 货币切换事件
const onCurrencyChange = () => {
  // 货币切换时只需要重新渲染图表，不需要重新获取数据
  // 汇总表数据已经是USD基准货币，前端根据选中货币换算显示，无需重新获取

  // 只在trend tab时才渲染图表（避免canvas元素不在DOM中的错误）
  if (activeTab.value === 'trend') {
    renderChart()
    renderCategoryTrendChart()
  }
}

// 获取汇率数据（获取所有启用的汇率）
const fetchExchangeRates = async () => {
  try {
    const response = await exchangeRateAPI.getAllActive()
    if (response.success && response.data) {
      exchangeRates.value = response.data
    } else if (response.data && Array.isArray(response.data)) {
      exchangeRates.value = response.data
    }
  } catch (error) {
    console.error('获取汇率数据失败:', error)
    exchangeRates.value = []
  }
}

// 获取指定货币和年份的年末汇率（USD为基准）
const getExchangeRateForYear = (currency, year) => {
  if (currency === 'USD') return 1

  // 查找该年份12-31或之前最近的汇率
  const yearEndDate = `${year}-12-31`

  const applicableRates = exchangeRates.value
    .filter(r => r.currency === currency && r.effectiveDate <= yearEndDate)
    .sort((a, b) => b.effectiveDate.localeCompare(a.effectiveDate))

  if (applicableRates.length > 0) {
    return applicableRates[0].rateToUsd
  }

  // 如果找不到，返回默认值
  return 1
}

// 将USD金额转换为选中货币（根据年份使用不同汇率）
const convertCurrency = (usdAmount, year = null) => {
  if (!usdAmount) return 0

  // All模式或USD：直接返回USD金额
  if (selectedCurrency.value === 'USD' || selectedCurrency.value === 'All') {
    return Number(usdAmount)
  }

  // 如果没有提供年份，使用最新汇率
  let rate
  if (year) {
    rate = getExchangeRateForYear(selectedCurrency.value, year)
  } else {
    const latestRate = exchangeRates.value
      .filter(r => r.currency === selectedCurrency.value && r.isActive)
      .sort((a, b) => b.effectiveDate.localeCompare(a.effectiveDate))[0]
    rate = latestRate ? latestRate.rateToUsd : 1
  }

  // USD转其他货币：USD金额 / 汇率
  return Number(usdAmount) / rate
}

// 获取趋势图数据
const fetchData = async () => {
  if (!familyId.value) return

  loading.value = true
  try {
    // 并行获取总支出趋势和大类趋势数据（始终获取USD数据）
    const [trendResponse, categoryResponse] = await Promise.all([
      expenseAnalysisAPI.getAnnualTrend(
        familyId.value,
        displayYears.value,
        'USD'  // 始终获取USD基准货币数据
      ),
      expenseAnalysisAPI.getAnnualCategoryTrend(
        familyId.value,
        displayYears.value,
        'USD'  // 始终获取USD基准货币数据
      )
    ])

    if (trendResponse.success && trendResponse.data) {
      trendData.value = trendResponse.data.sort((a, b) => b.year - a.year)
    }

    if (categoryResponse.success && categoryResponse.data) {
      categoryData.value = categoryResponse.data
      // 默认全选所有大类
      selectedCategories.value = categoryResponse.data.map(cat => cat.majorCategoryId)
    }

    // 如果当前在汇总表tab，也获取汇总表数据
    if (activeTab.value === 'table') {
      await fetchSummaryTable()
    }
  } catch (error) {
    console.error('获取年度支出趋势数据失败:', error)
  } finally {
    loading.value = false
  }
}

// 获取年度汇总表数据（获取USD基准货币数据）
const fetchSummaryTable = async () => {
  if (!familyId.value) return

  summaryTableLoading.value = true
  try {
    const response = await expenseAnalysisAPI.getAnnualSummaryTable(
      familyId.value,
      displayYears.value
    )

    if (response.success && response.data) {
      summaryTableData.value = response.data
    }
  } catch (error) {
    console.error('获取年度汇总表失败:', error)
  } finally {
    summaryTableLoading.value = false
  }
}

// 监听tab切换，切换到汇总表时加载数据，切换到趋势图时重新渲染图表
watch(activeTab, (newTab) => {
  if (newTab === 'table' && summaryTableData.value.rows.length === 0) {
    fetchSummaryTable()
  } else if (newTab === 'trend') {
    // 切换回趋势图tab时，重新渲染图表（以应用可能的货币切换）
    nextTick(() => {
      renderChart()
      renderCategoryTrendChart()
    })
  }
})

// 渲染图表
const renderChart = () => {
  if (trendData.value.length === 0) return

  const sortedData = [...trendData.value].reverse() // 从旧到新排序
  const years = sortedData.map(d => d.year)
  // 将USD金额转换为选中货币（使用各年份的汇率）
  const baseExpenses = sortedData.map(d => convertCurrency(d.baseExpense, d.year))
  const specialExpenses = sortedData.map(d => convertCurrency(d.specialExpense || 0, d.year))
  const actualExpenses = sortedData.map(d => convertCurrency(d.actualExpense, d.year))
  const baseGrowths = sortedData.map(d => d.yoyBaseChangePct ? Number(d.yoyBaseChangePct) : 0)
  const actualGrowths = sortedData.map(d => d.yoyActualChangePct ? Number(d.yoyActualChangePct) : 0)

  const currencySymbol = getCurrencySymbol(selectedCurrency.value)

  // 趋势图（双Y轴）
  if (trendChart) trendChart.destroy()
  if (!trendChartCanvas.value) {
    console.error('Canvas element not found')
    return
  }

  trendChart = new Chart(trendChartCanvas.value, {
      type: 'bar',
      data: {
        labels: years,
        datasets: [
          {
            label: '基础支出',
            data: baseExpenses,
            backgroundColor: 'rgba(99, 102, 241, 0.7)',
            borderColor: 'rgb(99, 102, 241)',
            borderWidth: 1,
            yAxisID: 'y'
          },
          {
            label: '特殊支出',
            data: specialExpenses,
            backgroundColor: 'rgba(251, 146, 60, 0.7)',
            borderColor: 'rgb(251, 146, 60)',
            borderWidth: 1,
            yAxisID: 'y'
          },
          {
            label: '实际支出',
            data: actualExpenses,
            backgroundColor: 'rgba(239, 68, 68, 0.7)',
            borderColor: 'rgb(239, 68, 68)',
            borderWidth: 1,
            yAxisID: 'y'
          },
          {
            label: '基础支出同比',
            data: baseGrowths,
            type: 'line',
            borderColor: 'rgb(59, 130, 246)',
            backgroundColor: 'rgba(59, 130, 246, 0.1)',
            borderWidth: 2,
            tension: 0.4,
            yAxisID: 'y1'
          },
          {
            label: '实际支出同比',
            data: actualGrowths,
            type: 'line',
            borderColor: 'rgb(234, 88, 12)',
            backgroundColor: 'rgba(234, 88, 12, 0.1)',
            borderWidth: 2,
            tension: 0.4,
            yAxisID: 'y1'
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        interaction: {
          mode: 'index',
          intersect: false,
        },
        plugins: {
          legend: {
            position: window.innerWidth < 768 ? 'bottom' : 'top',
            labels: {
              font: {
                size: window.innerWidth < 768 ? 10 : 12,
                weight: 'bold'
              },
              padding: window.innerWidth < 768 ? 6 : 10,
              boxWidth: window.innerWidth < 768 ? 20 : 40
            }
          },
          tooltip: {
            callbacks: {
              label: function(context) {
                if (context.dataset.label.includes('同比')) {
                  return context.dataset.label + ': ' + context.parsed.y.toFixed(2) + '%'
                } else {
                  return context.dataset.label + ': ' + currencySymbol + Number(context.parsed.y).toLocaleString('en-US', {
                    minimumFractionDigits: 0,
                    maximumFractionDigits: 2
                  })
                }
              }
            },
            titleFont: {
              size: window.innerWidth < 768 ? 11 : 12,
              weight: 'bold'
            },
            bodyFont: {
              size: window.innerWidth < 768 ? 10 : 12,
              weight: 'bold'
            },
            padding: window.innerWidth < 768 ? 6 : 10
          }
        },
        scales: {
          y: {
            type: 'linear',
            display: true,
            position: 'left',
            beginAtZero: true,
            ticks: {
              callback: function(value) {
                if (value >= 1000000) {
                  return currencySymbol + (value / 1000000).toFixed(1) + 'M'
                } else if (value >= 1000) {
                  return currencySymbol + (value / 1000).toFixed(1) + 'K'
                }
                return currencySymbol + value.toFixed(0)
              },
              font: {
                size: window.innerWidth < 768 ? 9 : 11,
                weight: 'bold'
              }
            }
          },
          y1: {
            type: 'linear',
            display: true,
            position: 'right',
            grid: {
              drawOnChartArea: false,
            },
            ticks: {
              callback: function(value) {
                return value.toFixed(1) + '%'
              },
              font: {
                size: window.innerWidth < 768 ? 9 : 11,
                weight: 'bold'
              }
            }
          },
          x: {
            ticks: {
              font: {
                size: window.innerWidth < 768 ? 9 : 11,
                weight: 'bold'
              }
            }
          }
        }
      }
    })
}

// 渲染大类趋势图
const renderCategoryTrendChart = () => {
  if (categoryData.value.length === 0) return

  if (!categoryTrendChartCanvas.value) {
    return
  }

  // 过滤出选中的大类
  const filteredCategories = categoryData.value.filter(category =>
    selectedCategories.value.includes(category.majorCategoryId)
  )

  // 如果没有选中任何大类，则不渲染
  if (filteredCategories.length === 0) {
    if (categoryTrendChart) categoryTrendChart.destroy()
    return
  }

  // 准备所有年份（从选中的大类中提取）
  const allYearsSet = new Set()
  filteredCategories.forEach(category => {
    category.yearlyData.forEach(item => {
      allYearsSet.add(item.year)
    })
  })
  const years = Array.from(allYearsSet).sort((a, b) => a - b)

  const currencySymbol = getCurrencySymbol(selectedCurrency.value)

  // 为每个大类生成颜色
  const colors = [
    'rgb(99, 102, 241)',   // 紫色
    'rgb(239, 68, 68)',    // 红色
    'rgb(34, 197, 94)',    // 绿色
    'rgb(234, 88, 12)',    // 橙色
    'rgb(59, 130, 246)',   // 蓝色
    'rgb(236, 72, 153)',   // 粉色
    'rgb(168, 85, 247)',   // 紫罗兰
    'rgb(20, 184, 166)',   // 青色
    'rgb(251, 191, 36)',   // 黄色
    'rgb(161, 161, 170)'   // 灰色
  ]

  // 为每个选中的大类创建数据集
  const datasets = filteredCategories.map((category, index) => {
    // 创建年份到金额的映射（并转换为选中货币，使用各年份的汇率）
    const yearToExpense = {}
    category.yearlyData.forEach(item => {
      yearToExpense[item.year] = convertCurrency(item.actualExpense, item.year)
    })

    // 按所有年份创建数据数组
    const data = years.map(year => yearToExpense[year] || null)

    const color = colors[index % colors.length]

    return {
      label: `${category.majorCategoryIcon} ${category.majorCategoryName}`,
      data: data,
      borderColor: color,
      backgroundColor: color.replace('rgb', 'rgba').replace(')', ', 0.1)'),
      borderWidth: 2,
      tension: 0.4,
      spanGaps: true  // 连接断点
    }
  })

  // 销毁旧图表
  if (categoryTrendChart) categoryTrendChart.destroy()

  // 创建新图表
  categoryTrendChart = new Chart(categoryTrendChartCanvas.value, {
    type: 'line',
    data: {
      labels: years,
      datasets: datasets
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      interaction: {
        mode: 'index',
        intersect: false,
      },
      plugins: {
        legend: {
          position: window.innerWidth < 768 ? 'bottom' : 'top',
          labels: {
            font: {
              size: window.innerWidth < 768 ? 10 : 12,
              weight: 'bold'
            },
            padding: window.innerWidth < 768 ? 6 : 10,
            boxWidth: window.innerWidth < 768 ? 20 : 40,
            usePointStyle: true
          }
        },
        tooltip: {
          callbacks: {
            label: function(context) {
              return context.dataset.label + ': ' + currencySymbol + Number(context.parsed.y).toLocaleString('en-US', {
                minimumFractionDigits: 0,
                maximumFractionDigits: 2
              })
            }
          },
          titleFont: {
            size: window.innerWidth < 768 ? 11 : 12,
            weight: 'bold'
          },
          bodyFont: {
            size: window.innerWidth < 768 ? 10 : 12,
            weight: 'bold'
          },
          padding: window.innerWidth < 768 ? 6 : 10
        }
      },
      scales: {
        y: {
          beginAtZero: true,
          ticks: {
            callback: function(value) {
              if (value >= 1000000) {
                return currencySymbol + (value / 1000000).toFixed(1) + 'M'
              } else if (value >= 1000) {
                return currencySymbol + (value / 1000).toFixed(1) + 'K'
              }
              return currencySymbol + value.toFixed(0)
            },
            font: {
              size: window.innerWidth < 768 ? 9 : 11,
              weight: 'bold'
            }
          }
        },
        x: {
          ticks: {
            font: {
              size: window.innerWidth < 768 ? 9 : 11,
              weight: 'bold'
            }
          }
        }
      }
    }
  })
}

// 货币符号映射
const getCurrencySymbol = (currency) => {
  const symbols = {
    'All': '$',  // All模式显示为USD符号
    'USD': '$',
    'CNY': '¥',
    'EUR': '€',
    'GBP': '£',
    'JPY': '¥',
    'AUD': 'A$',
    'CAD': 'C$'
  }
  return symbols[currency] || currency
}

// 格式化金额
const formatAmount = (amount) => {
  if (!amount && amount !== 0) return '0.00'
  return Number(amount).toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

// 格式化变化金额
const formatChange = (amount) => {
  if (!amount && amount !== 0) return '-'
  const prefix = amount > 0 ? '+' : ''
  const symbol = getCurrencySymbol(selectedCurrency.value)
  return prefix + symbol + formatAmount(Math.abs(amount))
}

// 格式化百分比
const formatPercent = (percent) => {
  if (!percent && percent !== 0) return '-'
  const prefix = percent > 0 ? '+' : ''
  return prefix + Number(percent).toFixed(2) + '%'
}

// 获取变化颜色
const getChangeColor = (value) => {
  if (!value && value !== 0) return 'text-gray-400'
  // 对于支出，增加是不好的（红色），减少是好的（绿色）
  return value > 0 ? 'text-red-600' : 'text-green-600'
}

// 解析特殊支出详情JSON
const parseSpecialExpenseDetails = (detailsJson) => {
  if (!detailsJson) return []
  try {
    // detailsJson可能是字符串或已解析的对象
    const details = typeof detailsJson === 'string' ? JSON.parse(detailsJson) : detailsJson
    return Array.isArray(details) ? details : []
  } catch (error) {
    console.error('解析特殊支出详情失败:', error)
    return []
  }
}

// 汇总所有大类的特殊支出详情
const getSpecialExpenseDetailsSummary = (row) => {
  const allDetails = []

  // 遍历所有大类
  if (row.categoryData && summaryTableData.value.categories) {
    summaryTableData.value.categories.forEach(category => {
      const categoryCode = category.code
      const categoryDataItem = row.categoryData[categoryCode]

      if (categoryDataItem && categoryDataItem.specialExpenseDetails) {
        const details = parseSpecialExpenseDetails(categoryDataItem.specialExpenseDetails)
        details.forEach(detail => {
          allDetails.push({
            categoryName: detail.minorCategoryName,
            amount: detail.amount
          })
        })
      }
    })
  }

  return allDetails
}

// 资产类型中文名称映射
const assetTypeNames = {
  'CASH': '现金类',
  'CRYPTOCURRENCY': '数字货币',
  'INSURANCE': '保险',
  'OTHER': '其他',
  'PRECIOUS_METALS': '贵金属',
  'REAL_ESTATE': '房地产',
  'RETIREMENT_FUND': '退休基金',
  'STOCKS': '股票投资'
}

// 负债类型中文名称映射
const liabilityTypeNames = {
  'AUTO_LOAN': '车贷',
  'CREDIT_CARD': '信用卡',
  'MORTGAGE': '房贷',
  'OTHER': '其他负债',
  'PERSONAL_LOAN': '个人贷款',
  'STUDENT_LOAN': '学生贷款'
}

// 解析调整值详情JSON并转换为可读格式
const parseAdjustmentDetails = (adjustmentDetailsJson) => {
  if (!adjustmentDetailsJson) return []
  try {
    // adjustmentDetailsJson可能是字符串或已解析的对象
    const details = typeof adjustmentDetailsJson === 'string' ? JSON.parse(adjustmentDetailsJson) : adjustmentDetailsJson
    if (!Array.isArray(details)) return []

    // 转换每个详情项
    return details.map(detail => {
      const type = detail.type
      const code = detail.code
      const amount = detail.amount
      const direction = detail.direction // ASSET类型可能有direction

      let typeName = ''
      let icon = ''

      if (type === 'ASSET') {
        typeName = assetTypeNames[code] || code
        icon = '📈'
        // ASSET调整：direction=SUBTRACT表示资产减少（负调整），否则资产增加（正调整）
        // 但amount已经是正确的符号了
      } else if (type === 'LIABILITY') {
        typeName = liabilityTypeNames[code] || code
        icon = '📉'
        // LIABILITY调整：正值表示负债减少
      } else if (type === 'PROPERTY_PURCHASE') {
        typeName = '房产购买'
        icon = '🏠'
      }

      return {
        type,
        code,
        typeName,
        icon,
        amount,
        direction
      }
    })
  } catch (error) {
    console.error('解析调整值详情失败:', error)
    return []
  }
}

// 计算总调整值（资产 + 负债）
const getTotalAdjustment = (assetAdjustment, liabilityAdjustment) => {
  const asset = assetAdjustment || 0
  const liability = liabilityAdjustment || 0
  return Number(asset) + Number(liability)
}

// 获取调整值颜色
const getTotalAdjustmentColor = (assetAdjustment, liabilityAdjustment) => {
  const total = getTotalAdjustment(assetAdjustment, liabilityAdjustment)
  if (total === 0) return 'text-gray-600'
  // 调整值为正表示支出增加（红色），为负表示支出减少（绿色）
  return total > 0 ? 'text-red-600' : 'text-green-600'
}

// 全选所有大类
const selectAllCategories = () => {
  selectedCategories.value = categoryData.value.map(cat => cat.majorCategoryId)
}

// 清空所有大类选择
const deselectAllCategories = () => {
  selectedCategories.value = []
}

// 大类过滤器变化事件
const onCategoryFilterChange = () => {
  // 重新渲染图表
  if (categoryTrendChartCanvas.value) {
    renderCategoryTrendChart()
  }
}

// 监听数据变化，当数据加载且 canvas 可用时渲染图表
watch([trendData, trendChartCanvas], async () => {
  if (trendData.value.length > 0 && trendChartCanvas.value) {
    // 等待 DOM 更新完成
    await nextTick()
    // 再次确认 canvas 存在（处理条件渲染的情况）
    if (trendChartCanvas.value) {
      renderChart()
    }
  }
}, { flush: 'post' }) // 使用 post flush 确保 DOM 更新后执行

// 监听大类趋势数据变化，当数据加载且 canvas 可用时渲染大类趋势图表
watch([categoryData, categoryTrendChartCanvas], async () => {
  if (categoryData.value.length > 0 && categoryTrendChartCanvas.value) {
    // 等待 DOM 更新完成
    await nextTick()
    // 再次确认 canvas 存在（处理条件渲染的情况）
    if (categoryTrendChartCanvas.value) {
      renderCategoryTrendChart()
    }
  }
}, { flush: 'post' }) // 使用 post flush 确保 DOM 更新后执行

// 组件挂载时获取数据
onMounted(async () => {
  await fetchExchangeRates()  // 获取汇率数据

  // Load data if family is already available
  if (familyId.value) {
    await fetchData()
  }
})
</script>
