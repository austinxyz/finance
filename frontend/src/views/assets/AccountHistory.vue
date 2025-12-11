<template>
  <div class="p-6 space-y-6">
    <!-- 页面标题 -->
    <div>
      <h1 class="text-2xl font-bold text-gray-900">资产账户与记录</h1>
      <p class="text-sm text-gray-600 mt-1">管理账户，按资产类型查看历史记录和趋势分析</p>
    </div>

    <!-- 资产分类 Tab -->
    <div class="border-b border-gray-200">
      <nav class="-mb-px flex space-x-4 overflow-x-auto" aria-label="Tabs">
        <button
          v-for="category in categories"
          :key="category.type"
          @click="selectedCategoryType = category.type"
          :class="[
            'whitespace-nowrap py-3 px-4 border-b-2 font-medium text-sm transition-colors',
            selectedCategoryType === category.type
              ? 'border-primary text-primary'
              : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
          ]"
        >
          {{ category.name }}
        </button>
      </nav>
    </div>

    <!-- 三列布局：账户列表 + 趋势图 + 历史记录 -->
    <div class="grid grid-cols-12 gap-6">
      <!-- 左侧：账户列表 -->
      <div class="col-span-12 lg:col-span-3">
        <div class="bg-white rounded-lg shadow border border-gray-200">
          <div class="px-4 py-3 border-b border-gray-200 flex items-center justify-between">
            <h2 class="text-sm font-semibold text-gray-900">账户列表</h2>
            <button
              @click="openAccountDialog()"
              class="text-primary hover:text-primary/80 text-xs font-medium flex items-center gap-1"
            >
              <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="12" y1="5" x2="12" y2="19"></line>
                <line x1="5" y1="12" x2="19" y2="12"></line>
              </svg>
              添加账户
            </button>
          </div>

          <div v-if="loadingAccounts" class="p-4 text-center text-gray-500 text-sm">
            加载中...
          </div>

          <div v-else-if="currentCategoryAccounts.length === 0" class="p-4 text-center text-gray-500 text-sm">
            该类型下暂无账户
          </div>

          <div v-else>
            <div class="divide-y divide-gray-200 max-h-[calc(100vh-400px)] overflow-y-auto">
              <div
                v-for="account in currentCategoryAccounts"
                :key="account.id"
                :class="[
                  'group relative',
                  selectedAccount?.id === account.id
                    ? 'bg-primary/10 border-l-4 border-primary'
                    : 'hover:bg-gray-50 border-l-4 border-transparent'
                ]"
              >
                <button
                  @click="selectAccount(account)"
                  class="w-full px-3 py-2 text-left transition-colors"
                >
                  <div class="flex items-center justify-between gap-2">
                    <div class="flex-1 min-w-0">
                      <div class="font-medium text-gray-900 text-sm truncate flex items-center gap-1">
                        {{ account.accountName }}
                        <span v-if="!account.isActive" class="text-xs text-gray-400 bg-gray-100 px-1 py-0.5 rounded flex-shrink-0">停用</span>
                      </div>
                      <div class="text-xs text-gray-500 mt-0.5 flex items-center gap-1.5 flex-wrap">
                        <span>{{ getUserName(account.userId) }}</span>
                        <span class="text-gray-300">•</span>
                        <span>{{ account.currency }}</span>
                        <span v-if="account.institution" class="text-gray-300">•</span>
                        <span v-if="account.institution" class="truncate">{{ account.institution }}</span>
                        <span
                          v-if="account.taxStatus"
                          class="px-1.5 py-0.5 rounded shrink-0"
                          :class="getTaxStatusClass(account.taxStatus)"
                        >
                          {{ getTaxStatusLabel(account.taxStatus) }}
                        </span>
                      </div>
                    </div>
                    <div v-if="account.latestAmountInBaseCurrency !== null" class="text-xs font-medium text-primary flex-shrink-0">
                      ${{ formatNumber(account.latestAmountInBaseCurrency) }}
                    </div>
                  </div>
                </button>

                <!-- 操作按钮 -->
                <div class="absolute top-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity flex gap-1">
                  <button
                    @click.stop="openAccountDialog(account)"
                    class="p-1 text-blue-600 hover:bg-blue-50 rounded"
                    title="编辑"
                  >
                    <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                      <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                    </svg>
                  </button>
                  <button
                    v-if="account.isActive"
                    @click.stop="disableAccount(account)"
                    class="p-1 text-orange-600 hover:bg-orange-50 rounded"
                    title="停用"
                  >
                    <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <circle cx="12" cy="12" r="10"></circle>
                      <line x1="4.93" y1="4.93" x2="19.07" y2="19.07"></line>
                    </svg>
                  </button>
                </div>
              </div>
            </div>

            <!-- 分类总和 -->
            <div class="px-4 py-3 border-t border-gray-200 bg-gray-50">
              <div class="flex items-center justify-between">
                <span class="text-sm font-medium text-gray-700">{{ getCategoryName(selectedCategoryType) }}总计</span>
                <span class="text-base font-bold text-primary">
                  ${{ formatNumber(categoryTotal) }}
                </span>
              </div>
              <div class="text-xs text-gray-500 mt-1">
                共 {{ currentCategoryAccounts.length }} 个账户
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 中间和右侧：趋势图 + 历史记录 -->
      <div v-if="selectedAccount" class="col-span-12 lg:col-span-9">
        <!-- 账户信息栏 -->
        <div class="bg-white rounded-lg shadow border border-gray-200 p-4 mb-6">
          <div class="flex items-center justify-between">
            <div>
              <h2 class="text-lg font-semibold text-gray-900">{{ selectedAccount.accountName }}</h2>
              <p class="text-sm text-gray-600 mt-1">
                {{ selectedAccount.categoryName }} • {{ selectedAccount.currency }}
                <span v-if="selectedAccount.institution" class="ml-2">• {{ selectedAccount.institution }}</span>
              </p>
            </div>
            <button
              @click="openCreateDialog"
              class="px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary/90 text-sm font-medium flex items-center gap-2"
            >
              <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="12" y1="5" x2="12" y2="19"></line>
                <line x1="5" y1="12" x2="19" y2="12"></line>
              </svg>
              添加记录
            </button>
          </div>
        </div>

        <!-- 两列：趋势图 + 历史记录 -->
        <div class="grid grid-cols-1 xl:grid-cols-2 gap-6">
          <!-- 趋势图 -->
          <div class="bg-white rounded-lg shadow border border-gray-200 p-6">
            <div class="flex items-center justify-between mb-4">
              <h3 class="text-base font-semibold text-gray-900">资产趋势</h3>
              <div class="flex gap-2">
                <button
                  v-for="range in timeRanges"
                  :key="range.value"
                  @click="selectedTimeRange = range.value"
                  :class="[
                    'px-2 py-1 text-xs rounded-md font-medium transition-colors',
                    selectedTimeRange === range.value
                      ? 'bg-primary text-white'
                      : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                  ]"
                >
                  {{ range.label }}
                </button>
              </div>
            </div>
            <div v-if="records.length > 0" class="h-80">
              <canvas ref="chartCanvas"></canvas>
            </div>
            <div v-else class="h-80 flex items-center justify-center text-gray-500 text-sm">
              暂无数据，请添加记录
            </div>
          </div>

          <!-- 历史记录 -->
          <div class="bg-white rounded-lg shadow border border-gray-200">
            <div class="px-4 py-3 border-b border-gray-200">
              <h3 class="text-base font-semibold text-gray-900">历史记录</h3>
            </div>

            <div v-if="loadingRecords" class="text-center py-8 text-gray-500 text-sm">
              加载中...
            </div>

            <div v-else-if="filteredRecords.length === 0" class="text-center py-8 text-gray-500 text-sm">
              暂无记录
            </div>

            <div v-else class="overflow-y-auto max-h-96">
              <table class="min-w-full divide-y divide-gray-200">
                <thead class="bg-gray-50 sticky top-0">
                  <tr>
                    <th class="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">日期</th>
                    <th class="px-3 py-2 text-right text-xs font-medium text-gray-500 uppercase">金额</th>
                    <th class="px-3 py-2 text-right text-xs font-medium text-gray-500 uppercase">操作</th>
                  </tr>
                </thead>
                <tbody class="bg-white divide-y divide-gray-200">
                  <tr v-for="record in filteredRecords" :key="record.id" class="hover:bg-gray-50">
                    <td class="px-3 py-2 text-sm text-gray-900">{{ formatDate(record.recordDate) }}</td>
                    <td class="px-3 py-2 text-sm text-gray-900 text-right font-medium">
                      {{ getCurrencySymbol(record.currency) }}{{ formatNumber(record.amount) }}
                    </td>
                    <td class="px-3 py-2 text-sm text-right">
                      <div class="flex justify-end gap-2">
                        <button
                          @click="editRecord(record)"
                          class="text-blue-600 hover:text-blue-800 text-xs"
                        >
                          编辑
                        </button>
                        <button
                          @click="deleteRecord(record)"
                          class="text-red-600 hover:text-red-800 text-xs"
                        >
                          删除
                        </button>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>

      <!-- 未选择账户的提示 -->
      <div v-else class="col-span-12 lg:col-span-9">
        <div class="bg-gray-50 rounded-lg border-2 border-dashed border-gray-300 p-12 text-center">
          <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
          <h3 class="mt-2 text-sm font-medium text-gray-900">请选择账户</h3>
          <p class="mt-1 text-sm text-gray-500">从左侧列表中选择一个账户以查看其历史记录</p>
        </div>
      </div>
    </div>

    <!-- 创建/编辑账户对话框 -->
    <div
      v-if="showAccountDialog"
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50"
      @click.self="closeAccountDialog"
    >
      <div class="bg-white rounded-lg p-6 w-full max-w-md">
        <h2 class="text-xl font-bold mb-4">
          {{ editingAccount ? '编辑账户' : '添加账户' }}
        </h2>
        <form @submit.prevent="submitAccountForm" class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              资产分类 *
            </label>
            <div class="w-full px-3 py-2 bg-gray-50 border border-gray-300 rounded-lg text-gray-900">
              {{ getCategoryName(selectedCategoryType) }}
            </div>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              关联用户 *
            </label>
            <select
              v-model="accountFormData.userId"
              required
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
            >
              <option v-for="user in users" :key="user.id" :value="user.id">
                {{ user.fullName }}
              </option>
            </select>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              账户名称 *
            </label>
            <input
              v-model="accountFormData.accountName"
              type="text"
              required
              placeholder="例如：招商银行储蓄卡"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">币种 *</label>
            <select
              v-model="accountFormData.currency"
              required
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
            >
              <option value="CNY">人民币 (CNY)</option>
              <option value="USD">美元 (USD)</option>
              <option value="EUR">欧元 (EUR)</option>
              <option value="HKD">港币 (HKD)</option>
              <option value="GBP">英镑 (GBP)</option>
              <option value="JPY">日元 (JPY)</option>
            </select>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">机构名称</label>
            <input
              v-model="accountFormData.institution"
              type="text"
              placeholder="例如：招商银行"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">账号</label>
            <input
              v-model="accountFormData.accountNumber"
              type="text"
              placeholder="例如：**** **** **** 1234"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">税务状态</label>
            <select
              v-model="accountFormData.taxStatus"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
            >
              <option value="TAXABLE">应税 (Taxable)</option>
              <option value="TAX_FREE">免税 (Tax-Free)</option>
              <option value="TAX_DEFERRED">延税 (Tax-Deferred)</option>
            </select>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">备注</label>
            <textarea
              v-model="accountFormData.notes"
              rows="3"
              placeholder="添加备注信息"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
            ></textarea>
          </div>

          <div class="flex gap-3 pt-4">
            <button
              type="submit"
              class="flex-1 px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary/90 font-medium"
            >
              {{ editingAccount ? '保存' : '创建' }}
            </button>
            <button
              type="button"
              @click="closeAccountDialog"
              class="flex-1 px-4 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 font-medium"
            >
              取消
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- 创建/编辑记录对话框 -->
    <div
      v-if="showDialog"
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50"
      @click.self="closeDialog"
    >
      <div class="bg-white rounded-lg p-6 w-full max-w-md">
        <h2 class="text-xl font-bold mb-4">
          {{ editingRecord ? '编辑记录' : '添加历史记录' }}
        </h2>
        <form @submit.prevent="submitForm" class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              记录日期 *
            </label>
            <input
              v-model="formData.recordDate"
              type="date"
              required
              :max="new Date().toISOString().split('T')[0]"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">金额 *</label>
            <input
              v-model="formData.amount"
              type="number"
              step="0.01"
              required
              placeholder="例如：10000.00"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">币种 *</label>
            <select
              v-model="formData.currency"
              required
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
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
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
            ></textarea>
          </div>

          <div class="flex gap-3 pt-4">
            <button
              type="submit"
              class="flex-1 px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary/90 font-medium"
            >
              {{ editingRecord ? '保存' : '创建' }}
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
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { assetCategoryAPI, assetAccountAPI, assetRecordAPI } from '@/api/asset'
import { userAPI } from '@/api/user'
import { Chart, registerables } from 'chart.js'
import 'chartjs-adapter-date-fns'

