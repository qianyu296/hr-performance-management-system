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
}

interface CenterSection {
  key: string
  title: string
  description: string
  summary: string
  entries: CenterEntry[]
}

const router = useRouter()
const authStore = useAuthStore()
const activeSectionKey = ref('')

const sections: CenterSection[] = [
  {
    key: 'requests',
    title: '申请单',
    description: '围绕员工日常发起和追踪的假勤申请。',
    summary: '适合员工、主管和 HR 查看申请进度、取消记录以及个人余额。',
    entries: [
      { path: '/attendance/leave', title: '请假管理', description: '发起请假、提交审批并跟踪状态。', permissions: ['attendance:submit'] },
      { path: '/attendance/overtime', title: '加班管理', description: '登记加班、选择补偿方式并查看进度。', permissions: ['attendance:submit'] },
      { path: '/attendance/balances', title: '假期余额', description: '查看个人余额和调整流水。', permissions: ['attendance:submit', 'attendance:balance:adjust'] },
    ],
  },
  {
    key: 'config',
    title: '规则配置',
    description: '维护假勤制度、日历和汇总口径。',
    summary: '适合假勤管理员集中维护年度规则、工作日历和统计结果。',
    entries: [
      { path: '/attendance/leave-types', title: '请假类型', description: '配置年度额度、扣减规则和最小单位。', permissions: ['attendance:manage'] },
      { path: '/attendance/calendar', title: '工作日历', description: '维护节假日、调休和工时安排。', permissions: ['attendance:manage'] },
      { path: '/attendance/summary', title: '月度假勤汇总', description: '按员工查看请假、加班和待审批汇总。', permissions: ['attendance:manage'] },
    ],
  },
]

const visibleSections = computed(() =>
  sections
    .map((section) => ({
      ...section,
      entries: section.entries.filter((entry) => authStore.can(entry.permissions)),
    }))
    .filter((section) => section.entries.length > 0)
)
const activeSection = computed(() => visibleSections.value.find((section) => section.key === activeSectionKey.value) ?? visibleSections.value[0] ?? null)
const metrics = computed(() => [
  { label: '可用入口', value: visibleSections.value.reduce((count, section) => count + section.entries.length, 0), note: '按权限动态聚合' },
  { label: '业务视角', value: visibleSections.value.length, note: '申请处理与规则配置分开展示' },
])

watchEffect(() => {
  if (!visibleSections.value.length) {
    activeSectionKey.value = ''
    return
  }
  if (!visibleSections.value.some((section) => section.key === activeSectionKey.value)) activeSectionKey.value = visibleSections.value[0].key
})
</script>

<template>
  <PageFrame title="假勤中心" description="集中处理假勤申请、规则配置和余额汇总。">
    <div class="metric-grid">
      <article v-for="metric in metrics" :key="metric.label" class="metric-item">
        <span>{{ metric.label }}</span>
        <strong>{{ metric.value }}</strong>
        <small>{{ metric.note }}</small>
      </article>
    </div>

    <EmptyState v-if="!visibleSections.length" title="暂无可用模块" description="当前账号还没有假勤相关权限。" />

    <template v-else>
      <el-tabs v-model="activeSectionKey" class="center-tabs">
        <el-tab-pane v-for="section in visibleSections" :key="section.key" :label="section.title" :name="section.key" />
      </el-tabs>

      <section v-if="activeSection" class="module-panel">
        <div class="module-panel__hero">
          <div>
            <span class="module-panel__eyebrow">业务视角</span>
            <h2>{{ activeSection.title }}</h2>
            <p>{{ activeSection.description }}</p>
          </div>
          <p class="module-panel__summary">{{ activeSection.summary }}</p>
        </div>

        <div class="entry-grid">
          <article v-for="entry in activeSection.entries" :key="entry.path" class="entry-card">
            <div>
              <h3>{{ entry.title }}</h3>
              <p>{{ entry.description }}</p>
            </div>
            <el-button text type="primary" @click="router.push(entry.path)">进入页面</el-button>
          </article>
        </div>
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
  display: grid;
  grid-template-columns: 1.3fr 1fr;
  gap: 24px;
  padding: 24px;
  border: 1px solid var(--el-border-color);
  border-radius: 18px;
  background: linear-gradient(135deg, #ffffff 0%, #effbf3 100%);
}

.module-panel__eyebrow {
  display: inline-block;
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 600;
  color: #059669;
  letter-spacing: 0.08em;
}

.module-panel__hero h2 {
  margin: 0;
  font-size: 26px;
  color: #111827;
}

.module-panel__hero p,
.module-panel__summary {
  margin: 10px 0 0;
  line-height: 1.7;
  color: #4b5563;
}

.module-panel__summary {
  margin: 0;
  align-self: center;
  padding: 18px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.72);
}

.entry-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.entry-card {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 16px;
  min-height: 190px;
  padding: 22px;
  border: 1px solid var(--el-border-color);
  border-radius: 16px;
  background: #ffffff;
}

.entry-card h3 {
  margin: 0 0 10px;
  font-size: 18px;
  color: #111827;
}

.entry-card p {
  margin: 0;
  line-height: 1.7;
  color: #4b5563;
}

@media (max-width: 900px) {
  .module-panel__hero {
    grid-template-columns: 1fr;
  }
}
</style>
