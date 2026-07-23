import { http } from './http'
import type { DepartmentNode } from '@/types/organization'

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

export interface SystemRoleDetail {
  id: string
  code: string
  name: string
  status: string
  version: string
  dataScopeType: string
  menuIds: string[]
  departmentIds: string[]
}

export interface SystemMenu {
  id: string
  parentId: string | null
  name: string
  permissionCode: string | null
  menuType: string
  routePath: string | null
  status: string
}

export interface CreateSystemRolePayload {
  code: string
  name: string
  status: string
  dataScopeType: string
  menuIds: string[]
  departmentIds: string[]
}

export interface UpdateSystemRolePayload {
  name: string
  status: string
  dataScopeType: string
  menuIds: string[]
  departmentIds: string[]
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

export async function fetchSystemRole(roleId: string) {
  const response = await http.get<ApiResponse<SystemRoleDetail>>(`/system/roles/${roleId}`)
  return response.data.data
}

export async function createSystemRole(payload: CreateSystemRolePayload) {
  const response = await http.post<ApiResponse<SystemRoleDetail>>('/system/roles', payload)
  return response.data.data
}

export async function updateSystemRole(roleId: string, payload: UpdateSystemRolePayload) {
  const response = await http.put<ApiResponse<SystemRoleDetail>>(`/system/roles/${roleId}`, payload)
  return response.data.data
}

export async function fetchSystemMenus() {
  const response = await http.get<ApiResponse<SystemMenu[]>>('/system/menus')
  return response.data.data
}

export async function fetchSystemDepartments() {
  const response = await http.get<ApiResponse<DepartmentNode[]>>('/system/departments')
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