<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { DepartmentNode, EmployeeDetail, Position, Rank, CreateEmployeePayload, UpdateEmployeePayload } from '@/types/organization'

const props = defineProps<{ modelValue: boolean; employee?: EmployeeDetail; departments: DepartmentNode[]; positions: Position[]; ranks: Rank[]; saving: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean]; submit: [payload: CreateEmployeePayload | UpdateEmployeePayload] }>()
const formRef = ref()
const isEdit = computed(() => Boolean(props.employee))
const form = reactive({ employeeNo: '', name: '', gender: '', departmentId: '', positionId: '', rankId: '', managerEmployeeId: '', employmentStatus: 'PROBATION', hireDate: '', probationStartDate: '', probationEndDate: '', version: '0' })

const flatDepartments = computed(() => {
  const result: DepartmentNode[] = []
  const visit = (nodes: DepartmentNode[]) => nodes.forEach((node) => { result.push(node); visit(node.children) })
  visit(props.departments)
  return result
})

watch(() => [props.modelValue, props.employee] as const, () => {
  const value = props.employee
  Object.assign(form, value ? {
    employeeNo: value.employeeNo, name: value.name, gender: value.gender ?? '', departmentId: value.departmentId,
    positionId: value.positionId, rankId: value.rankId ?? '', managerEmployeeId: value.managerEmployeeId ?? '',
    employmentStatus: value.employmentStatus, hireDate: value.hireDate, probationStartDate: value.probationStartDate ?? '',
    probationEndDate: value.probationEndDate ?? '', version: value.version,
  } : { employeeNo: '', name: '', gender: '', departmentId: '', positionId: '', rankId: '', managerEmployeeId: '', employmentStatus: 'PROBATION', hireDate: '', probationStartDate: '', probationEndDate: '', version: '0' })
}, { immediate: true })

async function submit() {
  await formRef.value?.validate()
  const common = { name: form.name, gender: form.gender || undefined, departmentId: form.departmentId, positionId: form.positionId, rankId: form.rankId || undefined, managerEmployeeId: form.managerEmployeeId || undefined, hireDate: form.hireDate, probationStartDate: form.probationStartDate || undefined, probationEndDate: form.probationEndDate || undefined }
  emit('submit', isEdit.value ? { ...common, version: form.version } : { ...common, employeeNo: form.employeeNo, employmentStatus: form.employmentStatus })
}
</script>

<template>
  <el-drawer :model-value="modelValue" :title="isEdit ? '编辑员工档案' : '新建员工档案'" size="min(520px, 92vw)" @close="emit('update:modelValue', false)">
    <el-form ref="formRef" :model="form" label-position="top" class="employee-form">
      <div class="form-grid">
        <el-form-item label="工号" prop="employeeNo" :rules="[{ required: true, message: '请输入工号' }]">
          <el-input v-model="form.employeeNo" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="姓名" prop="name" :rules="[{ required: true, message: '请输入姓名' }]">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="性别"><el-select v-model="form.gender" clearable><el-option label="男" value="MALE" /><el-option label="女" value="FEMALE" /></el-select></el-form-item>
        <el-form-item label="在职状态"><el-select v-model="form.employmentStatus" :disabled="isEdit"><el-option label="试用" value="PROBATION" /><el-option label="正式" value="FORMAL" /></el-select></el-form-item>
        <el-form-item label="部门" prop="departmentId" :rules="[{ required: true, message: '请选择部门' }]">
          <el-select v-model="form.departmentId" filterable><el-option v-for="item in flatDepartments" :key="item.id" :label="item.name" :value="item.id" /></el-select>
        </el-form-item>
        <el-form-item label="岗位" prop="positionId" :rules="[{ required: true, message: '请选择岗位' }]">
          <el-select v-model="form.positionId" filterable><el-option v-for="item in positions" :key="item.id" :label="item.name" :value="item.id" /></el-select>
        </el-form-item>
        <el-form-item label="职级"><el-select v-model="form.rankId" clearable><el-option v-for="item in ranks" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item>
        <el-form-item label="入职日期" prop="hireDate" :rules="[{ required: true, message: '请选择入职日期' }]">
          <el-date-picker v-model="form.hireDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="试用期开始"><el-date-picker v-model="form.probationStartDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="试用期结束"><el-date-picker v-model="form.probationEndDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
      </div>
    </el-form>
    <template #footer><el-button @click="emit('update:modelValue', false)">取消</el-button><el-button type="primary" :loading="saving" @click="submit">保存</el-button></template>
  </el-drawer>
</template>
