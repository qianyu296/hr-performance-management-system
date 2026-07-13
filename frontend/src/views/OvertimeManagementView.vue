<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import PageFrame from '@/components/common/PageFrame.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { cancelOvertimeRequest, createOvertimeRequest, fetchOvertimeRequests, submitOvertimeRequest, type OvertimeRequestItem } from '@/api/leave'
import { fetchWorkflowInstance, withdrawWorkflowInstance } from '@/api/workflow'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const requests = ref<OvertimeRequestItem[]>([])
const statusFilter = ref('')
const form = reactive({ timeRange: null as [Date, Date] | null, reason: '', compensationType: 'TIME_OFF' as 'TIME_OFF' | 'OVERTIME_PAY' })
const rules: FormRules<typeof form> = {
  timeRange: [{ required: true, message: '请选择加班起止时间', trigger: 'change' }],
  reason: [{ required: true, message: '请输入加班事由', trigger: 'blur' }],
  compensationType: [{ required: true, message: '请选择补偿方式', trigger: 'change' }],
}
const filteredRequests = computed(() => requests.value.filter((item) => !statusFilter.value || item.status === statusFilter.value))

function formatDateTime(value: string) { return new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value)) }
function statusLabel(status: string) { return ({ DRAFT: '草稿', IN_PROGRESS: '审批中', APPROVED: '已通过', REJECTED: '已驳回', CANCELLED: '已撤销' } as Record<string, string>)[status] ?? status }
function compensationLabel(type: string) { return type === 'TIME_OFF' ? '调休' : '加班费' }

async function loadData() {
  loading.value = true
  try { requests.value = await fetchOvertimeRequests() } finally { loading.value = false }
}

function openCreateDialog() {
  Object.assign(form, { timeRange: null, reason: '', compensationType: 'TIME_OFF' })
  dialogVisible.value = true
}

async function submitNewRequest() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || !form.timeRange) return
  submitting.value = true
  try {
    const draft = await createOvertimeRequest({ startTime: form.timeRange[0].toISOString(), endTime: form.timeRange[1].toISOString(), reason: form.reason, compensationType: form.compensationType })
    await submitOvertimeRequest(draft.id, 0)
    ElMessage.success('加班申请已提交审批')
    dialogVisible.value = false
    await loadData()
  } finally { submitting.value = false }
}

async function submitDraft(row: OvertimeRequestItem) {
  await submitOvertimeRequest(row.id, row.version)
  ElMessage.success('草稿已提交审批')
  await loadData()
}

async function withdrawPending(row: OvertimeRequestItem) {
  if (!row.workflowInstanceId) return
  await ElMessageBox.confirm('确认撤回这条审批中的加班申请吗？', '撤回加班', { type: 'warning' })
  const instance = await fetchWorkflowInstance(row.workflowInstanceId)
  await withdrawWorkflowInstance(row.workflowInstanceId, instance.version, 'Overtime request withdrawn by applicant')
  ElMessage.success('加班申请已撤回')
  await loadData()
}

async function cancelApproved(row: OvertimeRequestItem) {
  await ElMessageBox.confirm('确认撤销这条已通过的加班申请吗？调休补偿将被相应恢复。', '撤销加班', { type: 'warning' })
  await cancelOvertimeRequest(row.id, row.version)
  ElMessage.success('加班申请已撤销')
  await loadData()
}

onMounted(loadData)
</script>

<template>
  <PageFrame title="加班管理" description="发起加班申请，选择调休或加班费补偿，并跟踪审批和撤销状态。">
    <template #actions><el-button :icon="Refresh" @click="loadData">刷新</el-button><el-button type="primary" :icon="Plus" @click="openCreateDialog">新建加班</el-button></template>
    <template #filters><el-select v-model="statusFilter" placeholder="全部状态" clearable><el-option label="草稿" value="DRAFT" /><el-option label="审批中" value="IN_PROGRESS" /><el-option label="已通过" value="APPROVED" /><el-option label="已驳回" value="REJECTED" /><el-option label="已撤销" value="CANCELLED" /></el-select></template>
    <el-table v-loading="loading" :data="filteredRequests" class="data-table">
      <el-table-column prop="requestNo" label="申请单号" min-width="150" />
      <el-table-column label="加班时间" min-width="260"><template #default="{ row }">{{ formatDateTime(row.startTime) }} 至 {{ formatDateTime(row.endTime) }}</template></el-table-column>
      <el-table-column prop="durationHours" label="时长(小时)" width="110" />
      <el-table-column label="补偿方式" width="120"><template #default="{ row }">{{ compensationLabel(row.compensationType) }}</template></el-table-column>
      <el-table-column label="状态" width="110"><template #default="{ row }"><el-tag>{{ statusLabel(row.status) }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="180" fixed="right"><template #default="{ row }"><el-button v-if="row.status === 'DRAFT'" text type="primary" @click="submitDraft(row)">提交</el-button><el-button v-if="row.status === 'IN_PROGRESS'" text type="warning" @click="withdrawPending(row)">撤回</el-button><el-button v-if="row.status === 'APPROVED'" text type="danger" @click="cancelApproved(row)">撤销</el-button></template></el-table-column>
    </el-table>
    <EmptyState v-if="!loading && filteredRequests.length === 0" title="暂无加班记录" description="点击新建加班发起第一条申请。" />
    <el-dialog v-model="dialogVisible" title="新建加班申请" width="520px"><el-form ref="formRef" :model="form" :rules="rules" label-position="top"><el-form-item label="加班时间" prop="timeRange"><el-date-picker v-model="form.timeRange" type="datetimerange" start-placeholder="开始时间" end-placeholder="结束时间" class="form-control-full" /></el-form-item><el-form-item label="补偿方式" prop="compensationType"><el-radio-group v-model="form.compensationType"><el-radio-button label="TIME_OFF">调休</el-radio-button><el-radio-button label="OVERTIME_PAY">加班费</el-radio-button></el-radio-group></el-form-item><el-form-item label="加班事由" prop="reason"><el-input v-model.trim="form.reason" type="textarea" :rows="4" maxlength="500" show-word-limit /></el-form-item></el-form><template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="submitting" @click="submitNewRequest">提交审批</el-button></template></el-dialog>
  </PageFrame>
</template>
