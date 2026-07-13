import { http } from './http'

interface ApiResponse<T> { data: T }

export interface AttendanceMonthlySummary {
  id: string
  employeeId: string
  employeeNo: string
  employeeName: string
  departmentId: string
  departmentName: string
  attendanceMonth: string
  leaveHours: number
  overtimeHours: number
  timeOffDeltaHours: number
  pendingRequestCount: number
  generatedBy: string | null
  generatedTime: string
}

export async function fetchAttendanceMonthlySummaries(month: string, filters: { departmentId?: string; employeeId?: string }) {
  const response = await http.get<ApiResponse<AttendanceMonthlySummary[]>>('/attendance/monthly-summaries', { params: { month, ...filters } })
  return response.data.data
}

export async function rebuildAttendanceMonthlySummaries(month: string) {
  const response = await http.post<ApiResponse<{ affectedRows: number }>>('/attendance/monthly-summaries/rebuild', { month })
  return response.data.data
}
