import request from './request'

/**
 * Google Sheets同步API
 */
export default {
  /**
   * 同步年度财务报表到Google Sheets（异步）
   * @param {Object} params - 参数对象
   * @param {number} params.familyId - 家庭ID
   * @param {number} params.year - 年份
   * @param {string} params.permission - 权限设置（reader或writer）
   * @returns {Promise} 包含任务ID的响应
   */
  syncAnnualReport({ familyId, year, permission = 'reader' }) {
    return request.post('/google-sheets/sync-annual-report', null, {
      params: {
        familyId,
        year,
        permission
      }
    })
  },

  /**
   * 查询同步任务状态
   * @param {number} syncId - 同步任务ID
   * @returns {Promise} 任务状态和进度
   */
  getSyncStatus(syncId) {
    return request.get(`/google-sheets/sync-status/${syncId}`)
  },

  /**
   * 测试Google Sheets连接
   * @returns {Promise} 连接状态
   */
  testConnection() {
    return request.get('/google-sheets/test-connection')
  }
}
