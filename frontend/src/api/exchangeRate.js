import request from './request'

// 汇率管理相关API
export const exchangeRateAPI = {
  // 获取所有启用的汇率
  getAll() {
    return request.get('/exchange-rates')
  },

  // 获取特定货币的所有汇率历史
  getByCurrency(currency) {
    return request.get(`/exchange-rates/currency/${currency}`)
  },

  // 获取特定货币在指定日期的汇率
  getRate(currency, date = null) {
    const params = { currency }
    if (date) {
      params.date = date
    }
    return request.get('/exchange-rates/rate', { params })
  },

  // 获取所有货币的当前最新汇率
  getLatest() {
    return request.get('/exchange-rates/latest')
  },

  // 获取特定日期的所有汇率
  getByDate(date) {
    return request.get(`/exchange-rates/date/${date}`)
  },

  // 创建新的汇率记录
  create(data) {
    return request.post('/exchange-rates', data)
  },

  // 更新汇率记录
  update(id, data) {
    return request.put(`/exchange-rates/${id}`, data)
  },

  // 删除汇率记录
  delete(id) {
    return request.delete(`/exchange-rates/${id}`)
  },

  // 停用汇率记录
  deactivate(id) {
    return request.put(`/exchange-rates/${id}/deactivate`)
  },

  // 初始化默认汇率
  initialize() {
    return request.post('/exchange-rates/initialize')
  }
}
