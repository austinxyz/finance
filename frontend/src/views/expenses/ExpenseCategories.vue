<template>
  <div class="p-3 md:p-6 space-y-4 md:space-y-6">
    <!-- 家庭选择器和大类 Tab 在同一行 -->
    <div class="flex items-center justify-between gap-4 border-b border-gray-200">
      <nav class="-mb-px flex space-x-1 md:space-x-2 flex-1" aria-label="Tabs">
        <button
          v-for="category in majorCategories"
          :key="category.id"
          @click="selectedMajorCategory = category"
          :class="[
            'whitespace-nowrap py-2 px-2 border-b-2 font-medium text-xs transition-colors flex items-center gap-1',
            selectedMajorCategory?.id === category.id
              ? 'border-primary text-primary'
              : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
          ]"
        >
          <span class="text-sm">{{ category.icon }}</span>
          {{ category.name }}
        </button>
      </nav>

      <div class="flex items-center gap-2 flex-shrink-0 pb-3">
        <label class="text-sm font-medium text-gray-700 whitespace-nowrap">家庭:</label>
        <select
          v-model="selectedFamilyId"
          @change="onFamilyChange"
          class="px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-primary bg-white text-sm"
        >
          <option v-for="family in families" :key="family.id" :value="family.id">
            {{ family.familyName }}
          </option>
        </select>
      </div>
    </div>

    <!-- 三列布局：子分类列表 + 趋势图 + 历史记录 -->
    <div class="grid grid-cols-12 gap-6">
      <!-- 左侧：子分类列表 -->
      <div class="col-span-12 lg:col-span-3">
        <div class="bg-white rounded-lg shadow border border-gray-200">
          <div class="px-4 py-3 border-b border-gray-200 flex items-center justify-between">
            <h2 class="text-sm font-semibold text-gray-900">子分类列表</h2>
            <button
              @click="openCategoryDialog()"
              class="text-primary hover:text-primary/80 text-xs font-medium flex items-center gap-1"
            >
              <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="12" y1="5" x2="12" y2="19"></line>
                <line x1="5" y1="12" x2="19" y2="12"></line>
              </svg>
              添加子分类
            </button>
          </div>

          <div v-if="loadingCategories" class="p-4 text-center text-gray-500 text-sm">
            加载中...
          </div>

          <div v-else-if="minorCategories.length === 0" class="p-4 text-center text-gray-500 text-sm">
            该大类下暂无子分类
          </div>

          <div v-else>
            <div class="divide-y divide-gray-200 max-h-[calc(100vh-400px)] overflow-y-auto">
              <div
                v-for="category in minorCategories"
                :key="category.id"
                :class="[
                  'group relative',
                  selectedMinorCategory?.id === category.id
                    ? 'bg-primary/10 border-l-4 border-primary'
                    : 'hover:bg-gray-50 border-l-4 border-transparent'
                ]"
              >
                <button
                  @click="selectMinorCategory(category)"
                  class="w-full px-3 py-2 text-left transition-colors"
                >
                  <div class="flex items-center justify-between gap-2">
                    <div class="flex-1 min-w-0">
                      <div class="font-medium text-gray-900 text-sm truncate flex items-center gap-2">
                        {{ category.name }}
                        <span :class="[
                          'text-xs px-1.5 py-0.5 rounded flex-shrink-0',
                          category.expenseType === 'FIXED_DAILY'
                            ? 'bg-green-100 text-green-700'
                            : 'bg-orange-100 text-orange-700'
                        ]">
                          {{ category.expenseType === 'FIXED_DAILY' ? '固定' : '不定期' }}
                        </span>
                        <span v-if="!category.isActive" class="text-xs text-gray-400 bg-gray-100 px-1 py-0.5 rounded flex-shrink-0">停用</span>
                      </div>
                      <div class="text-xs text-gray-500 mt-0.5">
                        {{ category.recordCount || 0 }}条记录
                        <span v-if="category.latestAmount" class="ml-2">
                          最近¥{{ formatNumber(category.latestAmount) }}
                        </span>
                      </div>
                    </div>
                  </div>
                </button>

                <!-- 操作按钮 -->
                <div class="absolute top-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity flex gap-1">
                  <button
                    @click.stop="openCategoryDialog(category)"
                    class="p-1 text-blue-600 hover:bg-blue-50 rounded"
                    title="编辑"
                  >
                    <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                      <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                    </svg>
                  </button>
                  <button
                    v-if="category.isActive"
                    @click.stop="disableCategory(category)"
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
                <span class="text-sm font-medium text-gray-700">{{ selectedMajorCategory?.name }}总计</span>
                <span class="text-base font-bold text-primary">
                  ¥{{ formatNumber(categoryTotal) }}
                </span>
              </div>
              <div class="text-xs text-gray-500 mt-1">
                共 {{ minorCategories.length }} 个子分类
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 中间和右侧：趋势图 + 历史记录 -->
      <div v-if="selectedMinorCategory" class="col-span-12 lg:col-span-9">
        <!-- 分类信息栏 -->
        <div class="bg-white rounded-lg shadow border border-gray-200 p-4 mb-6">
          <div class="flex items-center justify-between">
            <div>
              <h2 class="text-lg font-semibold text-gray-900 flex items-center gap-2">
                <span class="text-2xl">{{ selectedMajorCategory?.icon }}</span>
                {{ selectedMajorCategory?.name }} - {{ selectedMinorCategory.name }}
              </h2>
              <p class="text-sm text-gray-600 mt-1">
                {{ selectedMinorCategory.description || '暂无说明' }}
              </p>
            </div>
            <button
              @click="openRecordDialog()"
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
              <h3 class="text-base font-semibold text-gray-900">支出趋势</h3>
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
            <div v-if="loadingRecords" class="p-4 text-center text-gray-500 text-sm">
              加载中...
            </div>
            <div v-else-if="records.length === 0" class="p-4 text-center text-gray-500 text-sm">
              暂无记录
            </div>
            <div v-else class="max-h-[480px] overflow-y-auto">
              <table class="w-full">
                <thead class="bg-gray-50 sticky top-0">
                  <tr>
                    <th class="px-4 py-2 text-left text-xs font-medium text-gray-700">期间</th>
                    <th class="px-4 py-2 text-right text-xs font-medium text-gray-700">金额</th>
                    <th class="px-4 py-2 text-right text-xs font-medium text-gray-700">操作</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-gray-200">
                  <tr v-for="record in records" :key="record.id" class="hover:bg-gray-50">
                    <td class="px-4 py-2 text-sm text-gray-900">{{ record.expensePeriod }}</td>
                    <td class="px-4 py-2 text-sm text-right font-medium text-gray-900">
                      {{ getCurrencySymbol(record.currency) }}{{ formatNumber(record.amount) }}
                    </td>
                    <td class="px-4 py-2 text-right">
                      <div class="flex items-center justify-end gap-2">
                        <button
                          @click="openRecordDialog(record)"
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

      <!-- 未选择提示 -->
      <div v-else class="col-span-12 lg:col-span-9">
        <div class="bg-white rounded-lg shadow border border-gray-200 p-12 text-center">
          <svg xmlns="http://www.w3.org/2000/svg" class="w-16 h-16 mx-auto text-gray-400 mb-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
            <polyline points="14 2 14 8 20 8"></polyline>
            <line x1="16" y1="13" x2="8" y2="13"></line>
            <line x1="16" y1="17" x2="8" y2="17"></line>
            <polyline points="10 9 9 9 8 9"></polyline>
          </svg>
          <h3 class="text-lg font-medium text-gray-900 mb-2">请选择子分类</h3>
          <p class="text-sm text-gray-600">从左侧列表选择一个子分类查看详情和历史记录</p>
        </div>
      </div>
    </div>

    <!-- 子分类对话框 -->
    <div v-if="showCategoryDialog" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-0 md:p-4">
      <div class="bg-white md:rounded-lg shadow-xl w-full max-w-md h-full md:h-auto md:mx-4 flex flex-col md:max-h-[90vh]">
        <div class="px-4 md:px-6 py-4 border-b border-gray-200 flex-shrink-0">
          <h3 class="text-lg font-semibold text-gray-900">
            {{ editingCategory ? '编辑子分类' : '添加子分类' }}
          </h3>
        </div>
        <div class="px-4 md:px-6 py-4 space-y-4 overflow-y-auto flex-1">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">名称 *</label>
            <input
              v-model="categoryForm.name"
              type="text"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent"
              placeholder="请输入子分类名称"
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">描述</label>
            <textarea
              v-model="categoryForm.description"
              rows="3"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent"
              placeholder="请输入描述（可选）"
            ></textarea>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">支出类型 *</label>
            <div class="flex flex-col md:flex-row gap-3 md:gap-4">
              <label class="flex items-start md:items-center gap-2 cursor-pointer p-3 md:p-0 border md:border-0 border-gray-200 rounded-lg md:rounded-none">
                <input
                  v-model="categoryForm.expenseType"
                  type="radio"
                  value="FIXED_DAILY"
                  class="w-4 h-4 mt-0.5 md:mt-0 text-primary focus:ring-2 focus:ring-primary flex-shrink-0"
                />
                <div class="flex-1">
                  <span class="text-sm text-gray-700 font-medium">固定日常</span>
                  <span class="block md:inline text-xs text-gray-500 mt-0.5 md:mt-0 md:ml-1">（如：房租、水电、伙食）</span>
                </div>
              </label>
              <label class="flex items-start md:items-center gap-2 cursor-pointer p-3 md:p-0 border md:border-0 border-gray-200 rounded-lg md:rounded-none">
                <input
                  v-model="categoryForm.expenseType"
                  type="radio"
                  value="LARGE_IRREGULAR"
                  class="w-4 h-4 mt-0.5 md:mt-0 text-primary focus:ring-2 focus:ring-primary flex-shrink-0"
                />
                <div class="flex-1">
                  <span class="text-sm text-gray-700 font-medium">大额不定期</span>
                  <span class="block md:inline text-xs text-gray-500 mt-0.5 md:mt-0 md:ml-1">（如：旅游、装修、家电）</span>
                </div>
              </label>
            </div>
          </div>
        </div>
        <div class="px-4 md:px-6 py-4 border-t border-gray-200 flex justify-end gap-3 flex-shrink-0">
          <button
            @click="closeCategoryDialog"
            class="px-4 py-2 text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 min-h-[44px]"
          >
            取消
          </button>
          <button
            @click="saveCategory"
            class="px-4 py-2 text-white bg-primary rounded-lg hover:bg-primary-dark min-h-[44px]"
          >
            保存
          </button>
        </div>
      </div>
    </div>

    <!-- 记录对话框 -->
    <div v-if="showRecordDialog" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div class="bg-white rounded-lg shadow-xl w-full max-w-md mx-4">
        <div class="px-6 py-4 border-b border-gray-200">
          <h3 class="text-lg font-semibold text-gray-900">
            {{ editingRecord ? '编辑记录' : '添加记录' }}
          </h3>
        </div>
        <div class="px-6 py-4 space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">家庭 *</label>
            <select
              v-model="recordForm.familyId"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent"
            >
              <option v-for="family in families" :key="family.id" :value="family.id">
                {{ family.familyName }}
              </option>
            </select>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">货币 *</label>
            <select
              v-model="recordForm.currency"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent"
            >
              <option v-for="currency in currencies" :key="currency" :value="currency">
                {{ currency }}
              </option>
            </select>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">期间 *</label>
            <input
              v-model="recordForm.expensePeriod"
              type="month"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent"
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">金额 *</label>
            <div class="relative">
              <span class="absolute left-3 top-2 text-gray-500">{{ recordForm.currency === 'CNY' ? '¥' : '$' }}</span>
              <input
                v-model="recordForm.amount"
                type="number"
                step="0.01"
                class="w-full pl-8 pr-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent"
                placeholder="0.00"
              />
            </div>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">备注</label>
            <textarea
              v-model="recordForm.description"
              rows="2"
              maxlength="200"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent"
              placeholder="请输入备注（可选，最多200字）"
            ></textarea>
          </div>
        </div>
        <div class="px-6 py-4 border-t border-gray-200 flex justify-end gap-3">
          <button
            @click="closeRecordDialog"
            class="px-4 py-2 text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50"
          >
            取消
          </button>
          <button
            @click="saveRecord"
            class="px-4 py-2 text-white bg-primary rounded-lg hover:bg-primary-dark"
          >
            保存
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { Chart } from 'chart.js/auto'
import 'chartjs-adapter-date-fns'
import { expenseCategoryAPI, expenseRecordAPI } from '@/api/expense'
import { familyAPI } from '@/api/family'
import { exchangeRateAPI } from '@/api/exchangeRate'

