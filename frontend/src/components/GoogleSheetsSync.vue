<template>
  <div v-if="show" class="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
    <div class="bg-white rounded-lg shadow-xl max-w-md w-full mx-4">
      <!-- 标题栏 -->
      <div class="flex items-center justify-between p-4 border-b border-gray-200">
        <h3 class="text-lg font-semibold text-gray-900">同步到Google Sheets</h3>
        <button @click="close" class="text-gray-400 hover:text-gray-600" :disabled="syncStatus === 'loading'">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      <!-- 内容区 -->
      <div class="p-6 space-y-4">
        <!-- 成功状态 -->
        <div v-if="syncStatus === 'success'" class="space-y-4">
          <div class="flex items-center justify-center w-12 h-12 mx-auto bg-green-100 rounded-full">
            <svg class="w-6 h-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
            </svg>
          </div>
          <div class="text-center">
            <p class="text-sm text-gray-600 mb-2">报表已成功同步到Google Sheets</p>
            <a :href="shareUrl" target="_blank"
               class="inline-flex items-center px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700">
              <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
              </svg>
              打开Google Sheets
            </a>
          </div>
        </div>

        <!-- 错误状态 -->
        <div v-else-if="syncStatus === 'error'" class="space-y-4">
          <div class="flex items-center justify-center w-12 h-12 mx-auto bg-red-100 rounded-full">
            <svg class="w-6 h-6 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </div>
          <div class="text-center">
            <p class="text-sm text-red-600 mb-2">同步失败</p>
            <p class="text-xs text-gray-500">{{ errorMessage }}</p>
          </div>
        </div>

        <!-- 加载/进行中状态 -->
        <div v-else-if="syncStatus === 'loading'" class="py-8">
          <div class="flex flex-col items-center justify-center">
            <!-- 进度环 -->
            <div class="relative w-24 h-24">
              <svg class="w-24 h-24 transform -rotate-90">
                <circle cx="48" cy="48" r="40" stroke="#e5e7eb" stroke-width="8" fill="none" />
                <circle cx="48" cy="48" r="40" :stroke="progressColor" stroke-width="8" fill="none"
                        stroke-linecap="round"
                        :stroke-dasharray="circleCircumference"
                        :stroke-dashoffset="circleDashOffset"
                        class="transition-all duration-300" />
              </svg>
              <div class="absolute inset-0 flex items-center justify-center">
                <span class="text-xl font-bold text-gray-700">{{ progress }}%</span>
              </div>
            </div>
            <p class="text-gray-600 mt-4 text-sm font-medium">{{ statusMessage }}</p>
            <p class="text-gray-500 mt-2 text-xs">{{ statusDetail }}</p>
          </div>
        </div>

        <!-- 配置表单 -->
        <div v-else class="space-y-4">
          <!-- 授权状态提示 -->
          <div v-if="!checkingAuth">
            <!-- 未授权提示 -->
            <div v-if="!isAuthorized" class="bg-yellow-50 border border-yellow-200 rounded-md p-3 mb-4">
              <div class="flex items-start">
                <svg class="w-5 h-5 text-yellow-600 mt-0.5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                </svg>
                <div class="ml-3 flex-1">
                  <p class="text-sm font-medium text-yellow-800 mb-2">需要授权Google账号</p>
                  <p class="text-xs text-yellow-700 mb-3">
                    首次使用需要授权应用访问您的Google Sheets，以便创建和管理电子表格。
                  </p>
                  <button @click="authorizeGoogle"
                          class="inline-flex items-center px-3 py-1.5 text-sm font-medium text-white bg-yellow-600 rounded-md hover:bg-yellow-700">
                    <svg class="w-4 h-4 mr-1.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                    </svg>
                    立即授权
                  </button>
                </div>
              </div>
            </div>

            <!-- 已授权提示 -->
            <div v-else class="bg-green-50 border border-green-200 rounded-md p-3 mb-4">
              <div class="flex items-center">
                <svg class="w-5 h-5 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <p class="ml-2 text-sm text-green-800">已授权Google账号</p>
              </div>
            </div>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">年份</label>
            <input type="number" v-model.number="year" :min="2000" :max="2100"
                   :disabled="!isAuthorized"
                   class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary disabled:bg-gray-100 disabled:cursor-not-allowed">
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">访问权限</label>
            <div class="space-y-2">
              <label class="flex items-center">
                <input type="radio" v-model="permission" value="reader"
                       :disabled="!isAuthorized"
                       class="form-radio text-primary focus:ring-primary disabled:cursor-not-allowed">
                <span class="ml-2 text-sm text-gray-700" :class="{ 'text-gray-400': !isAuthorized }">
                  <span class="font-medium">只读</span> - 其他人只能查看，不能编辑
                </span>
              </label>
              <label class="flex items-center">
                <input type="radio" v-model="permission" value="writer"
                       :disabled="!isAuthorized"
                       class="form-radio text-primary focus:ring-primary disabled:cursor-not-allowed">
                <span class="ml-2 text-sm text-gray-700" :class="{ 'text-gray-400': !isAuthorized }">
                  <span class="font-medium">可编辑</span> - 其他人可以查看和编辑
                </span>
              </label>
            </div>
          </div>

          <div class="bg-blue-50 border border-blue-200 rounded-md p-3">
            <div class="flex">
              <svg class="w-5 h-5 text-blue-600 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <div class="ml-3">
                <p class="text-xs text-blue-800">
                  将创建一个新的Google Sheets电子表格，包含资产负债表、资产负债表明细、开支表、投资账户明细等6个工作表。
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部按钮 -->
      <div class="flex justify-end gap-3 p-4 border-t border-gray-200">
        <button @click="close"
                :disabled="syncStatus === 'loading'"
                class="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed">
          {{ syncStatus === 'success' ? '关闭' : '取消' }}
        </button>
        <button v-if="syncStatus === 'idle'"
                @click="syncToGoogleSheets"
                :disabled="!year || !isAuthorized"
                class="px-4 py-2 text-sm font-medium text-white bg-primary rounded-md hover:bg-primary/90 disabled:opacity-50 disabled:cursor-not-allowed">
          开始同步
        </button>
        <button v-if="syncStatus === 'error'"
                @click="resetAndRetry"
                class="px-4 py-2 text-sm font-medium text-white bg-primary rounded-md hover:bg-primary/90">
          重试
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed, onBeforeUnmount, onMounted } from 'vue'
import googleSheetsApi from '../api/googleSheets'
import googleOAuthApi from '../api/googleOAuth'

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  },
  familyId: {
    type: Number,
    default: null
  },
  defaultYear: {
    type: Number,
    default: () => new Date().getFullYear()
  }
})

