export type PersonnelChangeType = 'ONBOARD' | 'CONFIRM' | 'TRANSFER' | 'PROMOTION' | 'DEMOTION' | 'SUSPEND' | 'TERMINATION'
export type PersonnelChangeStatus = 'DRAFT' | 'IN_PROGRESS' | 'APPROVED' | 'REJECTED' | 'WITHDRAWN' | 'EFFECTIVE'

export interface PersonnelAssignmentSnapshot {
  employeeNo?: string
  name?: string
  gender?: string | null
  departmentId?: string | null
  positionId?: string | null
  rankId?: string | null
  managerEmployeeId?: string | null
  employmentStatus?: string
  hireDate?: string | null
  probationStartDate?: string | null
  probationEndDate?: string | null
  terminationDate?: string | null
}

export interface PersonnelChangeListItem {
  id: string
  changeNo: string
  employeeId: string | null
  employeeName: string | null
  changeType: PersonnelChangeType
  effectiveDate: string
  status: PersonnelChangeStatus
  workflowInstanceId: string | null
  version: string
}

export interface ExitHandoverItem {
  id: string
  itemType: string
  receiverEmployeeId: string | null
  required: boolean
  status: string
  completedTime: string | null
  confirmedBy: string | null
  remark: string | null
  version: string
  canConfirm: boolean
}

export interface PersonnelChangeDetail {
  id: string
  changeNo: string
  employeeId: string | null
  changeType: PersonnelChangeType
  applicationDate: string
  effectiveDate: string
  reason: string
  beforeSnapshot: PersonnelAssignmentSnapshot | null
  afterSnapshot: PersonnelAssignmentSnapshot
  workflowInstanceId: string | null
  status: PersonnelChangeStatus
  createdBy: string | null
  createdTime: string
  version: string
  handoverItems: ExitHandoverItem[]
  canEdit: boolean
  canSubmit: boolean
  canWithdraw: boolean
  canMaintainHandover: boolean
  canExecute: boolean
}

export interface PersonnelChangePage {
  records: PersonnelChangeListItem[]
  total: number
  page: number
  pageSize: number
}

export interface PersonnelChangeQuery {
  page: number
  pageSize: number
  employeeId?: string
  departmentId?: string
  changeType?: string
  status?: string
  fromDate?: string
  toDate?: string
}

export interface PersonnelChangeEditorPayload {
  employeeId?: string
  changeType: PersonnelChangeType
  effectiveDate: string
  reason: string
  afterAssignment: PersonnelAssignmentSnapshot
}

export interface UpdatePersonnelChangePayload extends PersonnelChangeEditorPayload {
  version: string
}

export interface PersonnelChangeActionPayload {
  version: string
}

export interface EmployeeHistoryItem {
  id: string
  employeeId: string
  changeId: string | null
  eventType: string
  effectiveDate: string
  snapshot: PersonnelAssignmentSnapshot
  createdBy: string | null
  createdTime: string
}

export interface CreateExitHandoverItemPayload {
  itemType: string
  receiverEmployeeId?: string
  required?: boolean
  remark?: string
}

export interface ConfirmExitHandoverItemPayload {
  version: string
  remark?: string
}
