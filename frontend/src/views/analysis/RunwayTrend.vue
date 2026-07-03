<template>
  <div class="p-6 md:p-8 space-y-5 text-foreground">
    <!-- Header + range -->
    <div class="flex flex-wrap items-end justify-between gap-4">
      <div class="flex flex-col gap-1.5">
        <div class="flex items-center gap-2.5">
          <div class="w-8 h-8 rounded-lg bg-primary text-primary-foreground flex items-center justify-center">
            <TrendingUp class="w-5 h-5" />
          </div>
          <h1 class="text-2xl font-extrabold tracking-tight">财务规划 · 资金跑道趋势</h1>
        </div>
        <p class="ml-11 text-sm text-muted-foreground">把已保存的跑道报告汇总成趋势线 · 追踪现金流健康度</p>
      </div>
      <div class="flex gap-1.5 p-1 bg-card border border-border rounded-xl">
        <button
          v-for="opt in rangeOptions"
          :key="opt.value"
          @click="range = opt.value"
          class="px-4 py-1.5 rounded-lg text-sm font-semibold whitespace-nowrap transition-colors"
          :class="range === opt.value ? 'bg-primary text-primary-foreground' : 'text-muted-foreground hover:bg-muted'"
        >{{ opt.label }}</button>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="text-center py-16 text-muted-foreground">加载中…</div>

    <!-- Empty state -->
    <div v-else-if="points.length === 0" class="bg-card border border-border rounded-2xl text-center py-14 px-5">
      <h2 class="text-lg font-bold mb-2">还没有已保存的跑道报告</h2>
      <p class="text-sm text-muted-foreground">
        趋势需要至少一份已保存的报告。
        <RouterLink to="/analysis/runway" class="text-primary font-semibold">前往 资金跑道分析 →</RouterLink>
        保存一份后再回来查看趋势。
      </p>
    </div>

    <template v-else>
      <!-- KPI cards -->
      <div class="grid gap-4 grid-cols-[repeat(auto-fit,minmax(220px,1fr))]">
        <div class="bg-card border border-border rounded-2xl px-5 py-4 flex flex-col gap-2">
          <span class="text-sm text-muted-foreground font-medium">当前现金余额</span>
          <span class="text-[28px] font-extrabold tracking-tight tabular-nums">{{ formatUSD(kpis.cash.value) }}</span>
          <span class="text-sm font-semibold tabular-nums" :class="deltaClass(kpis.cash.delta)">{{ deltaText(kpis.cash.delta) }}</span>
        </div>
        <div class="bg-card border border-border rounded-2xl px-5 py-4 flex flex-col gap-2">
          <span class="text-sm text-muted-foreground font-medium">剩余跑道</span>
          <span class="text-[28px] font-extrabold tracking-tight tabular-nums">{{ kpis.runway.value != null ? kpis.runway.value + ' 个月' : '—' }}</span>
          <span class="text-sm font-semibold tabular-nums" :class="deltaClass(kpis.runway.delta)">{{ deltaText(kpis.runway.delta) }}</span>
        </div>
        <div class="bg-card border border-border rounded-2xl px-5 py-4 flex flex-col gap-2">
          <span class="text-sm text-muted-foreground font-medium">月度净烧钱率（报告值）</span>
          <span class="text-[28px] font-extrabold tracking-tight tabular-nums">{{ formatUSD(kpis.burn.value) }}</span>
          <span class="text-sm font-semibold tabular-nums" :class="deltaClass(kpis.burn.delta)">{{ deltaText(kpis.burn.delta) }}</span>
        </div>
        <div class="bg-card border border-border rounded-2xl px-5 py-4 flex flex-col gap-2">
          <span class="text-sm text-muted-foreground font-medium">预计现金耗尽</span>
          <span class="text-[28px] font-extrabold tracking-tight tabular-nums">{{ formatDepletion(kpis.depletion.value) }}</span>
          <span class="text-sm font-semibold text-muted-foreground">按报告口径</span>
        </div>
      </div>

      <!-- Trend chart -->
      <div class="bg-card border border-border rounded-2xl px-6 py-5">
        <div class="flex flex-wrap items-center justify-between gap-3 mb-3">
          <div class="flex flex-col gap-0.5">
            <h2 class="text-[17px] font-bold">{{ activeMetricMeta.title }}</h2>
            <span class="text-sm text-muted-foreground">{{ activeMetricMeta.sub }} · 近 {{ viewPoints.length }} 份报告</span>
          </div>
          <div class="flex flex-wrap gap-2">
            <button
              v-for="m in metrics"
              :key="m.key"
              @click="metric = m.key"
              class="px-3.5 py-2 rounded-lg text-sm font-semibold whitespace-nowrap border transition-colors"
              :class="metric === m.key ? 'bg-primary text-primary-foreground border-primary' : 'bg-card text-muted-foreground border-border hover:bg-muted'"
            >{{ m.tab }}</button>
          </div>
        </div>
        <div class="h-[340px]">
          <Line :data="chartData" :options="chartOptions" />
        </div>
      </div>

      <!-- Category table -->
      <div class="bg-card border border-border rounded-2xl px-6 py-5">
        <div class="flex flex-wrap items-baseline justify-between gap-2 mb-4">
          <h2 class="text-[17px] font-bold">分类支出明细</h2>
          <span class="text-sm text-muted-foreground">最新报告 · 合计 {{ formatUSD(categoryTotal) }}</span>
        </div>
        <div class="grid grid-cols-[1.4fr_1fr_2fr_0.9fr] gap-3 px-1 pb-2.5 border-b border-border text-xs font-semibold text-muted-foreground uppercase tracking-wide">
          <span>类别</span>
          <span class="text-right">金额</span>
          <span>占比</span>
          <span class="text-right">较上次报告</span>
        </div>
        <div v-for="c in categoryRows" :key="c.code"
             class="grid grid-cols-[1.4fr_1fr_2fr_0.9fr] gap-3 items-center px-1 py-3 border-b border-border/60">
          <div class="flex items-center gap-2.5">
            <span class="w-2.5 h-2.5 rounded-[3px]" :style="{ backgroundColor: c.color }"></span>
            <span class="text-sm font-semibold">{{ c.name }}</span>
          </div>
          <span class="text-sm font-semibold text-right tabular-nums">{{ formatUSD(c.amount) }}</span>
          <div class="flex items-center gap-2.5">
            <div class="flex-1 h-1.5 bg-muted rounded-full overflow-hidden">
              <div class="h-full rounded-full" :style="{ width: c.barW + '%', backgroundColor: c.color }"></div>
            </div>
            <span class="text-xs text-muted-foreground w-9 text-right tabular-nums">{{ c.share }}%</span>
          </div>
          <span class="text-sm font-semibold text-right tabular-nums" :class="categoryDeltaClass(c.delta)">{{ c.delta }}</span>
        </div>
      </div>
    </template>

    <div v-if="error" class="text-sm text-destructive">{{ error }}</div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Line } from 'vue-chartjs'
