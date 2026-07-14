import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { changePassword, fetchCurrentUser, fetchCurrentUserPermissions, login, logout, type CurrentUser, type LoginPayload, type LoginResult } from '@/api/auth'

const TOKEN_KEY = 'accessToken'
const REFRESH_TOKEN_KEY = 'refreshToken'
const PASSWORD_CHANGE_KEY = 'passwordChangeRequired'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(sessionStorage.getItem(TOKEN_KEY) ?? '')
  const refreshToken = ref(sessionStorage.getItem(REFRESH_TOKEN_KEY) ?? '')
  const passwordChangeRequired = ref(sessionStorage.getItem(PASSWORD_CHANGE_KEY) === 'true')
  const user = ref<CurrentUser | null>(null)
  const permissions = ref<string[]>([])
  const loading = ref(false)

  const isAuthenticated = computed(() => Boolean(token.value))
  const displayName = computed(() => user.value?.username ?? '管理员')

  function applySession(result: LoginResult) {
    token.value = result.accessToken; refreshToken.value = result.refreshToken; passwordChangeRequired.value = result.passwordChangeRequired
    sessionStorage.setItem(TOKEN_KEY, result.accessToken); sessionStorage.setItem(REFRESH_TOKEN_KEY, result.refreshToken); sessionStorage.setItem(PASSWORD_CHANGE_KEY, String(result.passwordChangeRequired))
  }

  async function signIn(payload: LoginPayload) {
    loading.value = true
    try {
      const result = await login(payload)
      applySession(result)
      if (!result.passwordChangeRequired) await loadCurrentUser()
      return result
    } finally {
      loading.value = false
    }
  }

  async function completePasswordChange(currentPassword: string, newPassword: string) {
    const result = await changePassword(currentPassword, newPassword)
    user.value = null; permissions.value = []; applySession(result); await loadCurrentUser()
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
    sessionStorage.removeItem(PASSWORD_CHANGE_KEY)
    passwordChangeRequired.value = false
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

  return { token, user, permissions, loading, isAuthenticated, passwordChangeRequired, displayName, signIn, completePasswordChange, loadCurrentUser, signOut, can }
})