// 数据状态
const loadingCategories = ref(false)
const loadingRecords = ref(false)
const majorCategories = ref([])
const selectedMajorCategory = ref(null)
const minorCategories = ref([])
const selectedMinorCategory = ref(null)
const records = ref([])
const families = ref([])
const selectedFamilyId = ref(null)
const currencies = ref([])

// 时间范围
const selectedTimeRange = ref('12m')
const timeRanges = [
  { value: '6m', label: '6月' },
  { value: '12m', label: '1年' },
  { value: '36m', label: '3年' },
  { value: 'all', label: '全部' }
]

// 对话框状态
const showCategoryDialog = ref(false)
const showRecordDialog = ref(false)
const editingCategory = ref(null)
const editingRecord = ref(null)

// 表单数据
const categoryForm = ref({
  name: '',
  description: '',
  expenseType: 'FIXED_DAILY'
})

const recordForm = ref({
  familyId: null,
  currency: 'USD',
  expensePeriod: '',
  amount: '',
  description: ''
})

// Chart实例
const chartCanvas = ref(null)
let chartInstance = null

// 计算属性 - 分类总和
const categoryTotal = computed(() => {
  // 这里计算选中大类下所有子分类的总支出
  // 实际应该从API获取
  return 0
})

// 辅助函数
function formatNumber(num) {
  return new Intl.NumberFormat('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(num || 0)
}

function getCurrencySymbol(currency) {
  return currency === 'CNY' ? '¥' : '$'
}

// 加载家庭列表
async function loadFamilies() {
  try {
    const response = await familyAPI.getAll()

    let familyList = []
    if (Array.isArray(response.data)) {
      familyList = response.data
    } else if (response.data && response.data.data) {
      familyList = response.data.data
    } else if (response.data && 'success' in response.data) {
      familyList = response.data.data || []
    }

    families.value = familyList

    // 如果selectedFamilyId还未设置，获取默认家庭
    if (!selectedFamilyId.value) {
      try {
        const defaultResponse = await familyAPI.getDefault()
        if (defaultResponse.success && defaultResponse.data) {
          selectedFamilyId.value = defaultResponse.data.id
          recordForm.value.familyId = defaultResponse.data.id
        } else if (families.value.length > 0) {
          selectedFamilyId.value = families.value[0].id
          recordForm.value.familyId = families.value[0].id
        }
      } catch (err) {
        console.error('获取默认家庭失败:', err)
        if (families.value.length > 0) {
          selectedFamilyId.value = families.value[0].id
          recordForm.value.familyId = families.value[0].id
        }
      }
    }
  } catch (error) {
    console.error('加载家庭列表失败:', error)
  }
}

// 加载货币列表
async function loadCurrencies() {
  try {
    const response = await exchangeRateAPI.getAllActive()

    let rates = []
    if (Array.isArray(response.data)) {
      rates = response.data
    } else if (response.data && response.data.data) {
      rates = response.data.data
    } else if (response.data && 'success' in response.data) {
      rates = response.data.data || []
    }

    // 提取唯一货币
    const currencySet = new Set(rates.map(r => r.currency))
    currencySet.add('USD') // 基础货币
    currencies.value = Array.from(currencySet).sort()
  } catch (error) {
    console.error('加载货币列表失败:', error)
    currencies.value = ['USD', 'CNY'] // 默认值
  }
}

// 加载大类
async function loadMajorCategories() {
  loadingCategories.value = true
  try {
    const response = await expenseCategoryAPI.getAll()

    // 处理响应数据 - 兼容多种返回格式
    let categoryData = []
    if (Array.isArray(response.data)) {
      categoryData = response.data
    } else if (response.data && response.data.data) {
      categoryData = response.data.data
    } else if (response.data && 'success' in response.data) {
      categoryData = response.data.data || []
    }

    majorCategories.value = categoryData
    if (majorCategories.value.length > 0) {
      selectedMajorCategory.value = majorCategories.value[0]
    }
  } catch (error) {
    console.error('加载大类失败:', error)
    alert('加载大类失败，请刷新重试')
  } finally {
    loadingCategories.value = false
  }
}

// 加载子分类
async function loadMinorCategories() {
  if (!selectedMajorCategory.value) return

  loadingCategories.value = true
  try {
    const response = await expenseCategoryAPI.getAll()

    // 处理响应数据 - 兼容多种返回格式
    let allCategories = []
    if (Array.isArray(response.data)) {
      allCategories = response.data
    } else if (response.data && response.data.data) {
      allCategories = response.data.data
    } else if (response.data && 'success' in response.data) {
      allCategories = response.data.data || []
    }

    const major = allCategories.find(c => c.id === selectedMajorCategory.value.id)
    minorCategories.value = major?.minorCategories || []
  } catch (error) {
    console.error('加载子分类失败:', error)
  } finally {
    loadingCategories.value = false
  }
}

// 选择子分类
async function selectMinorCategory(category) {
  selectedMinorCategory.value = category
  await loadRecords()
}

// 加载记录
async function loadRecords() {
  if (!selectedMinorCategory.value || !selectedFamilyId.value) return

  loadingRecords.value = true
  try {
    const familyId = selectedFamilyId.value

    // 根据时间范围计算起止期间
    const end = new Date()
    const start = new Date()
    if (selectedTimeRange.value === '6m') {
      start.setMonth(start.getMonth() - 5)
    } else if (selectedTimeRange.value === '12m') {
      start.setMonth(start.getMonth() - 11)
    } else if (selectedTimeRange.value === '36m') {
      start.setMonth(start.getMonth() - 35)
    } else {
      start.setFullYear(start.getFullYear() - 10)
    }

    const startPeriod = `${start.getFullYear()}-${String(start.getMonth() + 1).padStart(2, '0')}`
    const endPeriod = `${end.getFullYear()}-${String(end.getMonth() + 1).padStart(2, '0')}`

    const response = await expenseRecordAPI.getByPeriodRange(familyId, startPeriod, endPeriod)

    // 处理响应数据 - 兼容多种返回格式
    let allRecords = []
    if (Array.isArray(response.data)) {
      allRecords = response.data
    } else if (response.data && response.data.data) {
      allRecords = response.data.data
    } else if (response.data && 'success' in response.data) {
      allRecords = response.data.data || []
    }

    records.value = allRecords
      .filter(r => r.minorCategoryId === selectedMinorCategory.value.id)
      .sort((a, b) => b.expensePeriod.localeCompare(a.expensePeriod))

    await nextTick()
    updateChart()
  } catch (error) {
    console.error('加载记录失败:', error)
  } finally {
    loadingRecords.value = false
  }
}

// 更新图表 - 支持多货币显示
function updateChart() {
  if (!chartCanvas.value || records.value.length === 0) return

  if (chartInstance) {
    chartInstance.destroy()
  }

  // 按货币分组
  const recordsByCurrency = records.value.reduce((acc, record) => {
    const currency = record.currency || 'CNY'
    if (!acc[currency]) {
      acc[currency] = []
    }
    acc[currency].push(record)
    return acc
  }, {})

  // 货币颜色映射
  const currencyColors = {
    'USD': { border: '#3b82f6', bg: 'rgba(59, 130, 246, 0.1)' },   // 蓝色
    'CNY': { border: '#ef4444', bg: 'rgba(239, 68, 68, 0.1)' },    // 红色
    'EUR': { border: '#10b981', bg: 'rgba(16, 185, 129, 0.1)' },   // 绿色
    'GBP': { border: '#f59e0b', bg: 'rgba(245, 158, 11, 0.1)' },   // 黄色
    'JPY': { border: '#8b5cf6', bg: 'rgba(139, 92, 246, 0.1)' },   // 紫色
    'AUD': { border: '#ec4899', bg: 'rgba(236, 72, 153, 0.1)' },   // 粉色
    'CAD': { border: '#06b6d4', bg: 'rgba(6, 182, 212, 0.1)' }     // 青色
  }

  // 为每个货币创建dataset
  const datasets = Object.entries(recordsByCurrency).map(([currency, currencyRecords]) => {
    const sortedRecords = [...currencyRecords].sort((a, b) => a.expensePeriod.localeCompare(b.expensePeriod))
    const colors = currencyColors[currency] || { border: '#6b7280', bg: 'rgba(107, 114, 128, 0.1)' }

    return {
      label: `${currency}`,
      data: sortedRecords.map(r => ({
        x: new Date(r.expensePeriod + '-01'),
        y: r.amount
      })),
      borderColor: colors.border,
      backgroundColor: colors.bg,
      tension: 0.3
    }
  })

  chartInstance = new Chart(chartCanvas.value, {
    type: 'line',
    data: { datasets },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      scales: {
        x: {
          type: 'time',
          time: {
            unit: 'month',
            displayFormats: {
              month: 'yyyy-MM'
            }
          },
          title: {
            display: true,
            text: '期间'
          }
        },
        y: {
          beginAtZero: true,
          title: {
            display: true,
            text: '金额'
          }
        }
      },
      plugins: {
        legend: {
          display: Object.keys(recordsByCurrency).length > 1, // 多货币时显示图例
          position: 'top'
        },
        tooltip: {
          callbacks: {
            label: function(context) {
              const currency = context.dataset.label
              const amount = context.parsed.y
              const symbol = currency === 'CNY' ? '¥' : '$'
              return `${currency}: ${symbol}${amount.toFixed(2)}`
            }
          }
        }
      }
    }
  })
}

