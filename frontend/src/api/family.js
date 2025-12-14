import request from './request'

/**
 * 家庭管理API
 */
export const familyAPI = {
  /**
   * 获取所有家庭列表
   */
  getAll() {
    return request.get('/family')
  },

  /**
   * 获取指定家庭信息
   * @param {Number} familyId - 家庭ID
   */
  getById(familyId) {
    return request.get(`/family/${familyId}`)
  },

  /**
   * 创建新家庭
   * @param {Object} data - 家庭数据
   */
  create(data) {
    return request.post('/family', data)
  },

  /**
   * 更新家庭信息
   * @param {Number} familyId - 家庭ID
   * @param {Object} data - 家庭数据
   */
  update(familyId, data) {
    return request.post(`/family/${familyId}`, data)
  },

  /**
   * 获取家庭成员
   * @param {Number} familyId - 家庭ID
   */
  getMembers(familyId) {
    return request.get(`/family/${familyId}/members`)
  },

  /**
   * 获取默认家庭
   */
  getDefault() {
    return request.get('/family/default')
  },

  /**
   * 设置默认家庭
   * @param {Number} familyId - 家庭ID
   */
  setDefault(familyId) {
    return request.post(`/family/${familyId}/set-default`)
  }
}

export default familyAPI
