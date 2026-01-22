<template>
  <div class="min-h-screen bg-gray-50 sm:bg-transparent">
    <div class="p-4 sm:p-6 space-y-4 sm:space-y-6">
      <!-- 页面标题 -->
      <div class="bg-white sm:bg-transparent p-4 sm:p-0 rounded-lg sm:rounded-none shadow-sm sm:shadow-none">
        <h2 class="text-xl sm:text-2xl font-bold">汇率管理</h2>
      </div>

      <!-- Tab 导航 -->
      <div class="bg-white rounded-lg shadow-sm">
        <div class="border-b border-gray-200">
          <nav class="flex -mb-px">
            <button
              @click="activeTab = 'daily'"
              :class="[
                'flex-1 py-3 px-4 text-center border-b-2 font-medium text-sm transition-colors',
                activeTab === 'daily'
                  ? 'border-green-500 text-green-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              ]"
            >
              日期汇率管理
            </button>
            <button
              @click="activeTab = 'history'"
              :class="[
                'flex-1 py-3 px-4 text-center border-b-2 font-medium text-sm transition-colors',
                activeTab === 'history'
                  ? 'border-green-500 text-green-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              ]"
            >
              汇率历史与趋势
            </button>
          </nav>
        </div>

        <!-- Tab 内容 -->
        <div class="p-4 sm:p-6">
          <!-- Tab 1: 日期汇率管理 -->
          <DailyRateManager v-if="activeTab === 'daily'" />

          <!-- Tab 2: 汇率历史与趋势 -->
          <RateHistoryChart v-if="activeTab === 'history'" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import DailyRateManager from './components/DailyRateManager.vue'
import RateHistoryChart from './components/RateHistoryChart.vue'

const activeTab = ref('daily')
</script>
