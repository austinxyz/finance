import request from './request'

// 用户相关API
export const userAPI = {
  // 获取所有用户
  getAll() {
    return request.get('/users')
  },

  // 获取单个用户
  getById(id) {
    return request.get(`/users/${id}`)
  },

  // 创建用户
  create(data) {
    return request.post('/users', data)
  },

  // 更新用户
  update(id, data) {
    return request.put(`/users/${id}`, data)
  },

  // 删除用户
  delete(id) {
    return request.delete(`/users/${id}`)
  }
}
