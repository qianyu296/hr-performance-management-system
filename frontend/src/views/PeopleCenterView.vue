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
  highlights: string[]
  actionText: string
}

const router = useRouter()
const authStore = useAuthStore()
const activeTab = ref('')

const entries: CenterEntry[] = [
  {
    path: '/org/departments',
    title: '组织架构',
    description: '查看部门层级、负责人和组织结构调整。',
    permissions: ['org:read'],
    highlights: ['维护部门树与负责人关系', '处理组织启停、移动和层级调整', '统一承接员工归属与汇报链'],
    actionText: '进入组织架构',
  },
  {
    path: '/org/employees',
    title: '员工档案',
    description: '管理员工目录、任职信息和历史记录。',
    permissions: ['org:read'],
    highlights: ['查看员工主数据与任职状态', '维护岗位、职级和部门归属', '追踪履历和历史变更'],
    actionText: '进入员工档案',
  },
  {
    path: '/personnel/changes',
    title: '人事异动',
    description: '处理入转调离、交接事项和异动进度。',
    permissions: ['personnel:read', 'personnel:create', 'personnel:manage', 'personnel:approve', 'personnel:execute'],
    highlights: ['发起和跟踪入转调离申请', '查看审批流转与交接事项', '执行异动后的生效与落档'],
    actionText: '进入人事异动',
  },
]

const visibleEntries = computed(() => entries.filter((entry) => authStore.can(entry.permissions)))
const activeEntry = computed(() => visibleEntries.value.find((entry) => entry.path === activeTab.value) ?? visibleEntries.value[0] ?? null)
const metrics = computed(() => [
  { label: '可用子模块', value: visibleEntries.value.length, note: '按当前权限自动显示' },
  { label: '管理范围', value: '组织 + 人事', note: '覆盖组织结构、档案与异动流程' },
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
  <PageFrame title="组织人事" description="统一查看组织架构、员工档案和人事异动。">
    <div class="metric-grid">
      <article v-for="metric in metrics" :key="metric.label" class="metric-item">
        <span>{{ metric.label }}</span>
        <strong>{{ metric.value }}</strong>
        <small>{{ metric.note }}</small>
      </article>
    </div>

    <EmptyState v-if="!visibleEntries.length" title="暂无可用模块" description="当前账号还没有组织或人事相关权限。" />

    <template v-else>
      <el-tabs v-model="activeTab" class="center-tabs">
        <el-tab-pane v-for="entry in visibleEntries" :key="entry.path" :label="entry.title" :name="entry.path" />
      </el-tabs>

      <section v-if="activeEntry" class="module-panel">
        <div class="module-panel__hero">
          <div>
            <span class="module-panel__eyebrow">模块说明</span>
            <h2>{{ activeEntry.title }}</h2>
            <p>{{ activeEntry.description }}</p>
          </div>
          <el-button type="primary" @click="router.push(activeEntry.path)">{{ activeEntry.actionText }}</el-button>
        </div>

        <div class="module-panel__content">
          <article class="module-card">
            <h3>典型操作</h3>
            <ul>
              <li v-for="item in activeEntry.highlights" :key="item">{{ item }}</li>
            </ul>
          </article>
          <article class="module-card module-card--soft">
            <h3>推荐入口</h3>
            <p>如果你正在处理组织信息维护，从组织架构开始；如果你关注员工个体信息和变动结果，优先进入员工档案或人事异动。</p>
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
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  padding: 24px;
  border: 1px solid var(--el-border-color);
  border-radius: 18px;
  background: linear-gradient(135deg, #ffffff 0%, #eef5ff 100%);
}

.module-panel__eyebrow {
  display: inline-block;
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 600;
  color: #2563eb;
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

.module-panel__content {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 16px;
  margin-top: 16px;
}

.module-card {
  padding: 22px;
  border: 1px solid var(--el-border-color);
  border-radius: 16px;
  background: #ffffff;
}

.module-card--soft {
  background: #f8fafc;
}

.module-card h3 {
  margin: 0 0 12px;
  font-size: 16px;
  color: #111827;
}

.module-card p,
.module-card li {
  line-height: 1.7;
  color: #4b5563;
}

.module-card ul {
  margin: 0;
  padding-left: 18px;
}

@media (max-width: 900px) {
  .module-panel__hero,
  .module-panel__content {
    grid-template-columns: 1fr;
    flex-direction: column;
  }
}
</style>
