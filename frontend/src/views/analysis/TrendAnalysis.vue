<template>
  <div class="space-y-6 p-6">
    <!-- 页面标题和控制栏 -->
    <div class="bg-white rounded-lg shadow p-4">
      <div class="flex items-center justify-between mb-4">
        <h1 class="text-2xl font-bold text-gray-900">趋势分析</h1>
        <div class="flex items-center gap-2">
          <label class="text-sm font-medium text-gray-700">显示货币：</label>
          <select
            v-model="selectedCurrency"
            class="px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary text-sm"
          >
            <option value="USD">美元 (USD)</option>
            <option value="CNY">人民币 (CNY)</option>
          </select>
        </div>
      </div>

      <!-- 日期范围选择 -->
      <div class="flex items-center gap-3">
        <label class="text-sm font-medium text-gray-700">时间范围：</label>
        <div class="flex gap-2">
          <button
            v-for="range in timeRanges"
            :key="range.value"
            @click="selectTimeRange(range.value)"
            :class="[
              'px-3 py-1 text-sm rounded-md font-medium transition-colors',
              selectedTimeRange === range.value
                ? 'bg-primary text-white'
                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            ]"
          >
            {{ range.label }}
          </button>
        </div>
        <span class="text-gray-500 mx-2">或</span>
        <input
          v-model="customStartDate"
          type="date"
          class="px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary"
        />
        <span class="text-gray-500">至</span>
        <input
          v-model="customEndDate"
          type="date"
          class="px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary"
        />
        <button
          @click="loadAllTrends"
          class="px-4 py-2 bg-primary text-white rounded-md hover:bg-primary/90 text-sm font-medium"
        >
          查询
        </button>
      </div>
    </div>

    <!-- Tab 切换 -->
    <div class="border-b border-gray-200">
      <nav class="-mb-px flex space-x-4" aria-label="Tabs">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          @click="activeTab = tab.key"
          :class="[
            'whitespace-nowrap py-3 px-4 border-b-2 font-medium text-sm transition-colors',
            activeTab === tab.key
              ? 'border-primary text-primary'
              : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
          ]"
        >
          {{ tab.label }}
        </button>
      </nav>
    </div>

    <!-- 综合趋势（净资产、总资产、总负债） -->
    <div v-if="activeTab === 'overall'" class="bg-white rounded-lg shadow p-6">
      <div v-if="loading" class="flex justify-center items-center h-96">
        <div class="text-gray-500">加载中...</div>
      </div>
      <div v-else-if="overallTrendData.length === 0" class="flex justify-center items-center h-96">
        <div class="text-gray-500">暂无数据，请先添加资产或负债记录</div>
      </div>
      <div v-else class="grid grid-cols-1 lg:grid-cols-5 gap-6">
        <!-- 左侧图表 (3/5) -->
        <div class="lg:col-span-3 h-96">
          <Line :data="overallChartData" :options="overallChartOptions" />
        </div>

        <!-- 右侧统计表格 (2/5) -->
        <div class="lg:col-span-2">
          <h3 class="text-md font-semibold mb-3 text-gray-900">综合趋势</h3>
          <div class="border border-gray-200 rounded-lg overflow-hidden">
            <table class="min-w-full divide-y divide-gray-200">
              <thead class="bg-gray-50">
                <tr>
                  <th class="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">指标</th>
                  <th class="px-3 py-2 text-right text-xs font-medium text-gray-500 uppercase">最早</th>
                  <th class="px-3 py-2 text-right text-xs font-medium text-gray-500 uppercase">最新</th>
                  <th class="px-3 py-2 text-right text-xs font-medium text-gray-500 uppercase">变化</th>
                </tr>
              </thead>
              <tbody class="bg-white divide-y divide-gray-200">
                <tr v-for="stat in overallStats" :key="stat.name">
                  <td class="px-3 py-2 text-sm font-medium text-gray-900">{{ stat.name }}</td>
                  <td class="px-3 py-2 text-sm text-gray-600 text-right" :title="stat.earliestDate">
                    {{ currencySymbol }}{{ formatNumber(stat.earliestValue) }}
                  </td>
                  <td class="px-3 py-2 text-sm text-gray-900 text-right font-medium" :title="stat.latestDate">
                    {{ currencySymbol }}{{ formatNumber(stat.latestValue) }}
                  </td>
                  <td class="px-3 py-2 text-sm text-right">
                    <div :class="getChangeColorClass(stat.change)">
                      <div class="font-medium">{{ stat.change > 0 ? '+' : '' }}{{ formatNumber(stat.change) }}%</div>
                      <div class="text-xs">{{ stat.absoluteChange > 0 ? '+' : '' }}{{ currencySymbol }}{{ formatNumber(Math.abs(stat.absoluteChange)) }}</div>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <!-- 资产分类趋势（所有分类在一个图表中） -->
    <div v-if="activeTab === 'asset'" class="bg-white rounded-lg shadow p-6">
      <div v-if="loadingAssetCategories" class="flex justify-center items-center h-96">
        <div class="text-gray-500">加载中...</div>
      </div>
      <div v-else-if="!hasAssetCategoryData" class="flex justify-center items-center h-96">
        <div class="text-gray-500">暂无数据</div>
      </div>
      <div v-else class="grid grid-cols-1 lg:grid-cols-5 gap-6">
        <!-- 左侧图表 (3/5) -->
        <div class="lg:col-span-3 h-96">
          <Line :data="assetCategoriesChartData" :options="categoryChartOptions" />
        </div>

        <!-- 右侧统计表格 (2/5) -->
        <div class="lg:col-span-2">
          <h3 class="text-md font-semibold mb-3 text-gray-900">资产分类趋势</h3>
          <div class="border border-gray-200 rounded-lg overflow-hidden max-h-96 overflow-y-auto">
            <table class="min-w-full divide-y divide-gray-200">
              <thead class="bg-gray-50 sticky top-0">
                <tr>
                  <th class="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">分类</th>
                  <th class="px-3 py-2 text-right text-xs font-medium text-gray-500 uppercase">
                    <div>最早</div>
                    <div class="text-xs text-gray-400 font-normal normal-case">{{ assetGlobalDateRange.earliestDate }}</div>
                  </th>
                  <th class="px-3 py-2 text-right text-xs font-medium text-gray-500 uppercase">
                    <div>最新</div>
                    <div class="text-xs text-gray-400 font-normal normal-case">{{ assetGlobalDateRange.latestDate }}</div>
                  </th>
                  <th class="px-3 py-2 text-right text-xs font-medium text-gray-500 uppercase">变化</th>
                </tr>
              </thead>
              <tbody class="bg-white divide-y divide-gray-200">
                <tr v-for="stat in assetCategoryStats" :key="stat.name"
                    @click="selectAssetCategory(stat)"
                    :class="[
                      'cursor-pointer hover:bg-blue-50 transition-colors',
                      selectedAssetCategory?.name === stat.name ? 'bg-blue-100' : ''
                    ]">
                  <td class="px-3 py-2 text-sm font-medium text-gray-900 whitespace-nowrap">
                    <div class="flex items-center gap-2">
                      <div class="w-3 h-3 rounded-full" :style="{ backgroundColor: stat.color }"></div>
                      {{ stat.name }}
                    </div>
                  </td>
                  <td class="px-3 py-2 text-xs text-gray-600 text-right">
                    {{ currencySymbol }}{{ formatNumber(stat.earliestValue) }}
                  </td>
                  <td class="px-3 py-2 text-xs text-gray-900 text-right font-medium">
                    {{ currencySymbol }}{{ formatNumber(stat.latestValue) }}
                  </td>
                  <td class="px-3 py-2 text-xs text-right">
                    <div :class="getChangeColorClass(stat.change)">
                      <div class="font-medium">{{ stat.change > 0 ? '+' : '' }}{{ formatNumber(stat.change) }}%</div>
                      <div class="text-xs opacity-80">{{ stat.absoluteChange > 0 ? '+' : '' }}{{ currencySymbol }}{{ formatNumber(Math.abs(stat.absoluteChange)) }}</div>
                    </div>
                  </td>
                </tr>
                <!-- 总计行 -->
                <tr class="bg-blue-50 font-semibold">
                  <td class="px-3 py-2 text-sm text-gray-900">总计</td>
                  <td class="px-3 py-2 text-xs text-gray-900 text-right">
                    {{ currencySymbol }}{{ formatNumber(assetCategoryTotal.earliestValue) }}
                  </td>
                  <td class="px-3 py-2 text-xs text-gray-900 text-right font-medium">
                    {{ currencySymbol }}{{ formatNumber(assetCategoryTotal.latestValue) }}
                  </td>
                  <td class="px-3 py-2 text-xs text-right">
                    <div :class="getChangeColorClass(assetCategoryTotal.change)">
                      <div class="font-medium">{{ assetCategoryTotal.change > 0 ? '+' : '' }}{{ formatNumber(assetCategoryTotal.change) }}%</div>
                      <div class="text-xs opacity-80">{{ assetCategoryTotal.absoluteChange > 0 ? '+' : '' }}{{ currencySymbol }}{{ formatNumber(Math.abs(assetCategoryTotal.absoluteChange)) }}</div>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- 资产账号趋势（选中分类后显示） -->
      <div v-if="selectedAssetCategory" class="mt-6 pt-6 border-t border-gray-200">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-lg font-semibold text-gray-900">
            {{ selectedAssetCategory.name }} - 账号趋势
          </h3>
          <button
            @click="selectedAssetCategory = null"
            class="px-3 py-1 text-sm text-gray-600 hover:text-gray-800 hover:bg-gray-100 rounded transition-colors"
          >
            返回
          </button>
        </div>

        <div v-if="loadingAccountsTrend" class="flex justify-center items-center h-64">
          <div class="text-gray-500">加载中...</div>
        </div>
        <div v-else-if="!hasAccountsTrendData" class="flex justify-center items-center h-64">
          <div class="text-gray-500">该分类下暂无账号数据</div>
        </div>
        <div v-else class="grid grid-cols-1 lg:grid-cols-5 gap-6">
          <!-- 左侧图表 (3/5) -->
          <div class="lg:col-span-3 h-96">
            <Line :data="accountsTrendChartData" :options="categoryChartOptions" />
          </div>

          <!-- 右侧统计表格 (2/5) -->
          <div class="lg:col-span-2">
            <h4 class="text-md font-semibold mb-3 text-gray-900">账号明细</h4>
            <div class="border border-gray-200 rounded-lg overflow-hidden max-h-96 overflow-y-auto">
              <table class="min-w-full divide-y divide-gray-200">
                <thead class="bg-gray-50 sticky top-0">
                  <tr>
                    <th class="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">账号</th>
                    <th class="px-3 py-2 text-right text-xs font-medium text-gray-500 uppercase">最早</th>
                    <th class="px-3 py-2 text-right text-xs font-medium text-gray-500 uppercase">最新</th>
                    <th class="px-3 py-2 text-right text-xs font-medium text-gray-500 uppercase">变化</th>
                  </tr>
                </thead>
                <tbody class="bg-white divide-y divide-gray-200">
                  <tr v-for="stat in accountsTrendStats" :key="stat.accountId">
                    <td class="px-3 py-2 text-sm font-medium text-gray-900 whitespace-nowrap">
                      {{ stat.accountName }}
                    </td>
                    <td class="px-3 py-2 text-xs text-gray-600 text-right">
                      {{ currencySymbol }}{{ formatNumber(stat.earliestValue) }}
                    </td>
                    <td class="px-3 py-2 text-xs text-gray-900 text-right font-medium">
                      {{ currencySymbol }}{{ formatNumber(stat.latestValue) }}
                    </td>
                    <td class="px-3 py-2 text-xs text-right">
                      <div :class="getChangeColorClass(stat.change)">
                        <div class="font-medium">{{ stat.change > 0 ? '+' : '' }}{{ formatNumber(stat.change) }}%</div>
                        <div class="text-xs opacity-80">{{ stat.absoluteChange > 0 ? '+' : '' }}{{ currencySymbol }}{{ formatNumber(Math.abs(stat.absoluteChange)) }}</div>
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

    <!-- 负债分类趋势（所有分类在一个图表中） -->
    <div v-if="activeTab === 'liability'" class="bg-white rounded-lg shadow p-6">
      <div v-if="loadingLiabilityCategories" class="flex justify-center items-center h-96">
        <div class="text-gray-500">加载中...</div>
      </div>
      <div v-else-if="!hasLiabilityCategoryData" class="flex justify-center items-center h-96">
        <div class="text-gray-500">暂无数据</div>
      </div>
      <div v-else class="grid grid-cols-1 lg:grid-cols-5 gap-6">
        <!-- 左侧图表 (3/5) -->
        <div class="lg:col-span-3 h-96">
          <Line :data="liabilityCategoriesChartData" :options="categoryChartOptions" />
        </div>

        <!-- 右侧统计表格 (2/5) -->
        <div class="lg:col-span-2">
          <h3 class="text-md font-semibold mb-3 text-gray-900">负债分类趋势</h3>
          <div class="border border-gray-200 rounded-lg overflow-hidden max-h-96 overflow-y-auto">
            <table class="min-w-full divide-y divide-gray-200">
              <thead class="bg-gray-50 sticky top-0">
                <tr>
                  <th class="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">分类</th>
                  <th class="px-3 py-2 text-right text-xs font-medium text-gray-500 uppercase">
                    <div>最早</div>
                    <div class="text-xs text-gray-400 font-normal normal-case">{{ liabilityGlobalDateRange.earliestDate }}</div>
                  </th>
                  <th class="px-3 py-2 text-right text-xs font-medium text-gray-500 uppercase">
                    <div>最新</div>
                    <div class="text-xs text-gray-400 font-normal normal-case">{{ liabilityGlobalDateRange.latestDate }}</div>
                  </th>
                  <th class="px-3 py-2 text-right text-xs font-medium text-gray-500 uppercase">变化</th>
                </tr>
              </thead>
              <tbody class="bg-white divide-y divide-gray-200">
                <tr v-for="stat in liabilityCategoryStats" :key="stat.name"
                    @click="selectLiabilityCategory(stat)"
                    :class="[
                      'cursor-pointer hover:bg-red-50 transition-colors',
                      selectedLiabilityCategory?.name === stat.name ? 'bg-red-100' : ''
                    ]">
                  <td class="px-3 py-2 text-sm font-medium text-gray-900 whitespace-nowrap">
                    <div class="flex items-center gap-2">
                      <div class="w-3 h-3 rounded-full" :style="{ backgroundColor: stat.color }"></div>
                      {{ stat.name }}
                    </div>
                  </td>
                  <td class="px-3 py-2 text-xs text-gray-600 text-right">
                    {{ currencySymbol }}{{ formatNumber(stat.earliestValue) }}
                  </td>
                  <td class="px-3 py-2 text-xs text-gray-900 text-right font-medium">
                    {{ currencySymbol }}{{ formatNumber(stat.latestValue) }}
                  </td>
                  <td class="px-3 py-2 text-xs text-right">
                    <div :class="getChangeColorClass(stat.change)">
                      <div class="font-medium">{{ stat.change > 0 ? '+' : '' }}{{ formatNumber(stat.change) }}%</div>
                      <div class="text-xs opacity-80">{{ stat.absoluteChange > 0 ? '+' : '' }}{{ currencySymbol }}{{ formatNumber(Math.abs(stat.absoluteChange)) }}</div>
                    </div>
                  </td>
                </tr>
                <!-- 总计行 -->
                <tr class="bg-red-50 font-semibold">
                  <td class="px-3 py-2 text-sm text-gray-900">总计</td>
                  <td class="px-3 py-2 text-xs text-gray-900 text-right">
                    {{ currencySymbol }}{{ formatNumber(liabilityCategoryTotal.earliestValue) }}
                  </td>
                  <td class="px-3 py-2 text-xs text-gray-900 text-right font-medium">
                    {{ currencySymbol }}{{ formatNumber(liabilityCategoryTotal.latestValue) }}
                  </td>
                  <td class="px-3 py-2 text-xs text-right">
                    <div :class="getChangeColorClass(liabilityCategoryTotal.change)">
                      <div class="font-medium">{{ liabilityCategoryTotal.change > 0 ? '+' : '' }}{{ formatNumber(liabilityCategoryTotal.change) }}%</div>
                      <div class="text-xs opacity-80">{{ liabilityCategoryTotal.absoluteChange > 0 ? '+' : '' }}{{ currencySymbol }}{{ formatNumber(Math.abs(liabilityCategoryTotal.absoluteChange)) }}</div>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- 负债账号趋势（选中分类后显示） -->
      <div v-if="selectedLiabilityCategory" class="mt-6 pt-6 border-t border-gray-200">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-lg font-semibold text-gray-900">
            {{ selectedLiabilityCategory.name }} - 账号趋势
          </h3>
          <button
            @click="selectedLiabilityCategory = null"
            class="px-3 py-1 text-sm text-gray-600 hover:text-gray-800 hover:bg-gray-100 rounded transition-colors"
          >
            返回
          </button>
        </div>

        <div v-if="loadingLiabilityAccountsTrend" class="flex justify-center items-center h-64">
          <div class="text-gray-500">加载中...</div>
        </div>
        <div v-else-if="!hasLiabilityAccountsTrendData" class="flex justify-center items-center h-64">
          <div class="text-gray-500">该分类下暂无账号数据</div>
        </div>
        <div v-else class="grid grid-cols-1 lg:grid-cols-5 gap-6">
          <!-- 左侧图表 (3/5) -->
          <div class="lg:col-span-3 h-96">
            <Line :data="liabilityAccountsTrendChartData" :options="categoryChartOptions" />
          </div>

          <!-- 右侧统计表格 (2/5) -->
          <div class="lg:col-span-2">
            <h4 class="text-md font-semibold mb-3 text-gray-900">账号明细</h4>
            <div class="border border-gray-200 rounded-lg overflow-hidden max-h-96 overflow-y-auto">
              <table class="min-w-full divide-y divide-gray-200">
                <thead class="bg-gray-50 sticky top-0">
                  <tr>
                    <th class="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">账号</th>
                    <th class="px-3 py-2 text-right text-xs font-medium text-gray-500 uppercase">最早</th>
                    <th class="px-3 py-2 text-right text-xs font-medium text-gray-500 uppercase">最新</th>
                    <th class="px-3 py-2 text-right text-xs font-medium text-gray-500 uppercase">变化</th>
                  </tr>
                </thead>
                <tbody class="bg-white divide-y divide-gray-200">
                  <tr v-for="stat in liabilityAccountsTrendStats" :key="stat.accountId">
                    <td class="px-3 py-2 text-sm font-medium text-gray-900 whitespace-nowrap">
                      {{ stat.accountName }}
                    </td>
                    <td class="px-3 py-2 text-xs text-gray-600 text-right">
                      {{ currencySymbol }}{{ formatNumber(stat.earliestValue) }}
                    </td>
                    <td class="px-3 py-2 text-xs text-gray-900 text-right font-medium">
                      {{ currencySymbol }}{{ formatNumber(stat.latestValue) }}
                    </td>
                    <td class="px-3 py-2 text-xs text-right">
                      <div :class="getChangeColorClass(stat.change)">
                        <div class="font-medium">{{ stat.change > 0 ? '+' : '' }}{{ formatNumber(stat.change) }}%</div>
                        <div class="text-xs opacity-80">{{ stat.absoluteChange > 0 ? '+' : '' }}{{ currencySymbol }}{{ formatNumber(Math.abs(stat.absoluteChange)) }}</div>
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

    <!-- 净资产分类趋势（所有分类在一个图表中） -->
    <div v-if="activeTab === 'netAsset'" class="bg-white rounded-lg shadow p-6">
      <div v-if="loadingNetAssetCategories" class="flex justify-center items-center h-96">
        <div class="text-gray-500">加载中...</div>
      </div>
      <div v-else-if="!hasNetAssetCategoryData" class="flex justify-center items-center h-96">
        <div class="text-gray-500">暂无数据</div>
      </div>
      <div v-else class="grid grid-cols-1 lg:grid-cols-5 gap-6">
        <!-- 左侧图表 (3/5) -->
        <div class="lg:col-span-3 h-96">
          <Line :data="netAssetCategoriesChartData" :options="categoryChartOptions" />
        </div>

        <!-- 右侧统计表格 (2/5) -->
        <div class="lg:col-span-2">
          <h3 class="text-md font-semibold mb-3 text-gray-900">净资产分类趋势</h3>
          <div class="border border-gray-200 rounded-lg overflow-hidden overflow-x-auto">
            <table class="w-full divide-y divide-gray-200">
              <thead class="bg-gray-50 sticky top-0">
                <tr>
                  <th class="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">分类</th>
                  <th class="px-4 py-2 text-right text-xs font-medium text-gray-500 uppercase">
                    <div>最早</div>
                    <div class="text-xs text-gray-400 font-normal normal-case">{{ netAssetGlobalDateRange.earliestDate }}</div>
                  </th>
                  <th class="px-4 py-2 text-right text-xs font-medium text-gray-500 uppercase">
                    <div>最新</div>
                    <div class="text-xs text-gray-400 font-normal normal-case">{{ netAssetGlobalDateRange.latestDate }}</div>
                  </th>
                  <th class="px-4 py-2 text-right text-xs font-medium text-gray-500 uppercase">变化</th>
                </tr>
              </thead>
              <tbody class="bg-white divide-y divide-gray-200">
                <tr v-for="stat in netAssetCategoryStats" :key="stat.name">
                  <td class="px-4 py-2 text-sm font-medium text-gray-900 whitespace-nowrap">
                    <div class="flex items-center gap-2">
                      <div class="w-3 h-3 rounded-full" :style="{ backgroundColor: stat.color }"></div>
                      {{ stat.name }}
                    </div>
                  </td>
                  <td class="px-4 py-2 text-xs text-gray-600 text-right whitespace-nowrap">
                    {{ currencySymbol }}{{ formatNumber(stat.earliestValue) }}
                  </td>
                  <td class="px-4 py-2 text-xs text-gray-900 text-right font-medium whitespace-nowrap">
                    {{ currencySymbol }}{{ formatNumber(stat.latestValue) }}
                  </td>
                  <td class="px-4 py-2 text-xs text-right whitespace-nowrap">
                    <div :class="getChangeColorClass(stat.change)">
                      <div class="font-medium">{{ stat.change > 0 ? '+' : '' }}{{ formatNumber(stat.change) }}%</div>
                      <div class="text-xs opacity-80">{{ stat.absoluteChange > 0 ? '+' : '' }}{{ currencySymbol }}{{ formatNumber(Math.abs(stat.absoluteChange)) }}</div>
                    </div>
                  </td>
                </tr>
                <!-- 总计行 -->
                <tr class="bg-blue-50 font-semibold">
                  <td class="px-4 py-2 text-sm text-gray-900">总计</td>
                  <td class="px-4 py-2 text-xs text-gray-900 text-right whitespace-nowrap">
                    {{ currencySymbol }}{{ formatNumber(netAssetCategoryTotal.earliestValue) }}
                  </td>
                  <td class="px-4 py-2 text-xs text-gray-900 text-right font-medium whitespace-nowrap">
                    {{ currencySymbol }}{{ formatNumber(netAssetCategoryTotal.latestValue) }}
                  </td>
                  <td class="px-4 py-2 text-xs text-right whitespace-nowrap">
                    <div :class="getChangeColorClass(netAssetCategoryTotal.change)">
                      <div class="font-medium">{{ netAssetCategoryTotal.change > 0 ? '+' : '' }}{{ formatNumber(netAssetCategoryTotal.change) }}%</div>
                      <div class="text-xs opacity-80">{{ netAssetCategoryTotal.absoluteChange > 0 ? '+' : '' }}{{ currencySymbol }}{{ formatNumber(Math.abs(netAssetCategoryTotal.absoluteChange)) }}</div>
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
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { Line } from 'vue-chartjs'
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  Filler
} from 'chart.js'
import { analysisAPI } from '@/api/analysis'

