<template>
  <div class="space-y-4 md:space-y-6">
    <!-- 页头、家庭选择和日期选择 -->
    <div class="bg-white rounded-lg shadow p-3 md:p-4">
      <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-3 md:gap-0 mb-4">
        <h2 class="text-md md:text-lg font-semibold text-gray-900">优化建议</h2>
        <div class="flex flex-col sm:flex-row items-stretch sm:items-center gap-2 md:gap-4">
          <div class="flex items-center gap-2">
            <label class="text-xs md:text-sm font-medium text-gray-700">选择家庭：</label>
            <select
              v-model="selectedFamilyId"
              @change="onFamilyChange"
              class="px-2 md:px-3 py-1.5 md:py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary text-sm"
            >
              <option v-for="family in families" :key="family.id" :value="family.id">
                {{ family.familyName }}
              </option>
            </select>
          </div>
          <div class="flex items-center gap-2">
            <label class="text-xs md:text-sm font-medium text-gray-700">查询日期：</label>
          <input
            v-model="selectedDate"
            type="date"
            @change="loadRecommendations"
            class="px-2 md:px-3 py-1.5 md:py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary text-sm"
          />
          <button
            v-if="selectedDate"
            @click="clearDate"
            class="px-2 md:px-3 py-1.5 md:py-2 text-sm text-gray-600 hover:text-gray-800"
          >
            清除
          </button>
          </div>
        </div>
      </div>
      <div class="text-xs md:text-sm text-gray-600" v-if="recommendations.asOfDate">
        <span class="font-medium text-gray-700">数据日期:</span>
        <span class="text-blue-600 font-semibold">{{ formatDate(recommendations.asOfDate) }}</span>
      </div>
    </div>

    <!-- AI Prompt 生成器 -->
    <div class="bg-gradient-to-r from-green-50 to-teal-50 rounded-lg shadow-md p-6 border border-green-100">
      <div class="flex items-start gap-4">
        <div class="flex-shrink-0">
          <div class="w-12 h-12 bg-gradient-to-br from-green-600 to-teal-600 rounded-full flex items-center justify-center shadow-lg">
            <span class="text-2xl">📝</span>
          </div>
        </div>
        <div class="flex-1">
          <div class="flex items-center gap-2 mb-2">
            <h3 class="text-md md:text-lg font-semibold text-gray-900">AI Prompt 生成器</h3>
            <span class="px-2 py-0.5 text-xs font-medium bg-green-100 text-green-700 rounded-full">
              ✨ 适用于任何 AI 工具
            </span>
          </div>
          <p class="text-xs md:text-sm text-gray-600 mb-4">
            自动生成包含您完整财务数据的 Prompt，可复制到 ChatGPT、Claude、Gemini 等任何 AI 工具中获取专业理财建议
          </p>

          <!-- 个人情况输入 -->
          <div class="space-y-3">
            <div>
              <label class="text-xs md:text-sm font-medium text-gray-700 mb-1 block">添加您的个人情况（可选）:</label>
              <textarea
                v-model="personalContext"
                placeholder="例如：&#10;- 我计划明年买房，首付需要50万元&#10;- 我的风险承受能力较低，倾向于稳健投资&#10;- 希望在5年内积累孩子的教育基金30万元&#10;- 计划3年后创业，需要准备启动资金"
                class="w-full px-4 py-3 border border-green-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-transparent text-sm resize-none shadow-sm"
                rows="4"
              ></textarea>
            </div>

            <!-- 按钮组 -->
            <div class="flex items-center gap-3">
              <button
                @click="generatePrompt"
                :disabled="!recommendations || loading"
                class="px-6 py-2.5 bg-gradient-to-r from-green-600 to-teal-600 text-white rounded-lg hover:from-green-700 hover:to-teal-700 disabled:from-gray-300 disabled:to-gray-300 disabled:cursor-not-allowed transition-all text-sm font-medium flex items-center gap-2 shadow-md hover:shadow-lg"
              >
                <span>📋 生成 Prompt</span>
              </button>

              <button
                v-if="generatedPrompt"
                @click="copyPrompt"
                class="px-6 py-2.5 bg-white border-2 border-green-600 text-green-600 rounded-lg hover:bg-green-50 transition-all text-sm font-medium flex items-center gap-2 shadow-md"
              >
                <span v-if="!promptCopied">📋 复制 Prompt</span>
                <span v-else class="text-green-700">✅ 已复制</span>
              </button>
            </div>
          </div>

          <!-- 生成的 Prompt 预览 -->
          <div v-if="generatedPrompt" class="mt-4 bg-white rounded-lg border border-green-200 shadow-sm">
            <div class="px-3 md:px-4 py-2 md:py-3 bg-green-50 border-b border-green-200 flex items-center justify-between">
              <span class="text-sm font-semibold text-gray-700">生成的 Prompt 预览</span>
              <button
                @click="showFullPrompt = !showFullPrompt"
                class="text-xs text-green-600 hover:text-green-700 font-medium"
              >
                {{ showFullPrompt ? '收起' : '展开全部' }}
              </button>
            </div>
            <div class="p-4">
              <pre
                :class="['text-sm text-gray-700 whitespace-pre-wrap font-mono leading-relaxed', showFullPrompt ? '' : 'line-clamp-6']"
              >{{ generatedPrompt }}</pre>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- AI 增强建议输入框 -->
    <div class="bg-gradient-to-r from-purple-50 to-blue-50 rounded-lg shadow-md p-6 border border-purple-100">
      <div class="flex items-start gap-4">
        <div class="flex-shrink-0">
          <div class="w-12 h-12 bg-gradient-to-br from-purple-600 to-blue-600 rounded-full flex items-center justify-center shadow-lg">
            <span class="text-2xl">🤖</span>
          </div>
        </div>
        <div class="flex-1">
          <div class="flex items-center gap-2 mb-2">
            <h3 class="text-md md:text-lg font-semibold text-gray-900">AI 增强理财建议（需配置 API）</h3>
            <span v-if="aiEnabled" class="px-2 py-0.5 text-xs font-medium bg-green-100 text-green-700 rounded-full">
              ✓ 已启用
            </span>
          </div>
          <p class="text-xs md:text-sm text-gray-600 mb-4">
            基于系统的传统分析结果，结合你的个人情况和理财目标，AI 将为你提供更加个性化和深入的专业建议
          </p>

          <!-- 快捷问题示例 -->
          <div class="mb-3">
            <div class="text-xs font-medium text-gray-700 mb-2">常见问题示例（点击填入）:</div>
            <div class="flex flex-wrap gap-2">
              <button
                v-for="(example, index) in exampleQuestions"
                :key="index"
                @click="userContext = example"
                class="px-3 py-1 text-xs bg-white border border-purple-200 text-purple-700 rounded-full hover:bg-purple-50 transition-colors"
              >
                {{ example.substring(0, 30) }}...
              </button>
            </div>
          </div>

          <div class="space-y-3">
            <textarea
              v-model="userContext"
              placeholder="例如：我计划明年买房，首付需要50万，应该如何调整资产配置？&#10;或：我的风险承受能力较低，如何优化投资组合？&#10;或：如何为孩子的教育基金做规划？&#10;或：我想在5年内实现财务自由，需要做哪些准备？"
              class="w-full px-4 py-3 border border-purple-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent text-sm resize-none shadow-sm"
              rows="5"
            ></textarea>
            <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-3 md:gap-0">
              <div class="text-xs text-gray-500 flex items-center gap-2">
                <span v-if="!aiEnabled" class="flex items-center gap-1 text-orange-600">
                  <span>⚠️</span>
                  <span>需要配置 CLAUDE_API_KEY 环境变量</span>
                </span>
                <span v-else class="flex items-center gap-1 text-green-600">
                  <span>✓</span>
                  <span>AI 服务可用 - 使用 Claude 3.5 Sonnet</span>
                </span>
              </div>
              <button
                @click="getAIAdvice"
                :disabled="loadingAI || !userContext.trim()"
                class="px-6 py-2.5 bg-gradient-to-r from-purple-600 to-blue-600 text-white rounded-lg hover:from-purple-700 hover:to-blue-700 disabled:from-gray-300 disabled:to-gray-300 disabled:cursor-not-allowed transition-all text-sm font-medium flex items-center gap-2 shadow-md hover:shadow-lg"
              >
                <span v-if="loadingAI" class="inline-block w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></span>
                <span>{{ loadingAI ? '正在分析中...' : '🚀 获取 AI 建议' }}</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- AI 建议显示 -->
    <div v-if="aiAdvice" class="bg-gradient-to-br from-white to-purple-50 rounded-lg shadow-lg p-6 border border-purple-100">
      <div class="flex items-start gap-4">
        <div class="flex-shrink-0">
          <div class="w-12 h-12 bg-gradient-to-br from-green-400 to-blue-500 rounded-full flex items-center justify-center shadow-md">
            <span class="text-2xl">💡</span>
          </div>
        </div>
        <div class="flex-1">
          <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-3 md:gap-0 mb-4">
            <h3 class="text-md md:text-lg font-semibold text-gray-900 flex items-center gap-2">
              <span>AI 个性化建议</span>
              <span class="px-2 py-0.5 text-xs font-medium bg-purple-100 text-purple-700 rounded-full">
                Claude 3.5 Sonnet
              </span>
            </h3>
            <button
              @click="aiAdvice = ''"
              class="text-gray-400 hover:text-gray-600 transition-colors"
              title="关闭建议"
            >
              <span class="text-xl">×</span>
            </button>
          </div>
          <div class="prose prose-sm max-w-none">
            <div class="text-gray-700 leading-relaxed markdown-content" v-html="formatAIAdvice(aiAdvice)"></div>
          </div>

          <!-- 反馈按钮 -->
          <div class="mt-6 pt-4 border-t border-gray-200 flex items-center gap-3">
            <span class="text-xs md:text-sm text-gray-600">这个建议对你有帮助吗？</span>
            <div class="flex gap-2">
              <button class="px-3 py-1 text-sm bg-green-100 text-green-700 rounded-lg hover:bg-green-200 transition-colors">
                👍 有帮助
              </button>
              <button class="px-3 py-1 text-sm bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors">
                👎 需要改进
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 加载中状态 -->
    <div v-if="loading" class="flex items-center justify-center py-12">
      <div class="text-gray-500">加载中...</div>
    </div>

    <!-- 优化建议内容 -->
    <div v-else class="space-y-4 md:space-y-6">
      <!-- 综合评分卡片 -->
      <div class="bg-white rounded-lg shadow p-3 md:p-6">
        <h3 class="text-md md:text-lg font-semibold text-gray-900 mb-4">财务健康度评分</h3>
        <div class="flex items-center gap-4 md:gap-6">
          <!-- 健康度评分 -->
          <div class="relative w-32 h-32 flex-shrink-0">
            <svg class="w-32 h-32 transform -rotate-90">
              <circle cx="64" cy="64" r="56" stroke="#e5e7eb" stroke-width="12" fill="none" />
              <circle
                cx="64" cy="64" r="56"
                :stroke="getHealthLevelColor(recommendations.healthLevel)"
                stroke-width="12" fill="none"
                :stroke-dasharray="`${(recommendations.overallScore / 100) * 351.858} 351.858`"
                stroke-linecap="round"
              />
            </svg>
            <div class="absolute inset-0 flex flex-col items-center justify-center">
              <div class="text-3xl font-bold" :class="getHealthLevelTextColor(recommendations.healthLevel)">
                {{ formatNumber(recommendations.overallScore) }}
              </div>
              <div class="text-xs text-gray-500">健康分</div>
            </div>
          </div>

          <!-- 健康等级说明 -->
          <div class="flex-1">
            <div class="flex items-center gap-3 mb-3">
              <span class="text-2xl">{{ getHealthLevelIcon(recommendations.healthLevel) }}</span>
              <div>
                <div class="text-lg md:text-xl font-bold" :class="getHealthLevelTextColor(recommendations.healthLevel)">
                  {{ getHealthLevelName(recommendations.healthLevel) }}
                </div>
                <div class="text-xs md:text-sm text-gray-600">{{ getHealthLevelDescription(recommendations.healthLevel) }}</div>
              </div>
            </div>

            <!-- 预期效果 -->
            <div v-if="recommendations.expectedImpact" class="mt-4 p-3 bg-blue-50 rounded-lg">
              <div class="text-sm font-medium text-blue-900 mb-1">预期改善</div>
              <p class="text-sm text-blue-800">{{ recommendations.expectedImpact.overallImprovement }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 优先行动计划 -->
      <div v-if="recommendations.prioritizedActions?.length" class="bg-white rounded-lg shadow p-3 md:p-6">
        <h3 class="text-md md:text-lg font-semibold text-gray-900 mb-4">优先行动计划</h3>
        <div class="space-y-3">
          <div v-for="(action, index) in recommendations.prioritizedActions" :key="index"
               class="flex items-start gap-4 p-4 rounded-lg border-l-4"
               :class="getActionBorderClass(action.priority)">
            <div class="flex-shrink-0 w-8 h-8 rounded-full flex items-center justify-center font-bold text-white"
                 :class="getActionBgClass(action.priority)">
              {{ action.order }}
            </div>
            <div class="flex-1">
              <div class="flex items-center gap-2 mb-1">
                <span :class="getPriorityBadgeClass(action.priority)">
                  {{ getPriorityName(action.priority) }}
                </span>
                <span class="text-xs px-2 py-1 rounded-full bg-gray-100 text-gray-700">
                  {{ getCategoryName(action.category) }}
                </span>
                <span class="text-xs px-2 py-1 rounded-full bg-gray-100 text-gray-700">
                  {{ getTimeframeName(action.timeframe) }}
                </span>
              </div>
              <p class="text-xs md:text-sm text-gray-900 font-medium mb-1">{{ action.action }}</p>
              <p class="text-xs text-gray-600">预期效果: {{ action.expectedImpact }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 资产配置优化 -->
      <div v-if="recommendations.assetAllocationOptimization" class="bg-white rounded-lg shadow p-3 md:p-6">
        <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-3 md:gap-0 mb-4">
          <h3 class="text-md md:text-lg font-semibold text-gray-900">资产配置优化</h3>
          <span :class="getStatusBadgeClass(recommendations.assetAllocationOptimization.status)">
            {{ getStatusName(recommendations.assetAllocationOptimization.status) }}
          </span>
        </div>

        <div class="space-y-4">
          <div class="bg-gray-50 rounded-lg p-4">
            <p class="text-xs md:text-sm text-gray-700 mb-2">{{ recommendations.assetAllocationOptimization.summary }}</p>
            <div class="text-xs text-gray-600">
              评分: <span class="font-semibold">{{ formatNumber(recommendations.assetAllocationOptimization.currentScore) }}</span>
            </div>
          </div>

          <!-- 当前配置 vs 建议配置对比 -->
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <div class="text-xs md:text-sm font-medium text-gray-700 mb-2">当前配置</div>
              <div class="space-y-2">
                <div class="flex justify-between text-sm">
                  <span class="text-gray-600">现金:</span>
                  <span class="font-medium">{{ formatNumber(recommendations.assetAllocationOptimization.currentAllocation.cashPercentage) }}%</span>
                </div>
                <div class="flex justify-between text-sm">
                  <span class="text-gray-600">股票:</span>
                  <span class="font-medium">{{ formatNumber(recommendations.assetAllocationOptimization.currentAllocation.stocksPercentage) }}%</span>
                </div>
                <div class="flex justify-between text-sm">
                  <span class="text-gray-600">退休基金:</span>
                  <span class="font-medium">{{ formatNumber(recommendations.assetAllocationOptimization.currentAllocation.retirementPercentage) }}%</span>
                </div>
                <div class="flex justify-between text-sm">
                  <span class="text-gray-600">房地产:</span>
                  <span class="font-medium">{{ formatNumber(recommendations.assetAllocationOptimization.currentAllocation.realEstatePercentage) }}%</span>
                </div>
                <div class="flex justify-between text-sm">
                  <span class="text-gray-600">其他:</span>
                  <span class="font-medium">{{ formatNumber(recommendations.assetAllocationOptimization.currentAllocation.otherPercentage) }}%</span>
                </div>
              </div>
            </div>
            <div>
              <div class="text-xs md:text-sm font-medium text-gray-700 mb-2">建议配置</div>
              <div class="space-y-2">
                <div class="flex justify-between text-sm">
                  <span class="text-gray-600">现金:</span>
                  <span class="font-medium text-green-600">{{ formatNumber(recommendations.assetAllocationOptimization.recommendedAllocation.cashPercentage) }}%</span>
                </div>
                <div class="flex justify-between text-sm">
                  <span class="text-gray-600">股票:</span>
                  <span class="font-medium text-green-600">{{ formatNumber(recommendations.assetAllocationOptimization.recommendedAllocation.stocksPercentage) }}%</span>
                </div>
                <div class="flex justify-between text-sm">
                  <span class="text-gray-600">退休基金:</span>
                  <span class="font-medium text-green-600">{{ formatNumber(recommendations.assetAllocationOptimization.recommendedAllocation.retirementPercentage) }}%</span>
                </div>
                <div class="flex justify-between text-sm">
                  <span class="text-gray-600">房地产:</span>
                  <span class="font-medium text-green-600">{{ formatNumber(recommendations.assetAllocationOptimization.recommendedAllocation.realEstatePercentage) }}%</span>
                </div>
                <div class="flex justify-between text-sm">
                  <span class="text-gray-600">其他:</span>
                  <span class="font-medium text-green-600">{{ formatNumber(recommendations.assetAllocationOptimization.recommendedAllocation.otherPercentage) }}%</span>
                </div>
              </div>
            </div>
          </div>

          <div v-if="recommendations.assetAllocationOptimization.suggestions?.length">
            <div class="text-xs md:text-sm font-medium text-gray-700 mb-2">具体建议</div>
            <ul class="space-y-2">
              <li v-for="(suggestion, index) in recommendations.assetAllocationOptimization.suggestions" :key="index"
                  class="flex items-start gap-2 text-sm text-gray-600">
                <span class="text-primary mt-0.5">•</span>
                <span>{{ suggestion }}</span>
              </li>
            </ul>
          </div>
        </div>
      </div>

      <!-- 负债管理优化 -->
      <div v-if="recommendations.debtManagementOptimization" class="bg-white rounded-lg shadow p-3 md:p-6">
        <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-3 md:gap-0 mb-4">
          <h3 class="text-md md:text-lg font-semibold text-gray-900">负债管理优化</h3>
          <span :class="getStatusBadgeClass(recommendations.debtManagementOptimization.status)">
            {{ getStatusName(recommendations.debtManagementOptimization.status) }}
          </span>
        </div>

        <div class="space-y-4">
          <div class="bg-gray-50 rounded-lg p-4">
            <p class="text-xs md:text-sm text-gray-700 mb-2">{{ recommendations.debtManagementOptimization.summary }}</p>
            <div class="flex items-center gap-4 text-xs text-gray-600">
              <span>评分: <span class="font-semibold">{{ formatNumber(recommendations.debtManagementOptimization.currentScore) }}</span></span>
              <span>策略: <span class="font-semibold">{{ getStrategyName(recommendations.debtManagementOptimization.recommendedStrategy) }}</span></span>
              <span v-if="recommendations.debtManagementOptimization.expectedSavings">
                预期节省: <span class="font-semibold text-green-600">${{ formatNumber(recommendations.debtManagementOptimization.expectedSavings) }}</span>
              </span>
            </div>
          </div>

          <div v-if="recommendations.debtManagementOptimization.suggestions?.length">
            <div class="text-xs md:text-sm font-medium text-gray-700 mb-2">优化建议</div>
            <ul class="space-y-2">
              <li v-for="(suggestion, index) in recommendations.debtManagementOptimization.suggestions" :key="index"
                  class="flex items-start gap-2 text-sm text-gray-600">
                <span class="text-primary mt-0.5">•</span>
                <span>{{ suggestion }}</span>
              </li>
            </ul>
          </div>
        </div>
      </div>

      <!-- 流动性优化 -->
      <div v-if="recommendations.liquidityOptimization" class="bg-white rounded-lg shadow p-3 md:p-6">
        <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-3 md:gap-0 mb-4">
          <h3 class="text-md md:text-lg font-semibold text-gray-900">流动性优化</h3>
          <span :class="getStatusBadgeClass(recommendations.liquidityOptimization.status)">
            {{ getStatusName(recommendations.liquidityOptimization.status) }}
          </span>
        </div>

        <div class="space-y-4">
          <div class="bg-gray-50 rounded-lg p-4">
            <p class="text-xs md:text-sm text-gray-700 mb-3">{{ recommendations.liquidityOptimization.summary }}</p>
            <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 text-sm">
              <div>
                <div class="text-xs text-gray-500 mb-1">当前现金</div>
                <div class="font-semibold text-blue-600">${{ formatNumber(recommendations.liquidityOptimization.currentCash) }}</div>
              </div>
              <div>
                <div class="text-xs text-gray-500 mb-1">建议现金</div>
                <div class="font-semibold text-green-600">${{ formatNumber(recommendations.liquidityOptimization.recommendedCash) }}</div>
              </div>
              <div>
                <div class="text-xs text-gray-500 mb-1">缺口/盈余</div>
                <div class="font-semibold" :class="recommendations.liquidityOptimization.gap < 0 ? 'text-red-600' : 'text-green-600'">
                  ${{ formatNumber(Math.abs(recommendations.liquidityOptimization.gap)) }}
                  {{ recommendations.liquidityOptimization.gap < 0 ? '(不足)' : '(盈余)' }}
                </div>
              </div>
            </div>
          </div>

          <div v-if="recommendations.liquidityOptimization.suggestions?.length">
            <div class="text-xs md:text-sm font-medium text-gray-700 mb-2">优化建议</div>
            <ul class="space-y-2">
              <li v-for="(suggestion, index) in recommendations.liquidityOptimization.suggestions" :key="index"
                  class="flex items-start gap-2 text-sm text-gray-600">
                <span class="text-primary mt-0.5">•</span>
                <span>{{ suggestion }}</span>
              </li>
            </ul>
          </div>
        </div>
      </div>

      <!-- 风险优化 -->
      <div v-if="recommendations.riskOptimization" class="bg-white rounded-lg shadow p-3 md:p-6">
        <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-3 md:gap-0 mb-4">
          <h3 class="text-md md:text-lg font-semibold text-gray-900">风险优化</h3>
          <span :class="getStatusBadgeClass(recommendations.riskOptimization.status)">
            {{ getStatusName(recommendations.riskOptimization.status) }}
          </span>
        </div>

        <div class="space-y-4">
          <div class="bg-gray-50 rounded-lg p-4">
            <p class="text-xs md:text-sm text-gray-700">{{ recommendations.riskOptimization.summary }}</p>
          </div>

          <div v-if="recommendations.riskOptimization.adjustments?.length">
            <div class="text-xs md:text-sm font-medium text-gray-700 mb-2">建议调整</div>
            <div class="space-y-2">
              <div v-for="(adj, index) in recommendations.riskOptimization.adjustments" :key="index"
                   class="p-3 border border-gray-200 rounded-lg">
                <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-3 md:gap-0 mb-2">
                  <span class="text-xs md:text-sm font-medium text-gray-900">{{ adj.assetType }}</span>
                  <span class="text-xs text-gray-600">{{ adj.reason }}</span>
                </div>
                <div class="flex items-center gap-4 text-sm">
                  <span class="text-gray-600">当前: {{ formatNumber(adj.currentPercentage) }}%</span>
                  <span class="text-gray-400">→</span>
                  <span class="text-green-600 font-medium">建议: {{ formatNumber(adj.recommendedPercentage) }}%</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 税务优化 -->
      <div v-if="recommendations.taxOptimization" class="bg-white rounded-lg shadow p-3 md:p-6">
        <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-3 md:gap-0 mb-4">
          <h3 class="text-md md:text-lg font-semibold text-gray-900">税务优化</h3>
          <span :class="getStatusBadgeClass(recommendations.taxOptimization.status)">
            {{ getStatusName(recommendations.taxOptimization.status) }}
          </span>
        </div>

        <div class="space-y-4">
          <div class="bg-gray-50 rounded-lg p-4">
            <p class="text-xs md:text-sm text-gray-700 mb-3">{{ recommendations.taxOptimization.summary }}</p>
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
              <div>
                <div class="text-xs text-gray-500 mb-1">应税资产占比</div>
                <div class="font-semibold text-blue-600">{{ formatNumber(recommendations.taxOptimization.taxablePercentage) }}%</div>
              </div>
              <div>
                <div class="text-xs text-gray-500 mb-1">优化空间</div>
                <div class="font-semibold text-green-600">{{ formatNumber(recommendations.taxOptimization.optimizationPotential) }}%</div>
              </div>
            </div>
          </div>

          <div v-if="recommendations.taxOptimization.suggestions?.length">
            <div class="text-xs md:text-sm font-medium text-gray-700 mb-2">优化建议</div>
            <ul class="space-y-2">
              <li v-for="(suggestion, index) in recommendations.taxOptimization.suggestions" :key="index"
                  class="flex items-start gap-2 text-sm text-gray-600">
                <span class="text-primary mt-0.5">•</span>
                <span>{{ suggestion }}</span>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { analysisAPI } from '@/api/analysis'
import { familyAPI } from '@/api/family'

const loading = ref(false)
const selectedDate = ref('')
const families = ref([])
const selectedFamilyId = ref(null) // 将从默认家庭API获取
const recommendations = ref({
  asOfDate: '',
  overallScore: 0,
  healthLevel: 'FAIR',
  assetAllocationOptimization: null,
  debtManagementOptimization: null,
  liquidityOptimization: null,
  riskOptimization: null,
  taxOptimization: null,
  prioritizedActions: [],
  expectedImpact: null
})

// AI 建议相关状态
const userContext = ref('')
const aiAdvice = ref('')
const loadingAI = ref(false)
const aiEnabled = ref(false)

// AI Prompt 生成器相关状态
const personalContext = ref('')
const generatedPrompt = ref('')
const promptCopied = ref(false)
const showFullPrompt = ref(false)

// 快捷问题示例
const exampleQuestions = [
  '我计划明年买房，首付需要50万，应该如何调整资产配置？',
  '我的风险承受能力较低，如何优化投资组合？',
  '如何为孩子的教育基金做规划？',
  '我想在5年内实现财务自由，需要做哪些准备？',
  '如何平衡短期流动性和长期投资收益？'
]

const formatNumber = (num, decimals = 2) => {
  if (!num && num !== 0) return '0.00'
  return parseFloat(num).toLocaleString('en-US', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals
  })
}

const formatDate = (dateString) => {
  if (!dateString) return '-'
  if (typeof dateString === 'string' && dateString.match(/^\d{4}-\d{2}-\d{2}$/)) {
    const [year, month, day] = dateString.split('-')
    return `${year}年${month}月${day}日`
  }
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
}

const getHealthLevelColor = (level) => {
  const colors = {
    'EXCELLENT': '#10b981',
    'GOOD': '#3b82f6',
    'FAIR': '#f59e0b',
    'POOR': '#ef4444'
  }
  return colors[level] || colors.FAIR
}

const getHealthLevelTextColor = (level) => {
  const colors = {
    'EXCELLENT': 'text-green-600',
    'GOOD': 'text-blue-600',
    'FAIR': 'text-yellow-600',
    'POOR': 'text-red-600'
  }
  return colors[level] || colors.FAIR
}

const getHealthLevelIcon = (level) => {
  const icons = {
    'EXCELLENT': '🌟',
    'GOOD': '👍',
    'FAIR': '⚠️',
    'POOR': '❌'
  }
  return icons[level] || icons.FAIR
}

const getHealthLevelName = (level) => {
  const names = {
    'EXCELLENT': '优秀',
    'GOOD': '良好',
    'FAIR': '一般',
    'POOR': '较差'
  }
  return names[level] || '一般'
}

const getHealthLevelDescription = (level) => {
  const descriptions = {
    'EXCELLENT': '财务状况卓越,继续保持',
    'GOOD': '财务状况健康,可进一步优化',
    'FAIR': '财务状况尚可,需要改进',
    'POOR': '财务状况堪忧,需要立即行动'
  }
  return descriptions[level] || '财务状况一般'
}

const getStatusBadgeClass = (status) => {
  const classes = {
    'OPTIMAL': 'px-2 py-1 rounded-full text-xs font-medium bg-green-100 text-green-800',
    'ACCEPTABLE': 'px-2 py-1 rounded-full text-xs font-medium bg-blue-100 text-blue-800',
    'NEEDS_ATTENTION': 'px-2 py-1 rounded-full text-xs font-medium bg-yellow-100 text-yellow-800'
  }
  return classes[status] || classes.ACCEPTABLE
}

const getStatusName = (status) => {
  const names = {
    'OPTIMAL': '最优',
    'ACCEPTABLE': '可接受',
    'NEEDS_ATTENTION': '需要关注'
  }
  return names[status] || '可接受'
}

const getPriorityBadgeClass = (priority) => {
  const classes = {
    'CRITICAL': 'px-2 py-1 rounded-full text-xs font-medium bg-red-100 text-red-800',
    'HIGH': 'px-2 py-1 rounded-full text-xs font-medium bg-orange-100 text-orange-800',
    'MEDIUM': 'px-2 py-1 rounded-full text-xs font-medium bg-yellow-100 text-yellow-800',
    'LOW': 'px-2 py-1 rounded-full text-xs font-medium bg-green-100 text-green-800'
  }
  return classes[priority] || classes.MEDIUM
}

const getPriorityName = (priority) => {
  const names = {
    'CRITICAL': '紧急',
    'HIGH': '高优先级',
    'MEDIUM': '中优先级',
    'LOW': '低优先级'
  }
  return names[priority] || '中优先级'
}

const getActionBorderClass = (priority) => {
  const classes = {
    'CRITICAL': 'border-red-500 bg-red-50',
    'HIGH': 'border-orange-500 bg-orange-50',
    'MEDIUM': 'border-yellow-500 bg-yellow-50',
    'LOW': 'border-green-500 bg-green-50'
  }
  return classes[priority] || classes.MEDIUM
}

const getActionBgClass = (priority) => {
  const classes = {
    'CRITICAL': 'bg-red-500',
    'HIGH': 'bg-orange-500',
    'MEDIUM': 'bg-yellow-500',
    'LOW': 'bg-green-500'
  }
  return classes[priority] || classes.MEDIUM
}

const getCategoryName = (category) => {
  const names = {
    'ASSET_ALLOCATION': '资产配置',
    'DEBT': '债务管理',
    'LIQUIDITY': '流动性',
    'RISK': '风险控制',
    'TAX': '税务优化'
  }
  return names[category] || category
}

const getTimeframeName = (timeframe) => {
  const names = {
    'IMMEDIATE': '立即',
    'SHORT_TERM': '短期',
    'MEDIUM_TERM': '中期',
    'LONG_TERM': '长期'
  }
  return names[timeframe] || timeframe
}

const getStrategyName = (strategy) => {
  const names = {
    'AVALANCHE': '雪崩法(高息优先)',
    'SNOWBALL': '雪球法(小额优先)',
    'BALANCED': '均衡偿还'
  }
  return names[strategy] || strategy
}

const loadRecommendations = async () => {
  loading.value = true
  try {
    const response = await analysisAPI.getOptimizationRecommendations(null, selectedFamilyId.value, selectedDate.value || null)
    if (response.success) {
      recommendations.value = response.data
    }
  } catch (error) {
    console.error('加载优化建议失败:', error)
  } finally {
    loading.value = false
  }
}

const loadFamilies = async () => {
  try {
    const response = await familyAPI.getDefault()

    // getDefault() 返回单个家庭对象，需要包装成数组
    // 响应拦截器已经解包一层，所以response就是 { success: true, data: {...} }
    if (response && response.success && response.data && response.data.id) {
      families.value = [response.data]

      // 设置默认选中
      if (!selectedFamilyId.value) {
        selectedFamilyId.value = response.data.id
      }
    } else {
      families.value = []
      console.error('获取默认家庭失败: 返回数据格式错误', response)
    }
  } catch (error) {
    console.error('加载家庭列表失败:', error)
    families.value = []
  }
}

const onFamilyChange = () => {
  loadRecommendations()
}

const clearDate = () => {
  selectedDate.value = ''
  loadRecommendations()
}

// AI 建议相关方法
const getAIAdvice = async () => {
  if (!userContext.value.trim()) return

  loadingAI.value = true
  aiAdvice.value = ''

  try {
    const response = await analysisAPI.getAIAdvice(userContext.value.trim(), null)
    if (response.success && response.data) {
      aiAdvice.value = response.data.advice
      aiEnabled.value = response.data.aiEnabled
    }
  } catch (error) {
    console.error('获取 AI 建议失败:', error)
    aiAdvice.value = '获取 AI 建议时出错，请稍后重试。'
    aiEnabled.value = false
  } finally {
    loadingAI.value = false
  }
}

const formatAIAdvice = (advice) => {
  if (!advice) return ''

  // Convert markdown-like formatting to HTML with better styling
  let formatted = advice
    // Escape HTML
    .replace(/</g, '&lt;').replace(/>/g, '&gt;')
    // Headers (process from most specific to least specific)
    .replace(/^#### (.*$)/gim, '<h5 class="text-sm font-semibold text-gray-800 mt-3 mb-2">$1</h5>')
    .replace(/^### (.*$)/gim, '<h4 class="text-base font-semibold text-gray-900 mt-4 mb-2 border-l-4 border-purple-500 pl-3">$1</h4>')
    .replace(/^## (.*$)/gim, '<h3 class="text-lg font-bold text-gray-900 mt-5 mb-3 pb-2 border-b-2 border-purple-200">$1</h3>')
    .replace(/^# (.*$)/gim, '<h2 class="text-lg md:text-xl font-bold text-gray-900 mt-6 mb-4 pb-2 border-b-2 border-purple-300">$1</h2>')
    // Bold
    .replace(/\*\*(.*?)\*\*/g, '<strong class="font-semibold text-gray-900 bg-yellow-50 px-1 rounded">$1</strong>')
    // Italic
    .replace(/\*(.*?)\*/g, '<em class="italic text-gray-700">$1</em>')
    // Code blocks
    .replace(/```([\s\S]*?)```/g, '<pre class="bg-gray-100 rounded-lg p-3 my-3 overflow-x-auto text-sm"><code>$1</code></pre>')
    // Inline code
    .replace(/`(.*?)`/g, '<code class="bg-gray-100 text-purple-700 px-1.5 py-0.5 rounded text-sm font-mono">$1</code>')
    // Numbered lists
    .replace(/^(\d+)\.\s+(.*$)/gim, '<li class="ml-6 my-2 list-decimal marker:text-purple-600 marker:font-semibold">$2</li>')
    // Bullet lists
    .replace(/^[\-\*]\s+(.*$)/gim, '<li class="ml-6 my-2 list-disc marker:text-purple-500">$1</li>')
    // Line breaks - paragraphs
    .replace(/\n\n+/g, '</p><p class="mt-3 text-gray-700">')
    // Highlight important notes
    .replace(/💡\s*(.*?)(?=\n|$)/g, '<div class="bg-blue-50 border-l-4 border-blue-400 p-3 my-3 rounded-r-lg"><span class="text-blue-700">💡 $1</span></div>')
    .replace(/⚠️\s*(.*?)(?=\n|$)/g, '<div class="bg-yellow-50 border-l-4 border-yellow-400 p-3 my-3 rounded-r-lg"><span class="text-yellow-700">⚠️ $1</span></div>')
    .replace(/✅\s*(.*?)(?=\n|$)/g, '<div class="bg-green-50 border-l-4 border-green-400 p-3 my-3 rounded-r-lg"><span class="text-green-700">✅ $1</span></div>')

  return `<div class="text-gray-700">${formatted}</div>`
}

// AI Prompt 生成器相关方法
const generatePrompt = () => {
  if (!recommendations.value) return

  let prompt = '# 个人理财咨询\n\n'
  prompt += '我是一位寻求专业理财建议的用户，以下是我的财务状况和个人情况：\n\n'

  // 财务健康评分
  prompt += '## 财务健康状况\n\n'
  prompt += `- **财务健康评分**: ${formatNumber(recommendations.value.overallScore)}/100\n`
  prompt += `- **健康等级**: ${getHealthLevelName(recommendations.value.healthLevel)}\n`
  prompt += `- **评估**: ${getHealthLevelDescription(recommendations.value.healthLevel)}\n`
  if (recommendations.value.asOfDate) {
    prompt += `- **数据日期**: ${formatDate(recommendations.value.asOfDate)}\n`
  }
  prompt += '\n'

  // 资产配置信息
  if (recommendations.value.assetAllocationOptimization) {
    const asset = recommendations.value.assetAllocationOptimization
    prompt += '## 资产配置情况\n\n'
    prompt += `**当前状态**: ${getStatusName(asset.status)}\n`
    prompt += `**评分**: ${formatNumber(asset.currentScore)}/100\n\n`

    prompt += '### 当前资产配置\n'
    prompt += `- 现金: ${formatNumber(asset.currentAllocation.cashPercentage)}%\n`
    prompt += `- 股票: ${formatNumber(asset.currentAllocation.stocksPercentage)}%\n`
    prompt += `- 退休基金: ${formatNumber(asset.currentAllocation.retirementPercentage)}%\n`
    prompt += `- 房地产: ${formatNumber(asset.currentAllocation.realEstatePercentage)}%\n`
    prompt += `- 其他: ${formatNumber(asset.currentAllocation.otherPercentage)}%\n\n`

    prompt += '### 系统建议配置\n'
    prompt += `- 现金: ${formatNumber(asset.recommendedAllocation.cashPercentage)}%\n`
    prompt += `- 股票: ${formatNumber(asset.recommendedAllocation.stocksPercentage)}%\n`
    prompt += `- 退休基金: ${formatNumber(asset.recommendedAllocation.retirementPercentage)}%\n`
    prompt += `- 房地产: ${formatNumber(asset.recommendedAllocation.realEstatePercentage)}%\n`
    prompt += `- 其他: ${formatNumber(asset.recommendedAllocation.otherPercentage)}%\n\n`
  }

  // 负债管理信息
  if (recommendations.value.debtManagementOptimization) {
    const debt = recommendations.value.debtManagementOptimization
    prompt += '## 负债管理情况\n\n'
    prompt += `**当前状态**: ${getStatusName(debt.status)}\n`
    prompt += `**评分**: ${formatNumber(debt.currentScore)}/100\n`
    prompt += `**建议策略**: ${getStrategyName(debt.recommendedStrategy)}\n`
    if (debt.expectedSavings) {
      prompt += `**预期节省**: $${formatNumber(debt.expectedSavings)}\n`
    }
    prompt += '\n'
  }

  // 流动性信息
  if (recommendations.value.liquidityOptimization) {
    const liquidity = recommendations.value.liquidityOptimization
    prompt += '## 流动性状况\n\n'
    prompt += `**当前状态**: ${getStatusName(liquidity.status)}\n`
    prompt += `- 当前现金储备: $${formatNumber(liquidity.currentCash)}\n`
    prompt += `- 建议现金储备: $${formatNumber(liquidity.recommendedCash)}\n`
    prompt += `- 缺口/盈余: $${formatNumber(Math.abs(liquidity.gap))} ${liquidity.gap < 0 ? '(不足)' : '(盈余)'}\n\n`
  }

  // 风险优化信息
  if (recommendations.value.riskOptimization) {
    const risk = recommendations.value.riskOptimization
    prompt += '## 风险管理\n\n'
    prompt += `**当前状态**: ${getStatusName(risk.status)}\n`
    if (risk.adjustments && risk.adjustments.length > 0) {
      prompt += '\n### 建议调整\n'
      risk.adjustments.forEach(adj => {
        prompt += `- ${adj.assetType}: 从 ${formatNumber(adj.currentPercentage)}% 调整到 ${formatNumber(adj.recommendedPercentage)}% (${adj.reason})\n`
      })
    }
    prompt += '\n'
  }

  // 优先行动计划
  if (recommendations.value.prioritizedActions && recommendations.value.prioritizedActions.length > 0) {
    prompt += '## 系统推荐的优先行动计划\n\n'
    recommendations.value.prioritizedActions.forEach((action, index) => {
      prompt += `${index + 1}. **${getPriorityName(action.priority)}** [${getCategoryName(action.category)}] ${action.action}\n`
      prompt += `   - 时间框架: ${getTimeframeName(action.timeframe)}\n`
      prompt += `   - 预期效果: ${action.expectedImpact}\n\n`
    })
  }

  // 个人情况
  if (personalContext.value.trim()) {
    prompt += '## 我的个人情况和理财目标\n\n'
    prompt += personalContext.value.trim() + '\n\n'
  }

  // 咨询问题
  prompt += '## 咨询问题\n\n'
  prompt += '基于以上我的财务状况和个人情况，请为我提供：\n\n'
  prompt += '1. 对我当前财务状况的专业分析和评估\n'
  prompt += '2. 针对我个人情况的具体优化建议\n'
  prompt += '3. 详细的行动步骤和实施计划\n'
  prompt += '4. 需要注意的风险点和应对策略\n'
  prompt += '5. 短期（1年内）和长期（3-5年）的理财规划建议\n\n'
  prompt += '请给出专业、具体、可操作的建议。谢谢！\n'

  generatedPrompt.value = prompt
  showFullPrompt.value = false
}

const copyPrompt = async () => {
  if (!generatedPrompt.value) return

  try {
    await navigator.clipboard.writeText(generatedPrompt.value)
    promptCopied.value = true
    setTimeout(() => {
      promptCopied.value = false
    }, 2000)
  } catch (error) {
    console.error('复制失败:', error)
    // Fallback for older browsers
    const textArea = document.createElement('textarea')
    textArea.value = generatedPrompt.value
    textArea.style.position = 'fixed'
    textArea.style.left = '-999999px'
    document.body.appendChild(textArea)
    textArea.select()
    try {
      document.execCommand('copy')
      promptCopied.value = true
      setTimeout(() => {
        promptCopied.value = false
      }, 2000)
    } catch (err) {
      console.error('Fallback copy failed:', err)
    }
    document.body.removeChild(textArea)
  }
}

// 监听selectedFamilyId变化，自动加载数据
watch(selectedFamilyId, (newId) => {
  if (newId) {
    loadRecommendations()
  }
})

onMounted(async () => {
  await loadFamilies()
  // loadFamilies会设置selectedFamilyId，然后watcher会自动加载数据
})
</script>
