<template>
  <aside class="w-64 lg:w-64 md:w-56 sm:w-64 bg-card border-r border-border flex flex-col h-full">
    <!-- Logo/Brand -->
    <div class="p-6 border-b border-border">
      <router-link to="/" class="flex items-center space-x-2">
        <div class="w-8 h-8 bg-primary rounded-lg flex items-center justify-center">
          <span class="text-primary-foreground font-bold text-lg">💰</span>
        </div>
        <div>
          <h1 class="text-lg font-bold text-foreground">理财管家</h1>
          <p class="text-xs text-muted-foreground">Personal Finance</p>
        </div>
      </router-link>
    </div>

    <!-- Top Level Menu Tabs -->
    <div class="border-b border-border">
      <div class="flex">
        <button
          v-for="tab in topLevelTabs"
          :key="tab.key"
          @click="activeTopTab = tab.key"
          :class="[
            'flex-1 px-4 py-3 text-sm font-medium transition-colors',
            activeTopTab === tab.key
              ? 'text-primary border-b-2 border-primary bg-primary/5'
              : 'text-muted-foreground hover:text-foreground hover:bg-accent/50'
          ]"
        >
          <component :is="tab.icon" class="w-4 h-4 mx-auto mb-1" />
          <span class="block text-xs">{{ tab.label }}</span>
        </button>
      </div>
    </div>

    <!-- Navigation - Dynamic based on active tab -->
    <nav class="flex-1 overflow-y-auto p-4 space-y-2">
      <!-- 仪表盘 (always visible) -->
      <router-link
        v-if="activeTopTab === 'manage'"
        to="/dashboard"
        class="nav-item"
        :class="isActive('/dashboard')"
      >
        <LayoutDashboard class="w-5 h-5" />
        <span>仪表盘</span>
      </router-link>

      <!-- 管理类菜单 -->
      <template v-if="activeTopTab === 'manage'">
        <!-- 资产管理 -->
        <div class="space-y-1">
          <div class="nav-section-title">资产管理</div>
          <router-link
            to="/assets/history"
            class="nav-item"
            :class="isActive('/assets/history')"
          >
            <Wallet class="w-5 h-5" />
            <span>账户与记录</span>
          </router-link>
          <router-link
            to="/assets/batch-update"
            class="nav-item"
            :class="isActive('/assets/batch-update')"
          >
            <PenSquare class="w-5 h-5" />
            <span>批量更新</span>
          </router-link>
        </div>

        <!-- 负债管理 -->
        <div class="space-y-1">
          <div class="nav-section-title">负债管理</div>
          <router-link
            to="/liabilities/history"
            class="nav-item"
            :class="isActive('/liabilities/history')"
          >
            <CreditCard class="w-5 h-5" />
            <span>账户与记录</span>
          </router-link>
          <router-link
            to="/liabilities/batch-update"
            class="nav-item"
            :class="isActive('/liabilities/batch-update')"
          >
            <PenSquare class="w-5 h-5" />
            <span>批量更新</span>
          </router-link>
        </div>

        <!-- 支出管理 -->
        <div class="space-y-1">
          <div class="nav-section-title">支出管理</div>
          <router-link
            to="/expenses/categories"
            class="nav-item"
            :class="isActive('/expenses/categories')"
          >
            <Receipt class="w-5 h-5" />
            <span>分类与记录</span>
          </router-link>
          <router-link
            to="/expenses/batch-update"
            class="nav-item"
            :class="isActive('/expenses/batch-update')"
          >
            <PenSquare class="w-5 h-5" />
            <span>批量录入</span>
          </router-link>
          <router-link
            to="/expenses/budget"
            class="nav-item"
            :class="isActive('/expenses/budget')"
          >
            <DollarSign class="w-5 h-5" />
            <span>年度预算</span>
          </router-link>
        </div>
      </template>

      <!-- 分析类菜单 -->
      <template v-if="activeTopTab === 'analysis'">
        <!-- 资产负债分析 -->
        <div class="space-y-1">
          <div class="nav-section-title">资产负债分析</div>
          <router-link
            to="/analysis/trend"
            class="nav-item"
            :class="isActive('/analysis/trend')"
          >
            <TrendingUp class="w-5 h-5" />
            <span>趋势分析</span>
          </router-link>
          <router-link
            to="/analysis/annual-trend"
            class="nav-item"
            :class="isActive('/analysis/annual-trend')"
          >
            <Calendar class="w-5 h-5" />
            <span>年度趋势</span>
          </router-link>
          <router-link
            to="/analysis/allocation"
            class="nav-item"
            :class="isActive('/analysis/allocation')"
          >
            <PieChart class="w-5 h-5" />
            <span>资产配置</span>
          </router-link>
          <router-link
            to="/analysis/metrics"
            class="nav-item"
            :class="isActive('/analysis/metrics')"
          >
            <BarChart3 class="w-5 h-5" />
            <span>财务指标</span>
          </router-link>
        </div>

        <!-- 支出分析 -->
        <div class="space-y-1">
          <div class="nav-section-title">支出分析</div>
          <router-link
            to="/analysis/expense-annual"
            class="nav-item"
            :class="isActive('/analysis/expense-annual')"
          >
            <Receipt class="w-5 h-5" />
            <span>年度支出</span>
          </router-link>
          <router-link
            to="/analysis/expense-annual-actual"
            class="nav-item"
            :class="isActive('/analysis/expense-annual-actual')"
          >
            <DollarSign class="w-5 h-5" />
            <span>年度支出（实际）</span>
          </router-link>
          <router-link
            to="/analysis/expense-budget"
            class="nav-item"
            :class="isActive('/analysis/expense-budget')"
          >
            <BarChart3 class="w-5 h-5" />
            <span>预算执行</span>
          </router-link>
        </div>

        <!-- 智能分析 -->
        <div class="space-y-1">
          <div class="nav-section-title">智能分析</div>
          <router-link
            to="/analysis/risk"
            class="nav-item"
            :class="isActive('/analysis/risk')"
          >
            <Shield class="w-5 h-5" />
            <span>风险评估</span>
          </router-link>
          <router-link
            to="/analysis/optimization"
            class="nav-item"
            :class="isActive('/analysis/optimization')"
          >
            <Lightbulb class="w-5 h-5" />
            <span>优化建议</span>
          </router-link>
        </div>
      </template>

      <!-- 设置类菜单 -->
      <template v-if="activeTopTab === 'settings'">
        <!-- 工具 -->
        <div class="space-y-1">
          <div class="nav-section-title">工具</div>
          <router-link
            to="/tools/exchange-rates"
            class="nav-item"
            :class="isActive('/tools/exchange-rates')"
          >
            <DollarSign class="w-5 h-5" />
            <span>汇率管理</span>
          </router-link>
        </div>

        <!-- 系统设置 -->
        <div class="space-y-1">
          <div class="nav-section-title">系统设置</div>
          <router-link
            to="/settings/family"
            class="nav-item"
            :class="isActive('/settings/family')"
          >
            <Home class="w-5 h-5" />
            <span>家庭配置</span>
          </router-link>
          <router-link
            to="/settings/users"
            class="nav-item"
            :class="isActive('/settings/users')"
          >
            <UserCircle class="w-5 h-5" />
            <span>用户管理</span>
          </router-link>
        </div>
      </template>
    </nav>

    <!-- Footer/User Section -->
    <div class="p-4 border-t border-border">
      <div class="text-xs text-muted-foreground">
        <p>v1.0.0</p>
        <p>© 2025 个人理财管理</p>
      </div>
    </div>
  </aside>
