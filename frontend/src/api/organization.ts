import { http } from './http'
import type {
  CreateDepartmentPayload,
  CreateEmployeePayload,
  CreatePositionPayload,
  CreateRankPayload,
  CreatedEmployeeAccount,
  DepartmentNode,
  DisableDepartmentPayload,
  EmployeeOption,
  EmployeeDetail,
  EmployeePage,
  EmployeeQuery,
  MoveDepartmentPayload,
  Position,
  Rank,
  UpdateDepartmentPayload,
  UpdateEmployeePayload,
  UpdatePositionPayload,
  UpdateRankPayload,
} from '@/types/organization'

interface ApiResponse<T> { code: string; message: string; data: T; traceId: string }

const data = <T>(response: { data: ApiResponse<T> }) => response.data.data

export const fetchDepartmentTree = () => http.get<ApiResponse<DepartmentNode[]>>('/departments').then(data)
export const createDepartment = (payload: CreateDepartmentPayload) => http.post<ApiResponse<DepartmentNode>>('/departments', payload).then(data)
export const updateDepartment = (id: string, payload: UpdateDepartmentPayload) => http.patch<ApiResponse<DepartmentNode>>(`/departments/${id}`, payload).then(data)
export const moveDepartment = (id: string, payload: MoveDepartmentPayload) => http.post<ApiResponse<DepartmentNode>>(`/departments/${id}/move`, payload).then(data)
export const disableDepartment = (id: string, payload: DisableDepartmentPayload) => http.post<ApiResponse<DepartmentNode>>(`/departments/${id}/disable`, payload).then(data)
export const fetchPositions = () => http.get<ApiResponse<Position[]>>('/positions').then(data)
export const createPosition = (payload: CreatePositionPayload) => http.post<ApiResponse<Position>>('/positions', payload).then(data)
export const updatePosition = (id: string, payload: UpdatePositionPayload) => http.patch<ApiResponse<Position>>(`/positions/${id}`, payload).then(data)
export const fetchRanks = () => http.get<ApiResponse<Rank[]>>('/ranks').then(data)
export const createRank = (payload: CreateRankPayload) => http.post<ApiResponse<Rank>>('/ranks', payload).then(data)
export const updateRank = (id: string, payload: UpdateRankPayload) => http.patch<ApiResponse<Rank>>(`/ranks/${id}`, payload).then(data)
export const fetchEmployees = (params: EmployeeQuery) => http.get<ApiResponse<EmployeePage>>('/employees', { params }).then(data)
export const fetchEmployee = (id: string) => http.get<ApiResponse<EmployeeDetail>>(`/employees/${id}`).then(data)
export const createEmployee = (payload: CreateEmployeePayload) => http.post<ApiResponse<CreatedEmployeeAccount>>('/employees', payload).then(data)
export const updateEmployee = (id: string, payload: UpdateEmployeePayload) => http.patch<ApiResponse<EmployeeDetail>>(`/employees/${id}`, payload).then(data)

export async function fetchEmployeeOptions() {
  const pageSize = 100
  const maxPages = 1000
  let page = 1
  const records: EmployeeOption[] = []

  while (page <= maxPages) {
    const result = await fetchEmployees({ page, pageSize })
    if (result.records.length === 0) {
      break
    }

    records.push(
      ...result.records.map((item) => ({
        id: item.id,
        employeeNo: item.employeeNo,
        name: item.name,
        departmentName: item.departmentName,
        positionName: item.positionName,
      })),
    )

    if (records.length >= result.total || result.records.length < pageSize) {
      break
    }

    page += 1
  }

  return records
}
