<script setup lang="ts">
import axios from 'axios'
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, View } from '@element-plus/icons-vue'
import PageFrame from '@/components/common/PageFrame.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { approveWorkflowTask, fetchWorkflowInstance, fetchWorkflowTasks, rejectWorkflowTask, returnWorkflowTask, transferWorkflowTask, type WorkflowInstanceDetail, type WorkflowTaskItem } from '@/api/workflow'
import { useAuthStore } from '@/stores/auth'

const loading = ref(false)
const tasks = ref<WorkflowTaskItem[]>([])
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<WorkflowInstanceDetail | null>(null)
const authStore = useAuthStore()

const changeTypeLabelMap: Record<string, string> = {
  ONBOARD: '入职',
  CONFIRM: '转正',
  TRANSFER: '调动',
  PROMOTION: '晋升',
  DEMOTION: '降职',
  SUSPEND: '停职',
  TERMINATION: '离职',
}

const businessTypeLabelMap: Record<string, string> = {
  LEAVE: '请假',
  OVERTIME: '加班',
  PERSONNEL_CHANGE: '人事异动',
}

const statusLabelMap: Record<string, string> = {
  PENDING: '待审批',
  IN_PROGRESS: '审批中',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  RETURNED: '已退回',
  WITHDRAWN: '已撤回',
}

function formatDateTime(value?: string | null) {
  if (!value) return '-'
  return new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

function formatDate(value?: string | null) {
  if (!value) return '-'
  return new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium' }).format(new Date(value))
}

function businessTypeLabel(value: string) {
  return businessTypeLabelMap[value] ?? value
}

function requestTypeLabel(row: WorkflowTaskItem) {
  if (row.businessType === 'PERSONNEL_CHANGE') {
    return changeTypeLabelMap[row.leaveTypeName] ?? row.leaveTypeName
  }
  return row.leaveTypeName
}

function timeColumnLabel(row: WorkflowTaskItem) {
  return row.businessType === 'PERSONNEL_CHANGE' ? '生效日期' : '业务时间'
}

function timeColumnValue(row: WorkflowTaskItem) {
  if (row.businessType === 'PERSONNEL_CHANGE') {
    return formatDate(row.effectiveDate)
  }
  if (!row.startTime || !row.endTime) {
    return '-'
  }
  return `${formatDateTime(row.startTime)} 至 ${formatDateTime(row.endTime)}`
}

function durationValue(row: WorkflowTaskItem) {
  if (row.businessType === 'PERSONNEL_CHANGE') {
    return '-'
  }
  return row.durationHours ?? '-'
}

const timeColumnHeader = computed(() => {
  if (!tasks.value.length) return '业务时间'
  const hasPersonnelChange = tasks.value.some((row) => row.businessType === 'PERSONNEL_CHANGE')
  const hasTimedBusiness = tasks.value.some((row) => row.businessType !== 'PERSONNEL_CHANGE')
  if (hasPersonnelChange && hasTimedBusiness) return '时间/生效日期'
  return hasPersonnelChange ? '生效日期' : '业务时间'
})

async function loadData() {
  loading.value = true
  try {
    tasks.value = await fetchWorkflowTasks()
  } finally {
    loading.value = false
  }
}

async function approve(row: WorkflowTaskItem) {
  await approveWorkflowTask(row.id, row.version, '同意')
  ElMessage.success('审批已通过')
  await loadData()
}

async function reject(row: WorkflowTaskItem) {
  const result = await ElMessageBox.prompt('请输入驳回原因', '驳回申请', {
    confirmButtonText: '驳回',
    cancelButtonText: '取消',
    inputPattern: /.+/,
    inputErrorMessage: '驳回原因不能为空',
    type: 'warning',
  })
  await rejectWorkflowTask(row.id, row.version, result.value)
  ElMessage.success('申请已驳回')
  await loadData()
}

async function returnToInitiator(row: WorkflowTaskItem) {
  const result = await ElMessageBox.prompt('请输入退回修改原因', '退回申请', {
    confirmButtonText: '退回',
    cancelButtonText: '取消',
    inputPattern: /.+/,
    inputErrorMessage: '退回原因不能为空',
    type: 'warning',
  })
  await returnWorkflowTask(row.id, row.version, result.value)
  ElMessage.success('申请已退回发起人修改')
  await loadData()
}

async function transfer(row: WorkflowTaskItem) {
  const target = await ElMessageBox.prompt('请输入接收人的用户 ID', '转交任务', {
    confirmButtonText: '转交',
    cancelButtonText: '取消',
    inputPattern: /^\d+$/,
    inputErrorMessage: '请输入有效的用户 ID',
  })
  const comment = await ElMessageBox.prompt('请输入转交原因', '转交任务', {
    confirmButtonText: '确认转交',
    cancelButtonText: '取消',
    inputPattern: /.+/,
    inputErrorMessage: '转交原因不能为空',
    type: 'warning',
  })
  await transferWorkflowTask(row.id, row.version, comment.value, Number(target.value))
  ElMessage.success('任务已转交')
  await loadData()
}

async function openDetail(row: WorkflowTaskItem) {
  detailVisible.value = true
  detail.value = null
  detailLoading.value = true
  try {
    detail.value = await fetchWorkflowInstance(row.instanceId)
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.status === 404 && row.id !== row.instanceId) {
      try {
        detail.value = await fetchWorkflowInstance(row.id)
        return
      } catch {
        // Fall through to the shared error message below.
      }
    }
    ElMessage.error('无法加载流程详情')
  } finally {
    detailLoading.value = false
  }
}

