import request from './request'

const BASE_URL = '/property-records'

export const propertyRecordAPI = {
  // 创建房产记录
  create(data) {
    return request.post(BASE_URL, data)
  },

  // 更新房产记录
  update(id, data) {
    return request.put(`${BASE_URL}/${id}`, data)
  },

  // 删除房产记录
  delete(id) {
    return request.delete(`${BASE_URL}/${id}`)
  },

  // 获取房产记录详情
  getById(id) {
    return request.get(`${BASE_URL}/${id}`)
  },

  // 根据资产账户ID获取房产记录
  getByAssetAccountId(assetAccountId) {
    return request.get(`${BASE_URL}/by-asset/${assetAccountId}`)
  },

  // 获取家庭所有房产记录
  getByFamilyId(familyId) {
    return request.get(`${BASE_URL}/family/${familyId}`)
  },

  // 获取指定年份购买的房产记录
  getByFamilyIdAndYear(familyId, year) {
    return request.get(`${BASE_URL}/family/${familyId}/year/${year}`)
  }
}
