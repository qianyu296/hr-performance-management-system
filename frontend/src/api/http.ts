import axios from 'axios'

export const http = axios.create({ baseURL: '/api/v1', timeout: 10_000 })

http.interceptors.request.use((config) => {
  const token = sessionStorage.getItem('accessToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})
