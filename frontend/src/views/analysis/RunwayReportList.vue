<template>
  <div class="p-3 md:p-6 space-y-4 md:space-y-6">
    <!-- Header -->
    <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
      <div>
        <h1 class="text-xl md:text-2xl font-bold text-gray-900">历史跑道报告</h1>
        <p class="text-xs md:text-sm text-gray-600 mt-1">查看和管理已保存的资金跑道分析报告</p>
      </div>
      <router-link
        to="/analysis/runway"
        class="px-3 py-2 bg-blue-600 text-white text-sm rounded-lg hover:bg-blue-700 flex items-center gap-2 self-start"
      >
        ← 返回跑道分析
      </router-link>
    </div>

    <!-- Error -->
    <div v-if="error" class="bg-red-50 border border-red-200 rounded-lg p-4 text-sm text-red-700">
      {{ error }}
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flex justify-center py-12">
      <svg class="w-8 h-8 animate-spin text-blue-600" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"/>
      </svg>
    </div>

    <!-- Empty state -->
    <div v-else-if="!loading && reports.length === 0" class="bg-white rounded-xl border border-gray-200 p-12 text-center">
      <p class="text-gray-400 text-sm">暂无保存的报告</p>
      <p class="text-gray-400 text-xs mt-1">在资金跑道分析页点击「保存报告」创建第一份报告</p>
    </div>

    <!-- Reports table -->
    <div v-else class="bg-white rounded-xl border border-gray-200 overflow-hidden">
      <table class="w-full text-sm">
        <thead class="bg-gray-50">
          <tr>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">报告名称</th>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">保存时间</th>
            <th class="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">操作</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-100">
          <tr v-for="report in reports" :key="report.id" class="hover:bg-gray-50">
            <td class="px-4 py-3 font-medium text-gray-800">{{ report.reportName }}</td>
            <td class="px-4 py-3 text-gray-500">{{ formatDate(report.savedAt) }}</td>
            <td class="px-4 py-3 text-right">
              <div class="flex items-center justify-end gap-2">
                <router-link
                  :to="`/analysis/runway-reports/${report.id}`"
                  class="px-3 py-1.5 bg-blue-600 text-white text-xs rounded-lg hover:bg-blue-700"
                >
                  查看
                </router-link>
                <button
                  @click="deleteReport(report.id)"
                  class="px-3 py-1.5 bg-red-50 text-red-600 text-xs rounded-lg hover:bg-red-100 border border-red-200"
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
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '../../stores/auth'
import { runwayAPI } from '../../api/runway'

const authStore = useAuthStore()
const reports = ref([])
const loading = ref(false)
const error = ref(null)

async function fetchReports() {
  const familyId = authStore.familyId
  if (!familyId) return
  loading.value = true
  error.value = null
  try {
    const response = await runwayAPI.listRunwayReports(familyId)
    if (response.success) {
      reports.value = response.data
    } else {
      error.value = response.error || '加载失败'
    }
  } catch (e) {
    error.value = e.message || '请求失败'
  } finally {
    loading.value = false
  }
}

async function deleteReport(id) {
  if (!confirm('确认删除此报告？')) return
  try {
    const response = await runwayAPI.deleteRunwayReport(id)
    if (response.success) {
      reports.value = reports.value.filter(r => r.id !== id)
    } else {
      error.value = response.error || '删除失败'
    }
  } catch (e) {
    error.value = e.message || '删除失败'
  }
}

function formatDate(dt) {
  if (!dt) return '—'
  return new Date(dt).toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit',
  })
}

onMounted(fetchReports)
</script>
