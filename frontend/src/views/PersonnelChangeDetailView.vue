<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageFrame from '@/components/common/PageFrame.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import PersonnelChangeDiff from '@/components/personnel/PersonnelChangeDiff.vue'
import PersonnelChangeEditorDrawer from '@/components/personnel/PersonnelChangeEditorDrawer.vue'
import ExitHandoverList from '@/components/personnel/ExitHandoverList.vue'
import { fetchDepartmentTree, fetchEmployeeOptions, fetchPositions, fetchRanks } from '@/api/organization'
import {
  confirmExitHandoverItem,
  createExitHandoverItem,
  effectivePersonnelChange,
  fetchPersonnelChange,
  submitPersonnelChange,
  updatePersonnelChange,
  withdrawPersonnelChange,
} from '@/api/personnel'
import type { DepartmentNode, EmployeeOption, Position, Rank } from '@/types/organization'
import type { CreateExitHandoverItemPayload, PersonnelChangeDetail, PersonnelChangeEditorPayload, UpdatePersonnelChangePayload } from '@/types/personnel'

const route = useRoute()
const router = useRouter()

const detail = ref<PersonnelChangeDetail>()
const departments = ref<DepartmentNode[]>([])
const positions = ref<Position[]>([])
const ranks = ref<Rank[]>([])
const employees = ref<EmployeeOption[]>([])
const loading = ref(false)
const saving = ref(false)
const editorOpen = ref(false)

function errorMessage(error: unknown) {
  const message = (error as { response?: { data?: { message?: unknown } } })?.response?.data?.message
  return typeof message === 'string' ? message : '请求失败，请稍后重试'
}

function flattenDepartments(nodes: DepartmentNode[]): DepartmentNode[] {
  return nodes.flatMap((node) => [node, ...flattenDepartments(node.children)])
}

const departmentsById = computed(() => Object.fromEntries(flattenDepartments(departments.value).map((item) => [item.id, item.name])))
const positionsById = computed(() => Object.fromEntries(positions.value.map((item) => [item.id, item.name])))
const ranksById = computed(() => Object.fromEntries(ranks.value.map((item) => [item.id, item.name])))
const employeesById = computed(() => Object.fromEntries(employees.value.map((item) => [item.id, item.name])))

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

async function loadDetail() {
  const id = route.params.id as string
  if (!id) return
  loading.value = true
  try {
    detail.value = await fetchPersonnelChange(id)
  } catch (error) {
    ElMessage.error(errorMessage(error))
  } finally {
    loading.value = false
  }
}

async function submitChange() {
  if (!detail.value) return
  saving.value = true
  try {
    detail.value = await submitPersonnelChange(detail.value.id, { version: detail.value.version })
    ElMessage.success('异动单已提交')
  } catch (error) {
    ElMessage.error(errorMessage(error))
  } finally {
    saving.value = false
  }
}

async function withdrawChange() {
  if (!detail.value) return
  const confirmed = await ElMessageBox.confirm('撤回后单据会回到草稿状态。', '撤回异动单', { type: 'warning' }).catch(() => null)
  if (!confirmed) return
  saving.value = true
  try {
    detail.value = await withdrawPersonnelChange(detail.value.id, { version: detail.value.version })
    ElMessage.success('异动单已撤回')
  } catch (error) {
    ElMessage.error(errorMessage(error))
  } finally {
    saving.value = false
  }
}

async function effectiveChange() {
  if (!detail.value) return
  saving.value = true
  try {
    detail.value = await effectivePersonnelChange(detail.value.id, { version: detail.value.version })
    ElMessage.success('异动单已生效')
  } catch (error) {
    ElMessage.error(errorMessage(error))
  } finally {
    saving.value = false
  }
}

async function saveDraft(payload: PersonnelChangeEditorPayload | UpdatePersonnelChangePayload) {
  if (!detail.value) return
  saving.value = true
  try {
    detail.value = await updatePersonnelChange(detail.value.id, payload as UpdatePersonnelChangePayload)
    editorOpen.value = false
    ElMessage.success('异动草稿已更新')
  } catch (error) {
    ElMessage.error(errorMessage(error))
  } finally {
    saving.value = false
  }
}

