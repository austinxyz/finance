<template>
  <div class="space-y-6">
    <!-- 页头 -->
    <div class="bg-white rounded-lg shadow p-4 md:p-6">
      <h1 class="text-xl md:text-2xl font-bold text-gray-900">个人设置</h1>
      <p class="text-sm text-gray-600 mt-1">管理您的个人信息和账户设置</p>
    </div>

    <!-- Tab导航 + 内容 -->
    <div class="bg-white rounded-lg shadow">
      <!-- Tab头部 -->
      <div class="border-b border-gray-200">
        <div class="flex">
          <button
            @click="activeTab = 'profile'"
            :class="[
              'px-6 py-3 text-sm font-medium border-b-2 transition-colors',
              activeTab === 'profile'
                ? 'border-primary text-primary bg-primary/5'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            ]"
          >
            个人信息
          </button>
          <button
            @click="activeTab = 'password'"
            :class="[
              'px-6 py-3 text-sm font-medium border-b-2 transition-colors',
              activeTab === 'password'
                ? 'border-primary text-primary bg-primary/5'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            ]"
          >
            修改密码
          </button>
        </div>
      </div>

      <!-- Tab内容 -->
      <div class="p-6">
        <!-- 个人信息Tab -->
        <div v-if="activeTab === 'profile'">
          <form @submit.prevent="handleProfileUpdate">
            <!-- Grid布局 - 2列 -->
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
              <!-- 用户名 -->
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">用户名</label>
                <input
                  type="text"
                  :value="profileForm.username"
                  disabled
                  class="w-full px-3 py-2 border border-gray-300 rounded-lg bg-gray-50 text-gray-500 cursor-not-allowed text-sm"
                />
                <p class="text-xs text-gray-500 mt-0.5">用户名不可修改</p>
              </div>

              <!-- 邮箱 -->
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">邮箱 <span class="text-red-500">*</span></label>
                <input
                  v-model="profileForm.email"
                  type="email"
                  required
                  class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent text-sm"
                  placeholder="请输入邮箱"
                />
              </div>

              <!-- 姓名 -->
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">姓名</label>
                <input
                  v-model="profileForm.fullName"
                  type="text"
                  class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent text-sm"
                  placeholder="请输入姓名"
                />
              </div>

              <!-- 出生年月 -->
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">出生年月</label>
                <input
                  v-model="profileForm.birthDate"
                  type="month"
                  class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent text-sm"
                />
              </div>

              <!-- 年收入 -->
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">年收入</label>
                <div class="flex space-x-2">
                  <input
                    v-model.number="profileForm.annualIncome"
                    type="number"
                    step="0.01"
                    min="0"
                    class="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent text-sm"
                    placeholder="0.00"
                  />
                  <select
                    v-model="profileForm.incomeCurrency"
                    class="px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent text-sm"
                  >
                    <option value="USD">$ USD</option>
                    <option value="CNY">¥ CNY</option>
                    <option value="EUR">€ EUR</option>
                    <option value="GBP">£ GBP</option>
                    <option value="JPY">¥ JPY</option>
                  </select>
                </div>
              </div>

              <!-- 风险承受能力 -->
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">风险承受能力</label>
                <select
                  v-model="profileForm.riskTolerance"
                  class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent text-sm"
                >
                  <option value="">-- 请选择 --</option>
                  <option value="CONSERVATIVE">保守型</option>
                  <option value="MODERATE">稳健型</option>
                  <option value="AGGRESSIVE">进取型</option>
                </select>
              </div>

              <!-- 角色 -->
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">角色</label>
                <input
                  type="text"
                  :value="profileForm.role === 'ADMIN' ? '管理员' : '普通用户'"
                  disabled
                  class="w-full px-3 py-2 border border-gray-300 rounded-lg bg-gray-50 text-gray-500 cursor-not-allowed text-sm"
                />
              </div>

              <!-- 所属家庭 -->
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">所属家庭</label>
                <input
                  type="text"
                  :value="familyName"
                  disabled
                  class="w-full px-3 py-2 border border-gray-300 rounded-lg bg-gray-50 text-gray-500 cursor-not-allowed text-sm"
                />
              </div>
            </div>

            <!-- 备注 - 独占一行 -->
            <div class="mb-4">
              <label class="block text-sm font-medium text-gray-700 mb-1">备注</label>
              <textarea
                v-model="profileForm.notes"
                rows="2"
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent text-sm"
                placeholder="个人备注信息"
              ></textarea>
            </div>

            <!-- 成功/错误提示 -->
            <div v-if="profileError" class="mb-3 p-3 bg-red-50 border border-red-200 rounded-lg">
              <p class="text-red-600 text-sm">{{ profileError }}</p>
            </div>

            <div v-if="profileSuccess" class="mb-3 p-3 bg-green-50 border border-green-200 rounded-lg">
              <p class="text-green-600 text-sm">{{ profileSuccess }}</p>
            </div>

            <!-- 提交按钮 -->
            <div class="flex justify-end">
              <button
                type="submit"
                :disabled="profileLoading"
                class="px-6 py-2 bg-primary text-white rounded-lg hover:bg-primary/90 disabled:opacity-50 disabled:cursor-not-allowed transition text-sm font-medium"
              >
                {{ profileLoading ? '保存中...' : '保存个人信息' }}
              </button>
            </div>
          </form>
        </div>

        <!-- 修改密码Tab -->
        <div v-if="activeTab === 'password'">
          <form @submit.prevent="handlePasswordChange" class="max-w-md space-y-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">当前密码</label>
              <input
                v-model="passwordForm.currentPassword"
                type="password"
                required
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent text-sm"
                placeholder="请输入当前密码"
              />
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">新密码</label>
              <input
                v-model="passwordForm.newPassword"
                type="password"
                required
                minlength="6"
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent text-sm"
                placeholder="请输入新密码（至少6位）"
              />
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">确认新密码</label>
              <input
                v-model="passwordForm.confirmPassword"
                type="password"
                required
                minlength="6"
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent text-sm"
                placeholder="请再次输入新密码"
              />
            </div>

            <!-- 错误提示 -->
            <div v-if="error" class="p-3 bg-red-50 border border-red-200 rounded-lg">
              <p class="text-red-600 text-sm">{{ error }}</p>
            </div>

            <!-- 成功提示 -->
            <div v-if="success" class="p-3 bg-green-50 border border-green-200 rounded-lg">
              <p class="text-green-600 text-sm">{{ success }}</p>
            </div>

            <button
              type="submit"
              :disabled="loading"
              class="px-6 py-2 bg-primary text-white rounded-lg hover:bg-primary/90 disabled:opacity-50 disabled:cursor-not-allowed transition text-sm font-medium"
            >
              {{ loading ? '修改中...' : '修改密码' }}
            </button>
          </form>
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
const familyName = ref('')
const activeTab = ref('profile') // 'profile' 或 'password'

