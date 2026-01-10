<template>
  <div class="p-6 space-y-6">
    <!-- 家庭选择器和负债分类 Tab 在同一行 -->
    <div class="flex items-center justify-between gap-4 border-b border-gray-200">
      <nav class="-mb-px flex space-x-4 overflow-x-auto flex-1" aria-label="Tabs">
        <button
          v-for="category in categories"
          :key="category.type"
          @click="selectedCategoryType = category.type"
          :class="[
            'whitespace-nowrap py-3 px-4 border-b-2 font-medium text-sm transition-colors flex items-center gap-2',
            selectedCategoryType === category.type
              ? 'border-red-600 text-red-600'
              : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
          ]"
        >
          <span class="text-base">{{ category.icon }}</span>
          <span>{{ category.name }}</span>
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

    <!-- 三列布局：账户列表 + 趋势图 + 历史记录 -->
    <div class="grid grid-cols-12 gap-6">
      <!-- 左侧：账户列表 -->
      <div class="col-span-12 lg:col-span-3">
        <div class="bg-white rounded-lg shadow border border-gray-200">
          <div class="px-4 py-3 border-b border-gray-200 flex items-center justify-between">
            <h2 class="text-sm font-semibold text-gray-900">账户列表</h2>
            <button
              @click="openAccountDialog()"
              class="text-red-600 hover:text-red-700 text-xs font-medium flex items-center gap-1"
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
              <button
                v-for="account in currentCategoryAccounts"
                :key="account.id"
                @click="selectAccount(account)"
                :class="[
                  'w-full px-3 py-1.5 text-left transition-colors border-l-4',
                  selectedAccount?.id === account.id
                    ? 'bg-red-50 border-red-600'
                    : 'hover:bg-gray-50 border-transparent'
                ]"
              >
                <div class="flex items-center justify-between gap-2">
                  <div class="flex-1 min-w-0">
                    <div class="font-medium text-gray-900 text-sm truncate flex items-center gap-1.5">
                      {{ account.accountName }}
                      <span v-if="!account.isActive" class="text-xs text-gray-400 bg-gray-100 px-1 py-0.5 rounded flex-shrink-0">停用</span>
                    </div>
                  </div>
                  <div v-if="account.latestBalance !== null" class="text-sm font-semibold text-red-600 flex-shrink-0">
                    {{ getCurrencySymbol(account.currency) }}{{ formatNumber(account.latestBalance) }}
                  </div>
                </div>
              </button>
            </div>

            <!-- 分类总和 -->
            <div class="px-4 py-3 border-t border-gray-200 bg-red-50">
              <div class="flex items-center justify-between">
                <span class="text-sm font-medium text-gray-700">{{ getCategoryName(selectedCategoryType) }}总计</span>
                <span class="text-base font-bold text-red-600">
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
          <div class="flex items-start justify-between gap-4">
            <div class="flex-1">
              <h2 class="text-lg font-semibold text-gray-900">{{ selectedAccount.accountName }}</h2>

              <!-- 详细信息网格 -->
              <div class="mt-3 grid grid-cols-2 gap-x-6 gap-y-2">
                <div class="flex items-center gap-2">
                  <span class="text-xs text-gray-500 w-16">家庭:</span>
                  <span class="text-sm text-gray-900">{{ getFamilyName(selectedFamilyId) }}</span>
                </div>

                <div class="flex items-center gap-2">
                  <span class="text-xs text-gray-500 w-16">用户:</span>
                  <span class="text-sm text-gray-900">{{ getUserName(selectedAccount.userId) }}</span>
                </div>

                <div class="flex items-center gap-2">
                  <span class="text-xs text-gray-500 w-16">分类:</span>
                  <span class="text-sm text-gray-900">{{ selectedAccount.liabilityTypeName }}</span>
                </div>

                <div class="flex items-center gap-2">
                  <span class="text-xs text-gray-500 w-16">币种:</span>
                  <span class="text-sm text-gray-900">{{ getCurrencyLabel(selectedAccount.currency) }}</span>
                </div>

                <div v-if="selectedAccount.institution" class="flex items-center gap-2">
                  <span class="text-xs text-gray-500 w-16">机构:</span>
                  <span class="text-sm text-gray-900">{{ selectedAccount.institution }}</span>
                </div>

                <div v-if="selectedAccount.linkedAssetAccountName" class="flex items-center gap-2">
                  <span class="text-xs text-gray-500 w-16">关联资产:</span>
                  <span class="text-sm text-gray-900">{{ selectedAccount.linkedAssetAccountName }}</span>
                </div>
              </div>
            </div>

            <div class="flex gap-2 flex-shrink-0">
              <button
                @click="openAccountDialog(selectedAccount)"
                class="px-4 py-2 border border-blue-600 text-blue-600 rounded-lg hover:bg-blue-50 text-sm font-medium flex items-center gap-2"
                title="编辑账户"
              >
                <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                  <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                </svg>
                编辑
              </button>

              <button
                v-if="selectedAccount.isActive"
                @click="disableAccount(selectedAccount)"
                class="px-4 py-2 border border-orange-600 text-orange-600 rounded-lg hover:bg-orange-50 text-sm font-medium flex items-center gap-2"
                title="停用账户"
              >
                <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="12" r="10"></circle>
                  <line x1="4.93" y1="4.93" x2="19.07" y2="19.07"></line>
                </svg>
                停用
              </button>
            </div>
          </div>
        </div>

        <!-- 两列：趋势图 + 历史记录 -->
        <div class="grid grid-cols-1 xl:grid-cols-2 gap-6">
          <!-- 趋势图 -->
          <div class="bg-white rounded-lg shadow border border-gray-200 p-6">
            <div class="flex items-center justify-between mb-4">
              <h3 class="text-base font-semibold text-gray-900">负债趋势</h3>
              <div class="flex gap-2">
                <button
                  v-for="range in timeRanges"
                  :key="range.value"
                  @click="selectedTimeRange = range.value"
                  :class="[
                    'px-2 py-1 text-xs rounded-md font-medium transition-colors',
                    selectedTimeRange === range.value
                      ? 'bg-red-600 text-white'
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
            <div class="px-4 py-2.5 border-b border-gray-200 flex items-center justify-between">
              <h3 class="text-base font-semibold text-gray-900">历史记录</h3>
              <button
                @click="openCreateDialog"
                class="px-3 py-1.5 bg-red-600 text-white rounded-lg hover:bg-red-700 text-xs font-medium flex items-center gap-1.5"
              >
                <svg xmlns="http://www.w3.org/2000/svg" class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <line x1="12" y1="5" x2="12" y2="19"></line>
                  <line x1="5" y1="12" x2="19" y2="12"></line>
                </svg>
                添加记录
              </button>
            </div>

            <div v-if="loadingRecords" class="text-center py-8 text-gray-500 text-sm">
              加载中...
            </div>

            <div v-else-if="filteredRecords.length === 0" class="text-center py-8 text-gray-500 text-sm">
              暂无记录
            </div>

            <!-- 紧凑表格布局 -->
            <div v-else class="overflow-x-auto">
              <div class="overflow-y-auto max-h-96">
                <table class="min-w-full divide-y divide-gray-200">
                  <thead class="bg-gray-50 sticky top-0">
                    <tr>
                      <th class="px-3 py-1.5 text-left text-xs font-medium text-gray-500">日期</th>
                      <th class="px-3 py-1.5 text-right text-xs font-medium text-gray-500">余额</th>
                      <th class="px-3 py-1.5 text-right text-xs font-medium text-gray-500">操作</th>
                    </tr>
                  </thead>
                  <tbody class="bg-white divide-y divide-gray-200">
                    <tr v-for="record in filteredRecords" :key="record.id" class="hover:bg-gray-50">
                      <td class="px-3 py-1.5 text-sm text-gray-900">{{ formatDate(record.recordDate) }}</td>
                      <td class="px-3 py-1.5 text-sm text-gray-900 text-right font-medium">
                        {{ getCurrencySymbol(record.currency) }}{{ formatNumber(record.balanceInBaseCurrency || record.outstandingBalance || 0) }}
                      </td>
                      <td class="px-3 py-1.5 text-sm text-right">
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

    <!-- 创建/编辑账户对话框 - 移动端全屏 -->
    <div
      v-if="showAccountDialog"
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-0 md:p-4"
      @click.self="closeAccountDialog"
    >
      <div class="bg-white h-full md:h-auto md:rounded-lg w-full max-w-md flex flex-col overflow-hidden">
        <!-- 标题栏 -->
        <div class="flex-shrink-0 px-4 md:px-6 py-4 border-b border-gray-200">
          <h2 class="text-lg md:text-xl font-bold">
            {{ editingAccount ? '编辑账户' : '添加账户' }}
          </h2>
        </div>

        <!-- 滚动内容 -->
        <div class="flex-1 overflow-y-auto">
          <form @submit.prevent="submitAccountForm" class="p-4 md:p-6 space-y-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">
                负债分类 *
              </label>
              <div class="w-full px-3 py-2 bg-gray-50 border border-gray-300 rounded-lg text-gray-900 min-h-[44px] flex items-center">
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
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-red-600 min-h-[44px]"
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
                placeholder="例如：招商银行信用卡"
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-red-600 min-h-[44px]"
              />
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">币种 *</label>
              <select
                v-model="accountFormData.currency"
                required
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-red-600 min-h-[44px]"
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
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-red-600 min-h-[44px]"
              />
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">账号</label>
              <input
                v-model="accountFormData.accountNumber"
                type="text"
                placeholder="例如：**** **** **** 1234"
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-red-600 min-h-[44px]"
              />
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">关联资产账户</label>
              <select
                v-model="accountFormData.linkedAssetAccountId"
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-red-600 min-h-[44px]"
              >
                <option :value="null">无关联</option>
                <option v-for="asset in assetAccounts" :key="asset.id" :value="asset.id">
                  {{ asset.accountName }} ({{ asset.assetTypeName }})
                </option>
              </select>
              <p class="text-xs text-gray-500 mt-1">
                用于房贷等负债关联对应的资产（如房产）
              </p>
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">备注</label>
              <textarea
                v-model="accountFormData.notes"
                rows="3"
                placeholder="添加备注信息"
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-red-600"
              ></textarea>
            </div>
          </form>
        </div>

        <!-- 固定底部按钮 -->
        <div class="flex-shrink-0 px-4 md:px-6 py-4 border-t border-gray-200 bg-white">
          <div class="flex gap-3">
            <button
              @click="submitAccountForm"
              class="flex-1 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 font-medium min-h-[44px]"
            >
              {{ editingAccount ? '保存' : '创建' }}
            </button>
            <button
              type="button"
              @click="closeAccountDialog"
              class="flex-1 px-4 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 font-medium min-h-[44px]"
            >
              取消
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 创建/编辑记录对话框 - 移动端全屏 -->
    <div
      v-if="showDialog"
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-0 md:p-4"
      @click.self="closeDialog"
    >
      <div class="bg-white h-full md:h-auto md:rounded-lg w-full max-w-md flex flex-col overflow-hidden">
        <!-- 标题栏 -->
        <div class="flex-shrink-0 px-4 md:px-6 py-4 border-b border-gray-200">
          <h2 class="text-lg md:text-xl font-bold">
            {{ editingRecord ? '编辑记录' : '添加历史记录' }}
          </h2>
        </div>

        <!-- 滚动内容 -->
        <div class="flex-1 overflow-y-auto">
          <form @submit.prevent="submitForm" class="p-4 md:p-6 space-y-4" id="recordForm">
            <!-- 账户信息显示 -->
            <div class="bg-gray-50 rounded-lg p-3 border border-gray-200">
              <div class="text-xs text-gray-500 mb-1">账户信息</div>
              <div class="space-y-1">
                <div class="text-sm font-medium text-gray-900">{{ selectedAccount?.accountName }}</div>
                <div class="text-xs text-gray-600 flex items-center gap-2">
                  <span>{{ getFamilyName(selectedFamilyId) }}</span>
                  <span class="text-gray-300">•</span>
                  <span>{{ getUserName(selectedAccount?.userId) }}</span>
                  <span class="text-gray-300">•</span>
                  <span>{{ getCategoryName(selectedCategoryType) }}</span>
                </div>
              </div>
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">
                记录日期 *
              </label>
              <input
                v-model="formData.recordDate"
                type="date"
                required
                :max="new Date().toISOString().split('T')[0]"
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-red-600 min-h-[44px]"
              />
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">余额 *</label>
              <input
                v-model="formData.balance"
                type="number"
                step="0.01"
                required
                placeholder="例如：10000.00"
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-red-600 min-h-[44px]"
              />
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">币种</label>
              <div class="w-full px-3 py-2 bg-gray-100 border border-gray-300 rounded-lg text-gray-700 min-h-[44px] flex items-center">
                {{ getCurrencyLabel(selectedAccount?.currency) }}
              </div>
              <p class="text-xs text-gray-500 mt-1">币种跟随账户设置，不可修改</p>
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">备注</label>
              <textarea
                v-model="formData.notes"
                rows="3"
                placeholder="添加备注信息"
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-red-600"
              ></textarea>
            </div>
          </form>
        </div>

        <!-- 固定底部按钮 -->
        <div class="flex-shrink-0 px-4 md:px-6 py-4 border-t border-gray-200 bg-white">
          <div class="flex gap-3">
            <button
              type="submit"
              form="recordForm"
              class="flex-1 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 font-medium min-h-[44px]"
            >
              {{ editingRecord ? '保存' : '创建' }}
            </button>
            <button
              type="button"
              @click="closeDialog"
              class="flex-1 px-4 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 font-medium min-h-[44px]"
            >
              取消
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { liabilityTypeAPI, liabilityAccountAPI, liabilityRecordAPI } from '@/api/liability'
import { assetAccountAPI } from '@/api/asset'
import { familyAPI } from '@/api/family'
import { Chart, registerables } from 'chart.js'
import 'chartjs-adapter-date-fns'

