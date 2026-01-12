<template>
  <div class="space-y-6 p-6">
    <!-- 页面标题 -->
    <div class="bg-white rounded-lg shadow p-6">
      <h1 class="text-2xl font-bold text-gray-900">系统设置</h1>
      <p class="text-sm text-gray-600 mt-1">管理员系统配置</p>
    </div>

    <!-- 家庭选择设置 -->
    <div class="bg-white rounded-lg shadow p-6">
      <h2 class="text-xl font-semibold text-gray-900 mb-4">默认查看家庭</h2>
      <p class="text-sm text-gray-600 mb-6">
        作为管理员，您可以选择默认查看的家庭。选择后，所有财务数据将显示该家庭的信息。
      </p>

      <div class="max-w-md">
        <label class="block text-sm font-medium text-gray-700 mb-2">
          选择家庭
        </label>
        <div class="flex gap-3">
          <select
            v-model="selectedFamilyId"
            class="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-primary bg-white"
          >
            <option v-for="family in families" :key="family.id" :value="family.id">
              {{ family.familyName }}
            </option>
          </select>
          <button
            @click="handleFamilyChange"
            :disabled="selectedFamilyId === familyStore.currentFamilyId"
            class="px-6 py-2 bg-primary text-white rounded-lg hover:bg-primary/90 focus:ring-2 focus:ring-primary focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            应用更改
          </button>
        </div>

        <div v-if="currentFamily" class="mt-4 p-4 bg-blue-50 border border-blue-200 rounded-lg">
          <h3 class="text-sm font-semibold text-gray-900 mb-2">当前家庭信息</h3>
          <div class="text-sm text-gray-700 space-y-1">
            <p><span class="font-medium">家庭名称:</span> {{ currentFamily.familyName }}</p>
            <p><span class="font-medium">家庭ID:</span> {{ currentFamily.id }}</p>
            <p v-if="currentFamily.description" class="text-gray-600">{{ currentFamily.description }}</p>
          </div>
        </div>

        <div v-if="saveMessage" class="mt-4 p-3 bg-green-50 border border-green-200 rounded-lg">
          <p class="text-green-700 text-sm">✓ {{ saveMessage }}</p>
        </div>
      </div>
    </div>

    <!-- 其他系统设置可以在这里添加 -->
    <div class="bg-white rounded-lg shadow p-6">
      <h2 class="text-xl font-semibold text-gray-900 mb-4">其他设置</h2>
      <p class="text-sm text-gray-500">更多系统设置功能即将上线...</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useFamilyStore } from '../../stores/family'
import { useAuthStore } from '../../stores/auth'
import { useRouter } from 'vue-router'

const router = useRouter()
const familyStore = useFamilyStore()
const authStore = useAuthStore()

const selectedFamilyId = ref(null)
const saveMessage = ref('')

const families = computed(() => familyStore.families)
const currentFamily = computed(() => familyStore.currentFamily)

onMounted(async () => {
  // Check if user is admin
  if (!authStore.isAdmin) {
    alert('只有管理员可以访问此页面')
    router.push('/')
    return
  }

  // Load families if not already loaded
  if (!familyStore.isLoaded) {
    await familyStore.loadFamilies()
  }

  // Set selected family to current family
  selectedFamilyId.value = familyStore.currentFamilyId
})

const handleFamilyChange = () => {
  const timestamp = Date.now()
  console.log(`[SystemSettings ${timestamp}] handleFamilyChange START - selectedFamilyId:`, selectedFamilyId.value)
  console.log(`[SystemSettings ${timestamp}] Current familyStore.currentFamilyId:`, familyStore.currentFamilyId)

  const success = familyStore.setCurrentFamily(selectedFamilyId.value)

  console.log(`[SystemSettings ${timestamp}] setCurrentFamily result:`, success)
  console.log(`[SystemSettings ${timestamp}] After setCurrentFamily - familyStore.currentFamilyId:`, familyStore.currentFamilyId)

  if (success) {
    saveMessage.value = '家庭切换成功！页面数据将更新为所选家庭。'

    // Clear message after 3 seconds
    setTimeout(() => {
      saveMessage.value = ''
    }, 3000)

    // Optional: Reload current page to refresh data
    // window.location.reload()
  } else {
    saveMessage.value = ''
    alert('切换家庭失败，请重试')
    selectedFamilyId.value = familyStore.currentFamilyId
  }

  console.log(`[SystemSettings ${timestamp}] handleFamilyChange END - selectedFamilyId:`, selectedFamilyId.value)
}

// Watch for external family changes
watch(() => familyStore.currentFamilyId, (newId, oldId) => {
  const timestamp = Date.now()
  console.log(`[SystemSettings ${timestamp}] WATCH TRIGGERED - familyStore.currentFamilyId changed`)
  console.log(`[SystemSettings ${timestamp}] Old value:`, oldId)
  console.log(`[SystemSettings ${timestamp}] New value:`, newId)
  console.log(`[SystemSettings ${timestamp}] Current selectedFamilyId.value:`, selectedFamilyId.value)

  if (newId !== selectedFamilyId.value) {
    console.log(`[SystemSettings ${timestamp}] Watch updating selectedFamilyId from`, selectedFamilyId.value, 'to', newId)
    selectedFamilyId.value = newId
    console.log(`[SystemSettings ${timestamp}] Watch update complete - selectedFamilyId.value:`, selectedFamilyId.value)
  } else {
    console.log(`[SystemSettings ${timestamp}] Watch skipped - newId matches selectedFamilyId.value`)
  }
})
</script>
