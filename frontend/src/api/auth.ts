import { http } from './http'

export interface ApiResponse<T> {
  code: string
  message: string
  data: T
  traceId: string
}

export interface LoginPayload {
  username: string
  password: string
}

export interface LoginResult {
  accessToken: string
  refreshToken: string
  tokenType: string
  passwordChangeRequired: boolean
}

export interface CurrentUser {
  userId: string
  username: string
}

export async function login(payload: LoginPayload) {
  const response = await http.post<ApiResponse<LoginResult>>('/auth/login', payload)
  return response.data.data
}

export async function fetchCurrentUser() {
  const response = await http.get<ApiResponse<CurrentUser>>('/me')
  return response.data.data
}

export async function fetchCurrentUserPermissions() {
  const response = await http.get<ApiResponse<string[]>>('/me/permissions')
  return response.data.data
}

export async function refreshSession(refreshToken: string) {
  const response = await http.post<ApiResponse<LoginResult>>('/auth/refresh', { refreshToken })
  return response.data.data
}

export async function logout(accessToken: string) {
  await http.post('/auth/logout', undefined, { headers: { Authorization: `Bearer ${accessToken}` } })
}

export async function changePassword(currentPassword: string, newPassword: string) {
  const response = await http.post<ApiResponse<LoginResult>>('/auth/change-password', { currentPassword, newPassword })
  return response.data.data
}
