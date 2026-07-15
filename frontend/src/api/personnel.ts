import { http } from './http'
import type {
  ConfirmExitHandoverItemPayload,
  CreateExitHandoverItemPayload,
  EmployeeHistoryItem,
  PersonnelChangeActionPayload,
  PersonnelChangeDetail,
  PersonnelChangeEditorPayload,
  PersonnelChangePage,
  PersonnelChangeQuery,
  UpdatePersonnelChangePayload,
} from '@/types/personnel'

interface ApiResponse<T> { code: string; message: string; data: T; traceId: string }

const data = <T>(response: { data: ApiResponse<T> }) => response.data.data

export const fetchPersonnelChanges = (params: PersonnelChangeQuery) =>
  http.get<ApiResponse<PersonnelChangePage>>('/personnel-changes', { params }).then(data)

export const fetchPersonnelChange = (id: string) =>
  http.get<ApiResponse<PersonnelChangeDetail>>(`/personnel-changes/${id}`).then(data)

export const createPersonnelChange = (payload: PersonnelChangeEditorPayload) =>
  http.post<ApiResponse<PersonnelChangeDetail>>('/personnel-changes', payload).then(data)

export const updatePersonnelChange = (id: string, payload: UpdatePersonnelChangePayload) =>
  http.patch<ApiResponse<PersonnelChangeDetail>>(`/personnel-changes/${id}`, payload).then(data)

export const submitPersonnelChange = (id: string, payload: PersonnelChangeActionPayload) =>
  http.post<ApiResponse<PersonnelChangeDetail>>(`/personnel-changes/${id}/submit`, payload).then(data)

export const withdrawPersonnelChange = (id: string, payload: PersonnelChangeActionPayload) =>
  http.post<ApiResponse<PersonnelChangeDetail>>(`/personnel-changes/${id}/withdraw`, payload).then(data)

export const effectivePersonnelChange = (id: string, payload: PersonnelChangeActionPayload) =>
  http.post<ApiResponse<PersonnelChangeDetail>>(`/personnel-changes/${id}/effective`, payload).then(data)

export const fetchEmployeeHistory = (employeeId: string) =>
  http.get<ApiResponse<EmployeeHistoryItem[]>>(`/employees/${employeeId}/history`).then(data)

export const createExitHandoverItem = (changeId: string, payload: CreateExitHandoverItemPayload) =>
  http.post<ApiResponse<PersonnelChangeDetail>>(`/personnel-changes/${changeId}/handover-items`, payload).then(data)

export const confirmExitHandoverItem = (changeId: string, itemId: string, payload: ConfirmExitHandoverItemPayload) =>
  http.post<ApiResponse<PersonnelChangeDetail>>(`/personnel-changes/${changeId}/handover-items/${itemId}/confirm`, payload).then(data)
