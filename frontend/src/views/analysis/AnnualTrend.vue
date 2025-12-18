<template>
  <div class="p-3 md:p-6 space-y-4 md:space-y-6">
    <!-- 页面标题和控制栏 -->
    <div class="bg-white rounded-lg shadow p-3 md:p-4">
      <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-3 md:gap-0 mb-4">
        <h1 class="text-xl md:text-2xl font-bold text-gray-900">年度趋势分析</h1>
        <div class="flex flex-col sm:flex-row items-stretch sm:items-center gap-2 md:gap-4">
          <!-- 家庭选择 -->
          <div class="flex items-center gap-2">
            <label class="text-xs md:text-sm font-medium text-gray-700 whitespace-nowrap">选择家庭：</label>
            <select v-model.number="familyId" @change="onFamilyChange"
                    class="flex-1 sm:flex-none px-2 md:px-3 py-1.5 md:py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary text-xs md:text-sm min-w-0 sm:min-w-[180px]">
              <option v-for="family in families" :key="family.id" :value="family.id">
                {{ family.familyName }}
              </option>
            </select>
          </div>

          <!-- 年份选择 -->
          <div class="flex items-center gap-2">
            <label class="text-xs md:text-sm font-medium text-gray-700 whitespace-nowrap">显示年数：</label>
            <select v-model.number="displayYears" @change="fetchData"
                    class="flex-1 sm:flex-none px-2 md:px-3 py-1.5 md:py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary text-xs md:text-sm">
              <option :value="3">最近3年</option>
              <option :value="5">最近5年</option>
              <option :value="10">最近10年</option>
              <option :value="999">全部</option>
            </select>
          </div>

          <!-- 货币选择 -->
          <div class="flex items-center gap-2">
            <label class="text-xs md:text-sm font-medium text-gray-700 whitespace-nowrap">显示货币：</label>
            <select v-model="selectedCurrency" @change="onCurrencyChange"
                    class="flex-1 sm:flex-none px-2 md:px-3 py-1.5 md:py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary text-xs md:text-sm">
              <option value="USD">USD ($)</option>
              <option value="CNY">CNY (¥)</option>
              <option value="EUR">EUR (€)</option>
              <option value="GBP">GBP (£)</option>
              <option value="JPY">JPY (¥)</option>
            </select>
          </div>

          <button @click="calculateSummary"
                  class="px-3 md:px-4 py-1.5 md:py-2 bg-primary text-white rounded-md hover:bg-primary/90 text-xs md:text-sm font-medium whitespace-nowrap">
            刷新数据
          </button>
        </div>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="bg-white rounded-lg shadow p-12">
      <div class="flex flex-col items-center justify-center">
        <div class="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
        <p class="text-gray-600 mt-4">加载中...</p>
      </div>
    </div>

    <!-- Tab 切换 -->
    <div v-else-if="summaries.length > 0" class="space-y-4 md:space-y-6">
      <div class="bg-white rounded-lg shadow">
        <div class="border-b border-gray-200 overflow-x-auto scrollbar-hide">
          <nav class="-mb-px flex space-x-2 md:space-x-4 px-3 md:px-6 min-w-max" aria-label="Tabs">
            <button
              v-for="tab in tabs"
              :key="tab.key"
              @click="activeTab = tab.key"
              :class="[
                'whitespace-nowrap py-2 md:py-3 px-3 md:px-4 border-b-2 font-medium text-xs md:text-sm transition-colors',
                activeTab === tab.key
                  ? 'border-primary text-primary'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              ]"
            >
              {{ tab.label }}
            </button>
          </nav>
        </div>

        <!-- 综合趋势 -->
        <div v-show="activeTab === 'overall'" class="p-3 md:p-6">
          <div class="mb-3 md:mb-4">
            <h2 class="text-base md:text-lg font-semibold text-gray-900">综合趋势</h2>
            <p class="text-xs md:text-sm text-gray-500 mt-1">资产、负债、净资产年度变化趋势</p>
          </div>
          <div class="h-64 md:h-96">
            <canvas ref="comprehensiveChartCanvas"></canvas>
          </div>
        </div>

        <!-- 净资产趋势 -->
        <div v-show="activeTab === 'networth'" class="p-3 md:p-6">
          <div class="mb-3 md:mb-4">
            <h2 class="text-base md:text-lg font-semibold text-gray-900">净资产趋势</h2>
            <p class="text-xs md:text-sm text-gray-500 mt-1">净资产年度变化及同比增长率</p>
          </div>
          <div class="h-64 md:h-80">
            <canvas ref="netWorthChartCanvas"></canvas>
          </div>
        </div>

        <!-- 资产趋势 -->
        <div v-show="activeTab === 'asset'" class="p-3 md:p-6">
          <div class="mb-3 md:mb-4">
            <h2 class="text-base md:text-lg font-semibold text-gray-900">资产趋势</h2>
            <p class="text-xs md:text-sm text-gray-500 mt-1">总资产年度变化及同比增长率</p>
          </div>
          <div class="h-64 md:h-80">
            <canvas ref="assetChartCanvas"></canvas>
          </div>
        </div>

        <!-- 负债趋势 -->
        <div v-show="activeTab === 'liability'" class="p-3 md:p-6">
          <div class="mb-3 md:mb-4">
            <h2 class="text-base md:text-lg font-semibold text-gray-900">负债趋势</h2>
            <p class="text-xs md:text-sm text-gray-500 mt-1">总负债年度变化及同比增长率</p>
          </div>
          <div class="h-64 md:h-80">
            <canvas ref="liabilityChartCanvas"></canvas>
          </div>
        </div>

        <!-- 汇总表格 -->
        <div v-show="activeTab === 'table'" class="p-3 md:p-6">
          <div class="mb-3 md:mb-4">
            <h2 class="text-base md:text-lg font-semibold text-gray-900">年度汇总表</h2>
            <p class="text-xs md:text-sm text-gray-500 mt-1">各年度财务数据对比</p>
            <p class="text-xs text-blue-600 mt-1 md:hidden flex items-center gap-1">
              <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 5l7 7-7 7M5 5l7 7-7 7"/>
              </svg>
              左右滑动查看更多
            </p>
          </div>
          <div class="overflow-x-scroll -mx-3 md:mx-0 px-3 md:px-0">
            <table class="min-w-[600px] w-auto border-separate border-spacing-0">
              <thead class="bg-gray-50 border-b border-gray-200 sticky top-0">
                <tr>
                  <th class="px-1 md:px-2 py-1.5 md:py-2 text-left text-[10px] md:text-xs font-medium text-gray-500 uppercase">年份</th>
                  <th class="px-1 md:px-2 py-1.5 md:py-2 text-right text-[10px] md:text-xs font-medium text-gray-500 uppercase">总资产</th>
                  <th class="px-1 md:px-2 py-1.5 md:py-2 text-right text-[10px] md:text-xs font-medium text-gray-500 uppercase">资产同比</th>
                  <th class="px-1 md:px-2 py-1.5 md:py-2 text-right text-[10px] md:text-xs font-medium text-gray-500 uppercase">总负债</th>
                  <th class="px-1 md:px-2 py-1.5 md:py-2 text-right text-[10px] md:text-xs font-medium text-gray-500 uppercase">负债同比</th>
                  <th class="px-1 md:px-2 py-1.5 md:py-2 text-right text-[10px] md:text-xs font-medium text-gray-500 uppercase">净资产</th>
                  <th class="px-1 md:px-2 py-1.5 md:py-2 text-right text-[10px] md:text-xs font-medium text-gray-500 uppercase">净资产同比</th>
                  <th class="px-1 md:px-2 py-1.5 md:py-2 text-right text-[10px] md:text-xs font-medium text-gray-500 uppercase">房产净值同比</th>
                  <th class="px-1 md:px-2 py-1.5 md:py-2 text-right text-[10px] md:text-xs font-medium text-gray-500 uppercase">非房产净值同比</th>
                  <th class="px-1 md:px-2 py-1.5 md:py-2 text-right text-[10px] md:text-xs font-medium text-gray-500 uppercase">房产占比</th>
                  <th class="px-1 md:px-2 py-1.5 md:py-2 text-center text-[10px] md:text-xs font-medium text-gray-500 uppercase">日期</th>
                </tr>
              </thead>
              <tbody class="bg-white">
                <template v-for="summary in summaries" :key="summary.year">
                  <!-- 主行 -->
                  <tr class="hover:bg-gray-50 border-b border-gray-200">
                    <td class="px-1 md:px-2 py-1.5 md:py-2 whitespace-nowrap">
                      <div class="flex items-center gap-0.5 md:gap-1">
                        <button @click="toggleYearExpand(summary.year)"
                                class="text-gray-500 hover:text-gray-700 transition">
                          <svg v-if="!isYearExpanded(summary.year)" class="w-3 h-3 md:w-4 md:h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
                          </svg>
                          <svg v-else class="w-3 h-3 md:w-4 md:h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
                          </svg>
                        </button>
                        <div class="text-[10px] md:text-xs font-medium text-gray-900">{{ summary.year }}</div>
                      </div>
                    </td>
                    <td class="px-1 md:px-2 py-1.5 md:py-2 whitespace-nowrap text-right">
                      <div class="text-[10px] md:text-xs font-medium text-gray-900">{{ getCurrencySymbol(selectedCurrency) }}{{ formatAmount(summary.totalAssets, summary.year) }}</div>
                    </td>
                    <td class="px-1 md:px-2 py-1.5 md:py-2 whitespace-nowrap text-right">
                      <div v-if="summary.displayYoyAssetChange !== null" class="text-[10px] md:text-xs">
                        <div :class="getChangeColor(summary.displayYoyAssetChange)" class="font-medium">
                          {{ formatConvertedChange(summary.displayYoyAssetChange) }}
                        </div>
                        <div :class="getChangeColor(summary.displayYoyAssetChangePct)" class="text-[9px] md:text-xs">
                          ({{ formatPercent(summary.displayYoyAssetChangePct) }})
                        </div>
                      </div>
                      <div v-else class="text-[10px] md:text-xs text-gray-400">-</div>
                    </td>
                    <td class="px-1 md:px-2 py-1.5 md:py-2 whitespace-nowrap text-right">
                      <div class="text-[10px] md:text-xs font-medium text-gray-900">{{ getCurrencySymbol(selectedCurrency) }}{{ formatAmount(summary.totalLiabilities, summary.year) }}</div>
                    </td>
                    <td class="px-1 md:px-2 py-1.5 md:py-2 whitespace-nowrap text-right">
                      <div v-if="summary.displayYoyLiabilityChange !== null" class="text-[10px] md:text-xs">
                        <div :class="getChangeColor(summary.displayYoyLiabilityChange, true)" class="font-medium">
                          {{ formatConvertedChange(summary.displayYoyLiabilityChange) }}
                        </div>
                        <div :class="getChangeColor(summary.displayYoyLiabilityChangePct, true)" class="text-[9px] md:text-xs">
                          ({{ formatPercent(summary.displayYoyLiabilityChangePct) }})
                        </div>
                      </div>
                      <div v-else class="text-[10px] md:text-xs text-gray-400">-</div>
                    </td>
                    <td class="px-1 md:px-2 py-1.5 md:py-2 whitespace-nowrap text-right">
                      <div class="text-[10px] md:text-xs font-bold text-blue-600">{{ getCurrencySymbol(selectedCurrency) }}{{ formatAmount(summary.netWorth, summary.year) }}</div>
                    </td>
                    <td class="px-1 md:px-2 py-1.5 md:py-2 whitespace-nowrap text-right">
                      <div v-if="summary.displayYoyNetWorthChange !== null" class="text-[10px] md:text-xs">
                        <div :class="getChangeColor(summary.displayYoyNetWorthChange)" class="font-medium">
                          {{ formatConvertedChange(summary.displayYoyNetWorthChange) }}
                        </div>
                        <div :class="getChangeColor(summary.displayYoyNetWorthChangePct)" class="text-[9px] md:text-xs">
                          ({{ formatPercent(summary.displayYoyNetWorthChangePct) }})
                        </div>
                      </div>
                      <div v-else class="text-[10px] md:text-xs text-gray-400">基准年</div>
                    </td>
                    <td class="px-1 md:px-2 py-1.5 md:py-2 whitespace-nowrap text-right">
                      <div v-if="summary.displayYoyRealEstateNetWorthChange !== null" class="text-[10px] md:text-xs">
                        <div :class="getChangeColor(summary.displayYoyRealEstateNetWorthChange)" class="font-medium">
                          {{ formatConvertedChange(summary.displayYoyRealEstateNetWorthChange) }}
                        </div>
                        <div :class="getChangeColor(summary.displayYoyRealEstateNetWorthChangePct)" class="text-[9px] md:text-xs">
                          ({{ formatPercent(summary.displayYoyRealEstateNetWorthChangePct) }})
                        </div>
                      </div>
                      <div v-else class="text-[10px] md:text-xs text-gray-400">-</div>
                    </td>
                    <td class="px-1 md:px-2 py-1.5 md:py-2 whitespace-nowrap text-right">
                      <div v-if="summary.displayYoyNonRealEstateNetWorthChange !== null" class="text-[10px] md:text-xs">
                        <div :class="getChangeColor(summary.displayYoyNonRealEstateNetWorthChange)" class="font-medium">
                          {{ formatConvertedChange(summary.displayYoyNonRealEstateNetWorthChange) }}
                        </div>
                        <div :class="getChangeColor(summary.displayYoyNonRealEstateNetWorthChangePct)" class="text-[9px] md:text-xs">
                          ({{ formatPercent(summary.displayYoyNonRealEstateNetWorthChangePct) }})
                        </div>
                      </div>
                      <div v-else class="text-[10px] md:text-xs text-gray-400">-</div>
                    </td>
                    <td class="px-1 md:px-2 py-1.5 md:py-2 whitespace-nowrap text-right">
                      <div v-if="summary.realEstateToNetWorthRatio !== null" class="text-[10px] md:text-xs font-medium text-purple-600">
                        {{ formatPercent(summary.realEstateToNetWorthRatio) }}
                      </div>
                      <div v-else class="text-[10px] md:text-xs text-gray-400">-</div>
                    </td>
                    <td class="px-1 md:px-2 py-1.5 md:py-2 whitespace-nowrap text-center">
                      <div class="text-[10px] md:text-xs text-gray-600">{{ summary.summaryDate }}</div>
                    </td>
                  </tr>

                  <!-- 展开的分类明细（按TYPE显示） -->
                  <tr v-if="isYearExpanded(summary.year)" class="bg-gray-50">
                    <td colspan="11" class="px-2 md:px-6 py-3 md:py-4">
                      <div class="grid grid-cols-1 md:grid-cols-3 gap-3 md:gap-6">

                        <!-- 资产分类 -->
                        <div v-if="summary.assetBreakdown && Object.keys(summary.assetBreakdown).length > 0">
                          <h4 class="text-xs md:text-sm font-semibold text-gray-700 mb-2 md:mb-3">资产分类明细</h4>
                          <div class="space-y-1.5 md:space-y-2">
                            <div v-for="(value, category) in summary.assetBreakdown" :key="category"
                                 class="flex justify-between items-center text-xs md:text-sm">
                              <span class="text-gray-600">{{ getCategoryDisplayName(category) }}</span>
                              <span class="font-medium text-green-700">{{ getCurrencySymbol(selectedCurrency) }}{{ formatAmount(value, summary.year) }}</span>
                            </div>
                          </div>
                        </div>
                        <div v-else>
                          <h4 class="text-xs md:text-sm font-semibold text-gray-700 mb-2 md:mb-3">资产分类明细</h4>
                          <p class="text-xs md:text-sm text-gray-400">暂无分类数据</p>
                        </div>

                        <!-- 负债分类 -->
                        <div v-if="summary.liabilityBreakdown && Object.keys(summary.liabilityBreakdown).length > 0">
                          <h4 class="text-xs md:text-sm font-semibold text-gray-700 mb-2 md:mb-3">负债分类明细</h4>
                          <div class="space-y-1.5 md:space-y-2">
                            <div v-for="(value, category) in summary.liabilityBreakdown" :key="category"
                                 class="flex justify-between items-center text-xs md:text-sm">
                              <span class="text-gray-600">{{ getCategoryDisplayName(category) }}</span>
                              <span class="font-medium text-red-700">{{ getCurrencySymbol(selectedCurrency) }}{{ formatAmount(value, summary.year) }}</span>
                            </div>
                          </div>
                        </div>
                        <div v-else>
                          <h4 class="text-xs md:text-sm font-semibold text-gray-700 mb-2 md:mb-3">负债分类明细</h4>
                          <p class="text-xs md:text-sm text-gray-400">暂无分类数据</p>
                        </div>

                        <!-- 净资产分类 -->
                        <div v-if="summary.netAssetBreakdown && Object.keys(summary.netAssetBreakdown).length > 0">
                          <h4 class="text-xs md:text-sm font-semibold text-gray-700 mb-2 md:mb-3">净资产分类明细</h4>
                          <div class="space-y-1.5 md:space-y-2">
                            <div v-for="(value, category) in summary.netAssetBreakdown" :key="category"
                                 class="flex justify-between items-center text-xs md:text-sm">
                              <span class="text-gray-600">{{ getCategoryDisplayName(category) }}</span>
                              <span class="font-medium" :class="value >= 0 ? 'text-blue-700' : 'text-red-700'">
                                {{ getCurrencySymbol(selectedCurrency) }}{{ formatAmount(value, summary.year) }}
                              </span>
                            </div>
                          </div>
                        </div>
                        <div v-else>
                          <h4 class="text-xs md:text-sm font-semibold text-gray-700 mb-2 md:mb-3">净资产分类明细</h4>
                          <p class="text-xs md:text-sm text-gray-400">暂无分类数据</p>
                        </div>
                      </div>
                    </td>
                  </tr>
                </template>

                <!-- 累计行 -->
                <tr v-if="summaries.length > 0" class="bg-blue-50 border-t-2 border-blue-300 font-semibold">
                  <td class="px-1 md:px-2 py-2 md:py-3 whitespace-nowrap">
                    <div class="text-[10px] md:text-xs font-bold text-blue-900">累计</div>
                  </td>
                  <td class="px-1 md:px-2 py-2 md:py-3 whitespace-nowrap text-right">
                    <div class="text-[10px] md:text-xs text-gray-600">-</div>
                  </td>
                  <td class="px-1 md:px-2 py-2 md:py-3 whitespace-nowrap text-right">
                    <div class="text-[10px] md:text-xs text-gray-600">-</div>
                  </td>
                  <td class="px-1 md:px-2 py-2 md:py-3 whitespace-nowrap text-right">
                    <div class="text-[10px] md:text-xs text-gray-600">-</div>
                  </td>
                  <td class="px-1 md:px-2 py-2 md:py-3 whitespace-nowrap text-right">
                    <div class="text-[10px] md:text-xs text-gray-600">-</div>
                  </td>
                  <td class="px-1 md:px-2 py-2 md:py-3 whitespace-nowrap text-right">
                    <div class="text-[10px] md:text-xs text-gray-600">-</div>
                  </td>
                  <td class="px-1 md:px-2 py-2 md:py-3 whitespace-nowrap text-right">
                    <div class="text-[10px] md:text-xs">
                      <div :class="getChangeColor(cumulativeNetWorthChange)" class="font-bold">
                        {{ formatConvertedChange(cumulativeNetWorthChange) }}
                      </div>
                      <div :class="getChangeColor(averageAnnualGrowthRate)" class="text-[9px] md:text-xs">
                        ({{ formatPercent(averageAnnualGrowthRate) }})
                      </div>
                    </div>
                  </td>
                  <td class="px-1 md:px-2 py-2 md:py-3 whitespace-nowrap text-right">
                    <div class="text-[10px] md:text-xs">
                      <div :class="getChangeColor(cumulativeRealEstateChange)" class="font-bold">
                        {{ formatConvertedChange(cumulativeRealEstateChange) }}
                      </div>
                      <div :class="getChangeColor(averageRealEstateGrowthRate)" class="text-[9px] md:text-xs">
                        ({{ formatPercent(averageRealEstateGrowthRate) }})
                      </div>
                    </div>
                  </td>
                  <td class="px-1 md:px-2 py-2 md:py-3 whitespace-nowrap text-right">
                    <div class="text-[10px] md:text-xs">
                      <div :class="getChangeColor(cumulativeNonRealEstateChange)" class="font-bold">
                        {{ formatConvertedChange(cumulativeNonRealEstateChange) }}
                      </div>
                      <div :class="getChangeColor(averageNonRealEstateGrowthRate)" class="text-[9px] md:text-xs">
                        ({{ formatPercent(averageNonRealEstateGrowthRate) }})
                      </div>
                    </div>
                  </td>
                  <td class="px-1 md:px-2 py-2 md:py-3 whitespace-nowrap text-right">
                    <div class="text-[10px] md:text-xs text-gray-600">-</div>
                  </td>
                  <td class="px-1 md:px-2 py-2 md:py-3 whitespace-nowrap text-center">
                    <div class="text-[10px] md:text-xs text-gray-600">{{ yearRange }}</div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <!-- 无数据提示 -->
    <div v-else>
      <!-- 家庭选择（无数据时） -->
      <div class="bg-white rounded-lg shadow border border-gray-200 p-4">
        <div class="flex items-center gap-3">
          <label class="text-sm font-medium text-gray-700">家庭：</label>
          <select v-model.number="familyId" @change="onFamilyChange"
                  class="border border-gray-300 rounded-md px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 min-w-[200px]">
            <option v-for="family in families" :key="family.id" :value="family.id">
              {{ family.familyName }}
            </option>
          </select>
        </div>
      </div>

      <!-- 无数据提示 -->
      <div class="bg-white rounded-lg shadow border border-gray-200 p-12 text-center">
        <div class="text-gray-400 mb-2">
          <svg class="mx-auto h-12 w-12" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
          </svg>
        </div>
        <h3 class="text-lg font-medium text-gray-900 mb-2">暂无年度数据</h3>
        <p class="text-gray-600 mb-4">请先添加资产和负债记录，然后计算年度汇总</p>
        <button @click="calculateSummary"
                class="px-6 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 transition">
          计算年度汇总
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch, computed } from 'vue'
import { Chart, registerables } from 'chart.js'
import annualSummaryAPI from '@/api/annualSummary'
import familyAPI from '@/api/family'
import exchangeRateAPI from '@/api/exchangeRate'