// 打开分类对话框
function openCategoryDialog(category = null) {
  editingCategory.value = category
  if (category) {
    // 编辑模式
    categoryForm.value = {
      name: category.name,
      description: category.description || '',
      expenseType: category.expenseType || 'FIXED_DAILY'
    }
  } else {
    // 新建模式
    categoryForm.value = {
      name: '',
      description: '',
      expenseType: 'FIXED_DAILY'
    }
  }
  showCategoryDialog.value = true
}

// 保存分类
async function saveCategory() {
  if (!categoryForm.value.name.trim()) {
    alert('请输入子分类名称')
    return
  }

  try {
    if (editingCategory.value) {
      // 编辑 - 需要包含majorCategoryId
      await expenseCategoryAPI.updateMinor(editingCategory.value.id, {
        majorCategoryId: selectedMajorCategory.value.id,
        ...categoryForm.value
      })
      alert('修改成功')
    } else {
      // 新建
      await expenseCategoryAPI.createMinor({
        majorCategoryId: selectedMajorCategory.value.id,
        ...categoryForm.value
      })
      alert('添加成功')
    }
    showCategoryDialog.value = false
    await loadMinorCategories()
  } catch (error) {
    console.error('保存失败:', error)
    alert('保存失败，请重试')
  }
}

// 关闭分类对话框
function closeCategoryDialog() {
  showCategoryDialog.value = false
  editingCategory.value = null
  categoryForm.value = { name: '', description: '', expenseType: 'FIXED_DAILY' }
}