const emit = defineEmits(['close', 'success'])

// 状态管理
const year = ref(props.defaultYear)
const permission = ref('reader')
const syncStatus = ref('idle') // idle, loading, success, error
const progress = ref(0)
const syncId = ref(null)
const shareUrl = ref('')
const errorMessage = ref('')
const statusMessage = ref('正在同步到Google Sheets...')
const statusDetail = ref('正在启动任务，请稍候...')
const isAuthorized = ref(false)
const checkingAuth = ref(false)

// SSE连接
let eventSource = null

// 进度环计算
const circleCircumference = 2 * Math.PI * 40 // r=40
const circleDashOffset = computed(() => {
  return circleCircumference * (1 - progress.value / 100)
})

// 进度颜色
const progressColor = computed(() => {
  if (progress.value < 30) return '#3b82f6' // blue
  if (progress.value < 70) return '#8b5cf6' // purple
  return '#10b981' // green
})

// 监听显示状态，重置表单并检查授权
watch(() => props.show, (newVal) => {
  if (newVal) {
    resetForm()
    checkAuthorizationStatus()
  } else {
    closeEventSource()
  }
})

// 检查授权状态
const checkAuthorizationStatus = async () => {
  try {
    checkingAuth.value = true
    const response = await googleOAuthApi.checkAuthStatus()
    const data = response.data || response
    isAuthorized.value = data.authorized || false
  } catch (error) {
    console.error('检查授权状态失败:', error)
    isAuthorized.value = false
  } finally {
    checkingAuth.value = false
  }
}