// 注册 Chart.js 组件
ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  Filler
)

// Tab 定义
const tabs = [
  { key: 'overall', label: '综合趋势' },
  { key: 'netAsset', label: '净资产分类' },
  { key: 'asset', label: '资产分类' },
  { key: 'liability', label: '负债分类' }
]

const activeTab = ref('overall')
const loading = ref(false)
const loadingAssetCategories = ref(false)
const loadingLiabilityCategories = ref(false)
const loadingNetAssetCategories = ref(false)
const loadingAccountsTrend = ref(false)
const loadingLiabilityAccountsTrend = ref(false)

// 选中的资产分类（用于钻取）
const selectedAssetCategory = ref(null)
// 账号级别趋势数据
const accountsTrendData = ref({})

// 选中的负债分类（用于钻取）
const selectedLiabilityCategory = ref(null)
// 负债账号级别趋势数据
const liabilityAccountsTrendData = ref({})

// 货币选择
const selectedCurrency = ref('USD')
const exchangeRate = ref(7.2) // USD to CNY 默认汇率

// 时间范围
const timeRanges = [
  { value: '3m', label: '近3个月' },
  { value: '6m', label: '近6个月' },
  { value: '1y', label: '近1年' },
  { value: '3y', label: '近3年' },
  { value: 'all', label: '全部' }
]

