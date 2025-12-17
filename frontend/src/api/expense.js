import request from './request'

// 支出分类相关API
export const expenseCategoryAPI = {
  // 获取所有分类（大类+子分类）
  getAll() {
    return request.get('/expenses/categories')
  },

  // 创建子分类
  createMinor(data) {
    return request.post('/expenses/categories/minor', data)
  },

  // 更新子分类
  updateMinor(id, data) {
    return request.put(`/expenses/categories/minor/${id}`, data)
  },

  // 停用子分类
  disableMinor(id) {
    return request.delete(`/expenses/categories/minor/${id}`)
  }
}

// 支出记录相关API
export const expenseRecordAPI = {
  // 获取指定期间的支出记录
  getByPeriod(familyId, period) {
    return request.get('/expenses/records', {
      params: { familyId, period }
    })
  },

  // 获取期间范围的支出记录
  getByPeriodRange(familyId, startPeriod, endPeriod) {
    return request.get('/expenses/records/range', {
      params: { familyId, startPeriod, endPeriod }
    })
  },

  // 创建支出记录
  create(data) {
    return request.post('/expenses/records', data)
  },

  // 批量保存支出记录
  batchSave(data) {
    return request.post('/expenses/records/batch', data)
  },

  // 更新支出记录
  update(id, data) {
    return request.put(`/expenses/records/${id}`, data)
  },

  // 删除支出记录
  delete(id) {
    return request.delete(`/expenses/records/${id}`)
  }
}

// 支出分析相关API
export const expenseAnalysisAPI = {
  // 获取年度大类汇总
  getAnnualMajorCategories(familyId, year, currency = 'USD') {
    return request.get('/expenses/analysis/annual/major-categories', {
      params: { familyId, year, currency }
    })
  },

  // 获取年度小类汇总
  getAnnualMinorCategories(familyId, year, majorCategoryId, currency = 'USD') {
    return request.get('/expenses/analysis/annual/minor-categories', {
      params: { familyId, year, majorCategoryId, currency }
    })
  },

  // 获取月度趋势
  getAnnualMonthlyTrend(familyId, year, minorCategoryId, currency = 'USD') {
    return request.get('/expenses/analysis/annual/monthly-trend', {
      params: { familyId, year, minorCategoryId, currency }
    })
  },

  // 获取预算执行分析
  getBudgetExecution(familyId, budgetYear, currency = 'USD') {
    return request.get('/expenses/analysis/budget-execution', {
      params: { familyId, budgetYear, currency }
    })
  },

  // 获取年度支出汇总（实际）- 包含资产/负债调整
  getAnnualSummary(familyId, year, currency = 'USD', includeTotals = true) {
    return request.get('/expenses/analysis/annual/summary', {
      params: { familyId, year, currency, includeTotals }
    })
  },

  // 刷新年度支出汇总（触发存储过程）
  refreshAnnualSummary(familyId, year) {
    return request.post('/expenses/analysis/annual/summary/calculate', null, {
      params: { familyId, year }
    })
  },

  // 获取多年度支出趋势数据
  getAnnualTrend(familyId, limit = 5, currency = 'USD') {
    return request.get('/expenses/analysis/annual/trend', {
      params: { familyId, limit, currency }
    })
  },

  // 获取各大类的多年度基础支出趋势
  getAnnualCategoryTrend(familyId, limit = 5, currency = 'USD') {
    return request.get('/expenses/analysis/annual/category-trend', {
      params: { familyId, limit, currency }
    })
  }
}
