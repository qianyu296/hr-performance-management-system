<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import PageFrame from '@/components/common/PageFrame.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { approveWorkflowTask, fetchWorkflowTasks, rejectWorkflowTask, type WorkflowTaskItem } from '@/api/workflow'

const loading = ref(false)
const tasks = ref<WorkflowTaskItem[]>([])

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

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

onMounted(loadData)
</script>

<template>
  <PageFrame title="审批中心" description="处理分配给当前用户的请假审批待办。">
    <template #actions>
      <el-button :icon="Refresh" @click="loadData">刷新</el-button>
    </template>

    <el-table v-loading="loading" :data="tasks" class="data-table">
      <el-table-column prop="requestNo" label="申请单号" min-width="150" />
      <el-table-column prop="applicantName" label="申请人" width="120" />
      <el-table-column prop="leaveTypeName" label="类型" width="120" />
      <el-table-column label="请假时间" min-width="260">
        <template #default="{ row }">{{ formatDateTime(row.startTime) }} 至 {{ formatDateTime(row.endTime) }}</template>
      </el-table-column>
      <el-table-column prop="durationHours" label="时长(小时)" width="110" />
      <el-table-column label="操作" width="170" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" @click="approve(row)">通过</el-button>
          <el-button text type="danger" @click="reject(row)">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>
    <EmptyState v-if="!loading && tasks.length === 0" title="暂无审批待办" description="新的请假审批任务会出现在这里。" />
  </PageFrame>
</template>