const selectedTimeRange = ref('1y')
const customStartDate = ref('')
const customEndDate = ref('')

// 计算日期范围
const getDateRange = () => {
  if (customStartDate.value && customEndDate.value) {
    return { start: customStartDate.value, end: customEndDate.value }
  }

  const end = new Date()
  const start = new Date()

  switch (selectedTimeRange.value) {
    case '3m':
      start.setMonth(end.getMonth() - 3)
      break
    case '6m':
      start.setMonth(end.getMonth() - 6)
      break
    case '1y':
      start.setFullYear(end.getFullYear() - 1)
      break
    case '3y':
      start.setFullYear(end.getFullYear() - 3)
      break
    case 'all':
      start.setFullYear(end.getFullYear() - 10)
      break
  }

  return {
    start: start.toISOString().split('T')[0],
    end: end.toISOString().split('T')[0]
  }
}

// 趋势数据
const overallTrendData = ref([])
const assetCategoriesTrendData = ref({}) // { categoryType: [{ date, total }] }
const liabilityCategoriesTrendData = ref({}) // { categoryType: [{ date, total }] }
const netAssetCategoriesTrendData = ref({}) // { categoryType: [{ date, netTotal }] } 资产 - 对应负债

// 分类定义
const assetCategories = [
  { type: 'CASH', name: '现金类', color: 'rgb(34, 197, 94)' },
  { type: 'STOCKS', name: '股票投资', color: 'rgb(59, 130, 246)' },
  { type: 'RETIREMENT_FUND', name: '退休基金', color: 'rgb(168, 85, 247)' },
  { type: 'INSURANCE', name: '保险', color: 'rgb(251, 146, 60)' },
  { type: 'REAL_ESTATE', name: '房地产', color: 'rgb(239, 68, 68)' },
  { type: 'CRYPTOCURRENCY', name: '数字货币', color: 'rgb(234, 179, 8)' },
  { type: 'PRECIOUS_METALS', name: '贵金属', color: 'rgb(20, 184, 166)' },
  { type: 'OTHER', name: '其他', color: 'rgb(156, 163, 175)' }
]

