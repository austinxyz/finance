<template>
  <div class="p-3 md:p-6 space-y-4 md:space-y-6">
    <!-- 页面标题和控制 -->
    <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-3 md:gap-0">
      <div>
        <h1 class="text-xl md:text-2xl font-bold text-gray-900">年度支出（实际）</h1>
        <p class="text-xs md:text-sm text-gray-600 mt-1">经资产负债调整后的实际年度支出</p>
      </div>
      <div class="flex flex-col sm:flex-row items-stretch sm:items-center gap-2 md:gap-4">
        <!-- 家庭选择 -->
        <select
          v-model="selectedFamilyId"
          class="px-3 md:px-4 py-1.5 md:py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent text-sm md:text-base"
        >
          <option v-for="family in families" :key="family.id" :value="family.id">
            {{ family.familyName }}
          </option>
        </select>

        <!-- 年份选择 -->
        <select
          v-model="selectedYear"
          class="px-3 md:px-4 py-1.5 md:py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent text-sm md:text-base"
        >
          <option v-for="year in availableYears" :key="year" :value="year">
            {{ year }}年
          </option>
        </select>

        <!-- 刷新按钮 -->
        <button
          @click="handleRefresh"
          :disabled="refreshing || loading"
          class="px-3 md:px-4 py-1.5 md:py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition-colors text-sm md:text-base whitespace-nowrap"
        >
          <span v-if="refreshing">🔄 刷新中...</span>
          <span v-else>🔄 刷新数据</span>
        </button>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="text-center py-12">
      <div class="text-gray-500">加载中...</div>
    </div>

    <!-- 支出总览汇总卡片 -->
    <div v-else-if="totalRow" class="bg-gradient-to-r from-blue-50 to-indigo-50 rounded-lg shadow border border-blue-200 p-4 md:p-6">
      <h3 class="text-base md:text-lg font-semibold text-gray-900 mb-3 md:mb-4">年度支出总计</h3>
      <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-7 gap-3 md:gap-4">
        <div class="bg-white rounded-lg p-3 md:p-4 shadow-sm">
          <div class="text-xs text-gray-600 mb-1">基础支出</div>
          <div class="text-lg md:text-xl font-bold text-gray-900">{{ formatCurrency(totalRow.baseExpenseAmount) }}</div>
        </div>
        <div class="bg-white rounded-lg p-3 md:p-4 shadow-sm">
          <div class="text-xs text-gray-600 mb-1">特殊支出</div>
          <div class="text-lg md:text-xl font-bold text-orange-600">{{ formatCurrency(totalRow.specialExpense || 0) }}</div>
        </div>
        <div class="bg-white rounded-lg p-3 md:p-4 shadow-sm">
          <div class="text-xs text-gray-600 mb-1">资产调整</div>
          <div class="text-lg md:text-xl font-bold text-red-600">
            {{ totalRow.assetAdjustment > 0 ? '-' : '' }}{{ formatCurrency(Math.abs(totalRow.assetAdjustment)) }}
          </div>
        </div>
        <div class="bg-white rounded-lg p-3 md:p-4 shadow-sm">
          <div class="text-xs text-gray-600 mb-1">负债调整</div>
          <div class="text-lg md:text-xl font-bold text-red-600">
            {{ totalRow.liabilityAdjustment > 0 ? '-' : '' }}{{ formatCurrency(Math.abs(totalRow.liabilityAdjustment)) }}
          </div>
        </div>
        <div class="bg-white rounded-lg p-3 md:p-4 shadow-sm">
          <div class="text-xs text-gray-600 mb-1">实际支出</div>
          <div class="text-lg md:text-xl font-bold text-blue-600">{{ formatCurrency(totalRow.actualExpenseAmount) }}</div>
        </div>
        <div class="bg-white rounded-lg p-3 md:p-4 shadow-sm">
          <div class="text-xs text-gray-600 mb-1">{{ selectedYear - 1 }}年支出</div>
          <div class="text-lg md:text-xl font-bold text-gray-700">{{ formatCurrency(lastYearTotalExpense) }}</div>
        </div>
        <div class="bg-white rounded-lg p-3 md:p-4 shadow-sm">
          <div class="text-xs text-gray-600 mb-1">同比增长</div>
          <div class="text-lg md:text-xl font-bold" :class="yearOverYearGrowth >= 0 ? 'text-red-600' : 'text-green-600'">
            {{ yearOverYearGrowth >= 0 ? '+' : '' }}{{ yearOverYearGrowth.toFixed(1) }}%
          </div>
        </div>
      </div>
    </div>

    <!-- 主内容 -->
    <div v-if="!loading" class="grid grid-cols-1 lg:grid-cols-2 gap-4 md:gap-6">
      <!-- 左侧：实际支出饼图 -->
      <div class="bg-white rounded-lg shadow border border-gray-200 p-4 md:p-6">
        <h3 class="text-md md:text-lg font-semibold mb-3 md:mb-4">实际支出分布</h3>
        <div class="h-96 md:h-[500px]">
          <canvas ref="actualExpenseChartCanvas"></canvas>
        </div>
      </div>

      <!-- 右侧：年度支出汇总表格 -->
      <div class="bg-white rounded-lg shadow border border-gray-200 p-4 md:p-6">
        <h3 class="text-md md:text-lg font-semibold mb-3 md:mb-4">年度支出汇总</h3>
        <div class="overflow-auto">
          <table class="min-w-full divide-y divide-gray-200 text-xs">
            <thead class="bg-gray-50">
              <tr>
                <th class="px-2 py-1.5 text-left text-[10px] font-medium text-gray-500 uppercase">分类</th>
                <th class="px-2 py-1.5 text-right text-[10px] font-medium text-gray-500 uppercase">基础支出</th>
                <th class="px-2 py-1.5 text-right text-[10px] font-medium text-gray-500 uppercase">特殊支出</th>
                <th class="px-2 py-1.5 text-right text-[10px] font-medium text-gray-500 uppercase">资产调整</th>
                <th class="px-2 py-1.5 text-right text-[10px] font-medium text-gray-500 uppercase">负债调整</th>
                <th class="px-2 py-1.5 text-right text-[10px] font-medium text-gray-500 uppercase">实际支出</th>
                <th class="px-2 py-1.5 text-right text-[10px] font-medium text-gray-500 uppercase">占比</th>
              </tr>
            </thead>
            <tbody class="bg-white divide-y divide-gray-200">
              <tr
                v-for="item in categoryData"
                :key="item.majorCategoryId"
                class="hover:bg-gray-50 transition-colors"
              >
                <td class="px-2 py-1.5 whitespace-nowrap">
                  <span v-if="item.majorCategoryIcon" class="text-sm mr-1">{{ item.majorCategoryIcon }}</span>
                  <span class="text-xs">{{ item.majorCategoryName }}</span>
                </td>
                <td class="px-2 py-1.5 text-right text-xs">
                  {{ formatCurrency(item.baseExpenseAmount) }}
                </td>
                <td class="px-2 py-1.5 text-right text-xs text-orange-600">
                  {{ formatCurrency(item.specialExpense || 0) }}
                </td>
                <td class="px-2 py-1.5 text-right text-xs" :class="item.assetAdjustment > 0 ? 'text-red-600' : 'text-gray-600'">
                  {{ item.assetAdjustment > 0 ? '-' : '' }}{{ formatCurrency(Math.abs(item.assetAdjustment)) }}
                </td>
                <td class="px-2 py-1.5 text-right text-xs" :class="item.liabilityAdjustment > 0 ? 'text-red-600' : 'text-gray-600'">
                  {{ item.liabilityAdjustment > 0 ? '-' : '' }}{{ formatCurrency(Math.abs(item.liabilityAdjustment)) }}
                </td>
                <td class="px-2 py-1.5 text-right text-xs font-medium">
                  {{ formatCurrency(item.actualExpenseAmount) }}
                </td>
                <td class="px-2 py-1.5 text-right text-xs text-gray-600">
                  {{ calculatePercentage(item.actualExpenseAmount) }}
                </td>
              </tr>
            </tbody>
            <tfoot v-if="totalRow" class="bg-blue-50 border-t-2 border-blue-200">
              <tr class="font-semibold">
                <td class="px-2 py-1.5 whitespace-nowrap">
                  <span class="font-bold text-xs">{{ totalRow.majorCategoryName }}</span>
                </td>
                <td class="px-2 py-1.5 text-right text-xs">
                  {{ formatCurrency(totalRow.baseExpenseAmount) }}
                </td>
                <td class="px-2 py-1.5 text-right text-xs text-orange-600">
                  {{ formatCurrency(totalRow.specialExpense || 0) }}
                </td>
                <td class="px-2 py-1.5 text-right text-xs text-red-600">
                  {{ totalRow.assetAdjustment > 0 ? '-' : '' }}{{ formatCurrency(Math.abs(totalRow.assetAdjustment)) }}
                </td>
                <td class="px-2 py-1.5 text-right text-xs text-red-600">
                  {{ totalRow.liabilityAdjustment > 0 ? '-' : '' }}{{ formatCurrency(Math.abs(totalRow.liabilityAdjustment)) }}
                </td>
                <td class="px-2 py-1.5 text-right text-xs font-bold">
                  {{ formatCurrency(totalRow.actualExpenseAmount) }}
                </td>
                <td class="px-2 py-1.5 text-right text-xs">
                  -
                </td>
              </tr>
            </tfoot>
          </table>
        </div>
      </div>
    </div>

    <!-- 说明文字 -->
    <div class="bg-blue-50 border border-blue-200 rounded-lg p-4 text-xs md:text-sm text-gray-700">
      <p class="font-semibold mb-2">💡 说明：</p>
      <ul class="list-disc list-inside space-y-1">
        <li><strong>基础支出</strong>：从支出记录直接汇总的年度支出金额</li>
        <li><strong>特殊支出</strong>：单笔 ≥ $10,000 的大额支出（橙色标记）</li>
        <li><strong>资产调整</strong>：当年度资产增加部分（如保险现金价值增加），应从支出中扣除</li>
        <li><strong>负债调整</strong>：当年度负债减少部分（如房贷本金偿还），应从支出中扣除</li>
        <li><strong>实际支出</strong>：基础支出 + 特殊支出 - 资产调整 - 负债调整 = 真实消费金额</li>
      </ul>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, watch } from 'vue'
