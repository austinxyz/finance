import request from './request'

// 认证相关API
export const authAPI = {
  // 用户登录
  login(username, password) {
    return request.post('/auth/login', {
      username,
      password
    })
  },

  // 用户登出
  logout() {
    return request.post('/auth/logout')
  },

  // 获取当前用户信息
  getCurrentUser() {
    return request.get('/user-profile')
  },

  // 管理员加密密码 (仅管理员)
  encryptPasswords() {
    return request.post('/auth/admin/encrypt-passwords')
  }
}