const liabilityCategories = [
  { type: 'MORTGAGE', name: '房贷', color: 'rgb(220, 38, 38)' },
  { type: 'AUTO_LOAN', name: '车贷', color: 'rgb(234, 88, 12)' },
  { type: 'CREDIT_CARD', name: '信用卡', color: 'rgb(251, 146, 60)' },
  { type: 'PERSONAL_LOAN', name: '个人借款', color: 'rgb(249, 115, 22)' },
  { type: 'STUDENT_LOAN', name: '学生贷款', color: 'rgb(251, 191, 36)' },
  { type: 'BUSINESS_LOAN', name: '商业贷款', color: 'rgb(253, 224, 71)' },
  { type: 'OTHER', name: '其他', color: 'rgb(156, 163, 175)' }
]

// 净资产分类（使用数据库中定义的分类）
const netAssetCategories = [
  { code: 'REAL_ESTATE_NET', name: '房地产净值', color: 'rgb(16, 185, 129)' },
  { code: 'LIQUID_NET', name: '流动资产净值', color: 'rgb(59, 130, 246)' },
  { code: 'INVESTMENT_NET', name: '投资净值', color: 'rgb(139, 92, 246)' },
  { code: 'OTHER_NET', name: '其他净值', color: 'rgb(245, 158, 11)' }
]

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

