import request from './request'

// 资产类型相关API (8个大类)
export const assetTypeAPI = {
  // 获取所有资产类型
  getAll() {
    return request.get('/assets/types')
  }
}

// 资产账户相关API
export const assetAccountAPI = {
  // 获取所有账户
  getAll(userId = null) {
    const params = userId ? { userId } : {}
    return request.get('/assets/accounts', { params })
  },

  // 按家庭获取所有账户
  getAllByFamily(familyId) {
    return request.get('/assets/accounts', { params: { familyId } })
  },

  // 获取所有活跃账户（带最新余额）
  getActiveAccounts(userId = null) {
    const params = userId ? { userId } : {}
    return request.get('/assets/accounts', { params })
  },

  // 获取单个账户
  getById(id) {
    return request.get(`/assets/accounts/${id}`)
  },

  // 创建账户
  create(data) {
    return request.post('/assets/accounts', data)
  },

  // 更新账户
  update(id, data) {
    return request.put(`/assets/accounts/${id}`, data)
  },

  // 删除账户（软删除）
  delete(id) {
    return request.delete(`/assets/accounts/${id}`)
  }
}

// 资产记录相关API
export const assetRecordAPI = {
  // 获取账户的所有记录
  getByAccountId(accountId) {
    return request.get(`/assets/accounts/${accountId}/records`)
  },

  // 创建记录
  create(data) {
    return request.post('/assets/records', data)
  },

  // 更新记录
  update(id, data) {
    return request.put(`/assets/records/${id}`, data)
  },

  // 删除记录
  delete(id) {
    return request.delete(`/assets/records/${id}`)
  },

  // 检查哪些账户在指定日期已有记录
  checkExisting(data) {
    return request.post('/assets/records/batch/check', data)
  },

  // 批量更新记录
  batchUpdate(data) {
    return request.post('/assets/records/batch', data)
  },

  // 获取指定日期账户的之前值(离该日期最近但不晚于该日期的记录)
  getValueAtDate(accountId, date) {
    return request.get(`/assets/accounts/${accountId}/value-at-date`, {
      params: { date }
    })
  }
}
