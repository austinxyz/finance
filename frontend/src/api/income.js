import request from './request'

// 收入分类相关API
export const incomeCategoryAPI = {
  // 获取所有分类（大类+子分类）
  getAll() {
    return request.get('/incomes/categories')
  },

  // 获取所有大类
  getAllMajor() {
    return request.get('/incomes/categories/major')
  },

  // 获取指定大类的所有小类
  getMinorByMajor(majorCategoryId) {
    return request.get(`/incomes/categories/major/${majorCategoryId}/minor`)
  },

  // 创建小类
  createMinor(data) {
    return request.post('/incomes/categories/minor', data)
  },

  // 更新小类
  updateMinor(id, data) {
    return request.put(`/incomes/categories/minor/${id}`, data)
  },

  // 删除小类
  deleteMinor(id) {
    return request.delete(`/incomes/categories/minor/${id}`)
  }
}

// 收入记录相关API
export const incomeRecordAPI = {
  // 获取指定期间的收入记录
  getByPeriod(familyId, period) {
    return request.get('/incomes/records', {
      params: { familyId, period }
    })
  },

  // 获取期间范围的收入记录
  getByPeriodRange(familyId, startPeriod, endPeriod) {
    return request.get('/incomes/records/range', {
      params: { familyId, startPeriod, endPeriod }
    })
  },

  // 创建收入记录
  create(data) {
    return request.post('/incomes/records', data)
  },

  // 批量保存收入记录
  batchSave(data) {
    return request.post('/incomes/records/batch', data)
  },

  // 更新收入记录
  update(id, data) {
    return request.put(`/incomes/records/${id}`, data)
  },

  // 删除收入记录
  delete(id) {
    return request.delete(`/incomes/records/${id}`)
  }
}

// 收入分析相关API
export const incomeAnalysisAPI = {
  // 获取年度大类汇总
  getAnnualMajorCategories(familyId, year, currency = 'USD') {
    return request.get('/incomes-analysis/annual/major-categories', {
      params: { familyId, year, currency }
    })
  },

  // 获取年度小类汇总
  getAnnualMinorCategories(familyId, year, majorCategoryId, currency = 'USD') {
    return request.get('/incomes-analysis/annual/minor-categories', {
      params: { familyId, year, majorCategoryId, currency }
    })
  },

  // 获取月度趋势
  getAnnualMonthlyTrend(familyId, year, majorCategoryId, minorCategoryId, currency = 'USD') {
    return request.get('/incomes-analysis/annual/monthly-trend', {
      params: { familyId, year, majorCategoryId, minorCategoryId, currency }
    })
  },

  // 刷新年度收入汇总数据
  refreshAnnualSummary(familyId, year, currency = 'All') {
    return request.post('/incomes-analysis/annual/refresh', null, {
      params: { familyId, year, currency }
    })
  }
}
