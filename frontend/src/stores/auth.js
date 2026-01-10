import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authAPI } from '../api/auth'

export const useAuthStore = defineStore('auth', () => {
  // State
  const token = ref(localStorage.getItem('token') || null)
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))

  // Getters
  const isAuthenticated = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const familyId = computed(() => user.value?.familyId)
  const userId = computed(() => user.value?.id)
  const username = computed(() => user.value?.username)

  // Actions
  const login = async (username, password) => {
    try {
      const response = await authAPI.login(username, password)

      if (response.success) {
        // Store token and user info
        token.value = response.data.token
        user.value = response.data.user

        // Persist to localStorage
        localStorage.setItem('token', response.data.token)
        localStorage.setItem('user', JSON.stringify(response.data.user))

        // Verify token was saved
        const savedToken = localStorage.getItem('token')
        const savedUser = localStorage.getItem('user')

        console.log('=== Login Debug Info ===')
        console.log('Login successful for:', username)
        console.log('User data:', {
          id: user.value.id,
          username: user.value.username,
          role: user.value.role,
          familyId: user.value.familyId
        })
        console.log('Token saved:', !!savedToken)
        console.log('Token length:', savedToken?.length || 0)
        console.log('Token preview:', savedToken?.substring(0, 50) + '...')
        console.log('User saved:', !!savedUser)
        console.log('=======================')

        return response
      } else {
        throw new Error(response.message || 'Login failed')
      }
    } catch (error) {
      console.error('Login error:', error)
      throw error
    }
  }

  const logout = () => {
    // Clear state
    token.value = null
    user.value = null

    // Clear localStorage
    localStorage.removeItem('token')
    localStorage.removeItem('user')

    console.log('Logout successful')
  }

  const refreshUser = async () => {
    try {
      const response = await authAPI.getCurrentUser()
      if (response.success) {
        user.value = response.data
        localStorage.setItem('user', JSON.stringify(response.data))
      }
    } catch (error) {
      console.error('Failed to refresh user:', error)
      // If token is invalid, logout
      if (error.response?.status === 401) {
        logout()
      }
    }
  }

  return {
    // State
    token,
    user,
    // Getters
    isAuthenticated,
    isAdmin,
    familyId,
    userId,
    username,
    // Actions
    login,
    logout,
    refreshUser
  }
})