// 授权Google账号
const authorizeGoogle = async () => {
  try {
    const response = await googleOAuthApi.getAuthUrl()
    const data = response.data || response
    const authUrl = data.authUrl

    // 在新窗口打开授权页面
    const width = 600
    const height = 700
    const left = (window.screen.width - width) / 2
    const top = (window.screen.height - height) / 2
    const authWindow = window.open(
      authUrl,
      'Google授权',
      `width=${width},height=${height},left=${left},top=${top},toolbar=no,menubar=no`
    )

    // 监听授权窗口关闭
    const checkWindowClosed = setInterval(() => {
      if (authWindow && authWindow.closed) {
        clearInterval(checkWindowClosed)
        // 重新检查授权状态
        setTimeout(() => {
          checkAuthorizationStatus()
        }, 1000)
      }
    }, 500)
  } catch (error) {
    console.error('获取授权URL失败:', error)
    syncStatus.value = 'error'
    errorMessage.value = error.response?.data?.error || error.message || '获取授权URL失败'
  }
}

// 重置表单
const resetForm = () => {
  year.value = props.defaultYear
  permission.value = 'reader'
  syncStatus.value = 'idle'
  progress.value = 0
  syncId.value = null
  shareUrl.value = ''
  errorMessage.value = ''
  statusMessage.value = '正在同步到Google Sheets...'
  statusDetail.value = '正在启动任务，请稍候...'
  closeEventSource()
}

// 关闭弹窗
const close = () => {
  if (syncStatus.value !== 'loading') {
    emit('close')
  }
}

// 更新状态消息
const updateStatusMessage = (progressValue) => {
  if (progressValue <= 10) {
    statusMessage.value = '正在创建电子表格...'
    statusDetail.value = '初始化Google Sheets文档'
  } else if (progressValue <= 25) {
    statusMessage.value = '正在导出资产负债表...'
    statusDetail.value = '生成资产和负债汇总数据'
  } else if (progressValue <= 35) {
    statusMessage.value = '正在导出资产负债表明细...'
    statusDetail.value = '按用户和货币分组显示账户详情'
  } else if (progressValue <= 50) {
    statusMessage.value = '正在导出USD开支表...'
    statusDetail.value = '生成USD币种的支出预算对比'
  } else if (progressValue <= 65) {
    statusMessage.value = '正在导出CNY开支表...'
    statusDetail.value = '生成CNY币种的支出预算对比'
  } else if (progressValue <= 80) {
    statusMessage.value = '正在导出投资账户明细...'
    statusDetail.value = '生成投资账户时间序列数据'
  } else if (progressValue <= 90) {
    statusMessage.value = '正在导出退休账户明细...'
    statusDetail.value = '生成退休基金账户数据'
  } else {
    statusMessage.value = '正在完成...'
    statusDetail.value = '设置权限和生成分享链接'
  }
}

