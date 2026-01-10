<template>
  <div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100">
    <div class="bg-white p-8 rounded-lg shadow-xl w-full max-w-md">
      <!-- Logo and Title -->
      <div class="text-center mb-8">
        <div class="text-4xl font-bold text-indigo-600 mb-2">💰</div>
        <h1 class="text-2xl font-bold text-gray-800">Finance App</h1>
        <p class="text-gray-600 mt-2">Personal Finance Management</p>
      </div>

      <!-- Login Form -->
      <form @submit.prevent="handleLogin">
        <div class="mb-4">
          <label for="username" class="block text-sm font-medium text-gray-700 mb-2">
            Username
          </label>
          <input
            id="username"
            v-model="form.username"
            type="text"
            required
            class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent outline-none transition"
            placeholder="Enter your username"
            :disabled="loading"
          />
        </div>

        <div class="mb-6">
          <label for="password" class="block text-sm font-medium text-gray-700 mb-2">
            Password
          </label>
          <input
            id="password"
            v-model="form.password"
            type="password"
            required
            class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent outline-none transition"
            placeholder="Enter your password"
            :disabled="loading"
          />
        </div>

        <!-- Error Message -->
        <div v-if="error" class="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg">
          <p class="text-red-600 text-sm">{{ error }}</p>
        </div>

        <!-- Submit Button -->
        <button
          type="submit"
          :disabled="loading"
          class="w-full bg-indigo-600 text-white py-2 px-4 rounded-lg hover:bg-indigo-700 focus:ring-4 focus:ring-indigo-300 transition disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <span v-if="!loading">Login</span>
          <span v-else class="flex items-center justify-center">
            <svg class="animate-spin h-5 w-5 mr-2" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" fill="none"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            Logging in...
          </span>
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const form = ref({
  username: '',
  password: ''
})

const loading = ref(false)
const error = ref('')

// Check for previous 401 error on mount
onMounted(() => {
  const last401 = sessionStorage.getItem('last401Error')
  if (last401) {
    try {
      const errorInfo = JSON.parse(last401)
      console.error('=== Previous 401 Error ===')
      console.error('URL:', errorInfo.url)
      console.error('Method:', errorInfo.method)
      console.error('Had Token:', errorInfo.hadToken)
      console.error('Response:', errorInfo.response)
      console.error('Time:', errorInfo.timestamp)
      console.error('==========================')

      // Clear it so we don't show it again
      sessionStorage.removeItem('last401Error')
    } catch (e) {
      console.error('Failed to parse last 401 error:', e)
    }
  }
})

const handleLogin = async () => {
  try {
    loading.value = true
    error.value = ''

    await authStore.login(form.value.username, form.value.password)

    // Redirect to dashboard after successful login
    router.push('/dashboard')
  } catch (err) {
    console.error('Login failed:', err)
    error.value = err.response?.data?.message || err.message || 'Login failed. Please check your credentials.'
  } finally {
    loading.value = false
  }
}
</script>
