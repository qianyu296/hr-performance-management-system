<script setup lang="ts">
import { computed } from 'vue'
import type { PersonnelAssignmentSnapshot } from '@/types/personnel'

const props = defineProps<{
  beforeSnapshot: PersonnelAssignmentSnapshot | null
  afterSnapshot: PersonnelAssignmentSnapshot
  departmentsById: Record<string, string>
  positionsById: Record<string, string>
  ranksById: Record<string, string>
  employeesById: Record<string, string>
}>()

function renderLookup(value: string | null | undefined, dictionary: Record<string, string>) {
  return value ? (dictionary[value] ?? value) : '-'
}

function renderStatus(value: string | null | undefined) {
  const labels: Record<string, string> = {
    PENDING_ONBOARD: '待入职',
    PROBATION: '试用',
    FORMAL: '正式',
    SUSPENDED: '停职',
    TERMINATED: '离职',
  }
  return value ? (labels[value] ?? value) : '-'
}

const rows = computed(() => [
  { label: '工号', before: props.beforeSnapshot?.employeeNo ?? '-', after: props.afterSnapshot.employeeNo ?? '-' },
  { label: '姓名', before: props.beforeSnapshot?.name ?? '-', after: props.afterSnapshot.name ?? '-' },
  { label: '性别', before: props.beforeSnapshot?.gender ?? '-', after: props.afterSnapshot.gender ?? '-' },
  { label: '部门', before: renderLookup(props.beforeSnapshot?.departmentId, props.departmentsById), after: renderLookup(props.afterSnapshot.departmentId, props.departmentsById) },
  { label: '岗位', before: renderLookup(props.beforeSnapshot?.positionId, props.positionsById), after: renderLookup(props.afterSnapshot.positionId, props.positionsById) },
  { label: '职级', before: renderLookup(props.beforeSnapshot?.rankId, props.ranksById), after: renderLookup(props.afterSnapshot.rankId, props.ranksById) },
  { label: '直属主管', before: renderLookup(props.beforeSnapshot?.managerEmployeeId, props.employeesById), after: renderLookup(props.afterSnapshot.managerEmployeeId, props.employeesById) },
  { label: '在职状态', before: renderStatus(props.beforeSnapshot?.employmentStatus), after: renderStatus(props.afterSnapshot.employmentStatus) },
  { label: '入职日期', before: props.beforeSnapshot?.hireDate ?? '-', after: props.afterSnapshot.hireDate ?? '-' },
  { label: '试用开始', before: props.beforeSnapshot?.probationStartDate ?? '-', after: props.afterSnapshot.probationStartDate ?? '-' },
  { label: '试用结束', before: props.beforeSnapshot?.probationEndDate ?? '-', after: props.afterSnapshot.probationEndDate ?? '-' },
  { label: '离职日期', before: props.beforeSnapshot?.terminationDate ?? '-', after: props.afterSnapshot.terminationDate ?? '-' },
])
</script>

<template>
  <el-table :data="rows" class="data-table" size="small">
    <el-table-column prop="label" label="字段" width="140" />
    <el-table-column prop="before" label="异动前" min-width="200" />
    <el-table-column prop="after" label="异动后" min-width="200">
      <template #default="scope">
        <span :class="{ changed: scope.row.before !== scope.row.after }">{{ scope.row.after }}</span>
      </template>
    </el-table-column>
  </el-table>
</template>

<style scoped>
.changed {
  color: #1f6ac2;
  font-weight: 600;
}
</style>