// 同步到Google Sheets
const syncToGoogleSheets = async () => {
  // 先检查是否已授权
  if (!isAuthorized.value) {
    syncStatus.value = 'error'
    errorMessage.value = '请先授权Google账号'
    return
  }

  try {
    syncStatus.value = 'loading'
    progress.value = 0
    updateStatusMessage(0)

    // 启动异步任务
    const response = await googleSheetsApi.syncAnnualReport({
      familyId: props.familyId,
      year: year.value,
      permission: permission.value
    })

    // 检查响应结构（后端通过ApiResponse包装，前端拦截器已解包）
    const data = response.data || response

    if (data.status === 'PENDING' || data.status === 'IN_PROGRESS') {
      syncId.value = data.syncId
      progress.value = data.progress || 0
      // 建立SSE连接接收实时进度
      connectEventSource(syncId.value)
    } else {
      // 理论上不应该走到这里，因为现在都是异步的
      syncStatus.value = 'error'
      errorMessage.value = '未知的任务状态: ' + (data.status || 'undefined')
    }
  } catch (error) {
    console.error('同步失败:', error)
    syncStatus.value = 'error'
    errorMessage.value = error.response?.data?.error || error.message || '未知错误'
    closeEventSource()
  }
}

// 建立SSE连接
const connectEventSource = (taskSyncId) => {
  closeEventSource() // 先关闭已有的连接

  const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
  // 从 localStorage 获取 token 并添加到 URL（EventSource 不支持自定义 headers）
  const token = localStorage.getItem('token')
  const url = `${baseURL}/google-sheets/sync-progress/${taskSyncId}${token ? `?token=${encodeURIComponent(token)}` : ''}`

  eventSource = new EventSource(url)

  // 连接成功
  eventSource.addEventListener('connected', (event) => {
    const data = JSON.parse(event.data)
    progress.value = data.progress || 0
    updateStatusMessage(progress.value)
  })

  // 进度更新
  eventSource.addEventListener('progress', (event) => {
    const data = JSON.parse(event.data)
    progress.value = data.progress || 0
    statusMessage.value = data.message || getStatusMessage(progress.value)
    updateStatusMessage(progress.value)
  })

  // 任务完成
  eventSource.addEventListener('complete', (event) => {
    const data = JSON.parse(event.data)
    syncStatus.value = 'success'
    shareUrl.value = data.shareUrl
    progress.value = 100
    emit('success', data)
    closeEventSource()
  })

  // 任务失败
  eventSource.addEventListener('error', (event) => {
    // 检查是否是自定义错误事件
    if (event.data) {
      try {
        const data = JSON.parse(event.data)
        syncStatus.value = 'error'
        errorMessage.value = data.errorMessage || data.error || '任务执行失败'
      } catch (e) {
        console.error('解析错误消息失败:', e)
      }
    } else {
      // EventSource连接错误
      syncStatus.value = 'error'
      errorMessage.value = 'SSE连接中断'
    }
    closeEventSource()
  })

  // 通用错误处理
  eventSource.onerror = (error) => {
    // 检查 eventSource 是否仍然存在（可能已被 closeEventSource() 设置为 null）
    if (eventSource && eventSource.readyState === EventSource.CLOSED) {
      // 连接已关闭，静默处理
    }
  }
}

// 关闭SSE连接
const closeEventSource = () => {
  if (eventSource) {
    eventSource.close()
    eventSource = null
  }
}

// 辅助函数：根据进度获取状态消息（作为fallback）
const getStatusMessage = (progressValue) => {
  if (progressValue <= 10) return '正在创建电子表格...'
  if (progressValue <= 25) return '正在导出资产负债表...'
  if (progressValue <= 35) return '正在导出资产负债表明细...'
  if (progressValue <= 50) return '正在导出USD开支表...'
  if (progressValue <= 65) return '正在导出CNY开支表...'
  if (progressValue <= 80) return '正在导出投资账户明细...'
  if (progressValue <= 90) return '正在导出退休账户明细...'
  return '正在完成...'
}

// 重置并重试
const resetAndRetry = () => {
  syncStatus.value = 'idle'
  errorMessage.value = ''
  progress.value = 0
  syncId.value = null
  closeEventSource()
}

// 组件卸载前清理
onBeforeUnmount(() => {
  closeEventSource()
})
</script>

<style scoped>
.form-radio:checked {
  background-color: currentColor;
  border-color: transparent;
}
</style>
