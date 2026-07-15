<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageFrame from '@/components/common/PageFrame.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import PersonnelChangeEditorDrawer from '@/components/personnel/PersonnelChangeEditorDrawer.vue'
import { fetchDepartmentTree, fetchEmployeeOptions, fetchPositions, fetchRanks } from '@/api/organization'
import { createPersonnelChange, fetchPersonnelChange, fetchPersonnelChanges, updatePersonnelChange } from '@/api/personnel'
import type { DepartmentNode, EmployeeOption, Position, Rank } from '@/types/organization'
import type { PersonnelChangeDetail, PersonnelChangeEditorPayload, PersonnelChangeListItem, PersonnelChangeType, UpdatePersonnelChangePayload } from '@/types/personnel'

const route = useRoute()
const router = useRouter()

const departments = ref<DepartmentNode[]>([])
const positions = ref<Position[]>([])
const ranks = ref<Rank[]>([])
const employees = ref<EmployeeOption[]>([])
const changes = ref<PersonnelChangeListItem[]>([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const drawerOpen = ref(false)
const editingDetail = ref<PersonnelChangeDetail>()
const initialType = ref<PersonnelChangeType>('TRANSFER')
const initialEmployeeId = ref<string>()
const dateRange = ref<[string, string] | null>(null)
const query = reactive({ page: 1, pageSize: 20, employeeId: '', departmentId: '', changeType: '', status: '' })

function errorMessage(error: unknown) {
  const message = (error as { response?: { data?: { message?: unknown } } })?.response?.data?.message
  return typeof message === 'string' ? message : '请求失败，请稍后重试'
}

function flattenDepartments(nodes: DepartmentNode[]): DepartmentNode[] {
  return nodes.flatMap((node) => [node, ...flattenDepartments(node.children)])
}

async function loadReferenceData() {
  const [departmentTree, positionItems, rankItems, employeePage] = await Promise.all([
    fetchDepartmentTree(),
    fetchPositions(),
    fetchRanks(),
    fetchEmployeeOptions(),
  ])
  departments.value = departmentTree
  positions.value = positionItems
  ranks.value = rankItems
  employees.value = employeePage
}

async function loadChanges() {
  loading.value = true
  try {
    const page = await fetchPersonnelChanges({
      page: query.page,
      pageSize: query.pageSize,
      employeeId: query.employeeId || undefined,
      departmentId: query.departmentId || undefined,
      changeType: query.changeType || undefined,
      status: query.status || undefined,
      fromDate: dateRange.value?.[0],
      toDate: dateRange.value?.[1],
    })
    changes.value = page.records
    total.value = page.total
  } catch (error) {
    ElMessage.error(errorMessage(error))
  } finally {
    loading.value = false
  }
}

function openCreate(type: PersonnelChangeType = 'TRANSFER', employeeId?: string) {
  editingDetail.value = undefined
  initialType.value = type
  initialEmployeeId.value = employeeId
  drawerOpen.value = true
}

async function openEdit(id: string) {
  saving.value = true
  try {
    editingDetail.value = await fetchPersonnelChange(id)
    drawerOpen.value = true
  } catch (error) {
    ElMessage.error(errorMessage(error))
  } finally {
    saving.value = false
  }
}

async function saveChange(payload: PersonnelChangeEditorPayload | UpdatePersonnelChangePayload) {
  saving.value = true
  try {
    const saved = editingDetail.value
      ? await updatePersonnelChange(editingDetail.value.id, payload as UpdatePersonnelChangePayload)
      : await createPersonnelChange(payload as PersonnelChangeEditorPayload)
    drawerOpen.value = false
    ElMessage.success('异动草稿已保存')
    await loadChanges()
    await router.push(`/personnel/changes/${saved.id}`)
  } catch (error) {
    ElMessage.error(errorMessage(error))
  } finally {
    saving.value = false
  }
}

function statusType(status: string) {
  return ({
    DRAFT: 'info',
    IN_PROGRESS: 'warning',
    APPROVED: 'success',
    EFFECTIVE: 'success',
    REJECTED: 'danger',
    WITHDRAWN: '',
  } as Record<string, string>)[status] ?? 'info'
}

async function handleRouteQuery() {
  if (route.query.mode !== 'create') return
  openCreate((route.query.type as PersonnelChangeType | undefined) ?? 'TRANSFER', route.query.employeeId as string | undefined)
  const nextQuery = { ...route.query }
  delete nextQuery.mode
  delete nextQuery.type
  delete nextQuery.employeeId
  await router.replace({ path: route.path, query: nextQuery })
}

watch(() => route.query, () => {
  void handleRouteQuery()
}, { deep: true })

onMounted(async () => {
  try {
    await loadReferenceData()
    await loadChanges()
    await handleRouteQuery()
  } catch (error) {
    ElMessage.error(errorMessage(error))
  }
})
</script>

<template>
  <PageFrame title="人事异动" description="管理入转调离草稿、审批进度和生效状态。">
    <template #actions>
      <el-button type="primary" :icon="Plus" @click="openCreate('TRANSFER')">新建异动</el-button>
    </template>
    <template #filters>
      <el-select v-model="query.departmentId" filterable clearable placeholder="按当前部门筛选">
        <el-option v-for="item in flattenDepartments(departments)" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <el-select v-model="query.employeeId" filterable clearable placeholder="按员工筛选">
        <el-option v-for="item in employees" :key="item.id" :label="`${item.name} · ${item.employeeNo}`" :value="item.id" />
      </el-select>
      <el-select v-model="query.changeType" clearable placeholder="异动类型">
        <el-option label="入职" value="ONBOARD" />
        <el-option label="转正" value="CONFIRM" />
        <el-option label="调动" value="TRANSFER" />
        <el-option label="晋升" value="PROMOTION" />
        <el-option label="降职" value="DEMOTION" />
        <el-option label="停职" value="SUSPEND" />
        <el-option label="离职" value="TERMINATION" />
      </el-select>
      <el-select v-model="query.status" clearable placeholder="单据状态">
        <el-option label="草稿" value="DRAFT" />
        <el-option label="审批中" value="IN_PROGRESS" />
        <el-option label="已批准" value="APPROVED" />
        <el-option label="已生效" value="EFFECTIVE" />
        <el-option label="已驳回" value="REJECTED" />
        <el-option label="已撤回" value="WITHDRAWN" />
      </el-select>
      <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" />
      <el-button @click="query.page = 1; loadChanges()">查询</el-button>
    </template>
    <section class="employee-table-panel">
      <el-table v-loading="loading" :data="changes" class="data-table" height="620">
        <el-table-column prop="changeNo" label="单号" width="170" />
        <el-table-column prop="employeeName" label="员工" min-width="140" />
        <el-table-column prop="changeType" label="类型" width="110" />
        <el-table-column prop="effectiveDate" label="生效日期" width="130" />
        <el-table-column label="状态" width="110">
          <template #default="scope">
            <el-tag size="small" :type="statusType(scope.row.status)">{{ scope.row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="router.push(`/personnel/changes/${scope.row.id}`)">详情</el-button>
            <el-button v-if="scope.row.status === 'DRAFT'" link @click="openEdit(scope.row.id)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
      <EmptyState v-if="!loading && changes.length === 0" title="暂无异动记录" description="可以创建入职、转正、调动、停职或离职草稿。" />
      <div class="table-pagination"><el-pagination v-model:current-page="query.page" v-model:page-size="query.pageSize" :total="total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="loadChanges" /></div>
    </section>
    <PersonnelChangeEditorDrawer
      v-model="drawerOpen"
      :detail="editingDetail"
      :employees="employees"
      :departments="departments"
      :positions="positions"
      :ranks="ranks"
      :saving="saving"
      :initial-type="initialType"
      :initial-employee-id="initialEmployeeId"
      @submit="saveChange"
    />
  </PageFrame>
</template>
