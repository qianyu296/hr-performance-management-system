import { http } from './http'

interface ApiResponse<T> {
  data: T
}

export interface WorkflowTaskItem {
  id: string
  businessType: string
  businessId: string
  requestNo: string
  applicantName: string
  leaveTypeName: string
  startTime: string
  endTime: string
  durationHours: number
  status: string
  version: number
}

export async function fetchWorkflowTasks() {
  const response = await http.get<ApiResponse<WorkflowTaskItem[]>>('/workflow/tasks')
  return response.data.data
}

export async function approveWorkflowTask(id: string, version: number, comment: string) {
  const response = await http.post<ApiResponse<{ status: string }>>(`/workflow/tasks/${id}/approve`, { version, comment })
  return response.data.data
}

export async function rejectWorkflowTask(id: string, version: number, comment: string) {
  const response = await http.post<ApiResponse<{ status: string }>>(`/workflow/tasks/${id}/reject`, { version, comment })
  return response.data.data
}
