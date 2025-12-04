<template>
  <div class="space-y-6">
    <!-- 货币选择器和日期选择器 -->
    <div class="bg-white rounded-lg shadow p-4">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-lg font-semibold text-gray-900">资产配置</h2>
        <div class="flex items-center gap-4">
          <div class="flex items-center gap-2">
            <label class="text-sm font-medium text-gray-700">查询日期：</label>
            <input
              v-model="selectedDate"
              type="date"
              @change="onDateChange"
              class="px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary text-sm"
            />
            <button
              v-if="selectedDate"
              @click="clearDate"
              class="px-3 py-2 text-sm text-gray-600 hover:text-gray-800"
            >
              清除
            </button>
          </div>
          <div class="flex items-center gap-2">
            <label class="text-sm font-medium text-gray-700">显示货币：</label>
            <select
              v-model="selectedCurrency"
              @change="onCurrencyChange"
              class="px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary text-sm"
            >
              <option value="USD">美元 (USD)</option>
              <option value="CNY">人民币 (CNY)</option>
            </select>
          </div>
        </div>
      </div>
      <div class="text-sm text-gray-600">
        <span v-if="!selectedDate && actualDataDate" class="flex items-center gap-2">
          <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4 text-blue-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
            <line x1="16" y1="2" x2="16" y2="6"></line>
            <line x1="8" y1="2" x2="8" y2="6"></line>
            <line x1="3" y1="10" x2="21" y2="10"></line>
          </svg>
          <span>
            <span class="font-medium text-gray-700">数据日期:</span>
            <span class="text-blue-600 font-semibold">{{ formatDate(actualDataDate) }}</span>
            <span class="text-gray-500 ml-2">(显示最新可用数据)</span>
          </span>
        </span>
        <span v-else-if="selectedDate">
          <span class="font-medium text-gray-700">查询日期:</span> {{ selectedDate }}
          <span v-if="actualDataDate && actualDataDate !== selectedDate" class="ml-2">
            <span class="text-gray-500">→ 实际数据日期: </span>
            <span class="text-blue-600 font-semibold">{{ formatDate(actualDataDate) }}</span>
          </span>
        </span>
      </div>
    </div>

    <!-- 总览卡片作为导航 -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
      <!-- 净资产卡片 -->
      <button
        @click="switchTab('net')"
        :class="[
          'bg-white rounded-lg shadow p-4 text-left transition-all transform hover:scale-105',
          activeTab === 'net' ? 'ring-2 ring-blue-500 shadow-lg' : 'hover:shadow-md'
        ]"
      >
        <div class="text-xs text-gray-500 mb-1">净资产 ({{ selectedCurrency }})</div>
        <div class="text-2xl font-bold text-blue-600">
          {{ currencySymbol }}{{ formatNumber(convertedSummary.netWorth) }}
        </div>
        <div v-if="activeTab === 'net'" class="text-xs text-blue-600 mt-1">已选中</div>
      </button>

      <!-- 总资产卡片 -->
      <button
        @click="switchTab('asset')"
        :class="[
          'bg-white rounded-lg shadow p-4 text-left transition-all transform hover:scale-105',
          activeTab === 'asset' ? 'ring-2 ring-green-500 shadow-lg' : 'hover:shadow-md'
        ]"
      >
        <div class="text-xs text-gray-500 mb-1">总资产 ({{ selectedCurrency }})</div>
        <div class="text-2xl font-bold text-green-600">
          {{ currencySymbol }}{{ formatNumber(convertedSummary.totalAssets) }}
        </div>
        <div v-if="activeTab === 'asset'" class="text-xs text-green-600 mt-1">已选中</div>
      </button>

      <!-- 总负债卡片 -->
      <button
        @click="switchTab('liability')"
        :class="[
          'bg-white rounded-lg shadow p-4 text-left transition-all transform hover:scale-105',
          activeTab === 'liability' ? 'ring-2 ring-red-500 shadow-lg' : 'hover:shadow-md'
        ]"
      >
        <div class="text-xs text-gray-500 mb-1">总负债 ({{ selectedCurrency }})</div>
        <div class="text-2xl font-bold text-red-600">
          {{ currencySymbol }}{{ formatNumber(convertedSummary.totalLiabilities) }}
        </div>
        <div v-if="activeTab === 'liability'" class="text-xs text-red-600 mt-1">已选中</div>
      </button>
    </div>

    <!-- 内容区域 -->
    <div class="bg-white rounded-lg shadow p-6">
      <!-- 净资产tab: 两行布局 -->
      <div v-if="activeTab === 'net'" class="space-y-6">
        <!-- 第一行: 净资产分类饼图 + 分类列表 -->
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <!-- 主饼图 -->
          <div class="h-80 flex items-center justify-center">
            <div v-if="loading" class="text-gray-500">加载中...</div>
            <div v-else-if="!currentAllocation || currentAllocation.data.length === 0" class="text-gray-500">暂无数据</div>
            <Pie v-else :data="currentChartData" :options="mainPieChartOptions" />
          </div>

          <!-- 详细列表或钻取饼图 -->
          <div>
          <!-- 未选中类别时显示列表 -->
          <div v-if="!selectedCategory" class="space-y-2">
            <div
              v-for="item in currentAllocation?.data || []"
              :key="item.name || item.code"
              class="flex items-center justify-between p-3 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors"
            >
              <div class="flex items-center gap-3">
                <input
                  type="checkbox"
                  :checked="!isExcluded(item)"
                  @change="toggleCategory(item)"
                  class="w-4 h-4 text-blue-600 rounded focus:ring-2 focus:ring-blue-500"
                />
                <div class="w-4 h-4 rounded" :style="{ backgroundColor: item.color || getItemColor(item.name) }"></div>
                <span class="font-medium cursor-pointer" @click="selectCategory(item)">{{ item.name }}</span>
              </div>
              <div class="text-right">
                <div class="font-semibold">{{ currencySymbol }}{{ formatNumber(convertValue(getItemValue(item))) }}</div>
                <div class="text-sm text-gray-500">
                  {{ formatNumber(getAdjustedPercentage(item)) }}%
                </div>
              </div>
            </div>
          </div>

          <!-- 选中类别时显示钻取饼图或表格 -->
          <div v-else class="space-y-4">
            <div class="flex items-center justify-between">
              <h4 class="font-semibold">{{ selectedCategory.name }} - 账户分布</h4>
              <button
                @click="selectedCategory = null; drillDownData = null"
                class="text-sm text-gray-500 hover:text-gray-700"
              >
                返回
              </button>
            </div>

            <!-- 净资产显示表格 -->
            <div v-if="activeTab === 'net'" class="space-y-4">
              <div v-if="loadingDrillDown" class="text-center text-gray-500 py-8">加载中...</div>
              <div v-else-if="!drillDownData || (!drillDownData.assetAccounts?.length && !drillDownData.liabilityAccounts?.length)"
                   class="text-center text-gray-500 py-8">
                暂无账户数据
              </div>
              <div v-else class="space-y-4">
                <!-- 资产账户表格 -->
                <div v-if="drillDownData.assetAccounts?.length > 0">
                  <h5 class="text-sm font-semibold text-green-700 mb-2">资产账户</h5>
                  <div class="border border-gray-200 rounded-lg overflow-hidden">
                    <table class="min-w-full divide-y divide-gray-200">
                      <thead class="bg-green-50">
                        <tr>
                          <th class="px-4 py-2 text-left text-xs font-medium text-gray-700">账户名称</th>
                          <th class="px-4 py-2 text-left text-xs font-medium text-gray-700">分类</th>
                          <th class="px-4 py-2 text-right text-xs font-medium text-gray-700">余额</th>
                        </tr>
                      </thead>
                      <tbody class="bg-white divide-y divide-gray-200">
                        <tr v-for="account in drillDownData.assetAccounts" :key="account.accountId" class="hover:bg-gray-50">
                          <td class="px-4 py-2 text-sm text-gray-900">{{ account.accountName }}</td>
                          <td class="px-4 py-2 text-sm text-gray-600">{{ account.categoryName }}</td>
                          <td class="px-4 py-2 text-sm text-right font-medium text-green-600">
                            {{ currencySymbol }}{{ formatNumber(convertValue(account.balance)) }}
                          </td>
                        </tr>
                        <!-- 资产小计 -->
                        <tr class="bg-green-100 font-semibold">
                          <td class="px-4 py-2 text-sm text-gray-900" colspan="2">资产小计</td>
                          <td class="px-4 py-2 text-sm text-right text-green-700">
                            {{ currencySymbol }}{{ formatNumber(convertValue(drillDownData.assetAccounts.reduce((sum, a) => sum + a.balance, 0))) }}
                          </td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </div>

                <!-- 负债账户表格 -->
                <div v-if="drillDownData.liabilityAccounts?.length > 0">
                  <h5 class="text-sm font-semibold text-red-700 mb-2">负债账户</h5>
                  <div class="border border-gray-200 rounded-lg overflow-hidden">
                    <table class="min-w-full divide-y divide-gray-200">
                      <thead class="bg-red-50">
                        <tr>
                          <th class="px-4 py-2 text-left text-xs font-medium text-gray-700">账户名称</th>
                          <th class="px-4 py-2 text-left text-xs font-medium text-gray-700">分类</th>
                          <th class="px-4 py-2 text-right text-xs font-medium text-gray-700">余额</th>
                        </tr>
                      </thead>
                      <tbody class="bg-white divide-y divide-gray-200">
                        <tr v-for="account in drillDownData.liabilityAccounts" :key="account.accountId" class="hover:bg-gray-50">
                          <td class="px-4 py-2 text-sm text-gray-900">{{ account.accountName }}</td>
                          <td class="px-4 py-2 text-sm text-gray-600">{{ account.categoryName }}</td>
                          <td class="px-4 py-2 text-sm text-right font-medium text-red-600">
                            {{ currencySymbol }}{{ formatNumber(convertValue(account.balance)) }}
                          </td>
                        </tr>
                        <!-- 负债小计 -->
                        <tr class="bg-red-100 font-semibold">
                          <td class="px-4 py-2 text-sm text-gray-900" colspan="2">负债小计</td>
                          <td class="px-4 py-2 text-sm text-right text-red-700">
                            {{ currencySymbol }}{{ formatNumber(convertValue(drillDownData.liabilityAccounts.reduce((sum, a) => sum + a.balance, 0))) }}
                          </td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </div>

                <!-- 净值总计 -->
                <div class="text-center p-3 bg-blue-50 rounded-lg border border-blue-200">
                  <div class="text-xs text-gray-600">{{ selectedCategory?.name || '该类型' }}净值</div>
                  <div class="text-xl font-bold text-blue-700">
                    {{ currencySymbol }}{{ formatNumber(convertValue(
                      (drillDownData.assetAccounts?.reduce((sum, a) => sum + a.balance, 0) || 0) -
                      (drillDownData.liabilityAccounts?.reduce((sum, a) => sum + a.balance, 0) || 0)
                    )) }}
                  </div>
                </div>
              </div>
            </div>

            <!-- 资产和负债显示饼图 -->
            <div v-else class="space-y-3">
              <div class="h-64 flex items-center justify-center">
                <div v-if="loadingDrillDown" class="text-gray-500">加载中...</div>
                <div v-else-if="!drillDownChartData || drillDownData.length === 0" class="text-gray-500">暂无账户数据</div>
                <Pie v-else :data="drillDownChartData" :options="drillDownPieChartOptions" />
              </div>
              <!-- 在饼图下方显示类型总计 -->
              <div v-if="drillDownData && drillDownData.length > 0" class="text-center p-3 bg-gray-50 rounded-lg">
                <div class="text-xs text-gray-500">{{ selectedCategory?.name || '该类型' }}总计</div>
                <div class="text-xl font-bold text-gray-900">
                  {{ currencySymbol }}{{ formatNumber(convertValue(drillDownData.reduce((sum, item) => sum + item.balance, 0))) }}
                </div>
              </div>
            </div>
          </div>
        </div>
        </div>

        <!-- 第二行: 税收状态饼图 + 状态列表 -->
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <!-- 税收状态饼图 -->
          <div class="h-80 flex flex-col">
            <h4 class="font-semibold mb-4 text-center">税收状态分布</h4>
            <div class="flex-1 flex items-center justify-center">
              <div v-if="loadingTaxStatus" class="text-gray-500">加载中...</div>
              <div v-else-if="!taxStatusAllocation || taxStatusAllocation.data.length === 0" class="text-gray-500">暂无数据</div>
              <Pie v-else :data="taxStatusChartData" :options="taxStatusPieChartOptions" />
            </div>
          </div>

          <!-- 税收状态列表 -->
          <div>
            <div class="space-y-2">
              <div
                v-for="item in taxStatusAllocation?.data || []"
                :key="item.taxStatus"
                class="flex items-center justify-between p-3 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors"
              >
                <div class="flex items-center gap-3">
                  <div class="w-4 h-4 rounded" :style="{ backgroundColor: getTaxStatusColor(item.taxStatus) }"></div>
                  <span class="font-medium">{{ item.name }}</span>
                </div>
                <div class="text-right">
                  <div class="font-semibold">{{ currencySymbol }}{{ formatNumber(convertValue(item.value)) }}</div>
                  <div class="text-sm text-gray-500">
                  {{ formatNumber(getAdjustedPercentage(item)) }}%
                </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 第三行: 家庭成员分布饼图 + 成员列表 -->
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <!-- 家庭成员饼图 -->
          <div class="h-80 flex flex-col">
            <h4 class="font-semibold mb-4 text-center">家庭成员分布</h4>
            <div class="flex-1 flex items-center justify-center">
              <div v-if="loadingMember" class="text-gray-500">加载中...</div>
              <div v-else-if="!memberAllocation || memberAllocation.data.length === 0" class="text-gray-500">暂无数据</div>
              <Pie v-else :data="memberChartData" :options="memberPieChartOptions" />
            </div>
          </div>

          <!-- 家庭成员列表 -->
          <div>
            <div class="space-y-2">
              <div
                v-for="item in memberAllocation?.data || []"
                :key="item.userId"
                class="flex items-center justify-between p-3 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors"
              >
                <div class="flex items-center gap-3">
                  <div class="w-4 h-4 rounded" :style="{ backgroundColor: defaultColors[memberAllocation.data.indexOf(item) % defaultColors.length] }"></div>
                  <div>
                    <div class="font-medium">{{ item.displayName || item.userName }}</div>
                    <div class="text-xs text-gray-500">
                      资产: {{ currencySymbol }}{{ formatNumber(convertValue(item.assets)) }} |
                      负债: {{ currencySymbol }}{{ formatNumber(convertValue(item.liabilities)) }}
                    </div>
                  </div>
                </div>
                <div class="text-right">
                  <div class="font-semibold">{{ currencySymbol }}{{ formatNumber(convertValue(item.value)) }}</div>
                  <div class="text-sm text-gray-500">
                    {{ formatNumber(item.percentage) }}%
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 第四行: 货币分布饼图 + 货币列表 -->
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <!-- 货币分布饼图 -->
          <div class="h-80 flex flex-col">
            <h4 class="font-semibold mb-4 text-center">货币分布</h4>
            <div class="flex-1 flex items-center justify-center">
              <div v-if="loadingCurrency" class="text-gray-500">加载中...</div>
              <div v-else-if="!currencyAllocation || currencyAllocation.data.length === 0" class="text-gray-500">暂无数据</div>
              <Pie v-else :data="currencyChartData" :options="currencyPieChartOptions" />
            </div>
          </div>

          <!-- 货币列表 -->
          <div>
            <div class="space-y-2">
              <div
                v-for="item in currencyAllocation?.data || []"
                :key="item.currency"
                class="flex items-center justify-between p-3 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors"
              >
                <div class="flex items-center gap-3">
                  <div class="w-4 h-4 rounded" :style="{ backgroundColor: getCurrencyColor(item.currency) }"></div>
                  <div>
                    <div class="font-medium">{{ item.name }}</div>
                    <div class="text-xs text-gray-500">
                      资产: {{ getCurrencySymbol(item.currency) }}{{ formatNumber(item.assets) }} |
                      负债: {{ getCurrencySymbol(item.currency) }}{{ formatNumber(item.liabilities) }}
                    </div>
                  </div>
                </div>
                <div class="text-right">
                  <div class="font-semibold">{{ getCurrencySymbol(item.currency) }}{{ formatNumber(item.value) }}</div>
                  <div class="text-sm text-gray-500">
                    {{ formatNumber(item.percentage) }}%
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 资产和负债tab: 保持原有的两列布局 -->
      <div v-else class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <!-- 主饼图 -->
        <div class="h-80 flex items-center justify-center">
          <div v-if="loading" class="text-gray-500">加载中...</div>
          <div v-else-if="!currentAllocation || currentAllocation.data.length === 0" class="text-gray-500">暂无数据</div>
          <Pie v-else :data="currentChartData" :options="mainPieChartOptions" />
        </div>

        <!-- 详细列表或钻取饼图 -->
        <div>
          <!-- 未选中类别时显示列表 -->
          <div v-if="!selectedCategory" class="space-y-2">
            <div
              v-for="item in currentAllocation?.data || []"
              :key="item.name || item.code"
              class="flex items-center justify-between p-3 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors"
            >
              <div class="flex items-center gap-3">
                <input
                  type="checkbox"
                  :checked="!isExcluded(item)"
                  @change="toggleCategory(item)"
                  class="w-4 h-4 text-blue-600 rounded focus:ring-2 focus:ring-blue-500"
                />
                <div class="w-4 h-4 rounded" :style="{ backgroundColor: item.color || getItemColor(item.name) }"></div>
                <span class="font-medium cursor-pointer" @click="selectCategory(item)">{{ item.name }}</span>
              </div>
              <div class="text-right">
                <div class="font-semibold">{{ currencySymbol }}{{ formatNumber(convertValue(getItemValue(item))) }}</div>
                <div class="text-sm text-gray-500">
                  {{ formatNumber(getAdjustedPercentage(item)) }}%
                </div>
              </div>
            </div>
          </div>

          <!-- 选中类别时显示钻取饼图 -->
          <div v-else class="space-y-4">
            <div class="flex items-center justify-between">
              <h4 class="font-semibold">{{ selectedCategory.name }} - 账户分布</h4>
              <button
                @click="selectedCategory = null; drillDownData = null"
                class="text-sm text-gray-500 hover:text-gray-700"
              >
                返回
              </button>
            </div>

            <div class="space-y-3">
              <div class="h-64 flex items-center justify-center">
                <div v-if="loadingDrillDown" class="text-gray-500">加载中...</div>
                <div v-else-if="!drillDownChartData || drillDownData.length === 0" class="text-gray-500">暂无账户数据</div>
                <Pie v-else :data="drillDownChartData" :options="drillDownPieChartOptions" />
              </div>
              <!-- 在饼图下方显示类型总计 -->
              <div v-if="drillDownData && drillDownData.length > 0" class="text-center p-3 bg-gray-50 rounded-lg">
                <div class="text-xs text-gray-500">{{ selectedCategory?.name || '该类型' }}总计</div>
                <div class="text-xl font-bold text-gray-900">
                  {{ currencySymbol }}{{ formatNumber(convertValue(drillDownData.reduce((sum, item) => sum + item.balance, 0))) }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Pie } from 'vue-chartjs'