Chart.register(...registerables)

// 数据
const summaries = ref([])
const families = ref([])
const loading = ref(false)
const displayYears = ref(5)
const familyId = ref(null)
const activeTab = ref('overall')
const expandedYears = ref(new Set())
const selectedCurrency = ref('USD')
const baseCurrency = ref('USD')
const exchangeRate = ref(1)
const exchangeRatesByYear = ref({}) // 每一年的汇率映射

// Tabs 配置
const tabs = [
  { key: 'overall', label: '综合趋势' },
  { key: 'networth', label: '净资产趋势' },
  { key: 'asset', label: '资产趋势' },
  { key: 'liability', label: '负债趋势' },
  { key: 'table', label: '汇总表格' }
]

// 图表引用
const comprehensiveChartCanvas = ref(null)
const netWorthChartCanvas = ref(null)
const assetChartCanvas = ref(null)
const liabilityChartCanvas = ref(null)

// 图表实例
let comprehensiveChart = null
let netWorthChart = null
let assetChart = null
let liabilityChart = null

// 累计数据计算（基于显示货币）
// 累计净资产变化（从基准年到最新年份的总变化）
const cumulativeNetWorthChange = computed(() => {
  if (summaries.value.length < 2) return 0
  const latest = summaries.value[0] // 最新年份（summaries已按年份降序排列）
  const earliest = summaries.value[summaries.value.length - 1] // 最早年份（基准年）
  // 使用各自年份的汇率转换
  return convertAmount(latest.netWorth, latest.year) - convertAmount(earliest.netWorth, earliest.year)
})