// 格式化数字
const formatNumber = (num) => {
  if (!num && num !== 0) return '0.00'
  return parseFloat(num).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// 获取变化百分比的颜色类
const getChangeColorClass = (change) => {
  if (change > 0) return 'text-green-600 font-medium'
  if (change < 0) return 'text-red-600 font-medium'
  return 'text-gray-600'
}

// 计算资产分类的全局最早和最新日期
const assetGlobalDateRange = computed(() => {
  let earliestDate = null
  let latestDate = null

  Object.values(assetCategoriesTrendData.value).forEach(data => {
    if (data && data.length > 0) {
      const firstDate = data[0].date
      const lastDate = data[data.length - 1].date

      if (!earliestDate || firstDate < earliestDate) {
        earliestDate = firstDate
      }
      if (!latestDate || lastDate > latestDate) {
        latestDate = lastDate
      }
    }
  })

  return { earliestDate, latestDate }
})

// 计算负债分类的全局最早和最新日期
const liabilityGlobalDateRange = computed(() => {
  let earliestDate = null
  let latestDate = null

  Object.values(liabilityCategoriesTrendData.value).forEach(data => {
    if (data && data.length > 0) {
      const firstDate = data[0].date
      const lastDate = data[data.length - 1].date

      if (!earliestDate || firstDate < earliestDate) {
        earliestDate = firstDate
      }
      if (!latestDate || lastDate > latestDate) {
        latestDate = lastDate
      }
    }
  })

  return { earliestDate, latestDate }
})

// 计算综合趋势统计数据
const overallStats = computed(() => {
  if (overallTrendData.value.length === 0) return []

  const earliest = overallTrendData.value[0]
  const latest = overallTrendData.value[overallTrendData.value.length - 1]

  const calculateChange = (earliestVal, latestVal) => {
    if (!earliestVal || earliestVal === 0) return 0
    return ((latestVal - earliestVal) / earliestVal) * 100
  }

  return [
    {
      name: '净资产',
      earliestValue: convertValue(earliest.netWorth || 0),
      earliestDate: earliest.date,
      latestValue: convertValue(latest.netWorth || 0),
      latestDate: latest.date,
      change: calculateChange(earliest.netWorth, latest.netWorth),
      absoluteChange: convertValue(latest.netWorth || 0) - convertValue(earliest.netWorth || 0)
    },
    {
      name: '总资产',
      earliestValue: convertValue(earliest.totalAssets || 0),
      earliestDate: earliest.date,
      latestValue: convertValue(latest.totalAssets || 0),
      latestDate: latest.date,
      change: calculateChange(earliest.totalAssets, latest.totalAssets),
      absoluteChange: convertValue(latest.totalAssets || 0) - convertValue(earliest.totalAssets || 0)
    },
    {
      name: '总负债',
      earliestValue: convertValue(earliest.totalLiabilities || 0),
      earliestDate: earliest.date,
      latestValue: convertValue(latest.totalLiabilities || 0),
      latestDate: latest.date,
      change: calculateChange(earliest.totalLiabilities, latest.totalLiabilities),
      absoluteChange: convertValue(latest.totalLiabilities || 0) - convertValue(earliest.totalLiabilities || 0)
    }
  ]
})

// 计算资产分类统计数据
const assetCategoryStats = computed(() => {
  const stats = []

  assetCategories.forEach(category => {
    const data = assetCategoriesTrendData.value[category.type]
    if (data && data.length > 0) {
      const earliest = data[0]
      const latest = data[data.length - 1]

      const earliestVal = earliest.total || 0
      const latestVal = latest.total || 0
      const change = earliestVal === 0 ? 0 : ((latestVal - earliestVal) / earliestVal) * 100

      stats.push({
        name: category.name,
        color: category.color,
        earliestValue: convertValue(earliestVal),
        earliestDate: earliest.date,
        latestValue: convertValue(latestVal),
        latestDate: latest.date,
        change: change,
        absoluteChange: convertValue(latestVal) - convertValue(earliestVal)
      })
    }
  })

  return stats
})

// 计算负债分类统计数据
const liabilityCategoryStats = computed(() => {
  const stats = []

  liabilityCategories.forEach(category => {
    const data = liabilityCategoriesTrendData.value[category.type]
    if (data && data.length > 0) {
      const earliest = data[0]
      const latest = data[data.length - 1]

      const earliestVal = earliest.total || 0
      const latestVal = latest.total || 0
      const change = earliestVal === 0 ? 0 : ((latestVal - earliestVal) / earliestVal) * 100

      stats.push({
        name: category.name,
        color: category.color,
        earliestValue: convertValue(earliestVal),
        earliestDate: earliest.date,
        latestValue: convertValue(latestVal),
        latestDate: latest.date,
        change: change,
        absoluteChange: convertValue(latestVal) - convertValue(earliestVal)
      })
    }
  })

  return stats
})

// 检查是否有资产分类数据
const hasAssetCategoryData = computed(() => {
  return Object.keys(assetCategoriesTrendData.value).some(
    key => assetCategoriesTrendData.value[key]?.length > 0
  )
})

// 检查是否有负债分类数据
const hasLiabilityCategoryData = computed(() => {
  return Object.keys(liabilityCategoriesTrendData.value).some(
    key => liabilityCategoriesTrendData.value[key]?.length > 0
  )
})

// 计算资产分类总计
const assetCategoryTotal = computed(() => {
  const totalEarliest = assetCategoryStats.value.reduce((sum, stat) => sum + stat.earliestValue, 0)
  const totalLatest = assetCategoryStats.value.reduce((sum, stat) => sum + stat.latestValue, 0)
  const change = totalEarliest === 0 ? 0 : ((totalLatest - totalEarliest) / totalEarliest) * 100

  return {
    earliestValue: totalEarliest,
    latestValue: totalLatest,
    change: change,
    absoluteChange: totalLatest - totalEarliest
  }
})

// 计算负债分类总计
const liabilityCategoryTotal = computed(() => {
  const totalEarliest = liabilityCategoryStats.value.reduce((sum, stat) => sum + stat.earliestValue, 0)
  const totalLatest = liabilityCategoryStats.value.reduce((sum, stat) => sum + stat.latestValue, 0)
  const change = totalEarliest === 0 ? 0 : ((totalLatest - totalEarliest) / totalEarliest) * 100

  return {
    earliestValue: totalEarliest,
    latestValue: totalLatest,
    change: change,
    absoluteChange: totalLatest - totalEarliest
  }
})

// 计算净资产分类的全局最早和最新日期
const netAssetGlobalDateRange = computed(() => {
  let earliestDate = null
  let latestDate = null

  Object.values(netAssetCategoriesTrendData.value).forEach(data => {
    if (data && data.length > 0) {
      const firstDate = data[0].date
      const lastDate = data[data.length - 1].date

      if (!earliestDate || firstDate < earliestDate) {
        earliestDate = firstDate
      }
      if (!latestDate || lastDate > latestDate) {
        latestDate = lastDate
      }
    }
  })

  return { earliestDate, latestDate }
})

// 检查是否有净资产分类数据
const hasNetAssetCategoryData = computed(() => {
  return Object.keys(netAssetCategoriesTrendData.value).some(
    key => netAssetCategoriesTrendData.value[key]?.length > 0
  )
})

// 计算净资产分类统计数据
const netAssetCategoryStats = computed(() => {
  const stats = []

  netAssetCategories.forEach(category => {
    const data = netAssetCategoriesTrendData.value[category.code]
    if (data && data.length > 0) {
      const earliest = data[0]
      const latest = data[data.length - 1]

      const earliestVal = earliest.total || 0
      const latestVal = latest.total || 0
      const change = earliestVal === 0 ? 0 : ((latestVal - earliestVal) / earliestVal) * 100

      stats.push({
        name: category.name,
        color: category.color,
        earliestValue: convertValue(earliestVal),
        earliestDate: earliest.date,
        latestValue: convertValue(latestVal),
        latestDate: latest.date,
        change: change,
        absoluteChange: convertValue(latestVal) - convertValue(earliestVal)
      })
    }
  })

  return stats
})

// 计算净资产分类总计
const netAssetCategoryTotal = computed(() => {
  const totalEarliest = netAssetCategoryStats.value.reduce((sum, stat) => sum + stat.earliestValue, 0)
  const totalLatest = netAssetCategoryStats.value.reduce((sum, stat) => sum + stat.latestValue, 0)
  const change = totalEarliest === 0 ? 0 : ((totalLatest - totalEarliest) / totalEarliest) * 100

  return {
    earliestValue: totalEarliest,
    latestValue: totalLatest,
    change: change,
    absoluteChange: totalLatest - totalEarliest
  }
})

// 检查是否有账号趋势数据
const hasAccountsTrendData = computed(() => {
  return Object.keys(accountsTrendData.value).some(
    key => accountsTrendData.value[key]?.length > 0
  )
})

// 计算账号趋势统计数据
const accountsTrendStats = computed(() => {
  const stats = []

  Object.entries(accountsTrendData.value).forEach(([accountId, data]) => {
    if (data && data.length > 0) {
      const earliest = data[0]
      const latest = data[data.length - 1]

      const earliestVal = earliest.balance || 0
      const latestVal = latest.balance || 0
      const change = earliestVal === 0 ? 0 : ((latestVal - earliestVal) / earliestVal) * 100

      stats.push({
        accountId: parseInt(accountId),
        accountName: earliest.accountName || `账号 ${accountId}`,
        earliestValue: convertValue(earliestVal),
        earliestDate: earliest.date,
        latestValue: convertValue(latestVal),
        latestDate: latest.date,
        change: change,
        absoluteChange: convertValue(latestVal) - convertValue(earliestVal)
      })
    }
  })

  // 按最新金额排序
  return stats.sort((a, b) => b.latestValue - a.latestValue)
})

// 检查是否有负债账号趋势数据
const hasLiabilityAccountsTrendData = computed(() => {
  return Object.keys(liabilityAccountsTrendData.value).some(
    key => liabilityAccountsTrendData.value[key]?.length > 0
  )
})

// 计算负债账号趋势统计数据
const liabilityAccountsTrendStats = computed(() => {
  const stats = []

  Object.entries(liabilityAccountsTrendData.value).forEach(([accountId, data]) => {
    if (data && data.length > 0) {
      const earliest = data[0]
      const latest = data[data.length - 1]

      const earliestVal = earliest.balance || 0
      const latestVal = latest.balance || 0
      const change = earliestVal === 0 ? 0 : ((latestVal - earliestVal) / earliestVal) * 100

      stats.push({
        accountId: parseInt(accountId),
        accountName: earliest.accountName || `账号 ${accountId}`,
        earliestValue: convertValue(earliestVal),
        earliestDate: earliest.date,
        latestValue: convertValue(latestVal),
        latestDate: latest.date,
        change: change,
        absoluteChange: convertValue(latestVal) - convertValue(earliestVal)
      })
    }
  })

  // 按最新金额排序
  return stats.sort((a, b) => b.latestValue - a.latestValue)
})

// 净资产分类图表数据（多条线）
const netAssetCategoriesChartData = computed(() => {
  // 收集所有日期
  const allDates = new Set()
  Object.values(netAssetCategoriesTrendData.value).forEach(data => {
    if (data && Array.isArray(data)) {
      data.forEach(item => allDates.add(item.date))
    }
  })

  const labels = Array.from(allDates).sort()

  // 为每个分类创建一个dataset
  const datasets = netAssetCategories.map(category => {
    const categoryData = netAssetCategoriesTrendData.value[category.code] || []

    // 创建日期到值的映射
    const dataMap = {}
    categoryData.forEach(item => {
      dataMap[item.date] = convertValue(item.total || 0)
    })

    // 按照labels的顺序填充数据
    const data = labels.map(date => dataMap[date] || null)

    return {
      label: category.name,
      data: data,
      borderColor: category.color,
      backgroundColor: category.color.replace('rgb', 'rgba').replace(')', ', 0.1)'),
      tension: 0.3,
      fill: false,
      spanGaps: true // 连接空值
    }
  }).filter(dataset => dataset.data.some(v => v !== null && v > 0)) // 只显示有数据的分类

  return { labels, datasets }
})

// 综合趋势图表数据
const overallChartData = computed(() => {
  const labels = overallTrendData.value.map(item => item.date)

  return {
    labels,
    datasets: [
      {
        label: '净资产',
        data: overallTrendData.value.map(item => convertValue(item.netWorth || 0)),
        borderColor: 'rgb(59, 130, 246)',
        backgroundColor: 'rgba(59, 130, 246, 0.1)',
        tension: 0.3,
        fill: true
      },
      {
        label: '总资产',
        data: overallTrendData.value.map(item => convertValue(item.totalAssets || 0)),
        borderColor: 'rgb(34, 197, 94)',
        backgroundColor: 'rgba(34, 197, 94, 0.1)',
        tension: 0.3,
        fill: false
      },
      {
        label: '总负债',
        data: overallTrendData.value.map(item => convertValue(item.totalLiabilities || 0)),
        borderColor: 'rgb(239, 68, 68)',
        backgroundColor: 'rgba(239, 68, 68, 0.1)',
        tension: 0.3,
        fill: false
      }
    ]
  }
})

// 资产分类图表数据（多条线）
const assetCategoriesChartData = computed(() => {
  // 收集所有日期
  const allDates = new Set()
  Object.values(assetCategoriesTrendData.value).forEach(data => {
    if (data && Array.isArray(data)) {
      data.forEach(item => allDates.add(item.date))
    }
  })

  const labels = Array.from(allDates).sort()

  // 为每个分类创建一个dataset
  const datasets = assetCategories.map(category => {
    const categoryData = assetCategoriesTrendData.value[category.type] || []

    // 创建日期到值的映射
    const dataMap = {}
    categoryData.forEach(item => {
      dataMap[item.date] = convertValue(item.total || 0)
    })

    // 按照labels的顺序填充数据
    const data = labels.map(date => dataMap[date] || null)

    return {
      label: category.name,
      data: data,
      borderColor: category.color,
      backgroundColor: category.color.replace('rgb', 'rgba').replace(')', ', 0.1)'),
      tension: 0.3,
      fill: false,
      spanGaps: true // 连接空值
    }
  }).filter(dataset => dataset.data.some(v => v !== null && v > 0)) // 只显示有数据的分类

  return { labels, datasets }
})

// 账号趋势图表数据（多条线）
const accountsTrendChartData = computed(() => {
  // 收集所有日期
  const allDates = new Set()
  Object.values(accountsTrendData.value).forEach(data => {
    if (data && Array.isArray(data)) {
      data.forEach(item => allDates.add(item.date))
    }
  })

  const labels = Array.from(allDates).sort()

  // 为每个账号创建一个dataset
  const defaultAccountColors = [
    'rgb(59, 130, 246)',   // blue
    'rgb(34, 197, 94)',    // green
    'rgb(251, 146, 60)',   // orange
    'rgb(168, 85, 247)',   // purple
    'rgb(236, 72, 153)',   // pink
    'rgb(234, 179, 8)',    // yellow
    'rgb(20, 184, 166)',   // teal
    'rgb(239, 68, 68)'     // red
  ]

  const datasets = Object.entries(accountsTrendData.value).map(([accountId, data], index) => {
    if (!data || data.length === 0) return null

    const accountName = data[0]?.accountName || `账号 ${accountId}`
    const color = defaultAccountColors[index % defaultAccountColors.length]

    // 创建日期到值的映射
    const dataMap = {}
    data.forEach(item => {
      dataMap[item.date] = convertValue(item.balance || 0)
    })

    // 按照labels的顺序填充数据
    const values = labels.map(date => dataMap[date] || null)

    return {
      label: accountName,
      data: values,
      borderColor: color,
      backgroundColor: color.replace('rgb', 'rgba').replace(')', ', 0.1)'),
      tension: 0.3,
      fill: false,
      spanGaps: true
    }
  }).filter(dataset => dataset !== null && dataset.data.some(v => v !== null && v > 0))

  return { labels, datasets }
})

// 负债账号趋势图表数据（多条线）
const liabilityAccountsTrendChartData = computed(() => {
  // 收集所有日期
  const allDates = new Set()
  Object.values(liabilityAccountsTrendData.value).forEach(data => {
    if (data && Array.isArray(data)) {
      data.forEach(item => allDates.add(item.date))
    }
  })

  const labels = Array.from(allDates).sort()

  // 为每个账号创建一个dataset
  const defaultAccountColors = [
    'rgb(220, 38, 38)',    // red-600
    'rgb(234, 88, 12)',    // orange-600
    'rgb(251, 146, 60)',   // orange-400
    'rgb(249, 115, 22)',   // orange-500
    'rgb(251, 191, 36)',   // amber-400
    'rgb(253, 224, 71)',   // yellow-300
    'rgb(239, 68, 68)',    // red-500
    'rgb(156, 163, 175)'   // gray-400
  ]

  const datasets = Object.entries(liabilityAccountsTrendData.value).map(([accountId, data], index) => {
    if (!data || data.length === 0) return null

    const accountName = data[0]?.accountName || `账号 ${accountId}`
    const color = defaultAccountColors[index % defaultAccountColors.length]

    // 创建日期到值的映射
    const dataMap = {}
    data.forEach(item => {
      dataMap[item.date] = convertValue(item.balance || 0)
    })

    // 按照labels的顺序填充数据
    const values = labels.map(date => dataMap[date] || null)

    return {
      label: accountName,
      data: values,
      borderColor: color,
      backgroundColor: color.replace('rgb', 'rgba').replace(')', ', 0.1)'),
      tension: 0.3,
      fill: false,
      spanGaps: true
    }
  }).filter(dataset => dataset !== null && dataset.data.some(v => v !== null && v > 0))

  return { labels, datasets }
})

// 负债分类图表数据（多条线）
const liabilityCategoriesChartData = computed(() => {
  // 收集所有日期
  const allDates = new Set()
  Object.values(liabilityCategoriesTrendData.value).forEach(data => {
    if (data && Array.isArray(data)) {
      data.forEach(item => allDates.add(item.date))
    }
  })

  const labels = Array.from(allDates).sort()

  // 为每个分类创建一个dataset
  const datasets = liabilityCategories.map(category => {
    const categoryData = liabilityCategoriesTrendData.value[category.type] || []

    // 创建日期到值的映射
    const dataMap = {}
    categoryData.forEach(item => {
      dataMap[item.date] = convertValue(item.total || 0)
    })

    // 按照labels的顺序填充数据
    const data = labels.map(date => dataMap[date] || null)

    return {
      label: category.name,
      data: data,
      borderColor: category.color,
      backgroundColor: category.color.replace('rgb', 'rgba').replace(')', ', 0.1)'),
      tension: 0.3,
      fill: false,
      spanGaps: true // 连接空值
    }
  }).filter(dataset => dataset.data.some(v => v !== null && v > 0)) // 只显示有数据的分类

  return { labels, datasets }
})

// 综合趋势图表配置
const overallChartOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      display: true,
      position: 'top'
    },
    tooltip: {
      mode: 'index',
      intersect: false,
      callbacks: {
        label: function(context) {
          let label = context.dataset.label || ''
          if (label) {
            label += ': '
          }
          if (context.parsed.y !== null) {
            const symbol = selectedCurrency.value === 'CNY' ? '¥' : '$'
            label += symbol + context.parsed.y.toLocaleString('zh-CN', {
              minimumFractionDigits: 2,
              maximumFractionDigits: 2
            })
          }
          return label
        }
      }
    }
  },
  scales: {
    y: {
      beginAtZero: true,
      ticks: {
        callback: function(value) {
          const symbol = selectedCurrency.value === 'CNY' ? '¥' : '$'
          return symbol + value.toLocaleString('zh-CN')
        }
      }
    }
  },
  interaction: {
    mode: 'nearest',
    axis: 'x',
    intersect: false
  }
}))