import {
  Chart as ChartJS,
  ArcElement,
  Tooltip,
  Legend
} from 'chart.js'
import ChartDataLabels from 'chartjs-plugin-datalabels'
import { analysisAPI } from '@/api/analysis'
import { assetAccountAPI } from '@/api/asset'
import { liabilityAccountAPI } from '@/api/liability'

// 注册 Chart.js 组件
ChartJS.register(ArcElement, Tooltip, Legend, ChartDataLabels)

// Tab 定义
const tabs = [
  { key: 'net', label: '净资产' },
  { key: 'asset', label: '总资产' },
  { key: 'liability', label: '总负债' }
]

const activeTab = ref('net')
const loading = ref(false)
const loadingDrillDown = ref(false)
const selectedCategory = ref(null)
const drillDownData = ref(null)

// 排除分类功能
const excludedCategories = ref(new Set())

// 货币选择
const selectedCurrency = ref('USD')
const exchangeRate = ref(7.2) // USD to CNY 默认汇率

// 日期选择
const selectedDate = ref('')
const actualDataDate = ref(null)  // 实际数据日期

const summary = ref({
  totalAssets: 0,
  totalLiabilities: 0,
  netWorth: 0,
  actualDate: null
})

const netAllocation = ref({
  total: 0,
  data: []
})

