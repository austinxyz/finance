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

    console.log(`[FamilyStore ${timestamp}] loadFamilies CALLED - currentFamilyId:`, currentFamilyId.value)
    console.log(`[FamilyStore ${timestamp}] Call stack:`, new Error().stack)

    // Don't reload if already loaded
    if (isLoaded.value && !authStore.isAdmin) {
      console.log(`[FamilyStore ${timestamp}] loadFamilies SKIPPED - already loaded for non-admin`)
      return
    }

    try {
      loading.value = true

      if (authStore.isAdmin) {
        // Admin can see all families
        console.log(`[FamilyStore ${timestamp}] Loading families for ADMIN`)
        const response = await familyAPI.getAll()
        console.log(`[FamilyStore ${timestamp}] Admin families response:`, response)

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
        console.log(`[FamilyStore ${timestamp}] Loading family for REGULAR USER`)
        const response = await familyAPI.getDefault()
        console.log(`[FamilyStore ${timestamp}] User family response:`, response)

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

      console.log(`[FamilyStore ${timestamp}] Loaded families:`, families.value)
      console.log(`[FamilyStore ${timestamp}] About to call initializeCurrentFamily - currentFamilyId before:`, currentFamilyId.value)

      // Set current family
      await initializeCurrentFamily()

      console.log(`[FamilyStore ${timestamp}] loadFamilies COMPLETED - final currentFamilyId:`, currentFamilyId.value)
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

    console.log(`[FamilyStore ${timestamp}] initializeCurrentFamily CALLED - currentFamilyId before:`, currentFamilyId.value)
    console.log(`[FamilyStore ${timestamp}] isAdmin:`, authStore.isAdmin)

    if (authStore.isAdmin) {
      // For admin, try to restore last selected family from localStorage
      const savedFamilyId = localStorage.getItem('adminSelectedFamilyId')
      console.log(`[FamilyStore ${timestamp}] localStorage.getItem('adminSelectedFamilyId'):`, savedFamilyId)

      if (savedFamilyId) {
        const familyId = parseInt(savedFamilyId)
        console.log(`[FamilyStore ${timestamp}] Parsed savedFamilyId:`, familyId)

        if (families.value.find(f => f.id === familyId)) {
          console.log(`[FamilyStore ${timestamp}] Found saved family in families array, restoring to:`, familyId)
          currentFamilyId.value = familyId
          console.log(`[FamilyStore ${timestamp}] Admin restored family - currentFamilyId now:`, currentFamilyId.value)
          return
        } else {
          console.log(`[FamilyStore ${timestamp}] Saved family ${familyId} NOT found in families array`)
        }
      }

      // If no saved selection or invalid, use first family
      if (families.value.length > 0) {
        console.log(`[FamilyStore ${timestamp}] No valid saved selection - using first family:`, families.value[0].id)
        currentFamilyId.value = families.value[0].id
        console.log(`[FamilyStore ${timestamp}] Admin using first family - currentFamilyId now:`, currentFamilyId.value)
      }
    } else {
      // For regular user, ALWAYS use the family from the loaded families array
      // (which comes from getDefault() API)
      console.log(`[FamilyStore ${timestamp}] Regular user - using family from families array`)
      if (families.value.length > 0) {
        console.log(`[FamilyStore ${timestamp}] Setting to families[0]:`, families.value[0])
        currentFamilyId.value = families.value[0].id
        console.log(`[FamilyStore ${timestamp}] User family set - currentFamilyId now:`, currentFamilyId.value)
      }
    }

    console.log(`[FamilyStore ${timestamp}] initializeCurrentFamily COMPLETED - final currentFamilyId:`, currentFamilyId.value)
  }

  const setCurrentFamily = (familyId) => {
    const timestamp = Date.now()
    const authStore = useAuthStore()

    console.log(`[FamilyStore ${timestamp}] setCurrentFamily CALLED with familyId:`, familyId)
    console.log(`[FamilyStore ${timestamp}] Current currentFamilyId.value:`, currentFamilyId.value)
    console.log(`[FamilyStore ${timestamp}] isAdmin:`, authStore.isAdmin)
    console.log(`[FamilyStore ${timestamp}] Call stack:`, new Error().stack)

    // Validate family exists
    const familyExists = families.value.find(f => f.id === familyId)
    console.log(`[FamilyStore ${timestamp}] Family ${familyId} exists in families array:`, !!familyExists)

    if (!familyExists) {
      console.error(`[FamilyStore ${timestamp}] Invalid family ID:`, familyId)
      return false
    }

    // Only admin can change family
    if (!authStore.isAdmin && familyId !== authStore.familyId) {
      console.error(`[FamilyStore ${timestamp}] Regular users cannot change family`)
      return false
    }

    console.log(`[FamilyStore ${timestamp}] Changing family from`, currentFamilyId.value, 'to', familyId)
    currentFamilyId.value = familyId
    console.log(`[FamilyStore ${timestamp}] currentFamilyId.value is now:`, currentFamilyId.value)

    // For admin, save selection to localStorage
    if (authStore.isAdmin) {
      console.log(`[FamilyStore ${timestamp}] Saving to localStorage: adminSelectedFamilyId =`, familyId.toString())
      localStorage.setItem('adminSelectedFamilyId', familyId.toString())
      console.log(`[FamilyStore ${timestamp}] Verified localStorage.getItem('adminSelectedFamilyId'):`, localStorage.getItem('adminSelectedFamilyId'))
    }

    console.log(`[FamilyStore ${timestamp}] setCurrentFamily COMPLETED - returning true`)
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
