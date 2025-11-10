<template>
  <div class="p-6 space-y-6">
    <!-- 页面标题 -->
    <div>
      <h1 class="text-2xl font-bold text-gray-900">负债账户</h1>
      <p class="text-sm text-gray-600 mt-1">按分类管理您的所有负债账户</p>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="flex justify-center py-12">
      <div class="text-gray-500">加载中...</div>
    </div>

    <!-- 分类面板 - 两列布局 -->
    <div v-else class="grid grid-cols-1 lg:grid-cols-2 gap-4">
      <div
        v-for="type in categoryTypes"
        :key="type"
        class="bg-white rounded-lg shadow-sm border border-gray-200"
      >
        <!-- 面板头部 -->
        <div class="px-4 py-3 border-b border-gray-200 flex justify-between items-center">
          <div>
            <h2 class="text-base font-semibold text-gray-900">{{ getTypeLabel(type) }}</h2>
            <p class="text-xs text-gray-500 mt-0.5">
              {{ getAccountCountByType(type) }} 个账户 · ${{ formatAmount(getTotalByTypeInUSD(type)) }}
            </p>
          </div>
          <button
            @click="openCreateDialog(type)"
            class="px-3 py-1.5 bg-red-600 text-white rounded hover:bg-red-700 text-xs font-medium flex items-center gap-1"
          >
            <Plus class="w-3.5 h-3.5" />
            添加
          </button>
        </div>

        <!-- 账户列表 -->
        <div class="p-3">
          <div v-if="getAccountsByType(type).length === 0" class="text-center py-6 text-gray-500 text-sm">
            暂无账户
          </div>
          <div v-else class="space-y-1.5">
            <div
              v-for="account in getAccountsByType(type)"
              :key="account.id"
              class="flex items-center justify-between px-3 py-2 hover:bg-gray-50 rounded cursor-pointer group"
              @click="viewAccountDetails(account)"
            >
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-2">
                  <h3 class="font-medium text-gray-900 text-sm truncate">{{ account.accountName }}</h3>
                  <span v-if="account.userName" class="text-xs text-gray-500 shrink-0">{{ account.userName }}</span>
                </div>
                <div class="flex items-center gap-2 mt-0.5">
                  <span class="text-xs text-gray-500">{{ account.categoryName }}</span>
                  <span v-if="account.institution" class="text-xs text-gray-400">· {{ account.institution }}</span>
                  <span v-if="account.interestRate" class="text-xs text-gray-400">· {{ account.interestRate }}%</span>
                </div>
              </div>
              <div class="flex items-center gap-2 ml-2">
                <div class="text-right">
                  <div class="text-sm font-semibold text-red-600">
                    {{ getCurrencySymbol(account.currency) }}{{ formatAmount(account.latestBalance) }}
                  </div>
                  <div v-if="account.monthlyPayment" class="text-xs text-gray-500">
                    月还: {{ getCurrencySymbol(account.currency) }}{{ formatAmount(account.monthlyPayment) }}
                  </div>
                  <div v-else-if="account.accountNumber" class="text-xs text-gray-400">
                    {{ maskAccountNumber(account.accountNumber) }}
                  </div>
                </div>
                <div class="flex gap-0.5 opacity-0 group-hover:opacity-100 transition-opacity">
                  <button
                    @click.stop="editAccount(account)"
                    class="p-1 hover:bg-gray-200 rounded"
                    title="编辑"
                  >
                    <Pencil class="w-3.5 h-3.5 text-gray-600" />
                  </button>
                  <button
                    @click.stop="deleteAccount(account)"
                    class="p-1 hover:bg-red-100 rounded"
                    title="删除"
                  >
                    <Trash2 class="w-3.5 h-3.5 text-red-600" />
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 创建/编辑账户对话框 -->
    <div
      v-if="showCreateDialog || showEditDialog"
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50"
      @click.self="closeDialog"
    >
      <div class="bg-white rounded-lg p-6 w-full max-w-md">
        <h2 class="text-xl font-bold mb-4">
          {{ showEditDialog ? '编辑账户' : `添加${getTypeLabel(currentType)}账户` }}
        </h2>
        <form @submit.prevent="submitForm" class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">所有者</label>
            <select
              v-model="formData.userId"
              required
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-red-500"
            >
              <option value="">请选择用户</option>
              <option v-for="user in users" :key="user.id" :value="user.id">
                {{ user.fullName || user.username }}
              </option>
            </select>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">分类</label>
            <select
              v-model="formData.categoryId"
              required
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-red-500"
            >
              <option value="">请选择分类</option>
              <option v-for="cat in filteredCategoriesForForm" :key="cat.id" :value="cat.id">
                {{ cat.icon }} {{ cat.name }}
              </option>
            </select>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">账户名称</label>
            <input
              v-model="formData.accountName"
              type="text"
              required
              placeholder="例如:招商银行信用卡"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-red-500"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">机构名称</label>
            <input
              v-model="formData.institution"
              type="text"
              placeholder="例如：招商银行"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-red-500"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">账号</label>
            <input
              v-model="formData.accountNumber"
              type="text"
              placeholder="例如：6222021234567890"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-red-500"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">利率 (%)</label>
            <input
              v-model.number="formData.interestRate"
              type="number"
              step="0.01"
              placeholder="例如：4.35"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-red-500"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">月还款额</label>
            <input
              v-model.number="formData.monthlyPayment"
              type="number"
              step="0.01"
              placeholder="例如：5000"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-red-500"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">币种</label>
            <select
              v-model="formData.currency"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-red-500"
            >
              <option value="CNY">人民币 (CNY)</option>
              <option value="USD">美元 (USD)</option>
              <option value="EUR">欧元 (EUR)</option>
              <option value="HKD">港币 (HKD)</option>
            </select>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">备注</label>
            <textarea
              v-model="formData.notes"
              rows="3"
              placeholder="添加备注信息"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-red-500"
            ></textarea>
          </div>

          <div class="flex gap-3 pt-4">
            <button
              type="submit"
              class="flex-1 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 font-medium"
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
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Pencil, Trash2 } from 'lucide-vue-next'
import { liabilityCategoryAPI, liabilityAccountAPI, userAPI } from '@/api'