const assetAllocation = ref({
  total: 0,
  data: []
})

const liabilityAllocation = ref({
  total: 0,
  data: []
})

const taxStatusAllocation = ref({
  total: 0,
  data: []
})

const loadingTaxStatus = ref(false)

const memberAllocation = ref({
  total: 0,
  data: []
})

const loadingMember = ref(false)

const currencyAllocation = ref({
  total: 0,
  data: []
})

const loadingCurrency = ref(false)

// 颜色配置
const defaultColors = [
  'rgb(34, 197, 94)',   // green
  'rgb(59, 130, 246)',  // blue
  'rgb(251, 146, 60)',  // orange
  'rgb(168, 85, 247)',  // purple
  'rgb(236, 72, 153)',  // pink
  'rgb(234, 179, 8)',   // yellow
  'rgb(20, 184, 166)',  // teal
  'rgb(239, 68, 68)',   // red
  'rgb(156, 163, 175)', // gray
  'rgb(99, 102, 241)',  // indigo
]

const getItemColor = (name) => {
  const data = currentAllocation.value?.data || []
  const index = data.findIndex(item => item.name === name)
  return defaultColors[index % defaultColors.length]
}

const getItemValue = (item) => {
  if (activeTab.value === 'net') {
    return item.netValue || 0
  }
  return item.value || 0
}

