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
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="text-center py-12">
      <div class="text-gray-500">加载中...</div>
    </div>

    <!-- 主内容 -->
    <div v-else class="grid grid-cols-1 lg:grid-cols-2 gap-4 md:gap-6">
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
        <li><strong>资产调整</strong>：当年度资产增加部分（如保险现金价值增加），应从支出中扣除</li>
        <li><strong>负债调整</strong>：当年度负债减少部分（如房贷本金偿还），应从支出中扣除</li>
        <li><strong>实际支出</strong>：基础支出 - 资产调整 - 负债调整 = 真实消费金额</li>
      </ul>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, watch } from 'vue'
import { Chart, registerables } from 'chart.js'
import ChartDataLabels from 'chartjs-plugin-datalabels'
import { familyAPI } from '@/api/family'
import { expenseAnalysisAPI } from '@/api/expense'

Chart.register(...registerables, ChartDataLabels)

export default {
  name: 'ExpenseAnnualActual',
  setup() {
    // 响应式数据
    const families = ref([])
    const selectedFamilyId = ref(null)
    const selectedYear = ref(new Date().getFullYear())
    const loading = ref(false)
    const summaryData = ref([])

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
        const response = await expenseAnalysisAPI.getAnnualSummary(
          selectedFamilyId.value,
          selectedYear.value,
          'USD',
          true
        )

        if (response && response.success) {
          summaryData.value = response.data || []
        } else {
          summaryData.value = []
        }

        // 更新饼图
        setTimeout(() => updateActualExpenseChart(), 100)
      } catch (error) {
        console.error('加载年度支出汇总失败:', error)
        summaryData.value = []
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

      const ctx = actualExpenseChartCanvas.value.getContext('2d')
      actualExpenseChart.value = new Chart(ctx, {
        type: 'pie',
        data: {
          labels: categoryData.value.map(d => `${d.majorCategoryIcon || ''} ${d.majorCategoryName}`),
          datasets: [{
            data: categoryData.value.map(d => d.actualExpenseAmount),
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
                  const percentage = ((value / totalActualExpense.value) * 100).toFixed(1)
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
                const label = categoryData.value[context.dataIndex].majorCategoryName
                const percentage = ((value / totalActualExpense.value) * 100).toFixed(1)
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
      summaryData,
      categoryData,
      totalRow,
      actualExpenseChartCanvas,
      formatCurrency,
      calculatePercentage
    }
  }
}
</script>

<style scoped>
/* 自定义样式 */
</style>