// 打开记录对话框
function openRecordDialog(record = null) {
  editingRecord.value = record
  if (record) {
    // 编辑模式
    recordForm.value = {
      familyId: record.familyId,
      currency: record.currency || 'USD',
      expensePeriod: record.expensePeriod,
      amount: record.amount,
      description: record.description || ''
    }
  } else {
    // 新建模式 - 默认本月，默认第一个家庭
    const now = new Date()
    const period = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
    recordForm.value = {
      familyId: families.value.length > 0 ? families.value[0].id : null,
      currency: 'USD',
      expensePeriod: period,
      amount: '',
      description: ''
    }
  }
  showRecordDialog.value = true
}

// 保存记录
async function saveRecord() {
  if (!recordForm.value.familyId) {
    alert('请选择家庭')
    return
  }
  if (!recordForm.value.amount || parseFloat(recordForm.value.amount) <= 0) {
    alert('请输入有效的金额')
    return
  }

  try {
    // 获取子分类的expenseType
    const category = minorCategories.value.find(c => c.id === selectedMinorCategory.value.id)

    const data = {
      familyId: recordForm.value.familyId,
      minorCategoryId: selectedMinorCategory.value.id,
      expensePeriod: recordForm.value.expensePeriod,
      amount: parseFloat(recordForm.value.amount),
      currency: recordForm.value.currency,
      expenseType: category?.expenseType || 'FIXED_DAILY',
      description: recordForm.value.description || ''
    }

    if (editingRecord.value) {
      // 编辑
      await expenseRecordAPI.update(editingRecord.value.id, data)
      alert('修改成功')
    } else {
      // 新建
      await expenseRecordAPI.create(data)
      alert('添加成功')
    }
    showRecordDialog.value = false
    await loadRecords()
  } catch (error) {
    console.error('保存失败:', error)
    alert('保存失败，请重试')
  }
}

