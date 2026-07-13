<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import PageFrame from '@/components/common/PageFrame.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { fetchAttendanceMonthlySummaries, rebuildAttendanceMonthlySummaries, type AttendanceMonthlySummary } from '@/api/attendance'

const today = new Date()
const month = ref(`${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}`)
const departmentId = ref('')
const employeeId = ref('')
const loading = ref(false)
const rebuilding = ref(false)
const summaries = ref<AttendanceMonthlySummary[]>([])

function formatDateTime(value: string) { return new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value)) }

async function load() {
  loading.value = true
  try {
    summaries.value = await fetchAttendanceMonthlySummaries(month.value, { departmentId: departmentId.value || undefined, employeeId: employeeId.value || undefined })
  } catch {
    summaries.value = []
    ElMessage.error('无法加载月度汇总')
  } finally { loading.value = false }
}

async function rebuild() {
  rebuilding.value = true
  try {
    const result = await rebuildAttendanceMonthlySummaries(month.value)
    ElMessage.success(`汇总已重建，共 ${result.affectedRows} 条员工记录`)
    await load()
  } catch {
    ElMessage.error('汇总重建失败，请稍后重试')
  } finally { rebuilding.value = false }
}

onMounted(load)
</script>

<template>
  <PageFrame title="月度假勤汇总" description="按员工聚合当月已通过请假、加班、调休净变动和待审批数量。">
    <template #actions><el-button :icon="Refresh" @click="load">刷新</el-button><el-button type="primary" :loading="rebuilding" @click="rebuild">重建本月汇总</el-button></template>
    <template #filters>
      <el-date-picker v-model="month" type="month" value-format="YYYY-MM" placeholder="统计月份" @change="load" />
      <el-input v-model="departmentId" inputmode="numeric" placeholder="部门 ID" clearable @change="load" />
      <el-input v-model="employeeId" inputmode="numeric" placeholder="员工 ID" clearable @change="load" />
    </template>
    <el-table v-loading="loading" :data="summaries" class="data-table">
      <el-table-column prop="employeeNo" label="工号" width="130" />
      <el-table-column prop="employeeName" label="员工" width="130" />
      <el-table-column prop="departmentName" label="部门" min-width="150" />
      <el-table-column prop="leaveHours" label="请假(小时)" width="120" />
      <el-table-column prop="overtimeHours" label="加班(小时)" width="120" />
      <el-table-column prop="timeOffDeltaHours" label="调休净变动" width="130" />
      <el-table-column prop="pendingRequestCount" label="待审批" width="100" />
      <el-table-column label="生成时间" min-width="165"><template #default="{ row }">{{ formatDateTime(row.generatedTime) }}</template></el-table-column>
    </el-table>
    <EmptyState v-if="!loading && summaries.length === 0" title="暂无月度汇总" description="选择月份后点击重建，生成当前员工范围的汇总快照。" />
  </PageFrame>
</template>
