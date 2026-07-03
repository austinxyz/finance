import { describe, it, expect } from 'vitest'
import { truncateToRecent, computeKpis, computeCategoryRows } from '../runwayTrend'

const pts = [
  { savedAt: '2026-01-15T10:00:00', reportName: 'r1', liquidTotal: 180000, monthlyBurn: 11000, runwayMonths: 16, depletionDate: '2027-05' },
  { savedAt: '2026-03-02T10:00:00', reportName: 'r2', liquidTotal: 175000, monthlyBurn: 11800, runwayMonths: 15, depletionDate: '2027-06' },
  { savedAt: '2026-06-20T10:00:00', reportName: 'r3', liquidTotal: 168500, monthlyBurn: 12600, runwayMonths: 13, depletionDate: '2027-08' },
]

describe('truncateToRecent', () => {
  it('returns last N reports', () => {
    expect(truncateToRecent(pts, 2).map(p => p.reportName)).toEqual(['r2', 'r3'])
  })
  it('null/all returns everything', () => {
    expect(truncateToRecent(pts, null)).toHaveLength(3)
    expect(truncateToRecent(pts, 'all')).toHaveLength(3)
  })
})

describe('computeKpis', () => {
  it('latest values + delta vs previous report', () => {
    const k = computeKpis(pts)
    expect(k.cash.value).toBe(168500)
    expect(k.runway.value).toBe(13)
    expect(k.burn.value).toBe(12600)
    expect(k.depletion.value).toBe('2027-08')
    // cash fell → not good; delta text present
    expect(k.cash.delta).not.toBeNull()
    expect(k.cash.delta.good).toBe(false)
    // runway fell → not good
    expect(k.runway.delta.good).toBe(false)
    // burn rose → not good (more burn is worse)
    expect(k.burn.delta.good).toBe(false)
  })
  it('single report → deltas null (rendered as dash)', () => {
    const k = computeKpis([pts[2]])
    expect(k.cash.value).toBe(168500)
    expect(k.cash.delta).toBeNull()
    expect(k.runway.delta).toBeNull()
    expect(k.burn.delta).toBeNull()
  })
  it('empty → null', () => {
    expect(computeKpis([])).toBeNull()
  })
})

describe('computeCategoryRows', () => {
  const latest = [
    { code: 'RENT', name: '房租', color: '#1', amount: 6800 },
    { code: 'FOOD', name: '餐饮', color: '#2', amount: 3200 },
    { code: 'NEW', name: '教育', color: '#3', amount: 1200 },
  ]
  const prev = [
    { code: 'RENT', name: '房租', color: '#1', amount: 6800 },
    { code: 'FOOD', name: '餐饮', color: '#2', amount: 3000 },
  ]
  it('computes share and bar width', () => {
    const rows = computeCategoryRows(latest, prev)
    const rent = rows.find(r => r.code === 'RENT')
    expect(rent.share).toBe(61) // 6800 / 11200 ≈ 61%
    expect(rent.barW).toBe(100) // largest
  })
  it('delta vs previous: 持平 / 变化 / 新增', () => {
    const rows = computeCategoryRows(latest, prev)
    expect(rows.find(r => r.code === 'RENT').delta).toBe('持平')
    expect(rows.find(r => r.code === 'FOOD').delta).toContain('%')
    expect(rows.find(r => r.code === 'NEW').delta).toBe('新增')
  })
  it('empty latest → empty rows', () => {
    expect(computeCategoryRows([], [])).toEqual([])
  })
})
