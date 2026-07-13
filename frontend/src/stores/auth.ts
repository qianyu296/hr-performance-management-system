import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { fetchCurrentUser, fetchCurrentUserPermissions, login, logout, type CurrentUser, type LoginPayload } from '@/api/auth'

const TOKEN_KEY = 'accessToken'
const REFRESH_TOKEN_KEY = 'refreshToken'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(sessionStorage.getItem(TOKEN_KEY) ?? '')
  const refreshToken = ref(sessionStorage.getItem(REFRESH_TOKEN_KEY) ?? '')
  const user = ref<CurrentUser | null>(null)
  const permissions = ref<string[]>([])
  const loading = ref(false)

  const isAuthenticated = computed(() => Boolean(token.value))
  const displayName = computed(() => user.value?.username ?? '管理员')

  async function signIn(payload: LoginPayload) {
    loading.value = true
    try {
      const result = await login(payload)
      token.value = result.accessToken
      refreshToken.value = result.refreshToken
      sessionStorage.setItem(TOKEN_KEY, result.accessToken)
      sessionStorage.setItem(REFRESH_TOKEN_KEY, result.refreshToken)
      await loadCurrentUser()
    } finally {
      loading.value = false
    }
  }

  async function loadCurrentUser() {
    if (!token.value || user.value) return user.value
    try {
      const [currentUser, currentPermissions] = await Promise.all([fetchCurrentUser(), fetchCurrentUserPermissions()])
      user.value = currentUser
      permissions.value = currentPermissions
      return user.value
    } catch (error) {
      clearSession()
      throw error
    }
  }

  function can(permission?: string) {
    return !permission || permissions.value.includes(permission)
  }

  function clearSession() {
    token.value = ''
    refreshToken.value = ''
    user.value = null
    permissions.value = []
    sessionStorage.removeItem(TOKEN_KEY)
    sessionStorage.removeItem(REFRESH_TOKEN_KEY)
  }

  async function signOut() {
    const accessToken = token.value
    clearSession()
    if (!accessToken) return
    try {
      await logout(accessToken)
    } catch {
      // Local state is already cleared; the token will expire or be rejected by the next protected request.
    }
  }

  return { token, user, permissions, loading, isAuthenticated, displayName, signIn, loadCurrentUser, signOut, can }
})