// 分类趋势图表配置
const categoryChartOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      display: true,
      position: 'top'
    },
    tooltip: {
      mode: 'index',
      intersect: false,
      callbacks: {
        label: function(context) {
          let label = context.dataset.label || ''
          if (label) {
            label += ': '
          }
          if (context.parsed.y !== null) {
            const symbol = selectedCurrency.value === 'CNY' ? '¥' : '$'
            label += symbol + context.parsed.y.toLocaleString('zh-CN', {
              minimumFractionDigits: 2,
              maximumFractionDigits: 2
            })
          }
          return label
        }
      }
    }
  },
  scales: {
    y: {
      beginAtZero: true,
      ticks: {
        callback: function(value) {
          const symbol = selectedCurrency.value === 'CNY' ? '¥' : '$'
          return symbol + value.toLocaleString('zh-CN')
        }
      }
    }
  },
  interaction: {
    mode: 'nearest',
    axis: 'x',
    intersect: false
  }
}))

// 选择时间范围
const selectTimeRange = (range) => {
  selectedTimeRange.value = range
  customStartDate.value = ''
  customEndDate.value = ''
  loadAllTrends()
}

// 加载综合趋势
const loadOverallTrend = async () => {
  loading.value = true
  try {
    const { start, end } = getDateRange()
    const response = await analysisAPI.getOverallTrend(start, end)
    if (response.success) {
      overallTrendData.value = response.data
    }
  } catch (error) {
    console.error('加载综合趋势失败:', error)
    overallTrendData.value = []
  } finally {
    loading.value = false
  }
}