Chart.register(...registerables)

// 负债分类类型映射
const CATEGORY_TYPE_NAMES = {
  'MORTGAGE': '房贷',
  'AUTO_LOAN': '车贷',
  'CREDIT_CARD': '信用卡',
  'PERSONAL_LOAN': '个人借款',
  'STUDENT_LOAN': '学生贷款',
  'BUSINESS_LOAN': '商业贷款',
  'OTHER': '其他'
}

// 家庭相关
const families = ref([])
const selectedFamilyId = ref(null)

const categories = ref([])
const allCategories = ref([])  // 存储完整的分类信息（包含ID）
const selectedCategoryType = ref('CREDIT_CARD')
const accounts = ref([])
const selectedAccount = ref(null)
const records = ref([])
const users = ref([])
const assetAccounts = ref([])
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

const getLocalDateString = () => {
  const today = new Date()
  const localDate = new Date(today.getTime() - today.getTimezoneOffset() * 60000)
  return localDate.toISOString().split('T')[0]
}

const formData = ref({
  recordDate: getLocalDateString(),
  balance: '',
  currency: 'CNY',
  notes: ''
})

const accountFormData = ref({
  accountName: '',
  currency: 'CNY',
  institution: '',
  accountNumber: '',
  notes: '',
  userId: 1,
  linkedAssetAccountId: null
})