// 平均年化增长率（净资产）- CAGR从基准年开始计算（基于显示货币）
const averageAnnualGrowthRate = computed(() => {
  if (summaries.value.length < 2) return 0
  const latest = summaries.value[0]
  const earliest = summaries.value[summaries.value.length - 1] // 基准年
  const years = latest.year - earliest.year
  // 转换为显示货币
  const latestValue = convertAmount(latest.netWorth, latest.year)
  const earliestValue = convertAmount(earliest.netWorth, earliest.year)
  if (years === 0 || earliestValue <= 0) return 0
  // CAGR = (最新值/基准值)^(1/年数) - 1
  return (Math.pow(latestValue / earliestValue, 1 / years) - 1) * 100
})

// 累计房产净值变化（从基准年到最新年份的总变化，基于显示货币）
const cumulativeRealEstateChange = computed(() => {
  if (summaries.value.length < 2) return 0
  const latest = summaries.value[0]
  const earliest = summaries.value[summaries.value.length - 1]

  const latestRealEstateNetWorth = latest.realEstateNetWorth || 0
  const earliestRealEstateNetWorth = earliest.realEstateNetWorth || 0

  // 使用各自年份的汇率转换
  return convertAmount(latestRealEstateNetWorth, latest.year) - convertAmount(earliestRealEstateNetWorth, earliest.year)
})

