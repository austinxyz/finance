import request from './request'

/**
 * 年度财务摘要API（按家庭统计）
 */
export const annualSummaryAPI = {
  /**
   * 获取家庭所有年度摘要
   * @param {Number} familyId - 家庭ID
   */
  getAll(familyId) {
    return request.get(`/annual-summary/family/${familyId}`)
  },

  /**
   * 获取家庭指定年份的摘要
   * @param {Number} familyId - 家庭ID
   * @param {Number} year - 年份
   */
  getByYear(familyId, year) {
    return request.get(`/annual-summary/family/${familyId}/year/${year}`)
  },

  /**
   * 获取家庭指定年份范围的摘要
   * @param {Number} familyId - 家庭ID
   * @param {Number} startYear - 开始年份
   * @param {Number} endYear - 结束年份
   */
  getByRange(familyId, startYear, endYear) {
    return request.get(`/annual-summary/family/${familyId}/range`, {
      params: { startYear, endYear }
    })
  },

  /**
   * 获取最近N年的摘要
   * @param {Number} familyId - 家庭ID
   * @param {Number} limit - 年份数量，默认5年
   */
  getRecent(familyId, limit = 5) {
    return request.get(`/annual-summary/family/${familyId}/recent`, {
      params: { limit }
    })
  },

  /**
   * 计算或刷新指定年份的财务摘要
   * @param {Number} familyId - 家庭ID
   * @param {Number} year - 年份
   */
  calculate(familyId, year) {
    return request.post(`/annual-summary/family/${familyId}/calculate/${year}`)
  },

  /**
   * 批量刷新多个年份的摘要
   * @param {Number} familyId - 家庭ID
   * @param {Array<Number>} years - 年份数组
   */
  batchCalculate(familyId, years) {
    return request.post(`/annual-summary/family/${familyId}/batch-calculate`, years)
  },

  /**
   * 删除指定年份的摘要
   * @param {Number} familyId - 家庭ID
   * @param {Number} year - 年份
   */
  delete(familyId, year) {
    return request.delete(`/annual-summary/family/${familyId}/year/${year}`)
  },

  /**
   * 手动创建或更新摘要
   * @param {Number} familyId - 家庭ID
   * @param {Object} data - 摘要数据
   */
  saveOrUpdate(familyId, data) {
    return request.post(`/annual-summary/family/${familyId}/save`, data)
  }
}

export default annualSummaryAPI
