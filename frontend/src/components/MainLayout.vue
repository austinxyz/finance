<template>
  <div class="flex h-screen bg-background overflow-hidden">
    <!-- Sidebar -->
    <Sidebar />

    <!-- Main Content Area -->
    <div class="flex-1 flex flex-col overflow-hidden">
      <!-- Top Bar -->
      <header class="bg-card border-b border-border shadow-sm">
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
      <main class="flex-1 overflow-y-auto bg-background">
        <div class="container mx-auto px-6 py-6">
          <RouterView />
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { RouterView } from 'vue-router';
import Sidebar from './Sidebar.vue';

const route = useRoute();

const pageTitle = computed(() => {
  return route.meta?.title || '个人理财管理';
});

const pageDescription = computed(() => {
  return route.meta?.description || '';
});
</script>
