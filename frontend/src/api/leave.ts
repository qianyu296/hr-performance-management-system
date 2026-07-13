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
  workflowInstanceId: string | null
  version: number
}

export interface CreateLeaveRequestPayload {
  leaveTypeId: string
  startTime: string
  endTime: string
  reason: string
}

export interface WorkCalendarDay {
  id?: string
  workDate: string
  workday: boolean
  workHours: number
  holidayName?: string | null
  version?: string
}

export interface WorkCalendar {
  id: string
  calendarYear: number
  name: string
  timeZone: string
  status: 'ACTIVE' | 'INACTIVE'
  version: string
  days: WorkCalendarDay[]
}

export interface WorkCalendarPayload {
  calendarYear?: number
  name: string
  timeZone: string
  status: 'ACTIVE' | 'INACTIVE'
  days: Array<{ workDate: string; workday: boolean; workHours: number; holidayName?: string }>
  version?: string
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

export async function fetchWorkCalendar(year: number) {
  const response = await http.get<ApiResponse<WorkCalendar>>('/work-calendars', { params: { year } })
  return response.data.data
}

export async function createWorkCalendar(payload: WorkCalendarPayload) {
  const response = await http.post<ApiResponse<WorkCalendar>>('/work-calendars', payload)
  return response.data.data
}

export async function updateWorkCalendar(id: string, payload: WorkCalendarPayload) {
  const response = await http.put<ApiResponse<WorkCalendar>>(`/work-calendars/${id}`, payload)
  return response.data.data
}
