import request from './request'

// 分析相关API
export const analysisAPI = {
  // 获取资产总览
  getSummary(userId) {
    return request.get('/analysis/summary', { params: { userId } })
  },

  // 获取总资产趋势
  getTotalTrend(userId, startDate, endDate) {
    const params = { userId }
    if (startDate) params.startDate = startDate
    if (endDate) params.endDate = endDate
    return request.get('/analysis/trends/total', { params })
  },

  // 获取单个账户趋势
  getAccountTrend(accountId) {
    return request.get(`/analysis/trends/account/${accountId}`)
  },

  // 获取按分类的资产配置
  getAllocationByCategory(userId) {
    return request.get('/analysis/allocation/category', { params: { userId } })
  },

  // 获取按类型的资产配置
  getAllocationByType(userId) {
    return request.get('/analysis/allocation/type', { params: { userId } })
  }
}
