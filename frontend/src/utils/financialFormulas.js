/**
 * Financial formula utilities — pure functions, no side effects.
 * All return positive numbers representing absolute payment amounts.
 */

/**
 * PMT — monthly payment for a fixed-rate loan (principal + interest).
 * @param {number} annualRate  Annual interest rate (e.g. 0.065 for 6.5%)
 * @param {number} years       Loan term in years
 * @param {number} presentValue  Loan amount (principal)
 * @returns {number}  Monthly payment (positive)
 */
export function pmt(annualRate, years, presentValue) {
  if (presentValue <= 0 || annualRate <= 0) return 0
  const r = annualRate / 12
  const n = years * 12
  return (r * presentValue) / (1 - Math.pow(1 + r, -n))
}

/**
 * CUMPRINC — cumulative principal paid between two periods (inclusive).
 * @param {number} annualRate
 * @param {number} years
 * @param {number} presentValue
 * @param {number} startPeriod  First period (1-based)
 * @param {number} endPeriod    Last period (1-based)
 * @returns {number}  Total principal paid (positive)
 */
export function cumprinc(annualRate, years, presentValue, startPeriod, endPeriod) {
  if (presentValue <= 0 || annualRate <= 0) return 0
  const r = annualRate / 12
  const n = years * 12
  const payment = pmt(annualRate, years, presentValue)
  let total = 0
  let balance = presentValue
  for (let i = 1; i <= endPeriod; i++) {
    const interestPortion = balance * r
    const principalPortion = payment - interestPortion
    if (i >= startPeriod) total += principalPortion
    balance -= principalPortion
  }
  return total
}

/**
 * CUMIPMT — cumulative interest paid between two periods (inclusive).
 * @param {number} annualRate
 * @param {number} years
 * @param {number} presentValue
 * @param {number} startPeriod  First period (1-based)
 * @param {number} endPeriod    Last period (1-based)
 * @returns {number}  Total interest paid (positive)
 */
export function cumipmt(annualRate, years, presentValue, startPeriod, endPeriod) {
  if (presentValue <= 0 || annualRate <= 0) return 0
  const r = annualRate / 12
  const payment = pmt(annualRate, years, presentValue)
  let total = 0
  let balance = presentValue
  for (let i = 1; i <= endPeriod; i++) {
    const interestPortion = balance * r
    const principalPortion = payment - interestPortion
    if (i >= startPeriod) total += interestPortion
    balance -= principalPortion
  }
  return total
}
