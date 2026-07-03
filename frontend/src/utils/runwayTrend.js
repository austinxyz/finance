// Pure helpers for the 资金跑道趋势 page.
// Data source = saved runway report snapshots (per-report points), amounts already USD.

/** Keep only the most recent N points; null/'all' → all. */
export function truncateToRecent(points, n) {
  if (!points) return []
  if (!n || n === 'all') return points
  return points.slice(-n)
}

/** Format a USD amount with symbol; null → dash. */
export function formatUSD(amount) {
  if (amount == null) return '—'
  return '$' + Math.round(Number(amount)).toLocaleString('en-US')
}

function kpiDelta(cur, prev, kind, higherIsGood) {
  if (prev == null) return null
  const diff = Number(cur) - Number(prev)
  const good = higherIsGood ? diff >= 0 : diff <= 0
  const arrow = diff > 0 ? '▲' : diff < 0 ? '▼' : '·'
  let text
  if (kind === 'pct') {
    const base = Number(prev)
    const pct = base === 0 ? 0 : (diff / base) * 100
    text = `${arrow} ${Math.abs(pct).toFixed(1)}% 较上次报告`
  } else {
    text = `${arrow} ${Math.abs(diff).toFixed(1)} 个月 较上次报告`
  }
  return { text, good }
}

/**
 * KPI values from the latest point + delta vs the immediately preceding report.
 * Fewer than two points → deltas are null (view renders a dash).
 */
export function computeKpis(points) {
  if (!points || points.length === 0) return null
  const latest = points[points.length - 1]
  const prev = points.length >= 2 ? points[points.length - 2] : null
  return {
    cash: {
      value: latest.liquidTotal,
      delta: prev ? kpiDelta(latest.liquidTotal, prev.liquidTotal, 'pct', true) : null,
    },
    runway: {
      value: latest.runwayMonths,
      delta: prev ? kpiDelta(latest.runwayMonths, prev.runwayMonths, 'abs', true) : null,
    },
    burn: {
      // more burn is worse → higherIsGood = false
      value: latest.monthlyBurn,
      delta: prev ? kpiDelta(latest.monthlyBurn, prev.monthlyBurn, 'pct', false) : null,
    },
    depletion: { value: latest.depletionDate },
  }
}

/**
 * Category rows for the latest report: share of total, bar width vs max,
 * and per-category change vs the previous report (新增 when absent before).
 */
export function computeCategoryRows(latest, previous) {
  if (!latest || latest.length === 0) return []
  const total = latest.reduce((s, c) => s + Number(c.amount), 0)
  const max = Math.max(...latest.map((c) => Number(c.amount)))
  const prevByCode = {}
  ;(previous || []).forEach((c) => {
    prevByCode[c.code] = Number(c.amount)
  })
  return latest.map((c) => {
    const amount = Number(c.amount)
    let delta
    if (!(c.code in prevByCode)) {
      delta = '新增'
    } else {
      const p = prevByCode[c.code]
      if (p === 0 || amount === p) {
        delta = '持平'
      } else {
        const pct = ((amount - p) / p) * 100
        delta = (pct > 0 ? '▲ ' : '▼ ') + Math.abs(pct).toFixed(0) + '%'
      }
    }
    return {
      code: c.code,
      name: c.name,
      color: c.color,
      amount,
      share: total > 0 ? Math.round((amount / total) * 100) : 0,
      barW: max > 0 ? Math.round((amount / max) * 100) : 0,
      delta,
    }
  })
}