import {
  Chart as ChartJS, Title, Tooltip, Legend,
  LineElement, PointElement, LinearScale, CategoryScale, Filler,
} from 'chart.js'
import { TrendingUp } from 'lucide-vue-next'
import { runwayAPI } from '../../api/runway'
import { useAuthStore } from '../../stores/auth'
import { truncateToRecent, computeKpis, computeCategoryRows, formatUSD } from '../../utils/runwayTrend'

ChartJS.register(Title, Tooltip, Legend, LineElement, PointElement, LinearScale, CategoryScale, Filler)

const BRAND = 'hsl(142 76% 36%)'

const authStore = useAuthStore()
const loading = ref(true)
const error = ref('')
const points = ref([])
const categories = ref([])
const previousCategories = ref([])
const range = ref(12)
const metric = ref('runway')

const rangeOptions = [
  { value: 6, label: '近 6 份' },
  { value: 12, label: '近 12 份' },
  { value: 'all', label: '全部' },
]

const metrics = [
  { key: 'runway', tab: '剩余跑道月数', title: '剩余跑道月数趋势', sub: '当前现金 ÷ 报告口径净烧钱率', field: 'runwayMonths' },
  { key: 'burn', tab: '月度净烧钱', title: '月度净烧钱趋势', sub: '每份报告的月度净流出', field: 'monthlyBurn' },
  { key: 'cash', tab: '现金余额', title: '现金余额趋势', sub: '每份报告的可用现金余额', field: 'liquidTotal' },
]

