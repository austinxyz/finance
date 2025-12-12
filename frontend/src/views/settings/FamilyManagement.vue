<template>
  <div class="min-h-screen bg-gray-50 sm:bg-transparent p-4 sm:p-6 space-y-4 sm:space-y-6">
    <!-- Header with Family Selector -->
    <div class="flex flex-col sm:flex-row items-start justify-between gap-3 bg-white sm:bg-transparent p-4 sm:p-0 rounded-lg sm:rounded-none shadow-sm sm:shadow-none">
      <div>
        <h2 class="text-2xl font-bold text-gray-900">家庭配置</h2>
        <p class="mt-1 text-sm text-gray-500">
          管理家庭财务信息和家庭成员
        </p>
      </div>
      <div class="flex flex-col sm:flex-row gap-2 sm:gap-3 w-full sm:w-auto sm:ml-4" v-if="!loading">
        <select
          v-model="selectedFamilyId"
          @change="onFamilyChange"
          class="w-full sm:w-auto px-3 py-2.5 sm:py-2 border border-gray-300 rounded-md focus:ring-green-500 focus:border-green-500 sm:min-w-[200px] text-base sm:text-sm"
        >
          <option :value="null">-- 请选择家庭 --</option>
          <option v-for="family in allFamilies" :key="family.id" :value="family.id">
            {{ family.familyName }}
          </option>
        </select>
        <button
          @click="showCreateFamilyDialog = true"
          class="w-full sm:w-auto px-4 py-2.5 sm:py-2 bg-green-600 text-white rounded-md text-sm font-medium hover:bg-green-700 active:bg-green-800 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500 whitespace-nowrap touch-manipulation"
        >
          + 新建家庭
        </button>
      </div>
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
          <button @click="loadAllFamilies" class="mt-2 text-sm font-medium text-red-600 hover:text-red-500">
            重试
          </button>
        </div>
      </div>
    </div>

    <div v-if="!loading" class="space-y-6">
      <!-- 两列布局 -->
      <div v-if="selectedFamilyId" class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <!-- 左列：家庭基本信息 -->
        <div class="bg-white shadow rounded-lg">
          <div class="px-6 py-5 border-b border-gray-200">
            <h3 class="text-lg font-medium text-gray-900">家庭基本信息</h3>
          </div>

          <form @submit.prevent="saveFamily" class="px-6 py-5 space-y-6">
            <!-- 家庭名称 -->
            <div>
              <label for="familyName" class="block text-sm font-medium text-gray-700 mb-1">
                家庭名称 <span class="text-red-500">*</span>
              </label>
              <input
                id="familyName"
                v-model="familyForm.familyName"
                type="text"
                required
                class="block w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-green-500 focus:border-green-500"
                placeholder="例如：张家"
              />
            </div>

            <!-- 家庭年度支出 -->
            <div>
              <label for="annualExpenses" class="block text-sm font-medium text-gray-700 mb-1">
                家庭年度支出 <span class="text-red-500">*</span>
              </label>
              <div class="flex space-x-2">
                <div class="relative flex-1">
                  <input
                    id="annualExpenses"
                    v-model.number="familyForm.annualExpenses"
                    type="number"
                    step="0.01"
                    min="0"
                    required
                    class="block w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-green-500 focus:border-green-500"
                    placeholder="0.00"
                  />
                </div>
                <select
                  v-model="familyForm.expensesCurrency"
                  class="px-3 py-2 border border-gray-300 rounded-md focus:ring-green-500 focus:border-green-500"
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
              <label for="emergencyMonths" class="block text-sm font-medium text-gray-700 mb-1">
                紧急储备月数
              </label>
              <input
                id="emergencyMonths"
                v-model.number="familyForm.emergencyFundMonths"
                type="number"
                min="3"
                max="24"
                class="block w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-green-500 focus:border-green-500"
                placeholder="6"
              />
              <p class="mt-1 text-xs text-gray-500">
                建议保持 3-24 个月的家庭生活费用作为紧急储备
              </p>
            </div>

            <!-- 财务目标 -->
            <div>
              <label for="goals" class="block text-sm font-medium text-gray-700 mb-1">
                家庭财务目标
              </label>
              <textarea
                id="goals"
                v-model="familyForm.financialGoals"
                rows="4"
                class="block w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-green-500 focus:border-green-500"
                placeholder="例如：&#10;1. 5年内购买首套房&#10;2. 子女教育基金储备&#10;3. 退休养老规划"
              ></textarea>
              <p class="mt-1 text-xs text-gray-500">
                记录家庭的财务目标和规划
              </p>
            </div>

            <!-- Action Buttons -->
            <div class="flex justify-end space-x-3 pt-4 border-t border-gray-200">
              <button
                type="button"
                @click="resetFamilyForm"
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

        <!-- 右列：家庭成员管理 -->
        <div class="bg-white shadow rounded-lg flex flex-col">
          <!-- 成员Tab列表 -->
          <div v-if="members.length === 0" class="px-6 py-12 text-center text-gray-500">
            暂无家庭成员，点击"添加成员"按钮添加
            <button
              @click="addNewMember"
              class="mt-4 px-3 py-2 bg-green-600 text-white rounded-md text-sm font-medium hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500"
            >
              + 添加成员
            </button>
          </div>

          <div v-else class="flex flex-col flex-1 min-h-0">
            <!-- Tab头部 with 添加按钮 -->
            <div class="flex border-b border-gray-200 overflow-x-auto flex-shrink-0">
              <button
                v-for="member in members"
                :key="member.id"
                @click="selectMemberTab(member)"
                :class="[
                  'px-4 py-3 text-sm font-medium border-b-2 whitespace-nowrap transition-colors',
                  selectedMember?.id === member.id
                    ? 'border-green-600 text-green-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                ]"
              >
                {{ member.fullName || member.username }}
              </button>
              <div class="ml-auto flex items-center px-3">
                <button
                  @click="addNewMember"
                  class="px-3 py-1.5 bg-green-600 text-white rounded-md text-sm font-medium hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500 whitespace-nowrap"
                >
                  + 添加成员
                </button>
              </div>
            </div>

            <!-- Tab内容 -->
            <div v-if="selectedMember" class="flex-1 overflow-y-auto px-6 py-5">
              <form @submit.prevent="saveMemberInfo" class="space-y-4">
                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-1">邮箱 <span class="text-red-500">*</span></label>
                  <input
                    v-model="memberForm.email"
                    type="email"
                    required
                    class="block w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-green-500 focus:border-green-500"
                  />
                </div>

                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-1">姓名</label>
                  <input
                    v-model="memberForm.fullName"
                    type="text"
                    class="block w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-green-500 focus:border-green-500"
                  />
                </div>

                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-1">出生年月</label>
                  <input
                    v-model="memberForm.birthDate"
                    type="month"
                    class="block w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-green-500 focus:border-green-500"
                  />
                </div>

                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-1">年收入</label>
                  <div class="flex space-x-2">
                    <input
                      v-model.number="memberForm.annualIncome"
                      type="number"
                      step="0.01"
                      min="0"
                      class="flex-1 px-3 py-2 border border-gray-300 rounded-md focus:ring-green-500 focus:border-green-500"
                      placeholder="0.00"
                    />
                    <select
                      v-model="memberForm.incomeCurrency"
                      class="px-3 py-2 border border-gray-300 rounded-md focus:ring-green-500 focus:border-green-500"
                    >
                      <option value="USD">$ USD</option>
                      <option value="CNY">¥ CNY</option>
                      <option value="EUR">€ EUR</option>
                      <option value="GBP">£ GBP</option>
                      <option value="JPY">¥ JPY</option>
                    </select>
                  </div>
                </div>

                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-2">风险承受能力</label>
                  <select
                    v-model="memberForm.riskTolerance"
                    class="block w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-green-500 focus:border-green-500"
                  >
                    <option value="">-- 请选择 --</option>
                    <option value="CONSERVATIVE">保守型</option>
                    <option value="MODERATE">稳健型</option>
                    <option value="AGGRESSIVE">进取型</option>
                  </select>
                </div>

                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-1">备注</label>
                  <textarea
                    v-model="memberForm.notes"
                    rows="3"
                    class="block w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-green-500 focus:border-green-500"
                  ></textarea>
                </div>

                <div class="flex justify-between space-x-3 pt-4 border-t border-gray-200">
                  <button
                    type="button"
                    @click="removeMember(selectedMember)"
                    class="px-4 py-2 border border-red-300 rounded-md text-sm font-medium text-red-600 hover:bg-red-50"
                  >
                    移除成员
                  </button>
                  <button
                    type="submit"
                    :disabled="savingMember"
                    class="px-4 py-2 bg-green-600 text-white rounded-md text-sm font-medium hover:bg-green-700 disabled:opacity-50"
                  >
                    {{ savingMember ? '保存中...' : '保存成员信息' }}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 添加成员对话框 -->
    <div v-if="showAddMemberDialog" class="fixed inset-0 bg-black bg-opacity-50 flex items-end sm:items-center justify-center z-50 p-0 sm:p-4">
      <div class="bg-white rounded-t-2xl sm:rounded-lg shadow-xl w-full sm:max-w-md max-h-[90vh] overflow-y-auto animate-slide-up">
        <div class="px-6 py-4 border-b border-gray-200">
          <h3 class="text-lg font-medium text-gray-900">添加成员</h3>
        </div>

        <form @submit.prevent="saveNewMember" class="px-6 py-4 space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">用户名 <span class="text-red-500">*</span></label>
            <input
              v-model="newMemberForm.username"
              type="text"
              required
              class="block w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-green-500 focus:border-green-500"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">邮箱 <span class="text-red-500">*</span></label>
            <input
              v-model="newMemberForm.email"
              type="email"
              required
              class="block w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-green-500 focus:border-green-500"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">密码 <span class="text-red-500">*</span></label>
            <input
              v-model="newMemberForm.password"
              type="password"
              required
              class="block w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-green-500 focus:border-green-500"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">姓名</label>
            <input
              v-model="newMemberForm.fullName"
              type="text"
              class="block w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-green-500 focus:border-green-500"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">出生年月</label>
            <input
              v-model="newMemberForm.birthDate"
              type="month"
              class="block w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-green-500 focus:border-green-500"
            />
          </div>

          <div class="flex justify-end space-x-3 pt-4 border-t border-gray-200">
            <button
              type="button"
              @click="closeAddMemberDialog"
              class="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-50"
            >
              取消
            </button>
            <button
              type="submit"
              :disabled="savingMember"
              class="px-4 py-2 bg-green-600 text-white rounded-md text-sm font-medium hover:bg-green-700 disabled:opacity-50"
            >
              {{ savingMember ? '添加中...' : '添加' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- 创建新家庭对话框 -->
    <div v-if="showCreateFamilyDialog" class="fixed inset-0 bg-black bg-opacity-50 flex items-end sm:items-center justify-center z-50 p-0 sm:p-4">
      <div class="bg-white rounded-t-2xl sm:rounded-lg shadow-xl w-full sm:max-w-md animate-slide-up">
        <div class="px-6 py-4 border-b border-gray-200">
          <h3 class="text-lg font-medium text-gray-900">创建新家庭</h3>
        </div>

        <form @submit.prevent="createNewFamily" class="px-6 py-4 space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              家庭名称 <span class="text-red-500">*</span>
            </label>
            <input
              v-model="newFamilyForm.familyName"
              type="text"
              required
              class="block w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-green-500 focus:border-green-500"
              placeholder="例如：张家"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              家庭年度支出
            </label>
            <div class="relative">
              <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                <span class="text-gray-500 sm:text-sm">¥</span>
              </div>
              <input
                v-model.number="newFamilyForm.annualExpenses"
                type="number"
                step="0.01"
                min="0"
                class="block w-full pl-8 pr-3 py-2 border border-gray-300 rounded-md focus:ring-green-500 focus:border-green-500"
                placeholder="0.00"
              />
            </div>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              紧急储备月数
            </label>
            <input
              v-model.number="newFamilyForm.emergencyFundMonths"
              type="number"
              min="3"
              max="24"
              class="block w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-green-500 focus:border-green-500"
              placeholder="6"
            />
          </div>

          <div class="flex justify-end space-x-3 pt-4 border-t border-gray-200">
            <button
              type="button"
              @click="closeCreateFamilyDialog"
              class="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-50"
            >
              取消
            </button>
            <button
              type="submit"
              :disabled="creatingFamily"
              class="px-4 py-2 bg-green-600 text-white rounded-md text-sm font-medium hover:bg-green-700 disabled:opacity-50"
            >
              {{ creatingFamily ? '创建中...' : '创建' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Success Message -->
    <div v-if="showSuccess" class="fixed bottom-4 right-4 bg-green-50 border border-green-200 rounded-lg shadow-lg p-4 max-w-sm animate-slide-up z-50">
      <div class="flex items-start">
        <svg class="w-5 h-5 text-green-400 mt-0.5" fill="currentColor" viewBox="0 0 20 20">
          <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/>
        </svg>
        <div class="ml-3">
          <h3 class="text-sm font-medium text-green-800">{{ successMessage }}</h3>
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
const savingMember = ref(false)
const creatingFamily = ref(false)
const error = ref(null)
const showSuccess = ref(false)
const successMessage = ref('')

const showAddMemberDialog = ref(false)
const showCreateFamilyDialog = ref(false)

// 所有家庭列表
const allFamilies = ref([])
// 当前选中的家庭ID
const selectedFamilyId = ref(null)
// 当前选中的成员
const selectedMember = ref(null)

// 当前用户ID（简化处理，实际应从登录状态获取）
const currentUserId = 2 // AustinXu

const familyForm = ref({
  id: null,
  familyName: '',
  annualExpenses: 0,
  expensesCurrency: 'USD',
  emergencyFundMonths: 6,
  financialGoals: ''
})

const originalFamilyForm = ref(null)

const members = ref([])

const memberForm = ref({
  username: '',
  email: '',
  fullName: '',
  birthDate: null,
  annualIncome: null,
  incomeCurrency: 'USD',
  riskTolerance: '',
  notes: ''
})

const newMemberForm = ref({
  username: '',
  email: '',
  password: '',
  fullName: '',
  birthDate: null,
  annualIncome: null,
  incomeCurrency: 'USD',
  riskTolerance: '',
  notes: ''
})

const newFamilyForm = ref({
  familyName: '',
  annualExpenses: 0,
  expensesCurrency: 'USD',
  emergencyFundMonths: 6,
  financialGoals: ''
})

// 加载所有家庭列表
const loadAllFamilies = async () => {
  loading.value = true
  error.value = null

  try {
    const response = await request.get('/family')

    if (response.success && response.data) {
      allFamilies.value = response.data

      // 自动选择当前用户的家庭
      const userResponse = await request.get(`/family/user/${currentUserId}`)
      if (userResponse.success && userResponse.data) {
        selectedFamilyId.value = userResponse.data.id
        await loadFamilyDetails(selectedFamilyId.value)
      }
    }
  } catch (err) {
    console.error('加载家庭列表失败:', err)
    error.value = err.response?.data?.message || '加载家庭列表失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

// 加载指定家庭的详细信息
const loadFamilyDetails = async (familyId) => {
  if (!familyId) {
    familyForm.value = {
      id: null,
      familyName: '',
      annualExpenses: 0,
      expensesCurrency: 'USD',
      emergencyFundMonths: 6,
      financialGoals: ''
    }
    members.value = []
    selectedMember.value = null
    return
  }

  try {
    const response = await request.get(`/family/${familyId}`)

    if (response.success && response.data) {
      const family = response.data
      familyForm.value = {
        id: family.id,
        familyName: family.familyName || '',
        annualExpenses: family.annualExpenses || 0,
        expensesCurrency: family.expensesCurrency || 'USD',
        emergencyFundMonths: family.emergencyFundMonths || 6,
        financialGoals: family.financialGoals || ''
      }
      originalFamilyForm.value = JSON.parse(JSON.stringify(familyForm.value))

      // 加载家庭成员
      await loadMembers(family.id)
    }
  } catch (err) {
    console.error('加载家庭配置失败:', err)
    error.value = err.response?.data?.message || '加载家庭配置失败，请稍后重试'
  }
}

// 家庭选择变更处理
const onFamilyChange = async () => {
  await loadFamilyDetails(selectedFamilyId.value)
}

// 加载家庭成员
const loadMembers = async (familyId) => {
  try {
    const response = await request.get(`/family/${familyId}/members`)
    if (response.success && response.data) {
      members.value = response.data
      // 自动选择第一个成员
      if (members.value.length > 0) {
        selectMemberTab(members.value[0])
      } else {
        selectedMember.value = null
      }
    }
  } catch (err) {
    console.error('加载家庭成员失败:', err)
    members.value = []
    selectedMember.value = null
  }
}

// 选择成员Tab
const selectMemberTab = (member) => {
  selectedMember.value = member
  // 将完整日期转换为年月格式 (YYYY-MM-DD -> YYYY-MM)
  const birthDate = member.birthDate ? member.birthDate.substring(0, 7) : null

  memberForm.value = {
    username: member.username,
    email: member.email,
    fullName: member.fullName || '',
    birthDate: birthDate,
    annualIncome: member.annualIncome || null,
    incomeCurrency: member.incomeCurrency || 'USD',
    riskTolerance: member.riskTolerance || '',
    notes: member.notes || ''
  }
}

// 保存家庭信息
const saveFamily = async () => {
  saving.value = true
  error.value = null

  try {
    const response = await request.post(`/family/${familyForm.value.id}`, familyForm.value)

    if (response.success) {
      originalFamilyForm.value = JSON.parse(JSON.stringify(familyForm.value))
      showSuccessMessage('家庭信息保存成功')
    }
  } catch (err) {
    console.error('保存家庭配置失败:', err)
    error.value = err.response?.data?.message || '保存失败，请稍后重试'
  } finally {
    saving.value = false
  }
}

// 重置家庭表单
const resetFamilyForm = () => {
  if (originalFamilyForm.value) {
    familyForm.value = JSON.parse(JSON.stringify(originalFamilyForm.value))
  }
}

// 保存成员信息
const saveMemberInfo = async () => {
  if (!selectedMember.value) return

  savingMember.value = true

  try {
    // 转换年月格式为完整日期 (YYYY-MM -> YYYY-MM-01)
    const dataToSend = {
      ...memberForm.value,
      birthDate: memberForm.value.birthDate ? `${memberForm.value.birthDate}-01` : null
    }

    await request.put(`/users/${selectedMember.value.id}`, dataToSend)
    showSuccessMessage('成员信息更新成功')
    // 重新加载成员列表
    await loadMembers(familyForm.value.id)
  } catch (err) {
    console.error('保存成员失败:', err)
    error.value = err.response?.data?.message || '保存成员失败'
  } finally {
    savingMember.value = false
  }
}

// 添加新成员
const addNewMember = () => {
  showAddMemberDialog.value = true
  newMemberForm.value = {
    username: '',
    email: '',
    password: '',
    fullName: '',
    birthDate: null,
    annualIncome: null,
    incomeCurrency: 'USD',
    riskTolerance: '',
    notes: ''
  }
}

// 保存新成员
const saveNewMember = async () => {
  savingMember.value = true

  try {
    // 转换年月格式为完整日期 (YYYY-MM -> YYYY-MM-01)
    const newMember = {
      ...newMemberForm.value,
      birthDate: newMemberForm.value.birthDate ? `${newMemberForm.value.birthDate}-01` : null,
      familyId: familyForm.value.id
    }
    await request.post('/users', newMember)
    showSuccessMessage('成员添加成功')
    // 重新加载成员列表
    await loadMembers(familyForm.value.id)
    closeAddMemberDialog()
  } catch (err) {
    console.error('添加成员失败:', err)
    error.value = err.response?.data?.message || '添加成员失败'
  } finally {
    savingMember.value = false
  }
}

// 关闭添加成员对话框
const closeAddMemberDialog = () => {
  showAddMemberDialog.value = false
}

// 移除成员（软删除）
const removeMember = async (member) => {
  if (!confirm(`确定要将 ${member.fullName || member.username} 移出家庭吗？`)) {
    return
  }

  try {
    await request.delete(`/users/${member.id}`)
    showSuccessMessage('成员已移除')
    await loadMembers(familyForm.value.id)
  } catch (err) {
    console.error('移除成员失败:', err)
    error.value = err.response?.data?.message || '移除成员失败'
  }
}

// 创建新家庭
const createNewFamily = async () => {
  creatingFamily.value = true

  try {
    const response = await request.post('/family', newFamilyForm.value)

    if (response.success) {
      showSuccessMessage('家庭创建成功')
      // 重新加载家庭列表
      await loadAllFamilies()
      // 选择新创建的家庭
      selectedFamilyId.value = response.data.id
      await loadFamilyDetails(selectedFamilyId.value)
      closeCreateFamilyDialog()
    }
  } catch (err) {
    console.error('创建家庭失败:', err)
    error.value = err.response?.data?.message || '创建家庭失败，请稍后重试'
  } finally {
    creatingFamily.value = false
  }
}

// 关闭创建家庭对话框
const closeCreateFamilyDialog = () => {
  showCreateFamilyDialog.value = false
  newFamilyForm.value = {
    familyName: '',
    annualExpenses: 0,
    expensesCurrency: 'USD',
    emergencyFundMonths: 6,
    financialGoals: ''
  }
}

// 获取风险承受能力标签
const getRiskToleranceLabel = (value) => {
  const map = {
    'CONSERVATIVE': '保守型',
    'MODERATE': '稳健型',
    'AGGRESSIVE': '进取型'
  }
  return map[value] || value
}

// 显示成功消息
const showSuccessMessage = (message) => {
  successMessage.value = message
  showSuccess.value = true
  setTimeout(() => {
    showSuccess.value = false
  }, 3000)
}

onMounted(() => {
  loadAllFamilies()
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

/* 移动端触摸优化 */
@media (max-width: 640px) {
  input, textarea, select, button {
    font-size: 16px; /* 防止iOS自动缩放 */
  }
}

.touch-manipulation {
  touch-action: manipulation;
}
</style>