Chart.register(...registerables)

// 资产分类类型映射
const CATEGORY_TYPE_NAMES = {
  'CASH': '现金类',
  'STOCKS': '股票投资',
  'RETIREMENT_FUND': '退休基金',
  'INSURANCE': '保险',
  'REAL_ESTATE': '房地产',
  'CRYPTOCURRENCY': '数字货币',
  'PRECIOUS_METALS': '贵金属',
  'OTHER': '其他'
}

const categories = ref([])
const allCategories = ref([])  // 存储完整的分类信息（包含ID）
const selectedCategoryType = ref('CASH')
const accounts = ref([])
const selectedAccount = ref(null)
const records = ref([])
const users = ref([])
const loadingAccounts = ref(false)
const loadingRecords = ref(false)
const showDialog = ref(false)
const editingRecord = ref(null)
const showAccountDialog = ref(false)
const editingAccount = ref(null)
const chartCanvas = ref(null)
let chartInstance = null

const selectedTimeRange = ref('all')
const timeRanges = [
  { value: 'week', label: '本周' },
  { value: 'month', label: '本月' },
  { value: 'year', label: '本年' },
  { value: 'all', label: '全部' }
]

const formData = ref({
  recordDate: new Date().toISOString().split('T')[0],
  amount: '',
  currency: 'CNY',
  notes: ''
})

