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
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">年份</label>
            <input type="number" v-model.number="year" :min="2000" :max="2100"
                   class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary">
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">访问权限</label>
            <div class="space-y-2">
              <label class="flex items-center">
                <input type="radio" v-model="permission" value="reader"
                       class="form-radio text-primary focus:ring-primary">
                <span class="ml-2 text-sm text-gray-700">
                  <span class="font-medium">只读</span> - 其他人只能查看，不能编辑
                </span>
              </label>
              <label class="flex items-center">
                <input type="radio" v-model="permission" value="writer"
                       class="form-radio text-primary focus:ring-primary">
                <span class="ml-2 text-sm text-gray-700">
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
                :disabled="!year"
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
import { ref, watch, computed, onBeforeUnmount } from 'vue'
import googleSheetsApi from '../api/googleSheets'

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

// 监听显示状态，重置表单
watch(() => props.show, (newVal) => {
  if (newVal) {
    resetForm()
  } else {
    closeEventSource()
  }
})

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
  const url = `${baseURL}/google-sheets/sync-progress/${taskSyncId}`

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
    if (eventSource.readyState === EventSource.CLOSED) {
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
