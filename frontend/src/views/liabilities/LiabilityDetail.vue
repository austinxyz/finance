<template>
  <div class="p-3 md:p-6 space-y-4 md:space-y-6">
    <!-- 返回按钮 -->
    <button @click="router.back()" class="flex items-center text-gray-600 hover:text-gray-900 text-sm md:text-base">
      <ChevronLeft class="w-4 h-4 md:w-5 md:h-5" />
      <span>返回</span>
    </button>

    <div v-if="loading" class="flex justify-center py-12">
      <div class="text-gray-500">加载中...</div>
    </div>

    <div v-else-if="account">
      <!-- 账户信息 -->
      <Card>
        <CardHeader>
          <div class="flex justify-between items-start">
            <div>
              <CardTitle class="text-lg md:text-2xl">{{ account.accountName }}</CardTitle>
              <p class="text-xs md:text-sm text-gray-500 mt-1">{{ account.categoryName }}</p>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          <div class="grid grid-cols-2 md:grid-cols-4 gap-3 md:gap-4">
            <div>
              <div class="text-xs md:text-sm text-gray-600">机构</div>
              <div class="text-sm md:text-base font-medium">{{ account.institution || '-' }}</div>
            </div>
            <div>
              <div class="text-xs md:text-sm text-gray-600">账号</div>
              <div class="text-sm md:text-base font-medium truncate">{{ maskAccountNumber(account.accountNumber) || '-' }}</div>
            </div>
            <div>
              <div class="text-xs md:text-sm text-gray-600">币种</div>
              <div class="text-sm md:text-base font-medium">{{ account.currency }}</div>
            </div>
            <div>
              <div class="text-xs md:text-sm text-gray-600">当前余额</div>
              <div class="text-lg md:text-xl font-bold text-red-600">
                {{ getCurrencySymbol(account.currency) }}{{ formatAmount(account.latestBalance) }}
              </div>
            </div>
          </div>
          <div v-if="account.interestRate || account.monthlyPayment" class="mt-4 pt-4 border-t">
            <div class="grid grid-cols-2 gap-4">
              <div v-if="account.interestRate">
                <div class="text-sm text-gray-600">利率</div>
                <div class="font-medium">{{ (account.interestRate * 100).toFixed(2) }}%</div>
              </div>
              <div v-if="account.monthlyPayment">
                <div class="text-sm text-gray-600">月供</div>
                <div class="font-medium">{{ getCurrencySymbol(account.currency) }}{{ formatAmount(account.monthlyPayment) }}</div>
              </div>
            </div>
          </div>
          <div v-if="account.notes" class="mt-4 pt-4 border-t">
            <div class="text-sm text-gray-600">备注</div>
            <div class="text-sm mt-1">{{ account.notes }}</div>
          </div>
        </CardContent>
      </Card>

      <!-- 负债记录 -->
      <div class="space-y-3 md:space-y-4">
        <div class="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-3">
          <h2 class="text-lg md:text-xl font-bold">负债记录</h2>
          <Button @click="showAddRecordDialog = true" class="bg-red-600 hover:bg-red-700 text-sm md:text-base">
            <Plus class="w-3 h-3 md:w-4 md:h-4 mr-1 md:mr-2" />
            添加记录
          </Button>
        </div>

        <div v-if="records.length === 0" class="text-center py-8 md:py-12 bg-gray-50 rounded-lg">
          <p class="text-sm md:text-base text-gray-500">暂无记录数据</p>
        </div>

        <!-- 移动端：卡片样式 -->
        <div v-if="records.length > 0" class="md:hidden space-y-3">
          <div v-for="(record, index) in records" :key="record.id" class="bg-white rounded-lg border p-4">
            <div class="flex justify-between items-start mb-3">
              <div>
                <div class="text-xs text-gray-500">日期</div>
                <div class="text-sm font-medium">{{ record.recordDate }}</div>
              </div>
              <div class="flex gap-2">
                <button @click="editRecord(record)" class="text-blue-600 hover:text-blue-800 p-1">
                  <Pencil class="w-4 h-4" />
                </button>
                <button @click="deleteRecord(record)" class="text-red-600 hover:text-red-800 p-1">
                  <Trash2 class="w-4 h-4" />
                </button>
              </div>
            </div>
            <div class="space-y-2">
              <div class="flex justify-between">
                <span class="text-xs text-gray-600">余额</span>
                <span class="text-sm font-semibold">${{ formatAmount(record.balanceInBaseCurrency) }}</span>
              </div>
              <div class="flex justify-between">
                <span class="text-xs text-gray-600">变化</span>
                <span v-if="index < records.length - 1" :class="getChangeClass(record, records[index + 1])" class="text-sm font-medium">
                  {{ formatChange(record, records[index + 1]) }}
                </span>
                <span v-else class="text-sm text-gray-400">-</span>
              </div>
              <div v-if="record.notes" class="pt-2 border-t">
                <div class="text-xs text-gray-600">备注</div>
                <div class="text-xs text-gray-800 mt-1">{{ record.notes }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 桌面端：表格样式 -->
        <div v-if="records.length > 0" class="hidden md:block bg-white rounded-lg border overflow-x-auto">
          <table class="w-full">
            <thead class="bg-gray-50 border-b">
              <tr>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">日期</th>
                <th class="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">余额</th>
                <th class="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">变化</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">备注</th>
                <th class="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y">
              <tr v-for="(record, index) in records" :key="record.id" class="hover:bg-gray-50">
                <td class="px-6 py-4 text-sm">{{ record.recordDate }}</td>
                <td class="px-6 py-4 text-sm text-right font-medium">
                  ${{ formatAmount(record.balanceInBaseCurrency) }}
                </td>
                <td class="px-6 py-4 text-sm text-right">
                  <span v-if="index < records.length - 1" :class="getChangeClass(record, records[index + 1])">
                    {{ formatChange(record, records[index + 1]) }}
                  </span>
                  <span v-else class="text-gray-400">-</span>
                </td>
                <td class="px-6 py-4 text-sm text-gray-600">
                  {{ record.notes || '-' }}
                </td>
                <td class="px-6 py-4 text-center">
                  <button
                    @click="editRecord(record)"
                    class="text-blue-600 hover:text-blue-800 mr-3"
                  >
                    <Pencil class="w-4 h-4 inline" />
                  </button>
                  <button
                    @click="deleteRecord(record)"
                    class="text-red-600 hover:text-red-800"
                  >
                    <Trash2 class="w-4 h-4 inline" />
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- 添加/编辑记录对话框 -->
    <div
      v-if="showAddRecordDialog || showEditRecordDialog"
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4"
      @click.self="closeRecordDialog"
    >
      <div class="bg-white rounded-lg p-4 md:p-6 w-full max-w-md max-h-[90vh] overflow-y-auto">
        <h2 class="text-lg md:text-xl font-bold mb-3 md:mb-4">
          {{ showEditRecordDialog ? '编辑记录' : '添加记录' }}
        </h2>
        <form @submit.prevent="submitRecordForm" class="space-y-3 md:space-y-4">
          <div>
            <label class="block text-xs md:text-sm font-medium text-gray-700 mb-1">日期</label>
            <input
              v-model="recordForm.recordDate"
              type="date"
              required
              class="w-full px-2 md:px-3 py-1.5 md:py-2 text-sm md:text-base border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-red-500"
            />
          </div>

          <div>
            <label class="block text-xs md:text-sm font-medium text-gray-700 mb-1">余额</label>
            <input
              v-model.number="recordForm.balance"
              type="number"
              step="0.01"
              required
              placeholder="0.00"
              class="w-full px-2 md:px-3 py-1.5 md:py-2 text-sm md:text-base border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-red-500"
            />
          </div>

          <div>
            <label class="block text-xs md:text-sm font-medium text-gray-700 mb-1">汇率</label>
            <input
              v-model.number="recordForm.exchangeRate"
              type="number"
              step="0.000001"
              placeholder="1.0"
              class="w-full px-2 md:px-3 py-1.5 md:py-2 text-sm md:text-base border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-red-500"
            />
          </div>

          <div>
            <label class="block text-xs md:text-sm font-medium text-gray-700 mb-1">备注</label>
            <textarea
              v-model="recordForm.notes"
              rows="3"
              placeholder="添加备注信息"
              class="w-full px-2 md:px-3 py-1.5 md:py-2 text-sm md:text-base border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-red-500"
            ></textarea>
          </div>

          <div class="flex gap-2 md:gap-3 pt-3 md:pt-4">
            <Button type="submit" class="flex-1 bg-red-600 hover:bg-red-700 text-sm md:text-base py-2 md:py-2.5">
              {{ showEditRecordDialog ? '保存' : '添加' }}
            </Button>
            <Button type="button" @click="closeRecordDialog" class="flex-1 bg-gray-200 hover:bg-gray-300 text-gray-700 text-sm md:text-base py-2 md:py-2.5">
              取消
            </Button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ChevronLeft, Plus, Pencil, Trash2 } from 'lucide-vue-next'