const accountFormData = ref({
  accountName: '',
  currency: 'CNY',
  institution: '',
  accountNumber: '',
  taxStatus: 'TAXABLE',
  notes: '',
  userId: 1
})

// 当前分类下的账户
const currentCategoryAccounts = computed(() => {
  return accounts.value.filter(account => account.categoryType === selectedCategoryType.value)
})

// 当前分类的总和（基准货币）
const categoryTotal = computed(() => {
  return currentCategoryAccounts.value.reduce((total, account) => {
    // 确保正确处理 null/undefined 和数字类型转换
    const amount = account.latestAmountInBaseCurrency
    if (amount === null || amount === undefined) {
      return total
    }
    return total + Number(amount)
  }, 0)
})

// 根据时间范围过滤记录
const filteredRecords = computed(() => {
  if (selectedTimeRange.value === 'all') {
    return records.value
  }

  const now = new Date()
  const filterDate = new Date()

  switch (selectedTimeRange.value) {
    case 'week':
      filterDate.setDate(now.getDate() - 7)
      break
    case 'month':
      filterDate.setMonth(now.getMonth() - 1)
      break
    case 'year':
      filterDate.setFullYear(now.getFullYear() - 1)
      break
  }

  return records.value.filter(record => {
    const recordDate = new Date(record.recordDate)
    return recordDate >= filterDate
  })
})