// 加载所有资产分类趋势
const loadAssetCategoriesTrend = async () => {
  loadingAssetCategories.value = true
  try {
    const { start, end } = getDateRange()
    const trendData = {}

    // 并行加载所有分类的数据
    await Promise.all(
      assetCategories.map(async (category) => {
        try {
          const response = await analysisAPI.getAssetCategoryTrend(category.type, start, end)
          if (response.success && response.data && response.data.length > 0) {
            trendData[category.type] = response.data
          }
        } catch (error) {
          console.error(`加载${category.name}趋势失败:`, error)
        }
      })
    )

    assetCategoriesTrendData.value = trendData
  } catch (error) {
    console.error('加载资产分类趋势失败:', error)
    assetCategoriesTrendData.value = {}
  } finally {
    loadingAssetCategories.value = false
  }
}

// 加载所有负债分类趋势
const loadLiabilityCategoriesTrend = async () => {
  loadingLiabilityCategories.value = true
  try {
    const { start, end } = getDateRange()
    const trendData = {}

    // 并行加载所有分类的数据
    await Promise.all(
      liabilityCategories.map(async (category) => {
        try {
          const response = await analysisAPI.getLiabilityCategoryTrend(category.type, start, end)
          if (response.success && response.data && response.data.length > 0) {
            trendData[category.type] = response.data
          }
        } catch (error) {
          console.error(`加载${category.name}趋势失败:`, error)
        }
      })
    )

    liabilityCategoriesTrendData.value = trendData
  } catch (error) {
    console.error('加载负债分类趋势失败:', error)
    liabilityCategoriesTrendData.value = {}
  } finally {
    loadingLiabilityCategories.value = false
  }
}

