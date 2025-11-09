<template>
  <aside class="w-64 bg-card border-r border-border flex flex-col h-full">
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

    <!-- Navigation -->
    <nav class="flex-1 overflow-y-auto p-4 space-y-2">
      <!-- Dashboard -->
      <router-link
        to="/dashboard"
        class="nav-item"
        :class="isActive('/dashboard')"
      >
        <LayoutDashboard class="w-5 h-5" />
        <span>仪表盘</span>
      </router-link>

      <!-- 资产管理 -->
      <div class="space-y-1">
        <div class="nav-section-title">资产管理</div>
        <router-link
          to="/assets/accounts"
          class="nav-item"
          :class="isActive('/assets/accounts')"
        >
          <Wallet class="w-5 h-5" />
          <span>账户管理</span>
        </router-link>
        <router-link
          to="/assets/batch-update"
          class="nav-item"
          :class="isActive('/assets/batch-update')"
        >
          <PenSquare class="w-5 h-5" />
          <span>批量更新</span>
        </router-link>
        <router-link
          to="/assets/history"
          class="nav-item"
          :class="isActive('/assets/history')"
        >
          <History class="w-5 h-5" />
          <span>历史记录</span>
        </router-link>
      </div>

      <!-- 数据分析 -->
      <div class="space-y-1">
        <div class="nav-section-title">数据分析</div>
        <router-link
          to="/analysis/trend"
          class="nav-item"
          :class="isActive('/analysis/trend')"
        >
          <TrendingUp class="w-5 h-5" />
          <span>趋势分析</span>
        </router-link>
        <router-link
          to="/analysis/allocation"
          class="nav-item"
          :class="isActive('/analysis/allocation')"
        >
          <PieChart class="w-5 h-5" />
          <span>资产配置</span>
        </router-link>
      </div>

      <!-- 系统设置 -->
      <div class="space-y-1">
        <div class="nav-section-title">系统设置</div>
        <router-link
          to="/settings/users"
          class="nav-item"
          :class="isActive('/settings/users')"
        >
          <Users class="w-5 h-5" />
          <span>用户管理</span>
        </router-link>
      </div>
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
import { useRoute } from 'vue-router';
import {
  LayoutDashboard,
  Wallet,
  PenSquare,
  History,
  TrendingUp,
  PieChart,
  Users
} from 'lucide-vue-next';

const route = useRoute();

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
