<template>
  <div class="p-3 md:p-6 space-y-4 md:space-y-6">
    <!-- 页面标题和筛选控制区 -->
    <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
      <div>
        <h1 class="text-xl md:text-2xl font-bold text-gray-900">现金流整合视图</h1>
        <p class="text-xs md:text-sm text-gray-600 mt-1">分析收支对比、储蓄率趋势和现金流量</p>
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

    <!-- 现金流概览卡片 -->
    <div v-if="cashFlowSummary" class="bg-gradient-to-r from-blue-50 to-indigo-50 rounded-lg shadow border border-blue-200 p-4">
      <h3 class="text-base font-semibold text-gray-900 mb-3">现金流概览</h3>
      <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-3">
        <div class="bg-white rounded-lg p-3 shadow-sm">
          <div class="text-xs text-gray-600 mb-1">总收入</div>
          <div class="text-lg font-bold text-green-600">{{ formatCurrency(cashFlowSummary.totalIncome) }}</div>
        </div>
        <div class="bg-white rounded-lg p-3 shadow-sm">
          <div class="text-xs text-gray-600 mb-1">总支出</div>
          <div class="text-lg font-bold text-red-600">{{ formatCurrency(cashFlowSummary.totalExpense) }}</div>
        </div>
        <div class="bg-white rounded-lg p-3 shadow-sm">
          <div class="text-xs text-gray-600 mb-1">净现金流</div>
          <div class="text-lg font-bold" :class="cashFlowSummary.netCashFlow >= 0 ? 'text-blue-600' : 'text-red-600'">
            {{ formatCurrency(cashFlowSummary.netCashFlow) }}
          </div>
        </div>
        <div class="bg-white rounded-lg p-3 shadow-sm">
          <div class="text-xs text-gray-600 mb-1">储蓄率</div>
          <div class="text-lg font-bold text-purple-600">{{ cashFlowSummary.savingsRate.toFixed(1) }}%</div>
        </div>
        <div class="bg-white rounded-lg p-3 shadow-sm">
          <div class="text-xs text-gray-600 mb-1">平均月度结余</div>
          <div class="text-lg font-bold text-indigo-600">{{ formatCurrency(cashFlowSummary.netCashFlow / 12) }}</div>
        </div>
      </div>
    </div>

    <!-- 收支对比分析 - 横向柱状图 -->
    <div class="bg-white rounded-lg shadow border border-gray-200 p-4">
      <h3 class="text-base font-semibold text-gray-900 mb-3">月度收支对比</h3>
      <div class="h-[400px]">
        <canvas ref="incomeExpenseChartCanvas" v-if="monthlyData.length > 0"></canvas>
        <div v-else class="h-full flex items-center justify-center text-gray-500 text-sm">
          暂无数据
        </div>
      </div>
    </div>

    <!-- 储蓄率趋势图 -->
    <div class="bg-white rounded-lg shadow border border-gray-200 p-4">
      <h3 class="text-base font-semibold text-gray-900 mb-3">储蓄率趋势</h3>
      <div class="h-[350px]">
        <canvas ref="savingsRateChartCanvas" v-if="monthlyData.length > 0"></canvas>
        <div v-else class="h-full flex items-center justify-center text-gray-500 text-sm">
          暂无数据
        </div>
      </div>
    </div>

    <!-- 现金流量表 -->
    <div class="bg-white rounded-lg shadow border border-gray-200 p-4">
      <h3 class="text-base font-semibold text-gray-900 mb-3">现金流量表</h3>
      <div class="overflow-x-auto">
        <table class="min-w-full divide-y divide-gray-200">
          <thead class="bg-gray-50">
            <tr>
              <th class="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">月份</th>
              <th class="px-4 py-2 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">收入</th>
              <th class="px-4 py-2 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">支出</th>
              <th class="px-4 py-2 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">净现金流</th>
              <th class="px-4 py-2 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">储蓄率</th>
              <th class="px-4 py-2 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">累计结余</th>
            </tr>
          </thead>
          <tbody class="bg-white divide-y divide-gray-200">
            <tr v-for="data in monthlyData" :key="data.month" class="hover:bg-gray-50">
              <td class="px-4 py-2 text-sm text-gray-900">{{ data.month }}月</td>
              <td class="px-4 py-2 text-sm text-right text-green-600 font-medium">{{ formatCurrency(data.income) }}</td>
              <td class="px-4 py-2 text-sm text-right text-red-600 font-medium">{{ formatCurrency(data.expense) }}</td>
              <td class="px-4 py-2 text-sm text-right font-medium" :class="data.netCashFlow >= 0 ? 'text-blue-600' : 'text-red-600'">
                {{ formatCurrency(data.netCashFlow) }}
              </td>
              <td class="px-4 py-2 text-sm text-right" :class="getSavingsRateClass(data.savingsRate)">
                {{ data.savingsRate.toFixed(1) }}%
              </td>
              <td class="px-4 py-2 text-sm text-right text-gray-700 font-medium">{{ formatCurrency(data.cumulative) }}</td>
            </tr>
          </tbody>
          <tfoot class="bg-gray-100 font-semibold">
            <tr>
              <td class="px-4 py-2 text-sm text-gray-900">合计</td>
              <td class="px-4 py-2 text-sm text-right text-green-600">{{ formatCurrency(cashFlowSummary?.totalIncome || 0) }}</td>
              <td class="px-4 py-2 text-sm text-right text-red-600">{{ formatCurrency(cashFlowSummary?.totalExpense || 0) }}</td>
              <td class="px-4 py-2 text-sm text-right" :class="cashFlowSummary?.netCashFlow >= 0 ? 'text-blue-600' : 'text-red-600'">
                {{ formatCurrency(cashFlowSummary?.netCashFlow || 0) }}
              </td>
              <td class="px-4 py-2 text-sm text-right text-purple-600">{{ cashFlowSummary?.savingsRate.toFixed(1) || 0 }}%</td>
              <td class="px-4 py-2 text-sm text-right text-gray-900">-</td>
            </tr>
          </tfoot>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed, nextTick } from 'vue';
