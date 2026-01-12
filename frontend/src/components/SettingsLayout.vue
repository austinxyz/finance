<template>
  <div class="settings-layout">
    <div class="settings-container">
      <!-- 侧边栏 -->
      <aside class="settings-sidebar">
        <nav class="settings-nav">
          <div class="nav-section">
            <h3 class="nav-section-title">个人设置</h3>
            <router-link
              v-for="item in personalMenuItems"
              :key="item.path"
              :to="item.path"
              class="nav-item"
              :class="{ active: isActive(item.path) }"
            >
              <span class="nav-icon">{{ item.icon }}</span>
              <span class="nav-label">{{ item.label }}</span>
            </router-link>
          </div>

          <div class="nav-section" v-if="isAdmin">
            <h3 class="nav-section-title">管理员</h3>
            <router-link
              v-for="item in adminMenuItems"
              :key="item.path"
              :to="item.path"
              class="nav-item"
              :class="{ active: isActive(item.path) }"
            >
              <span class="nav-icon">{{ item.icon }}</span>
              <span class="nav-label">{{ item.label }}</span>
            </router-link>
          </div>
        </nav>
      </aside>

      <!-- 主内容区 -->
      <main class="settings-content">
        <slot></slot>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const route = useRoute();
const authStore = useAuthStore();

// 使用auth store的isAdmin
const isAdmin = computed(() => authStore.isAdmin);

// 调试信息
onMounted(() => {
});

const personalMenuItems = [
  { path: '/settings/profile', label: '个人信息', icon: '👤' },
  { path: '/settings/my-family', label: '我的家庭', icon: '🏠' }
];

const adminMenuItems = [
  { path: '/settings/family', label: '家庭配置', icon: '⚙️' },
  { path: '/settings/users', label: '用户管理', icon: '👥' },
  { path: '/settings/backup', label: '备份管理', icon: '💾' }
];

function isActive(path) {
  return route.path === path;
}
</script>

<style scoped>
.settings-layout {
  min-height: calc(100vh - 64px);
  background: var(--surface);
}

.settings-container {
  max-width: 1400px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 24px;
  padding: 24px;
}

/* 侧边栏 */
.settings-sidebar {
  background: var(--card-bg);
  border-radius: 12px;
  padding: 20px;
  height: fit-content;
  position: sticky;
  top: 88px;
}

.settings-nav {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.nav-section {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-section-title {
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  color: var(--text-secondary);
  letter-spacing: 0.05em;
  padding: 0 12px 8px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  color: var(--text-primary);
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
  cursor: pointer;
}

.nav-item:hover {
  background: var(--hover-bg);
}

.nav-item.active {
  background: var(--primary);
  color: white;
}

.nav-icon {
  font-size: 18px;
  width: 20px;
  text-align: center;
}

.nav-label {
  flex: 1;
}

/* 主内容区 */
.settings-content {
  min-height: 400px;
}

/* 响应式 */
@media (max-width: 768px) {
  .settings-container {
    grid-template-columns: 1fr;
    gap: 16px;
    padding: 16px;
  }

  .settings-sidebar {
    position: static;
    padding: 16px;
  }

  .settings-nav {
    gap: 16px;
  }

  .nav-section {
    gap: 2px;
  }
}
</style>
