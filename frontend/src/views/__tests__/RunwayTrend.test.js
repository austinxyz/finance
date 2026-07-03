import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'

// --- mocks ---
const getRunwayTrend = vi.fn()
vi.mock('../../api/runway', () => ({
  runwayAPI: { getRunwayTrend: (...a) => getRunwayTrend(...a) },
}))
vi.mock('../../stores/auth', () => ({
  useAuthStore: () => ({ familyId: 1 }),
}))
// Avoid chart.js touching canvas (unsupported in jsdom)
vi.mock('vue-chartjs', () => ({ Line: { name: 'Line', render: () => null } }))

import RunwayTrend from '../analysis/RunwayTrend.vue'

const mountView = () =>
  mount(RunwayTrend, {
    global: {
      stubs: {
        Line: true,
        RouterLink: { template: '<a :href="to"><slot /></a>', props: ['to'] },
      },
    },
  })

const threePoints = {
  points: [
    { savedAt: '2026-01-15T10:00:00', reportName: 'r1', liquidTotal: 180000, monthlyBurn: 11000, runwayMonths: 16, depletionDate: '2027-05' },
    { savedAt: '2026-03-02T10:00:00', reportName: 'r2', liquidTotal: 175000, monthlyBurn: 11800, runwayMonths: 15, depletionDate: '2027-06' },
    { savedAt: '2026-06-20T10:00:00', reportName: 'r3', liquidTotal: 168500, monthlyBurn: 12600, runwayMonths: 13, depletionDate: '2027-08' },
  ],
  categories: [
    { code: 'RENT', name: '房租', color: 'hsl(142 76% 36%)', amount: 6800 },
    { code: 'FOOD', name: '餐饮', color: 'hsl(217 91% 60%)', amount: 3200 },
  ],
  previousCategories: [
    { code: 'RENT', name: '房租', color: 'hsl(142 76% 36%)', amount: 6800 },
  ],
}

describe('RunwayTrend.vue', () => {
  beforeEach(() => getRunwayTrend.mockReset())

  it('shows empty-state with link to 资金跑道分析 when no reports', async () => {
    getRunwayTrend.mockResolvedValue({ success: true, data: { points: [], categories: [], previousCategories: [] } })
    const w = mountView()
    await flushPromises()
    expect(w.text()).toContain('资金跑道分析')
    expect(w.html()).toContain('/analysis/runway')
  })

  it('renders per-report KPI labels (report-value wording, not monthly)', async () => {
    getRunwayTrend.mockResolvedValue({ success: true, data: threePoints })
    const w = mountView()
    await flushPromises()
    const t = w.text()
    expect(t).toContain('当前现金余额')
    expect(t).toContain('剩余跑道')
    expect(t).toContain('月度净烧钱率（报告值）')
    expect(t).toContain('预计现金耗尽')
    expect(t).not.toContain('近3月均')
  })

  it('applies finance-ui brand token classes', async () => {
    getRunwayTrend.mockResolvedValue({ success: true, data: threePoints })
    const w = mountView()
    await flushPromises()
    expect(w.html()).toMatch(/bg-primary/)
  })

  it('metric tabs switch the active metric', async () => {
    getRunwayTrend.mockResolvedValue({ success: true, data: threePoints })
    const w = mountView()
    await flushPromises()
    const burnTab = w.findAll('button').find((b) => b.text().includes('月度净烧钱'))
    expect(burnTab).toBeTruthy()
    await burnTab.trigger('click')
    expect(burnTab.classes().join(' ')).toMatch(/bg-primary/)
  })

  it('category table shows 较上次报告 delta wording', async () => {
    getRunwayTrend.mockResolvedValue({ success: true, data: threePoints })
    const w = mountView()
    await flushPromises()
    expect(w.text()).toContain('分类支出明细')
    expect(w.text()).toContain('较上次报告')
  })
})