// 格式化数字
const formatNumber = (num) => {
  if (!num) return '0.00'
  return parseFloat(num).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '-'
  // 直接解析字符串格式 YYYY-MM-DD，避免时区转换
  if (typeof dateString === 'string' && dateString.match(/^\d{4}-\d{2}-\d{2}$/)) {
    const [year, month, day] = dateString.split('-')
    return `${year}/${month}/${day}`
  }
  // 兼容其他格式
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
}

// 获取货币符号
const getCurrencySymbol = (currency) => {
  const currencyMap = {
    'CNY': '¥',
    'USD': '$',
    'EUR': '€',
    'GBP': '£',
    'JPY': '¥',
    'HKD': 'HK$'
  }
  return currencyMap[currency] || currency + ' '
}

// 获取分类名称
const getCategoryName = (categoryType) => {
  return CATEGORY_TYPE_NAMES[categoryType] || categoryType
}

// 获取用户名称
const getUserName = (userId) => {
  const user = users.value.find(u => u.id === userId)
  return user ? user.fullName : '未知用户'
}

// 加载资产分类
const loadCategories = async () => {
  try {
    // 加载分类类型
    const typesResponse = await assetCategoryAPI.getTypes(1) // 使用默认用户ID 1
    if (typesResponse.success && typesResponse.data) {
      // 将类型转换为分类对象
      categories.value = typesResponse.data.map(type => ({
        type: type,
        name: CATEGORY_TYPE_NAMES[type] || type
      }))

      // 如果有分类，默认选中第一个
      if (categories.value.length > 0) {
        selectedCategoryType.value = categories.value[0].type
      }
    }

    // 加载完整分类信息（包含ID）
    const allResponse = await assetCategoryAPI.getAll(1)
    if (allResponse.success && allResponse.data) {
      allCategories.value = allResponse.data
    }
  } catch (error) {
    console.error('加载分类失败:', error)
  }
}

