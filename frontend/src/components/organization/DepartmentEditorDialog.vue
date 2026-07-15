<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import type { DepartmentNode, CreateDepartmentPayload, EmployeeOption, MoveDepartmentPayload, UpdateDepartmentPayload } from '@/types/organization'

type Mode = 'create' | 'edit' | 'move' | 'disable'

const props = defineProps<{
  modelValue: boolean
  mode: Mode
  department?: DepartmentNode
  departments: DepartmentNode[]
  employees: EmployeeOption[]
  saving: boolean
  parentId?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  save: [payload: CreateDepartmentPayload | UpdateDepartmentPayload | MoveDepartmentPayload | { version: string }]
}>()

const form = reactive({
  code: '',
  name: '',
  parentId: '',
  leaderEmployeeId: '',
  sortNo: 0,
  status: 'ACTIVE',
  effectiveDate: '',
  version: '0',
})

const title = computed(() => ({
  create: '新建部门',
  edit: '编辑部门',
  move: '移动部门',
  disable: '停用部门',
}[props.mode]))

const availableParents = computed(() => {
  if (!props.department) {
    return props.departments
  }
  const blockedPrefix = props.department.path
  const filterNodes = (nodes: DepartmentNode[]): DepartmentNode[] =>
    nodes
      .filter((node) => node.id !== props.department?.id && !node.path.startsWith(blockedPrefix))
      .map((node) => ({ ...node, children: filterNodes(node.children) }))
  return filterNodes(props.departments)
})

watch(() => [props.modelValue, props.mode, props.department, props.parentId] as const, () => {
  const value = props.department
  Object.assign(form, props.mode === 'create'
    ? {
        code: '',
        name: '',
        parentId: props.parentId ?? '',
        leaderEmployeeId: '',
        sortNo: 0,
        status: 'ACTIVE',
        effectiveDate: new Date().toISOString().slice(0, 10),
        version: '0',
      }
    : value
      ? {
          code: value.code,
          name: value.name,
          parentId: value.parentId ?? '',
          leaderEmployeeId: value.leaderEmployeeId ?? '',
          sortNo: value.sortNo,
          status: value.status,
          effectiveDate: value.effectiveDate,
          version: value.version,
        }
      : {
          code: '',
          name: '',
          parentId: '',
          leaderEmployeeId: '',
          sortNo: 0,
          status: 'ACTIVE',
          effectiveDate: '',
          version: '0',
        })
}, { immediate: true })

function submit() {
  if (props.mode === 'create') {
    emit('save', {
      code: form.code,
      name: form.name,
      parentId: form.parentId || undefined,
      leaderEmployeeId: form.leaderEmployeeId || undefined,
      sortNo: form.sortNo,
      status: form.status,
      effectiveDate: form.effectiveDate,
    })
    return
  }
  if (props.mode === 'edit') {
    emit('save', {
      name: form.name,
      leaderEmployeeId: form.leaderEmployeeId || undefined,
      sortNo: form.sortNo,
      status: form.status,
      effectiveDate: form.effectiveDate,
      version: form.version,
    })
    return
  }
  if (props.mode === 'move') {
    emit('save', { parentId: form.parentId || undefined, version: form.version })
    return
  }
  emit('save', { version: form.version })
}
</script>

<template>
  <el-dialog :model-value="modelValue" :title="title" width="min(560px, 94vw)" @close="emit('update:modelValue', false)">
    <el-form label-position="top" class="department-dialog-form">
      <template v-if="mode === 'create' || mode === 'edit'">
        <div class="form-grid">
          <el-form-item v-if="mode === 'create'" label="部门编码" required>
            <el-input v-model="form.code" />
          </el-form-item>
          <el-form-item :label="mode === 'create' ? '部门名称' : '名称'" required>
            <el-input v-model="form.name" />
          </el-form-item>
          <el-form-item v-if="mode === 'create'" label="上级部门">
            <el-tree-select
              v-model="form.parentId"
              :data="departments"
              check-strictly
              clearable
              node-key="id"
              :props="{ label: 'name', children: 'children' }"
              placeholder="留空表示顶级部门"
              class="form-control-full"
            />
          </el-form-item>
          <el-form-item label="负责人">
            <el-select v-model="form.leaderEmployeeId" filterable clearable>
              <el-option v-for="item in employees" :key="item.id" :label="`${item.name} · ${item.employeeNo}`" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="排序">
            <el-input-number v-model="form.sortNo" :min="0" class="form-control-full" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="form.status">
              <el-option label="启用" value="ACTIVE" />
              <el-option label="停用" value="INACTIVE" />
            </el-select>
          </el-form-item>
          <el-form-item label="生效日期">
            <el-date-picker v-model="form.effectiveDate" type="date" value-format="YYYY-MM-DD" class="form-control-full" />
          </el-form-item>
        </div>
      </template>

      <template v-else-if="mode === 'move'">
        <el-alert type="info" :closable="false" show-icon title="移动后会同步重算当前部门及所有下级部门路径。" class="dialog-alert" />
        <el-form-item label="新的上级部门">
          <el-tree-select
            v-model="form.parentId"
            :data="availableParents"
            clearable
            check-strictly
            node-key="id"
            :props="{ label: 'name', children: 'children' }"
            placeholder="留空表示顶级部门"
            class="form-control-full"
          />
        </el-form-item>
      </template>

      <template v-else>
        <el-alert type="warning" :closable="false" show-icon title="停用后部门仍保留历史记录，但不能继续分配在职员工。" class="dialog-alert" />
        <p class="dialog-copy">确认停用“{{ department?.name }}”吗？如果仍有在职员工或启用中的下级部门，后端会返回阻断原因。</p>
      </template>
    </el-form>
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">{{ mode === 'disable' ? '确认停用' : '保存' }}</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.dialog-alert {
  margin-bottom: 16px;
}

.dialog-copy {
  margin: 0;
  color: #566273;
  line-height: 1.7;
}
</style>
