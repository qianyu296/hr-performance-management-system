import { http } from '@/api/http'

interface ApiResponse<T> { data: T }
export interface DepartmentHeadcount { departmentName: string; headcount: number }
export interface PerformanceLevelDistribution { levelCode: string; count: number }

export const fetchDepartmentHeadcounts = () => http.get<ApiResponse<DepartmentHeadcount[]>>('/reports/headcount-by-department').then((r) => r.data.data)
export const fetchPerformanceLevelDistribution = () => http.get<ApiResponse<PerformanceLevelDistribution[]>>('/reports/performance-level-distribution').then((r) => r.data.data)
