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
export interface ManagedLeaveType extends LeaveTypeOption { minUnitHours: number; annualQuota: number | null; status: 'ACTIVE' | 'INACTIVE'; version: string }
export interface LeaveTypePayload { code?: string; name: string; deductBalance: boolean; annualQuota: number | null; minUnitHours: number; version?: string }

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

export interface LeaveBalance {
  id: string
  employeeId: string
  balanceType: string
  balanceYear: number
  availableHours: number
  frozenHours: number
  version: string
}

export interface LeaveBalanceChange {
  id: string
  balanceType: string
  deltaHours: number
  beforeHours: number
  afterHours: number
  sourceType: string
  reason: string
  createdBy: string | null
  createdTime: string
}

export interface OvertimeRequestItem {
  id: string
  requestNo: string
  startTime: string
  endTime: string
  durationHours: number
  compensationType: 'TIME_OFF' | 'OVERTIME_PAY'
  status: string
  workflowInstanceId: string | null
  version: number
}

export async function fetchLeaveTypes() {
  const response = await http.get<ApiResponse<LeaveTypeOption[]>>('/leave-types')
  return response.data.data
}
export async function fetchManagedLeaveTypes() { const response = await http.get<ApiResponse<ManagedLeaveType[]>>('/leave-types', { params: { includeInactive: true } }); return response.data.data }
export async function createLeaveType(payload: LeaveTypePayload) { const response = await http.post<ApiResponse<ManagedLeaveType>>('/leave-types', payload); return response.data.data }
export async function updateLeaveType(id: string, payload: LeaveTypePayload) { const response = await http.patch<ApiResponse<ManagedLeaveType>>(`/leave-types/${id}`, payload); return response.data.data }
export async function disableLeaveType(id: string, version: string) { const response = await http.post<ApiResponse<ManagedLeaveType>>(`/leave-types/${id}/disable`, { version }); return response.data.data }

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

export async function fetchLeaveBalances(employeeId?: string) {
  const url = employeeId ? `/leave-balances/employees/${employeeId}` : '/leave-balances'
  const response = await http.get<ApiResponse<LeaveBalance[]>>(url)
  return response.data.data
}

export async function fetchLeaveBalanceChanges(id: string) {
  const response = await http.get<ApiResponse<LeaveBalanceChange[]>>(`/leave-balances/${id}/changes`)
  return response.data.data
}

export async function adjustLeaveBalance(id: string, payload: { deltaHours: number; direction: 'INCREASE' | 'DECREASE'; reason: string; version: string }) {
  const response = await http.post<ApiResponse<LeaveBalance>>(`/leave-balances/${id}/adjust`, payload)
  return response.data.data
}

export async function fetchOvertimeRequests() {
  const response = await http.get<ApiResponse<OvertimeRequestItem[]>>('/overtime-requests')
  return response.data.data
}

export async function createOvertimeRequest(payload: { startTime: string; endTime: string; reason: string; compensationType: 'TIME_OFF' | 'OVERTIME_PAY' }) {
  const response = await http.post<ApiResponse<OvertimeRequestItem>>('/overtime-requests', payload)
  return response.data.data
}

export async function submitOvertimeRequest(id: string, version: number) {
  const response = await http.post<ApiResponse<OvertimeRequestItem>>(`/overtime-requests/${id}/submit`, { version })
  return response.data.data
}

export async function cancelOvertimeRequest(id: string, version: number) {
  const response = await http.post<ApiResponse<OvertimeRequestItem>>(`/overtime-requests/${id}/cancel`, { version })
  return response.data.data
}
