<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import PageFrame from '@/components/common/PageFrame.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import DepartmentTreePanel from '@/components/organization/DepartmentTreePanel.vue'
import DepartmentEditorDialog from '@/components/organization/DepartmentEditorDialog.vue'
import { createDepartment, disableDepartment, fetchDepartmentTree, fetchEmployeeOptions, moveDepartment, updateDepartment } from '@/api/organization'
import type { CreateDepartmentPayload, DepartmentNode, EmployeeOption, MoveDepartmentPayload, UpdateDepartmentPayload } from '@/types/organization'

type DialogMode = 'create' | 'edit' | 'move' | 'disable'

const departments = ref<DepartmentNode[]>([])
const employees = ref<EmployeeOption[]>([])
const loading = ref(false)
const saving = ref(false)
const selectedId = ref<string>()
const dialogOpen = ref(false)
const dialogMode = ref<DialogMode>('create')
const dialogDepartment = ref<DepartmentNode>()
const dialogParentId = ref<string>()

function errorMessage(error: unknown) {
  const message = (error as { response?: { data?: { message?: unknown } } })?.response?.data?.message
  return typeof message === 'string' ? message : '请求失败，请稍后重试'
}

function errorCode(error: unknown) {
  const code = (error as { response?: { data?: { code?: unknown } } })?.response?.data?.code
  return typeof code === 'string' ? code : undefined
}

function flattenDepartments(nodes: DepartmentNode[]): DepartmentNode[] {
  return nodes.flatMap((node) => [node, ...flattenDepartments(node.children)])
}

const departmentMap = computed(() => Object.fromEntries(flattenDepartments(departments.value).map((item) => [item.id, item])))
const leaderNames = computed(() => Object.fromEntries(employees.value.map((item) => [item.id, item.name])))
const selectedDepartment = computed(() => selectedId.value ? departmentMap.value[selectedId.value] : undefined)

async function loadData() {
  loading.value = true
  try {
    const [departmentTree, employeeOptions] = await Promise.all([
      fetchDepartmentTree(),
      fetchEmployeeOptions(),
    ])
    departments.value = departmentTree
    employees.value = employeeOptions
    if (!selectedId.value && departmentTree.length) {
      selectedId.value = departmentTree[0].id
    }
    if (selectedId.value && !departmentMap.value[selectedId.value]) {
      selectedId.value = departmentTree[0]?.id
    }
  } catch (error) {
    if (errorCode(error) === 'VERSION_CONFLICT') {
      ElMessage.warning('部门已被其他人更新，已刷新当前数据')
      await loadData()
      return
    }
    ElMessage.error(errorMessage(error))
  } finally {
    loading.value = false
  }
}

function openDialog(mode: DialogMode, department?: DepartmentNode, parentId?: string) {
  dialogMode.value = mode
  dialogDepartment.value = department
  dialogParentId.value = parentId
  dialogOpen.value = true
}