function actionLabel(action: string) {
  return ({ SUBMIT: '提交申请', RESUBMIT: '重新提交', APPROVE: '审批通过', REJECT: '驳回申请', RETURN: '退回修改', TRANSFER: '转交任务', WITHDRAW: '撤回申请' } as Record<string, string>)[action] ?? action
}

onMounted(loadData)
</script>

<template>
  <PageFrame title="审批中心" description="处理分配给当前用户的审批待办，并查看完整流程历史。">
    <template #actions>
      <el-button :icon="Refresh" @click="loadData">刷新</el-button>
    </template>

    <el-table v-loading="loading" :data="tasks" class="data-table">
      <el-table-column prop="requestNo" label="申请单号" min-width="150" />
      <el-table-column prop="applicantName" label="申请人" width="120" />
      <el-table-column label="业务类型" width="110">
        <template #default="{ row }">{{ businessTypeLabel(row.businessType) }}</template>
      </el-table-column>
      <el-table-column label="申请类型" min-width="130">
        <template #default="{ row }">{{ requestTypeLabel(row) }}</template>
      </el-table-column>
      <el-table-column :label="timeColumnHeader" min-width="260">
        <template #default="{ row }">
          <div class="workflow-task-time">
            <span>{{ timeColumnValue(row) }}</span>
            <small>{{ timeColumnLabel(row) }}</small>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="时长(小时)" width="110">
        <template #default="{ row }">{{ durationValue(row) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">{{ statusLabelMap[row.status] ?? row.status }}</template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-tooltip content="查看流程详情"><el-button text :icon="View" aria-label="查看流程详情" @click="openDetail(row)" /></el-tooltip>
          <el-button text type="primary" @click="approve(row)">通过</el-button>
          <el-button text type="danger" @click="reject(row)">驳回</el-button>
          <el-button text type="warning" @click="returnToInitiator(row)">退回</el-button>
          <el-button v-if="authStore.can('workflow:intervene')" text @click="transfer(row)">转交</el-button>
        </template>
      </el-table-column>
    </el-table>
    <EmptyState v-if="!loading && tasks.length === 0" title="暂无审批待办" description="新的审批任务会出现在这里。" />

    <el-drawer v-model="detailVisible" title="流程详情" size="min(440px, 100vw)">
      <div v-loading="detailLoading" class="workflow-detail">
        <template v-if="detail">
          <el-descriptions :column="1" size="small" border>
            <el-descriptions-item label="流程类型">{{ businessTypeLabel(detail.businessType) }}</el-descriptions-item>
            <el-descriptions-item label="流程状态">{{ statusLabelMap[detail.status] ?? detail.status }}</el-descriptions-item>
            <el-descriptions-item label="当前节点">{{ detail.currentNodeNo ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="业务编号">{{ detail.businessId }}</el-descriptions-item>
          </el-descriptions>
          <h2 class="workflow-history-title">处理历史</h2>
          <el-timeline>
            <el-timeline-item v-for="item in detail.history" :key="item.id" :timestamp="formatDateTime(item.createdTime)" placement="top">
              <strong>{{ actionLabel(item.action) }}</strong>
              <span class="workflow-history-actor">{{ item.actorUsername }}</span>
              <p v-if="item.comment">{{ item.comment }}</p>
            </el-timeline-item>
          </el-timeline>
        </template>
      </div>
    </el-drawer>
  </PageFrame>
</template>

<style scoped>
.workflow-task-time {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.workflow-task-time small {
  color: var(--el-text-color-secondary);
}
</style>