import { Chart, registerables } from 'chart.js';
import { familyAPI } from '@/api/family';
import { incomeRecordAPI } from '@/api/income';
import { expenseRecordAPI } from '@/api/expense';

Chart.register(...registerables);

// 响应式数据
const families = ref([]);
const selectedFamilyId = ref(null);
const selectedYear = ref(new Date().getFullYear());
const selectedCurrency = ref('All');
const refreshing = ref(false);

// 收入和支出数据
const incomeData = ref([]);
const expenseData = ref([]);
const monthlyData = ref([]);
const cashFlowSummary = ref(null);

// 图表引用
const incomeExpenseChartCanvas = ref(null);
const savingsRateChartCanvas = ref(null);
let incomeExpenseChart = null;
let savingsRateChart = null;

// 可选年份
const availableYears = computed(() => {
  const currentYear = new Date().getFullYear();
  const years = [];
  for (let i = currentYear; i >= 2020; i--) {
    years.push(i);
  }
  return years;
});

// 币种选项
const currencies = ['All', 'CNY', 'USD'];

// 格式化金额
const formatCurrency = (amount, currency = selectedCurrency.value) => {
  const absAmount = Math.abs(amount || 0);
  const formattedAmount = new Intl.NumberFormat('zh-CN', {
    style: 'decimal',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(absAmount);

  let symbol = '$'; // 默认USD
  if (currency === 'CNY') {
    symbol = '¥';
  } else if (currency === 'All') {
    symbol = '$'; // All模式显示为USD
  }

  const sign = amount < 0 ? '-' : '';
  return `${sign}${symbol}${formattedAmount}`;
};

// 获取储蓄率颜色类
const getSavingsRateClass = (rate) => {
  if (rate >= 30) return 'text-green-600 font-semibold';
  if (rate >= 20) return 'text-blue-600';
  if (rate >= 10) return 'text-yellow-600';
  return 'text-red-600';
};

// 加载家庭列表
const loadFamilies = async () => {
  try {
    const response = await familyAPI.getAll();
    if (response.data) {
      families.value = response.data;
      if (families.value.length > 0 && !selectedFamilyId.value) {
        selectedFamilyId.value = families.value[0].id;
      }
    }
  } catch (error) {
    console.error('加载家庭列表失败:', error);
  }
};

// 加载收入数据 - 直接从记录中按月汇总
const loadIncomeData = async () => {
  if (!selectedFamilyId.value) return;

  try {
    const startPeriod = `${selectedYear.value}-01`;
    const endPeriod = `${selectedYear.value}-12`;

    // 获取期间范围的记录
    const response = await incomeRecordAPI.getByPeriodRange(
      selectedFamilyId.value,
      startPeriod,
      endPeriod
    );

    if (response.data) {
      const records = response.data;

      // 初始化月度数据（1-12月）
      const monthlyTotals = Array(12).fill(0).map((_, index) => ({
        month: index + 1,
        totalAmount: 0
      }));

      // 按月汇总金额
      records.forEach(record => {
        // 排除投资回报（major_category_id = 3）
        if (record.majorCategoryId === 3) {
          return;
        }

        const period = record.period; // income_records使用period字段，格式: "2025-01"
        const monthMatch = period.match(/-(\d{2})$/);
        if (!monthMatch) return;

        const month = parseInt(monthMatch[1], 10);
        const monthIndex = month - 1;

        if (monthlyTotals[monthIndex]) {
          // 根据选择的货币，使用对应的金额
          let amount = 0;
          if (selectedCurrency.value === 'All') {
            // All模式：使用amountUsd（所有币种折算为USD）
            amount = parseFloat(record.amountUsd || 0);
          } else if (selectedCurrency.value === 'CNY') {
            // CNY模式：只统计CNY记录
            if (record.currency === 'CNY') {
              amount = parseFloat(record.amount || 0);
            }
          } else if (selectedCurrency.value === 'USD') {
            // USD模式：只统计USD记录
            if (record.currency === 'USD') {
              amount = parseFloat(record.amount || 0);
            }
          }

          monthlyTotals[monthIndex].totalAmount += amount;
        }
      });

      incomeData.value = monthlyTotals;
    }
  } catch (error) {
    console.error('加载收入数据失败:', error);
    incomeData.value = [];
  }
};

// 加载支出数据 - 直接从记录中按月汇总
const loadExpenseData = async () => {
  if (!selectedFamilyId.value) return;

  try {
    const startPeriod = `${selectedYear.value}-01`;
    const endPeriod = `${selectedYear.value}-12`;

    // 获取期间范围的记录
    const response = await expenseRecordAPI.getByPeriodRange(
      selectedFamilyId.value,
      startPeriod,
      endPeriod
    );

    if (response.data) {
      const records = response.data;

      // 初始化月度数据（1-12月）
      const monthlyTotals = Array(12).fill(0).map((_, index) => ({
        month: index + 1,
        totalAmount: 0
      }));

      // 按月汇总金额
      records.forEach(record => {
        const period = record.expensePeriod; // expense_records使用expensePeriod字段，格式: "2025-01"
        const monthMatch = period.match(/-(\d{2})$/);
        if (!monthMatch) return;

        const month = parseInt(monthMatch[1], 10);
        const monthIndex = month - 1;

        if (monthlyTotals[monthIndex]) {
          // 根据选择的货币，使用对应的金额
          let amount = 0;
          if (selectedCurrency.value === 'All') {
            // All模式：所有币种都统计（注意：expense_records没有USD转换字段）
            // 对于CNY记录，需要手动转换为USD（简化处理：假设1 CNY = 0.14 USD）
            if (record.currency === 'CNY') {
              amount = parseFloat(record.amount || 0) * 0.14; // CNY转USD的近似汇率
            } else {
              amount = parseFloat(record.amount || 0);
            }
          } else if (selectedCurrency.value === 'CNY') {
            // CNY模式：只统计CNY记录
            if (record.currency === 'CNY') {
              amount = parseFloat(record.amount || 0);
            }
          } else if (selectedCurrency.value === 'USD') {
            // USD模式：只统计USD记录
            if (record.currency === 'USD') {
              amount = parseFloat(record.amount || 0);
            }
          }

          monthlyTotals[monthIndex].totalAmount += amount;
        }
      });

      expenseData.value = monthlyTotals;
    }
  } catch (error) {
    console.error('加载支出数据失败:', error);
    expenseData.value = [];
  }
};

// 计算月度现金流数据
const calculateMonthlyData = () => {
  const data = [];
  let cumulative = 0;

  for (let month = 1; month <= 12; month++) {
    const incomeRecord = incomeData.value.find(d => d.month === month) || { totalAmount: 0 };
    const expenseRecord = expenseData.value.find(d => d.month === month) || { totalAmount: 0 };

    const income = incomeRecord.totalAmount || 0;
    const expense = expenseRecord.totalAmount || 0;
    const netCashFlow = income - expense;
    const savingsRate = income > 0 ? (netCashFlow / income) * 100 : 0;

    cumulative += netCashFlow;

    data.push({
      month,
      income,
      expense,
      netCashFlow,
      savingsRate,
      cumulative
    });
  }

  monthlyData.value = data;

  // 计算现金流汇总
  const totalIncome = data.reduce((sum, d) => sum + d.income, 0);
  const totalExpense = data.reduce((sum, d) => sum + d.expense, 0);
  const netCashFlow = totalIncome - totalExpense;
  const savingsRate = totalIncome > 0 ? (netCashFlow / totalIncome) * 100 : 0;

  cashFlowSummary.value = {
    totalIncome,
    totalExpense,
    netCashFlow,
    savingsRate
  };
};

// 创建收支对比图表
const createIncomeExpenseChart = () => {
  if (!incomeExpenseChartCanvas.value) return;

  const ctx = incomeExpenseChartCanvas.value.getContext('2d');

  if (incomeExpenseChart) {
    incomeExpenseChart.destroy();
  }

  const labels = monthlyData.value.map(d => `${d.month}月`);
  const incomeValues = monthlyData.value.map(d => d.income);
  const expenseValues = monthlyData.value.map(d => d.expense);
  const netCashFlowValues = monthlyData.value.map(d => d.netCashFlow);

  incomeExpenseChart = new Chart(ctx, {
    type: 'bar',
    data: {
      labels,
      datasets: [
        {
          label: '收入',
          data: incomeValues,
          backgroundColor: 'rgba(34, 197, 94, 0.7)',
          borderColor: 'rgba(34, 197, 94, 1)',
          borderWidth: 1
        },
        {
          label: '支出',
          data: expenseValues,
          backgroundColor: 'rgba(239, 68, 68, 0.7)',
          borderColor: 'rgba(239, 68, 68, 1)',
          borderWidth: 1
        },
        {
          label: '净现金流',
          data: netCashFlowValues,
          backgroundColor: 'rgba(59, 130, 246, 0.7)',
          borderColor: 'rgba(59, 130, 246, 1)',
          borderWidth: 1,
          type: 'line',
          yAxisID: 'y1'
        }
      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      interaction: {
        mode: 'index',
        intersect: false
      },
      scales: {
        y: {
          type: 'linear',
          display: true,
          position: 'left',
          title: {
            display: true,
            text: '金额'
          },
          ticks: {
            callback: (value) => formatCurrency(value).replace(/[¥$]/, '')
          }
        },
        y1: {
          type: 'linear',
          display: true,
          position: 'right',
          title: {
            display: true,
            text: '净现金流'
          },
          grid: {
            drawOnChartArea: false
          },
          ticks: {
            callback: (value) => formatCurrency(value).replace(/[¥$]/, '')
          }
        }
      },
      plugins: {
        tooltip: {
          callbacks: {
            label: (context) => {
              return `${context.dataset.label}: ${formatCurrency(context.parsed.y)}`;
            }
          }
        },
        legend: {
          position: 'top'
        }
      }
    }
  });
};

// 创建储蓄率趋势图
const createSavingsRateChart = () => {
  if (!savingsRateChartCanvas.value) return;

  const ctx = savingsRateChartCanvas.value.getContext('2d');

  if (savingsRateChart) {
    savingsRateChart.destroy();
  }

  const labels = monthlyData.value.map(d => `${d.month}月`);
  const savingsRateValues = monthlyData.value.map(d => d.savingsRate);

  savingsRateChart = new Chart(ctx, {
    type: 'line',
    data: {
      labels,
      datasets: [
        {
          label: '储蓄率 (%)',
          data: savingsRateValues,
          borderColor: 'rgba(147, 51, 234, 1)',
          backgroundColor: 'rgba(147, 51, 234, 0.1)',
          borderWidth: 2,
          fill: true,
          tension: 0.4
        }
      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      scales: {
        y: {
          beginAtZero: true,
          title: {
            display: true,
            text: '储蓄率 (%)'
          },
          ticks: {
            callback: (value) => `${value.toFixed(0)}%`
          }
        }
      },
      plugins: {
        tooltip: {
          callbacks: {
            label: (context) => {
              return `储蓄率: ${context.parsed.y.toFixed(1)}%`;
            }
          }
        },
        legend: {
          position: 'top'
        }
      }
    }
  });
};

// 刷新数据
const refreshData = async () => {
  refreshing.value = true;
  try {
    await Promise.all([loadIncomeData(), loadExpenseData()]);
    calculateMonthlyData();
    await nextTick();
    createIncomeExpenseChart();
    createSavingsRateChart();
  } catch (error) {
    console.error('刷新数据失败:', error);
  } finally {
    refreshing.value = false;
  }
};

// 初始化
onMounted(async () => {
  await loadFamilies();
  await refreshData();
});

// 监听筛选条件变化
watch([selectedFamilyId, selectedYear, selectedCurrency], () => {
  refreshData();
});
</script>
