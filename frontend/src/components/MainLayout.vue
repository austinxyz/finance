<template>
  <div class="flex h-screen bg-background overflow-hidden">
    <!-- Mobile Header (only visible on small screens) -->
    <header class="lg:hidden fixed top-0 left-0 right-0 z-50 bg-card border-b border-border h-14 flex items-center px-4">
      <!-- Menu Button -->
      <button
        @click="toggleMobileSidebar"
        class="p-2 rounded-lg hover:bg-accent text-foreground"
      >
        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
        </svg>
      </button>

      <!-- Logo -->
      <router-link to="/" class="flex items-center ml-3">
        <div class="w-8 h-8 bg-primary rounded-lg flex items-center justify-center">
          <span class="text-primary-foreground font-bold text-lg">💰</span>
        </div>
        <h1 class="ml-2 text-base font-bold text-foreground">理财管家</h1>
      </router-link>
    </header>

    <!-- Overlay for mobile sidebar -->
    <div
      v-if="showMobileSidebar"
      @click="closeMobileSidebar"
      class="lg:hidden fixed inset-0 bg-black/50 z-40 transition-opacity"
    ></div>

    <!-- Sidebar (responsive) -->
    <aside
      :class="[
        'fixed top-0 bottom-0 z-40 transition-transform duration-300',
        'lg:translate-x-0 lg:static lg:block',
        showMobileSidebar ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'
      ]"
    >
      <Sidebar @navigate="closeMobileSidebar" />
    </aside>

    <!-- Main Content Area -->
    <div class="flex-1 flex flex-col overflow-hidden">
      <!-- Top Bar (desktop only) -->
      <header class="hidden lg:block bg-card border-b border-border shadow-sm">
        <div class="px-6 py-4">
          <div class="flex items-center justify-between">
            <!-- Breadcrumb / Page Title -->
            <div>
              <h2 class="text-xl font-semibold text-foreground">{{ pageTitle }}</h2>
              <p v-if="pageDescription" class="text-sm text-muted-foreground mt-1">
                {{ pageDescription }}
              </p>
            </div>

            <!-- User Actions / Search / Notifications -->
            <div class="flex items-center space-x-4">
              <div class="text-sm text-muted-foreground">
                欢迎回来
              </div>
            </div>
          </div>
        </div>
      </header>

      <!-- Main Content -->
      <main class="flex-1 overflow-y-auto bg-background pt-14 lg:pt-0">
        <div class="container mx-auto px-4 md:px-6 py-4 md:py-6">
          <RouterView />
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { useRoute } from 'vue-router';
import { RouterView } from 'vue-router';
import Sidebar from './Sidebar.vue';

const route = useRoute();
const showMobileSidebar = ref(false);

const pageTitle = computed(() => {
  return route.meta?.title || '个人理财管理';
});

const pageDescription = computed(() => {
  return route.meta?.description || '';
});

const toggleMobileSidebar = () => {
  showMobileSidebar.value = !showMobileSidebar.value;
};

const closeMobileSidebar = () => {
  showMobileSidebar.value = false;
};

// Auto-close sidebar on route change (mobile)
watch(() => route.path, () => {
  closeMobileSidebar();
});
</script>
