<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import type { DepartmentNode, EmployeeOption, Position, Rank } from '@/types/organization'
import type { PersonnelChangeDetail, PersonnelChangeEditorPayload, PersonnelChangeType, UpdatePersonnelChangePayload } from '@/types/personnel'

const props = defineProps<{
  modelValue: boolean
  detail?: PersonnelChangeDetail
  employees: EmployeeOption[]
  departments: DepartmentNode[]
  positions: Position[]
  ranks: Rank[]
  saving: boolean
  initialType?: PersonnelChangeType
  initialEmployeeId?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  submit: [payload: PersonnelChangeEditorPayload | UpdatePersonnelChangePayload]
}>()

const isEdit = computed(() => Boolean(props.detail))
const form = reactive({
  employeeId: '',
  changeType: 'TRANSFER' as PersonnelChangeType,
  effectiveDate: '',
  reason: '',
  employeeNo: '',
  name: '',
  gender: '',
  departmentId: '',
  positionId: '',
  rankId: '',
  managerEmployeeId: '',
  employmentStatus: '',
  hireDate: '',
  probationStartDate: '',
  probationEndDate: '',
  terminationDate: '',
  version: '0',
})

const departmentOptions = computed(() => {
  const values: DepartmentNode[] = []
  const visit = (nodes: DepartmentNode[]) => nodes.forEach((node) => {
    values.push(node)
    visit(node.children)
  })
  visit(props.departments)
  return values
})

const onboardMode = computed(() => form.changeType === 'ONBOARD')
const assignmentMode = computed(() => ['ONBOARD', 'TRANSFER', 'PROMOTION', 'DEMOTION'].includes(form.changeType))
const terminationMode = computed(() => form.changeType === 'TERMINATION')

watch(() => [props.modelValue, props.detail, props.initialType, props.initialEmployeeId] as const, () => {
  const snapshot = props.detail?.afterSnapshot
  Object.assign(form, props.detail ? {
    employeeId: props.detail.employeeId ?? '',
    changeType: props.detail.changeType,
    effectiveDate: props.detail.effectiveDate,
    reason: props.detail.reason,
    employeeNo: snapshot?.employeeNo ?? '',
    name: snapshot?.name ?? '',
    gender: snapshot?.gender ?? '',
    departmentId: snapshot?.departmentId ?? '',
    positionId: snapshot?.positionId ?? '',
    rankId: snapshot?.rankId ?? '',
    managerEmployeeId: snapshot?.managerEmployeeId ?? '',
    employmentStatus: snapshot?.employmentStatus ?? '',
    hireDate: snapshot?.hireDate ?? '',
    probationStartDate: snapshot?.probationStartDate ?? '',
    probationEndDate: snapshot?.probationEndDate ?? '',
    terminationDate: snapshot?.terminationDate ?? '',
    version: props.detail.version,
  } : {
    employeeId: props.initialEmployeeId ?? '',
    changeType: props.initialType ?? 'TRANSFER',
    effectiveDate: new Date().toISOString().slice(0, 10),
    reason: '',
    employeeNo: '',
    name: '',
    gender: '',
    departmentId: '',
    positionId: '',
    rankId: '',
    managerEmployeeId: '',
    employmentStatus: props.initialType === 'ONBOARD' ? 'PROBATION' : '',
    hireDate: '',
    probationStartDate: '',
    probationEndDate: '',
    terminationDate: '',
    version: '0',
  })
}, { immediate: true })

function buildAfterAssignment() {
  return {
    employeeNo: onboardMode.value ? form.employeeNo || undefined : undefined,
    name: onboardMode.value ? form.name || undefined : undefined,
    gender: form.gender || undefined,
    departmentId: form.departmentId || undefined,
    positionId: form.positionId || undefined,
    rankId: form.rankId || undefined,
    managerEmployeeId: form.managerEmployeeId || undefined,
    employmentStatus: form.employmentStatus || undefined,
    hireDate: onboardMode.value ? (form.hireDate || undefined) : undefined,
    probationStartDate: form.probationStartDate || undefined,
    probationEndDate: form.probationEndDate || undefined,
    terminationDate: terminationMode.value ? (form.terminationDate || undefined) : undefined,
  }
}

function submit() {
  const payload = {
    employeeId: onboardMode.value ? undefined : (form.employeeId || undefined),
    changeType: form.changeType,
    effectiveDate: form.effectiveDate,
    reason: form.reason,
    afterAssignment: buildAfterAssignment(),
  }
  if (isEdit.value) {
    emit('submit', { ...payload, version: form.version })
    return
  }
  emit('submit', payload)
}
</script>

