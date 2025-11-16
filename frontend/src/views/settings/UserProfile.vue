<template>
  <div class="space-y-6">
    <!-- Header -->
    <div>
      <h2 class="text-2xl font-bold text-gray-900">用户配置</h2>
      <p class="mt-1 text-sm text-gray-500">
        配置您的财务参数，用于优化理财建议和风险评估
      </p>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="flex justify-center items-center py-12">
      <div class="text-center">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-green-600 mx-auto mb-4"></div>
        <p class="text-gray-600">加载中...</p>
      </div>
    </div>

    <!-- Error State -->
    <div v-if="error && !loading" class="bg-red-50 border border-red-200 rounded-lg p-4">
      <div class="flex items-start">
        <svg class="w-5 h-5 text-red-400 mt-0.5" fill="currentColor" viewBox="0 0 20 20">
          <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd"/>
        </svg>
        <div class="ml-3">
          <h3 class="text-sm font-medium text-red-800">加载失败</h3>
          <p class="mt-1 text-sm text-red-700">{{ error }}</p>
          <button @click="loadProfile" class="mt-2 text-sm font-medium text-red-600 hover:text-red-500">
            重试
          </button>
        </div>
      </div>
    </div>

    <!-- Form -->
    <div v-if="!loading" class="bg-white shadow rounded-lg">
      <div class="px-6 py-5 border-b border-gray-200">
        <h3 class="text-lg font-medium text-gray-900">财务配置</h3>
      </div>

      <form @submit.prevent="saveProfile" class="px-6 py-5 space-y-6">
        <!-- 预估年度支出 -->
        <div>
          <label for="annualExpenses" class="block text-sm font-medium text-gray-700 mb-1">
            预估年度支出 <span class="text-red-500">*</span>
          </label>
          <div class="relative">
            <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
              <span class="text-gray-500 sm:text-sm">¥</span>
            </div>
            <input
              id="annualExpenses"
              v-model.number="form.estimatedAnnualExpenses"
              type="number"
              step="0.01"
              min="0"
              required
              class="block w-full pl-8 pr-12 py-2 border border-gray-300 rounded-md focus:ring-green-500 focus:border-green-500"
              placeholder="0.00"
            />
          </div>
          <p class="mt-1 text-xs text-gray-500">
            用于计算建议现金储备（年度支出 + 50,000 紧急备用金）
          </p>
        </div>

        <!-- 紧急储备月数 -->
        <div>
          <label for="emergencyMonths" class="block text-sm font-medium text-gray-700 mb-1">
            紧急储备月数
          </label>
          <input
            id="emergencyMonths"
            v-model.number="form.emergencyFundMonths"
            type="number"
            min="3"
            max="12"
            class="block w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-green-500 focus:border-green-500"
            placeholder="6"
          />
          <p class="mt-1 text-xs text-gray-500">
            建议保持 3-12 个月的生活费用作为紧急储备
          </p>
        </div>

        <!-- 风险承受能力 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">
            风险承受能力
          </label>
          <div class="space-y-2">
            <label class="flex items-start p-3 border rounded-lg cursor-pointer hover:bg-gray-50 transition"
                   :class="form.riskTolerance === 'CONSERVATIVE' ? 'border-green-500 bg-green-50' : 'border-gray-200'">
              <input
                type="radio"
                v-model="form.riskTolerance"
                value="CONSERVATIVE"
                class="mt-1 h-4 w-4 text-green-600 focus:ring-green-500"
              />
              <div class="ml-3">
                <div class="text-sm font-medium text-gray-900">保守型</div>
                <div class="text-xs text-gray-500">偏好低风险投资，注重资本保全</div>
              </div>
            </label>

            <label class="flex items-start p-3 border rounded-lg cursor-pointer hover:bg-gray-50 transition"
                   :class="form.riskTolerance === 'MODERATE' ? 'border-green-500 bg-green-50' : 'border-gray-200'">
              <input
                type="radio"
                v-model="form.riskTolerance"
                value="MODERATE"
                class="mt-1 h-4 w-4 text-green-600 focus:ring-green-500"
              />
              <div class="ml-3">
                <div class="text-sm font-medium text-gray-900">稳健型</div>
                <div class="text-xs text-gray-500">平衡风险与收益，适度承受波动</div>
              </div>
            </label>

            <label class="flex items-start p-3 border rounded-lg cursor-pointer hover:bg-gray-50 transition"
                   :class="form.riskTolerance === 'AGGRESSIVE' ? 'border-green-500 bg-green-50' : 'border-gray-200'">
              <input
                type="radio"
                v-model="form.riskTolerance"
                value="AGGRESSIVE"
                class="mt-1 h-4 w-4 text-green-600 focus:ring-green-500"
              />
              <div class="ml-3">
                <div class="text-sm font-medium text-gray-900">进取型</div>
                <div class="text-xs text-gray-500">追求高收益，愿意承担较大风险</div>
              </div>
            </label>
          </div>
        </div>

        <!-- 备注 -->
        <div>
          <label for="notes" class="block text-sm font-medium text-gray-700 mb-1">
            备注
          </label>
          <textarea
            id="notes"
            v-model="form.notes"
            rows="3"
            class="block w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-green-500 focus:border-green-500"
            placeholder="其他财务相关信息..."
          ></textarea>
          <p class="mt-1 text-xs text-gray-500">
            可以记录一些个人财务目标、投资偏好等信息
          </p>
        </div>

        <!-- Action Buttons -->
        <div class="flex justify-end space-x-3 pt-4 border-t border-gray-200">
          <button
            type="button"
            @click="resetForm"
            class="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500"
          >
            重置
          </button>
          <button
            type="submit"
            :disabled="saving"
            class="px-4 py-2 bg-green-600 text-white rounded-md text-sm font-medium hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <span v-if="!saving">保存配置</span>
            <span v-else class="flex items-center">
              <svg class="animate-spin -ml-1 mr-2 h-4 w-4 text-white" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              保存中...
            </span>
          </button>
        </div>
      </form>
    </div>

    <!-- Success Message -->
    <div v-if="showSuccess" class="fixed bottom-4 right-4 bg-green-50 border border-green-200 rounded-lg shadow-lg p-4 max-w-sm animate-slide-up">
      <div class="flex items-start">
        <svg class="w-5 h-5 text-green-400 mt-0.5" fill="currentColor" viewBox="0 0 20 20">
          <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/>
        </svg>
        <div class="ml-3">
          <h3 class="text-sm font-medium text-green-800">保存成功</h3>
          <p class="mt-1 text-sm text-green-700">用户配置已更新</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/api/request'

