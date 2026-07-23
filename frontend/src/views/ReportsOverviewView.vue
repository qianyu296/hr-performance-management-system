<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageFrame from '@/components/common/PageFrame.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { fetchDepartmentHeadcounts, type DepartmentHeadcount } from '@/api/reports'

const router = useRouter()
const departments = ref<DepartmentHeadcount[]>([])
const loading = ref(false)
const totalHeadcount = computed(() => departments.value.reduce((total, item) => total + item.headcount, 0))

async function load() {
  loading.value = true
  try {
    departments.value = await fetchDepartmentHeadcounts()
  } catch {
    ElMessage.error('无法加载人员分析数据')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <PageFrame title="数据分析" description="查看人员规模和假勤统计入口。">
    <template #actions>
      <el-button @click="load">刷新</el-button>
    </template>

    <div class="metric-grid">
      <article class="metric-item">
        <span>在职人员</span>
        <strong>{{ totalHeadcount }}</strong>
        <small>按当前启用部门汇总</small>
      </article>
      <article class="metric-item">
        <span>分析模块</span>
        <strong>2</strong>
        <small>人员分析与假勤统计</small>
      </article>
    </div>

    <section class="report-entry-grid">
      <article class="report-entry-card">
        <div>
          <h2>人员分析</h2>
          <p>查看部门人数和组织规模分布，适合管理层掌握当前人员结构。</p>
        </div>
        <el-button text type="primary" disabled>当前页面</el-button>
      </article>
      <article class="report-entry-card">
        <div>
          <h2>假勤统计</h2>
          <p>查看月度请假、加班、调休净变动和待审批数据，统一归档到数据分析模块。</p>
        </div>
        <el-button text type="primary" @click="router.push('/reports/attendance')">进入页面</el-button>
      </article>
    </section>

    <section v-loading="loading">
      <div class="section-heading"><h2>部门人员规模</h2></div>
      <el-table :data="departments" class="data-table">
        <el-table-column prop="departmentName" label="部门" />
        <el-table-column prop="headcount" label="在职人数" width="140" />
      </el-table>
      <EmptyState v-if="!loading && !departments.length" title="暂无人员数据" description="创建启用部门和员工后会显示汇总。" />
    </section>
  </PageFrame>
</template>

<style scoped>
.report-entry-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.report-entry-card {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 16px;
  min-height: 180px;
  padding: 22px;
  border: 1px solid var(--el-border-color);
  border-radius: 16px;
  background: #ffffff;
}

.report-entry-card h2 {
  margin: 0 0 10px;
  font-size: 20px;
  color: #111827;
}

.report-entry-card p {
  margin: 0;
  line-height: 1.7;
  color: #4b5563;
}
</style>
