<template>
  <div class="p-6 space-y-6">
    <!-- 页面标题 -->
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">用户管理</h1>
        <p class="text-sm text-gray-600 mt-1">管理系统用户账号</p>
      </div>
      <button
        @click="openCreateDialog"
        class="px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary/90 text-sm font-medium flex items-center gap-2"
      >
        <UserPlus class="w-4 h-4" />
        添加用户
      </button>
    </div>

    <!-- 用户列表 -->
    <div v-if="loading" class="flex justify-center py-12">
      <div class="text-gray-500">加载中...</div>
    </div>

    <div v-else class="bg-white rounded-lg shadow-sm border border-gray-200">
      <div class="overflow-x-auto">
        <table class="w-full">
          <thead class="bg-gray-50 border-b border-gray-200">
            <tr>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">用户名</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">姓名</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">邮箱</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">状态</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">创建时间</th>
              <th class="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr v-for="user in users" :key="user.id" class="hover:bg-gray-50">
              <td class="px-6 py-4 text-sm font-medium text-gray-900">{{ user.username }}</td>
              <td class="px-6 py-4 text-sm text-gray-600">{{ user.fullName || '-' }}</td>
              <td class="px-6 py-4 text-sm text-gray-600">{{ user.email }}</td>
              <td class="px-6 py-4 text-sm">
                <span
                  :class="[
                    'px-2 py-1 rounded-full text-xs font-medium',
                    user.isActive ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
                  ]"
                >
                  {{ user.isActive ? '活跃' : '停用' }}
                </span>
              </td>
              <td class="px-6 py-4 text-sm text-gray-600">{{ formatDate(user.createdAt) }}</td>
              <td class="px-6 py-4 text-center">
                <div class="flex justify-center gap-2">
                  <button
                    @click="editUser(user)"
                    class="p-1.5 hover:bg-gray-100 rounded"
                    title="编辑"
                  >
                    <Pencil class="w-4 h-4 text-gray-600" />
                  </button>
                  <button
                    @click="toggleUserStatus(user)"
                    class="p-1.5 hover:bg-yellow-50 rounded"
                    :title="user.isActive ? '停用' : '启用'"
                  >
                    <component :is="user.isActive ? ShieldOff : ShieldCheck" class="w-4 h-4 text-yellow-600" />
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 创建/编辑用户对话框 -->
    <div
      v-if="showCreateDialog || showEditDialog"
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50"
      @click.self="closeDialog"
    >
      <div class="bg-white rounded-lg p-6 w-full max-w-md">
        <h2 class="text-xl font-bold mb-4">
          {{ showEditDialog ? '编辑用户' : '添加用户' }}
        </h2>
        <form @submit.prevent="submitForm" class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">用户名 *</label>
            <input
              v-model="formData.username"
              type="text"
              required
              :disabled="showEditDialog"
              placeholder="请输入用户名"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary disabled:bg-gray-100"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">姓名</label>
            <input
              v-model="formData.fullName"
              type="text"
              placeholder="请输入姓名"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">邮箱 *</label>
            <input
              v-model="formData.email"
              type="email"
              required
              placeholder="请输入邮箱"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>

          <div v-if="!showEditDialog">
            <label class="block text-sm font-medium text-gray-700 mb-1">密码 *</label>
            <input
              v-model="formData.passwordHash"
              type="password"
              required
              placeholder="请输入密码"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>

          <div v-else>
            <label class="block text-sm font-medium text-gray-700 mb-1">新密码（留空则不修改）</label>
            <input
              v-model="formData.passwordHash"
              type="password"
              placeholder="请输入新密码"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>

          <div class="flex gap-3 pt-4">
            <button
              type="submit"
              class="flex-1 px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary/90 font-medium"
            >
              {{ showEditDialog ? '保存' : '创建' }}
            </button>
            <button
              type="button"
              @click="closeDialog"
              class="flex-1 px-4 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 font-medium"
            >
              取消
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { UserPlus, Pencil, ShieldOff, ShieldCheck } from 'lucide-vue-next'
import { userAPI } from '@/api'

const loading = ref(false)
const users = ref([])
const showCreateDialog = ref(false)
const showEditDialog = ref(false)
const currentEditId = ref(null)

const formData = ref({
  username: '',
  fullName: '',
  email: '',
  passwordHash: '',
  isActive: true
})

// 格式化日期
function formatDate(dateString) {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 加载用户列表
async function loadUsers() {
  loading.value = true
  try {
    const response = await userAPI.getAll()
    if (response.success) {
      users.value = response.data
    }
  } catch (error) {
    console.error('加载用户列表失败:', error)
    alert('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

// 打开创建对话框
function openCreateDialog() {
  showCreateDialog.value = true
}

// 编辑用户
function editUser(user) {
  currentEditId.value = user.id
  formData.value = {
    username: user.username,
    fullName: user.fullName || '',
    email: user.email,
    passwordHash: '',
    isActive: user.isActive
  }
  showEditDialog.value = true
}

// 切换用户状态
async function toggleUserStatus(user) {
  const action = user.isActive ? '停用' : '启用'
  if (!confirm(`确定要${action}用户"${user.username}"吗？`)) return

  try {
    const updatedUser = {
      ...user,
      isActive: !user.isActive
    }
    const response = await userAPI.update(user.id, updatedUser)
    if (response.success) {
      await loadUsers()
    }
  } catch (error) {
    console.error('更新用户状态失败:', error)
    alert('更新用户状态失败')
  }
}

// 提交表单
async function submitForm() {
  try {
    let response
    if (showEditDialog.value) {
      response = await userAPI.update(currentEditId.value, formData.value)
    } else {
      response = await userAPI.create(formData.value)
    }

    if (response.success) {
      closeDialog()
      await loadUsers()
    }
  } catch (error) {
    console.error('提交失败:', error)
    alert(error.response?.data?.message || '操作失败，请重试')
  }
}

// 关闭对话框
function closeDialog() {
  showCreateDialog.value = false
  showEditDialog.value = false
  currentEditId.value = null
  formData.value = {
    username: '',
    fullName: '',
    email: '',
    passwordHash: '',
    isActive: true
  }
}

onMounted(() => {
  loadUsers()
})
</script>
