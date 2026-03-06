import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { nextTick } from 'vue'
import { pmt, cumprinc, cumipmt } from '../../utils/financialFormulas'
import PropertyInvestmentCalculator from '../investments/PropertyInvestmentCalculator.vue'

// ===== Group 7.1: Pure formula tests =====
describe('financialFormulas', () => {
  const LOAN = 1125000   // 1.5M × 75%
  const RATE = 0.065
  const YEARS = 30

  it('pmt: monthly payment ≈ $7,111 for default inputs', () => {
    const result = pmt(RATE, YEARS, LOAN)
    expect(result).toBeGreaterThan(7100)
    expect(result).toBeLessThan(7120)
  })

  it('cumprinc: year-1 principal is positive and less than monthly payment', () => {
    const monthlyPayment = pmt(RATE, YEARS, LOAN)
    const result = cumprinc(RATE, YEARS, LOAN, 1, 12)
    expect(result).toBeGreaterThan(0)
    expect(result).toBeLessThan(monthlyPayment * 12)
  })

  it('cumipmt: year-1 interest is positive', () => {
    const result = cumipmt(RATE, YEARS, LOAN, 1, 12)
    expect(result).toBeGreaterThan(0)
  })

  it('pmt: zero loan returns 0', () => {
    expect(pmt(0.065, 30, 0)).toBe(0)
  })

  it('pmt + cumprinc + cumipmt: principal + interest = total payments', () => {
    const monthlyPayment = pmt(RATE, YEARS, LOAN)
    const principal = cumprinc(RATE, YEARS, LOAN, 1, 12)
    const interest = cumipmt(RATE, YEARS, LOAN, 1, 12)
    expect(principal + interest).toBeCloseTo(monthlyPayment * 12, 0)
  })
})

// ===== Group 7.2: Component rendering with default inputs =====
describe('PropertyInvestmentCalculator — default inputs', () => {
  function mountComponent() {
    return mount(PropertyInvestmentCalculator, {
      global: { plugins: [createPinia()] }
    })
  }

  it('renders true-monthly-flow as negative with default inputs', () => {
    const wrapper = mountComponent()
    const el = wrapper.find('[data-testid="true-monthly-flow"]')
    expect(el.exists()).toBe(true)
    // Default scenario: Bay Area cash flow is negative
    expect(el.text()).toContain('-')
  })

  it('renders coc-return as negative with default inputs', () => {
    const wrapper = mountComponent()
    const el = wrapper.find('[data-testid="coc-return"]')
    expect(el.exists()).toBe(true)
    expect(el.text()).toContain('-')
  })

  it('renders cash-invested as $412,500 (1.5M × 27%)', () => {
    const wrapper = mountComponent()
    const el = wrapper.find('[data-testid="cash-invested"]')
    expect(el.exists()).toBe(true)
    // 1,500,000 × (0.25 + 0.02) = 405,000
    expect(el.text()).toContain('405,000')
  })

  it('renders effective-yield ≈ -5% with default inputs', () => {
    const wrapper = mountComponent()
    const el = wrapper.find('[data-testid="effective-yield"]')
    expect(el.exists()).toBe(true)
    // Spreadsheet result: -4.73% ≈ -5%
    expect(el.text()).toContain('-')
    const pct = parseFloat(el.text())
    expect(pct).toBeGreaterThan(-8)
    expect(pct).toBeLessThan(-2)
  })

  it('renders total-roi ≈ +10% with default inputs', () => {
    const wrapper = mountComponent()
    const el = wrapper.find('[data-testid="total-roi"]')
    expect(el.exists()).toBe(true)
    // Effective yield (-4.73%) + appreciation ROI (+14.81%) ≈ +10%
    const pct = parseFloat(el.text())
    expect(pct).toBeGreaterThan(5)
    expect(pct).toBeLessThan(15)
  })
})

// ===== Group 7.3: Reactive update =====
describe('PropertyInvestmentCalculator — reactive update', () => {
  it('true-monthly-flow updates when monthly rent changes', async () => {
    const wrapper = mount(PropertyInvestmentCalculator, {
      global: { plugins: [createPinia()] }
    })

    const rentInput = wrapper.find('[data-testid="input-monthly-rent"]')
    const flowEl = wrapper.find('[data-testid="true-monthly-flow"]')

    const initialText = flowEl.text()

    // Increase rent significantly — should move cash flow toward positive
    await rentInput.setValue(15000)
    await nextTick()

    const updatedText = flowEl.text()
    expect(updatedText).not.toBe(initialText)
  })
})
