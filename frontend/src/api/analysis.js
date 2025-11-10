import request from './request'

// 分析相关API
export const analysisAPI = {
  // 获取资产总览 (不传userId则获取所有用户的资产)
  getSummary(userId = null) {
    const params = userId ? { userId } : {}
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
  getAllocationByType(userId = null) {
    const params = userId ? { userId } : {}
    return request.get('/analysis/allocation/type', { params })
  },

  // 获取净资产配置（资产减去对应负债）
  getNetAssetAllocation(userId = null) {
    const params = userId ? { userId } : {}
    return request.get('/analysis/allocation/net', { params })
  },

  // 获取按类型的负债配置
  getLiabilityAllocation(userId = null) {
    const params = userId ? { userId } : {}
    return request.get('/analysis/allocation/liability', { params })
  }
}