import { Chart, registerables } from 'chart.js'
import ChartDataLabels from 'chartjs-plugin-datalabels'
import { familyAPI } from '@/api/family'
import { expenseAnalysisAPI, expenseCategoryAPI } from '@/api/expense'

Chart.register(...registerables, ChartDataLabels)

export default {
  name: 'ExpenseAnnualActual',
  setup() {
    // 响应式数据
    const families = ref([])
    const selectedFamilyId = ref(null)
    const selectedYear = ref(new Date().getFullYear())
    const loading = ref(false)
    const refreshing = ref(false)
    const summaryData = ref([])
    const lastYearTotalExpense = ref(0)

    // Chart实例
    const actualExpenseChart = ref(null)
    const actualExpenseChartCanvas = ref(null)

    // 计算属性
    const availableYears = computed(() => {
      const currentYear = new Date().getFullYear()
      const years = []
      for (let year = currentYear; year >= currentYear - 10; year--) {
        years.push(year)
      }
      return years
    })

    // 分类数据（排除总计行）
    const categoryData = computed(() => {
      return summaryData.value.filter(item => item.majorCategoryId !== 0)
    })

    // 总计行（majorCategoryId为0）
    const totalRow = computed(() => {
      return summaryData.value.find(item => item.majorCategoryId === 0)
    })

    // 总实际支出（排除总计行）
    const totalActualExpense = computed(() => {
      return categoryData.value.reduce((sum, item) => sum + parseFloat(item.actualExpenseAmount || 0), 0)
    })

    // 同比增长率（基于实际支出）
    const yearOverYearGrowth = computed(() => {
      if (!totalRow.value || lastYearTotalExpense.value === 0) return 0
      const currentYear = parseFloat(totalRow.value.actualExpenseAmount || 0)
      return ((currentYear - lastYearTotalExpense.value) / lastYearTotalExpense.value) * 100
    })

    // 加载家庭列表
    const loadFamilies = async () => {
      try {
        const response = await familyAPI.getAll()
        let data = response.data

        if (Array.isArray(data)) {
          families.value = data
        } else if (data && data.data && Array.isArray(data.data)) {
          families.value = data.data
        } else if (data && data.success && Array.isArray(data.data)) {
          families.value = data.data
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

    // 加载年度支出汇总数据
    const loadSummaryData = async () => {
      if (!selectedFamilyId.value) return

      loading.value = true
      try {
        // 首先加载所有大类
        const categoriesResponse = await expenseCategoryAPI.getAll()
        let allCategories = []
        if (Array.isArray(categoriesResponse.data)) {
          allCategories = categoriesResponse.data
        } else if (categoriesResponse.data && categoriesResponse.data.data) {
          allCategories = categoriesResponse.data.data
        } else if (categoriesResponse.data && 'success' in categoriesResponse.data) {
          allCategories = categoriesResponse.data.data || []
        }

        // 加载当前年份实际支出数据
        const response = await expenseAnalysisAPI.getAnnualSummary(
          selectedFamilyId.value,
          selectedYear.value,
          'USD',
          true
        )

        let actualExpenseData = []
        if (response && response.success) {
          actualExpenseData = response.data || []
        }

        // 创建实际支出数据的映射（majorCategoryId -> 数据）
        const expenseMap = new Map()
        actualExpenseData.forEach(item => {
          expenseMap.set(item.majorCategoryId, item)
        })

        // 合并：确保所有大类都显示（即使支出为0）
        const mergedData = []

        // 添加所有大类，即使没有支出记录
        allCategories.forEach(category => {
          if (expenseMap.has(category.id)) {
            // 有实际支出数据
            mergedData.push(expenseMap.get(category.id))
          } else {
            // 没有支出数据，创建零值记录
            mergedData.push({
              majorCategoryId: category.id,
              majorCategoryName: category.name,
              majorCategoryIcon: category.icon,
              baseExpenseAmount: 0,
              assetAdjustment: 0,
              liabilityAdjustment: 0,
              actualExpenseAmount: 0
            })
          }
        })

        // 添加总计行（majorCategoryId = 0）
        const totalRow = expenseMap.get(0)
        if (totalRow) {
          mergedData.push(totalRow)
        }

        summaryData.value = mergedData

        // 加载上一年数据（仅获取总计）
        try {
          const lastYearResponse = await expenseAnalysisAPI.getAnnualSummary(
            selectedFamilyId.value,
            selectedYear.value - 1,
            'USD',
            true
          )

          if (lastYearResponse && lastYearResponse.success) {
            const lastYearData = lastYearResponse.data || []
            const lastYearTotal = lastYearData.find(item => item.majorCategoryId === 0)
            // 使用实际支出（调整后的支出）进行对比
            lastYearTotalExpense.value = lastYearTotal ? parseFloat(lastYearTotal.actualExpenseAmount || 0) : 0
          } else {
            lastYearTotalExpense.value = 0
          }
        } catch (error) {
          console.error('加载上一年数据失败:', error)
          lastYearTotalExpense.value = 0
        }

        // 更新饼图
        setTimeout(() => updateActualExpenseChart(), 100)
      } catch (error) {
        console.error('加载年度支出汇总失败:', error)
        summaryData.value = []
        lastYearTotalExpense.value = 0
      } finally {
        loading.value = false
      }
    }

    // 更新实际支出饼图
    const updateActualExpenseChart = () => {
      if (!actualExpenseChartCanvas.value) return

      if (actualExpenseChart.value) {
        actualExpenseChart.value.destroy()
      }

      // 饼图只显示非零支出的分类
      const nonZeroCategories = categoryData.value.filter(d => parseFloat(d.actualExpenseAmount || 0) > 0)

      const ctx = actualExpenseChartCanvas.value.getContext('2d')
      actualExpenseChart.value = new Chart(ctx, {
        type: 'pie',
        data: {
          labels: nonZeroCategories.map(d => `${d.majorCategoryIcon || ''} ${d.majorCategoryName}`),
          datasets: [{
            data: nonZeroCategories.map(d => d.actualExpenseAmount),
            backgroundColor: [
              '#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4', '#FFEAA7',
              '#DFE6E9', '#74B9FF', '#A29BFE', '#FD79A8', '#FDCB6E'
            ]
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: true,
          plugins: {
            legend: {
              position: 'bottom',
              labels: {
                padding: 12,
                font: {
                  size: 12
                }
              }
            },
            tooltip: {
              callbacks: {
                label: (context) => {
                  const value = context.parsed
                  const total = nonZeroCategories.reduce((sum, d) => sum + parseFloat(d.actualExpenseAmount || 0), 0)
                  const percentage = ((value / total) * 100).toFixed(1)
                  return ` ${formatChartAmount(value)} (${percentage}%)`
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
                const total = nonZeroCategories.reduce((sum, d) => sum + parseFloat(d.actualExpenseAmount || 0), 0)
                const percentage = ((value / total) * 100).toFixed(1)
                // 只显示占比大于5%的标签，避免拥挤
                if (percentage >= 5) {
                  return `${label}\n${percentage}%`
                }
                return ''
              },
              textAlign: 'center'
            }
          }
        }
      })
    }

    // 计算百分比
    const calculatePercentage = (amount) => {
      if (totalActualExpense.value === 0) return '0.0%'
      return ((parseFloat(amount) / totalActualExpense.value) * 100).toFixed(1) + '%'
    }

    // 格式化金额
    const formatCurrency = (amount) => {
      return '$' + parseFloat(amount).toLocaleString('en-US', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
      })
    }

    // 格式化图表金额（K/M格式）
    const formatChartAmount = (value) => {
      if (value >= 1000000) {
        return '$' + (value / 1000000).toFixed(2) + 'M'
      } else if (value >= 1000) {
        return '$' + (value / 1000).toFixed(2) + 'K'
      } else {
        return '$' + value.toFixed(2)
      }
    }

    // 刷新数据（触发存储过程）
    const handleRefresh = async () => {
      if (!selectedFamilyId.value) {
        alert('请先选择家庭')
        return
      }

      refreshing.value = true
      try {
        // 调用存储过程刷新年度支出汇总
        await expenseAnalysisAPI.refreshAnnualSummary(selectedFamilyId.value, selectedYear.value)

        // 重新加载数据
        await loadSummaryData()

        alert('✅ 数据刷新成功！')
      } catch (error) {
        console.error('刷新失败:', error)
        alert('❌ 刷新失败: ' + (error.message || '未知错误'))
      } finally {
        refreshing.value = false
      }
    }

    // 监听选项变化
    watch([selectedFamilyId, selectedYear], () => {
      loadSummaryData()
    })

    // 组件挂载时
    onMounted(async () => {
      await loadFamilies()
      await loadSummaryData()
    })

    return {
      families,
      selectedFamilyId,
      selectedYear,
      availableYears,
      loading,
      refreshing,
      summaryData,
      categoryData,
      totalRow,
      lastYearTotalExpense,
      yearOverYearGrowth,
      actualExpenseChartCanvas,
      formatCurrency,
      calculatePercentage,
      handleRefresh
    }
  }
}
</script>

<style scoped>
/* 自定义样式 */
</style>
