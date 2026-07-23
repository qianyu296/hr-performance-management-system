import { http } from './http'

interface ApiResponse<T> {
  data: T
}

export interface WorkflowTaskItem {
  id: string
  instanceId: string
  businessType: string
  businessId: string
  requestNo: string
  applicantName: string
  leaveTypeName: string
  startTime: string | null
  endTime: string | null
  effectiveDate: string | null
  durationHours: number | null
  status: string
  version: number
}

export interface WorkflowHistoryItem {
  id: string
  taskId: string | null
  nodeNo: number | null
  actorUserId: string
  actorUsername: string
  action: string
  comment: string
  createdTime: string
}

export interface WorkflowInstanceDetail {
  id: string
  businessType: string
  businessId: string
  initiatorUserId: string
  status: string
  currentNodeNo: number | null
  version: string
  history: WorkflowHistoryItem[]
}

export type WorkflowNodeType = 'SPECIFIC_USER' | 'DIRECT_MANAGER' | 'DEPARTMENT_LEADER' | 'HR'

export interface WorkflowTemplateNode {
  nodeNo: string
  nodeType: WorkflowNodeType
  approverRule: Record<string, unknown>
}

export interface WorkflowTemplate {
  id: string
  code: string
  name: string
  businessType: 'LEAVE' | 'OVERTIME' | 'PERSONNEL_CHANGE' | 'PERFORMANCE_APPEAL'
  priority: number
  templateVersion: string
  status: 'ACTIVE' | 'INACTIVE'
  version: string
  departmentIds: string[]
  nodes: WorkflowTemplateNode[]
}

export interface WorkflowTemplatePayload {
  code?: string
  name: string
  businessType: WorkflowTemplate['businessType']
  priority: number
  templateVersion?: number
  status: WorkflowTemplate['status']
  departmentIds: string[]
  nodes: Array<{ nodeNo: number; nodeType: WorkflowNodeType; approverRule: Record<string, unknown> }>
  version?: string
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

export async function returnWorkflowTask(id: string, version: number, comment: string) {
  const response = await http.post<ApiResponse<{ status: string }>>(`/workflow/tasks/${id}/return`, { version, comment })
  return response.data.data
}

export async function transferWorkflowTask(id: string, version: number, comment: string, transferToUserId: number) {
  const response = await http.post<ApiResponse<{ status: string }>>(`/workflow/tasks/${id}/transfer`, { version, comment, transferToUserId })
  return response.data.data
}

export async function fetchWorkflowInstance(id: string) {
  const response = await http.get<ApiResponse<WorkflowInstanceDetail>>(`/workflow/tasks/instances/${id}`)
  return response.data.data
}

export async function withdrawWorkflowInstance(id: string, version: string, comment: string) {
  const response = await http.post<ApiResponse<{ status: string }>>(`/workflow/tasks/instances/${id}/withdraw`, { version, comment })
  return response.data.data
}

export async function fetchWorkflowTemplates() {
  const response = await http.get<ApiResponse<WorkflowTemplate[]>>('/workflow/templates')
  return response.data.data
}

export async function createWorkflowTemplate(payload: WorkflowTemplatePayload) {
  const response = await http.post<ApiResponse<WorkflowTemplate>>('/workflow/templates', payload)
  return response.data.data
}

export async function updateWorkflowTemplate(id: string, payload: WorkflowTemplatePayload) {
  const response = await http.put<ApiResponse<WorkflowTemplate>>(`/workflow/templates/${id}`, payload)
  return response.data.data
}