<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Setting } from '@element-plus/icons-vue'
import PageFrame from '@/components/common/PageFrame.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import DepartmentTreePanel from '@/components/organization/DepartmentTreePanel.vue'
import EmployeeEditorDrawer from '@/components/organization/EmployeeEditorDrawer.vue'
import PositionRankDialog from '@/components/organization/PositionRankDialog.vue'
import { createPosition, createRank, fetchDepartmentTree, fetchEmployee, fetchEmployees, fetchPositions, fetchRanks, updateEmployee, updatePosition, updateRank } from '@/api/organization'
import { fetchEmployeeHistory } from '@/api/personnel'
import type { CreateEmployeePayload, DepartmentNode, EmployeeDetail, EmployeeListItem, Position, Rank, UpdateEmployeePayload } from '@/types/organization'
import type { EmployeeHistoryItem } from '@/types/personnel'

const departments = ref<DepartmentNode[]>([])
const positions = ref<Position[]>([])
const ranks = ref<Rank[]>([])
const employees = ref<EmployeeListItem[]>([])
const employeeHistory = ref<EmployeeHistoryItem[]>([])
const total = ref(0)
const loading = ref(false)
const treeLoading = ref(false)
const historyLoading = ref(false)
const saving = ref(false)
const drawerOpen = ref(false)
const masterDataOpen = ref(false)
const currentEmployee = ref<EmployeeDetail>()
const router = useRouter()
const query = reactive({ page: 1, pageSize: 20, keyword: '', departmentId: undefined as string | undefined, employmentStatus: '' })

function errorMessage(error: any) {
  return error?.response?.data?.message || '请求失败，请稍后重试'
}

async function loadReferenceData() {
  treeLoading.value = true
  try { [departments.value, positions.value, ranks.value] = await Promise.all([fetchDepartmentTree(), fetchPositions(), fetchRanks()]) }
  catch (error) { ElMessage.error(errorMessage(error)) }
  finally { treeLoading.value = false }
}

async function loadEmployees() {
  loading.value = true
  try {
    const page = await fetchEmployees({ page: query.page, pageSize: query.pageSize, keyword: query.keyword || undefined, departmentId: query.departmentId, employmentStatus: query.employmentStatus || undefined })
    employees.value = page.records; total.value = page.total
  } catch (error) { ElMessage.error(errorMessage(error)) }
  finally { loading.value = false }
}

function chooseDepartment(id?: string) { query.departmentId = id; query.page = 1; loadEmployees() }
function openOnboard() { router.push({ path: '/personnel/changes', query: { mode: 'create', type: 'ONBOARD' } }) }
function openPersonnelChange(employeeId: string, type = 'TRANSFER') { drawerOpen.value = false; router.push({ path: '/personnel/changes', query: { mode: 'create', type, employeeId } }) }
async function openEdit(id: string) {
  historyLoading.value = true
  try {
    const [employee, history] = await Promise.all([fetchEmployee(id), fetchEmployeeHistory(id)])
    currentEmployee.value = employee
    employeeHistory.value = history
    drawerOpen.value = true
  } catch (error) {
    ElMessage.error(errorMessage(error))
  } finally {
    historyLoading.value = false
  }
}

async function saveEmployee(payload: CreateEmployeePayload | UpdateEmployeePayload) {
  saving.value = true
  try {
    if (!currentEmployee.value) return
    await updateEmployee(currentEmployee.value.id, payload as UpdateEmployeePayload)
    ElMessage.success('员工档案已保存')
    drawerOpen.value = false
    await loadEmployees()
  } catch (error: any) {
    if (error?.response?.data?.code === 'VERSION_CONFLICT') ElMessage.warning('档案已被其他人更新，请关闭后重新打开')
    else ElMessage.error(errorMessage(error))
  } finally { saving.value = false }
}

