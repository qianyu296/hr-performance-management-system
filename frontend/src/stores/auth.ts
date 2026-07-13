import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { fetchCurrentUser, login, type CurrentUser, type LoginPayload } from '@/api/auth'

const TOKEN_KEY = 'accessToken'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(sessionStorage.getItem(TOKEN_KEY) ?? '')
  const user = ref<CurrentUser | null>(null)
  const loading = ref(false)

  const isAuthenticated = computed(() => Boolean(token.value))
  const displayName = computed(() => user.value?.username ?? '管理员')

  async function signIn(payload: LoginPayload) {
    loading.value = true
    try {
      const result = await login(payload)
      token.value = result.accessToken
      sessionStorage.setItem(TOKEN_KEY, result.accessToken)
      user.value = await fetchCurrentUser()
    } finally {
      loading.value = false
    }
  }

  async function loadCurrentUser() {
    if (!token.value || user.value) return user.value
    try {
      user.value = await fetchCurrentUser()
      return user.value
    } catch (error) {
      signOut()
      throw error
    }
  }

  function signOut() {
    token.value = ''
    user.value = null
    sessionStorage.removeItem(TOKEN_KEY)
  }

  return { token, user, loading, isAuthenticated, displayName, signIn, loadCurrentUser, signOut }
})
