<template>
  <div v-if="isVisible" class="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
    <div class="bg-white rounded-lg shadow-xl max-w-md w-full mx-4 p-6">
      <!-- 警告图标 -->
      <div class="flex items-center justify-center w-12 h-12 mx-auto bg-red-100 rounded-full mb-4">
        <svg class="w-6 h-6 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
        </svg>
      </div>

      <!-- 标题 -->
      <h3 class="text-lg font-bold text-center text-gray-900 mb-2">
        ⚠️ 受保护数据删除警告
      </h3>

      <!-- 消息内容 -->
      <div class="mb-4 text-sm text-gray-600 text-center">
        <p class="mb-2">{{ message }}</p>
        <p class="text-red-600 font-medium">此操作将永久删除数据，无法撤销！</p>
      </div>

      <!-- 确认输入框 -->
      <div class="mb-4">
        <label class="block text-sm font-medium text-gray-700 mb-2">
          请输入 <span class="font-bold text-red-600">DELETE</span> 以确认删除：
        </label>
        <input
          v-model="confirmText"
          type="text"
          class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-red-500"
          placeholder="输入 DELETE"
          @keyup.enter="handleConfirm"
        />
      </div>

      <!-- 备份提醒 -->
      <div class="mb-4 p-3 bg-blue-50 border border-blue-200 rounded-md">
        <p class="text-xs text-blue-800">
          💡 <strong>提示：</strong>系统会自动备份数据库。如需恢复，请联系管理员。
        </p>
      </div>

      <!-- 操作按钮 -->
      <div class="flex gap-3">
        <button
          @click="handleCancel"
          class="flex-1 px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50 transition-colors"
        >
          取消
        </button>
        <button
          @click="handleConfirm"
          :disabled="confirmText !== 'DELETE'"
          :class="[
            'flex-1 px-4 py-2 rounded-md text-white transition-colors',
            confirmText === 'DELETE'
              ? 'bg-red-600 hover:bg-red-700'
              : 'bg-gray-300 cursor-not-allowed'
          ]"
        >
          确认删除
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  message: {
    type: String,
    default: '您正在尝试删除受保护家庭的数据'
  }
})

const emit = defineEmits(['confirm', 'cancel'])

const isVisible = ref(false)
const confirmText = ref('')

const show = () => {
  isVisible.value = true
  confirmText.value = ''
}

const hide = () => {
  isVisible.value = false
  confirmText.value = ''
}

const handleConfirm = () => {
  if (confirmText.value === 'DELETE') {
    emit('confirm')
    hide()
  }
}

const handleCancel = () => {
  emit('cancel')
  hide()
}

defineExpose({
  show,
  hide
})
</script>

<style scoped>
/* 对话框动画可以在这里添加 */
</style>