const router = useRouter()
const userId = ref(1) // TODO: 从用户登录状态获取
const loading = ref(false)
const categoryTypes = ref([])
const categories = ref([])
const accounts = ref([])
const users = ref([])
const showCreateDialog = ref(false)
const showEditDialog = ref(false)
const currentEditId = ref(null)
const currentType = ref(null)

const formData = ref({
  userId: '',
  categoryId: '',
  accountName: '',
  institution: '',
  accountNumber: '',
  interestRate: null,
  monthlyPayment: null,
  currency: 'CNY',
  notes: ''
})

// 大分类类型标签映射
const typeLabels = {
  'MORTGAGE': '房贷',
  'AUTO_LOAN': '车贷',
  'CREDIT_CARD': '信用卡',
  'PERSONAL_LOAN': '个人借债',
  'STUDENT_LOAN': '学生贷款',
  'OTHER': '其他'
}

// 过滤后的分类列表（用于表单中的下拉框）
const filteredCategoriesForForm = computed(() => {
  if (!currentType.value) return categories.value
  return categories.value.filter(cat => cat.type === currentType.value)
})

// 根据大分类获取账户列表
function getAccountsByType(type) {
  return accounts.value.filter(acc => acc.categoryType === type)
}

// 根据大分类获取账户数量
function getAccountCountByType(type) {
  return getAccountsByType(type).length
}

// 根据大分类获取总额（USD基准货币）
function getTotalByType(type) {
  return getAccountsByType(type).reduce((sum, acc) => sum + (acc.latestBalanceInBaseCurrency || 0), 0)
}

// 根据大分类获取总额（USD）- 现在基准货币就是USD，直接返回
function getTotalByTypeInUSD(type) {
  return getTotalByType(type)
}

