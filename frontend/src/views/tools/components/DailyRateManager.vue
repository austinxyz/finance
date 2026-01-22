<template>
  <div class="space-y-6">
    <!-- 日期选择和操作按钮 -->
    <div class="flex flex-col sm:flex-row gap-3 sm:items-center">
      <div class="flex-1">
        <label class="block text-sm font-medium text-gray-700 mb-1">选择日期</label>
        <input
          v-model="selectedDate"
          type="date"
          class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
          @change="loadRatesForDate"
        />
      </div>
      <div class="flex gap-2 sm:mt-6">
        <button
          @click="fetchFromAPI"
          :disabled="loading || fetching"
          class="flex-1 sm:flex-none px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 disabled:bg-gray-400 disabled:cursor-not-allowed transition-colors text-sm font-medium"
        >
          {{ fetching ? '获取中...' : '从API获取' }}
        </button>
        <button
          @click="batchSave"
          :disabled="loading || !hasChanges"
          class="flex-1 sm:flex-none px-4 py-2 bg-green-500 text-white rounded-lg hover:bg-green-600 disabled:bg-gray-400 disabled:cursor-not-allowed transition-colors text-sm font-medium"
        >
          批量保存
        </button>
      </div>
    </div>

    <!-- 汇率表单 -->
    <div v-if="!loading" class="space-y-4">
      <div
        v-for="currency in currencies"
        :key="currency"
        class="bg-gray-50 p-4 rounded-lg"
      >
        <div class="flex items-center gap-4">
          <div class="w-20 font-medium text-gray-700">{{ currency }}</div>
          <div class="flex-1">
            <div class="flex items-center gap-2">
              <span class="text-sm text-gray-500">1 {{ currency }} =</span>
              <input
                v-model.number="rates[currency]"
                type="number"
                step="0.00000001"
                placeholder="请输入汇率"
                class="flex-1 px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500 text-sm"
                @input="markAsChanged(currency)"
              />
              <span class="text-sm text-gray-500">USD</span>
            </div>
          </div>
          <div class="w-24 text-right">
            <span
              v-if="changedCurrencies.has(currency)"
              class="text-xs text-orange-600 font-medium"
            >
              已修改
            </span>
            <span
              v-else-if="rates[currency]"
              class="text-xs text-gray-500"
            >
              已保存
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="text-center py-12">
      <div class="animate-spin rounded-full h-10 w-10 border-b-2 border-green-600 mx-auto mb-4"></div>
      <p class="text-gray-500 text-sm">加载中...</p>
    </div>

    <!-- 说明 -->
    <div class="bg-blue-50 border border-blue-200 rounded-lg p-4">
      <h4 class="font-medium text-blue-900 mb-2">使用说明</h4>
      <ul class="text-sm text-blue-800 space-y-1">
        <li>• 选择日期后，系统会加载该日期的现有汇率（如果有）</li>
        <li>• 点击"从API获取"可自动获取该日期的汇率数据</li>
        <li>• 修改汇率后，点击"批量保存"保存所有修改</li>
        <li>• 汇率格式：1单位货币 = X美元（例如：1 CNY = 0.14 USD）</li>
      </ul>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { exchangeRateAPI } from '@/api/exchangeRate'
import { getTodayDate } from '@/lib/utils'

const currencies = ['CNY', 'EUR', 'GBP', 'JPY', 'AUD', 'CAD']
const selectedDate = ref(getTodayDate())
const rates = ref({})
const originalRates = ref({})
const changedCurrencies = ref(new Set())
const loading = ref(false)
const fetching = ref(false)

// 是否有修改
const hasChanges = computed(() => changedCurrencies.value.size > 0)

// 加载指定日期的汇率
const loadRatesForDate = async () => {
  loading.value = true
  changedCurrencies.value.clear()

  try {
    const response = await exchangeRateAPI.getRatesByDate(selectedDate.value)
    if (response.success) {
      // 重置 rates
      rates.value = {}
      originalRates.value = {}

      // 填充已有汇率
      response.data.forEach(rate => {
        rates.value[rate.currency] = rate.rateToUsd
        originalRates.value[rate.currency] = rate.rateToUsd
      })

      // 确保所有货币都有key
      currencies.forEach(currency => {
        if (!(currency in rates.value)) {
          rates.value[currency] = null
          originalRates.value[currency] = null
        }
      })
    }
  } catch (error) {
    console.error('加载汇率失败:', error)
    alert('加载汇率失败，请重试')
  } finally {
    loading.value = false
  }
}

// 从API获取汇率
const fetchFromAPI = async () => {
  if (!confirm(`确定要从API获取 ${selectedDate.value} 的汇率吗？这将覆盖当前输入的值。`)) {
    return
  }

  fetching.value = true
  try {
    const response = await exchangeRateAPI.fetchFromAPI(selectedDate.value)
    if (response.success) {
      alert(`成功获取 ${response.data.length} 条汇率记录`)
      // 重新加载
      await loadRatesForDate()
    } else {
      alert(response.message || '获取失败')
    }
  } catch (error) {
    console.error('从API获取汇率失败:', error)
    alert(error.response?.data?.message || '获取失败，请重试')
  } finally {
    fetching.value = false
  }
}

// 标记为已修改
const markAsChanged = (currency) => {
  const currentValue = rates.value[currency]
  const originalValue = originalRates.value[currency]

  if (currentValue !== originalValue) {
    changedCurrencies.value.add(currency)
  } else {
    changedCurrencies.value.delete(currency)
  }
}

// 批量保存
const batchSave = async () => {
  if (!hasChanges.value) {
    return
  }

  const changedList = Array.from(changedCurrencies.value)
  if (!confirm(`确定要保存 ${changedList.length} 个货币的汇率修改吗？`)) {
    return
  }

  loading.value = true
  try {
    let successCount = 0
    let errorCount = 0

    for (const currency of changedList) {
      const rateValue = rates.value[currency]
      if (!rateValue || rateValue <= 0) {
        console.warn(`跳过无效汇率: ${currency}`)
        errorCount++
        continue
      }

      try {
        const data = {
          currency,
          rateToUsd: rateValue,
          effectiveDate: selectedDate.value,
          source: '手动输入',
          isActive: true
        }

        // 尝试创建或更新
        try {
          await exchangeRateAPI.create(data)
          successCount++
        } catch (createError) {
          // 如果创建失败（可能已存在），尝试查找并更新
          const existingResponse = await exchangeRateAPI.getRatesByDate(selectedDate.value)
          const existingRate = existingResponse.data?.find(r => r.currency === currency)

          if (existingRate) {
            await exchangeRateAPI.update(existingRate.id, data)
            successCount++
          } else {
            throw createError
          }
        }
      } catch (error) {
        console.error(`保存 ${currency} 失败:`, error)
        errorCount++
      }
    }

    if (errorCount > 0) {
      alert(`保存完成：成功 ${successCount} 个，失败 ${errorCount} 个`)
    } else {
      alert(`成功保存 ${successCount} 个汇率`)
    }

    // 重新加载
    await loadRatesForDate()
  } catch (error) {
    console.error('批量保存失败:', error)
    alert('批量保存失败，请重试')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadRatesForDate()
})
</script>
