export interface DepartmentNode {
  id: string
  code: string
  name: string
  parentId: string | null
  path: string
  status: string
  children: DepartmentNode[]
}

export interface Position {
  id: string; code: string; name: string; jobFamily: string | null; description: string | null
  sortNo: number; status: string; version: string
}

export interface Rank {
  id: string; code: string; name: string; rankOrder: number; status: string; version: string
}

export interface EmployeeListItem {
  id: string; employeeNo: string; name: string; departmentName: string; positionName: string
  rankName: string | null; managerName: string | null; employmentStatus: string; version: string
}

export interface EmployeeDetail extends EmployeeListItem {
  gender: string | null; departmentId: string; positionId: string; rankId: string | null
  managerEmployeeId: string | null; hireDate: string; probationStartDate: string | null; probationEndDate: string | null
}

export interface EmployeePage { records: EmployeeListItem[]; total: number; page: number; pageSize: number }
export interface EmployeeQuery { page: number; pageSize: number; keyword?: string; departmentId?: string; positionId?: string; employmentStatus?: string }
export interface CreateEmployeePayload { employeeNo: string; name: string; gender?: string; departmentId: string; positionId: string; rankId?: string; managerEmployeeId?: string; employmentStatus: string; hireDate: string; probationStartDate?: string; probationEndDate?: string }
export interface UpdateEmployeePayload { name: string; gender?: string; departmentId: string; positionId: string; rankId?: string; managerEmployeeId?: string; hireDate: string; probationStartDate?: string; probationEndDate?: string; version: string }
export interface CreatePositionPayload { code: string; name: string; jobFamily?: string; description?: string; sortNo: number; status: string }
export type UpdatePositionPayload = Omit<CreatePositionPayload, 'code'> & { version: string }
export interface CreateRankPayload { code: string; name: string; rankOrder: number; status: string }
export type UpdateRankPayload = Omit<CreateRankPayload, 'code'> & { version: string }
