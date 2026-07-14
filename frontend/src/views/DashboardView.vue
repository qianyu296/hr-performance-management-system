<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageFrame from '@/components/common/PageFrame.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { fetchWorkflowTasks, type WorkflowTaskItem } from '@/api/workflow'
import { fetchManagerPerformanceTasks, fetchMyPerformanceTasks, type PerformanceTask } from '@/api/performance'

type DashboardItem = { type: string; title: string; owner: string; status: string; route: string }
const router = useRouter(); const workflowTasks = ref<WorkflowTaskItem[]>([]); const myTasks = ref<PerformanceTask[]>([]); const managerTasks = ref<PerformanceTask[]>([]); const loading = ref(false)
const items = computed<DashboardItem[]>(() => [
  ...workflowTasks.value.map((task) => ({ type: '审批', title: task.requestNo || task.businessType, owner: task.applicantName, status: task.status, route: '/workflow/tasks' })),
  ...myTasks.value.filter((task) => task.status === 'PENDING_SELF_ASSESSMENT').map((task) => ({ type: '绩效自评', title: task.cycleName, owner: task.employeeName, status: task.status, route: '/performance/tasks' })),
  ...managerTasks.value.map((task) => ({ type: '绩效评分', title: task.cycleName, owner: task.employeeName, status: task.status, route: '/performance/tasks' })),
])
const metrics = computed(() => [{ label: '待处理审批', value: workflowTasks.value.length, note: '等待我的流程处理' }, { label: '待完成自评', value: myTasks.value.filter((task) => task.status === 'PENDING_SELF_ASSESSMENT').length, note: '当前周期绩效任务' }, { label: '待我评分', value: managerTasks.value.length, note: '直属下属提交后显示' }])
async function load() {
  loading.value = true
  const [workflow, mineResult, managerResult] = await Promise.allSettled([fetchWorkflowTasks(), fetchMyPerformanceTasks(), fetchManagerPerformanceTasks()])
  if (workflow.status === 'fulfilled') workflowTasks.value = workflow.value
  if (mineResult.status === 'fulfilled') myTasks.value = mineResult.value
  if (managerResult.status === 'fulfilled') managerTasks.value = managerResult.value
  if ([workflow, mineResult, managerResult].some((result) => result.status === 'rejected')) ElMessage.warning('部分待办暂时无法加载')
  loading.value = false
}
onMounted(load)
</script>

<template>
  <PageFrame title="工作台" description="聚合审批待办与当前绩效任务。">
    <template #actions><el-button @click="load">刷新</el-button></template>
    <div class="metric-grid">
      <article v-for="metric in metrics" :key="metric.label" class="metric-item">
        <span>{{ metric.label }}</span>
        <strong>{{ metric.value }}</strong>
        <small>{{ metric.note }}</small>
      </article>
    </div>
    <section class="dashboard-section">
      <div class="section-heading">
        <h2>待处理事项</h2>
        <el-button text @click="router.push('/workflow/tasks')">审批中心</el-button>
      </div>
      <el-table v-loading="loading" :data="items" class="data-table">
        <el-table-column prop="type" label="业务类型" min-width="150" />
        <el-table-column prop="title" label="事项" min-width="260" />
        <el-table-column prop="owner" label="相关员工" width="140" />
        <el-table-column prop="status" label="状态" width="190" />
        <el-table-column label="操作" width="120"><template #default="{ row }"><el-button text type="primary" @click="router.push(row.route)">处理</el-button></template></el-table-column>
      </el-table>
      <EmptyState v-if="!loading && !items.length" title="暂无待处理事项" description="新的审批或绩效任务会显示在这里。" />
    </section>
  </PageFrame>
</template>