// 关闭记录对话框
function closeRecordDialog() {
  showRecordDialog.value = false
  editingRecord.value = null
  recordForm.value = {
    familyId: families.value.length > 0 ? families.value[0].id : null,
    currency: 'USD',
    expensePeriod: '',
    amount: '',
    description: ''
  }
}

// 停用分类
async function disableCategory(category) {
  if (!confirm(`确定停用"${category.name}"吗？`)) return

  try {
    await expenseCategoryAPI.disableMinor(category.id)
    alert('停用成功')
    await loadMinorCategories()
  } catch (error) {
    console.error('停用失败:', error)
    alert('停用失败，请重试')
  }
}

// 删除记录
async function deleteRecord(record) {
  if (!confirm('确定删除此条记录吗？')) return

  try {
    await expenseRecordAPI.delete(record.id)
    alert('删除成功')
    await loadRecords()
  } catch (error) {
    console.error('删除失败:', error)
    alert('删除失败，请重试')
  }
}

// 家庭切换事件处理
function onFamilyChange() {
  // 清空当前选中的子分类和记录
  selectedMinorCategory.value = null
  records.value = []
  // 更新记录表单的家庭ID
  recordForm.value.familyId = selectedFamilyId.value
  // 重新加载子分类
  loadMinorCategories()
}

// 监听selectedFamilyId变化
watch(selectedFamilyId, (newId) => {
  if (newId) {
    recordForm.value.familyId = newId
    loadMinorCategories()
  }
})

// 监听大类变化
watch(selectedMajorCategory, () => {
  selectedMinorCategory.value = null
  records.value = []
  loadMinorCategories()
})

// 监听时间范围变化
watch(selectedTimeRange, () => {
  if (selectedMinorCategory.value) {
    loadRecords()
  }
})

// 初始化
onMounted(() => {
  loadFamilies()
  loadCurrencies()
  loadMajorCategories()
})
</script>