// 加载用户列表
const loadUsers = async () => {
  try {
    const response = await userAPI.getAll()
    if (response.success) {
      users.value = response.data
    }
  } catch (error) {
    console.error('加载用户失败:', error)
  }
}

// 加载账户列表
const loadAccounts = async () => {
  loadingAccounts.value = true
  try {
    const response = await assetAccountAPI.getAll()
    if (response.success) {
      accounts.value = response.data
    }
  } catch (error) {
    console.error('加载账户失败:', error)
  } finally {
    loadingAccounts.value = false
  }
}

// 选择账户
const selectAccount = async (account) => {
  selectedAccount.value = account
  await loadRecords()
}

// 加载记录
const loadRecords = async () => {
  if (!selectedAccount.value) {
    records.value = []
    return
  }

  loadingRecords.value = true
  try {
    const response = await assetRecordAPI.getByAccountId(selectedAccount.value.id)
    if (response.success) {
      records.value = response.data
      await nextTick()
      updateChart()
    }
  } catch (error) {
    console.error('加载记录失败:', error)
  } finally {
    loadingRecords.value = false
  }
}

// 更新图表
const updateChart = () => {
  if (!chartCanvas.value || filteredRecords.value.length === 0) {
    if (chartInstance) {
      chartInstance.destroy()
      chartInstance = null
    }
    return
  }

  const sortedRecords = [...filteredRecords.value].sort((a, b) =>
    new Date(a.recordDate) - new Date(b.recordDate)
  )

  // 使用 {x, y} 格式的数据点，x为日期对象
  const data = sortedRecords.map(r => ({
    x: new Date(r.recordDate),
    y: r.amount
  }))

  // 获取账户货币符号用于显示
  const currencySymbol = getCurrencySymbol(selectedAccount.value?.currency || 'USD')

  if (chartInstance) {
    chartInstance.destroy()
  }

  const ctx = chartCanvas.value.getContext('2d')
  chartInstance = new Chart(ctx, {
    type: 'line',
    data: {
      datasets: [{
        label: '资产金额',
        data: data,
        borderColor: 'rgb(34, 197, 94)',
        backgroundColor: 'rgba(34, 197, 94, 0.1)',
        tension: 0.1,
        fill: true
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          display: true,
          position: 'top'
        },
        tooltip: {
          callbacks: {
            label: function(context) {
              return '金额: ' + currencySymbol + formatNumber(context.parsed.y)
            }
          }
        }
      },
      scales: {
        x: {
          type: 'time',
          time: {
            unit: 'day',
            displayFormats: {
              day: 'yyyy-MM-dd',
              month: 'yyyy-MM',
              year: 'yyyy'
            }
          },
          title: {
            display: true,
            text: '日期'
          }
        },
        y: {
          beginAtZero: false,
          title: {
            display: true,
            text: '金额'
          },
          ticks: {
            callback: function(value) {
              return currencySymbol + formatNumber(value)
            }
          }
        }
      }
    }
  })
}