// 平均年化增长率（房产净值）- CAGR从基准年开始计算（基于显示货币）
const averageRealEstateGrowthRate = computed(() => {
  if (summaries.value.length < 2) return 0
  const latest = summaries.value[0]
  const earliest = summaries.value[summaries.value.length - 1] // 基准年
  const years = latest.year - earliest.year

  const latestRealEstateNetWorth = latest.realEstateNetWorth || 0
  const earliestRealEstateNetWorth = earliest.realEstateNetWorth || 0

  // 转换为显示货币
  const latestValue = convertAmount(latestRealEstateNetWorth, latest.year)
  const earliestValue = convertAmount(earliestRealEstateNetWorth, earliest.year)

  if (years === 0 || earliestValue <= 0) return 0
  // CAGR = (最新值/基准值)^(1/年数) - 1
  return (Math.pow(latestValue / earliestValue, 1 / years) - 1) * 100
})

// 累计非房产净值变化（从基准年到最新年份的总变化，基于显示货币）
const cumulativeNonRealEstateChange = computed(() => {
  if (summaries.value.length < 2) return 0
  const latest = summaries.value[0]
  const earliest = summaries.value[summaries.value.length - 1]

  const latestNonRealEstateNetWorth = latest.nonRealEstateNetWorth || 0
  const earliestNonRealEstateNetWorth = earliest.nonRealEstateNetWorth || 0

  // 使用各自年份的汇率转换
  return convertAmount(latestNonRealEstateNetWorth, latest.year) - convertAmount(earliestNonRealEstateNetWorth, earliest.year)
})