// 货币符号
const currencySymbol = computed(() => {
  return selectedCurrency.value === 'CNY' ? '¥' : '$'
})

// 转换金额
const convertValue = (valueInUSD) => {
  if (selectedCurrency.value === 'CNY') {
    return valueInUSD * exchangeRate.value
  }
  return valueInUSD
}

// 转换后的汇总数据
const convertedSummary = computed(() => {
  return {
    totalAssets: convertValue(summary.value.totalAssets),
    totalLiabilities: convertValue(summary.value.totalLiabilities),
    netWorth: convertValue(summary.value.netWorth)
  }
})

// 货币切换处理
const onCurrencyChange = () => {
  // 货币切换时不需要重新加载数据，只需要重新计算显示值
  // 数据转换通过 convertValue 函数和 computed 属性自动处理
}

// 当前激活的配置数据
const currentAllocation = computed(() => {
  switch (activeTab.value) {
    case 'net':
      return netAllocation.value
    case 'asset':
      return assetAllocation.value
    case 'liability':
      return liabilityAllocation.value
    default:
      return { total: 0, data: [] }
  }
})

// 过滤后的分类（排除被勾选的）
const filteredCategories = computed(() => {
  const allCategories = currentAllocation.value?.data || []
  return allCategories
    .filter(item => !excludedCategories.value.has(getItemKey(item)))
    .map(item => {
      const value = getItemValue(item)
      const adjustedPercentage = filteredTotal.value > 0
        ? ((value / filteredTotal.value) * 100).toFixed(2)
        : '0.00'
      return {
        ...item,
        adjustedPercentage
      }
    })
})

