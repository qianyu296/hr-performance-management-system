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
  tokenType: string
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
