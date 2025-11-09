<template>
  <div class="p-6 space-y-6">
    <!-- 返回按钮 -->
    <button @click="router.back()" class="flex items-center text-gray-600 hover:text-gray-900">
      <ChevronLeft class="w-5 h-5" />
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
              <CardTitle class="text-2xl">{{ account.accountName }}</CardTitle>
              <p class="text-sm text-gray-500 mt-1">{{ account.categoryName }}</p>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div>
              <div class="text-sm text-gray-600">机构</div>
              <div class="font-medium">{{ account.institution || '-' }}</div>
            </div>
            <div>
              <div class="text-sm text-gray-600">账号</div>
              <div class="font-medium">{{ maskAccountNumber(account.accountNumber) || '-' }}</div>
            </div>
            <div>
              <div class="text-sm text-gray-600">币种</div>
              <div class="font-medium">{{ account.currency }}</div>
            </div>
            <div>
              <div class="text-sm text-gray-600">当前金额</div>
              <div class="text-xl font-bold text-primary">
                {{ getCurrencySymbol(account.currency) }}{{ formatAmount(account.latestAmount) }}
              </div>
            </div>
          </div>
          <div v-if="account.notes" class="mt-4 pt-4 border-t">
            <div class="text-sm text-gray-600">备注</div>
            <div class="text-sm mt-1">{{ account.notes }}</div>
          </div>
        </CardContent>
      </Card>

      <!-- 资产记录 -->
      <div class="space-y-4">
        <div class="flex justify-between items-center">
          <h2 class="text-xl font-bold">资产记录</h2>
          <Button @click="showAddRecordDialog = true" class="bg-primary hover:bg-primary/90">
            <Plus class="w-4 h-4 mr-2" />
            添加记录
          </Button>
        </div>

        <div v-if="records.length === 0" class="text-center py-12 bg-gray-50 rounded-lg">
          <p class="text-gray-500">暂无记录数据</p>
        </div>

        <div v-else class="bg-white rounded-lg border">
          <table class="w-full">
            <thead class="bg-gray-50 border-b">
              <tr>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">日期</th>
                <th class="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">金额</th>
                <th class="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">数量</th>
                <th class="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">单价</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">备注</th>
                <th class="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y">
              <tr v-for="record in records" :key="record.id" class="hover:bg-gray-50">
                <td class="px-6 py-4 text-sm">{{ record.recordDate }}</td>
                <td class="px-6 py-4 text-sm text-right font-medium">
                  ¥{{ formatAmount(record.amountInBaseCurrency) }}
                </td>
                <td class="px-6 py-4 text-sm text-right">
                  {{ record.quantity ? formatAmount(record.quantity) : '-' }}
                </td>
                <td class="px-6 py-4 text-sm text-right">
                  {{ record.unitPrice ? formatAmount(record.unitPrice) : '-' }}
                </td>
                <td class="px-6 py-4 text-sm text-gray-600">
                  {{ record.notes || '-' }}
                </td>
                <td class="px-6 py-4 text-center">
                  <button
                    @click="editRecord(record)"
                    class="text-primary hover:text-primary/80 mr-3"
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
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50"
      @click.self="closeRecordDialog"
    >
      <div class="bg-white rounded-lg p-6 w-full max-w-md">
        <h2 class="text-xl font-bold mb-4">
          {{ showEditRecordDialog ? '编辑记录' : '添加记录' }}
        </h2>
        <form @submit.prevent="submitRecordForm" class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">日期</label>
            <input
              v-model="recordForm.recordDate"
              type="date"
              required
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">金额</label>
            <input
              v-model.number="recordForm.amount"
              type="number"
              step="0.01"
              required
              placeholder="0.00"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">数量</label>
              <input
                v-model.number="recordForm.quantity"
                type="number"
                step="0.000001"
                placeholder="可选"
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">单价</label>
              <input
                v-model.number="recordForm.unitPrice"
                type="number"
                step="0.000001"
                placeholder="可选"
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
              />
            </div>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">汇率</label>
            <input
              v-model.number="recordForm.exchangeRate"
              type="number"
              step="0.000001"
              placeholder="1.0"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">备注</label>
            <textarea
              v-model="recordForm.notes"
              rows="3"
              placeholder="添加备注信息"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
            ></textarea>
          </div>

          <div class="flex gap-3 pt-4">
            <Button type="submit" class="flex-1 bg-primary hover:bg-primary/90">
              {{ showEditRecordDialog ? '保存' : '添加' }}
            </Button>
            <Button type="button" @click="closeRecordDialog" class="flex-1 bg-gray-200 hover:bg-gray-300 text-gray-700">
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
import { assetAccountAPI, assetRecordAPI } from '@/api'

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
  amount: null,
  quantity: null,
  unitPrice: null,
  exchangeRate: 1.0,
  notes: ''
})

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

// 加载账户信息
async function loadAccount() {
  loading.value = true
  try {
    const response = await assetAccountAPI.getById(accountId.value)
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
    const response = await assetRecordAPI.getByAccountId(accountId.value)
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
    amount: record.amount,
    quantity: record.quantity,
    unitPrice: record.unitPrice,
    exchangeRate: record.exchangeRate || 1.0,
    notes: record.notes || ''
  }
  showEditRecordDialog.value = true
}

// 删除记录
async function deleteRecord(record) {
  if (!confirm(`确定要删除 ${record.recordDate} 的记录吗？`)) return

  try {
    const response = await assetRecordAPI.delete(record.id)
    if (response.success) {
      await loadRecords()
      await loadAccount() // 重新加载账户以更新最新金额
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
      response = await assetRecordAPI.update(currentEditRecordId.value, data)
    } else {
      response = await assetRecordAPI.create(data)
    }

    if (response.success) {
      closeRecordDialog()
      await loadRecords()
      await loadAccount() // 重新加载账户以更新最新金额
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
    amount: null,
    quantity: null,
    unitPrice: null,
    exchangeRate: 1.0,
    notes: ''
  }
}

onMounted(() => {
  loadAccount()
  loadRecords()
})
</script>