// 平均年化增长率（非房产净值）- CAGR从基准年开始计算（基于显示货币）
const averageNonRealEstateGrowthRate = computed(() => {
  if (summaries.value.length < 2) return 0
  const latest = summaries.value[0]
  const earliest = summaries.value[summaries.value.length - 1] // 基准年
  const years = latest.year - earliest.year

  const latestNonRealEstateNetWorth = latest.nonRealEstateNetWorth || 0
  const earliestNonRealEstateNetWorth = earliest.nonRealEstateNetWorth || 0

  // 转换为显示货币
  const latestValue = convertAmount(latestNonRealEstateNetWorth, latest.year)
  const earliestValue = convertAmount(earliestNonRealEstateNetWorth, earliest.year)

  if (years === 0 || earliestValue <= 0) return 0
  // CAGR = (最新值/基准值)^(1/年数) - 1
  return (Math.pow(latestValue / earliestValue, 1 / years) - 1) * 100
})

// 年份范围
const yearRange = computed(() => {
  if (summaries.value.length === 0) return '-'
  if (summaries.value.length === 1) return summaries.value[0].year.toString()
  const latest = summaries.value[0].year
  const earliest = summaries.value[summaries.value.length - 1].year
  return `${earliest}-${latest}`
})

// 获取家庭列表
const fetchFamilies = async () => {
  try {
    const response = await familyAPI.getAll()
    families.value = response.data

    // 如果familyId还未设置，获取默认家庭
    if (!familyId.value) {
      try {
        const defaultResponse = await familyAPI.getDefault()
        if (defaultResponse.success && defaultResponse.data) {
          familyId.value = defaultResponse.data.id
        } else if (families.value.length > 0) {
          familyId.value = families.value[0].id
        }
      } catch (err) {
        console.error('获取默认家庭失败:', err)
        if (families.value.length > 0) {
          familyId.value = families.value[0].id
        }
      }
    }
  } catch (error) {
    console.error('获取家庭列表失败:', error)
  }
}

// 家庭切换事件
const onFamilyChange = () => {
  fetchData()
}

// 计算基于显示货币的同比数据
const recalculateYoYMetrics = () => {
  if (summaries.value.length < 2) return

  // 从旧到新排序以便计算同比
  const sortedSummaries = [...summaries.value].sort((a, b) => a.year - b.year)

  for (let i = 1; i < sortedSummaries.length; i++) {
    const current = sortedSummaries[i]
    const previous = sortedSummaries[i - 1]

    // 转换为显示货币后的金额
    const currentAssets = convertAmount(current.totalAssets, current.year)
    const previousAssets = convertAmount(previous.totalAssets, previous.year)
    const currentLiabilities = convertAmount(current.totalLiabilities, current.year)
    const previousLiabilities = convertAmount(previous.totalLiabilities, previous.year)
    const currentNetWorth = convertAmount(current.netWorth, current.year)
    const previousNetWorth = convertAmount(previous.netWorth, previous.year)
    const currentRealEstateNetWorth = convertAmount(current.realEstateNetWorth, current.year)
    const previousRealEstateNetWorth = convertAmount(previous.realEstateNetWorth, previous.year)
    const currentNonRealEstateNetWorth = convertAmount(current.nonRealEstateNetWorth, current.year)
    const previousNonRealEstateNetWorth = convertAmount(previous.nonRealEstateNetWorth, previous.year)

    // 重新计算同比变化和百分比
    current.displayYoyAssetChange = currentAssets - previousAssets
    current.displayYoyAssetChangePct = previousAssets > 0 ? ((currentAssets - previousAssets) / previousAssets) * 100 : 0

    current.displayYoyLiabilityChange = currentLiabilities - previousLiabilities
    current.displayYoyLiabilityChangePct = previousLiabilities > 0 ? ((currentLiabilities - previousLiabilities) / previousLiabilities) * 100 : 0

    current.displayYoyNetWorthChange = currentNetWorth - previousNetWorth
    current.displayYoyNetWorthChangePct = previousNetWorth > 0 ? ((currentNetWorth - previousNetWorth) / previousNetWorth) * 100 : 0

    current.displayYoyRealEstateNetWorthChange = currentRealEstateNetWorth - previousRealEstateNetWorth
    current.displayYoyRealEstateNetWorthChangePct = previousRealEstateNetWorth > 0 ? ((currentRealEstateNetWorth - previousRealEstateNetWorth) / previousRealEstateNetWorth) * 100 : 0

    current.displayYoyNonRealEstateNetWorthChange = currentNonRealEstateNetWorth - previousNonRealEstateNetWorth
    current.displayYoyNonRealEstateNetWorthChangePct = previousNonRealEstateNetWorth > 0 ? ((currentNonRealEstateNetWorth - previousNonRealEstateNetWorth) / previousNonRealEstateNetWorth) * 100 : 0
  }

  // 第一年（最早年份）没有同比数据
  const earliest = sortedSummaries[0]
  earliest.displayYoyAssetChange = null
  earliest.displayYoyAssetChangePct = null
  earliest.displayYoyLiabilityChange = null
  earliest.displayYoyLiabilityChangePct = null
  earliest.displayYoyNetWorthChange = null
  earliest.displayYoyNetWorthChangePct = null
  earliest.displayYoyRealEstateNetWorthChange = null
  earliest.displayYoyRealEstateNetWorthChangePct = null
  earliest.displayYoyNonRealEstateNetWorthChange = null
  earliest.displayYoyNonRealEstateNetWorthChangePct = null
}

// 获取数据
const fetchData = async () => {
  if (!familyId.value) return

  loading.value = true
  try {
    const response = await annualSummaryAPI.getRecent(familyId.value, displayYears.value)
    summaries.value = response.data.sort((a, b) => b.year - a.year)

    // 设置基础货币
    if (summaries.value.length > 0 && summaries.value[0].currency) {
      baseCurrency.value = summaries.value[0].currency
      if (!selectedCurrency.value) {
        selectedCurrency.value = summaries.value[0].currency
      }
    }

    // 获取所有年度的汇率
    await fetchExchangeRatesForAllYears()

    // 重新计算基于显示货币的同比数据
    recalculateYoYMetrics()

    await nextTick()
    renderCharts()
  } catch (error) {
    console.error('获取年度汇总数据失败:', error)
  } finally {
    loading.value = false
  }
}