// 过滤后的总值
const filteredTotal = computed(() => {
  const allCategories = currentAllocation.value?.data || []
  return allCategories
    .filter(item => !excludedCategories.value.has(getItemKey(item)))
    .reduce((sum, item) => sum + getItemValue(item), 0)
})

// 主饼图数据 - 根据排除的分类进行过滤
const currentChartData = computed(() => {
  const allData = currentAllocation.value?.data || []
  // 过滤掉被排除的分类
  const filteredData = allData.filter(item => !excludedCategories.value.has(getItemKey(item)))

  return {
    labels: filteredData.map(item => item.name),
    datasets: [
      {
        data: filteredData.map(item => getItemValue(item)),
        backgroundColor: filteredData.map((item, index) => {
          // 使用原始数据中的颜色，如果没有则用默认颜色
          if (item.color) return item.color
          const originalIndex = allData.findIndex(d => getItemKey(d) === getItemKey(item))
          return defaultColors[originalIndex % defaultColors.length]
        }),
        borderWidth: 2,
        borderColor: '#fff'
      }
    ]
  }
})

// 钻取饼图数据
const drillDownChartData = computed(() => {
  if (!drillDownData.value || drillDownData.value.length === 0) {
    return null
  }

  return {
    labels: drillDownData.value.map(item => item.accountName),
    datasets: [
      {
        data: drillDownData.value.map(item => item.balance),
        backgroundColor: drillDownData.value.map((_, index) =>
          defaultColors[index % defaultColors.length]
        ),
        borderWidth: 2,
        borderColor: '#fff'
      }
    ]
  }
})

// 主饼图配置 - 带点击事件和数据标签
const mainPieChartOptions = computed(() => {
  return {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: false
      },
      tooltip: {
        callbacks: {
          label: function(context) {
            const label = context.label || ''
            const valueInUSD = context.parsed || 0
            const value = convertValue(valueInUSD)
            const total = context.dataset.data.reduce((a, b) => a + b, 0)
            const percentage = ((valueInUSD / total) * 100).toFixed(2)
            const symbol = selectedCurrency.value === 'CNY' ? '¥' : '$'
            return `${label}: ${symbol}${value.toLocaleString('en-US', { minimumFractionDigits: 2 })} (${percentage}%)`
          }
        }
      },
      datalabels: {
        color: '#fff',
        font: {
          weight: 'bold',
          size: 12
        },
        formatter: (value, context) => {
          const total = context.dataset.data.reduce((a, b) => a + b, 0)
          const percentage = ((value / total) * 100).toFixed(1)
          const convertedValue = convertValue(value)
          const symbol = selectedCurrency.value === 'CNY' ? '¥' : '$'

          // 只显示百分比大于5%的标签,避免拥挤
          if (percentage < 5) return ''

          // 格式化数值,简化显示
          const formattedValue = convertedValue >= 10000
            ? (convertedValue / 10000).toFixed(1) + (selectedCurrency.value === 'CNY' ? '万' : 'K')
            : convertedValue.toFixed(0)

          return `${symbol}${formattedValue}\n${percentage}%`
        },
        textAlign: 'center',
        anchor: 'center',
        align: 'center'
      }
    },
    onClick: (event, elements) => {
      if (elements.length > 0) {
        const index = elements[0].index
        // 需要从过滤后的数据找到对应的原始item
        const filteredData = currentAllocation.value.data.filter(item => !excludedCategories.value.has(getItemKey(item)))
        const item = filteredData[index]
        if (item) {
          selectCategory(item)
        }
      }
    }
  }
})

// 钻取饼图配置
const drillDownPieChartOptions = computed(() => {
  return {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'bottom',
        labels: {
          boxWidth: 12,
          padding: 10,
          font: {
            size: 11
          }
        }
      },
      tooltip: {
        callbacks: {
          label: function(context) {
            const label = context.label || ''
            const valueInUSD = context.parsed || 0
            const value = convertValue(valueInUSD)
            const total = context.dataset.data.reduce((a, b) => a + b, 0)
            const percentage = ((valueInUSD / total) * 100).toFixed(2)
            const symbol = selectedCurrency.value === 'CNY' ? '¥' : '$'
            return `${label}: ${symbol}${value.toLocaleString('en-US', { minimumFractionDigits: 2 })} (${percentage}%)`
          },
          footer: function(tooltipItems) {
            const totalInUSD = tooltipItems[0].dataset.data.reduce((a, b) => a + b, 0)
            const total = convertValue(totalInUSD)
            const symbol = selectedCurrency.value === 'CNY' ? '¥' : '$'
            return `${selectedCategory.value?.name || '该类型'}总计: ${symbol}${total.toLocaleString('en-US', { minimumFractionDigits: 2 })}`
          }
        }
      },
      datalabels: {
        color: '#fff',
        font: {
          weight: 'bold',
          size: 10
        },
        formatter: (value, context) => {
          const total = context.dataset.data.reduce((a, b) => a + b, 0)
          const percentage = ((value / total) * 100).toFixed(1)

          // 只显示百分比大于5%的标签
          if (percentage < 5) return ''

          return `${percentage}%`
        },
        anchor: 'center',
        align: 'center'
      }
    }
  }
})