</template>

<script setup>
import { ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import {
  LayoutDashboard,
  Wallet,
  PenSquare,
  CreditCard,
  Receipt,
  TrendingUp,
  Calendar,
  PieChart,
  BarChart3,
  Shield,
  Lightbulb,
  Home,
  UserCircle,
  DollarSign,
  FolderKanban,
  LineChart,
  Settings
} from 'lucide-vue-next';

// Emit navigate event for mobile sidebar close
const emit = defineEmits(['navigate']);

const route = useRoute();
const activeTopTab = ref('manage');

// 顶级分类
const topLevelTabs = [
  { key: 'manage', label: '管理', icon: FolderKanban },
  { key: 'analysis', label: '分析', icon: LineChart },
  { key: 'settings', label: '设置', icon: Settings }
];

// 根据当前路由自动切换顶级tab
watch(() => route.path, (newPath) => {
  if (newPath.startsWith('/analysis')) {
    activeTopTab.value = 'analysis';
  } else if (newPath.startsWith('/settings') || newPath.startsWith('/tools')) {
    activeTopTab.value = 'settings';
  } else {
    activeTopTab.value = 'manage';
  }
}, { immediate: true });

const isActive = (path) => {
  return route.path.startsWith(path)
    ? 'bg-accent text-accent-foreground'
    : 'text-muted-foreground hover:bg-accent/50 hover:text-foreground';
};
</script>

<style scoped>
.nav-item {
  @apply flex items-center space-x-3 px-3 py-2 rounded-md text-sm font-medium transition-colors cursor-pointer;
}

.nav-section-title {
  @apply px-3 py-2 text-xs font-semibold text-muted-foreground uppercase tracking-wider;
}
</style>