// 监听时间范围变化
watch(selectedTimeRange, () => {
  updateChart()
})

// 监听分类切换
watch(selectedCategoryType, () => {
  selectedAccount.value = null
  records.value = []
  if (chartInstance) {
    chartInstance.destroy()
    chartInstance = null
  }
})

// 打开创建对话框
const openCreateDialog = () => {
  editingRecord.value = null
  formData.value = {
    recordDate: new Date().toISOString().split('T')[0],
    amount: '',
    currency: selectedAccount.value?.currency || 'CNY',
    notes: ''
  }
  showDialog.value = true
}

// 编辑记录
const editRecord = (record) => {
  editingRecord.value = record

  // 调试：打印原始数据
  console.log('原始 record.recordDate:', record.recordDate)
  console.log('类型:', typeof record.recordDate)
  console.log('是否为数组:', Array.isArray(record.recordDate))

  // 处理日期格式 - 确保转换为 YYYY-MM-DD 字符串
  let dateStr = record.recordDate
  if (Array.isArray(record.recordDate)) {
    // 如果是数组格式 [year, month, day]
    const [year, month, day] = record.recordDate
    dateStr = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
    console.log('转换后的日期 (从数组):', dateStr)
  } else if (typeof record.recordDate === 'string' && record.recordDate.includes('T')) {
    // 如果是 ISO 8601 格式，只取日期部分
    dateStr = record.recordDate.split('T')[0]
    console.log('转换后的日期 (从ISO):', dateStr)
  } else {
    console.log('使用原始日期:', dateStr)
  }

  formData.value = {
    recordDate: dateStr,
    amount: record.amount,
    currency: record.currency,
    notes: record.notes || ''
  }
  showDialog.value = true
}

// 删除记录
const deleteRecord = async (record) => {
  if (!confirm(`确定要删除 ${formatDate(record.recordDate)} 的记录吗？`)) return

  try {
    const response = await assetRecordAPI.delete(record.id)
    if (response.success) {
      await loadRecords()
    }
  } catch (error) {
    console.error('删除记录失败:', error)
    alert('删除失败，请重试')
  }
}