// 税收状态饼图数据
const taxStatusChartData = computed(() => {
  if (!taxStatusAllocation.value || !taxStatusAllocation.value.data) {
    return null
  }

  // 税收状态颜色映射
  const taxStatusColors = {
    '应税': 'rgb(251, 146, 60)',    // orange
    '免税': 'rgb(34, 197, 94)',     // green
    '延税': 'rgb(59, 130, 246)'     // blue
  }

  return {
    labels: taxStatusAllocation.value.data.map(item => item.name),
    datasets: [
      {
        data: taxStatusAllocation.value.data.map(item => item.value),
        backgroundColor: taxStatusAllocation.value.data.map(item => taxStatusColors[item.name] || defaultColors[0]),
        borderWidth: 2,
        borderColor: '#fff'
      }
    ]
  }
})

// 税收状态饼图配置
const taxStatusPieChartOptions = computed(() => {
  return {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'bottom',
        labels: {
          boxWidth: 12,
          padding: 10,
          font: {
            size: 11
          }
        }
      },
      tooltip: {
        callbacks: {
          label: function(context) {
            const label = context.label || ''
            const valueInUSD = context.parsed || 0
            const value = convertValue(valueInUSD)
            const total = context.dataset.data.reduce((a, b) => a + b, 0)
            const percentage = ((valueInUSD / total) * 100).toFixed(2)
            const symbol = selectedCurrency.value === 'CNY' ? '¥' : '$'
            return `${label}: ${symbol}${value.toLocaleString('en-US', { minimumFractionDigits: 2 })} (${percentage}%)`
          }
        }
      },
      datalabels: {
        color: '#fff',
        font: {
          weight: 'bold',
          size: 11
        },
        formatter: (value, context) => {
          const total = context.dataset.data.reduce((a, b) => a + b, 0)
          const percentage = ((value / total) * 100).toFixed(1)
          const convertedValue = convertValue(value)
          const symbol = selectedCurrency.value === 'CNY' ? '¥' : '$'

          // 只显示百分比大于3%的标签
          if (percentage < 3) return ''

          // 格式化数值
          const formattedValue = convertedValue >= 10000
            ? (convertedValue / 10000).toFixed(1) + (selectedCurrency.value === 'CNY' ? '万' : 'K')
            : convertedValue.toFixed(0)

          return `${symbol}${formattedValue}\n${percentage}%`
        },
        textAlign: 'center',
        anchor: 'center',
        align: 'center'
      }
    }
  }
})

// 家庭成员饼图数据
const memberChartData = computed(() => {
  if (!memberAllocation.value || !memberAllocation.value.data) {
    return null
  }

  return {
    labels: memberAllocation.value.data.map(item => item.displayName || item.userName),
    datasets: [
      {
        data: memberAllocation.value.data.map(item => item.value),
        backgroundColor: memberAllocation.value.data.map((item, index) => defaultColors[index % defaultColors.length]),
        borderWidth: 2,
        borderColor: '#fff'
      }
    ]
  }
})

// 家庭成员饼图配置
const memberPieChartOptions = computed(() => {
  return {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'bottom',
        labels: {
          boxWidth: 12,
          padding: 10,
          font: {
            size: 11
          }
        }
      },
      tooltip: {
        callbacks: {
          label: function(context) {
            const label = context.label || ''
            const valueInUSD = context.parsed || 0
            const value = convertValue(valueInUSD)
            const total = context.dataset.data.reduce((a, b) => a + b, 0)
            const percentage = ((valueInUSD / total) * 100).toFixed(2)
            const symbol = selectedCurrency.value === 'CNY' ? '¥' : '$'
            return `${label}: ${symbol}${value.toLocaleString('en-US', { minimumFractionDigits: 2 })} (${percentage}%)`
          }
        }
      },
      datalabels: {
        color: '#fff',
        font: {
          weight: 'bold',
          size: 11
        },
        formatter: (value, context) => {
          const total = context.dataset.data.reduce((a, b) => a + b, 0)
          const percentage = ((value / total) * 100).toFixed(1)
          const convertedValue = convertValue(value)
          const symbol = selectedCurrency.value === 'CNY' ? '¥' : '$'

          // 只显示百分比大于3%的标签
          if (percentage < 3) return ''

          // 格式化数值
          const formattedValue = convertedValue >= 10000
            ? (convertedValue / 10000).toFixed(1) + (selectedCurrency.value === 'CNY' ? '万' : 'K')
            : convertedValue.toFixed(0)

          return `${symbol}${formattedValue}\n${percentage}%`
        },
        textAlign: 'center',
        anchor: 'center',
        align: 'center'
      }
    }
  }
})

// 货币分布饼图数据
const currencyChartData = computed(() => {
  if (!currencyAllocation.value || !currencyAllocation.value.data) {
    return null
  }

  return {
    labels: currencyAllocation.value.data.map(item => item.name),
    datasets: [
      {
        // 使用基础货币金额来计算饼图扇形大小，确保与百分比一致
        data: currencyAllocation.value.data.map(item => item.valueInBaseCurrency || item.value),
        backgroundColor: currencyAllocation.value.data.map(item => getCurrencyColor(item.currency)),
        borderWidth: 2,
        borderColor: '#fff'
      }
    ]
  }
})

