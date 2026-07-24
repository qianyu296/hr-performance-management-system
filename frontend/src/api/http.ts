import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'

export const http = axios.create({ baseURL: '/api/v1', timeout: 10_000 })

const ACCESS_TOKEN_KEY = 'accessToken'
const REFRESH_TOKEN_KEY = 'refreshToken'

export function isForbiddenError(error: unknown) {
  return (error as AxiosError | undefined)?.response?.status === 403
}

interface ApiResponse<T> {
  data: T
}

interface SessionTokens {
  accessToken: string
  refreshToken: string
}

type RetryableRequestConfig = InternalAxiosRequestConfig & { _retry?: boolean }

let refreshInFlight: Promise<SessionTokens> | null = null

function clearStoredTokens() {
  sessionStorage.removeItem(ACCESS_TOKEN_KEY)
  sessionStorage.removeItem(REFRESH_TOKEN_KEY)
}

async function refreshAccessToken(refreshToken: string) {
  const response = await axios.post<ApiResponse<SessionTokens>>('/api/v1/auth/refresh', { refreshToken }, { timeout: 10_000 })
  sessionStorage.setItem(ACCESS_TOKEN_KEY, response.data.data.accessToken)
  sessionStorage.setItem(REFRESH_TOKEN_KEY, response.data.data.refreshToken)
  return response.data.data
}

http.interceptors.request.use((config) => {
  const token = sessionStorage.getItem(ACCESS_TOKEN_KEY)
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(undefined, async (error: AxiosError) => {
  const config = error.config as RetryableRequestConfig | undefined
  const isAuthRequest = config?.url?.startsWith('/auth/')
  const refreshToken = sessionStorage.getItem(REFRESH_TOKEN_KEY)

  if (error.response?.status !== 401 || !config || config._retry || isAuthRequest || !refreshToken) {
    return Promise.reject(error)
  }

  config._retry = true
  try {
    refreshInFlight ??= refreshAccessToken(refreshToken).finally(() => { refreshInFlight = null })
    const tokens = await refreshInFlight
    config.headers.Authorization = `Bearer ${tokens.accessToken}`
    return http(config)
  } catch {
    clearStoredTokens()
    return Promise.reject(error)
  }
})
