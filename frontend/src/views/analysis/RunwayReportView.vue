<template>
  <div class="p-3 md:p-6 space-y-4 md:space-y-6">
    <!-- Header -->
    <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
      <div>
        <router-link to="/analysis/runway-reports" class="text-sm text-blue-600 hover:underline">← 历史报告</router-link>
        <h1 class="text-xl md:text-2xl font-bold text-gray-900 mt-1">{{ report?.reportName || '报告详情' }}</h1>
        <p v-if="report" class="text-xs text-gray-500 mt-0.5">保存于 {{ formatDate(report.savedAt) }}</p>
      </div>
      <button
        v-if="snapshot"
        @click="exportPDF"
        :disabled="pdfExporting"
        class="px-3 py-2 bg-gray-700 text-white text-sm rounded-lg hover:bg-gray-800 disabled:opacity-50 flex items-center gap-2 self-start"
      >
        {{ pdfExporting ? '生成中...' : '导出 PDF' }}
      </button>
    </div>

    <!-- Error -->
    <div v-if="error" class="bg-red-50 border border-red-200 rounded-lg p-4 text-sm text-red-700">
      {{ error }}
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flex justify-center py-12">
      <svg class="w-8 h-8 animate-spin text-blue-600" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"/>
      </svg>
    </div>

    <div v-if="snapshot" ref="pdfContent">
      <!-- PDF header (family info) -->
      <div v-if="familyName" class="bg-white rounded-xl border border-gray-200 px-4 py-3 flex items-center justify-between">
        <span class="text-sm font-semibold text-gray-700">{{ familyName }}</span>
        <span class="text-xs text-gray-400">{{ report?.reportName }} · 保存于 {{ formatDate(report?.savedAt) }}</span>
      </div>

      <!-- Summary Cards -->
      <div class="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <div class="bg-white rounded-xl border border-gray-200 p-4">
          <p class="text-xs text-gray-500 mb-1">流动资产总额</p>
          <p class="text-xl font-bold text-blue-600">{{ formatUSD(snapshot.snapshot.liquidTotal) }}</p>
        </div>
        <div class="bg-white rounded-xl border border-gray-200 p-4">
          <p class="text-xs text-gray-500 mb-1">月均支出</p>
          <p class="text-xl font-bold text-orange-600">{{ formatUSD(snapshot.snapshot.monthlyBurn) }}</p>
        </div>
        <div class="bg-white rounded-xl border border-gray-200 p-4">
          <p class="text-xs text-gray-500 mb-1">Runway（基准）</p>
          <p class="text-xl font-bold" :class="runwayColor(snapshot.snapshot.runwayMonths)">
            {{ snapshot.snapshot.runwayMonths != null ? snapshot.snapshot.runwayMonths + ' 个月' : '—' }}
          </p>
          <p class="text-xs text-gray-400 mt-1">约 {{ snapshot.snapshot.runwayMonths ? (snapshot.snapshot.runwayMonths / 12).toFixed(1) : '?' }} 年</p>
        </div>
        <div class="bg-white rounded-xl border border-gray-200 p-4">
          <p class="text-xs text-gray-500 mb-1">预计耗尽日期</p>
          <p class="text-xl font-bold text-gray-700">{{ snapshot.snapshot.depletionDate || '—' }}</p>
        </div>
      </div>

      <!-- Scenario Comparison -->
      <div v-if="scenarios.length" class="bg-white rounded-xl border border-gray-200 overflow-hidden">
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

      <!-- Account Breakdown -->
      <div v-if="accountBreakdown.length" class="bg-white rounded-xl border border-gray-200 overflow-hidden">
        <div class="px-4 py-3 border-b border-gray-100">
          <h2 class="text-sm font-semibold text-gray-700">账户明细</h2>
        </div>
        <table class="w-full text-sm">
          <thead class="bg-gray-50">
            <tr>
              <th class="px-4 py-2 text-left text-xs font-medium text-gray-500">账户</th>
              <th class="px-4 py-2 text-left text-xs font-medium text-gray-500">类型</th>
              <th class="px-4 py-2 text-right text-xs font-medium text-gray-500">金额</th>
              <th class="px-4 py-2 text-center text-xs font-medium text-gray-500">状态</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-100">
            <tr
              v-for="acc in accountBreakdown"
              :key="acc.id"
              :class="excludedIds.has(acc.id) ? 'opacity-40 bg-gray-50' : ''"
            >
              <td class="px-4 py-2 font-medium text-gray-800">{{ acc.accountName }}</td>
              <td class="px-4 py-2 text-gray-500">{{ assetTypeLabel(acc.accountType) }}</td>
              <td class="px-4 py-2 text-right text-gray-700">{{ formatUSD(acc.usdValue) }}</td>
              <td class="px-4 py-2 text-center">
                <span v-if="excludedIds.has(acc.id)" class="text-xs text-gray-400">已排除</span>
                <span v-else class="text-xs text-green-600">已纳入</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Expense Breakdown -->
      <div v-if="expenseRows.length" class="bg-white rounded-xl border border-gray-200 overflow-hidden">
        <div class="px-4 py-3 border-b border-gray-100">
          <h2 class="text-sm font-semibold text-gray-700">月均支出结构</h2>
        </div>
        <table class="w-full text-sm">
          <thead class="bg-gray-50">
            <tr>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">支出大类</th>
              <th class="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">月均金额</th>
              <th class="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">调整</th>
              <th class="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">调整后</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-100">
            <tr v-for="row in expenseRows" :key="row.code">
              <td class="px-4 py-3 font-medium text-gray-700">{{ expenseCategoryLabel(row.code) }}</td>
              <td class="px-4 py-3 text-right text-red-600">{{ formatUSD(row.base) }}</td>
              <td class="px-4 py-3 text-right" :class="row.adj > 0 ? 'text-red-500' : row.adj < 0 ? 'text-green-600' : 'text-gray-400'">
                {{ row.adj !== 0 ? (row.adj > 0 ? '+' : '') + formatUSD(row.adj) : '—' }}
              </td>
              <td class="px-4 py-3 text-right font-medium text-gray-800">{{ formatUSD(row.effective) }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Settings footer -->
      <div class="text-xs text-gray-400 text-right">
        设置：回溯 {{ snapshot.settings.lookbackMonths }} 个月 | 乐观 ×{{ snapshot.settings.optimisticMultiplier }} | 悲观 ×{{ snapshot.settings.pessimisticMultiplier }}
      </div>
    </div><!-- end pdfContent -->
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import { runwayAPI } from '../../api/runway'
import { familyAPI } from '../../api/family'
import html2canvas from 'html2canvas'
import { jsPDF } from 'jspdf'

const route = useRoute()
const authStore = useAuthStore()

const report = ref(null)
const snapshot = ref(null)
const loading = ref(false)
const pdfExporting = ref(false)
const error = ref(null)
const pdfContent = ref(null)
const familyName = ref(null)

const excludedIds = computed(() => new Set(snapshot.value?.excludedAccountIds ?? []))

const accountBreakdown = computed(() =>
  [...(snapshot.value?.snapshot?.accountBreakdown ?? [])].sort((a, b) => b.usdValue - a.usdValue)
)

const expenseRows = computed(() => {
  if (!snapshot.value?.snapshot?.expenseBreakdown) return []
  const adj = snapshot.value.expenseAdjustments || {}
  return Object.entries(snapshot.value.snapshot.expenseBreakdown)
    .sort(([, a], [, b]) => b - a)
    .map(([code, base]) => ({
      code,
      base: Number(base),
      adj: Number(adj[code] || 0),
      effective: Number(base) + Number(adj[code] || 0),
    }))
})

const scenarios = computed(() => {
  if (!snapshot.value) return []
  const burn = Number(snapshot.value.snapshot.monthlyBurn)
  const liquid = Number(snapshot.value.snapshot.liquidTotal)
  const opt = snapshot.value.settings.optimisticMultiplier
  const pes = snapshot.value.settings.pessimisticMultiplier
  const calcMonths = (mult) => burn * mult > 0 ? Math.floor(liquid / (burn * mult)) : null
  const calcDepletion = (months) => {
    if (months == null) return '—'
    const d = new Date()
    d.setMonth(d.getMonth() + months)
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
  }
  const optMonths = calcMonths(opt)
  const pesMonths = calcMonths(pes)
  return [
    { label: `乐观（×${opt}）`, burn: burn * opt, months: optMonths ?? '—', depletion: calcDepletion(optMonths), textClass: 'text-green-600', rowClass: 'bg-green-50/30' },
    { label: '基准（×1.0）', burn, months: snapshot.value.snapshot.runwayMonths ?? '—', depletion: snapshot.value.snapshot.depletionDate, textClass: 'text-blue-600', rowClass: '' },
    { label: `悲观（×${pes}）`, burn: burn * pes, months: pesMonths ?? '—', depletion: calcDepletion(pesMonths), textClass: 'text-red-600', rowClass: 'bg-red-50/30' },
  ]
})

async function fetchReport() {
  const id = route.params.id
  const familyId = authStore.familyId
  if (!id || !familyId) return
  loading.value = true
  error.value = null
  try {
    const response = await runwayAPI.getRunwayReport(id, familyId)
    if (response.success) {
      report.value = response.data
      snapshot.value = JSON.parse(response.data.snapshotJson)
    } else {
      error.value = response.error || '加载失败'
    }
  } catch (e) {
    error.value = e.message || '请求失败'
  } finally {
    loading.value = false
  }
}

async function exportPDF() {
  if (!snapshot.value || !report.value || !pdfContent.value || pdfExporting.value) return
  pdfExporting.value = true
  try {
    const today = new Date().toISOString().slice(0, 10)
    const canvas = await html2canvas(pdfContent.value, {
      scale: 2,
      backgroundColor: '#f9fafb',
      useCORS: true,
      logging: false,
    })
    const doc = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' })
    const pageW = doc.internal.pageSize.getWidth()
    const pageH = doc.internal.pageSize.getHeight()
    const imgW = pageW
    const imgH = (canvas.height * imgW) / canvas.width
    let remaining = imgH
    let offsetY = 0
    doc.addImage(canvas.toDataURL('image/png'), 'PNG', 0, offsetY, imgW, imgH)
    remaining -= pageH
    while (remaining > 0) {
      offsetY -= pageH
      doc.addPage()
      doc.addImage(canvas.toDataURL('image/png'), 'PNG', 0, offsetY, imgW, imgH)
      remaining -= pageH
    }
    doc.save(`runway-report-${today}.pdf`)
  } finally {
    pdfExporting.value = false
  }
}

const assetTypeNames = { CASH: '现金', STOCKS: '股票账户', CRYPTOCURRENCY: '加密货币', PRECIOUS_METALS: '贵金属', RETIREMENT_FUND: '退休基金', INSURANCE: '保险', REAL_ESTATE: '房产', OTHER: '其他' }
const expenseCategoryNames = { CHILDREN: '子女教育', CLOTHING: '服装', FOOD: '餐饮', HOUSING: '住房', TRANSPORTATION: '交通', INSURANCE: '保险', SOCIAL: '社交娱乐', ENTERTAINMENT: '娱乐', BUSINESS: '商业', OTHER: '其他' }

function assetTypeLabel(code) { return assetTypeNames[code] || code }
function expenseCategoryLabel(code) { return expenseCategoryNames[code] || code }
function formatUSD(amount) {
  if (amount == null) return '—'
  return '$' + new Intl.NumberFormat('en-US', { maximumFractionDigits: 0 }).format(Math.abs(Number(amount)))
}
function runwayColor(months) {
  if (months == null) return 'text-gray-400'
  if (months >= 24) return 'text-green-600'
  if (months >= 12) return 'text-yellow-600'
  return 'text-red-600'
}
function formatDate(dt) {
  if (!dt) return '—'
  return new Date(dt).toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

onMounted(async () => {
  fetchReport()
  try {
    const res = await familyAPI.getDefault()
    if (res.success) familyName.value = res.data.familyName
  } catch {}
})
</script>