async function savePosition(payload: any, id?: string) {
  saving.value = true
  try { id ? await updatePosition(id, payload) : await createPosition(payload); positions.value = await fetchPositions(); ElMessage.success('岗位已保存') }
  catch (error) { ElMessage.error(errorMessage(error)) } finally { saving.value = false }
}

async function saveRank(payload: any, id?: string) {
  saving.value = true
  try { id ? await updateRank(id, payload) : await createRank(payload); ranks.value = await fetchRanks(); ElMessage.success('职级已保存') }
  catch (error) { ElMessage.error(errorMessage(error)) } finally { saving.value = false }
}

onMounted(async () => { await loadReferenceData(); await loadEmployees() })
</script>

<template>
  <PageFrame title="员工档案" description="维护员工普通资料并查看当前任职与履历。任职变化统一通过人事异动流程处理。">
    <template #actions>
      <el-button :icon="Setting" @click="masterDataOpen = true">岗位与职级</el-button>
      <el-button type="primary" :icon="Plus" @click="openOnboard">发起入职</el-button>
    </template>
    <template #filters>
      <el-input v-model="query.keyword" placeholder="搜索姓名或工号" clearable @keyup.enter="query.page = 1; loadEmployees()" />
      <el-select v-model="query.employmentStatus" placeholder="全部状态" clearable @change="query.page = 1; loadEmployees()"><el-option label="试用" value="PROBATION" /><el-option label="正式" value="FORMAL" /><el-option label="停职" value="SUSPENDED" /><el-option label="离职" value="TERMINATED" /></el-select>
      <el-button @click="query.page = 1; loadEmployees()">查询</el-button>
    </template>
    <div class="organization-directory">
      <DepartmentTreePanel :departments="departments" :selected-id="query.departmentId" :loading="treeLoading" @select="chooseDepartment" />
      <section class="employee-table-panel" aria-label="员工目录">
        <el-table v-loading="loading" :data="employees" class="data-table" height="560">
          <el-table-column prop="employeeNo" label="工号" width="130" />
          <el-table-column prop="name" label="姓名" min-width="130" />
          <el-table-column prop="departmentName" label="部门" min-width="150" />
          <el-table-column prop="positionName" label="岗位" min-width="150" />
          <el-table-column prop="rankName" label="职级" width="100" />
          <el-table-column label="状态" width="100"><template #default="scope"><el-tag size="small" :type="scope.row.employmentStatus === 'FORMAL' ? 'success' : scope.row.employmentStatus === 'PROBATION' ? 'warning' : 'info'">{{ scope.row.employmentStatus }}</el-tag></template></el-table-column>
          <el-table-column label="操作" width="168" fixed="right">
            <template #default="scope">
              <el-button link type="primary" class="employee-record-action" @click="openEdit(scope.row.id)">档案</el-button>
              <el-button link class="employee-record-action" @click="openPersonnelChange(scope.row.id)">异动</el-button>
            </template>
          </el-table-column>
        </el-table>
        <EmptyState v-if="!loading && employees.length === 0" title="暂无员工记录" description="调整筛选条件，或创建第一份员工档案。" />
        <div class="table-pagination"><el-pagination v-model:current-page="query.page" v-model:page-size="query.pageSize" :total="total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="loadEmployees" /></div>
      </section>
    </div>
    <EmployeeEditorDrawer
      v-model="drawerOpen"
      :employee="currentEmployee"
      :departments="departments"
      :positions="positions"
      :ranks="ranks"
      :history="employeeHistory"
      :history-loading="historyLoading"
      :saving="saving"
      @submit="saveEmployee"
      @start-personnel-change="openPersonnelChange"
    />
    <PositionRankDialog v-model="masterDataOpen" :positions="positions" :ranks="ranks" :saving="saving" @save-position="savePosition" @save-rank="saveRank" />
  </PageFrame>
</template>

<style scoped>
.employee-record-action {
  min-width: 72px;
  white-space: nowrap;
}
</style>
