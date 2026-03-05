import request from './request'

export const runwayAPI = {
  /**
   * Get runway analysis for a family
   * @param {number} familyId
   * @param {number} months - lookback window (default 6)
   * @param {string[]} includedTypes - asset type codes to include as liquid
   */
  getAnalysis(familyId, months = 6, includedTypes = null) {
    const params = { familyId, months }
    if (includedTypes && includedTypes.length > 0) {
      params.includedTypes = includedTypes
    }
    return request.get('/runway/analysis', { params })
  },

  saveRunwayReport(familyId, snapshotJson) {
    return request.post('/runway/reports', { familyId, snapshotJson })
  },

  listRunwayReports(familyId) {
    return request.get('/runway/reports', { params: { familyId } })
  },

  getRunwayReport(id, familyId) {
    return request.get(`/runway/reports/${id}`, { params: { familyId } })
  },

  deleteRunwayReport(id) {
    return request.delete(`/runway/reports/${id}`)
  }
}