async function addHandoverItem(payload: CreateExitHandoverItemPayload) {
  if (!detail.value) return
  saving.value = true
  try {
    detail.value = await createExitHandoverItem(detail.value.id, payload)
    ElMessage.success('交接事项已保存')
  } catch (error) {
    ElMessage.error(errorMessage(error))
  } finally {
    saving.value = false
  }
}

async function confirmHandoverItem(itemId: string, version: string, remark?: string) {
  if (!detail.value) return
  saving.value = true
  try {
    detail.value = await confirmExitHandoverItem(detail.value.id, itemId, { version, remark })
    ElMessage.success('交接事项已确认')
  } catch (error) {
    ElMessage.error(errorMessage(error))
  } finally {
    saving.value = false
  }
}

watch(() => route.params.id, () => {
  void loadDetail()
}, { immediate: true })

onMounted(async () => {
  try {
    await loadReferenceData()
  } catch (error) {
    ElMessage.error(errorMessage(error))
  }
})
</script>

<template>
  <PageFrame title="异动详情" description="查看异动前后差异、交接事项和当前处理状态。">
    <template #actions>
      <el-button @click="router.push('/personnel/changes')">返回列表</el-button>
      <el-button v-if="detail?.canEdit" @click="editorOpen = true">编辑草稿</el-button>
      <el-button v-if="detail?.canSubmit" type="primary" :loading="saving" @click="submitChange">提交审批</el-button>
      <el-button v-if="detail?.canWithdraw" type="warning" :loading="saving" @click="withdrawChange">撤回</el-button>
      <el-button v-if="detail?.canExecute" type="success" :loading="saving" @click="effectiveChange">执行生效</el-button>
    </template>
    <template v-if="detail">
      <div class="metric-grid">
        <article class="metric-item"><span>异动单号</span><strong>{{ detail.changeNo }}</strong><small>状态：{{ detail.status }}</small></article>
        <article class="metric-item"><span>员工</span><strong>{{ detail.afterSnapshot.name || detail.employeeId || '-' }}</strong><small>类型：{{ detail.changeType }}</small></article>
        <article class="metric-item"><span>生效日期</span><strong>{{ detail.effectiveDate }}</strong><small>申请日期：{{ detail.applicationDate }}</small></article>
      </div>
      <section class="dashboard-section">
        <div class="section-heading"><h2>申请信息</h2></div>
        <div class="detail-copy">
          <p>{{ detail.reason }}</p>
          <small>创建时间：{{ detail.createdTime }}，流程实例：{{ detail.workflowInstanceId || '未提交' }}</small>
        </div>
      </section>
      <section class="dashboard-section">
        <div class="section-heading"><h2>前后对比</h2></div>
        <div class="detail-table-wrap">
          <PersonnelChangeDiff
            :before-snapshot="detail.beforeSnapshot"
            :after-snapshot="detail.afterSnapshot"
            :departments-by-id="departmentsById"
            :positions-by-id="positionsById"
            :ranks-by-id="ranksById"
            :employees-by-id="employeesById"
          />
        </div>
      </section>
      <ExitHandoverList
        v-if="detail.changeType === 'TERMINATION'"
        :items="detail.handoverItems"
        :employees-by-id="employeesById"
        :editable="detail.canMaintainHandover"
        :saving="saving"
        @add="addHandoverItem"
        @confirm="confirmHandoverItem"
      />
    </template>
    <EmptyState v-else-if="!loading" title="异动单不存在" description="请返回列表重新选择一条记录。" />
    <PersonnelChangeEditorDrawer
      v-model="editorOpen"
      :detail="detail"
      :employees="employees"
      :departments="departments"
      :positions="positions"
      :ranks="ranks"
      :saving="saving"
      @submit="saveDraft"
    />
  </PageFrame>
</template>

<style scoped>
.detail-copy {
  padding: 18px;
  color: #435064;
}

.detail-copy p {
  margin: 0 0 8px;
  line-height: 1.7;
}

.detail-copy small {
  color: #7a8796;
}

.detail-table-wrap {
  padding: 18px;
}
</style>