// 货币分布饼图配置
const currencyPieChartOptions = computed(() => {
  return {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'bottom',
        labels: {
          boxWidth: 12,
          padding: 10,
          font: {
            size: 11
          }
        }
      },
      tooltip: {
        callbacks: {
          label: function(context) {
            const label = context.label || ''
            const value = context.parsed || 0
            // Use pre-calculated percentage from API instead of recalculating
            const item = currencyAllocation.value.data[context.dataIndex]
            const percentage = item.percentage.toFixed(2)
            const symbol = getCurrencySymbol(item.currency)
            return `${label}: ${symbol}${value.toLocaleString('en-US', { minimumFractionDigits: 2 })} (${percentage}%)`
          }
        }
      },
      datalabels: {
        color: '#fff',
        font: {
          weight: 'bold',
          size: 11
        },
        formatter: (value, context) => {
          // Use pre-calculated percentage from API instead of recalculating
          const item = currencyAllocation.value.data[context.dataIndex]
          const percentage = item.percentage.toFixed(1)
          const symbol = getCurrencySymbol(item.currency)

          // 只显示百分比大于3%的标签
          if (parseFloat(percentage) < 3) return ''

          // Format original currency value
          const originalValue = item.value
          let formattedOriginal
          if (item.currency === 'CNY' && originalValue >= 10000) {
            formattedOriginal = (originalValue / 10000).toFixed(1) + '万'
          } else if (item.currency !== 'CNY' && originalValue >= 1000) {
            formattedOriginal = (originalValue / 1000).toFixed(1) + 'K'
          } else {
            formattedOriginal = originalValue.toFixed(0)
          }

          // Format base currency value (USD)
          const baseValue = item.valueInBaseCurrency
          const formattedBase = baseValue >= 10000
            ? '$' + (baseValue / 1000).toFixed(1) + 'K'
            : '$' + baseValue.toFixed(0)

          // Show both currencies if they're different
          if (item.currency === 'USD') {
            return `${symbol}${formattedOriginal}\n${percentage}%`
          } else {
            return `${symbol}${formattedOriginal}\n(${formattedBase})\n${percentage}%`
          }
        },
        textAlign: 'center',
        anchor: 'center',
        align: 'center'
      }
    }
  }
})

// 格式化数字
const formatNumber = (num) => {
  if (!num) return '0.00'
  return parseFloat(num).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '-'
  // 如果是 YYYY-MM-DD 格式，转换为 YYYY年MM月DD日
  if (typeof dateString === 'string' && dateString.match(/^\d{4}-\d{2}-\d{2}$/)) {
    const [year, month, day] = dateString.split('-')
    return `${year}年${month}月${day}日`
  }
  // 兼容其他格式
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
}

// 加载资产总览
const loadSummary = async () => {
  try {
    const response = await analysisAPI.getSummary(null, selectedDate.value || null)
    if (response.success) {
      summary.value = response.data
      // 设置实际数据日期
      if (response.data.actualDate) {
        actualDataDate.value = response.data.actualDate
      }
    }
  } catch (error) {
    console.error('加载资产总览失败:', error)
  }
}

