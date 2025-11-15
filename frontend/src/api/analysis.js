import request from './request'

// 分析相关API
export const analysisAPI = {
  // 获取资产总览 (不传userId则获取所有用户的资产)
  getSummary(userId = null, asOfDate = null) {
    const params = {}
    if (userId) params.userId = userId
    if (asOfDate) params.asOfDate = asOfDate
    return request.get('/analysis/summary', { params })
  },

  // 获取总资产趋势 (不传userId则获取所有用户的资产)
  getTotalTrend(userId = null, startDate, endDate) {
    const params = {}
    if (userId) params.userId = userId
    if (startDate) params.startDate = startDate
    if (endDate) params.endDate = endDate
    return request.get('/analysis/trends/total', { params })
  },

  // 获取单个账户趋势
  getAccountTrend(accountId) {
    return request.get(`/analysis/trends/account/${accountId}`)
  },

  // 获取按分类的资产配置 (不传userId则获取所有用户的资产)
  getAllocationByCategory(userId = null) {
    const params = userId ? { userId } : {}
    return request.get('/analysis/allocation/category', { params })
  },

  // 获取按类型的资产配置 (不传userId则获取所有用户的资产)
  getAllocationByType(userId = null, asOfDate = null) {
    const params = {}
    if (userId) params.userId = userId
    if (asOfDate) params.asOfDate = asOfDate
    return request.get('/analysis/allocation/type', { params })
  },

  // 获取净资产配置（资产减去对应负债）
  getNetAssetAllocation(userId = null, asOfDate = null) {
    const params = {}
    if (userId) params.userId = userId
    if (asOfDate) params.asOfDate = asOfDate
    return request.get('/analysis/allocation/net', { params })
  },

  // 获取按类型的负债配置
  getLiabilityAllocation(userId = null, asOfDate = null) {
    const params = {}
    if (userId) params.userId = userId
    if (asOfDate) params.asOfDate = asOfDate
    return request.get('/analysis/allocation/liability', { params })
  },

  // 获取综合趋势数据（净资产、总资产、总负债）
  getOverallTrend(startDate, endDate, userId = null) {
    const params = { startDate, endDate }
    if (userId) params.userId = userId
    return request.get('/analysis/trends/overall', { params })
  },

  // 获取资产分类趋势数据
  getAssetCategoryTrend(categoryType, startDate, endDate, userId = null) {
    const params = { startDate, endDate }
    if (userId) params.userId = userId
    return request.get(`/analysis/trends/asset-category/${categoryType}`, { params })
  },

  // 获取负债分类趋势数据
  getLiabilityCategoryTrend(categoryType, startDate, endDate, userId = null) {
    const params = { startDate, endDate }
    if (userId) params.userId = userId
    return request.get(`/analysis/trends/liability-category/${categoryType}`, { params })
  },

  // 获取净资产分类趋势数据
  getNetAssetCategoryTrend(categoryCode, startDate, endDate, userId = null) {
    const params = { startDate, endDate }
    if (userId) params.userId = userId
    return request.get(`/analysis/trends/net-asset-category/${categoryCode}`, { params })
  },

  // 获取指定类型和日期的资产账户及其余额
  getAssetAccountsWithBalances(categoryType, userId = null, asOfDate = null) {
    const params = {}
    if (userId) params.userId = userId
    if (asOfDate) params.asOfDate = asOfDate
    return request.get(`/analysis/allocation/asset-accounts/${categoryType}`, { params })
  },

  // 获取指定类型和日期的负债账户及其余额
  getLiabilityAccountsWithBalances(categoryType, userId = null, asOfDate = null) {
    const params = {}
    if (userId) params.userId = userId
    if (asOfDate) params.asOfDate = asOfDate
    return request.get(`/analysis/allocation/liability-accounts/${categoryType}`, { params })
  },

  // 获取资产分类下所有账户的趋势数据
  getAssetAccountsTrendByCategory(categoryType, startDate, endDate, userId = null) {
    const params = { startDate, endDate }
    if (userId) params.userId = userId
    return request.get(`/analysis/trends/asset-accounts/${categoryType}`, { params })
  },

  // 获取负债分类下所有账户的趋势数据
  getLiabilityAccountsTrendByCategory(categoryType, startDate, endDate, userId = null) {
    const params = { startDate, endDate }
    if (userId) params.userId = userId
    return request.get(`/analysis/trends/liability-accounts/${categoryType}`, { params })
  },

  // 获取净资产类别下的所有账户详情（包含资产账户和负债账户）
  getNetAssetCategoryAccounts(categoryCode, userId = null, asOfDate = null) {
    const params = {}
    if (userId) params.userId = userId
    if (asOfDate) params.asOfDate = asOfDate
    return request.get(`/analysis/allocation/net-asset-accounts/${categoryCode}`, { params })
  },

  // 获取按税收状态的净资产配置
  getNetWorthByTaxStatus(userId = null, asOfDate = null) {
    const params = {}
    if (userId) params.userId = userId
    if (asOfDate) params.asOfDate = asOfDate
    return request.get('/analysis/allocation/net-worth-by-tax-status', { params })
  }
}
