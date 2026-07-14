<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import PageFrame from '@/components/common/PageFrame.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import {
  cancelLeaveRequest,
  createLeaveRequest,
  fetchLeaveRequests,
  fetchLeaveTypes,
  submitLeaveRequest,
  type LeaveRequestItem,
  type LeaveTypeOption,
} from '@/api/leave'
import { fetchWorkflowInstance, withdrawWorkflowInstance } from '@/api/workflow'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const leaveTypes = ref<LeaveTypeOption[]>([])
const requests = ref<LeaveRequestItem[]>([])
const statusFilter = ref('')

const form = reactive({
  leaveTypeId: '',
  timeRange: null as [Date, Date] | null,
  reason: '',
})

const rules: FormRules<typeof form> = {
  leaveTypeId: [{ required: true, message: '请选择请假类型', trigger: 'change' }],
  timeRange: [{ required: true, message: '请选择起止时间', trigger: 'change' }],
  reason: [{ required: true, message: '请输入请假事由', trigger: 'blur' }],
}

const filteredRequests = computed(() => requests.value.filter((item) => !statusFilter.value || item.status === statusFilter.value))

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

function statusLabel(status: string) {
  return ({ DRAFT: '草稿', IN_PROGRESS: '审批中', APPROVED: '已通过', REJECTED: '已驳回', CANCELLED: '已撤销' } as Record<string, string>)[status] ?? status
}

function submitErrorMessage(error: any) {
  const message = error?.response?.data?.message
  if (message === 'Leave request overlaps an existing request') {
    return '请假时间与已有审批中或已通过的请假申请重叠，请调整时间后重试'
  }
  if (message === 'Leave balance is insufficient') {
    return '可用请假余额不足，请联系 HR 调整余额或缩短请假时长'
  }
  if (message === 'Workflow task is invalid') {
    return '请假审批流程未能找到有效审批人，请联系 HR 检查流程节点配置'
  }
  if (message === 'No matching workflow template is available') {
    return '未配置适用的请假审批流程，请联系 HR 配置流程模板'
  }
  return '请假提交失败，请稍后重试或联系 HR 确认请假规则与审批流程'
}

async function loadData() {
  loading.value = true
  try {
    const [types, list] = await Promise.all([fetchLeaveTypes(), fetchLeaveRequests()])
    leaveTypes.value = types
    requests.value = list
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  form.leaveTypeId = leaveTypes.value[0]?.id ?? ''
  form.timeRange = null
  form.reason = ''
  dialogVisible.value = true
}

async function submitNewRequest() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || !form.timeRange) return
  submitting.value = true
  try {
    const draft = await createLeaveRequest({
      leaveTypeId: form.leaveTypeId,
      startTime: form.timeRange[0].toISOString(),
      endTime: form.timeRange[1].toISOString(),
      reason: form.reason,
    })
    await submitLeaveRequest(draft.id, 0)
    ElMessage.success('请假申请已提交审批')
    dialogVisible.value = false
    await loadData()
  } catch (error) {
    ElMessage.error(submitErrorMessage(error))
  } finally {
    submitting.value = false
  }
}

async function submitDraft(row: LeaveRequestItem) {
  try {
    await submitLeaveRequest(row.id, row.version)
    ElMessage.success('草稿已提交审批')
    await loadData()
  } catch (error) {
    ElMessage.error(submitErrorMessage(error))
  }
}

async function cancelApproved(row: LeaveRequestItem) {
  await ElMessageBox.confirm('确认撤销这条已通过的请假申请吗？撤销后会恢复对应余额。', '撤销请假', { type: 'warning' })
  await cancelLeaveRequest(row.id, row.version)
  ElMessage.success('请假申请已撤销')
  await loadData()
}

async function withdrawPending(row: LeaveRequestItem) {
  if (!row.workflowInstanceId) return
  await ElMessageBox.confirm('确认撤回这条审批中的请假申请吗？撤回后会恢复为草稿。', '撤回请假', { type: 'warning' })
  const instance = await fetchWorkflowInstance(row.workflowInstanceId)
  await withdrawWorkflowInstance(row.workflowInstanceId, instance.version, 'Request withdrawn by applicant')
  ElMessage.success('请假申请已撤回')
  await loadData()
}

onMounted(loadData)
</script>

<template>
  <PageFrame title="请假管理" description="创建请假申请，查看提交状态，并处理已通过申请的撤销。">
    <template #actions>
      <el-button :icon="Refresh" @click="loadData">刷新</el-button>
      <el-button type="primary" :icon="Plus" @click="openCreateDialog">新建请假</el-button>
    </template>
    <template #filters>
      <el-select v-model="statusFilter" placeholder="全部状态" clearable>
        <el-option label="草稿" value="DRAFT" />
        <el-option label="审批中" value="IN_PROGRESS" />
        <el-option label="已通过" value="APPROVED" />
        <el-option label="已驳回" value="REJECTED" />
        <el-option label="已撤销" value="CANCELLED" />
      </el-select>
    </template>

    <el-table v-loading="loading" :data="filteredRequests" class="data-table">
      <el-table-column prop="requestNo" label="申请单号" min-width="150" />
      <el-table-column prop="leaveTypeName" label="请假类型" min-width="130" />
      <el-table-column label="请假时间" min-width="260">
        <template #default="{ row }">{{ formatDateTime(row.startTime) }} 至 {{ formatDateTime(row.endTime) }}</template>
      </el-table-column>
      <el-table-column prop="durationHours" label="时长(小时)" width="110" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }"><el-tag>{{ statusLabel(row.status) }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'DRAFT'" text type="primary" @click="submitDraft(row)">提交</el-button>
          <el-button v-if="row.status === 'IN_PROGRESS'" text type="warning" @click="withdrawPending(row)">撤回</el-button>
          <el-button v-if="row.status === 'APPROVED'" text type="danger" @click="cancelApproved(row)">撤销</el-button>
        </template>
      </el-table-column>
    </el-table>
    <EmptyState v-if="!loading && filteredRequests.length === 0" title="暂无请假记录" description="点击新建请假发起第一条申请。" />

    <el-dialog v-model="dialogVisible" title="新建请假申请" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="请假类型" prop="leaveTypeId">
          <el-select v-model="form.leaveTypeId" placeholder="请选择请假类型" class="form-control-full">
            <el-option v-for="item in leaveTypes" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="起止时间" prop="timeRange">
          <el-date-picker v-model="form.timeRange" type="datetimerange" start-placeholder="开始时间" end-placeholder="结束时间" class="form-control-full" />
        </el-form-item>
        <el-form-item label="请假事由" prop="reason">
          <el-input v-model.trim="form.reason" type="textarea" :rows="4" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitNewRequest">提交审批</el-button>
      </template>
    </el-dialog>
  </PageFrame>
</template>
