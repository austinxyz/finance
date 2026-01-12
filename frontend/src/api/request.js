import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
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

    console.log(`[Request ${timestamp}] ${method} ${fullUrl}`)
    console.log(`[Request ${timestamp}] Token:`, hasToken ? 'YES' : 'NO')
    console.log(`[Request ${timestamp}] Params:`, params)
    console.log(`[Request ${timestamp}] Headers:`, { ...config.headers })

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
    console.error('请求错误:', error)

    // Handle 401 Unauthorized - redirect to login
    if (error.response?.status === 401) {
      const debugInfo = {
        url: error.config?.url,
        method: error.config?.method,
        hadToken: !!error.config?.headers?.Authorization,
        response: error.response?.data,
        timestamp: new Date().toISOString()
      }

      console.error('=== 401 Unauthorized ===')
      console.error('URL:', debugInfo.url)
      console.error('Method:', debugInfo.method)
      console.error('Had token:', debugInfo.hadToken)
      console.error('Response:', debugInfo.response)
      console.error('========================')

      // Save error to sessionStorage so we can see it after redirect
      sessionStorage.setItem('last401Error', JSON.stringify(debugInfo))

      // Clear auth data
      localStorage.removeItem('token')
      localStorage.removeItem('user')

      // Redirect to login if not already there
      if (window.location.pathname !== '/login') {
        console.warn('Redirecting to login due to 401 error')
        alert(`401错误:\nURL: ${debugInfo.url}\n有Token: ${debugInfo.hadToken}\n后端消息: ${debugInfo.response?.message || '无'}`)
        window.location.href = '/login'
      }
    }

    return Promise.reject(error)
  }
)

export default request