// 获取特定日期的汇率
const fetchExchangeRateForDate = async (date) => {
  if (selectedCurrency.value === baseCurrency.value) {
    return 1
  }

  try {
    const response = await exchangeRateAPI.getRateByDate(selectedCurrency.value, date)
    if (response.success && response.data) {
      // 数据库存储的是该货币对USD的汇率
      if (baseCurrency.value === 'USD') {
        return 1 / response.data.rateToUsd
      } else {
        // 如果基础货币不是USD，需要先获取基础货币的汇率
        const baseRateResponse = await exchangeRateAPI.getRateByDate(baseCurrency.value, date)
        if (baseRateResponse.success && baseRateResponse.data) {
          // 计算相对汇率: baseCurrency -> USD -> selectedCurrency
          return baseRateResponse.data.rateToUsd / response.data.rateToUsd
        }
      }
    }
  } catch (error) {
    console.error(`获取${date}的汇率失败:`, error)
  }
  return 1
}

// 为所有年度获取汇率
const fetchExchangeRatesForAllYears = async () => {
  if (selectedCurrency.value === baseCurrency.value) {
    exchangeRate.value = 1
    // 为所有年份设置汇率为1
    summaries.value.forEach(summary => {
      exchangeRatesByYear.value[summary.year] = 1
    })
    return
  }

  try {
    // 为每一年获取该年最晚日期的汇率
    const ratePromises = summaries.value.map(async (summary) => {
      const rate = await fetchExchangeRateForDate(summary.summaryDate)
      return { year: summary.year, rate }
    })

    const rates = await Promise.all(ratePromises)

    // 构建年份到汇率的映射
    rates.forEach(({ year, rate }) => {
      exchangeRatesByYear.value[year] = rate
    })

    // 设置默认汇率为最新年份的汇率（用于当前日期）
    if (summaries.value.length > 0) {
      exchangeRate.value = exchangeRatesByYear.value[summaries.value[0].year] || 1
    }
  } catch (error) {
    console.error('获取年度汇率失败:', error)
    exchangeRate.value = 1
  }
}

// 货币切换处理
const onCurrencyChange = async () => {
  await fetchExchangeRatesForAllYears()
  // 重新计算基于显示货币的同比数据
  recalculateYoYMetrics()
  await nextTick()
  renderCharts()
}