// 个人信息表单
const profileForm = ref({
  username: '',
  email: '',
  fullName: '',
  birthDate: '',
  annualIncome: null,
  incomeCurrency: 'USD',
  riskTolerance: '',
  notes: '',
  role: ''
})

const profileLoading = ref(false)
const profileError = ref('')
const profileSuccess = ref('')

// 密码修改表单
const passwordForm = ref({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const loading = ref(false)
const error = ref('')
const success = ref('')

// 加载用户信息
const loadUserInfo = async () => {
  try {
    const response = await request.get(`/users/${authStore.userId}`)
    if (response.success && response.data) {
      const user = response.data
      // 转换完整日期为年月格式 (YYYY-MM-DD -> YYYY-MM)
      const birthDate = user.birthDate ? user.birthDate.substring(0, 7) : ''

      profileForm.value = {
        username: user.username,
        email: user.email || '',
        fullName: user.fullName || '',
        birthDate: birthDate,
        annualIncome: user.annualIncome || null,
        incomeCurrency: user.incomeCurrency || 'USD',
        riskTolerance: user.riskTolerance || '',
        notes: user.notes || '',
        role: user.role || 'USER'
      }
    }
  } catch (err) {
    console.error('加载用户信息失败:', err)
    profileError.value = '加载用户信息失败'
  }
}

// 加载家庭名称
const loadFamilyName = async () => {
  try {
    const response = await familyAPI.getDefault()
    if (response.success && response.data) {
      familyName.value = response.data.familyName
    }
  } catch (err) {
    console.error('加载家庭信息失败:', err)
  }
}

// 保存个人信息
const handleProfileUpdate = async () => {
  profileError.value = ''
  profileSuccess.value = ''
  profileLoading.value = true

  try {
    // 转换年月格式为完整日期 (YYYY-MM -> YYYY-MM-01)
    const dataToSend = {
      username: profileForm.value.username, // 必须包含，但不会被修改
      email: profileForm.value.email,
      fullName: profileForm.value.fullName,
      birthDate: profileForm.value.birthDate ? `${profileForm.value.birthDate}-01` : null,
      annualIncome: profileForm.value.annualIncome,
      incomeCurrency: profileForm.value.incomeCurrency,
      riskTolerance: profileForm.value.riskTolerance || null,
      notes: profileForm.value.notes
    }

    const response = await request.put(`/users/${authStore.userId}`, dataToSend)

    if (response.success) {
      profileSuccess.value = '个人信息保存成功！'
      // 3秒后清除成功提示
      setTimeout(() => {
        profileSuccess.value = ''
      }, 3000)
    } else {
      profileError.value = response.message || '保存失败'
    }
  } catch (err) {
    console.error('保存个人信息失败:', err)
    profileError.value = err.response?.data?.message || '保存失败，请重试'
  } finally {
    profileLoading.value = false
  }
}

// 修改密码
const handlePasswordChange = async () => {
  error.value = ''
  success.value = ''

  // 验证新密码
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    error.value = '两次输入的新密码不一致'
    return
  }

  if (passwordForm.value.newPassword.length < 6) {
    error.value = '新密码至少需要6位'
    return
  }

  loading.value = true

  try {
    // 调用后端密码修改专用 API
    const response = await request.put(`/users/${authStore.userId}/password`, {
      currentPassword: passwordForm.value.currentPassword,
      newPassword: passwordForm.value.newPassword
    })

    if (response.success) {
      success.value = '密码修改成功！'
      // 清空表单
      passwordForm.value = {
        currentPassword: '',
        newPassword: '',
        confirmPassword: ''
      }

      // 3秒后清除成功提示
      setTimeout(() => {
        success.value = ''
      }, 3000)
    } else {
      error.value = response.message || '密码修改失败'
    }
  } catch (err) {
    console.error('密码修改失败:', err)
    error.value = err.response?.data?.message || '密码修改失败，请检查当前密码是否正确'
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadUserInfo()
  await loadFamilyName()
})
</script>