// 加载所有净资产分类趋势
const loadNetAssetCategoriesTrend = async () => {
  loadingNetAssetCategories.value = true
  try {
    const { start, end } = getDateRange()
    const trendData = {}

    // 并行加载所有分类的数据
    await Promise.all(
      netAssetCategories.map(async (category) => {
        try {
          const response = await analysisAPI.getNetAssetCategoryTrend(category.code, start, end)
          if (response.success && response.data && response.data.length > 0) {
            trendData[category.code] = response.data
          }
        } catch (error) {
          console.error(`加载${category.name}趋势失败:`, error)
        }
      })
    )

    netAssetCategoriesTrendData.value = trendData
  } catch (error) {
    console.error('加载净资产分类趋势失败:', error)
    netAssetCategoriesTrendData.value = {}
  } finally {
    loadingNetAssetCategories.value = false
  }
}

// 选择资产分类（触发钻取）
const selectAssetCategory = async (stat) => {
  selectedAssetCategory.value = stat

  // 找到对应的分类类型
  const category = assetCategories.find(c => c.name === stat.name)
  if (!category) return

  loadingAccountsTrend.value = true
  try {
    const { start, end } = getDateRange()
    const response = await analysisAPI.getAssetAccountsTrendByCategory(category.type, start, end)

    if (response.success && response.data) {
      accountsTrendData.value = response.data
    } else {
      accountsTrendData.value = {}
    }
  } catch (error) {
    console.error('加载账号趋势失败:', error)
    accountsTrendData.value = {}
  } finally {
    loadingAccountsTrend.value = false
  }
}

// 选择负债分类（触发钻取）
const selectLiabilityCategory = async (stat) => {
  selectedLiabilityCategory.value = stat

  // 找到对应的分类类型
  const category = liabilityCategories.find(c => c.name === stat.name)
  if (!category) return

  loadingLiabilityAccountsTrend.value = true
  try {
    const { start, end } = getDateRange()
    const response = await analysisAPI.getLiabilityAccountsTrendByCategory(category.type, start, end)

    if (response.success && response.data) {
      liabilityAccountsTrendData.value = response.data
    } else {
      liabilityAccountsTrendData.value = {}
    }
  } catch (error) {
    console.error('加载负债账号趋势失败:', error)
    liabilityAccountsTrendData.value = {}
  } finally {
    loadingLiabilityAccountsTrend.value = false
  }
}

// 加载所有趋势
const loadAllTrends = () => {
  // 切换tab时清除选中的分类
  selectedAssetCategory.value = null
  accountsTrendData.value = {}
  selectedLiabilityCategory.value = null
  liabilityAccountsTrendData.value = {}

  if (activeTab.value === 'overall') {
    loadOverallTrend()
  } else if (activeTab.value === 'asset') {
    loadAssetCategoriesTrend()
  } else if (activeTab.value === 'liability') {
    loadLiabilityCategoriesTrend()
  } else if (activeTab.value === 'netAsset') {
    loadNetAssetCategoriesTrend()
  }
}

// 监听 tab 切换
watch(activeTab, () => {
  loadAllTrends()
})

onMounted(() => {
  loadAllTrends()
})
</script>
