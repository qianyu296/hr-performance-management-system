import { http } from './http'
import type { CreateEmployeePayload, CreatePositionPayload, CreateRankPayload, DepartmentNode, EmployeeDetail, EmployeePage, EmployeeQuery, Position, Rank, UpdateEmployeePayload, UpdatePositionPayload, UpdateRankPayload } from '@/types/organization'

interface ApiResponse<T> { code: string; message: string; data: T; traceId: string }

const data = <T>(response: { data: ApiResponse<T> }) => response.data.data

export const fetchDepartmentTree = () => http.get<ApiResponse<DepartmentNode[]>>('/departments').then(data)
export const fetchPositions = () => http.get<ApiResponse<Position[]>>('/positions').then(data)
export const createPosition = (payload: CreatePositionPayload) => http.post<ApiResponse<Position>>('/positions', payload).then(data)
export const updatePosition = (id: string, payload: UpdatePositionPayload) => http.patch<ApiResponse<Position>>(`/positions/${id}`, payload).then(data)
export const fetchRanks = () => http.get<ApiResponse<Rank[]>>('/ranks').then(data)
export const createRank = (payload: CreateRankPayload) => http.post<ApiResponse<Rank>>('/ranks', payload).then(data)
export const updateRank = (id: string, payload: UpdateRankPayload) => http.patch<ApiResponse<Rank>>(`/ranks/${id}`, payload).then(data)
export const fetchEmployees = (params: EmployeeQuery) => http.get<ApiResponse<EmployeePage>>('/employees', { params }).then(data)
export const fetchEmployee = (id: string) => http.get<ApiResponse<EmployeeDetail>>(`/employees/${id}`).then(data)
export const createEmployee = (payload: CreateEmployeePayload) => http.post<ApiResponse<EmployeeDetail>>('/employees', payload).then(data)
export const updateEmployee = (id: string, payload: UpdateEmployeePayload) => http.patch<ApiResponse<EmployeeDetail>>(`/employees/${id}`, payload).then(data)
