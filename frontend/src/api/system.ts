import { http } from './http'

interface ApiResponse<T> {
  data: T
}

export interface SystemRole {
  id: string
  code: string
  name: string
  status: string
  version: string
}

export interface SystemUser {
  id: string
  username: string
  employeeId: string | null
  status: string
  roleIds: string[]
  version: string
}

interface PageResult<T> {
  records: T[]
  total: number
  page: number
  pageSize: number
}

export async function fetchSystemRoles() {
  const response = await http.get<ApiResponse<SystemRole[]>>('/system/roles')
  return response.data.data
}

export async function fetchSystemUsers(page = 1, pageSize = 20) {
  const response = await http.get<ApiResponse<PageResult<SystemUser>>>('/system/users', { params: { page, pageSize } })
  return response.data.data
}

export async function replaceUserRoles(userId: string, roleIds: string[], version: string) {
  const response = await http.put<ApiResponse<SystemUser>>(`/system/users/${userId}/roles`, { roleIds, version })
  return response.data.data
}
