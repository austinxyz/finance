<template>
  <div class="p-3 md:p-6 space-y-4 md:space-y-6">
    <!-- Header -->
    <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
      <div>
        <h1 class="text-xl md:text-2xl font-bold text-gray-900">资金跑道分析</h1>
        <p class="text-xs md:text-sm text-gray-600 mt-1">测算流动资产在不工作情况下能维持多久</p>
      </div>
      <button
        @click="fetchData"
        :disabled="loading"
        class="px-3 py-2 bg-blue-600 text-white text-sm rounded-lg hover:bg-blue-700 disabled:opacity-50 flex items-center gap-2"
      >
        <svg v-if="loading" class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"/>
        </svg>
        <span>{{ loading ? '计算中...' : '刷新' }}</span>
      </button>
    </div>

    <!-- Controls -->
    <div class="bg-white rounded-xl border border-gray-200 p-4 space-y-3">
      <div class="flex flex-col sm:flex-row gap-4">
        <!-- Lookback window -->
        <div class="flex items-center gap-2">
          <label class="text-sm font-medium text-gray-700 whitespace-nowrap">支出回溯：</label>
          <select
            v-model="selectedMonths"
            @change="fetchData"
            class="px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
          >
            <option :value="3">近 3 个月</option>
            <option :value="6">近 6 个月</option>
            <option :value="12">近 12 个月</option>
          </select>
        </div>

        <!-- Scenario multipliers -->
        <div class="flex items-center gap-3">
          <label class="text-sm font-medium text-gray-700 whitespace-nowrap">乐观倍数：</label>
          <input
            v-model.number="optimisticMultiplier"
            type="number" min="0.1" max="1" step="0.05"
            class="w-20 px-2 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-green-500"
          />
          <label class="text-sm font-medium text-gray-700 whitespace-nowrap">悲观倍数：</label>
          <input
            v-model.number="pessimisticMultiplier"
            type="number" min="1" max="3" step="0.05"
            class="w-20 px-2 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-red-500"
          />
        </div>
      </div>
    </div>

    <!-- Warnings -->
    <div v-if="data && (data.assetDataMissing || data.expenseDataWarning)" class="space-y-2">
      <div
        v-if="data.assetDataMissing"
        class="flex items-start gap-2 bg-amber-50 border border-amber-200 rounded-lg p-3"
      >
        <svg class="w-5 h-5 text-amber-500 mt-0.5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/>
        </svg>
        <span class="text-sm text-amber-700">
          资产数据可能不是最新的
          <span v-if="data.latestSnapshotDate">（最近快照：{{ data.latestSnapshotDate }}）</span>
          ，请在资产管理页更新数据后重新计算。
        </span>
      </div>
      <div
        v-if="data.expenseDataWarning"
        class="flex items-start gap-2 bg-amber-50 border border-amber-200 rounded-lg p-3"
      >
        <svg class="w-5 h-5 text-amber-500 mt-0.5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/>
        </svg>
        <span class="text-sm text-amber-700">
          支出数据仅覆盖 {{ data.expenseMonthsUsed }} 个月，估算基础较少，结果仅供参考。
        </span>
      </div>
    </div>

    <!-- Error state -->
    <div v-if="error" class="bg-red-50 border border-red-200 rounded-lg p-4 text-sm text-red-700">
      {{ error }}
    </div>

    <!-- Loading skeleton -->
    <div v-if="loading && !data" class="grid grid-cols-2 lg:grid-cols-4 gap-4">
      <div v-for="i in 4" :key="i" class="bg-white rounded-xl border border-gray-200 p-4 animate-pulse">
        <div class="h-4 bg-gray-200 rounded w-3/4 mb-3"/>
        <div class="h-8 bg-gray-200 rounded w-1/2"/>
      </div>
    </div>

    <!-- Summary Cards -->
    <div v-if="data" class="grid grid-cols-2 lg:grid-cols-4 gap-4">
      <div class="bg-white rounded-xl border border-gray-200 p-4">
        <p class="text-xs text-gray-500 mb-1">流动资产总额</p>
        <p class="text-xl font-bold text-blue-600">{{ formatUSD(effectiveLiquidTotal) }}</p>
        <p class="text-xs text-gray-400 mt-1">
          {{ effectiveAccounts.length }} / {{ data.accountBreakdown?.length || 0 }} 个账户
          <span v-if="excludedAccounts.size > 0" class="text-amber-500">（已排除 {{ excludedAccounts.size }} 个）</span>
        </p>
      </div>
      <div class="bg-white rounded-xl border border-gray-200 p-4">
        <p class="text-xs text-gray-500 mb-1">月均支出（近 {{ data.expenseMonthsUsed }} 个月）</p>
        <p class="text-xl font-bold text-orange-600">{{ formatUSD(effectiveMonthlyBurn) }}</p>
        <p class="text-xs mt-1">
          <span class="text-gray-400">基准 {{ formatUSD(data.monthlyBurn) }}</span>
          <span v-if="totalAdjustment !== 0" class="ml-1" :class="totalAdjustment > 0 ? 'text-red-500' : 'text-green-500'">
            {{ totalAdjustment > 0 ? '+' : '' }}{{ formatUSD(totalAdjustment) }}
          </span>
        </p>
      </div>
      <div class="bg-white rounded-xl border border-gray-200 p-4">
        <p class="text-xs text-gray-500 mb-1">Runway（基准）</p>
        <p class="text-xl font-bold" :class="runwayColor(effectiveRunwayMonths)">
          {{ effectiveRunwayMonths != null ? effectiveRunwayMonths + ' 个月' : '—' }}
        </p>
        <p class="text-xs text-gray-400 mt-1">约 {{ effectiveRunwayMonths ? (effectiveRunwayMonths / 12).toFixed(1) : '?' }} 年</p>
      </div>
      <div class="bg-white rounded-xl border border-gray-200 p-4">
        <p class="text-xs text-gray-500 mb-1">预计耗尽日期</p>
        <p class="text-xl font-bold text-gray-700">{{ effectiveDepletionDate }}</p>
        <p class="text-xs text-gray-400 mt-1">按当前支出估算</p>
      </div>
    </div>

    <!-- Scenario Comparison -->
    <div v-if="data && effectiveRunwayMonths != null" class="bg-white rounded-xl border border-gray-200 overflow-hidden">
      <div class="px-4 py-3 border-b border-gray-100">
        <h2 class="text-sm font-semibold text-gray-700">情景对比</h2>
      </div>
      <div class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead class="bg-gray-50">
            <tr>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">情景</th>
              <th class="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">月支出</th>
              <th class="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">Runway（月）</th>
              <th class="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">耗尽日期</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-100">
            <tr v-for="s in scenarios" :key="s.label" :class="s.rowClass">
              <td class="px-4 py-3 font-medium" :class="s.textClass">{{ s.label }}</td>
              <td class="px-4 py-3 text-right text-gray-700">{{ formatUSD(s.burn) }}</td>
              <td class="px-4 py-3 text-right font-bold" :class="s.textClass">{{ s.months }} 个月</td>
              <td class="px-4 py-3 text-right text-gray-600">{{ s.depletion }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Account Breakdown Chart + Table -->
    <div v-if="data && data.accountBreakdown?.length" class="grid grid-cols-1 lg:grid-cols-2 gap-4">
      <!-- Doughnut chart -->
      <div class="bg-white rounded-xl border border-gray-200 p-4">
        <h2 class="text-sm font-semibold text-gray-700 mb-4">流动资产构成</h2>
        <div class="relative h-56">
          <canvas ref="assetChartRef"></canvas>
        </div>
      </div>

      <!-- Account table with per-account exclusion -->
      <div class="bg-white rounded-xl border border-gray-200 overflow-hidden">
        <div class="px-4 py-3 border-b border-gray-100 flex items-center justify-between">
          <h2 class="text-sm font-semibold text-gray-700">账户明细</h2>
          <span class="text-xs text-gray-400">勾选纳入计算的账户</span>
        </div>
        <div class="overflow-y-auto max-h-72">
          <table class="w-full text-sm">
            <thead class="bg-gray-50 sticky top-0">
              <tr>
                <th class="px-3 py-2 text-center text-xs font-medium text-gray-500 w-8">纳入</th>
                <th class="px-4 py-2 text-left text-xs font-medium text-gray-500">账户</th>
                <th class="px-4 py-2 text-left text-xs font-medium text-gray-500">类型</th>
                <th class="px-4 py-2 text-right text-xs font-medium text-gray-500">金额</th>
                <th class="px-4 py-2 text-right text-xs font-medium text-gray-500">占比</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-100">
              <tr
                v-for="acc in sortedAccounts"
                :key="acc.id"
                :class="excludedAccounts.has(acc.id) ? 'opacity-40 bg-gray-50' : ''"
              >
                <td class="px-3 py-2 text-center">
                  <input
                    type="checkbox"
                    :checked="!excludedAccounts.has(acc.id)"
                    @change="toggleAccount(acc.id)"
                    class="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                  />
                </td>
                <td class="px-4 py-2 font-medium text-gray-800">{{ acc.accountName }}</td>
                <td class="px-4 py-2 text-gray-500">{{ assetTypeLabel(acc.accountType) }}</td>
                <td class="px-4 py-2 text-right text-gray-700">{{ formatUSD(acc.usdValue) }}</td>
                <td class="px-4 py-2 text-right text-gray-500">
                  {{ pct(acc.usdValue, data.liquidTotal) }}%
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- Expense Breakdown with adjustments -->
    <div v-if="data && Object.keys(data.expenseBreakdown || {}).length" class="bg-white rounded-xl border border-gray-200 overflow-hidden">
      <div class="px-4 py-3 border-b border-gray-100 flex items-center justify-between">
        <h2 class="text-sm font-semibold text-gray-700">月均支出结构（按大类）</h2>
        <span class="text-xs text-gray-400">可在「调整」列输入预期变化额（正数=增加，负数=减少）</span>
      </div>
      <div class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead class="bg-gray-50">
            <tr>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">支出大类</th>
              <th class="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">月均金额</th>
              <th class="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">调整 (USD/月)</th>
              <th class="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">调整后</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-100">
            <tr v-for="[code, amount] in sortedExpenses" :key="code">
              <td class="px-4 py-3 font-medium text-gray-700">{{ expenseCategoryLabel(code) }}</td>
              <td class="px-4 py-3 text-right text-red-600 font-medium">{{ formatUSD(amount) }}</td>
              <td class="px-4 py-3 text-right">
                <input
                  type="number"
                  step="10"
                  :value="expenseAdjustments[code] || ''"
                  @input="setAdjustment(code, $event.target.value)"
                  placeholder="0"
                  class="w-24 px-2 py-1 text-right border border-gray-300 rounded text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  :class="(expenseAdjustments[code] || 0) > 0 ? 'text-red-600' : (expenseAdjustments[code] || 0) < 0 ? 'text-green-600' : 'text-gray-500'"
                />
              </td>
              <td class="px-4 py-3 text-right font-medium text-gray-800">
                {{ formatUSD(Number(amount) + (expenseAdjustments[code] || 0)) }}
              </td>
            </tr>
            <tr class="bg-gray-50 font-semibold">
              <td class="px-4 py-3 text-gray-800">合计</td>
              <td class="px-4 py-3 text-right text-orange-600">{{ formatUSD(data.monthlyBurn) }}</td>
              <td class="px-4 py-3 text-right" :class="totalAdjustment > 0 ? 'text-red-600' : totalAdjustment < 0 ? 'text-green-600' : 'text-gray-500'">
                {{ totalAdjustment !== 0 ? (totalAdjustment > 0 ? '+' : '') + formatUSD(totalAdjustment) : '—' }}
              </td>
              <td class="px-4 py-3 text-right text-orange-600">{{ formatUSD(effectiveMonthlyBurn) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { Chart, registerables } from 'chart.js'
import { useAuthStore } from '../../stores/auth'
import { runwayAPI } from '../../api/runway'

Chart.register(...registerables)

const authStore = useAuthStore()
const familyId = computed(() => authStore.familyId)

const data = ref(null)
const loading = ref(false)
const error = ref(null)

const selectedMonths = ref(6)
const optimisticMultiplier = ref(0.8)
const pessimisticMultiplier = ref(1.2)

// Per-account exclusion (stores account IDs that are excluded)
const excludedAccounts = ref(new Set())

// Per-category expense adjustments (USD/month delta)
const expenseAdjustments = ref({})

const assetTypeNames = {
  CASH: '现金',
  STOCKS: '股票账户',
  CRYPTOCURRENCY: '加密货币',
  PRECIOUS_METALS: '贵金属',
  RETIREMENT_FUND: '退休基金',
  INSURANCE: '保险',
  REAL_ESTATE: '房产',
  OTHER: '其他',
}

const expenseCategoryNames = {
  CHILDREN: '子女教育',
  CLOTHING: '服装',
  FOOD: '餐饮',
  HOUSING: '住房',
  TRANSPORTATION: '交通',
  INSURANCE: '保险',
  SOCIAL: '社交娱乐',
  ENTERTAINMENT: '娱乐',
  BUSINESS: '商业',
  OTHER: '其他',
}

const assetChartRef = ref(null)
let chartInstance = null

// ── Derived state ──────────────────────────────────────────

const effectiveAccounts = computed(() => {
  if (!data.value?.accountBreakdown) return []
  return data.value.accountBreakdown.filter(a => !excludedAccounts.value.has(a.id))
})

const effectiveLiquidTotal = computed(() =>
  effectiveAccounts.value.reduce((sum, a) => sum + Number(a.usdValue), 0)
)

const totalAdjustment = computed(() =>
  Object.values(expenseAdjustments.value).reduce((sum, v) => sum + (Number(v) || 0), 0)
)

const effectiveMonthlyBurn = computed(() => {
  if (!data.value?.monthlyBurn) return 0
  return Number(data.value.monthlyBurn) + totalAdjustment.value
})

const effectiveRunwayMonths = computed(() => {
  if (!effectiveMonthlyBurn.value || effectiveMonthlyBurn.value <= 0) return null
  if (!effectiveLiquidTotal.value || effectiveLiquidTotal.value <= 0) return null
  return Math.floor(effectiveLiquidTotal.value / effectiveMonthlyBurn.value)
})

const effectiveDepletionDate = computed(() => {
  if (effectiveRunwayMonths.value == null) return '—'
  return computeDepletion(effectiveRunwayMonths.value)
})

// ── Fetch ──────────────────────────────────────────────────

async function fetchData() {
  if (!familyId.value) return
  loading.value = true
  error.value = null
  try {
    const response = await runwayAPI.getAnalysis(familyId.value, selectedMonths.value)
    if (response.success) {
      data.value = response.data
      await nextTick()
      renderChart()
    } else {
      error.value = response.error || '加载失败'
    }
  } catch (e) {
    error.value = e.response?.data?.error || e.message || '请求失败，请检查后端连接'
  } finally {
    loading.value = false
  }
}

function toggleAccount(id) {
  const set = new Set(excludedAccounts.value)
  if (set.has(id)) set.delete(id)
  else set.add(id)
  excludedAccounts.value = set
}

function setAdjustment(code, value) {
  const num = parseFloat(value)
  const adj = { ...expenseAdjustments.value }
  if (!value || isNaN(num) || num === 0) {
    delete adj[code]
  } else {
    adj[code] = num
  }
  expenseAdjustments.value = adj
}

// ── Chart ──────────────────────────────────────────────────

function renderChart() {
  if (!assetChartRef.value || !effectiveAccounts.value.length) return
  if (chartInstance) chartInstance.destroy()

  const accounts = [...effectiveAccounts.value].sort((a, b) => b.usdValue - a.usdValue)
  const colors = [
    '#3B82F6', '#10B981', '#F59E0B', '#EF4444', '#8B5CF6',
    '#06B6D4', '#84CC16', '#F97316', '#EC4899', '#6366F1',
  ]

  chartInstance = new Chart(assetChartRef.value, {
    type: 'doughnut',
    data: {
      labels: accounts.map(a => a.accountName),
      datasets: [{
        data: accounts.map(a => Number(a.usdValue)),
        backgroundColor: colors.slice(0, accounts.length),
        borderWidth: 2,
        borderColor: '#fff',
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { position: 'bottom', labels: { font: { size: 11 }, padding: 8 } },
        tooltip: {
          callbacks: {
            label: ctx => ` ${ctx.label}: ${formatUSD(ctx.parsed)}`
          }
        }
      }
    }
  })
}

watch(effectiveAccounts, async () => {
  await nextTick()
  renderChart()
})

// ── Scenarios ──────────────────────────────────────────────

function computeScenarioMonths(multiplier) {
  const burn = effectiveMonthlyBurn.value * multiplier
  if (burn <= 0 || !effectiveLiquidTotal.value) return null
  return Math.floor(effectiveLiquidTotal.value / burn)
}

function computeDepletion(months) {
  if (months == null) return '—'
  const d = new Date()
  d.setMonth(d.getMonth() + months)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
}

const scenarios = computed(() => {
  if (!data.value) return []
  const baseBurn = effectiveMonthlyBurn.value
  const optMonths = computeScenarioMonths(optimisticMultiplier.value)
  const pesMonths = computeScenarioMonths(pessimisticMultiplier.value)
  return [
    {
      label: `乐观（×${optimisticMultiplier.value}）`,
      burn: baseBurn * optimisticMultiplier.value,
      months: optMonths ?? '—',
      depletion: computeDepletion(optMonths),
      textClass: 'text-green-600',
      rowClass: 'bg-green-50/30',
    },
    {
      label: '基准（×1.0）',
      burn: baseBurn,
      months: effectiveRunwayMonths.value ?? '—',
      depletion: effectiveDepletionDate.value,
      textClass: 'text-blue-600',
      rowClass: '',
    },
    {
      label: `悲观（×${pessimisticMultiplier.value}）`,
      burn: baseBurn * pessimisticMultiplier.value,
      months: pesMonths ?? '—',
      depletion: computeDepletion(pesMonths),
      textClass: 'text-red-600',
      rowClass: 'bg-red-50/30',
    },
  ]
})

// ── Helpers ────────────────────────────────────────────────

const sortedAccounts = computed(() => {
  if (!data.value?.accountBreakdown) return []
  return [...data.value.accountBreakdown].sort((a, b) => b.usdValue - a.usdValue)
})

const sortedExpenses = computed(() => {
  if (!data.value?.expenseBreakdown) return []
  return Object.entries(data.value.expenseBreakdown).sort(([, a], [, b]) => b - a)
})

function formatUSD(amount) {
  if (amount == null) return '—'
  return '$' + new Intl.NumberFormat('en-US', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(Math.abs(Number(amount)))
}

function pct(part, total) {
  if (!total || Number(total) === 0) return '0'
  return ((Number(part) / Number(total)) * 100).toFixed(1)
}

function runwayColor(months) {
  if (months == null) return 'text-gray-400'
  if (months >= 24) return 'text-green-600'
  if (months >= 12) return 'text-yellow-600'
  return 'text-red-600'
}

function assetTypeLabel(code) {
  return assetTypeNames[code] || code
}

function expenseCategoryLabel(code) {
  return expenseCategoryNames[code] || code
}

onMounted(fetchData)
</script>