// 格式化金额
function formatAmount(amount) {
  if (!amount && amount !== 0) return '0.00'
  return Number(amount).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

// 隐藏账号中间部分
function maskAccountNumber(number) {
  if (!number || number.length < 8) return number
  const start = number.slice(0, 4)
  const end = number.slice(-4)
  return `${start}****${end}`
}

// 获取大类别标签
function getTypeLabel(type) {
  return typeLabels[type] || type
}

// 获取货币符号
function getCurrencySymbol(currency) {
  const currencyMap = {
    'CNY': '¥',
    'USD': '$',
    'EUR': '€',
    'GBP': '£',
    'JPY': '¥',
    'HKD': 'HK$',
    'AUD': 'A$',
    'CAD': 'C$',
    'SGD': 'S$',
    'KRW': '₩'
  }
  return currencyMap[currency] || currency + ' '
}

// 加载大分类类型
async function loadCategoryTypes() {
  try {
    const response = await liabilityCategoryAPI.getTypes(userId.value)
    if (response.success) {
      categoryTypes.value = response.data
    }
  } catch (error) {
    console.error('加载分类类型失败:', error)
  }
}

// 加载用户列表
async function loadUsers() {
  try {
    const response = await userAPI.getAll()
    if (response.success) {
      users.value = response.data.filter(u => u.isActive) // 只显示活跃用户
    }
  } catch (error) {
    console.error('加载用户列表失败:', error)
  }
}

// 加载分类
async function loadCategories() {
  try {
    const response = await liabilityCategoryAPI.getAll(userId.value)
    if (response.success) {
      categories.value = response.data
    }
  } catch (error) {
    console.error('加载分类失败:', error)
  }
}

// 加载账户
async function loadAccounts() {
  loading.value = true
  try {
    const response = await liabilityAccountAPI.getAll()
    if (response.success) {
      accounts.value = response.data
    }
  } catch (error) {
    console.error('加载账户失败:', error)
  } finally {
    loading.value = false
  }
}

// 打开创建对话框
function openCreateDialog(type) {
  currentType.value = type
  showCreateDialog.value = true
}

// 查看账户详情
function viewAccountDetails(account) {
  router.push(`/liabilities/accounts/${account.id}`)
}

// 编辑账户
function editAccount(account) {
  currentEditId.value = account.id
  currentType.value = account.categoryType
  formData.value = {
    userId: account.userId,
    categoryId: account.categoryId,
    accountName: account.accountName,
    institution: account.institution || '',
    accountNumber: account.accountNumber || '',
    interestRate: account.interestRate || null,
    monthlyPayment: account.monthlyPayment || null,
    currency: account.currency || 'CNY',
    notes: account.notes || ''
  }
  showEditDialog.value = true
}

// 删除账户
async function deleteAccount(account) {
  if (!confirm(`确定要删除账户"${account.accountName}"吗？\n\n提示：如果账户有负债记录，将标记为不活跃；否则将永久删除。`)) return

  try {
    const response = await liabilityAccountAPI.delete(account.id)
    if (response.success) {
      await loadAccounts()
    }
  } catch (error) {
    console.error('删除账户失败:', error)
    alert('删除失败，请重试')
  }
}

// 提交表单
async function submitForm() {
  try {
    const data = {
      ...formData.value,
      isActive: true
    }

    let response
    if (showEditDialog.value) {
      response = await liabilityAccountAPI.update(currentEditId.value, data)
    } else {
      response = await liabilityAccountAPI.create(data)
    }

    if (response.success) {
      closeDialog()
      await loadAccounts()
    }
  } catch (error) {
    console.error('提交失败:', error)
    alert('操作失败，请重试')
  }
}

// 关闭对话框
function closeDialog() {
  showCreateDialog.value = false
  showEditDialog.value = false
  currentEditId.value = null
  currentType.value = null
  formData.value = {
    userId: '',
    categoryId: '',
    accountName: '',
    institution: '',
    accountNumber: '',
    interestRate: null,
    monthlyPayment: null,
    currency: 'CNY',
    notes: ''
  }
}

onMounted(async () => {
  await loadUsers()
  await loadCategoryTypes()
  await loadCategories()
  await loadAccounts()
})
</script>
