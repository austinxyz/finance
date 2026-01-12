import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { familyAPI } from '../api/family'
import { useAuthStore } from './auth'

export const useFamilyStore = defineStore('family', () => {
  // State
  const families = ref([])
  const currentFamilyId = ref(null)
  const loading = ref(false)

  // Getters
  const currentFamily = computed(() => {
    return families.value.find(f => f.id === currentFamilyId.value) || null
  })

  const isLoaded = computed(() => families.value.length > 0)

  // Actions
  const loadFamilies = async () => {
    const timestamp = Date.now()
    const authStore = useAuthStore()


    // Don't reload if already loaded
    if (isLoaded.value && !authStore.isAdmin) {
      return
    }

    try {
      loading.value = true

      if (authStore.isAdmin) {
        // Admin can see all families
        const response = await familyAPI.getAll()

        // Handle response format: { success: true, data: [...] }
        if (response && response.success && response.data) {
          families.value = Array.isArray(response.data) ? response.data : []
        } else if (Array.isArray(response)) {
          // Fallback: direct array response
          families.value = response
        } else {
          families.value = []
          console.error('Unexpected families response format:', response)
        }
      } else {
        // Regular user only sees their family
        const response = await familyAPI.getDefault()

        // Handle response format: { success: true, data: {...} }
        if (response && response.success && response.data) {
          families.value = [response.data]
        } else if (response && response.id) {
          // Fallback: direct family object
          families.value = [response]
        } else {
          families.value = []
          console.error('Unexpected family response format:', response)
        }
      }


      // Set current family
      await initializeCurrentFamily()

    } catch (error) {
      console.error(`[FamilyStore ${timestamp}] Failed to load families:`, error)
      families.value = []
    } finally {
      loading.value = false
    }
  }

  const initializeCurrentFamily = async () => {
    const timestamp = Date.now()
    const authStore = useAuthStore()


    if (authStore.isAdmin) {
      // For admin, try to restore last selected family from localStorage
      const savedFamilyId = localStorage.getItem('adminSelectedFamilyId')

      if (savedFamilyId) {
        const familyId = parseInt(savedFamilyId)

        if (families.value.find(f => f.id === familyId)) {
          currentFamilyId.value = familyId
          return
        } else {
        }
      }

      // If no saved selection or invalid, use first family
      if (families.value.length > 0) {
        currentFamilyId.value = families.value[0].id
      }
    } else {
      // For regular user, ALWAYS use the family from the loaded families array
      // (which comes from getDefault() API)
      if (families.value.length > 0) {
        currentFamilyId.value = families.value[0].id
      }
    }

  }

  const setCurrentFamily = (familyId) => {
    const timestamp = Date.now()
    const authStore = useAuthStore()


    // Validate family exists
    const familyExists = families.value.find(f => f.id === familyId)

    if (!familyExists) {
      console.error(`[FamilyStore ${timestamp}] Invalid family ID:`, familyId)
      return false
    }

    // Only admin can change family
    if (!authStore.isAdmin && familyId !== authStore.familyId) {
      console.error(`[FamilyStore ${timestamp}] Regular users cannot change family`)
      return false
    }

    currentFamilyId.value = familyId

    // For admin, save selection to localStorage
    if (authStore.isAdmin) {
      localStorage.setItem('adminSelectedFamilyId', familyId.toString())
    }

    return true
  }

  const reset = () => {
    families.value = []
    currentFamilyId.value = null
    localStorage.removeItem('adminSelectedFamilyId')
  }

  return {
    // State
    families,
    currentFamilyId,
    loading,
    // Getters
    currentFamily,
    isLoaded,
    // Actions
    loadFamilies,
    setCurrentFamily,
    reset
  }
})