const loading = ref(false)
const saving = ref(false)
const error = ref(null)
const showSuccess = ref(false)

const form = ref({
  estimatedAnnualExpenses: 0,
  emergencyFundMonths: 6,
  riskTolerance: 'MODERATE',
  notes: ''
})

const originalForm = ref(null)

// 加载用户配置
const loadProfile = async () => {
  loading.value = true
  error.value = null

  try {
    const response = await request.get('/user-profile', {
      params: { userId: 1 }
    })

    if (response.data.success && response.data.data) {
      const profile = response.data.data
      form.value = {
        estimatedAnnualExpenses: profile.estimatedAnnualExpenses || 0,
        emergencyFundMonths: profile.emergencyFundMonths || 6,
        riskTolerance: profile.riskTolerance || 'MODERATE',
        notes: profile.notes || ''
      }
      // 保存原始数据用于重置
      originalForm.value = JSON.parse(JSON.stringify(form.value))
    }
  } catch (err) {
    console.error('加载用户配置失败:', err)
    error.value = err.response?.data?.message || '加载用户配置失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

// 保存用户配置
const saveProfile = async () => {
  saving.value = true
  error.value = null

  try {
    const response = await request.post('/user-profile', form.value, {
      params: { userId: 1 }
    })

    if (response.data.success) {
      // 更新原始数据
      originalForm.value = JSON.parse(JSON.stringify(form.value))

      // 显示成功消息
      showSuccess.value = true
      setTimeout(() => {
        showSuccess.value = false
      }, 3000)
    }
  } catch (err) {
    console.error('保存用户配置失败:', err)
    error.value = err.response?.data?.message || '保存失败，请稍后重试'
  } finally {
    saving.value = false
  }
}

// 重置表单
const resetForm = () => {
  if (originalForm.value) {
    form.value = JSON.parse(JSON.stringify(originalForm.value))
  }
}

onMounted(() => {
  loadProfile()
})
</script>

<style scoped>
@keyframes slide-up {
  from {
    transform: translateY(100%);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.animate-slide-up {
  animation: slide-up 0.3s ease-out;
}
</style>
