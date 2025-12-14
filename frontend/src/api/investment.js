import request from './request'

// 投资账户相关API
export const investmentAccountAPI = {
  // 获取所有投资账户
  getAll(familyId) {
    return request.get('/investments/accounts', {
      params: { familyId }
    })
  },

  // 按大类获取投资账户
  getByCategory(familyId, categoryId) {
    return request.get('/investments/accounts/by-category', {
      params: { familyId, categoryId }
    })
  },

  // 获取投资类别列表（is_investment = TRUE的资产分类）
  getCategories() {
    return request.get('/investments/categories')
  }
}

// 投资交易记录相关API
export const investmentTransactionAPI = {
  // 获取账户的交易记录
  getByAccount(accountId, startPeriod = null, endPeriod = null) {
    const params = { accountId }
    if (startPeriod) params.startPeriod = startPeriod
    if (endPeriod) params.endPeriod = endPeriod
    return request.get('/investments/transactions', { params })
  },

  // 创建交易记录
  create(data) {
    return request.post('/investments/transactions', data)
  },

  // 更新交易记录
  update(id, data) {
    return request.put(`/investments/transactions/${id}`, data)
  },

  // 删除交易记录
  delete(id) {
    return request.delete(`/investments/transactions/${id}`)
  },

  // 批量保存交易记录
  batchSave(data) {
    return request.post('/investments/transactions/batch', data)
  }
}

// 投资分析相关API
export const investmentAnalysisAPI = {
  // 获取年度投资汇总
  getAnnualSummary(familyId, year, currency = 'USD') {
    return request.get('/investments/analysis/annual/summary', {
      params: { familyId, year, currency }
    })
  },

  // 获取大类投资分析
  getAnnualByCategory(familyId, year, currency = 'USD') {
    return request.get('/investments/analysis/annual/by-category', {
      params: { familyId, year, currency }
    })
  },

  // 获取账户投资分析
  getAnnualByAccount(familyId, year, assetTypeId = null, currency = 'USD') {
    const params = { familyId, year, currency }
    if (assetTypeId) params.assetTypeId = assetTypeId
    return request.get('/investments/analysis/annual/by-account', { params })
  },

  // 获取账户月度趋势
  getAccountMonthlyTrend(accountId, year) {
    return request.get('/investments/analysis/annual/monthly-trend', {
      params: { accountId, year }
    })
  }
}
