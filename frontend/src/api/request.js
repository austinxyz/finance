import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000  // 30 seconds for large trend analysis queries
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    const timestamp = Date.now()

    // Add Authorization header if token exists
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    // Enhanced logging with full URL and params
    const method = config.method?.toUpperCase()
    const url = config.url
    const params = config.params || {}
    const hasToken = !!token

    // Construct full URL with query string for display
    const queryString = Object.keys(params).length > 0
      ? '?' + Object.entries(params).map(([k, v]) => `${k}=${v}`).join('&')
      : ''
    const fullUrl = `${url}${queryString}`


    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    // Handle 401 Unauthorized - redirect to login
    if (error.response?.status === 401) {
      // Clear auth data
      localStorage.removeItem('token')
      localStorage.removeItem('user')

      // Redirect to login if not already there
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }

    return Promise.reject(error)
  }
)

export default request
