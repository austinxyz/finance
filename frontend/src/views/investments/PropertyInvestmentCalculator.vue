<template>
  <div class="p-3 md:p-4 space-y-3">
    <!-- Header -->
    <div>
      <h1 class="text-xl font-bold text-gray-900">🏠 房产投资计算器</h1>
      <p class="text-xs text-gray-500 mt-0.5">暴力计算器 — 不提供情绪价值，只提供财务真相</p>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-4 items-start">

      <!-- ===== COL 1: INPUT PANEL ===== -->
      <div class="bg-white rounded-xl border border-gray-200 p-4">
        <h2 class="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-3">输入参数</h2>
        <div class="space-y-2">

          <div class="flex items-center justify-between gap-2">
            <span class="text-xs text-gray-600 shrink-0">房价 <span class="text-gray-400">湾区入门价</span></span>
            <div class="flex rounded border border-yellow-300 overflow-hidden">
              <span class="px-1.5 bg-yellow-100 border-r border-yellow-200 text-xs text-gray-400 flex items-center">$</span>
              <input v-model.number="purchasePrice" type="number" step="10000" min="0"
                class="w-28 px-1.5 py-1 text-xs bg-yellow-50 focus:outline-none"
                data-testid="input-purchase-price" />
            </div>
          </div>

          <div class="flex items-center justify-between gap-2">
            <span class="text-xs text-gray-600 shrink-0">首付比例 <span class="text-gray-400">通常 25%</span></span>
            <div class="flex rounded border border-yellow-300 overflow-hidden">
              <input v-model.number="downPaymentPctInput" type="number" step="1" min="0" max="100"
                class="w-16 px-1.5 py-1 text-xs bg-yellow-50 focus:outline-none" />
              <span class="px-1.5 bg-yellow-100 border-l border-yellow-200 text-xs text-gray-400 flex items-center">%</span>
            </div>
          </div>

          <div class="flex items-center justify-between gap-2">
            <span class="text-xs text-gray-600">过户费率</span>
            <div class="flex rounded border border-yellow-300 overflow-hidden">
              <input v-model.number="closingCostPctInput" type="number" step="0.1" min="0" max="10"
                class="w-16 px-1.5 py-1 text-xs bg-yellow-50 focus:outline-none" />
              <span class="px-1.5 bg-yellow-100 border-l border-yellow-200 text-xs text-gray-400 flex items-center">%</span>
            </div>
          </div>

          <div class="flex items-center justify-between gap-2">
            <span class="text-xs text-gray-600 shrink-0">贷款利率 <span class="text-gray-400">投资房</span></span>
            <div class="flex rounded border border-yellow-300 overflow-hidden">
              <input v-model.number="annualRateInput" type="number" step="0.1" min="0"
                class="w-16 px-1.5 py-1 text-xs bg-yellow-50 focus:outline-none" />
              <span class="px-1.5 bg-yellow-100 border-l border-yellow-200 text-xs text-gray-400 flex items-center">%</span>
            </div>
          </div>

          <div class="border-t border-gray-100 my-1"></div>

          <div class="flex items-center justify-between gap-2">
            <span class="text-xs text-gray-600 shrink-0">月租金 <span class="text-gray-400">去 Zillow 查</span></span>
            <div class="flex rounded border border-yellow-300 overflow-hidden">
              <span class="px-1.5 bg-yellow-100 border-r border-yellow-200 text-xs text-gray-400 flex items-center">$</span>
              <input v-model.number="monthlyRent" type="number" step="100" min="0"
                class="w-24 px-1.5 py-1 text-xs bg-yellow-50 focus:outline-none"
                data-testid="input-monthly-rent" />
            </div>
          </div>

          <div class="flex items-center justify-between gap-2">
            <span class="text-xs text-gray-600 shrink-0">房产税率 <span class="text-gray-400">加州 ~1.25%</span></span>
            <div class="flex rounded border border-yellow-300 overflow-hidden">
              <input v-model.number="propertyTaxRateInput" type="number" step="0.05" min="0"
                class="w-16 px-1.5 py-1 text-xs bg-yellow-50 focus:outline-none" />
              <span class="px-1.5 bg-yellow-100 border-l border-yellow-200 text-xs text-gray-400 flex items-center">%</span>
            </div>
          </div>

          <div class="flex items-center justify-between gap-2">
            <span class="text-xs text-gray-600">保险/年</span>
            <div class="flex rounded border border-yellow-300 overflow-hidden">
              <span class="px-1.5 bg-yellow-100 border-r border-yellow-200 text-xs text-gray-400 flex items-center">$</span>
              <input v-model.number="annualInsurance" type="number" step="100" min="0"
                class="w-24 px-1.5 py-1 text-xs bg-yellow-50 focus:outline-none" />
            </div>
          </div>

          <div class="flex items-center justify-between gap-2">
            <span class="text-xs text-gray-600 shrink-0">HOA/月 <span class="text-gray-400">独栋写 0</span></span>
            <div class="flex rounded border border-yellow-300 overflow-hidden">
              <span class="px-1.5 bg-yellow-100 border-r border-yellow-200 text-xs text-gray-400 flex items-center">$</span>
              <input v-model.number="monthlyHOA" type="number" step="50" min="0"
                class="w-24 px-1.5 py-1 text-xs bg-yellow-50 focus:outline-none" />
            </div>
          </div>

          <div class="flex items-center justify-between gap-2">
            <span class="text-xs text-gray-600 shrink-0">空置率 <span class="text-gray-400">至少 5%</span></span>
            <div class="flex rounded border border-yellow-300 overflow-hidden">
              <input v-model.number="vacancyRateInput" type="number" step="1" min="0" max="100"
                class="w-16 px-1.5 py-1 text-xs bg-yellow-50 focus:outline-none" />
              <span class="px-1.5 bg-yellow-100 border-l border-yellow-200 text-xs text-gray-400 flex items-center">%</span>
            </div>
          </div>

          <div class="border-t border-gray-100 my-1"></div>

          <div class="flex items-center justify-between gap-2">
            <span class="text-xs text-gray-600 shrink-0">边际税率 <span class="text-gray-400">37%+10%</span></span>
            <div class="flex rounded border border-yellow-300 overflow-hidden">
              <input v-model.number="marginalTaxRateInput" type="number" step="1" min="0" max="100"
                class="w-16 px-1.5 py-1 text-xs bg-yellow-50 focus:outline-none" />
              <span class="px-1.5 bg-yellow-100 border-l border-yellow-200 text-xs text-gray-400 flex items-center">%</span>
            </div>
          </div>

          <div class="flex items-center justify-between gap-2">
            <span class="text-xs text-gray-600 shrink-0">土地占比 <span class="text-gray-400">湾区约 60%</span></span>
            <div class="flex rounded border border-yellow-300 overflow-hidden">
              <input v-model.number="landValuePctInput" type="number" step="5" min="0" max="100"
                class="w-16 px-1.5 py-1 text-xs bg-yellow-50 focus:outline-none" />
              <span class="px-1.5 bg-yellow-100 border-l border-yellow-200 text-xs text-gray-400 flex items-center">%</span>
            </div>
          </div>

          <div class="flex items-center justify-between gap-2">
            <span class="text-xs text-gray-600 shrink-0">年涨幅 <span class="text-gray-400">历史约 4%</span></span>
            <div class="flex rounded border border-yellow-300 overflow-hidden">
              <input v-model.number="appreciationRateInput" type="number" step="0.5" min="0"
                class="w-16 px-1.5 py-1 text-xs bg-yellow-50 focus:outline-none" />
              <span class="px-1.5 bg-yellow-100 border-l border-yellow-200 text-xs text-gray-400 flex items-center">%</span>
            </div>
          </div>

        </div>
      </div>

      <!-- ===== COL 2: CASH FLOW + CORE METRICS ===== -->
      <div class="space-y-4">

        <!-- Section 1: Monthly Cash Flow -->
        <div class="bg-white rounded-xl border border-gray-200 overflow-hidden">
          <div class="px-4 py-2.5 border-b border-gray-100 bg-gray-50">
            <h2 class="text-xs font-semibold text-gray-700">🩸 月度现金流</h2>
          </div>
          <table class="w-full text-xs">
            <tbody class="divide-y divide-gray-100">
              <tr>
                <td class="px-3 py-1.5 text-gray-600">收入（扣除空置）</td>
                <td class="px-3 py-1.5 text-right text-green-700 font-medium">{{ fmt(grossRentalIncome) }}</td>
              </tr>
              <tr class="bg-red-50/30">
                <td class="px-3 py-1.5 text-gray-500 pl-5">— 月供 P&I</td>
                <td class="px-3 py-1.5 text-right text-red-500">-{{ fmt(mortgagePayment) }}</td>
              </tr>
              <tr class="bg-red-50/30">
                <td class="px-3 py-1.5 text-gray-500 pl-5">— 房产税</td>
                <td class="px-3 py-1.5 text-right text-red-500">-{{ fmt(monthlyPropertyTax) }}</td>
              </tr>
              <tr class="bg-red-50/30">
                <td class="px-3 py-1.5 text-gray-500 pl-5">— 保险</td>
                <td class="px-3 py-1.5 text-right text-red-500">-{{ fmt(monthlyInsurance) }}</td>
              </tr>
              <tr class="bg-red-50/30">
                <td class="px-3 py-1.5 text-gray-500 pl-5">— HOA</td>
                <td class="px-3 py-1.5 text-right text-red-500">-{{ fmt(monthlyHOA) }}</td>
              </tr>
              <tr class="bg-red-50/30">
                <td class="px-3 py-1.5 text-gray-500 pl-5">— 维修（1%/年）</td>
                <td class="px-3 py-1.5 text-right text-red-500">-{{ fmt(monthlyMaintenance) }}</td>
              </tr>
              <tr class="border-t-2 border-gray-200">
                <td class="px-3 py-2 font-semibold text-gray-800">⚠️ 月净现金流</td>
                <td class="px-3 py-2 text-right text-xl font-bold" :class="trueMonthlyFlow >= 0 ? 'text-green-600' : 'text-red-600'" data-testid="true-monthly-flow">
                  {{ trueMonthlyFlow >= 0 ? '' : '-' }}{{ fmt(Math.abs(trueMonthlyFlow)) }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Section 2: Core Metrics -->
        <div class="bg-white rounded-xl border border-gray-200 overflow-hidden">
          <div class="px-4 py-2.5 border-b border-gray-100 bg-gray-50">
            <h2 class="text-xs font-semibold text-gray-700">核心指标</h2>
          </div>
          <div class="grid grid-cols-2 divide-x divide-y divide-gray-100">
            <div class="px-3 py-2.5">
              <p class="text-xs text-gray-400">总投入</p>
              <p class="text-base font-bold text-gray-800 mt-0.5" data-testid="cash-invested">{{ fmt(cashInvested) }}</p>
              <p class="text-xs text-gray-400">首付 + 过户费</p>
            </div>
            <div class="px-3 py-2.5">
              <p class="text-xs text-gray-400">年现金流</p>
              <p class="text-base font-bold mt-0.5" :class="annualCashFlow >= 0 ? 'text-green-600' : 'text-red-600'">
                {{ annualCashFlow >= 0 ? '' : '-' }}{{ fmt(Math.abs(annualCashFlow)) }}
              </p>
              <p class="text-xs text-gray-400">每年贴 / 收</p>
            </div>
            <div class="px-3 py-2.5">
              <p class="text-xs text-gray-400">首年本金偿还</p>
              <p class="text-base font-bold text-blue-600 mt-0.5">{{ fmt(annualPrincipalPaydown) }}</p>
              <p class="text-xs text-gray-400">租客帮你还的</p>
            </div>
            <div class="px-3 py-2.5">
              <p class="text-xs text-gray-400">CoC 现金回报率</p>
              <p class="text-base font-bold mt-0.5" :class="cocReturnColor" data-testid="coc-return">{{ fmtPct(cocReturn) }}</p>
              <p class="text-xs text-gray-400">最扎心的数字</p>
            </div>
          </div>
        </div>

      </div>

      <!-- ===== COL 3: TAX-ADJUSTED + APPRECIATION ===== -->
      <div class="space-y-4">

        <!-- Section 3: Tax-Adjusted -->
        <div class="bg-white rounded-xl border border-gray-200 overflow-hidden">
          <div class="px-4 py-2.5 border-b border-gray-100 bg-gray-50">
            <h2 class="text-xs font-semibold text-gray-700">🛡️ 税务调整后回报</h2>
          </div>
          <table class="w-full text-xs">
            <tbody class="divide-y divide-gray-100">
              <tr>
                <td class="px-3 py-1.5 text-gray-600">NOI 净营运收入</td>
                <td class="px-3 py-1.5 text-right text-gray-700">{{ fmt(noi) }}/yr</td>
              </tr>
              <tr>
                <td class="px-3 py-1.5 text-gray-600">首年利息</td>
                <td class="px-3 py-1.5 text-right text-red-600">-{{ fmt(firstYearInterest) }}</td>
              </tr>
              <tr>
                <td class="px-3 py-1.5 text-gray-600">年折旧 <span class="text-gray-400">建筑÷27.5年</span></td>
                <td class="px-3 py-1.5 text-right text-red-600">-{{ fmt(annualDepreciation) }}</td>
              </tr>
              <tr :class="paperLoss < 0 ? 'bg-orange-50/40' : ''">
                <td class="px-3 py-1.5 font-medium text-gray-700">账面亏损</td>
                <td class="px-3 py-1.5 text-right font-medium" :class="paperLoss < 0 ? 'text-orange-600' : 'text-green-600'">
                  {{ paperLoss < 0 ? '-' : '' }}{{ fmt(Math.abs(paperLoss)) }}
                </td>
              </tr>
              <tr class="bg-blue-50/30">
                <td class="px-3 py-1.5 font-medium text-gray-700">抵税存钱罐</td>
                <td class="px-3 py-1.5 text-right font-bold text-blue-600">{{ fmt(taxSavingsBank) }}</td>
              </tr>
            </tbody>
          </table>
          <div v-if="paperLoss < 0" class="px-3 py-2 bg-amber-50 border-t border-amber-100 text-xs text-amber-700 leading-snug">
            ⚠️ 悬挂亏损：非 REP/STR 无法直接抵扣 W2，卖房时一次性释放。
          </div>
          <div class="px-3 py-2.5 border-t border-gray-100">
            <div class="flex justify-between items-center">
              <span class="text-xs font-semibold text-gray-700">有效回报率</span>
              <span class="text-xl font-bold" :class="effectiveYield >= 0.05 ? 'text-green-600' : 'text-red-600'" data-testid="effective-yield">
                {{ fmtPct(effectiveYield) }}
              </span>
            </div>
            <p class="text-xs text-gray-400 mt-0.5">含抵税 · 对标 5% 无风险利率</p>
          </div>
        </div>

        <!-- Section 4: Appreciation & Final Verdict -->
        <div class="bg-white rounded-xl border border-gray-200 overflow-hidden">
          <div class="px-4 py-2.5 border-b border-gray-100 bg-gray-50">
            <h2 class="text-xs font-semibold text-gray-700">⚖️ 增值回报与终极审判</h2>
          </div>
          <div class="p-3 space-y-2">
            <div class="flex justify-between text-xs">
              <span class="text-gray-600">年增值</span>
              <span class="font-medium text-green-700">{{ fmt(appreciationGain) }}/yr</span>
            </div>
            <div class="flex justify-between text-xs">
              <span class="text-gray-600">增值回报率</span>
              <span class="font-medium text-green-700">{{ fmtPct(appreciationROI) }}</span>
            </div>
            <div class="border-t border-gray-200 pt-3">
              <div class="flex justify-between items-center">
                <div>
                  <p class="text-sm font-bold text-gray-800">🏁 TOTAL WEALTH ROI</p>
                  <p class="text-xs text-gray-400 mt-0.5">现金流 + 本金 + 抵税 + 增值</p>
                </div>
                <div class="text-right">
                  <p class="text-3xl font-bold" :class="totalROIColor" data-testid="total-roi">{{ fmtPct(totalWealthROI) }}</p>
                  <p class="text-xs mt-0.5" :class="totalROIColor">{{ totalROILabel }}</p>
                </div>
              </div>
            </div>
            <div class="grid grid-cols-3 gap-1.5 text-xs text-center">
              <div class="bg-red-50 rounded-lg p-1.5">
                <p class="font-semibold text-red-600">&lt; 8%</p>
                <p class="text-gray-500">不如 VOO</p>
              </div>
              <div class="bg-yellow-50 rounded-lg p-1.5">
                <p class="font-semibold text-yellow-600">8–14%</p>
                <p class="text-gray-500">接近大盘</p>
              </div>
              <div class="bg-green-50 rounded-lg p-1.5">
                <p class="font-semibold text-green-600">≥ 15%</p>
                <p class="text-gray-500">超越大盘</p>
              </div>
            </div>
          </div>
        </div>

      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { pmt, cumprinc, cumipmt } from '../../utils/financialFormulas'

// ===== INPUTS (percentage fields stored as 0-100 for display, converted in computed) =====
const purchasePrice = ref(1500000)
const downPaymentPctInput = ref(25)       // display: 25  → value: 0.25
const closingCostPctInput = ref(2)        // display: 2   → value: 0.02
const annualRateInput = ref(6.5)          // display: 6.5 → value: 0.065
const monthlyRent = ref(5000)
const propertyTaxRateInput = ref(1.25)   // display: 1.25 → value: 0.0125
const annualInsurance = ref(1200)
const monthlyHOA = ref(400)
const vacancyRateInput = ref(5)          // display: 5   → value: 0.05
const marginalTaxRateInput = ref(47)     // display: 47  → value: 0.47
const landValuePctInput = ref(60)        // display: 60  → value: 0.60
const appreciationRateInput = ref(4)     // display: 4   → value: 0.04

// Normalized rates
const downPaymentPct = computed(() => downPaymentPctInput.value / 100)
const closingCostPct = computed(() => closingCostPctInput.value / 100)
const annualRate = computed(() => annualRateInput.value / 100)
const propertyTaxRate = computed(() => propertyTaxRateInput.value / 100)
const vacancyRate = computed(() => vacancyRateInput.value / 100)
const marginalTaxRate = computed(() => marginalTaxRateInput.value / 100)
const landValuePct = computed(() => landValuePctInput.value / 100)
const appreciationRate = computed(() => appreciationRateInput.value / 100)

// ===== SECTION 1: MONTHLY CASH FLOW =====
const loanAmount = computed(() => purchasePrice.value * (1 - downPaymentPct.value))
const grossRentalIncome = computed(() => monthlyRent.value * (1 - vacancyRate.value))
const mortgagePayment = computed(() => pmt(annualRate.value, 30, loanAmount.value))
const monthlyPropertyTax = computed(() => (purchasePrice.value * propertyTaxRate.value) / 12)
const monthlyInsurance = computed(() => annualInsurance.value / 12)
const monthlyMaintenance = computed(() => (purchasePrice.value * 0.01) / 12)
const trueMonthlyFlow = computed(() =>
  grossRentalIncome.value
  - mortgagePayment.value
  - monthlyPropertyTax.value
  - monthlyInsurance.value
  - monthlyHOA.value
  - monthlyMaintenance.value
)

// ===== SECTION 2: CORE METRICS =====
const cashInvested = computed(() => purchasePrice.value * (downPaymentPct.value + closingCostPct.value))
const annualCashFlow = computed(() => trueMonthlyFlow.value * 12)
const annualPrincipalPaydown = computed(() => cumprinc(annualRate.value, 30, loanAmount.value, 1, 12))
const cocReturn = computed(() => cashInvested.value > 0 ? annualCashFlow.value / cashInvested.value : 0)
const cocReturnColor = computed(() => {
  if (cocReturn.value < 0) return 'text-red-600'
  if (cocReturn.value < 0.04) return 'text-yellow-600'
  return 'text-green-600'
})

// ===== SECTION 3: TAX-ADJUSTED =====
const annualDepreciation = computed(() => (purchasePrice.value * (1 - landValuePct.value)) / 27.5)
const firstYearInterest = computed(() => cumipmt(annualRate.value, 30, loanAmount.value, 1, 12))
const noi = computed(() =>
  (grossRentalIncome.value - monthlyPropertyTax.value - monthlyInsurance.value - monthlyHOA.value - monthlyMaintenance.value) * 12
)
const paperLoss = computed(() => noi.value - firstYearInterest.value - annualDepreciation.value)
const taxSavingsBank = computed(() => paperLoss.value < 0 ? Math.abs(paperLoss.value) * marginalTaxRate.value : 0)
const effectiveYield = computed(() =>
  cashInvested.value > 0
    ? (annualCashFlow.value + taxSavingsBank.value + annualPrincipalPaydown.value) / cashInvested.value
    : 0
)

// ===== SECTION 4: APPRECIATION & TOTAL ROI =====
const appreciationGain = computed(() => purchasePrice.value * appreciationRate.value)
const appreciationROI = computed(() => cashInvested.value > 0 ? appreciationGain.value / cashInvested.value : 0)
const totalWealthROI = computed(() => effectiveYield.value + appreciationROI.value)

const totalROIColor = computed(() => {
  if (totalWealthROI.value < 0.08) return 'text-red-600'
  if (totalWealthROI.value < 0.15) return 'text-yellow-600'
  return 'text-green-600'
})
const totalROILabel = computed(() => {
  if (totalWealthROI.value < 0.08) return '低于大盘均值'
  if (totalWealthROI.value < 0.15) return '接近大盘'
  return '目标：超越大盘'
})

// ===== FORMATTERS =====
function fmt(amount) {
  if (amount == null || isNaN(amount)) return '$—'
  return '$' + new Intl.NumberFormat('en-US', { maximumFractionDigits: 0 }).format(Math.round(amount))
}
function fmtPct(value) {
  if (value == null || isNaN(value)) return '—'
  return (value * 100).toFixed(1) + '%'
}
</script>