// 提交表单
const submitForm = async () => {
  try {
    const data = {
      accountId: selectedAccount.value.id,
      recordDate: formData.value.recordDate,
      amount: parseFloat(formData.value.amount),
      currency: formData.value.currency,
      notes: formData.value.notes,
      exchangeRate: 1.0
    }

    let response
    if (editingRecord.value) {
      response = await assetRecordAPI.update(editingRecord.value.id, data)
    } else {
      response = await assetRecordAPI.create(data)
    }

    if (response.success) {
      closeDialog()
      await loadRecords()
      // 重新加载账户列表以更新最新金额
      await loadAccounts()
    }
  } catch (error) {
    console.error('提交失败:', error)
    alert('操作失败，请重试')
  }
}

// 关闭对话框
const closeDialog = () => {
  showDialog.value = false
  editingRecord.value = null
}

// 打开账户对话框
const openAccountDialog = (account = null) => {
  editingAccount.value = account
  if (account) {
    accountFormData.value = {
      accountName: account.accountName,
      currency: account.currency,
      institution: account.institution || '',
      accountNumber: account.accountNumber || '',
      taxStatus: account.taxStatus || 'TAXABLE',
      notes: account.notes || '',
      userId: account.userId,
      categoryId: account.categoryId  // 保存 categoryId 用于更新
    }
  } else {
    accountFormData.value = {
      accountName: '',
      currency: 'CNY',
      institution: '',
      accountNumber: '',
      taxStatus: 'TAXABLE',
      notes: '',
      userId: users.value.length > 0 ? users.value[0].id : 1
    }
  }
  showAccountDialog.value = true
}

// 关闭账户对话框
const closeAccountDialog = () => {
  showAccountDialog.value = false
  editingAccount.value = null
}

// 提交账户表单
const submitAccountForm = async () => {
  try {
    let data

    if (editingAccount.value) {
      // 编辑：使用已保存的 categoryId
      data = {
        ...accountFormData.value,
        isActive: true
      }
    } else {
      // 创建：根据 categoryType 查找 categoryId
      const category = allCategories.value.find(cat => cat.type === selectedCategoryType.value)
      if (!category) {
        alert('未找到对应的分类，请刷新页面重试')
        return
      }

      data = {
        ...accountFormData.value,
        categoryId: category.id,
        isActive: true
      }
    }

    let response
    if (editingAccount.value) {
      response = await assetAccountAPI.update(editingAccount.value.id, data)
    } else {
      response = await assetAccountAPI.create(data)
    }

    if (response.success) {
      closeAccountDialog()
      await loadAccounts()
      alert(editingAccount.value ? '账户更新成功' : '账户创建成功')
    }
  } catch (error) {
    console.error('提交失败:', error)
    alert('操作失败，请重试')
  }
}

// 停用账户（软删除）
const disableAccount = async (account) => {
  if (!confirm(`确定要停用账户"${account.accountName}"吗？停用后该账户将不再显示在活跃列表中。`)) return

  try {
    const data = {
      ...account,
      isActive: false
    }
    const response = await assetAccountAPI.update(account.id, data)
    if (response.success) {
      await loadAccounts()
      if (selectedAccount.value?.id === account.id) {
        selectedAccount.value = null
        records.value = []
      }
      alert('账户已停用')
    }
  } catch (error) {
    console.error('停用账户失败:', error)
    alert('停用失败，请重试')
  }
}

// 获取税务状态标签
const getTaxStatusLabel = (taxStatus) => {
  const labels = {
    'TAXABLE': '应税',
    'TAX_FREE': '免税',
    'TAX_DEFERRED': '延税'
  }
  return labels[taxStatus] || taxStatus
}

// 获取税务状态样式
const getTaxStatusClass = (taxStatus) => {
  const classes = {
    'TAXABLE': 'bg-orange-100 text-orange-700 text-xs',
    'TAX_FREE': 'bg-green-100 text-green-700 text-xs',
    'TAX_DEFERRED': 'bg-blue-100 text-blue-700 text-xs'
  }
  return classes[taxStatus] || 'bg-gray-100 text-gray-700 text-xs'
}

onMounted(async () => {
  await loadUsers()
  await loadCategories()
  await loadAccounts()
})
</script>
