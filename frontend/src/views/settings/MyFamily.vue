<template>
  <div class="min-h-screen bg-gray-50 sm:bg-transparent p-4 sm:p-6 space-y-4 sm:space-y-6">
    <!-- 页头 -->
    <div class="flex flex-col sm:flex-row items-start justify-between gap-3 bg-white sm:bg-transparent p-4 sm:p-0 rounded-lg sm:rounded-none shadow-sm sm:shadow-none">
      <div>
        <h2 class="text-2xl font-bold text-gray-900">我的家庭</h2>
        <p class="mt-1 text-sm text-gray-500">
          查看您的家庭信息
        </p>
      </div>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="flex justify-center items-center py-12">
      <div class="text-center">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-green-600 mx-auto mb-4"></div>
        <p class="text-gray-600">加载中...</p>
      </div>
    </div>

    <!-- 家庭信息 -->
    <div v-else-if="family" class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <!-- 左列：家庭基本信息 -->
      <div class="bg-white shadow rounded-lg">
        <div class="px-6 py-5 border-b border-gray-200">
          <h3 class="text-lg font-medium text-gray-900">家庭基本信息</h3>
        </div>

        <form @submit.prevent="saveFamily" class="px-6 py-5 space-y-6">
          <!-- 家庭名称 -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              家庭名称 <span class="text-red-500">*</span>
            </label>
            <input
              v-model="familyForm.familyName"
              type="text"
              required
              class="block w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-green-500 focus:border-green-500 text-sm"
              placeholder="例如：张家"
            />
          </div>

          <!-- 家庭年度支出 -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              家庭年度支出 <span class="text-red-500">*</span>
            </label>
            <div class="flex space-x-2">
              <input
                v-model.number="familyForm.annualExpenses"
                type="number"
                step="0.01"
                min="0"
                required
                class="block w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-green-500 focus:border-green-500 text-sm"
                placeholder="0.00"
              />
              <select
                v-model="familyForm.expensesCurrency"
                class="px-3 py-2 border border-gray-300 rounded-md focus:ring-green-500 focus:border-green-500 text-sm"
              >
                <option value="USD">$ USD</option>
                <option value="CNY">¥ CNY</option>
                <option value="EUR">€ EUR</option>
                <option value="GBP">£ GBP</option>
                <option value="JPY">¥ JPY</option>
              </select>
            </div>
            <p class="mt-1 text-xs text-gray-500">
              家庭整体年度支出，用于计算紧急储备金需求
            </p>
          </div>

          <!-- 紧急储备月数 -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">紧急储备月数</label>
            <input
              v-model.number="familyForm.emergencyFundMonths"
              type="number"
              min="3"
              max="24"
              class="block w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-green-500 focus:border-green-500 text-sm"
              placeholder="6"
            />
            <p class="mt-1 text-xs text-gray-500">
              建议保持 3-24 个月的家庭生活费用作为紧急储备
            </p>
          </div>

          <!-- 财务目标 -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">家庭财务目标</label>
            <textarea
              v-model="familyForm.financialGoals"
              rows="4"
              class="block w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-green-500 focus:border-green-500 text-sm"
              placeholder="例如：&#10;1. 5年内购买首套房&#10;2. 子女教育基金储备&#10;3. 退休养老规划"
            ></textarea>
            <p class="mt-1 text-xs text-gray-500">
              记录家庭的财务目标和规划
            </p>
          </div>

          <!-- 成功/错误提示 -->
          <div v-if="saveError" class="p-3 bg-red-50 border border-red-200 rounded-lg">
            <p class="text-red-600 text-sm">{{ saveError }}</p>
          </div>

          <div v-if="saveSuccess" class="p-3 bg-green-50 border border-green-200 rounded-lg">
            <p class="text-green-600 text-sm">{{ saveSuccess }}</p>
          </div>

          <!-- 操作按钮 -->
          <div class="flex justify-between items-center pt-4 border-t border-gray-200">
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

      <!-- 右列：家庭成员 -->
      <div class="bg-white shadow rounded-lg">
        <div class="px-6 py-5 border-b border-gray-200">
          <h3 class="text-lg font-medium text-gray-900">家庭成员</h3>
        </div>

        <div class="px-6 py-5">
          <div v-if="family.members && family.members.length > 0" class="space-y-3">
            <div
              v-for="member in family.members"
              :key="member.id"
              class="p-4 border border-gray-200 rounded-lg hover:border-green-300 transition-colors"
            >
              <div class="flex items-start justify-between">
                <div class="flex-1">
                  <div class="flex items-center gap-2">
                    <h4 class="font-medium text-gray-900">{{ member.fullName || member.username }}</h4>
                    <span
                      v-if="member.id === authStore.userId"
                      class="px-2 py-0.5 bg-blue-100 text-blue-700 text-xs rounded-full"
                    >
                      我
                    </span>
                    <span
                      v-if="member.role === 'ADMIN'"
                      class="px-2 py-0.5 bg-green-100 text-green-700 text-xs rounded-full"
                    >
                      管理员
                    </span>
                  </div>
                  <p class="text-sm text-gray-500 mt-1">{{ member.email }}</p>
                  <div v-if="member.annualIncome" class="text-sm text-gray-600 mt-2">
                    年收入: {{ getCurrencySymbol(member.incomeCurrency) }}{{ member.annualIncome }}
                  </div>
                  <div v-if="member.riskTolerance" class="text-sm text-gray-600 mt-1">
                    风险承受能力: {{ getRiskToleranceLabel(member.riskTolerance) }}
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div v-else class="text-center py-8 text-gray-500 text-sm">
            暂无成员信息
          </div>
        </div>
      </div>
    </div>

    <!-- 错误提示 -->
    <div v-else class="bg-red-50 border border-red-200 rounded-lg p-4">
      <div class="flex items-start">
        <svg class="w-5 h-5 text-red-400 mt-0.5" fill="currentColor" viewBox="0 0 20 20">
          <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd"/>
        </svg>
        <div class="ml-3">
          <h3 class="text-sm font-medium text-red-800">加载失败</h3>
          <p class="mt-1 text-sm text-red-700">加载家庭信息失败，请稍后重试</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { familyAPI } from '@/api/family'
