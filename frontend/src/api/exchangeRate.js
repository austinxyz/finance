import request from './request'

/**
 * 汇率API
 */
export const exchangeRateAPI = {
  /**
   * 获取最新的所有汇率
   */
  getLatest() {
    return request.get('/exchange-rates/latest')
  },

  /**
   * 获取特定货币的汇率
   * @param {String} currency - 货币代码 (如 CNY, EUR, GBP)
   * @param {String} date - 日期 (可选，格式: YYYY-MM-DD)
   */
  getRate(currency, date = null) {
    return request.get('/exchange-rates/rate', {
      params: { currency, date }
    })
  },

  /**
   * 获取特定日期的货币汇率（便捷方法）
   * @param {String} currency - 货币代码 (如 CNY, EUR, GBP)
   * @param {String} date - 日期 (格式: YYYY-MM-DD)
   */
  getRateByDate(currency, date) {
    return this.getRate(currency, date)
  },

  /**
   * 获取所有汇率（包括停用的）
   */
  getAll() {
    return request.get('/exchange-rates/all')
  },

  /**
   * 获取所有启用的汇率
   */
  getAllActive() {
    return request.get('/exchange-rates')
  },

  /**
   * 创建新汇率
   * @param {Object} data - 汇率数据
   */
  create(data) {
    return request.post('/exchange-rates', data)
  },

  /**
   * 更新汇率
   * @param {Number} id - 汇率ID
   * @param {Object} data - 汇率数据
   */
  update(id, data) {
    return request.put(`/exchange-rates/${id}`, data)
  },

  /**
   * 停用汇率
   * @param {Number} id - 汇率ID
   */
  deactivate(id) {
    return request.put(`/exchange-rates/${id}/deactivate`)
  },

  /**
   * 删除汇率
   * @param {Number} id - 汇率ID
   */
  delete(id) {
    return request.delete(`/exchange-rates/${id}`)
  },

  /**
   * 初始化默认汇率
   */
  initialize() {
    return request.post('/exchange-rates/initialize')
  },

  /**
   * 从第三方API获取并保存汇率
   * @param {String} date - 日期 (可选，格式: YYYY-MM-DD)
   */
  fetchFromAPI(date = null) {
    return request.post('/exchange-rates/fetch-from-api', null, {
      params: { date }
    })
  },

  /**
   * 获取特定货币在日期范围内的汇率
   * @param {String} currency - 货币代码
   * @param {String} startDate - 开始日期 (格式: YYYY-MM-DD)
   * @param {String} endDate - 结束日期 (格式: YYYY-MM-DD)
   */
  getRatesByRange(currency, startDate, endDate) {
    return request.get(`/exchange-rates/currency/${currency}/range`, {
      params: { startDate, endDate }
    })
  },

  /**
   * 获取特定货币的所有历史汇率
   * @param {String} currency - 货币代码
   */
  getRatesByCurrency(currency) {
    return request.get(`/exchange-rates/currency/${currency}`)
  },

  /**
   * 获取特定日期的所有汇率
   * @param {String} date - 日期 (格式: YYYY-MM-DD)
   */
  getRatesByDate(date) {
    return request.get(`/exchange-rates/date/${date}`)
  }
}

export default exchangeRateAPI