const activeMetricMeta = computed(() => metrics.find((m) => m.key === metric.value))
const viewPoints = computed(() => truncateToRecent(points.value, range.value))
const kpis = computed(() => computeKpis(points.value) || {
  cash: { value: null, delta: null }, runway: { value: null, delta: null },
  burn: { value: null, delta: null }, depletion: { value: null },
})
const categoryRows = computed(() => computeCategoryRows(categories.value, previousCategories.value))
const categoryTotal = computed(() => categoryRows.value.reduce((s, c) => s + c.amount, 0))

const chartData = computed(() => {
  const field = activeMetricMeta.value.field
  const rows = viewPoints.value
  return {
    labels: rows.map((p) => formatLabel(p.savedAt)),
    datasets: [{
      label: activeMetricMeta.value.tab,
      data: rows.map((p) => Number(p[field])),
      borderColor: BRAND,
      backgroundColor: 'rgba(22, 163, 74, 0.15)',
      fill: true,
      tension: 0.35,
      borderWidth: 2.5,
      pointBackgroundColor: '#fff',
      pointBorderColor: BRAND,
      pointRadius: 3,
    }],
  }
})

const chartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: false },
    tooltip: {
      callbacks: {
        title: (items) => items[0]?.label ?? '',
        label: (item) => {
          if (metric.value === 'runway') return `${item.formattedValue} 个月`
          return formatUSD(item.raw)
        },
      },
    },
  },
  scales: {
    y: { ticks: { callback: (v) => (metric.value === 'runway' ? v : '$' + Math.round(v / 1000) + 'k') } },
    x: { grid: { display: false } },
  },
}

function formatLabel(iso) {
  const d = new Date(iso)
  return `${String(d.getFullYear()).slice(2)}/${String(d.getMonth() + 1).padStart(2, '0')}/${String(d.getDate()).padStart(2, '0')}`
}
function formatDepletion(period) {
  if (!period) return '—'
  const [y, m] = String(period).split('-')
  return m ? `${y}年${parseInt(m, 10)}月` : period
}
function deltaText(delta) {
  return delta ? delta.text : '—'
}
function deltaClass(delta) {
  if (!delta) return 'text-muted-foreground'
  return delta.good ? 'text-primary' : 'text-destructive'
}
function categoryDeltaClass(delta) {
  if (delta === '新增') return 'text-primary'
  if (delta === '持平') return 'text-muted-foreground'
  return delta.startsWith('▲') ? 'text-destructive' : 'text-primary'
}

async function fetchTrend() {
  loading.value = true
  error.value = ''
  try {
    const familyId = authStore.familyId
    if (!familyId) { loading.value = false; return }
    const res = await runwayAPI.getRunwayTrend(familyId)
    if (res.success) {
      points.value = res.data.points || []
      categories.value = res.data.categories || []
      previousCategories.value = res.data.previousCategories || []
    } else {
      error.value = res.error || '加载失败'
    }
  } catch (e) {
    error.value = e.response?.data?.error || e.message || '请求失败'
  } finally {
    loading.value = false
  }
}

onMounted(fetchTrend)
</script>