// 当前分类下的账户
const currentCategoryAccounts = computed(() => {
  return accounts.value.filter(account => account.liabilityTypeCode === selectedCategoryType.value)
})

// 当前分类的总和（基准货币）
const categoryTotal = computed(() => {
  return currentCategoryAccounts.value.reduce((total, account) => {
    // 确保正确处理 null/undefined 和数字类型转换
    const balance = account.latestBalanceInBaseCurrency
    if (balance === null || balance === undefined) {
      return total
    }
    return total + Number(balance)
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

// 获取家庭名称
const getFamilyName = (familyId) => {
  const family = families.value.find(f => f.id === familyId)
  return family ? family.familyName : '未知家庭'
}

// 获取货币标签
const getCurrencyLabel = (currency) => {
  const currencyLabels = {
    'CNY': '人民币 (CNY)',
    'USD': '美元 (USD)',
    'EUR': '欧元 (EUR)',
    'GBP': '英镑 (GBP)',
    'JPY': '日元 (JPY)',
    'HKD': '港币 (HKD)'
  }
  return currencyLabels[currency] || currency
}

// 加载负债分类
const loadCategories = async () => {
  try {
    // 加载负债类型（7个大类）
    const response = await liabilityTypeAPI.getAll()
    if (response.success && response.data) {
      // 存储完整的负债类型信息（包含ID）
      allCategories.value = response.data

      // 转换为Tab显示用的分类对象（包含图标）
      categories.value = response.data.map(liabilityType => ({
        type: liabilityType.categoryType,
        name: liabilityType.categoryName,
        icon: liabilityType.categoryIcon  // 后端返回的字段是 categoryIcon
      }))

      // 如果有分类，默认选中第一个
      if (categories.value.length > 0) {
        selectedCategoryType.value = categories.value[0].type
      }
    }
  } catch (error) {
    console.error('加载负债类型失败:', error)
  }
}

// 加载家庭列表
const loadFamilies = async () => {
  try {
    const response = await familyAPI.getDefault()
    if (response.success) {
      families.value = response.data ? [response.data] : []
    }

    // 如果selectedFamilyId还未设置，获取默认家庭
    if (!selectedFamilyId.value) {
      try {
        const defaultResponse = await familyAPI.getDefault()
        if (defaultResponse.success && defaultResponse.data) {
          selectedFamilyId.value = defaultResponse.data.id
        } else if (families.value.length > 0) {
          selectedFamilyId.value = families.value[0].id
        }
      } catch (err) {
        console.error('获取默认家庭失败:', err)
        if (families.value.length > 0) {
          selectedFamilyId.value = families.value[0].id
        }
      }
    }
  } catch (error) {
    console.error('加载家庭列表失败:', error)
  }
}

// 加载用户列表（只加载当前家庭的成员）
const loadUsers = async () => {
  if (!selectedFamilyId.value) return

  try {
    const response = await familyAPI.getMembers(selectedFamilyId.value)
    if (response.success) {
      users.value = response.data
    }
  } catch (error) {
    console.error('加载用户失败:', error)
  }
}

// 加载账户列表
const loadAccounts = async () => {
  if (!selectedFamilyId.value) return

  loadingAccounts.value = true
  try {
    const response = await liabilityAccountAPI.getAllByFamily(selectedFamilyId.value)
    if (response.success) {
      accounts.value = response.data
    }
  } catch (error) {
    console.error('加载账户失败:', error)
  } finally {
    loadingAccounts.value = false
  }
}

// 加载资产账户列表（用于关联选择）
const loadAssetAccounts = async () => {
  if (!selectedFamilyId.value) return

  try {
    const response = await assetAccountAPI.getAllByFamily(selectedFamilyId.value)
    if (response.success) {
      assetAccounts.value = response.data
    }
  } catch (error) {
    console.error('加载资产账户失败:', error)
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
    const response = await liabilityRecordAPI.getByAccountId(selectedAccount.value.id)
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
    y: r.balanceInBaseCurrency || r.outstandingBalance || 0
  }))

  if (chartInstance) {
    chartInstance.destroy()
  }

  const ctx = chartCanvas.value.getContext('2d')
  chartInstance = new Chart(ctx, {
    type: 'line',
    data: {
      datasets: [{
        label: '负债余额',
        data: data,
        borderColor: 'rgb(220, 38, 38)',
        backgroundColor: 'rgba(220, 38, 38, 0.1)',
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
              return '余额: $' + formatNumber(context.parsed.y)
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
            text: '余额'
          },
          ticks: {
            callback: function(value) {
              return '$' + formatNumber(value)
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
    recordDate: getLocalDateString(),
    balance: '',
    currency: selectedAccount.value?.currency || 'CNY',
    notes: ''
  }
  showDialog.value = true
}

// 编辑记录
const editRecord = (record) => {
  editingRecord.value = record

  // 处理日期格式 - 确保转换为 YYYY-MM-DD 字符串
  let dateStr = record.recordDate
  if (Array.isArray(record.recordDate)) {
    // 如果是数组格式 [year, month, day]
    const [year, month, day] = record.recordDate
    dateStr = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
  } else if (typeof record.recordDate === 'string' && record.recordDate.includes('T')) {
    // 如果是 ISO 8601 格式，只取日期部分
    dateStr = record.recordDate.split('T')[0]
  }

  formData.value = {
    recordDate: dateStr,
    balance: record.outstandingBalance || 0,
    currency: record.currency,
    notes: record.notes || ''
  }
  showDialog.value = true
}

// 删除记录
const deleteRecord = async (record) => {
  if (!confirm(`确定要删除 ${formatDate(record.recordDate)} 的记录吗？`)) return

  try {
    const response = await liabilityRecordAPI.delete(record.id)
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
      outstandingBalance: parseFloat(formData.value.balance),
      currency: formData.value.currency,
      notes: formData.value.notes,
      exchangeRate: 1.0
    }

    let response
    if (editingRecord.value) {
      response = await liabilityRecordAPI.update(editingRecord.value.id, data)
    } else {
      response = await liabilityRecordAPI.create(data)
    }

    if (response.success) {
      closeDialog()
      await loadRecords()
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
const openAccountDialog = async (account = null) => {
  // 加载资产账户列表
  await loadAssetAccounts()

  editingAccount.value = account
  if (account) {
    accountFormData.value = {
      accountName: account.accountName,
      currency: account.currency,
      institution: account.institution || '',
      accountNumber: account.accountNumber || '',
      notes: account.notes || '',
      userId: account.userId,
      liabilityTypeId: account.liabilityTypeId,  // 保存 liabilityTypeId 用于更新
      linkedAssetAccountId: account.linkedAssetAccountId || null
    }
  } else {
    accountFormData.value = {
      accountName: '',
      currency: 'CNY',
      institution: '',
      accountNumber: '',
      notes: '',
      userId: users.value.length > 0 ? users.value[0].id : 1,
      linkedAssetAccountId: null
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
      // 编辑：使用已保存的 liabilityTypeId
      data = {
        ...accountFormData.value,
        isActive: true
      }
    } else {
      // 创建：根据 categoryType 查找 liabilityTypeId
      const liabilityType = allCategories.value.find(cat => cat.categoryType === selectedCategoryType.value)
      if (!liabilityType) {
        alert('未找到对应的负债类型，请刷新页面重试')
        return
      }

      data = {
        ...accountFormData.value,
        liabilityTypeId: liabilityType.categoryId,  // categoryId 就是 liabilityType 的 ID
        isActive: true
      }
    }

    let response
    if (editingAccount.value) {
      response = await liabilityAccountAPI.update(editingAccount.value.id, data)
    } else {
      response = await liabilityAccountAPI.create(data)
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
    const response = await liabilityAccountAPI.update(account.id, data)
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

// 家庭切换事件处理
const onFamilyChange = () => {
  // 重新加载用户列表和账户列表
  loadUsers()
  loadAccounts()
  // 清空当前选中的账户和记录
  selectedAccount.value = null
  records.value = []
  if (chartInstance) {
    chartInstance.destroy()
    chartInstance = null
  }
}

// 监听selectedFamilyId变化，自动加载用户和账户数据
watch(selectedFamilyId, (newId) => {
  if (newId) {
    loadUsers()
    loadAccounts()
  }
})

onMounted(async () => {
  await loadFamilies()
  await loadCategories()
  // loadUsers 和 loadAccounts 将通过 watcher 自动调用
})
</script>