// 计算年度汇总
const calculateSummary = async () => {
  loading.value = true
  try {
    // 生成从2000年到当前年份的所有年份
    const currentYear = new Date().getFullYear()
    const startYear = 2000
    const allYears = []
    for (let year = startYear; year <= currentYear; year++) {
      allYears.push(year)
    }

    // 批量计算所有年份（存储过程会自动跳过没有数据的年份）
    await annualSummaryAPI.batchCalculate(familyId.value, allYears)

    await fetchData()
  } catch (error) {
    console.error('计算年度汇总失败:', error)
    alert('计算失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 渲染图表
const renderCharts = () => {
  if (summaries.value.length === 0) return

  const sortedData = [...summaries.value].reverse() // 从旧到新排序
  const years = sortedData.map(s => s.year)
  // 使用每年各自的汇率进行转换
  const assets = sortedData.map(s => convertAmount(s.totalAssets, s.year))
  const liabilities = sortedData.map(s => convertAmount(s.totalLiabilities, s.year))
  const netWorths = sortedData.map(s => convertAmount(s.netWorth, s.year))
  // 使用基于显示货币重新计算的同比百分比
  const assetGrowths = sortedData.map(s => s.displayYoyAssetChangePct || 0)
  const liabilityGrowths = sortedData.map(s => s.displayYoyLiabilityChangePct || 0)
  const netWorthGrowths = sortedData.map(s => s.displayYoyNetWorthChangePct || 0)

  const currencySymbol = getCurrencySymbol(selectedCurrency.value)

  // 综合趋势图
  if (comprehensiveChart) comprehensiveChart.destroy()
  if (comprehensiveChartCanvas.value) {
    comprehensiveChart = new Chart(comprehensiveChartCanvas.value, {
      type: 'line',
      data: {
        labels: years,
        datasets: [
          {
            label: '总资产',
            data: assets,
            borderColor: 'rgb(34, 197, 94)',
            backgroundColor: 'rgba(34, 197, 94, 0.1)',
            fill: true,
            tension: 0.4
          },
          {
            label: '总负债',
            data: liabilities,
            borderColor: 'rgb(239, 68, 68)',
            backgroundColor: 'rgba(239, 68, 68, 0.1)',
            fill: true,
            tension: 0.4
          },
          {
            label: '净资产',
            data: netWorths,
            borderColor: 'rgb(59, 130, 246)',
            backgroundColor: 'rgba(59, 130, 246, 0.1)',
            fill: true,
            tension: 0.4,
            borderWidth: 3
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: window.innerWidth < 768 ? 'bottom' : 'top',
            labels: {
              font: {
                size: window.innerWidth < 768 ? 10 : 12,
                weight: 'bold'
              },
              padding: window.innerWidth < 768 ? 6 : 10,
              boxWidth: window.innerWidth < 768 ? 20 : 40
            }
          },
          tooltip: {
            callbacks: {
              label: function(context) {
                return context.dataset.label + ': ' + currencySymbol + Number(context.parsed.y).toLocaleString('en-US', {
                  minimumFractionDigits: window.innerWidth < 768 ? 0 : 2,
                  maximumFractionDigits: window.innerWidth < 768 ? 0 : 2
                })
              }
            },
            titleFont: {
              size: window.innerWidth < 768 ? 11 : 12,
              weight: 'bold'
            },
            bodyFont: {
              size: window.innerWidth < 768 ? 10 : 12,
              weight: 'bold'
            },
            padding: window.innerWidth < 768 ? 6 : 10
          }
        },
        scales: {
          y: {
            beginAtZero: true,
            ticks: {
              callback: function(value) {
                const converted = value
                if (converted >= 1000000) {
                  return currencySymbol + (converted / 1000000).toFixed(window.innerWidth < 768 ? 0 : 1) + 'M'
                } else if (converted >= 1000) {
                  return currencySymbol + (converted / 1000).toFixed(window.innerWidth < 768 ? 0 : 1) + 'K'
                }
                return currencySymbol + converted.toFixed(0)
              },
              font: {
                size: window.innerWidth < 768 ? 9 : 11,
                weight: 'bold'
              },
              maxTicksLimit: window.innerWidth < 768 ? 5 : 8
            }
          },
          x: {
            ticks: {
              font: {
                size: window.innerWidth < 768 ? 9 : 11,
                weight: 'bold'
              },
              maxRotation: window.innerWidth < 768 ? 45 : 0,
              minRotation: window.innerWidth < 768 ? 45 : 0
            }
          }
        }
      }
    })
  }

  // 净资产趋势图（双Y轴）
  if (netWorthChart) netWorthChart.destroy()
  if (netWorthChartCanvas.value) {
    netWorthChart = new Chart(netWorthChartCanvas.value, {
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
            data: netWorthGrowths,
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
            position: window.innerWidth < 768 ? 'bottom' : 'top',
            labels: {
              font: {
                size: window.innerWidth < 768 ? 10 : 12,
                weight: 'bold'
              },
              padding: window.innerWidth < 768 ? 6 : 10,
              boxWidth: window.innerWidth < 768 ? 20 : 40
            }
          },
          tooltip: {
            callbacks: {
              label: function(context) {
                if (context.dataset.label === '净资产') {
                  return context.dataset.label + ': ' + currencySymbol + Number(context.parsed.y).toLocaleString('en-US', {
                    minimumFractionDigits: window.innerWidth < 768 ? 0 : 2,
                    maximumFractionDigits: window.innerWidth < 768 ? 0 : 2
                  })
                } else {
                  return context.dataset.label + ': ' + context.parsed.y.toFixed(window.innerWidth < 768 ? 1 : 2) + '%'
                }
              }
            },
            titleFont: {
              size: window.innerWidth < 768 ? 11 : 12,
              weight: 'bold'
            },
            bodyFont: {
              size: window.innerWidth < 768 ? 10 : 12,
              weight: 'bold'
            },
            padding: window.innerWidth < 768 ? 6 : 10
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
                const converted = value
                if (converted >= 1000000) {
                  return currencySymbol + (converted / 1000000).toFixed(window.innerWidth < 768 ? 0 : 1) + 'M'
                } else if (converted >= 1000) {
                  return currencySymbol + (converted / 1000).toFixed(window.innerWidth < 768 ? 0 : 1) + 'K'
                }
                return currencySymbol + converted.toFixed(0)
              },
              font: {
                size: window.innerWidth < 768 ? 9 : 11,
                weight: 'bold'
              },
              maxTicksLimit: window.innerWidth < 768 ? 5 : 8
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
                return value.toFixed(window.innerWidth < 768 ? 0 : 1) + '%'
              },
              font: {
                size: window.innerWidth < 768 ? 9 : 11,
                weight: 'bold'
              },
              maxTicksLimit: window.innerWidth < 768 ? 5 : 8
            }
          },
          x: {
            ticks: {
              font: {
                size: window.innerWidth < 768 ? 9 : 11,
                weight: 'bold'
              },
              maxRotation: window.innerWidth < 768 ? 45 : 0,
              minRotation: window.innerWidth < 768 ? 45 : 0
            }
          }
        }
      }
    })
  }

  // 资产趋势图（双Y轴）
  if (assetChart) assetChart.destroy()
  if (assetChartCanvas.value) {
    assetChart = new Chart(assetChartCanvas.value, {
      type: 'bar',
      data: {
        labels: years,
        datasets: [
          {
            label: '总资产',
            data: assets,
            backgroundColor: 'rgba(34, 197, 94, 0.7)',
            borderColor: 'rgb(34, 197, 94)',
            borderWidth: 1,
            yAxisID: 'y'
          },
          {
            label: '同比增长率',
            data: assetGrowths,
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
            position: window.innerWidth < 768 ? 'bottom' : 'top',
            labels: {
              font: {
                size: window.innerWidth < 768 ? 10 : 12,
                weight: 'bold'
              },
              padding: window.innerWidth < 768 ? 6 : 10,
              boxWidth: window.innerWidth < 768 ? 20 : 40
            }
          },
          tooltip: {
            callbacks: {
              label: function(context) {
                if (context.dataset.label === '总资产') {
                  return context.dataset.label + ': ' + currencySymbol + Number(context.parsed.y).toLocaleString('en-US', {
                    minimumFractionDigits: window.innerWidth < 768 ? 0 : 2,
                    maximumFractionDigits: window.innerWidth < 768 ? 0 : 2
                  })
                } else {
                  return context.dataset.label + ': ' + context.parsed.y.toFixed(window.innerWidth < 768 ? 1 : 2) + '%'
                }
              }
            },
            titleFont: {
              size: window.innerWidth < 768 ? 11 : 12,
              weight: 'bold'
            },
            bodyFont: {
              size: window.innerWidth < 768 ? 10 : 12,
              weight: 'bold'
            },
            padding: window.innerWidth < 768 ? 6 : 10
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
                const converted = value
                if (converted >= 1000000) {
                  return currencySymbol + (converted / 1000000).toFixed(window.innerWidth < 768 ? 0 : 1) + 'M'
                } else if (converted >= 1000) {
                  return currencySymbol + (converted / 1000).toFixed(window.innerWidth < 768 ? 0 : 1) + 'K'
                }
                return currencySymbol + converted.toFixed(0)
              },
              font: {
                size: window.innerWidth < 768 ? 9 : 11,
                weight: 'bold'
              },
              maxTicksLimit: window.innerWidth < 768 ? 5 : 8
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
                return value.toFixed(window.innerWidth < 768 ? 0 : 1) + '%'
              },
              font: {
                size: window.innerWidth < 768 ? 9 : 11,
                weight: 'bold'
              },
              maxTicksLimit: window.innerWidth < 768 ? 5 : 8
            }
          },
          x: {
            ticks: {
              font: {
                size: window.innerWidth < 768 ? 9 : 11,
                weight: 'bold'
              },
              maxRotation: window.innerWidth < 768 ? 45 : 0,
              minRotation: window.innerWidth < 768 ? 45 : 0
            }
          }
        }
      }
    })
  }

  // 负债趋势图（双Y轴）
  if (liabilityChart) liabilityChart.destroy()
  if (liabilityChartCanvas.value) {
    liabilityChart = new Chart(liabilityChartCanvas.value, {
      type: 'bar',
      data: {
        labels: years,
        datasets: [
          {
            label: '总负债',
            data: liabilities,
            backgroundColor: 'rgba(239, 68, 68, 0.7)',
            borderColor: 'rgb(239, 68, 68)',
            borderWidth: 1,
            yAxisID: 'y'
          },
          {
            label: '同比增长率',
            data: liabilityGrowths,
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
            position: window.innerWidth < 768 ? 'bottom' : 'top',
            labels: {
              font: {
                size: window.innerWidth < 768 ? 10 : 12,
                weight: 'bold'
              },
              padding: window.innerWidth < 768 ? 6 : 10,
              boxWidth: window.innerWidth < 768 ? 20 : 40
            }
          },
          tooltip: {
            callbacks: {
              label: function(context) {
                if (context.dataset.label === '总负债') {
                  return context.dataset.label + ': ' + currencySymbol + Number(context.parsed.y).toLocaleString('en-US', {
                    minimumFractionDigits: window.innerWidth < 768 ? 0 : 2,
                    maximumFractionDigits: window.innerWidth < 768 ? 0 : 2
                  })
                } else {
                  return context.dataset.label + ': ' + context.parsed.y.toFixed(window.innerWidth < 768 ? 1 : 2) + '%'
                }
              }
            },
            titleFont: {
              size: window.innerWidth < 768 ? 11 : 12,
              weight: 'bold'
            },
            bodyFont: {
              size: window.innerWidth < 768 ? 10 : 12,
              weight: 'bold'
            },
            padding: window.innerWidth < 768 ? 6 : 10
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
                const converted = value
                if (converted >= 1000000) {
                  return currencySymbol + (converted / 1000000).toFixed(window.innerWidth < 768 ? 0 : 1) + 'M'
                } else if (converted >= 1000) {
                  return currencySymbol + (converted / 1000).toFixed(window.innerWidth < 768 ? 0 : 1) + 'K'
                }
                return currencySymbol + converted.toFixed(0)
              },
              font: {
                size: window.innerWidth < 768 ? 9 : 11,
                weight: 'bold'
              },
              maxTicksLimit: window.innerWidth < 768 ? 5 : 8
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
                return value.toFixed(window.innerWidth < 768 ? 0 : 1) + '%'
              },
              font: {
                size: window.innerWidth < 768 ? 9 : 11,
                weight: 'bold'
              },
              maxTicksLimit: window.innerWidth < 768 ? 5 : 8
            }
          },
          x: {
            ticks: {
              font: {
                size: window.innerWidth < 768 ? 9 : 11,
                weight: 'bold'
              },
              maxRotation: window.innerWidth < 768 ? 45 : 0,
              minRotation: window.innerWidth < 768 ? 45 : 0
            }
          }
        }
      }
    })
  }
}

