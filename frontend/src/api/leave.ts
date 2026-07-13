import { http } from './http'

interface ApiResponse<T> {
  data: T
}

export interface LeaveTypeOption {
  id: string
  code: string
  name: string
  deductBalance: boolean
}

export interface LeaveRequestItem {
  id: string
  requestNo: string
  leaveTypeName: string
  startTime: string
  endTime: string
  durationHours: number
  status: string
  version: number
}

export interface CreateLeaveRequestPayload {
  leaveTypeId: string
  startTime: string
  endTime: string
  reason: string
}

export async function fetchLeaveTypes() {
  const response = await http.get<ApiResponse<LeaveTypeOption[]>>('/leave-types')
  return response.data.data
}

export async function fetchLeaveRequests() {
  const response = await http.get<ApiResponse<LeaveRequestItem[]>>('/leave-requests')
  return response.data.data
}

export async function createLeaveRequest(payload: CreateLeaveRequestPayload) {
  const response = await http.post<ApiResponse<{ id: string; status: string; version?: number }>>('/leave-requests', payload)
  return response.data.data
}

export async function submitLeaveRequest(id: string, version: number) {
  const response = await http.post<ApiResponse<{ id: string; status: string }>>(`/leave-requests/${id}/submit`, { version })
  return response.data.data
}

export async function cancelLeaveRequest(id: string, version: number) {
  const response = await http.post<ApiResponse<{ id: string; status: string }>>(`/leave-requests/${id}/cancel`, { version })
  return response.data.data
}
