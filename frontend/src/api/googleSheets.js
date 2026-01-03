import request from './request'

/**
 * Google Sheets同步API
 */
export default {
  /**
   * 同步年度财务报表到Google Sheets
   * @param {Object} params - 参数对象
   * @param {number} params.familyId - 家庭ID
   * @param {number} params.year - 年份
   * @param {string} params.permission - 权限设置（reader或writer）
   * @returns {Promise} 包含分享链接和spreadsheetId的响应
   */
  syncAnnualReport({ familyId, year, permission = 'reader' }) {
    return request.post('/google-sheets/sync-annual-report', null, {
      params: {
        familyId,
        year,
        permission
      },
      timeout: 60000 // 60秒超时，因为需要创建多个工作表并设置权限
    })
  },

  /**
   * 测试Google Sheets连接
   * @returns {Promise} 连接状态
   */
  testConnection() {
    return request.get('/google-sheets/test-connection')
  }
}
