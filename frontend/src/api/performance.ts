import { http } from '@/api/http'

interface ApiResponse<T> { data: T }

export interface PerformanceMetric { id: string; code: string; name: string; metricType: string; unit: string | null; scoreMethod: string; scoreConfig: string; description: string | null; status: string; version: number }
export interface SchemeItem { id?: string; metricId: string; weight: number; scoreMethod: string; scoreConfig: string; sortNo: number }
export interface LevelRule { id?: string; levelCode: string; minScore: number; maxScore: number; includeMin: boolean; includeMax: boolean }
export interface SchemeVersion { id: string; schemeId: string; versionNo: number; evaluationStages: string; status: string; version: number; items: SchemeItem[]; levelRules: LevelRule[] }
export interface PerformanceScheme { id: string; code: string; name: string; applicabilityRule: string; status: string; version: number; versions: SchemeVersion[] }
export interface PerformanceCycle { id: string; code: string; name: string; schemeVersionId: string; startDate: string; endDate: string; selfDeadline: string; managerDeadline: string; appealDeadline: string | null; applicabilityRule: string; status: string; version: number }
export const fetchPerformanceMetrics = () => http.get<ApiResponse<PerformanceMetric[]>>('/performance/metrics').then((r) => r.data.data)
export const createPerformanceMetric = (payload: Omit<PerformanceMetric, 'id' | 'version'>) => http.post<ApiResponse<PerformanceMetric>>('/performance/metrics', payload).then((r) => r.data.data)
export const fetchPerformanceSchemes = () => http.get<ApiResponse<PerformanceScheme[]>>('/performance/schemes').then((r) => r.data.data)
export const createPerformanceScheme = (payload: { code: string; name: string; applicabilityRule: string }) => http.post<ApiResponse<PerformanceScheme>>('/performance/schemes', payload).then((r) => r.data.data)
export const createSchemeVersion = (schemeId: string, payload: { evaluationStages: string; items: SchemeItem[]; levelRules: LevelRule[] }) => http.post<ApiResponse<SchemeVersion>>(`/performance/schemes/${schemeId}/versions`, payload).then((r) => r.data.data)
export const enableSchemeVersion = (id: string, version: number) => http.post<ApiResponse<SchemeVersion>>(`/performance/scheme-versions/${id}/enable`, { version }).then((r) => r.data.data)
export const fetchPerformanceCycles = () => http.get<ApiResponse<PerformanceCycle[]>>('/performance/cycles').then((r) => r.data.data)
export const createPerformanceCycle = (payload: Omit<PerformanceCycle, 'id' | 'status' | 'version'>) => http.post<ApiResponse<PerformanceCycle>>('/performance/cycles', payload).then((r) => r.data.data)
export const startPerformanceCycle = (id: string, version: number) => http.post<ApiResponse<PerformanceCycle>>(`/performance/cycles/${id}/start`, { version }).then((r) => r.data.data)
export interface PerformanceTaskScore { stage: string; rawScore: string | null; weightedScore: string | null; comment: string | null }
export interface PerformanceTaskItem { id: string; metricSnapshot: string; weight: number; version: number; scores: PerformanceTaskScore[] }
export interface PerformanceTask { id: string; cycleId: string; cycleName: string; employeeId: string; employeeName: string; managerEmployeeId: string | null; status: string; version: number; items: PerformanceTaskItem[]; totalScore: string | null; levelCode: string | null; publishStatus: string | null }
export const fetchMyPerformanceTasks = () => http.get<ApiResponse<PerformanceTask[]>>('/performance/tasks/mine').then((r) => r.data.data)
export const fetchManagerPerformanceTasks = () => http.get<ApiResponse<PerformanceTask[]>>('/performance/tasks/manager').then((r) => r.data.data)
export const submitSelfAssessment = (id: string, version: number, items: { taskItemId: string; rawScore: number; comment: string }[]) => http.post<ApiResponse<PerformanceTask>>(`/performance/tasks/${id}/self-assessment`, { version, items }).then((r) => r.data.data)
export const submitManagerScore = (id: string, version: number, items: { taskItemId: string; rawScore: number; comment: string }[]) => http.post<ApiResponse<PerformanceTask>>(`/performance/tasks/${id}/manager-score`, { version, items }).then((r) => r.data.data)
export const publishPerformanceCycle = (id: string, version: number) => http.post<ApiResponse<{ publishedCount: number }>>(`/performance/cycles/${id}/publish`, { version }).then((r) => r.data.data)
