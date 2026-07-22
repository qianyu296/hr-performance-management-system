<script setup lang="ts">
import { computed, ref, watchEffect } from 'vue'
import { useRouter } from 'vue-router'
import EmptyState from '@/components/common/EmptyState.vue'
import PageFrame from '@/components/common/PageFrame.vue'
import { useAuthStore } from '@/stores/auth'

interface CenterEntry {
  path: string
  title: string
  description: string
  permissions: string[]
  bullets: string[]
}

const router = useRouter()
const authStore = useAuthStore()
const activeTab = ref('')

const entries: CenterEntry[] = [
  {
    path: '/workflow/tasks',
    title: '审批任务',
    description: '处理待办任务、查看流程明细和审批历史。',
    permissions: ['workflow:approve'],
    bullets: ['统一查看待我处理的审批事项', '支持审批、驳回、退回和转交', '可直接追踪流程实例和业务单据'],
  },
  {
    path: '/workflow/templates',
    title: '流程模板',
    description: '维护审批节点、适用范围和优先级。',
    permissions: ['workflow:manage'],
    bullets: ['配置业务审批链和适用范围', '按优先级管理多套流程模板', '模板变更仅影响后续新提交单据'],
  },
]

const visibleEntries = computed(() => entries.filter((entry) => authStore.can(entry.permissions)))
const activeEntry = computed(() => visibleEntries.value.find((entry) => entry.path === activeTab.value) ?? visibleEntries.value[0] ?? null)
const metrics = computed(() => [
  { label: '可用入口', value: visibleEntries.value.length, note: '审批处理与流程配置分开管理' },
  { label: '核心目标', value: '提效', note: '减少流程流转和定位成本' },
])

watchEffect(() => {
  if (!visibleEntries.value.length) {
    activeTab.value = ''
    return
  }
  if (!visibleEntries.value.some((entry) => entry.path === activeTab.value)) activeTab.value = visibleEntries.value[0].path
})
</script>

<template>
  <PageFrame title="审批中心" description="统一处理审批任务和流程模板配置。">
    <div class="metric-grid">
      <article v-for="metric in metrics" :key="metric.label" class="metric-item">
        <span>{{ metric.label }}</span>
        <strong>{{ metric.value }}</strong>
        <small>{{ metric.note }}</small>
      </article>
    </div>

    <EmptyState v-if="!visibleEntries.length" title="暂无可用模块" description="当前账号还没有审批相关权限。" />

    <template v-else>
      <el-tabs v-model="activeTab" class="center-tabs">
        <el-tab-pane v-for="entry in visibleEntries" :key="entry.path" :label="entry.title" :name="entry.path" />
      </el-tabs>

      <section v-if="activeEntry" class="module-panel">
        <div class="module-panel__hero">
          <div>
            <span class="module-panel__eyebrow">协同流程</span>
            <h2>{{ activeEntry.title }}</h2>
            <p>{{ activeEntry.description }}</p>
          </div>
          <el-button type="primary" @click="router.push(activeEntry.path)">进入页面</el-button>
        </div>

        <article class="module-card">
          <h3>你可以在这里完成</h3>
          <ul>
            <li v-for="item in activeEntry.bullets" :key="item">{{ item }}</li>
          </ul>
        </article>
      </section>
    </template>
  </PageFrame>
</template>

<style scoped>
.center-tabs {
  margin-top: 24px;
}

.module-panel {
  margin-top: 12px;
}

.module-panel__hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  padding: 24px;
  border: 1px solid var(--el-border-color);
  border-radius: 18px;
  background: linear-gradient(135deg, #ffffff 0%, #f4f3ff 100%);
}

.module-panel__eyebrow {
  display: inline-block;
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 600;
  color: #7c3aed;
  letter-spacing: 0.08em;
}

.module-panel__hero h2 {
  margin: 0;
  font-size: 26px;
  color: #111827;
}

.module-panel__hero p {
  margin: 10px 0 0;
  max-width: 680px;
  line-height: 1.7;
  color: #4b5563;
}

.module-card {
  margin-top: 16px;
  padding: 22px;
  border: 1px solid var(--el-border-color);
  border-radius: 16px;
  background: #ffffff;
}

.module-card h3 {
  margin: 0 0 12px;
  font-size: 16px;
  color: #111827;
}

.module-card ul {
  margin: 0;
  padding-left: 18px;
}

.module-card li {
  line-height: 1.7;
  color: #4b5563;
}

@media (max-width: 900px) {
  .module-panel__hero {
    flex-direction: column;
  }
}
</style>
