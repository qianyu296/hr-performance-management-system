<script setup lang="ts">
import PageFrame from '@/components/common/PageFrame.vue'

const metrics = [
  { label: '待处理审批', value: '0', note: '请假、人事与绩效流程' },
  { label: '我的请假', value: '0', note: '本月申请与待提交草稿' },
  { label: '绩效任务', value: '0', note: '当前周期待完成事项' },
  { label: '未读通知', value: '0', note: '系统与流程消息' },
]
</script>

<template>
  <PageFrame title="工作台" description="聚合待办、通知和当前工作周期的关键事项。">
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
        <el-button text>查看全部</el-button>
      </div>
      <el-table :data="[]" class="data-table">
        <el-table-column label="业务类型" min-width="180" />
        <el-table-column label="事项" min-width="260" />
        <el-table-column label="发起人" width="140" />
        <el-table-column label="提交时间" width="180" />
        <el-table-column label="操作" width="120" />
      </el-table>
      <div class="empty-inline">暂无待处理事项</div>
    </section>
  </PageFrame>
</template>