// 加载净资产配置
const loadNetAllocation = async () => {
  loading.value = true
  try {
    const response = await analysisAPI.getNetAssetAllocation(null, selectedDate.value || null)
    if (response.success) {
      netAllocation.value = response.data
    }
  } catch (error) {
    console.error('加载净资产配置失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载总资产配置
const loadAssetAllocation = async () => {
  loading.value = true
  try {
    const response = await analysisAPI.getAllocationByType(null, selectedDate.value || null)
    if (response.success) {
      assetAllocation.value = response.data
    }
  } catch (error) {
    console.error('加载资产配置失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载总负债配置
const loadLiabilityAllocation = async () => {
  loading.value = true
  try {
    const response = await analysisAPI.getLiabilityAllocation(null, selectedDate.value || null)
    if (response.success) {
      liabilityAllocation.value = response.data
    }
  } catch (error) {
    console.error('加载负债配置失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载税收状态配置
const loadTaxStatusAllocation = async () => {
  loadingTaxStatus.value = true
  try {
    const response = await analysisAPI.getNetWorthByTaxStatus(null, selectedDate.value || null)
    if (response.success) {
      taxStatusAllocation.value = response.data
    }
  } catch (error) {
    console.error('加载税收状态配置失败:', error)
  } finally {
    loadingTaxStatus.value = false
  }
}

// 加载家庭成员配置
const loadMemberAllocation = async () => {
  loadingMember.value = true
  try {
    const response = await analysisAPI.getNetWorthByMember(null, selectedDate.value || null)
    if (response.success) {
      memberAllocation.value = response.data
    }
  } catch (error) {
    console.error('加载家庭成员配置失败:', error)
  } finally {
    loadingMember.value = false
  }
}

// 日期变化处理
const onDateChange = () => {
  // 重新加载所有数据
  loadSummary()
  // 重新加载当前激活的 tab 数据
  if (activeTab.value === 'net') {
    loadNetAllocation()
    loadTaxStatusAllocation()
    loadMemberAllocation()
    loadCurrencyAllocation()
  } else if (activeTab.value === 'asset') {
    loadAssetAllocation()
  } else if (activeTab.value === 'liability') {
    loadLiabilityAllocation()
  }
}

// 清除日期
const clearDate = () => {
  selectedDate.value = ''
  onDateChange()
}

// 切换 Tab
const switchTab = (tabKey) => {
  activeTab.value = tabKey
  selectedCategory.value = null
  drillDownData.value = null
  excludedCategories.value.clear() // 清空排除的分类

  // 根据 tab 加载对应数据
  if (tabKey === 'net') {
    if (netAllocation.value.data.length === 0) {
      loadNetAllocation()
    }
    if (taxStatusAllocation.value.data.length === 0) {
      loadTaxStatusAllocation()
    }
    if (memberAllocation.value.data.length === 0) {
      loadMemberAllocation()
    }
    if (currencyAllocation.value.data.length === 0) {
      loadCurrencyAllocation()
    }
  } else if (tabKey === 'asset' && assetAllocation.value.data.length === 0) {
    loadAssetAllocation()
  } else if (tabKey === 'liability' && liabilityAllocation.value.data.length === 0) {
    loadLiabilityAllocation()
  }
}

// 选择类别，加载账户分布
const selectCategory = async (item) => {
  selectedCategory.value = item
  loadingDrillDown.value = true

  try {
    if (activeTab.value === 'net') {
      // 对于净资产，获取该净资产类别下的所有资产账户和负债账户
      const response = await analysisAPI.getNetAssetCategoryAccounts(
        item.code,
        null,
        selectedDate.value || null
      )
      if (response.success) {
        drillDownData.value = response.data
      }
    } else if (activeTab.value === 'asset') {
      // 资产类型名称映射（中文 -> 英文）
      const typeMap = {
        '现金类': 'CASH',
        '股票投资': 'STOCKS',
        '退休基金': 'RETIREMENT_FUND',
        '保险': 'INSURANCE',
        '房地产': 'REAL_ESTATE',
        '数字货币': 'CRYPTOCURRENCY',
        '贵金属': 'PRECIOUS_METALS',
        '其他': 'OTHER'
      }

      const categoryType = typeMap[item.name]
      if (categoryType) {
        const response = await analysisAPI.getAssetAccountsWithBalances(
          categoryType,
          null,
          selectedDate.value || null
        )
        if (response.success) {
          drillDownData.value = response.data
        }
      }
    } else if (activeTab.value === 'liability') {
      // 负债类型名称映射（中文 -> 英文）
      const typeMap = {
        '房贷': 'MORTGAGE',
        '车贷': 'AUTO_LOAN',
        '信用卡': 'CREDIT_CARD',
        '个人借债': 'PERSONAL_LOAN',
        '学生贷款': 'STUDENT_LOAN',
        '商业贷款': 'BUSINESS_LOAN',
        '其他': 'OTHER'
      }

      const categoryType = typeMap[item.name]
      if (categoryType) {
        const response = await analysisAPI.getLiabilityAccountsWithBalances(
          categoryType,
          null,
          selectedDate.value || null
        )
        if (response.success) {
          drillDownData.value = response.data
        }
      }
    }
  } catch (error) {
    console.error('加载账户分布失败:', error)
    drillDownData.value = []
  } finally {
    loadingDrillDown.value = false
  }
}

// 获取分类的唯一标识
const getItemKey = (item) => {
  return item.code || item.name
}

// 检查分类是否被排除
const isExcluded = (item) => {
  return excludedCategories.value.has(getItemKey(item))
}

// 切换分类的包含/排除状态
const toggleCategory = (item) => {
  const key = getItemKey(item)
  if (excludedCategories.value.has(key)) {
    excludedCategories.value.delete(key)
  } else {
    excludedCategories.value.add(key)
  }
  // 触发响应式更新
  excludedCategories.value = new Set(excludedCategories.value)
}

// 获取调整后的百分比(考虑排除的分类)
const getAdjustedPercentage = (item) => {
  const value = getItemValue(item)
  const total = filteredTotal.value
  if (total === 0) return '0.00'
  return ((value / total) * 100).toFixed(2)
}

// 获取税收状态颜色
const getTaxStatusColor = (taxStatus) => {
  const taxStatusColors = {
    'TAXABLE': 'rgb(251, 146, 60)',    // orange
    'TAX_FREE': 'rgb(34, 197, 94)',     // green
    'TAX_DEFERRED': 'rgb(59, 130, 246)' // blue
  }
  return taxStatusColors[taxStatus] || defaultColors[0]
}

// 获取货币颜色
const getCurrencyColor = (currency) => {
  const currencyColors = {
    'CNY': 'rgb(239, 68, 68)',      // red - 人民币
    'USD': 'rgb(34, 197, 94)',      // green - 美元
    'EUR': 'rgb(59, 130, 246)',     // blue - 欧元
    'GBP': 'rgb(168, 85, 247)',     // purple - 英镑
    'JPY': 'rgb(251, 146, 60)',     // orange - 日元
    'HKD': 'rgb(236, 72, 153)',     // pink - 港币
    'AUD': 'rgb(20, 184, 166)',     // teal - 澳元
    'CAD': 'rgb(234, 179, 8)'       // yellow - 加元
  }
  return currencyColors[currency] || defaultColors[0]
}

// 获取货币符号
const getCurrencySymbol = (currency) => {
  const currencySymbols = {
    'CNY': '¥',
    'USD': '$',
    'EUR': '€',
    'GBP': '£',
    'JPY': '円',
    'HKD': 'HK$',
    'AUD': 'A$',
    'CAD': 'C$'
  }
  return currencySymbols[currency] || currency
}

// 加载货币分布配置
const loadCurrencyAllocation = async () => {
  loadingCurrency.value = true
  try {
    const response = await analysisAPI.getNetWorthByCurrency(null, selectedDate.value || null)
    if (response.success) {
      currencyAllocation.value = response.data
    }
  } catch (error) {
    console.error('加载货币分布配置失败:', error)
  } finally {
    loadingCurrency.value = false
  }
}

onMounted(() => {
  loadSummary()
  loadNetAllocation()  // 默认加载净资产配置
  loadTaxStatusAllocation()  // 默认加载税收状态配置
  loadMemberAllocation()  // 默认加载家庭成员配置
  loadCurrencyAllocation()  // 默认加载货币分布配置
})
</script>