import request from '@/api/request'

const authStore = useAuthStore()
const family = ref(null)
const loading = ref(false)

const familyForm = ref({
  id: null,
  familyName: '',
  annualExpenses: 0,
  expensesCurrency: 'USD',
  emergencyFundMonths: 6,
  financialGoals: ''
})

const originalFamilyForm = ref(null)
const saving = ref(false)
const saveError = ref('')
const saveSuccess = ref('')

const formatDate = (dateString) => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

const getCurrencySymbol = (currency) => {
  const symbols = {
    'USD': '$',
    'CNY': '¥',
    'EUR': '€',
    'GBP': '£',
    'JPY': '¥'
  }
  return symbols[currency] || ''
}

const getRiskToleranceLabel = (value) => {
  const map = {
    'CONSERVATIVE': '保守型',
    'MODERATE': '稳健型',
    'AGGRESSIVE': '进取型'
  }
  return map[value] || value
}

const loadFamily = async () => {
  loading.value = true
  try {
    const response = await familyAPI.getDefault()
    if (response.success && response.data) {
      family.value = response.data

      // Populate form data
      familyForm.value = {
        id: response.data.id,
        familyName: response.data.familyName || '',
        annualExpenses: response.data.annualExpenses || 0,
        expensesCurrency: response.data.expensesCurrency || 'USD',
        emergencyFundMonths: response.data.emergencyFundMonths || 6,
        financialGoals: response.data.financialGoals || ''
      }

      // Store original values for reset
      originalFamilyForm.value = JSON.parse(JSON.stringify(familyForm.value))
    }
  } catch (error) {
    console.error('加载家庭信息失败:', error)
  } finally {
    loading.value = false
  }
}

const saveFamily = async () => {
  saving.value = true
  saveError.value = ''
  saveSuccess.value = ''

  try {
    const response = await request.post(`/family/${familyForm.value.id}`, familyForm.value)

    if (response.success) {
      // Update original values
      originalFamilyForm.value = JSON.parse(JSON.stringify(familyForm.value))
      saveSuccess.value = '家庭信息保存成功！'

      // Reload family to update members list
      await loadFamily()

      // Clear success message after 3 seconds
      setTimeout(() => {
        saveSuccess.value = ''
      }, 3000)
    } else {
      saveError.value = response.message || '保存失败'
    }
  } catch (err) {
    console.error('保存家庭信息失败:', err)
    saveError.value = err.response?.data?.message || '保存失败，请重试'
  } finally {
    saving.value = false
  }
}

const resetForm = () => {
  if (originalFamilyForm.value) {
    familyForm.value = JSON.parse(JSON.stringify(originalFamilyForm.value))
    saveError.value = ''
    saveSuccess.value = ''
  }
}

onMounted(() => {
  loadFamily()
})
</script>