import Card from '@/components/ui/Card.vue'
import CardHeader from '@/components/ui/CardHeader.vue'
import CardTitle from '@/components/ui/CardTitle.vue'
import CardContent from '@/components/ui/CardContent.vue'
import Button from '@/components/ui/Button.vue'
import { liabilityAccountAPI, liabilityRecordAPI } from '@/api/liability'

const route = useRoute()
const router = useRouter()
const accountId = ref(parseInt(route.params.id))
const loading = ref(false)
const account = ref(null)
const records = ref([])
const showAddRecordDialog = ref(false)
const showEditRecordDialog = ref(false)
const currentEditRecordId = ref(null)

const recordForm = ref({
  recordDate: new Date().toISOString().split('T')[0],
  balance: null,
  exchangeRate: 1.0,
  notes: ''
})

// 格式化金额
function formatAmount(amount) {
  if (!amount && amount !== 0) return '0.00'
  return Number(amount).toLocaleString('en-US', {
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

// 计算变化并格式化
function formatChange(current, previous) {
  if (!previous) return '-'
  const change = previous.balanceInBaseCurrency - current.balanceInBaseCurrency
  const sign = change >= 0 ? '-' : '+'
  return `${sign}$${formatAmount(Math.abs(change))}`
}

// 获取变化样式类（负债减少是好的，显示为绿色）
function getChangeClass(current, previous) {
  if (!previous) return ''
  const change = previous.balanceInBaseCurrency - current.balanceInBaseCurrency
  return change >= 0 ? 'text-green-600 font-medium' : 'text-red-600 font-medium'
}

// 加载账户信息
async function loadAccount() {
  loading.value = true
  try {
    const response = await liabilityAccountAPI.getById(accountId.value)
    if (response.success) {
      account.value = response.data
    }
  } catch (error) {
    console.error('加载账户信息失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载记录
async function loadRecords() {
  try {
    const response = await liabilityRecordAPI.getByAccountId(accountId.value)
    if (response.success) {
      records.value = response.data
    }
  } catch (error) {
    console.error('加载记录失败:', error)
  }
}

// 编辑记录
function editRecord(record) {
  currentEditRecordId.value = record.id
  recordForm.value = {
    recordDate: record.recordDate,
    balance: record.balance,
    exchangeRate: record.exchangeRate || 1.0,
    notes: record.notes || ''
  }
  showEditRecordDialog.value = true
}

// 删除记录
async function deleteRecord(record) {
  if (!confirm(`确定要删除 ${record.recordDate} 的记录吗？`)) return

  try {
    const response = await liabilityRecordAPI.delete(record.id)
    if (response.success) {
      await loadRecords()
      await loadAccount() // 重新加载账户以更新最新余额
    }
  } catch (error) {
    console.error('删除记录失败:', error)
    alert('删除失败，请重试')
  }
}

// 提交记录表单
async function submitRecordForm() {
  try {
    const data = {
      accountId: accountId.value,
      ...recordForm.value,
      currency: account.value.currency
    }

    let response
    if (showEditRecordDialog.value) {
      response = await liabilityRecordAPI.update(currentEditRecordId.value, data)
    } else {
      response = await liabilityRecordAPI.create(data)
    }

    if (response.success) {
      closeRecordDialog()
      await loadRecords()
      await loadAccount() // 重新加载账户以更新最新余额
    }
  } catch (error) {
    console.error('提交失败:', error)
    alert(error.response?.data?.message || '操作失败，请重试')
  }
}

// 关闭记录对话框
function closeRecordDialog() {
  showAddRecordDialog.value = false
  showEditRecordDialog.value = false
  currentEditRecordId.value = null
  recordForm.value = {
    recordDate: new Date().toISOString().split('T')[0],
    balance: null,
    exchangeRate: 1.0,
    notes: ''
  }
}

onMounted(() => {
  loadAccount()
  loadRecords()
})
</script>