// 货币符号映射
const getCurrencySymbol = (currency) => {
  const symbols = {
    'USD': '$',
    'CNY': '¥',
    'EUR': '€',
    'GBP': '£',
    'JPY': '¥',
    'AUD': 'A$',
    'CAD': 'C$'
  }
  return symbols[currency] || currency
}

// 应用汇率转换（支持指定年份的汇率）
const convertAmount = (amount, year = null) => {
  if (!amount && amount !== 0) return 0
  // 如果指定了年份，使用该年份的汇率；否则使用默认汇率
  const rate = year !== null ? (exchangeRatesByYear.value[year] || 1) : exchangeRate.value
  return Number(amount) * rate
}

// 格式化金额（支持指定年份的汇率）
const formatAmount = (amount, year = null) => {
  if (!amount && amount !== 0) return '0.00'
  const converted = convertAmount(amount, year)
  return Number(converted).toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

// 格式化金额（简短）
const formatAmountShort = (amount) => {
  const converted = convertAmount(amount)
  if (converted >= 1000000) {
    return (converted / 1000000).toFixed(1) + 'M'
  } else if (converted >= 1000) {
    return (converted / 1000).toFixed(1) + 'K'
  }
  return converted.toFixed(0)
}

// 格式化变化金额（支持指定年份的汇率）
const formatChange = (amount, year = null) => {
  if (!amount && amount !== 0) return '-'
  const prefix = amount > 0 ? '+' : ''
  const symbol = getCurrencySymbol(selectedCurrency.value)
  return prefix + symbol + formatAmount(Math.abs(amount), year)
}

// 格式化已转换的金额（不再进行汇率转换）
const formatConvertedAmount = (amount) => {
  if (!amount && amount !== 0) return '0.00'
  return Number(amount).toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

// 格式化已转换的变化金额（不再进行汇率转换）
const formatConvertedChange = (amount) => {
  if (!amount && amount !== 0) return '-'
  const prefix = amount > 0 ? '+' : ''
  const symbol = getCurrencySymbol(selectedCurrency.value)
  return prefix + symbol + formatConvertedAmount(Math.abs(amount))
}

// 格式化百分比
const formatPercent = (percent) => {
  if (!percent && percent !== 0) return '-'
  const prefix = percent > 0 ? '+' : ''
  return prefix + Number(percent).toFixed(2) + '%'
}

// 获取变化颜色
const getChangeColor = (value, isLiability = false) => {
  if (!value && value !== 0) return 'text-gray-400'

  // 对于负债，减少是好的（绿色），增加是不好的（红色）
  if (isLiability) {
    return value > 0 ? 'text-red-600' : 'text-green-600'
  }

  // 对于资产和净资产，增加是好的（绿色），减少是不好的（红色）
  return value > 0 ? 'text-green-600' : 'text-red-600'
}

// 分类显示名称映射（TYPE -> 中文名称）
const getCategoryDisplayName = (categoryType) => {
  const categoryNames = {
    // 资产分类
    'CASH': '现金类',
    'STOCKS': '股票投资',
    'RETIREMENT_FUND': '退休基金',
    'INSURANCE': '保险',
    'REAL_ESTATE': '房地产',
    'CRYPTOCURRENCY': '数字货币',
    'PRECIOUS_METALS': '贵金属',
    'OTHER': '其他',
    // 负债分类
    'MORTGAGE': '房贷',
    'AUTO_LOAN': '车贷',
    'CREDIT_CARD': '信用卡',
    'PERSONAL_LOAN': '个人贷款',
    'STUDENT_LOAN': '学生贷款',
    // 净资产分类
    'REAL_ESTATE_NET': '房地产净值',
    'RETIREMENT_FUND_NET': '退休基金净值',
    'LIQUID_NET': '流动资产净值',
    'INSURANCE_NET': '保险净值',
    'INVESTMENT_NET': '投资净值',
    'OTHER_NET': '其他净值'
  }
  return categoryNames[categoryType] || categoryType
}

// 切换年份展开状态
const toggleYearExpand = (year) => {
  if (expandedYears.value.has(year)) {
    expandedYears.value.delete(year)
  } else {
    expandedYears.value.add(year)
  }
  // 强制更新
  expandedYears.value = new Set(expandedYears.value)
}

// 检查是否展开
const isYearExpanded = (year) => {
  return expandedYears.value.has(year)
}

// 监听 activeTab 变化，重新渲染图表
watch(activeTab, async () => {
  await nextTick()
  renderCharts()
})

// 组件挂载时获取数据
onMounted(async () => {
  await fetchFamilies()
  await fetchData()
})
</script>

