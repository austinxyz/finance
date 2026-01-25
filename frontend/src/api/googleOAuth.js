import request from './request'

export default {
  /**
   * 检查OAuth授权状态
   */
  checkAuthStatus() {
    return request.get('/google-oauth/status')
  },

  /**
   * 获取OAuth授权URL
   */
  getAuthUrl() {
    return request.get('/google-oauth/auth-url')
  },

  /**
   * 清除授权令牌（重新授权）
   */
  revokeAuthorization() {
    return request.post('/google-oauth/revoke')
  }
}
