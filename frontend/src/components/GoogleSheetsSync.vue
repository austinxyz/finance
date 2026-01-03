<template>
  <div v-if="show" class="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
    <div class="bg-white rounded-lg shadow-xl max-w-md w-full mx-4">
      <!-- 标题栏 -->
      <div class="flex items-center justify-between p-4 border-b border-gray-200">
        <h3 class="text-lg font-semibold text-gray-900">同步到Google Sheets</h3>
        <button @click="close" class="text-gray-400 hover:text-gray-600">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="width" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
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

        <!-- 加载状态 -->
        <div v-else-if="syncStatus === 'loading'" class="py-8">
          <div class="flex flex-col items-center justify-center">
            <div class="inline-block animate-spin rounded-full h-10 w-10 border-b-2 border-primary"></div>
            <p class="text-gray-600 mt-4 text-sm font-medium">正在同步到Google Sheets...</p>
            <p class="text-gray-500 mt-2 text-xs">正在创建电子表格和导出数据，请稍候（可能需要30-60秒）</p>
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
                  将创建一个新的Google Sheets电子表格，包含资产负债表、开支表、投资账户明细等5个工作表。
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部按钮 -->
      <div class="flex justify-end gap-3 p-4 border-t border-gray-200">
        <button @click="close"
                class="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50">
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
import { ref, watch } from 'vue'
import googleSheetsApi from '../api/googleSheets'

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  },
  familyId: {
    type: Number,
    required: true
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
const shareUrl = ref('')
const errorMessage = ref('')

// 监听显示状态，重置表单
watch(() => props.show, (newVal) => {
  if (newVal) {
    resetForm()
  }
})

// 重置表单
const resetForm = () => {
  year.value = props.defaultYear
  permission.value = 'reader'
  syncStatus.value = 'idle'
  shareUrl.value = ''
  errorMessage.value = ''
}

// 关闭弹窗
const close = () => {
  emit('close')
}

// 同步到Google Sheets
const syncToGoogleSheets = async () => {
  try {
    syncStatus.value = 'loading'

    const response = await googleSheetsApi.syncAnnualReport({
      familyId: props.familyId,
      year: year.value,
      permission: permission.value
    })

    shareUrl.value = response.data.shareUrl
    syncStatus.value = 'success'
    emit('success', response.data)
  } catch (error) {
    console.error('同步失败:', error)
    syncStatus.value = 'error'
    errorMessage.value = error.response?.data?.error || error.message || '未知错误'
  }
}

// 重置并重试
const resetAndRetry = () => {
  syncStatus.value = 'idle'
  errorMessage.value = ''
}
</script>

<style scoped>
.form-radio:checked {
  background-color: currentColor;
  border-color: transparent;
}
</style>