<template>
  <el-drawer :model-value="modelValue" :title="isEdit ? '编辑异动草稿' : '新建人事异动'" size="min(620px, 96vw)" @close="emit('update:modelValue', false)">
    <el-form label-position="top" class="employee-form">
      <div class="form-grid">
        <el-form-item label="异动类型" required>
          <el-select v-model="form.changeType" :disabled="isEdit">
            <el-option label="入职" value="ONBOARD" />
            <el-option label="转正" value="CONFIRM" />
            <el-option label="调动" value="TRANSFER" />
            <el-option label="晋升" value="PROMOTION" />
            <el-option label="降职" value="DEMOTION" />
            <el-option label="停职" value="SUSPEND" />
            <el-option label="离职" value="TERMINATION" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!onboardMode" label="员工" required>
          <el-select v-model="form.employeeId" filterable :disabled="isEdit">
            <el-option v-for="item in employees" :key="item.id" :label="`${item.name} · ${item.employeeNo}`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="生效日期" required>
          <el-date-picker v-model="form.effectiveDate" type="date" value-format="YYYY-MM-DD" class="form-control-full" />
        </el-form-item>
        <el-form-item label="原因" required>
          <el-input v-model="form.reason" type="textarea" :rows="3" />
        </el-form-item>

        <template v-if="onboardMode">
          <el-form-item label="工号" required><el-input v-model="form.employeeNo" /></el-form-item>
          <el-form-item label="姓名" required><el-input v-model="form.name" /></el-form-item>
          <el-form-item label="性别"><el-select v-model="form.gender" clearable><el-option label="男" value="MALE" /><el-option label="女" value="FEMALE" /></el-select></el-form-item>
          <el-form-item label="在职状态"><el-select v-model="form.employmentStatus"><el-option label="试用" value="PROBATION" /><el-option label="正式" value="FORMAL" /></el-select></el-form-item>
          <el-form-item label="部门" required><el-select v-model="form.departmentId" filterable><el-option v-for="item in departmentOptions" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item>
          <el-form-item label="岗位" required><el-select v-model="form.positionId" filterable><el-option v-for="item in positions" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item>
          <el-form-item label="职级"><el-select v-model="form.rankId" clearable><el-option v-for="item in ranks" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item>
          <el-form-item label="直属主管"><el-select v-model="form.managerEmployeeId" filterable clearable><el-option v-for="item in employees" :key="item.id" :label="`${item.name} · ${item.employeeNo}`" :value="item.id" /></el-select></el-form-item>
          <el-form-item label="入职日期" required><el-date-picker v-model="form.hireDate" type="date" value-format="YYYY-MM-DD" class="form-control-full" /></el-form-item>
          <el-form-item label="试用开始"><el-date-picker v-model="form.probationStartDate" type="date" value-format="YYYY-MM-DD" class="form-control-full" /></el-form-item>
          <el-form-item label="试用结束"><el-date-picker v-model="form.probationEndDate" type="date" value-format="YYYY-MM-DD" class="form-control-full" /></el-form-item>
        </template>

        <template v-else-if="assignmentMode">
          <el-form-item label="目标部门"><el-select v-model="form.departmentId" filterable clearable><el-option v-for="item in departmentOptions" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item>
          <el-form-item label="目标岗位"><el-select v-model="form.positionId" filterable clearable><el-option v-for="item in positions" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item>
          <el-form-item label="目标职级"><el-select v-model="form.rankId" clearable><el-option v-for="item in ranks" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item>
          <el-form-item label="目标主管"><el-select v-model="form.managerEmployeeId" filterable clearable><el-option v-for="item in employees" :key="item.id" :label="`${item.name} · ${item.employeeNo}`" :value="item.id" /></el-select></el-form-item>
          <el-form-item label="异动后状态"><el-select v-model="form.employmentStatus" clearable><el-option label="试用" value="PROBATION" /><el-option label="正式" value="FORMAL" /><el-option label="停职" value="SUSPENDED" /></el-select></el-form-item>
        </template>

        <template v-else-if="form.changeType === 'CONFIRM'">
          <el-form-item label="异动后状态"><el-select v-model="form.employmentStatus"><el-option label="正式" value="FORMAL" /></el-select></el-form-item>
        </template>

        <template v-else-if="form.changeType === 'SUSPEND'">
          <el-form-item label="异动后状态"><el-select v-model="form.employmentStatus"><el-option label="停职" value="SUSPENDED" /></el-select></el-form-item>
        </template>

        <template v-else-if="terminationMode">
          <el-form-item label="异动后状态"><el-select v-model="form.employmentStatus"><el-option label="离职" value="TERMINATED" /></el-select></el-form-item>
          <el-form-item label="离职日期"><el-date-picker v-model="form.terminationDate" type="date" value-format="YYYY-MM-DD" class="form-control-full" /></el-form-item>
        </template>
      </div>
    </el-form>
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">{{ isEdit ? '保存草稿' : '创建草稿' }}</el-button>
    </template>
  </el-drawer>
</template>