async function saveDepartment(payload: CreateDepartmentPayload | UpdateDepartmentPayload | MoveDepartmentPayload | { version: string }) {
  saving.value = true
  try {
    let updated: DepartmentNode
    if (dialogMode.value === 'create') {
      updated = await createDepartment(payload as CreateDepartmentPayload)
    } else if (dialogMode.value === 'edit') {
      updated = await updateDepartment(dialogDepartment.value!.id, payload as UpdateDepartmentPayload)
    } else if (dialogMode.value === 'move') {
      updated = await moveDepartment(dialogDepartment.value!.id, payload as MoveDepartmentPayload)
    } else {
      updated = await disableDepartment(dialogDepartment.value!.id, payload as { version: string })
    }
    ElMessage.success(dialogMode.value === 'disable' ? '部门已停用' : '部门信息已保存')
    dialogOpen.value = false
    selectedId.value = updated.id
    await loadData()
  } catch (error) {
    ElMessage.error(errorMessage(error))
  } finally {
    saving.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <PageFrame title="组织架构" description="维护部门层级、负责人和生效状态。停用前会校验在职员工和活动下级部门。">
    <template #actions>
      <el-button :icon="Refresh" @click="loadData">刷新</el-button>
      <el-button type="primary" :icon="Plus" @click="openDialog('create')">新建顶级部门</el-button>
    </template>
    <div class="organization-directory">
      <DepartmentTreePanel :departments="departments" :selected-id="selectedId" :loading="loading" @select="selectedId = $event" />
      <section class="employee-table-panel department-detail-panel">
        <template v-if="selectedDepartment">
          <div class="section-heading">
            <h2>{{ selectedDepartment.name }}</h2>
            <div class="page-actions">
              <el-button @click="openDialog('create', undefined, selectedDepartment.id)">新建下级部门</el-button>
              <el-button @click="openDialog('edit', selectedDepartment)">编辑</el-button>
              <el-button @click="openDialog('move', selectedDepartment)">移动</el-button>
              <el-button type="danger" plain @click="openDialog('disable', selectedDepartment)">停用</el-button>
            </div>
          </div>
          <div class="department-summary-grid">
            <article class="metric-item">
              <span>部门编码</span>
              <strong>{{ selectedDepartment.code }}</strong>
              <small>用于主数据唯一识别</small>
            </article>
            <article class="metric-item">
              <span>负责人</span>
              <strong>{{ selectedDepartment.leaderEmployeeId ? (leaderNames[selectedDepartment.leaderEmployeeId] ?? selectedDepartment.leaderEmployeeId) : '-' }}</strong>
              <small>变更负责人需具备组织写权限</small>
            </article>
            <article class="metric-item">
              <span>当前状态</span>
              <strong>{{ selectedDepartment.status === 'ACTIVE' ? '启用' : '停用' }}</strong>
              <small>停用后不再允许继续分配在职员工</small>
            </article>
          </div>
          <el-descriptions :column="2" border class="department-descriptions">
            <el-descriptions-item label="上级部门">{{ selectedDepartment.parentId ? (departmentMap[selectedDepartment.parentId]?.name ?? selectedDepartment.parentId) : '顶级部门' }}</el-descriptions-item>
            <el-descriptions-item label="排序">{{ selectedDepartment.sortNo }}</el-descriptions-item>
            <el-descriptions-item label="生效日期">{{ selectedDepartment.effectiveDate }}</el-descriptions-item>
            <el-descriptions-item label="路径">{{ selectedDepartment.path }}</el-descriptions-item>
            <el-descriptions-item label="下级部门数">{{ selectedDepartment.children.length }}</el-descriptions-item>
            <el-descriptions-item label="版本">{{ selectedDepartment.version }}</el-descriptions-item>
          </el-descriptions>
          <section class="dashboard-section">
            <div class="section-heading"><h2>直属下级</h2></div>
            <el-table :data="selectedDepartment.children" class="data-table" size="small">
              <el-table-column prop="code" label="编码" width="140" />
              <el-table-column prop="name" label="名称" min-width="180" />
              <el-table-column label="负责人" min-width="140">
                <template #default="scope">{{ scope.row.leaderEmployeeId ? (leaderNames[scope.row.leaderEmployeeId] ?? scope.row.leaderEmployeeId) : '-' }}</template>
              </el-table-column>
              <el-table-column label="状态" width="100">
                <template #default="scope"><el-tag size="small" :type="scope.row.status === 'ACTIVE' ? 'success' : 'info'">{{ scope.row.status === 'ACTIVE' ? '启用' : '停用' }}</el-tag></template>
              </el-table-column>
            </el-table>
          </section>
        </template>
        <EmptyState v-else title="请选择部门" description="从左侧组织树中选择一个节点，查看详情并执行维护动作。" />
      </section>
    </div>
    <DepartmentEditorDialog
      v-model="dialogOpen"
      :mode="dialogMode"
      :department="dialogDepartment"
      :departments="departments"
      :employees="employees"
      :saving="saving"
      :parent-id="dialogParentId"
      @save="saveDepartment"
    />
  </PageFrame>
</template>

<style scoped>
.department-detail-panel {
  min-height: 620px;
}

.department-summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  padding: 18px;
}

.department-descriptions {
  padding: 0 18px 18px;
}

@media (max-width: 900px) {
  .department-summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
