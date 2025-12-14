import request from './request'

// 负债类型相关API
export const liabilityTypeAPI = {
  // 获取所有负债类型
  getAll() {
    return request.get('/liabilities/types')
  }
}

// 负债分类相关API
export const liabilityCategoryAPI = {
  // 获取所有大分类类型
  getTypes(userId) {
    return request.get('/liabilities/categories/types', { params: { userId } })
  },

  // 获取所有分类
  getAll(userId) {
    return request.get('/liabilities/categories', { params: { userId } })
  },

  // 创建分类
  create(data) {
    return request.post('/liabilities/categories', data)
  }
}

// 负债账户相关API
export const liabilityAccountAPI = {
  // 获取所有账户
  getAll(userId = null) {
    const params = userId ? { userId } : {}
    return request.get('/liabilities/accounts', { params })
  },

  // 按家庭获取所有账户
  getAllByFamily(familyId) {
    return request.get('/liabilities/accounts', { params: { familyId } })
  },

  // 获取所有活跃账户（带最新余额）
  getActiveAccounts(userId = null) {
    const params = userId ? { userId } : {}
    return request.get('/liabilities/accounts', { params })
  },

  // 获取单个账户
  getById(id) {
    return request.get(`/liabilities/accounts/${id}`)
  },

  // 创建账户
  create(data) {
    return request.post('/liabilities/accounts', data)
  },

  // 更新账户
  update(id, data) {
    return request.put(`/liabilities/accounts/${id}`, data)
  },

  // 删除账户（软删除）
  delete(id) {
    return request.delete(`/liabilities/accounts/${id}`)
  }
}

// 负债记录相关API
export const liabilityRecordAPI = {
  // 获取账户的所有记录
  getByAccountId(accountId) {
    return request.get(`/liabilities/accounts/${accountId}/records`)
  },

  // 创建记录
  create(data) {
    return request.post('/liabilities/records', data)
  },

  // 更新记录
  update(id, data) {
    return request.put(`/liabilities/records/${id}`, data)
  },

  // 删除记录
  delete(id) {
    return request.delete(`/liabilities/records/${id}`)
  },

  // 检查哪些账户在指定日期已有记录
  checkExisting(data) {
    return request.post('/liabilities/records/batch/check', data)
  },

  // 批量更新记录
  batchUpdate(data) {
    return request.post('/liabilities/records/batch', data)
  },

  // 获取指定日期账户的之前值(离该日期最近但不晚于该日期的记录)
  getValueAtDate(accountId, date) {
    return request.get(`/liabilities/accounts/${accountId}/value-at-date`, {
      params: { date }
    })
  }
}
