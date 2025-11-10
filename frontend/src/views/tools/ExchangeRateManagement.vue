<template>
  <div class="space-y-6">
    <!-- 页面标题和操作按钮 -->
    <div class="flex justify-between items-center">
      <h2 class="text-2xl font-bold">汇率管理</h2>
      <div class="flex gap-2">
        <button
          @click="initializeDefaultRates"
          class="px-4 py-2 bg-gray-500 text-white rounded hover:bg-gray-600 transition-colors"
          :disabled="loading"
        >
          初始化默认汇率
        </button>
        <button
          @click="openCreateDialog"
          class="px-4 py-2 bg-green-500 text-white rounded hover:bg-green-600 transition-colors"
        >
          添加汇率
        </button>
      </div>
    </div>

    <!-- 汇率列表 -->
    <div class="bg-white rounded-lg shadow">
      <div class="overflow-x-auto">
        <table class="min-w-full divide-y divide-gray-200">
          <thead class="bg-gray-50">
            <tr>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                货币代码
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                汇率 (对美元)
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                生效日期
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                来源
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                状态
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                备注
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                操作
              </th>
            </tr>
          </thead>
          <tbody class="bg-white divide-y divide-gray-200">
            <tr v-if="loading">
              <td colspan="7" class="px-6 py-4 text-center text-gray-500">
                加载中...
              </td>
            </tr>
            <tr v-else-if="exchangeRates.length === 0">
              <td colspan="7" class="px-6 py-4 text-center text-gray-500">
                暂无汇率数据，请点击"初始化默认汇率"或"添加汇率"
              </td>
            </tr>
            <tr v-else v-for="rate in exchangeRates" :key="rate.id" class="hover:bg-gray-50">
              <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                {{ rate.currency }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                {{ formatRate(rate.rateToUsd) }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                {{ rate.effectiveDate }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                {{ rate.source || '-' }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap">
                <span
                  :class="[
                    'px-2 inline-flex text-xs leading-5 font-semibold rounded-full',
                    rate.isActive ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
                  ]"
                >
                  {{ rate.isActive ? '启用' : '停用' }}
                </span>
              </td>
              <td class="px-6 py-4 text-sm text-gray-500">
                {{ rate.notes || '-' }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm font-medium space-x-2">
                <button
                  @click="openEditDialog(rate)"
                  class="text-blue-600 hover:text-blue-900"
                >
                  编辑
                </button>
                <button
                  v-if="rate.isActive"
                  @click="deactivateRate(rate.id)"
                  class="text-yellow-600 hover:text-yellow-900"
                >
                  停用
                </button>
                <button
                  @click="deleteRate(rate.id)"
                  class="text-red-600 hover:text-red-900"
                >
                  删除
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 创建/编辑对话框 -->
    <div
      v-if="showDialog"
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50"
      @click.self="closeDialog"
    >
      <div class="bg-white rounded-lg shadow-xl p-6 w-full max-w-md">
        <h3 class="text-lg font-semibold mb-4">
          {{ editingRate ? '编辑汇率' : '添加汇率' }}
        </h3>

        <form @submit.prevent="saveRate" class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              货币代码 <span class="text-red-500">*</span>
            </label>
            <input
              v-model="formData.currency"
              type="text"
              required
              maxlength="10"
              placeholder="例如: CNY, EUR, GBP"
              class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
              :disabled="editingRate"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              汇率 (1单位货币 = X美元) <span class="text-red-500">*</span>
            </label>
            <input
              v-model="formData.rateToUsd"
              type="number"
              step="0.00000001"
              required
              placeholder="例如: 0.14 表示 1 CNY = 0.14 USD"
              class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              生效日期 <span class="text-red-500">*</span>
            </label>
            <input
              v-model="formData.effectiveDate"
              type="date"
              required
              class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
              :disabled="editingRate"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              来源
            </label>
            <input
              v-model="formData.source"
              type="text"
              maxlength="100"
              placeholder="例如: 手动输入, API, 央行"
              class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              备注
            </label>
            <textarea
              v-model="formData.notes"
              rows="3"
              maxlength="500"
              placeholder="可选"
              class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
            ></textarea>
          </div>

          <div class="flex items-center">
            <input
              v-model="formData.isActive"
              type="checkbox"
              id="isActive"
              class="h-4 w-4 text-green-600 focus:ring-green-500 border-gray-300 rounded"
            />
            <label for="isActive" class="ml-2 block text-sm text-gray-900">
              启用此汇率
            </label>
          </div>

          <div class="flex justify-end gap-2 mt-6">
            <button
              type="button"
              @click="closeDialog"
              class="px-4 py-2 bg-gray-300 text-gray-700 rounded hover:bg-gray-400 transition-colors"
            >
              取消
            </button>
            <button
              type="submit"
              class="px-4 py-2 bg-green-500 text-white rounded hover:bg-green-600 transition-colors"
              :disabled="submitting"
            >
              {{ submitting ? '保存中...' : '保存' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { exchangeRateAPI } from '@/api/exchangeRate'

const exchangeRates = ref([])
const loading = ref(false)
const showDialog = ref(false)
const editingRate = ref(null)
const submitting = ref(false)

const formData = ref({
  currency: '',
  rateToUsd: '',
  effectiveDate: new Date().toISOString().split('T')[0],
  source: '手动输入',
  notes: '',
  isActive: true
})

// 加载所有汇率
const loadExchangeRates = async () => {
  loading.value = true
  try {
    const response = await exchangeRateAPI.getAll()
    if (response.success) {
      exchangeRates.value = response.data
    }
  } catch (error) {
    console.error('加载汇率失败:', error)
    alert('加载汇率失败，请重试')
  } finally {
    loading.value = false
  }
}

// 格式化汇率显示
const formatRate = (rate) => {
  if (!rate) return '0.00000000'
  return parseFloat(rate).toFixed(8)
}

// 打开创建对话框
const openCreateDialog = () => {
  editingRate.value = null
  formData.value = {
    currency: '',
    rateToUsd: '',
    effectiveDate: new Date().toISOString().split('T')[0],
    source: '手动输入',
    notes: '',
    isActive: true
  }
  showDialog.value = true
}

// 打开编辑对话框
const openEditDialog = (rate) => {
  editingRate.value = rate
  formData.value = {
    currency: rate.currency,
    rateToUsd: rate.rateToUsd,
    effectiveDate: rate.effectiveDate,
    source: rate.source || '',
    notes: rate.notes || '',
    isActive: rate.isActive
  }
  showDialog.value = true
}

// 关闭对话框
const closeDialog = () => {
  showDialog.value = false
  editingRate.value = null
}

// 保存汇率
const saveRate = async () => {
  submitting.value = true
  try {
    if (editingRate.value) {
      // 更新
      const response = await exchangeRateAPI.update(editingRate.value.id, formData.value)
      if (response.success) {
        alert('汇率更新成功')
        closeDialog()
        loadExchangeRates()
      } else {
        alert(response.message || '更新失败')
      }
    } else {
      // 创建
      const response = await exchangeRateAPI.create(formData.value)
      if (response.success) {
        alert('汇率创建成功')
        closeDialog()
        loadExchangeRates()
      } else {
        alert(response.message || '创建失败')
      }
    }
  } catch (error) {
    console.error('保存汇率失败:', error)
    alert(error.response?.data?.message || '保存失败，请重试')
  } finally {
    submitting.value = false
  }
}

// 停用汇率
const deactivateRate = async (id) => {
  if (!confirm('确定要停用此汇率吗？')) {
    return
  }

  try {
    const response = await exchangeRateAPI.deactivate(id)
    if (response.success) {
      alert('汇率已停用')
      loadExchangeRates()
    } else {
      alert(response.message || '停用失败')
    }
  } catch (error) {
    console.error('停用汇率失败:', error)
    alert('停用失败，请重试')
  }
}

// 删除汇率
const deleteRate = async (id) => {
  if (!confirm('确定要删除此汇率吗？此操作不可恢复！')) {
    return
  }

  try {
    const response = await exchangeRateAPI.delete(id)
    if (response.success) {
      alert('汇率删除成功')
      loadExchangeRates()
    } else {
      alert(response.message || '删除失败')
    }
  } catch (error) {
    console.error('删除汇率失败:', error)
    alert('删除失败，请重试')
  }
}

// 初始化默认汇率
const initializeDefaultRates = async () => {
  if (!confirm('确定要初始化默认汇率吗？如果今天已有汇率数据，将不会创建重复记录。')) {
    return
  }

  loading.value = true
  try {
    const response = await exchangeRateAPI.initialize()
    if (response.success) {
      alert('默认汇率初始化成功')
      loadExchangeRates()
    } else {
      alert(response.message || '初始化失败')
    }
  } catch (error) {
    console.error('初始化默认汇率失败:', error)
    alert('初始化失败，请重试')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadExchangeRates()
})
</script>
