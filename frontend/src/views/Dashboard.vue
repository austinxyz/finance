<template>
  <div class="p-6 space-y-6">
    <!-- Welcome Section with Family Selector and Export Button -->
    <div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h1 class="text-2xl sm:text-3xl font-bold text-gray-900">财务概览</h1>
        <p class="text-sm sm:text-base text-gray-600 mt-1 sm:mt-2">
          欢迎使用个人理财管理系统
        </p>
      </div>
      <div class="flex items-center gap-3">
        <div class="flex items-center gap-2">
          <label class="text-sm font-medium text-gray-700 whitespace-nowrap">选择家庭:</label>
          <select
            v-model="familyId"
            @change="onFamilyChange"
            class="flex-1 sm:flex-none min-w-0 px-3 sm:px-4 py-2 text-sm sm:text-base border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 bg-white"
          >
            <option v-for="family in families" :key="family.id" :value="family.id">
              {{ family.familyName }}
            </option>
          </select>
        </div>
        <!-- Google Sheets 同步 -->
        <button
          @click="showGoogleSheetsSync = true"
          class="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 flex items-center gap-2"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
          </svg>
          <span>Google Sheets</span>
        </button>
      </div>
    </div>

    <!-- Quick Stats -->
    <div class="grid grid-cols-2 md:grid-cols-4 gap-3">
      <div class="bg-white rounded-lg shadow border border-gray-200 p-4">
        <div class="text-xs font-medium text-gray-600 mb-1">当前净资产</div>
        <div class="text-lg font-bold text-blue-600">${{ formatAmount(summaryData.netWorth) }}</div>
      </div>

      <div class="bg-white rounded-lg shadow border border-gray-200 p-4">
        <div class="text-xs font-medium text-gray-600 mb-1">{{ currentYear }}年职业收入</div>
        <div class="text-lg font-bold text-green-600">${{ formatAmount(careerIncome) }}</div>
      </div>

      <div class="bg-white rounded-lg shadow border border-gray-200 p-4">
        <div class="text-xs font-medium text-gray-600 mb-1">{{ currentYear }}年总支出</div>
        <div class="text-lg font-bold text-purple-600">${{ formatAmount(totalBaseExpense) }}</div>
      </div>

      <div class="bg-white rounded-lg shadow border border-gray-200 p-4">
        <div class="text-xs font-medium text-gray-600 mb-1">{{ currentYear }}年实际支出</div>
        <div class="text-lg font-bold text-orange-600">${{ formatAmount(actualExpense) }}</div>
      </div>

      <div class="bg-white rounded-lg shadow border border-gray-200 p-4">
        <div class="text-xs font-medium text-gray-600 mb-1">{{ currentYear }}年投资回报</div>
        <div class="text-lg font-bold" :class="getReturnColor(investmentReturn)">${{ formatAmountWithSign(investmentReturn) }}</div>
      </div>

      <div class="bg-white rounded-lg shadow border border-gray-200 p-4">
        <div class="text-xs font-medium text-gray-600 mb-1">当前总资产</div>
        <div class="text-lg font-bold text-emerald-600">${{ formatAmount(summaryData.totalAssets) }}</div>
      </div>

      <div class="bg-white rounded-lg shadow border border-gray-200 p-4">
        <div class="text-xs font-medium text-gray-600 mb-1">当前总负债</div>
        <div class="text-lg font-bold text-red-600">${{ formatAmount(summaryData.totalLiabilities) }}</div>
      </div>
    </div>

    <!-- 分布图表 - 第一行：净资产、职业收入、总支出 -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-4">
      <!-- 净资产分布 -->
      <div class="bg-white rounded-lg shadow border border-gray-200">
        <div class="px-6 py-4 border-b border-gray-200">
          <h2 class="text-lg font-semibold text-gray-900">净资产分布</h2>
          <p class="text-xs text-gray-500 mt-1">资产类型分布情况</p>
        </div>
        <div class="p-6">
          <div v-if="netAssetCategories.length === 0" class="text-sm text-gray-500 text-center py-8">
            暂无净资产数据
          </div>
          <div v-else class="h-64">
            <canvas ref="netWorthChartCanvas"></canvas>
          </div>
          <div v-if="netAssetCategories.length > 0" class="mt-6 space-y-2">
            <div v-for="category in netAssetCategories" :key="category.type" class="flex items-center justify-between text-sm">
              <div class="flex items-center gap-2">
                <div class="w-3 h-3 rounded-full" :style="{ backgroundColor: category.color }"></div>
                <span class="text-gray-700">{{ category.name }}</span>
              </div>
              <div class="flex items-center gap-3">
                <span class="text-gray-900 font-medium" :class="{ 'text-red-600': category.isNegative }">${{ formatAmount(Math.abs(category.total)) }}</span>
                <span class="text-gray-500">{{ category.percentage }}%</span>
              </div>
            </div>
            <div class="flex items-center justify-between text-sm pt-2 border-t border-gray-200 font-semibold">
              <span class="text-gray-700">净资产总计</span>
              <span class="text-blue-600">${{ formatAmount(netWorth) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 职业收入分布 -->
      <div class="bg-white rounded-lg shadow border border-gray-200">
        <div class="px-6 py-4 border-b border-gray-200">
          <h2 class="text-lg font-semibold text-gray-900">{{ currentYear }}年职业收入</h2>
          <p class="text-xs text-gray-500 mt-1">排除投资收益</p>
        </div>
        <div class="p-6">
          <div v-if="loadingIncome" class="text-sm text-gray-500 text-center py-8">
            加载中...
          </div>
          <div v-else-if="incomeCategories.length === 0" class="text-sm text-gray-500 text-center py-8">
            暂无收入数据
          </div>
          <div v-else>
            <div class="h-48">
              <canvas ref="incomeChartCanvas"></canvas>
            </div>
            <div class="mt-4 space-y-1 max-h-40 overflow-y-auto text-xs">
              <div v-for="category in incomeCategories" :key="category.id" class="flex items-center justify-between">
                <div class="flex items-center gap-2">
                  <div class="w-2 h-2 rounded-full" :style="{ backgroundColor: category.color }"></div>
                  <span class="text-gray-700">{{ category.name }}</span>
                </div>
                <span class="text-gray-900 font-medium">${{ formatAmount(category.amount) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 年度总支出分布 -->
      <div class="bg-white rounded-lg shadow border border-gray-200">
        <div class="px-6 py-4 border-b border-gray-200">
          <h2 class="text-lg font-semibold text-gray-900">{{ currentYear }}年总支出</h2>
          <p class="text-xs text-gray-500 mt-1">{{ currentYear }}年 · 预算支出金额</p>
        </div>
        <div class="p-6">
          <div v-if="loadingExpense" class="text-sm text-gray-500 text-center py-8">
            加载中...
          </div>
          <div v-else-if="baseExpenseCategories.length === 0" class="text-sm text-gray-500 text-center py-8">
            暂无支出数据
          </div>
          <div v-else>
            <div class="h-64">
              <canvas ref="baseExpenseChartCanvas"></canvas>
            </div>
            <div class="mt-6 space-y-2">
              <div v-for="category in baseExpenseCategories" :key="category.id" class="flex items-center justify-between text-sm">
                <div class="flex items-center gap-2">
                  <div class="w-3 h-3 rounded-full" :style="{ backgroundColor: category.color }"></div>
                  <span class="text-gray-700">{{ category.name }}</span>
                </div>
                <div class="flex items-center gap-3">
                  <span class="text-gray-900 font-medium">${{ formatAmount(category.amount) }}</span>
                  <span class="text-gray-500">{{ category.percentage }}%</span>
                </div>
              </div>
            </div>
            <div class="mt-4 pt-4 border-t border-gray-200">
              <div class="flex items-center justify-between text-sm font-semibold">
                <span class="text-gray-700">总支出</span>
                <span class="text-lg text-purple-600">${{ formatAmount(totalBaseExpense) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

    </div>

    <!-- 分布图表 - 第二行：实际支出、总资产、总负债 -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-4">
      <!-- 年度实际支出分布 -->
      <div class="bg-white rounded-lg shadow border border-gray-200">
        <div class="px-6 py-4 border-b border-gray-200">
          <h2 class="text-lg font-semibold text-gray-900">{{ currentYear }}年实际支出</h2>
          <p class="text-xs text-gray-500 mt-1">{{ currentYear }}年 · 实际支出金额</p>
        </div>
        <div class="p-6">
          <div v-if="loadingExpense" class="text-sm text-gray-500 text-center py-8">
            加载中...
          </div>
          <div v-else-if="expenseCategories.length === 0" class="text-sm text-gray-500 text-center py-8">
            暂无支出数据
          </div>
          <div v-else>
            <div class="h-64">
              <canvas ref="expenseChartCanvas"></canvas>
            </div>
            <div class="mt-6 space-y-2">
              <div v-for="category in expenseCategories" :key="category.id" class="flex items-center justify-between text-sm">
                <div class="flex items-center gap-2">
                  <div class="w-3 h-3 rounded-full" :style="{ backgroundColor: category.color }"></div>
                  <span class="text-gray-700">{{ category.name }}</span>
                </div>
                <div class="flex items-center gap-3">
                  <span class="text-gray-900 font-medium">${{ formatAmount(category.amount) }}</span>
                  <span class="text-gray-500">{{ category.percentage }}%</span>
                </div>
              </div>
            </div>
            <div class="mt-4 pt-4 border-t border-gray-200">
              <div class="flex items-center justify-between text-sm font-semibold">
                <span class="text-gray-700">实际总支出</span>
                <span class="text-lg text-orange-600">${{ formatAmount(totalExpense) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 当前总资产分布 -->
      <div class="bg-white rounded-lg shadow border border-gray-200">
        <div class="px-6 py-4 border-b border-gray-200">
          <h2 class="text-lg font-semibold text-gray-900">当前总资产分布</h2>
          <p class="text-xs text-gray-500 mt-1">{{ totalAssetAccounts }} 个账户 · {{ activeAssetAccounts }} 活跃</p>
        </div>
        <div class="p-6">
          <div v-if="assetCategories.length === 0" class="text-sm text-gray-500 text-center py-8">
            暂无资产数据
          </div>
          <div v-else class="h-64">
            <canvas ref="assetChartCanvas"></canvas>
          </div>
          <div v-if="assetCategories.length > 0" class="mt-6 space-y-2">
            <div v-for="category in assetCategories" :key="category.type" class="flex items-center justify-between text-sm">
              <div class="flex items-center gap-2">
                <div class="w-3 h-3 rounded-full" :style="{ backgroundColor: category.color }"></div>
                <span class="text-gray-700">{{ category.name }}</span>
              </div>
              <div class="flex items-center gap-3">
                <span class="text-gray-900 font-medium">${{ formatAmount(category.total) }}</span>
                <span class="text-gray-500">{{ category.percentage }}%</span>
              </div>
            </div>
            <div class="flex items-center justify-between text-sm pt-2 border-t border-gray-200 font-semibold">
              <span class="text-gray-700">总计</span>
              <span class="text-green-600">${{ formatAmount(totalAssets) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 负债分布 -->
      <div class="bg-white rounded-lg shadow border border-gray-200">
        <div class="px-6 py-4 border-b border-gray-200">
          <h2 class="text-lg font-semibold text-gray-900">当前总负债分布</h2>
          <p class="text-xs text-gray-500 mt-1">{{ totalLiabilityAccounts }} 个账户 · {{ activeLiabilityAccounts }} 活跃</p>
        </div>
        <div class="p-6">
          <div v-if="liabilityCategories.length === 0" class="text-sm text-gray-500 text-center py-8">
            暂无负债数据
          </div>
          <div v-else class="h-64">
            <canvas ref="liabilityChartCanvas"></canvas>
          </div>
          <div v-if="liabilityCategories.length > 0" class="mt-6 space-y-2">
            <div v-for="category in liabilityCategories" :key="category.type" class="flex items-center justify-between text-sm">
              <div class="flex items-center gap-2">
                <div class="w-3 h-3 rounded-full" :style="{ backgroundColor: category.color }"></div>
                <span class="text-gray-700">{{ category.name }}</span>
              </div>
              <div class="flex items-center gap-3">
                <span class="text-gray-900 font-medium">${{ formatAmount(category.total) }}</span>
                <span class="text-gray-500">{{ category.percentage }}%</span>
              </div>
            </div>
            <div class="flex items-center justify-between text-sm pt-2 border-t border-gray-200 font-semibold">
              <span class="text-gray-700">总计</span>
              <span class="text-red-600">${{ formatAmount(totalLiabilities) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 快捷操作 & 年度净资产趋势 -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <!-- 快捷操作 -->
      <div class="bg-white rounded-lg shadow border border-gray-200 lg:col-span-1">
        <div class="px-6 py-4 border-b border-gray-200">
          <h2 class="text-lg font-semibold text-gray-900">快捷操作</h2>
        </div>
        <div class="p-6">
          <div class="space-y-2">
            <button
              @click="$router.push('/assets/history')"
              class="w-full px-4 py-3 text-left border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors flex items-center gap-3"
            >
              <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5 text-green-600" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="12" y1="5" x2="12" y2="19"></line>
                <line x1="5" y1="12" x2="19" y2="12"></line>
              </svg>
              <span class="font-medium text-gray-900">管理资产账户</span>
            </button>
            <button
              @click="$router.push('/liabilities/history')"
              class="w-full px-4 py-3 text-left border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors flex items-center gap-3"
            >
              <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5 text-red-600" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="12" y1="5" x2="12" y2="19"></line>
                <line x1="5" y1="12" x2="19" y2="12"></line>
              </svg>
              <span class="font-medium text-gray-900">管理负债账户</span>
            </button>
            <button
              @click="$router.push('/assets/batch-update')"
              class="w-full px-4 py-3 text-left border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors flex items-center gap-3"
            >
              <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5 text-blue-600" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
              </svg>
              <span class="font-medium text-gray-900">批量更新资产</span>
            </button>
            <button
              @click="$router.push('/expenses/batch-update')"
              class="w-full px-4 py-3 text-left border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors flex items-center gap-3"
            >
              <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5 text-orange-600" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
              </svg>
              <span class="font-medium text-gray-900">批量录入支出</span>
            </button>
            <button
              @click="$router.push('/expenses/categories')"
              class="w-full px-4 py-3 text-left border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors flex items-center gap-3"
            >
              <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5 text-amber-600" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                <polyline points="14 2 14 8 20 8"></polyline>
                <line x1="16" y1="13" x2="8" y2="13"></line>
                <line x1="16" y1="17" x2="8" y2="17"></line>
                <polyline points="10 9 9 9 8 9"></polyline>
              </svg>
              <span class="font-medium text-gray-900">支出分类与记录</span>
            </button>
            <button
              @click="$router.push('/analysis/trend')"
              class="w-full px-4 py-3 text-left border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors flex items-center gap-3"
            >
              <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5 text-purple-600" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"></polyline>
              </svg>
              <span class="font-medium text-gray-900">查看趋势分析</span>
            </button>
          </div>
        </div>
      </div>

      <!-- 年度净资产趋势 -->
      <div class="bg-white rounded-lg shadow border border-gray-200 lg:col-span-2">
      <div class="px-6 py-4 border-b border-gray-200 flex items-center justify-between">
        <h2 class="text-lg font-semibold text-gray-900">年度净资产趋势</h2>
        <div class="flex gap-2">
          <button
            v-for="range in timeRanges"
            :key="range.value"
            @click="selectTimeRange(range.value)"
            :class="[
              'px-3 py-1 text-xs rounded-md font-medium transition-colors',
              selectedTimeRange === range.value
                ? 'bg-blue-600 text-white'
                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            ]"
          >
            {{ range.label }}
          </button>
        </div>
      </div>
      <div class="p-6">
        <div v-if="loadingTrend" class="text-sm text-gray-500 text-center py-12">
          加载中...
        </div>
        <div v-else-if="overallTrendData.length === 0" class="text-sm text-gray-500 text-center py-12">
          暂无趋势数据，请先添加资产或负债记录
        </div>
        <div v-else class="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <!-- 左侧图表 (1/2) -->
          <div class="h-96">
            <canvas ref="annualNetWorthChartCanvas"></canvas>
          </div>

          <!-- 右侧年度汇总表格 (1/2) -->
          <div>
            <div class="border border-gray-200 rounded-lg overflow-hidden">
              <table class="w-full divide-y divide-gray-200">
                <thead class="bg-gray-50">
                  <tr>
                    <th class="px-2 py-1.5 text-left text-xs font-medium text-gray-500 uppercase">年份</th>
                    <th class="px-2 py-1.5 text-right text-xs font-medium text-gray-500 uppercase">净资产</th>
                    <th class="px-2 py-1.5 text-right text-xs font-medium text-gray-500 uppercase">同比</th>
                  </tr>
                </thead>
                <tbody class="bg-white divide-y divide-gray-200">
                  <tr v-for="(item, index) in annualSummaryData" :key="item.year" class="hover:bg-gray-50">
                    <td class="px-2 py-1.5 text-sm font-medium text-gray-900">{{ item.year }}</td>
                    <td class="px-2 py-1.5 text-sm text-gray-900 text-right font-medium whitespace-nowrap">
                      ${{ formatAmount(item.netWorth) }}
                    </td>
                    <td class="px-2 py-1.5 text-sm text-right whitespace-nowrap">
                      <div v-if="item.yoyChange !== null">
                        <div :class="getChangeColorClass(item.yoyChangePct)" class="font-medium">
                          {{ item.yoyChange > 0 ? '+' : '' }}${{ formatAmount(Math.abs(item.yoyChange)) }}
                        </div>
                        <div :class="getChangeColorClass(item.yoyChangePct)" class="text-xs">
                          ({{ item.yoyChangePct > 0 ? '+' : '' }}{{ item.yoyChangePct.toFixed(1) }}%)
                        </div>
                      </div>
                      <div v-else class="text-xs text-gray-400">-</div>
                    </td>
                  </tr>
                </tbody>
                <tfoot class="bg-gray-50 border-t-2 border-gray-300">
                  <tr>
                    <td class="px-2 py-2 text-sm font-bold text-gray-900">累计</td>
                    <td class="px-2 py-2 text-sm text-right font-bold text-blue-600 whitespace-nowrap">
                      ${{ formatAmount(totalNetWorth) }}
                    </td>
                    <td class="px-2 py-2 text-sm text-right whitespace-nowrap">
                      <div v-if="totalChange !== null && annualizedGrowthRate !== null">
                        <div :class="getChangeColorClass(totalChange)" class="font-bold">
                          {{ totalChange > 0 ? '+' : '' }}${{ formatAmount(Math.abs(totalChange)) }}
                        </div>
                        <div :class="getChangeColorClass(annualizedGrowthRate)" class="text-xs mt-0.5">
                          (年均{{ annualizedGrowthRate > 0 ? '+' : '' }}{{ annualizedGrowthRate.toFixed(2) }}%)
                        </div>
                      </div>
                    </td>
                  </tr>
                </tfoot>
              </table>
            </div>
          </div>
        </div>
      </div>
      </div>
    </div>

    <!-- Google Sheets同步弹窗 -->
    <GoogleSheetsSync
      :show="showGoogleSheetsSync"
      :familyId="familyId"
      :defaultYear="currentYear"
      @close="showGoogleSheetsSync = false"
      @success="onSyncSuccess"
    />

  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { assetAccountAPI, liabilityAccountAPI, familyAPI } from '@/api'
import { analysisAPI } from '@/api/analysis'
import { annualSummaryAPI } from '@/api/annualSummary'
import { expenseAnalysisAPI } from '@/api/expense'
import { incomeAnalysisAPI } from '@/api/income'
import GoogleSheetsSync from '@/components/GoogleSheetsSync.vue'
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  LineController,
  BarElement,
  BarController,
  ArcElement,
  DoughnutController,
  Title,
  Tooltip,
  Legend,
  Filler
} from 'chart.js'
import ChartDataLabels from 'chartjs-plugin-datalabels'

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  LineController,
  BarElement,
  BarController,
  ArcElement,
  DoughnutController,
  Title,
  Tooltip,
  Legend,
  Filler,
  ChartDataLabels
)

// ========== 图表配置常量 ==========
// 使用统一的颜色方案（Material Design）
const CHART_COLORS = [
  '#2196F3', // Blue 500
  '#4CAF50', // Green 500
  '#FF9800', // Orange 500
  '#F44336', // Red 500
  '#9C27B0', // Purple 500
  '#E91E63', // Pink 500
  '#00BCD4', // Cyan 500
  '#FF5722', // Deep Orange 500
  '#009688', // Teal 500
  '#8BC34A', // Light Green 500
  '#FFC107', // Amber 500
  '#673AB7'  // Deep Purple 500
]

// 图表默认配置
const CHART_DEFAULTS = {
  responsive: true,
  maintainAspectRatio: false,
  animation: {
    duration: 750,
    easing: 'easeInOutQuart'
  },
  interaction: {
    mode: 'index',
    intersect: false
  }
}

// 饼图配置生成器
const createDoughnutChartOptions = (formatFn, showDataLabels = true) => ({
  ...CHART_DEFAULTS,
  plugins: {
    legend: {
      display: false // 使用自定义图例
    },
    tooltip: {
      backgroundColor: 'rgba(0, 0, 0, 0.8)',
      padding: 12,
      titleFont: {
        size: 14,
        weight: 'bold'
      },
      bodyFont: {
        size: 13
      },
      cornerRadius: 6,
      displayColors: true,
      callbacks: {
        label: function(context) {
          const label = context.label || ''
          const value = context.parsed || 0
          const dataset = context.dataset
          const total = dataset.data.reduce((a, b) => Math.abs(a) + Math.abs(b), 0)
          const percentage = total > 0 ? ((Math.abs(value) / total) * 100).toFixed(1) : '0.0'
          return `${label}: $${formatFn(Math.abs(value))} (${percentage}%)`
        }
      }
    },
    datalabels: showDataLabels ? {
      color: '#fff',
      font: {
        weight: 'bold',
        size: 11,
        lineHeight: 1.2
      },
      formatter: (value, context) => {
        const dataset = context.dataset
        const total = dataset.data.reduce((a, b) => Math.abs(a) + Math.abs(b), 0)
        const percentage = total > 0 ? ((Math.abs(value) / total) * 100).toFixed(1) : '0.0'
        const label = context.chart.data.labels[context.dataIndex]

        if (percentage <= 5) return ''

        if (percentage > 10) {
          return `${label}\n${percentage}%`
        } else {
          return `${percentage}%`
        }
      },
      anchor: 'center',
      align: 'center',
      textAlign: 'center'
    } : false
  }
})

const userId = ref(1) // TODO: 从用户登录状态获取
const families = ref([])
const familyId = ref(null) // 将从默认家庭API获取
const showGoogleSheetsSync = ref(false)
const assetAccounts = ref([])
const liabilityAccounts = ref([])
const netAssetAllocation = ref({ total: 0, data: [] })
const summaryData = ref({ totalAssets: 0, totalLiabilities: 0, netWorth: 0 })
const loading = ref(false)
const loadingTrend = ref(false)
const loadingExpense = ref(false)
const loadingIncome = ref(false)

// 财务指标数据
const financialMetrics = ref({
  currentNetWorth: 0,
  lastYearNetWorth: 0,
  annualExpense: 0,
  annualInvestmentReturn: 0,
  annualWorkIncome: 0,
  year: new Date().getFullYear()
})

const assetChartCanvas = ref(null)
const liabilityChartCanvas = ref(null)
const netWorthChartCanvas = ref(null)
const annualNetWorthChartCanvas = ref(null)
const baseExpenseChartCanvas = ref(null)
const expenseChartCanvas = ref(null)
const incomeChartCanvas = ref(null)
let assetChartInstance = null
let liabilityChartInstance = null
let netWorthChartInstance = null
let annualNetWorthChartInstance = null
let baseExpenseChartInstance = null
let expenseChartInstance = null
let incomeChartInstance = null

// 使用当前年份
const currentYear = ref(new Date().getFullYear())
const annualExpenseSummary = ref([])
const annualIncomeSummary = ref([])

const selectedTimeRange = ref('3y')
const timeRanges = [
  { value: '1y', label: '近1年' },
  { value: '3y', label: '近3年' },
  { value: '5y', label: '近5年' },
  { value: '10y', label: '近10年' },
  { value: 'all', label: '全部' }
]

// 趋势数据
const overallTrendData = ref([])

// 资产分类定义（与TrendAnalysis保持一致）
const assetCategoryDefinitions = [
  { type: 'CASH', name: '现金类', color: 'rgb(34, 197, 94)' },
  { type: 'STOCKS', name: '股票投资', color: 'rgb(59, 130, 246)' },
  { type: 'RETIREMENT_FUND', name: '退休基金', color: 'rgb(168, 85, 247)' },
  { type: 'INSURANCE', name: '保险', color: 'rgb(251, 146, 60)' },
  { type: 'REAL_ESTATE', name: '房地产', color: 'rgb(239, 68, 68)' },
  { type: 'CRYPTOCURRENCY', name: '数字货币', color: 'rgb(234, 179, 8)' },
  { type: 'PRECIOUS_METALS', name: '贵金属', color: 'rgb(20, 184, 166)' },
  { type: 'OTHER', name: '其他', color: 'rgb(156, 163, 175)' }
]

// 负债分类定义（与TrendAnalysis保持一致）
const liabilityCategoryDefinitions = [
  { type: 'MORTGAGE', name: '房贷', color: 'rgb(220, 38, 38)' },
  { type: 'AUTO_LOAN', name: '车贷', color: 'rgb(234, 88, 12)' },
  { type: 'CREDIT_CARD', name: '信用卡', color: 'rgb(251, 146, 60)' },
  { type: 'PERSONAL_LOAN', name: '个人借款', color: 'rgb(249, 115, 22)' },
  { type: 'STUDENT_LOAN', name: '学生贷款', color: 'rgb(251, 191, 36)' },
  { type: 'BUSINESS_LOAN', name: '商业贷款', color: 'rgb(253, 224, 71)' },
  { type: 'OTHER', name: '其他', color: 'rgb(156, 163, 175)' }
]

// 创建type到定义的映射，用于快速查找
const assetCategoryMap = assetCategoryDefinitions.reduce((map, def) => {
  map[def.type] = def
  return map
}, {})

const liabilityCategoryMap = liabilityCategoryDefinitions.reduce((map, def) => {
  map[def.type] = def
  return map
}, {})

// 支出分类颜色映射（使用大类icon对应的颜色）
const EXPENSE_COLORS = [
  '#fb923c', // 橙色 - 子女
  '#ec4899', // 粉红色 - 衣
  '#ef4444', // 红色 - 食
  '#8b5cf6', // 紫色 - 住
  '#3b82f6', // 蓝色 - 行
  '#10b981', // 绿色 - 保险
  '#f59e0b', // 琥珀色 - 人情
  '#06b6d4', // 青色 - 娱乐
  '#84cc16', // 黄绿色 - 经营
  '#6b7280'  // 灰色 - 其他
]

// 资产类型颜色映射（与AssetAllocation.vue保持一致）
const ASSET_TYPE_COLORS = {
  '现金类': '#3b82f6',      // 蓝色
  '股票投资': '#8b5cf6',    // 紫色
  '退休基金': '#10b981',    // 绿色
  '保险': '#f59e0b',        // 琥珀色
  '房地产': '#ec4899',      // 粉红色
  '数字货币': '#06b6d4',    // 青色
  '贵金属': '#84cc16',      // 黄绿色
  '其他': '#6b7280'         // 灰色
}

// 计算总资产（使用分析API的summary数据，已考虑汇率转换）
const totalAssets = computed(() => {
  return summaryData.value.totalAssets || 0
})

// 计算总负债（使用分析API的summary数据，已考虑汇率转换）
const totalLiabilities = computed(() => {
  return summaryData.value.totalLiabilities || 0
})

// 计算净资产 - 使用summary数据（更准确，因为使用统一的汇率和计算逻辑）
const netWorth = computed(() => {
  return summaryData.value.netWorth || (totalAssets.value - totalLiabilities.value)
})

// 计算资产负债率
const debtRatio = computed(() => {
  if (totalAssets.value === 0) return '0.0'
  return ((totalLiabilities.value / totalAssets.value) * 100).toFixed(1)
})

// 资产负债率颜色
const debtRatioColor = computed(() => {
  const ratio = parseFloat(debtRatio.value)
  if (ratio < 30) return 'text-green-600'
  if (ratio < 50) return 'text-yellow-600'
  return 'text-red-600'
})

// 账户统计
const totalAssetAccounts = computed(() => assetAccounts.value.length)
const totalLiabilityAccounts = computed(() => liabilityAccounts.value.length)
const activeAssetAccounts = computed(() => assetAccounts.value.filter(acc => acc.isActive).length)
const activeLiabilityAccounts = computed(() => liabilityAccounts.value.filter(acc => acc.isActive).length)
const totalAccounts = computed(() => totalAssetAccounts.value + totalLiabilityAccounts.value)

// 净资产分类数据（从API获取，资产 - 对应负债）
const netAssetAllocationData = ref([])

// 资产分类数据（从API获取，按类型分类的资产）
const assetAllocationData = ref([])

// 净资产分类统计（用于净资产分布图表）
const netAssetCategories = computed(() => {
  if (!netAssetAllocationData.value || netAssetAllocationData.value.length === 0) {
    return []
  }

  // 显示所有非零净资产分类（包括负值）
  const categories = netAssetAllocationData.value.filter(cat => cat.netValue !== 0)

  // 计算总净资产（用于计算百分比）
  const totalPositive = categories
    .filter(cat => cat.netValue > 0)
    .reduce((sum, cat) => sum + cat.netValue, 0)

  return categories.map(cat => ({
    type: cat.code,
    name: cat.name,
    color: cat.color,
    total: cat.netValue,
    percentage: totalPositive > 0 ? ((cat.netValue / totalPositive) * 100).toFixed(1) : '0.0',
    isNegative: cat.netValue < 0
  })).sort((a, b) => b.total - a.total)
})

// 资产分类统计（用于资产分布图表）
const assetCategories = computed(() => {
  if (!assetAllocationData.value || assetAllocationData.value.length === 0) {
    return []
  }

  // 显示所有非零资产分类
  const categories = assetAllocationData.value.filter(cat => cat.value > 0)

  return categories.map(cat => ({
    name: cat.name,
    color: ASSET_TYPE_COLORS[cat.name] || '#6b7280', // 使用颜色映射，如果找不到则用灰色
    total: cat.value,
    percentage: cat.percentage.toFixed(1)
  })).sort((a, b) => b.total - a.total)
})

// 负债分类数据（从API获取，已考虑汇率转换）
const liabilityAllocationData = ref([])

// 负债分类统计（用于负债分布图表）
const liabilityCategories = computed(() => {
  if (!liabilityAllocationData.value || liabilityAllocationData.value.length === 0) {
    return []
  }

  // 显示所有非零负债分类
  const categories = liabilityAllocationData.value.filter(cat => cat.value > 0)

  return categories.map(cat => ({
    name: cat.name,
    color: liabilityCategoryDefinitions.find(def => def.name === cat.name)?.color || '#6b7280',
    total: cat.value,
    percentage: cat.percentage.toFixed(1)
  })).sort((a, b) => b.total - a.total)
})

// 支出分类统计（本年度实际支出）
const expenseCategories = computed(() => {
  // 过滤出大类汇总（minorCategoryName为null）且不是总计（majorCategoryId不为0）
  const majorCategories = annualExpenseSummary.value.filter(
    item => !item.minorCategoryName && item.majorCategoryId !== 0
  )

  if (majorCategories.length === 0) return []

  const total = majorCategories.reduce((sum, cat) => sum + Number(cat.actualExpenseAmount || 0), 0)

  return majorCategories.map((cat, index) => ({
    id: cat.majorCategoryId,
    name: cat.majorCategoryName,
    amount: Number(cat.actualExpenseAmount || 0),
    color: EXPENSE_COLORS[index % EXPENSE_COLORS.length],
    percentage: total > 0 ? ((Number(cat.actualExpenseAmount || 0) / total) * 100).toFixed(1) : '0.0'
  })).filter(cat => cat.amount > 0).sort((a, b) => b.amount - a.amount)
})

// 总支出
const totalExpense = computed(() => {
  return expenseCategories.value.reduce((sum, cat) => sum + cat.amount, 0)
})

// 总支出分类统计（本年度预算支出，不含实际调整）
const baseExpenseCategories = computed(() => {
  // 过滤出大类汇总（minorCategoryName为null）且不是总计（majorCategoryId不为0）
  const majorCategories = annualExpenseSummary.value.filter(
    item => !item.minorCategoryName && item.majorCategoryId !== 0
  )

  if (majorCategories.length === 0) return []

  const total = majorCategories.reduce((sum, cat) => sum + Number(cat.baseExpenseAmount || 0), 0)

  return majorCategories.map((cat, index) => ({
    id: cat.majorCategoryId,
    name: cat.majorCategoryName,
    amount: Number(cat.baseExpenseAmount || 0),
    color: EXPENSE_COLORS[index % EXPENSE_COLORS.length],
    percentage: total > 0 ? ((Number(cat.baseExpenseAmount || 0) / total) * 100).toFixed(1) : '0.0'
  })).filter(cat => cat.amount > 0).sort((a, b) => b.amount - a.amount)
})

// 总支出总计（预算金额）
const totalBaseExpense = computed(() => {
  return baseExpenseCategories.value.reduce((sum, cat) => sum + cat.amount, 0)
})

// 收入分类统计（本年度职业收入，排除投资收益）
const incomeCategories = computed(() => {
  // 过滤出大类汇总（minorCategoryName为null或minorCategoryId为null）
  // 排除投资收益 (majorCategoryName 包含 "投资" 或 "Investment")
  const majorCategories = annualIncomeSummary.value.filter(
    item => !item.minorCategoryName && item.majorCategoryId !== 0 &&
      !item.majorCategoryName?.includes('投资') && !item.majorCategoryName?.includes('Investment')
  )

  if (majorCategories.length === 0) return []

  const total = majorCategories.reduce((sum, cat) => sum + Number(cat.actualIncomeAmount || 0), 0)

  return majorCategories.map((cat, index) => ({
    id: cat.majorCategoryId,
    name: cat.majorCategoryChineseName || cat.majorCategoryName,
    amount: Number(cat.actualIncomeAmount || 0),
    color: CHART_COLORS[index % CHART_COLORS.length],
    percentage: total > 0 ? ((Number(cat.actualIncomeAmount || 0) / total) * 100).toFixed(1) : '0.0'
  })).filter(cat => cat.amount > 0).sort((a, b) => b.amount - a.amount)
})

// 职业收入总计（排除投资收益）
const careerIncome = computed(() => {
  return incomeCategories.value.reduce((sum, cat) => sum + cat.amount, 0)
})

// 实际支出总计
const actualExpense = computed(() => {
  return totalExpense.value
})

// 投资回报（使用财务指标中的数据）
const investmentReturn = computed(() => {
  return financialMetrics.value.annualInvestmentReturn || 0
})

// 获取变化百分比的颜色类
const getChangeColorClass = (change) => {
  if (change > 0) return 'text-green-600 font-medium'
  if (change < 0) return 'text-red-600 font-medium'
  return 'text-gray-600'
}

// 计算年份范围
const getYearRange = () => {
  let years = 5 // 默认5年

  switch (selectedTimeRange.value) {
    case '1y':
      years = 1
      break
    case '3y':
      years = 3
      break
    case '5y':
      years = 5
      break
    case '10y':
      years = 10
      break
    case 'all':
      years = 20
      break
  }

  return { years }
}

// 计算日期范围
const getDateRange = () => {
  const end = new Date()
  const start = new Date()

  switch (selectedTimeRange.value) {
    case '1y':
      start.setFullYear(end.getFullYear() - 1)
      break
    case '3y':
      start.setFullYear(end.getFullYear() - 3)
      break
    case '5y':
      start.setFullYear(end.getFullYear() - 5)
      break
    case '10y':
      start.setFullYear(end.getFullYear() - 10)
      break
    case 'all':
      start.setFullYear(end.getFullYear() - 20)
      break
  }

  return {
    start: start.toISOString().split('T')[0],
    end: end.toISOString().split('T')[0]
  }
}

// 计算年度汇总数据（用于右侧表格）
const annualSummaryData = computed(() => {
  if (overallTrendData.value.length === 0) return []

  const data = overallTrendData.value.map((item, index) => {
    const year = new Date(item.date).getFullYear()
    const netWorth = item.netWorth || 0

    let yoyChange = null
    let yoyChangePct = null

    if (index > 0) {
      const prevNetWorth = overallTrendData.value[index - 1].netWorth || 0
      yoyChange = netWorth - prevNetWorth
      yoyChangePct = prevNetWorth !== 0 ? (yoyChange / prevNetWorth) * 100 : 0
    }

    return {
      year,
      netWorth,
      yoyChange,
      yoyChangePct
    }
  })

  return data.reverse() // 最新年份在上
})

// 计算累计净资产（最新年份）
const totalNetWorth = computed(() => {
  if (annualSummaryData.value.length === 0) return 0
  return annualSummaryData.value[0].netWorth
})

// 计算总变化（从最早到最新）
const totalChange = computed(() => {
  if (annualSummaryData.value.length < 2) return null
  const latest = annualSummaryData.value[0].netWorth
  const earliest = annualSummaryData.value[annualSummaryData.value.length - 1].netWorth
  return latest - earliest
})

const totalChangePct = computed(() => {
  if (totalChange.value === null) return null
  const earliest = annualSummaryData.value[annualSummaryData.value.length - 1].netWorth
  return earliest !== 0 ? (totalChange.value / earliest) * 100 : 0
})

// 计算年化复合增长率（CAGR）
const annualizedGrowthRate = computed(() => {
  if (annualSummaryData.value.length < 2) return null
  const latest = annualSummaryData.value[0].netWorth
  const earliest = annualSummaryData.value[annualSummaryData.value.length - 1].netWorth
  const years = annualSummaryData.value[0].year - annualSummaryData.value[annualSummaryData.value.length - 1].year

  if (years === 0 || earliest <= 0) return null

  // CAGR = (最新值/最早值)^(1/年数) - 1
  return (Math.pow(latest / earliest, 1 / years) - 1) * 100
})

// 更新年度净资产图表（双Y轴：柱状图+折线图）
function updateAnnualNetWorthChart() {
  if (!annualNetWorthChartCanvas.value) {
    console.warn('Canvas element not ready')
    return
  }

  if (overallTrendData.value.length === 0) {
    console.warn('No trend data available')
    return
  }

  if (annualNetWorthChartInstance) {
    annualNetWorthChartInstance.destroy()
    annualNetWorthChartInstance = null
  }

  console.log('Creating annual net worth chart with', overallTrendData.value.length, 'data points')

  // 准备数据
  const years = overallTrendData.value.map(item => new Date(item.date).getFullYear())
  const netWorths = overallTrendData.value.map(item => item.netWorth || 0)

  // 计算同比增长率
  const growthRates = overallTrendData.value.map((item, index) => {
    if (index === 0) return null
    const current = item.netWorth || 0
    const previous = overallTrendData.value[index - 1].netWorth || 0
    return previous !== 0 ? ((current - previous) / previous) * 100 : 0
  })

  const ctx = annualNetWorthChartCanvas.value.getContext('2d')
  annualNetWorthChartInstance = new ChartJS(ctx, {
    type: 'bar',
    data: {
      labels: years,
      datasets: [
        {
          label: '净资产',
          data: netWorths,
          backgroundColor: 'rgba(59, 130, 246, 0.7)',
          borderColor: 'rgb(59, 130, 246)',
          borderWidth: 1,
          yAxisID: 'y'
        },
        {
          label: '同比增长率',
          data: growthRates,
          type: 'line',
          borderColor: 'rgb(234, 88, 12)',
          backgroundColor: 'rgba(234, 88, 12, 0.1)',
          borderWidth: 2,
          tension: 0.4,
          yAxisID: 'y1'
        }
      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      interaction: {
        mode: 'index',
        intersect: false,
      },
      plugins: {
        legend: {
          position: 'top',
        },
        tooltip: {
          callbacks: {
            label: function(context) {
              if (context.dataset.label === '净资产') {
                return context.dataset.label + ': $' + Number(context.parsed.y).toLocaleString('en-US', {
                  minimumFractionDigits: 2,
                  maximumFractionDigits: 2
                })
              } else {
                return context.dataset.label + ': ' + (context.parsed.y !== null ? context.parsed.y.toFixed(2) + '%' : '-')
              }
            }
          }
        }
      },
      scales: {
        y: {
          type: 'linear',
          display: true,
          position: 'left',
          beginAtZero: true,
          ticks: {
            callback: function(value) {
              if (value >= 1000000) {
                return '$' + (value / 1000000).toFixed(1) + 'M'
              } else if (value >= 1000) {
                return '$' + (value / 1000).toFixed(1) + 'K'
              }
              return '$' + value.toFixed(0)
            }
          }
        },
        y1: {
          type: 'linear',
          display: true,
          position: 'right',
          grid: {
            drawOnChartArea: false,
          },
          ticks: {
            callback: function(value) {
              return value.toFixed(1) + '%'
            }
          }
        }
      }
    }
  })
}

// 格式化金额
function formatAmount(amount) {
  if (!amount && amount !== 0) return '0.00'
  return Number(amount).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

// 格式化金额（带正负号）
function formatAmountWithSign(amount) {
  if (!amount && amount !== 0) return '0.00'
  const formatted = formatAmount(Math.abs(amount))
  return amount >= 0 ? formatted : `-${formatted}`
}

// 获取投资回报颜色
function getReturnColor(amount) {
  if (!amount && amount !== 0) return 'text-gray-600'
  return amount >= 0 ? 'text-green-600' : 'text-red-600'
}

// 加载财务指标数据
async function loadFinancialMetrics() {
  try {
    console.log('开始加载财务指标, familyId:', familyId.value)
    const response = await analysisAPI.getFinancialMetrics(null, familyId.value, null)
    console.log('财务指标响应:', response)
    if (response.success && response.data) {
      financialMetrics.value = {
        currentNetWorth: response.data.currentNetWorth || 0,
        lastYearNetWorth: response.data.lastYearNetWorth || 0,
        annualExpense: response.data.annualExpense || 0,
        annualInvestmentReturn: response.data.annualInvestmentReturn || 0,
        annualWorkIncome: response.data.annualWorkIncome || 0,
        year: response.data.year || new Date().getFullYear()
      }
      console.log('财务指标已设置:', financialMetrics.value)
    } else {
      console.warn('财务指标响应失败或无数据:', response)
    }
  } catch (error) {
    console.error('加载财务指标失败:', error)
  }
}

// 加载账户数据
async function loadAccounts() {
  loading.value = true
  try {
    // 使用familyId过滤所有API调用
    const [assetResponse, liabilityResponse, netAssetResponse, assetAllocationResponse, liabilityAllocationResponse, summaryResponse] = await Promise.all([
      assetAccountAPI.getAllByFamily(familyId.value),
      liabilityAccountAPI.getAllByFamily(familyId.value),
      analysisAPI.getNetAssetAllocation(null, familyId.value, null), // userId, familyId, asOfDate - 净资产分布
      analysisAPI.getAllocationByType(null, familyId.value, null), // userId, familyId, asOfDate - 资产分布（按类型）
      analysisAPI.getLiabilityAllocation(null, familyId.value, null), // userId, familyId, asOfDate - 负债分布（按类型）
      analysisAPI.getSummary(null, familyId.value, null), // userId, familyId, asOfDate
      loadFinancialMetrics() // await财务指标加载
    ])

    if (assetResponse.success) {
      assetAccounts.value = assetResponse.data
    }

    if (liabilityResponse.success) {
      liabilityAccounts.value = liabilityResponse.data
    }

    if (netAssetResponse.success && netAssetResponse.data && netAssetResponse.data.data) {
      netAssetAllocationData.value = netAssetResponse.data.data
    }

    if (assetAllocationResponse.success && assetAllocationResponse.data && assetAllocationResponse.data.data) {
      assetAllocationData.value = assetAllocationResponse.data.data
    }

    if (liabilityAllocationResponse.success && liabilityAllocationResponse.data && liabilityAllocationResponse.data.data) {
      liabilityAllocationData.value = liabilityAllocationResponse.data.data
    }

    if (summaryResponse.success && summaryResponse.data) {
      summaryData.value = summaryResponse.data
    }

    await nextTick()
    updateCharts()
  } catch (error) {
    console.error('加载账户失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载综合趋势
async function loadOverallTrend() {
  loadingTrend.value = true
  try {
    // 使用AnnualSummary API代替OverallTrend API以确保数据一致性
    const { years } = getYearRange()
    const response = await annualSummaryAPI.getRecent(familyId.value, years) // 使用当前选择的familyId
    if (response.success) {
      // 转换AnnualSummary数据格式为OverallTrend格式
      overallTrendData.value = response.data
        .map(item => ({
          date: item.summaryDate,
          netWorth: item.netWorth,
          totalAssets: item.totalAssets,
          totalLiabilities: item.totalLiabilities
        }))
        .reverse() // AnnualSummary返回的是倒序，需要reverse
    }
  } catch (error) {
    console.error('加载综合趋势失败:', error)
    overallTrendData.value = []
  } finally {
    loadingTrend.value = false
  }
}

// 选择时间范围
function selectTimeRange(range) {
  selectedTimeRange.value = range
  loadOverallTrend()
}

// 更新图表
function updateCharts() {
  updateNetWorthChart()
  updateAssetChart()
  updateLiabilityChart()
}

// 更新净资产分布图表
function updateNetWorthChart() {
  if (!netWorthChartCanvas.value || netAssetCategories.value.length === 0) {
    if (netWorthChartInstance) {
      netWorthChartInstance.destroy()
      netWorthChartInstance = null
    }
    return
  }

  if (netWorthChartInstance) {
    netWorthChartInstance.destroy()
    netWorthChartInstance = null
  }

  const ctx = netWorthChartCanvas.value.getContext('2d')

  // 准备数据
  const chartData = netAssetCategories.value.map(c => ({
    label: c.name,
    value: Math.abs(c.total),
    color: c.color,
    isNegative: c.isNegative
  }))

  netWorthChartInstance = new ChartJS(ctx, {
    type: 'doughnut',
    data: {
      labels: chartData.map(c => c.label),
      datasets: [{
        data: chartData.map(c => c.value),
        backgroundColor: chartData.map(c => c.color),
        borderWidth: 2,
        borderColor: '#fff',
        hoverBorderWidth: 3,
        hoverBorderColor: '#fff',
        hoverOffset: 10
      }]
    },
    options: createDoughnutChartOptions(formatAmount, true)
  })
}

// 更新资产分布图表
function updateAssetChart() {
  if (!assetChartCanvas.value || assetCategories.value.length === 0) {
    if (assetChartInstance) {
      assetChartInstance.destroy()
      assetChartInstance = null
    }
    return
  }

  if (assetChartInstance) {
    assetChartInstance.destroy()
    assetChartInstance = null
  }

  const ctx = assetChartCanvas.value.getContext('2d')

  assetChartInstance = new ChartJS(ctx, {
    type: 'doughnut',
    data: {
      labels: assetCategories.value.map(c => c.name),
      datasets: [{
        data: assetCategories.value.map(c => c.total),
        backgroundColor: assetCategories.value.map(c => c.color),
        borderWidth: 2,
        borderColor: '#fff',
        hoverBorderWidth: 3,
        hoverBorderColor: '#fff',
        hoverOffset: 10
      }]
    },
    options: createDoughnutChartOptions(formatAmount, true)
  })
}

// 更新负债分布图表
function updateLiabilityChart() {
  if (!liabilityChartCanvas.value || liabilityCategories.value.length === 0) {
    if (liabilityChartInstance) {
      liabilityChartInstance.destroy()
      liabilityChartInstance = null
    }
    return
  }

  if (liabilityChartInstance) {
    liabilityChartInstance.destroy()
    liabilityChartInstance = null
  }

  const ctx = liabilityChartCanvas.value.getContext('2d')

  liabilityChartInstance = new ChartJS(ctx, {
    type: 'doughnut',
    data: {
      labels: liabilityCategories.value.map(c => c.name),
      datasets: [{
        data: liabilityCategories.value.map(c => c.total),
        backgroundColor: liabilityCategories.value.map(c => c.color),
        borderWidth: 2,
        borderColor: '#fff',
        hoverBorderWidth: 3,
        hoverBorderColor: '#fff',
        hoverOffset: 10
      }]
    },
    options: createDoughnutChartOptions(formatAmount, true)
  })
}

// 更新总支出分布图表（预算金额）
function updateBaseExpenseChart() {
  if (!baseExpenseChartCanvas.value || baseExpenseCategories.value.length === 0) {
    if (baseExpenseChartInstance) {
      baseExpenseChartInstance.destroy()
      baseExpenseChartInstance = null
    }
    return
  }

  if (baseExpenseChartInstance) {
    baseExpenseChartInstance.destroy()
    baseExpenseChartInstance = null
  }

  const ctx = baseExpenseChartCanvas.value.getContext('2d')

  baseExpenseChartInstance = new ChartJS(ctx, {
    type: 'doughnut',
    data: {
      labels: baseExpenseCategories.value.map(c => c.name),
      datasets: [{
        data: baseExpenseCategories.value.map(c => c.amount),
        backgroundColor: baseExpenseCategories.value.map(c => c.color),
        borderWidth: 2,
        borderColor: '#fff',
        hoverBorderWidth: 3,
        hoverBorderColor: '#fff',
        hoverOffset: 10
      }]
    },
    options: createDoughnutChartOptions(formatAmount, true)
  })
}

// 更新支出分布图表（实际金额）
function updateExpenseChart() {
  if (!expenseChartCanvas.value || expenseCategories.value.length === 0) {
    if (expenseChartInstance) {
      expenseChartInstance.destroy()
      expenseChartInstance = null
    }
    return
  }

  if (expenseChartInstance) {
    expenseChartInstance.destroy()
    expenseChartInstance = null
  }

  const ctx = expenseChartCanvas.value.getContext('2d')

  expenseChartInstance = new ChartJS(ctx, {
    type: 'doughnut',
    data: {
      labels: expenseCategories.value.map(c => c.name),
      datasets: [{
        data: expenseCategories.value.map(c => c.amount),
        backgroundColor: expenseCategories.value.map(c => c.color),
        borderWidth: 2,
        borderColor: '#fff',
        hoverBorderWidth: 3,
        hoverBorderColor: '#fff',
        hoverOffset: 10
      }]
    },
    options: createDoughnutChartOptions(formatAmount, true)
  })
}

// 更新收入分布图表
function updateIncomeChart() {
  if (!incomeChartCanvas.value || incomeCategories.value.length === 0) {
    if (incomeChartInstance) {
      incomeChartInstance.destroy()
      incomeChartInstance = null
    }
    return
  }

  if (incomeChartInstance) {
    incomeChartInstance.destroy()
    incomeChartInstance = null
  }

  const ctx = incomeChartCanvas.value.getContext('2d')

  incomeChartInstance = new ChartJS(ctx, {
    type: 'doughnut',
    data: {
      labels: incomeCategories.value.map(c => c.name),
      datasets: [{
        data: incomeCategories.value.map(c => c.amount),
        backgroundColor: incomeCategories.value.map(c => c.color),
        borderWidth: 2,
        borderColor: '#fff',
        hoverBorderWidth: 3,
        hoverBorderColor: '#fff',
        hoverOffset: 10
      }]
    },
    options: createDoughnutChartOptions(formatAmount, true)
  })
}

// 加载年度支出汇总数据
async function loadAnnualExpenseSummary() {
  loadingExpense.value = true
  try {
    console.log('开始加载年度支出汇总, familyId:', familyId.value, 'year:', currentYear.value)
    const response = await expenseAnalysisAPI.getAnnualSummary(
      familyId.value,
      currentYear.value,
      'USD',
      true
    )
    console.log('年度支出响应:', response)

    if (response.success) {
      annualExpenseSummary.value = response.data || []
      console.log('年度支出数据已设置:', annualExpenseSummary.value.length, '条记录')
      // 使用setTimeout确保DOM完全渲染后再更新图表
      setTimeout(() => {
        if (baseExpenseChartCanvas.value && baseExpenseCategories.value.length > 0) {
          updateBaseExpenseChart()
        }
        if (expenseChartCanvas.value && expenseCategories.value.length > 0) {
          updateExpenseChart()
        }
      }, 200)
    } else {
      console.warn('年度支出响应失败或无数据:', response)
    }
  } catch (error) {
    console.error('加载年度支出汇总失败:', error)
    annualExpenseSummary.value = []
  } finally {
    loadingExpense.value = false
  }
}

// 加载年度收入汇总数据
async function loadAnnualIncomeSummary() {
  loadingIncome.value = true
  try {
    console.log('开始加载年度收入汇总, familyId:', familyId.value, 'year:', currentYear.value)
    const response = await incomeAnalysisAPI.getAnnualMajorCategories(
      familyId.value,
      currentYear.value,
      'USD'
    )
    console.log('年度收入响应:', response)

    if (response.success) {
      annualIncomeSummary.value = response.data || []
      console.log('年度收入数据已设置:', annualIncomeSummary.value.length, '条记录')
      // 使用setTimeout确保DOM完全渲染后再更新图表
      setTimeout(() => {
        if (incomeChartCanvas.value && incomeCategories.value.length > 0) {
          updateIncomeChart()
        }
      }, 200)
    } else {
      console.warn('年度收入响应失败或无数据:', response)
    }
  } catch (error) {
    console.error('加载年度收入汇总失败:', error)
    annualIncomeSummary.value = []
  } finally {
    loadingIncome.value = false
  }
}

// 监听趋势数据变化，自动更新图表
watch(overallTrendData, async () => {
  if (overallTrendData.value.length > 0) {
    // 使用 nextTick 确保 DOM 完全渲染，并增加重试机制
    await nextTick()

    // 如果 canvas 还没准备好，等待最多 500ms
    let retries = 0
    const maxRetries = 5

    const tryUpdateChart = () => {
      if (annualNetWorthChartCanvas.value) {
        updateAnnualNetWorthChart()
      } else if (retries < maxRetries) {
        retries++
        setTimeout(tryUpdateChart, 100)
      } else {
        console.warn('年度净资产图表 canvas 元素未找到')
      }
    }

    tryUpdateChart()
  }
}, { deep: true })

// 加载家庭列表
async function loadFamilies() {
  try {
    console.log('开始加载家庭信息...')

    // Get current user's default family
    const defaultResponse = await familyAPI.getDefault()
    console.log('默认家庭响应:', defaultResponse)

    if (defaultResponse.success && defaultResponse.data) {
      const defaultFamily = defaultResponse.data

      // Set families list to only include user's family
      families.value = [defaultFamily]
      console.log('已加载家庭:', families.value)

      // Set the familyId
      if (!familyId.value) {
        familyId.value = defaultFamily.id
        console.log('设置家庭ID:', familyId.value)
      }
    }
  } catch (error) {
    console.error('加载家庭信息失败:', error)
    // Don't redirect to login here - the response interceptor will handle 401
  }
}

// 家庭切换事件处理
function onFamilyChange() {
  // 重新加载所有数据
  loadAccounts()
  loadOverallTrend()
  loadAnnualExpenseSummary()
  loadAnnualIncomeSummary()
}


// Google Sheets同步成功回调
function onSyncSuccess(data) {
  console.log('同步到Google Sheets成功:', data)
  // 可以在这里显示成功通知
}

// 监听总支出数据变化，自动更新图表
watch(baseExpenseCategories, async () => {
  if (baseExpenseCategories.value.length > 0) {
    await nextTick()
    updateBaseExpenseChart()
  }
}, { deep: true })

// 监听实际支出数据变化，自动更新图表
watch(expenseCategories, async () => {
  if (expenseCategories.value.length > 0) {
    await nextTick()
    updateExpenseChart()
  }
}, { deep: true })

// 监听收入数据变化，自动更新图表
watch(incomeCategories, async () => {
  if (incomeCategories.value.length > 0) {
    await nextTick()
    updateIncomeChart()
  }
}, { deep: true })

// 监听familyId变化，自动加载数据
watch(familyId, (newId) => {
  if (newId) {
    loadAccounts()
    loadOverallTrend()
    loadAnnualExpenseSummary()
    loadAnnualIncomeSummary()
  }
})

// 清理所有图表实例
function destroyAllCharts() {
  if (netWorthChartInstance) {
    netWorthChartInstance.destroy()
    netWorthChartInstance = null
  }
  if (assetChartInstance) {
    assetChartInstance.destroy()
    assetChartInstance = null
  }
  if (liabilityChartInstance) {
    liabilityChartInstance.destroy()
    liabilityChartInstance = null
  }
  if (baseExpenseChartInstance) {
    baseExpenseChartInstance.destroy()
    baseExpenseChartInstance = null
  }
  if (expenseChartInstance) {
    expenseChartInstance.destroy()
    expenseChartInstance = null
  }
  if (incomeChartInstance) {
    incomeChartInstance.destroy()
    incomeChartInstance = null
  }
  if (annualNetWorthChartInstance) {
    annualNetWorthChartInstance.destroy()
    annualNetWorthChartInstance = null
  }
}

// 响应式窗口大小调整
let resizeTimer = null
function handleResize() {
  if (resizeTimer) clearTimeout(resizeTimer)

  resizeTimer = setTimeout(() => {
    // 重新渲染所有活动的图表
    if (netAssetCategories.value.length > 0) updateNetWorthChart()
    if (assetCategories.value.length > 0) updateAssetChart()
    if (liabilityCategories.value.length > 0) updateLiabilityChart()
    if (incomeCategories.value.length > 0) updateIncomeChart()
    if (baseExpenseCategories.value.length > 0) updateBaseExpenseChart()
    if (expenseCategories.value.length > 0) updateExpenseChart()
    if (overallTrendData.value.length > 0) updateAnnualNetWorthChart()
  }, 250) // 250ms 防抖
}

onMounted(async () => {
  await loadFamilies()
  // loadFamilies会设置familyId，然后watcher会自动加载数据

  // 添加窗口大小调整监听
  window.addEventListener('resize', handleResize)
})

// 组件卸载时清理
onBeforeUnmount(() => {
  destroyAllCharts()
  window.removeEventListener('resize', handleResize)
  if (resizeTimer) clearTimeout(resizeTimer)
})
</script>
